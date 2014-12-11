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

public class TestResponseData {

    @Test(groups = "fast")
    public void testDeserialization() throws Exception {
        final String xml = "<responseData>" +
                           "  <transaction>" +
                           "    <transaction-details>" +
                           "      <transaction-detail/>" +
                           "    </transaction-details>" +
                           "  </transaction>" +
                           "</responseData>";
        final XmlMapper xmlMapper = XmlMapperProvider.get();
        final ResponseData responseData = xmlMapper.readValue(xml, ResponseData.class);
        Assert.assertNotNull(responseData.getTransaction());
        Assert.assertNotNull(responseData.getTransaction().getTransactionDetails());
        Assert.assertNotNull(responseData.getTransaction().getTransactionDetails().isEmpty());
    }
}
