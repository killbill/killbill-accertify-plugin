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

package org.killbill.billing.plugin.accertify.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.jooq.impl.DSL;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.api.TransactionType;
import org.killbill.billing.plugin.accertify.client.TransactionResults;
import org.killbill.billing.plugin.accertify.dao.gen.tables.records.AccertifyResponsesRecord;
import org.killbill.billing.plugin.dao.PluginDao;

import static org.killbill.billing.plugin.accertify.dao.gen.tables.AccertifyResponses.ACCERTIFY_RESPONSES;

public class AccertifyDao extends PluginDao {

    public AccertifyDao(final DataSource dataSource) throws SQLException {
        super(dataSource);
    }

    public void addResponse(final UUID kbAccountId,
                            final String kbPaymentExternalKey,
                            final String kbPaymentTransactionExternalKey,
                            final TransactionType transactionType,
                            final BigDecimal amount,
                            final Currency currency,
                            @Nullable final TransactionResults transactionResults,
                            final DateTime utcNow,
                            final UUID kbTenantId) throws SQLException {
        execute(dataSource.getConnection(),
                new WithConnectionCallback<Void>() {
                    @Override
                    public Void withConnection(final Connection conn) throws SQLException {
                        DSL.using(conn, dialect, settings)
                           .insertInto(ACCERTIFY_RESPONSES,
                                       ACCERTIFY_RESPONSES.KB_ACCOUNT_ID,
                                       ACCERTIFY_RESPONSES.KB_PAYMENT_EXTERNAL_KEY,
                                       ACCERTIFY_RESPONSES.KB_PAYMENT_TRANSACTION_EXTERNAL_KEY,
                                       ACCERTIFY_RESPONSES.TRANSACTION_TYPE,
                                       ACCERTIFY_RESPONSES.AMOUNT,
                                       ACCERTIFY_RESPONSES.CURRENCY,
                                       ACCERTIFY_RESPONSES.TRANSACTION_ID,
                                       ACCERTIFY_RESPONSES.CROSS_REFERENCE,
                                       ACCERTIFY_RESPONSES.RULES_TRIPPED,
                                       ACCERTIFY_RESPONSES.TOTAL_SCORE,
                                       ACCERTIFY_RESPONSES.RECOMMENDATION_CODE,
                                       ACCERTIFY_RESPONSES.REMARKS,
                                       ACCERTIFY_RESPONSES.ADDITIONAL_DATA,
                                       ACCERTIFY_RESPONSES.CREATED_DATE,
                                       ACCERTIFY_RESPONSES.KB_TENANT_ID)
                           .values(kbAccountId.toString(),
                                   kbPaymentExternalKey,
                                   kbPaymentTransactionExternalKey,
                                   transactionType.name(),
                                   amount,
                                   currency.name(),
                                   transactionResults == null ? null : transactionResults.getTransactionId(),
                                   transactionResults == null ? null : transactionResults.getCrossReference(),
                                   transactionResults == null ? null : transactionResults.getRulesTripped(),
                                   transactionResults == null ? null : transactionResults.getTotalScore(),
                                   transactionResults == null ? null : transactionResults.getRecommendationCode(),
                                   transactionResults == null ? null : transactionResults.getRemarks(),
                                   transactionResults == null || transactionResults.getResponseData() == null ? null : asString(transactionResults.getResponseData().getTransaction()),
                                   toTimestamp(utcNow),
                                   kbTenantId.toString())
                           .execute();
                        return null;
                    }
                });
    }

    public List<AccertifyResponsesRecord> getResponses(final String kbPaymentExternalKey, final UUID kbTenantId) throws SQLException {
        return execute(dataSource.getConnection(),
                       new WithConnectionCallback<List<AccertifyResponsesRecord>>() {
                           @Override
                           public List<AccertifyResponsesRecord> withConnection(final Connection conn) throws SQLException {
                               return DSL.using(conn, dialect, settings)
                                         .selectFrom(ACCERTIFY_RESPONSES)
                                         .where(ACCERTIFY_RESPONSES.KB_PAYMENT_EXTERNAL_KEY.equal(kbPaymentExternalKey))
                                         .and(ACCERTIFY_RESPONSES.KB_TENANT_ID.equal(kbTenantId.toString()))
                                         .orderBy(ACCERTIFY_RESPONSES.RECORD_ID.asc())
                                         .fetch();
                           }
                       });
    }
}
