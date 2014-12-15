/*
 * Copyright 2014 Groupon, Inc
 * Copyright 2014 The Billing Project, LLC
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentTransaction;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.accertify.client.AccertifyClient;
import org.killbill.billing.plugin.accertify.core.AccertifyActivator;
import org.killbill.billing.plugin.accertify.dao.AccertifyDao;
import org.killbill.billing.plugin.accertify.dao.gen.tables.records.AccertifyResponsesRecord;
import org.killbill.billing.routing.plugin.api.PaymentRoutingContext;
import org.killbill.billing.routing.plugin.api.PriorPaymentRoutingResult;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.killbill.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

// To run these tests, you need two properties files in the classpath (e.g. src/test/resources/accertify.properties):
// * accertify.properties with your Accertify credentials
// * payload.properties with plugin properties matching your XML schema
// See README.md for details on the required properties
public class TestAccertifyPaymentRoutingPluginApi extends TestWithEmbeddedDBBase {

    private static final String ACCERTIFY_PROPERTIES = "accertify.properties";
    private static final String PAYLOAD_RESOURCE = "payload.properties";

    private Collection<String> paymentPluginsSubjectToAutomaticRejection;
    private AccertifyDao dao;
    private AccertifyClient client;
    private OSGIKillbillAPI killbillApi;
    private OSGIConfigPropertiesService configProperties;
    private OSGIKillbillLogService logService;
    private Clock clock;
    private PaymentRoutingContext routingContext;
    private List<PluginProperty> pluginProperties;

    @BeforeMethod(groups = "slow")
    public void setUp() throws Exception {
        final String paymentPluginName = "killbill-ACME";

        final Account account = TestUtils.buildAccount(Currency.USD, "US");
        final UUID accountId = account.getId();
        final Payment payment = TestUtils.buildPayment(accountId, account.getPaymentMethodId(), account.getCurrency());
        final PaymentTransaction paymentTransaction = TestUtils.buildPaymentTransaction(payment, TransactionType.AUTHORIZE, payment.getCurrency());
        final PaymentMethod paymentMethod = TestUtils.buildPaymentMethod(accountId, account.getPaymentMethodId(), paymentPluginName);
        killbillApi = TestUtils.buildOSGIKillbillAPI(account, payment, paymentMethod);

        paymentPluginsSubjectToAutomaticRejection = ImmutableList.<String>of(paymentPluginName);
        dao = new AccertifyDao(embeddedDB.getDataSource());
        configProperties = Mockito.mock(OSGIConfigPropertiesService.class);
        logService = TestUtils.buildLogService();
        clock = new DefaultClock();

        buildAccertifyClient();

        routingContext = buildPaymentRoutingContext(accountId, payment, paymentTransaction);

        buildPluginProperties();
    }

    @Test(groups = "slow")
    public void testIntegrationWithPluginMatch() throws Exception {
        final AccertifyPaymentRoutingPluginApi pluginApi = new AccertifyPaymentRoutingPluginApi(paymentPluginsSubjectToAutomaticRejection,
                                                                                                dao,
                                                                                                client,
                                                                                                killbillApi,
                                                                                                configProperties,
                                                                                                logService,
                                                                                                clock);

        final PriorPaymentRoutingResult routingResult = pluginApi.priorCall(routingContext, pluginProperties);

        final List<AccertifyResponsesRecord> responses = dao.getResponses(routingContext.getPaymentExternalKey(), routingContext.getTenantId());
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(AccertifyPaymentRoutingPluginApi.ACCERTIFY_REJECT.equals(responses.get(0).getRecommendationCode()), routingResult.isAborted());
    }

    @Test(groups = "slow")
    public void testIntegrationWithoutPluginMatch() throws Exception {
        final AccertifyPaymentRoutingPluginApi pluginApi = new AccertifyPaymentRoutingPluginApi(ImmutableList.<String>of(),
                                                                                                dao,
                                                                                                client,
                                                                                                killbillApi,
                                                                                                configProperties,
                                                                                                logService,
                                                                                                clock);

        final PriorPaymentRoutingResult routingResult = pluginApi.priorCall(routingContext, pluginProperties);

        final List<AccertifyResponsesRecord> responses = dao.getResponses(routingContext.getPaymentExternalKey(), routingContext.getTenantId());
        Assert.assertEquals(responses.size(), 1);
        Assert.assertFalse(routingResult.isAborted());
    }

    private void buildPluginProperties() throws IOException {
        this.pluginProperties = new LinkedList<PluginProperty>();
        final String payload = TestUtils.toString(PAYLOAD_RESOURCE);
        for (final String rawProperty : payload.split("\n")) {
            final String[] rawPropertySplit = rawProperty.split("=");
            pluginProperties.add(new PluginProperty(rawPropertySplit[0], rawPropertySplit.length == 1 ? null : rawPropertySplit[1], false));
        }
    }

    private void buildAccertifyClient() throws IOException {
        final Properties properties = TestUtils.loadProperties(ACCERTIFY_PROPERTIES);
        final String proxyPortString = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "proxyPort");
        final String strictSSLString = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "strictSSL");
        client = new AccertifyClient(properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "url"),
                                     properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "username"),
                                     properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "password"),
                                     properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "proxyHost"),
                                     Strings.isNullOrEmpty(proxyPortString) ? null : Integer.valueOf(proxyPortString),
                                     Strings.isNullOrEmpty(strictSSLString) ? true : Boolean.valueOf(strictSSLString));
    }

    private PaymentRoutingContext buildPaymentRoutingContext(final UUID accountId, final Payment payment, final PaymentTransaction paymentTransaction) {
        // Need to initialize these for Mockito (mocks of mocks)
        final String paymentExternalKey = payment.getExternalKey();
        final UUID paymentMethodId = payment.getPaymentMethodId();
        final String paymentTransactionExternalKey = paymentTransaction.getExternalKey();
        final TransactionType transactionType = paymentTransaction.getTransactionType();
        final BigDecimal amount = paymentTransaction.getAmount();
        final Currency currency = paymentTransaction.getCurrency();

        final PaymentRoutingContext routingContext = Mockito.mock(PaymentRoutingContext.class);
        Mockito.when(routingContext.getAccountId()).thenReturn(accountId);
        Mockito.when(routingContext.getPaymentExternalKey()).thenReturn(paymentExternalKey);
        Mockito.when(routingContext.getTransactionExternalKey()).thenReturn(paymentTransactionExternalKey);
        Mockito.when(routingContext.getTransactionType()).thenReturn(transactionType);
        Mockito.when(routingContext.getAmount()).thenReturn(amount);
        Mockito.when(routingContext.getCurrency()).thenReturn(currency);
        Mockito.when(routingContext.getPaymentMethodId()).thenReturn(paymentMethodId);
        Mockito.when(routingContext.getTenantId()).thenReturn(UUID.randomUUID());
        return routingContext;
    }
}
