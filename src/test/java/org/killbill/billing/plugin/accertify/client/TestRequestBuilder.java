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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestRequestBuilder {

    private RequestBuilder requestBuilder;

    @BeforeMethod(groups = "fast")
    public void setUp() throws Exception {
        requestBuilder = new RequestBuilder();
    }

    @Test(groups = "fast")
    public void testEmptyBuilder() throws AccertifyClientException {
        final String xml = requestBuilder.build();
        Assert.assertEquals(xml, "<transactions/>", xml);
    }

    @Test(groups = "fast")
    public void testSimpleBuilder() throws AccertifyClientException {
        requestBuilder.addTransactionEntry("ipAddress", "127.0.0.1");
        requestBuilder.addTransactionEntry("transactionType", "CAPTURE");
        requestBuilder.addTransactionEntry("totalAmount", "12.44");
        requestBuilder.addTransactionEntry("totalAmountCurrency", "USD");

        final String xml = requestBuilder.build();
        Assert.assertEquals(xml, "<transactions>" +
                                 "<transaction>" +
                                 "<ipAddress>127.0.0.1</ipAddress>" +
                                 "<totalAmount>12.44</totalAmount>" +
                                 "<totalAmountCurrency>USD</totalAmountCurrency>" +
                                 "<transactionType>CAPTURE</transactionType>" +
                                 "</transaction>" +
                                 "</transactions>", xml);
    }

    @Test(groups = "fast")
    public void testOneLevelBuilder() throws AccertifyClientException {
        requestBuilder.addTransactionEntry("ipAddress", "127.0.0.1");
        requestBuilder.addTransactionEntry("customerInformation->userId", "12345");
        requestBuilder.addTransactionEntry("customerInformation->userLastName", "Doe");
        requestBuilder.addTransactionEntry("collectionTransaction->cardDetails->billingFirstName", "John");
        requestBuilder.addTransactionEntry("collectionTransaction->cardDetails->billingLastName", "Doe");

        final String xml = requestBuilder.build();
        Assert.assertEquals(xml, "<transactions>" +
                                 "<transaction>" +
                                 "<collectionTransaction>" +
                                 "<cardDetails>" +
                                 "<billingFirstName>John</billingFirstName>" +
                                 "<billingLastName>Doe</billingLastName>" +
                                 "</cardDetails>" +
                                 "</collectionTransaction>" +
                                 "<customerInformation>" +
                                 "<userId>12345</userId>" +
                                 "<userLastName>Doe</userLastName>" +
                                 "</customerInformation>" +
                                 "<ipAddress>127.0.0.1</ipAddress>" +
                                 "</transaction>" +
                                 "</transactions>", xml);
    }

    @Test(groups = "fast")
    public void testOneLevelListsBuilder() throws AccertifyClientException {
        requestBuilder.addTransactionEntry("ipAddress", "127.0.0.1");
        requestBuilder.addTransactionEntry("orderDetails->orderDetail[1]->shippingName", "John Doe");
        requestBuilder.addTransactionEntry("orderDetails->orderDetail[0]->shippingName", "Jane Doe");

        final String xml = requestBuilder.build();
        Assert.assertEquals(xml, "<transactions>" +
                                 "<transaction>" +
                                 "<ipAddress>127.0.0.1</ipAddress>" +
                                 "<orderDetails>" +
                                 "<orderDetail>" +
                                 "<shippingName>Jane Doe</shippingName>" +
                                 "</orderDetail>" +
                                 "<orderDetail>" +
                                 "<shippingName>John Doe</shippingName>" +
                                 "</orderDetail>" +
                                 "</orderDetails>" +
                                 "</transaction>" +
                                 "</transactions>", xml);
    }
}
