/*
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.accertify.api;

import java.sql.SQLException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.killbill.billing.control.plugin.api.PaymentControlApiException;
import org.killbill.billing.control.plugin.api.PaymentControlContext;
import org.killbill.billing.control.plugin.api.PriorPaymentControlResult;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.accertify.client.AccertifyClientException;
import org.killbill.billing.plugin.accertify.client.RequestBuilder;
import org.killbill.billing.plugin.accertify.client.TransactionResults;
import org.killbill.billing.plugin.accertify.core.AccertifyConfigurationHandler;
import org.killbill.billing.plugin.accertify.dao.AccertifyDao;
import org.killbill.billing.plugin.api.control.PluginPaymentControlPluginApi;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class AccertifyPaymentControlPluginApi extends PluginPaymentControlPluginApi {

    private static final Logger logger = LoggerFactory.getLogger(AccertifyPaymentControlPluginApi.class);

    private static final Pattern ACCERTIFY_PROPERTIES_PATTERN = Pattern.compile("accertify_(.*)");

    @VisibleForTesting
    static final String ACCERTIFY_REJECT = "REJECT";

    private final Collection<String> paymentPluginsSubjectToAutomaticRejection;
    private final AccertifyDao dao;
    private final AccertifyConfigurationHandler accertifyConfigurationHandler;

    public AccertifyPaymentControlPluginApi(final Collection<String> paymentPluginsSubjectToAutomaticRejection,
                                            final AccertifyDao dao,
                                            final AccertifyConfigurationHandler accertifyConfigurationHandler,
                                            final OSGIKillbillAPI killbillApi,
                                            final OSGIConfigPropertiesService configProperties,
                                            final OSGIKillbillLogService logService,
                                            final Clock clock) {
        super(killbillApi, configProperties, logService, clock);
        this.paymentPluginsSubjectToAutomaticRejection = paymentPluginsSubjectToAutomaticRejection;
        this.dao = dao;
        this.accertifyConfigurationHandler = accertifyConfigurationHandler;
    }

    @Override
    public PriorPaymentControlResult priorCall(final PaymentControlContext context, final Iterable<PluginProperty> properties) throws PaymentControlApiException {
        // Check with Accertify
        final boolean shouldReject = assess(context, properties);
        // Check if we should automatically reject the payment
        final boolean shouldHonorAccertify = shouldHonorAccertify(context);

        final boolean shouldAbortPayment = shouldReject && shouldHonorAccertify;
        logger.info("Accertify result: shouldAbortPayment={} (shouldReject={}, shouldHonorAccertify={})", shouldAbortPayment, shouldReject, shouldHonorAccertify);
        return new AccertifyPriorPaymentControlResult(shouldAbortPayment, context);
    }

    private boolean assess(final PaymentControlContext context, final Iterable<PluginProperty> properties) {
        final String transactions;
        try {
            transactions = createAccertifyTransactions(properties);
        } catch (final AccertifyClientException e) {
            logger.warn("Error while creating the Accertify payload", e);
            return false;
        }

        TransactionResults transactionResults = null;
        try {
            transactionResults = accertifyConfigurationHandler.getConfigurable(context.getTenantId()).assess(transactions);
            logger.info("Accertify {} recommendation: kbPaymentTransactionId={}, total-score={}, rules-tripped={}, remarks={}",
                        transactionResults.getRecommendationCode(),
                        context.getTransactionId(),
                        transactionResults.getTotalScore(),
                        transactionResults.getRulesTripped(),
                        transactionResults.getRemarks());
        } catch (final AccertifyClientException e) {
            logger.warn("Error while going to Accertify", e);
        }

        try {
            dao.addResponse(context.getAccountId(),
                            context.getPaymentExternalKey(),
                            context.getTransactionExternalKey(),
                            context.getTransactionType(),
                            context.getAmount(),
                            context.getCurrency(),
                            transactionResults,
                            clock.getUTCNow(),
                            context.getTenantId());
        } catch (final SQLException e) {
            logger.warn("Error while storing the Accertify record", e);
        }

        return transactionResults != null && ACCERTIFY_REJECT.equals(transactionResults.getRecommendationCode());
    }

    private String createAccertifyTransactions(final Iterable<PluginProperty> properties) throws AccertifyClientException {
        final RequestBuilder requestBuilder = new RequestBuilder();
        for (final PluginProperty pluginProperty : properties) {
            if (pluginProperty.getKey() != null) {
                final Matcher matcher = ACCERTIFY_PROPERTIES_PATTERN.matcher(pluginProperty.getKey());
                if (matcher.matches()) {
                    final String key = matcher.group(1);
                    requestBuilder.addTransactionEntry(key, pluginProperty.getValue());
                }
            }
        }
        return requestBuilder.build();
    }

    private boolean shouldHonorAccertify(final PaymentControlContext context) {
        final PaymentMethod paymentMethod = getPaymentMethod(context.getPaymentMethodId(), context);
        return paymentPluginsSubjectToAutomaticRejection.contains(paymentMethod.getPluginName());
    }
}
