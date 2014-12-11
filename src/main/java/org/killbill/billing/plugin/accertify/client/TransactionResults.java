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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "transaction-results")
public class TransactionResults {

    @JacksonXmlProperty(localName = "transaction-id")
    private String transactionId;
    @JacksonXmlProperty(localName = "cross-reference")
    private String crossReference;
    @JacksonXmlProperty(localName = "rules-tripped")
    private String rulesTripped;
    @JacksonXmlProperty(localName = "total-score")
    private String totalScore;
    @JacksonXmlProperty(localName = "recommendation-code")
    private String recommendationCode;
    @JacksonXmlProperty(localName = "remarks")
    private String remarks;
    @JacksonXmlProperty(localName = "responseData")
    private ResponseData responseData;

    public String getTransactionId() {
        return transactionId;
    }

    public String getCrossReference() {
        return crossReference;
    }

    public String getRulesTripped() {
        return rulesTripped;
    }

    public String getTotalScore() {
        return totalScore;
    }

    public String getRecommendationCode() {
        return recommendationCode;
    }

    public String getRemarks() {
        return remarks;
    }

    public ResponseData getResponseData() {
        return responseData;
    }
}
