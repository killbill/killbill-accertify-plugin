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

package org.killbill.billing.plugin.accertify.client;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class TestTransactions {

    @Test(groups = "fast")
    public void testSerialization() throws JsonProcessingException {
        final Transaction transaction1 = new Transaction();
        transaction1.putAll(ImmutableMap.<String, Object>of("transactionID", "1", "totalAmount", "10"));
        final Transaction transaction2 = new Transaction();
        transaction2.putAll(ImmutableMap.<String, Object>of("transactionID", "2", "totalAmount", "15"));

        final Transactions transactions = new Transactions();
        transactions.getTransactions().add(transaction1);
        transactions.getTransactions().add(transaction2);

        final ObjectMapper xmlWriter = XmlMapperProvider.get();
        final String xml = xmlWriter.writeValueAsString(transactions);
        Assert.assertEquals(xml, "<transactions>" +
                                 "<transaction>" +
                                 "<totalAmount>10</totalAmount>" +
                                 "<transactionID>1</transactionID>" +
                                 "</transaction>" +
                                 "<transaction>" +
                                 "<totalAmount>15</totalAmount>" +
                                 "<transactionID>2</transactionID>" +
                                 "</transaction>" +
                                 "</transactions>", xml);
    }
}
