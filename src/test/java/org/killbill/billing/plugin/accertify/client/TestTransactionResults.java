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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class TestTransactionResults {

    @Test(groups = "fast")
    public void testDeserialization() throws Exception {
        final String xml = "<transaction-results>" +
                           "    <transaction-id>aa</transaction-id>" +
                           "    <cross-reference>bb-cc-dd-ee</cross-reference>" +
                           "    <rules-tripped>foo bar baz</rules-tripped>" +
                           "    <total-score>42</total-score>" +
                           "    <recommendation-code>ACCEPT</recommendation-code>" +
                           "    <remarks />" +
                           "    <responseData>" +
                           "      <transaction>" +
                           "        <transaction-details>" +
                           "          <transaction-detail/>" +
                           "        </transaction-details>" +
                           "      </transaction>" +
                           "    </responseData>" +
                           "</transaction-results>";
        final XmlMapper xmlMapper = XmlMapperProvider.get();
        final TransactionResults transactionResults = xmlMapper.readValue(xml, TransactionResults.class);
        Assert.assertEquals(transactionResults.getTransactionId(), "aa");
        Assert.assertEquals(transactionResults.getCrossReference(), "bb-cc-dd-ee");
        Assert.assertEquals(transactionResults.getRulesTripped(), "foo bar baz");
        Assert.assertEquals(transactionResults.getTotalScore(), "42");
        Assert.assertEquals(transactionResults.getRecommendationCode(), "ACCEPT");
        Assert.assertNotNull(transactionResults.getResponseData().getTransaction());
        Assert.assertNotNull(transactionResults.getResponseData().getTransaction().getTransactionDetails());
        Assert.assertNotNull(transactionResults.getResponseData().getTransaction().getTransactionDetails().isEmpty());
    }
}
