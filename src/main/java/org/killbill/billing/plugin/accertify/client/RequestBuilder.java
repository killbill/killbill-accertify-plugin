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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

public class RequestBuilder {

    private static final Pattern SUBTREE_LIST_PATTERN = Pattern.compile("(.*)\\[([0-9]+)\\]");

    private final ObjectMapper mapper;
    private final Transactions transactions;

    public RequestBuilder() {
        this.mapper = XmlMapperProvider.get();
        this.transactions = new Transactions();
    }

    public RequestBuilder addTransactionEntry(final String entryKey, @Nullable final Object entryValue) {
        // entryKey is something like ipAddress or collectionTransaction->billingFirstName or orderDetails->orderDetail[1]->shippingName
        final String[] hierarchy = entryKey.split("->");
        putDataInSubTree(hierarchy, entryValue, getOrCreateTransaction());
        return this;
    }

    private void putDataInSubTree(final String[] hierarchy, final Object entryValue, final Map<String, Object> tree) {
        Preconditions.checkState(hierarchy.length >= 1, "Invalid hierarchy " + Arrays.toString(hierarchy) + " for value " + entryValue + " and tree " + tree);
        if (hierarchy.length == 1) {
            tree.put(hierarchy[0], entryValue);
            return;
        }

        final Map<String, Object> subTree;
        final String[] subHierarchy;

        final Matcher matcher = SUBTREE_LIST_PATTERN.matcher(hierarchy[1]);
        if (matcher.matches()) {
            final String subTreeRootElement = hierarchy[0];
            final Integer childNumber = Integer.parseInt(matcher.group(2));
            Preconditions.checkNotNull(subTreeRootElement, "Invalid hierarchy " + Arrays.toString(hierarchy) + " for value " + entryValue + " and tree " + tree);
            Preconditions.checkNotNull(childNumber, "Invalid hierarchy " + Arrays.toString(hierarchy) + " for value " + entryValue + " and tree " + tree);

            if (tree.get(subTreeRootElement) == null) {
                tree.put(subTreeRootElement, new RequestDataCollection(subTreeRootElement));
            }
            final List<Map<String, Object>> children = (RequestDataCollection) tree.get(subTreeRootElement);
            // Populate the missing children if inserted out-of-order
            if (childNumber >= children.size()) {
                for (int i = 0; i <= childNumber; i++) {
                    children.add(new LinkedHashMap<String, Object>());
                }
            }

            subTree = children.get(childNumber);
            subHierarchy = new String[hierarchy.length - 1];
            System.arraycopy(hierarchy, 1, subHierarchy, 0, hierarchy.length - 1);
            subHierarchy[0] = matcher.group(1);
        } else {
            final String subTreeRootElement = hierarchy[0];
            if (tree.get(subTreeRootElement) == null) {
                tree.put(subTreeRootElement, new LinkedHashMap<String, Object>());
            }

            subTree = (Map<String, Object>) tree.get(subTreeRootElement);
            subHierarchy = new String[hierarchy.length - 1];
            System.arraycopy(hierarchy, 1, subHierarchy, 0, hierarchy.length - 1);
        }

        putDataInSubTree(subHierarchy, entryValue, subTree);
    }

    private Map<String, Object> getOrCreateTransaction() {
        // Support a single transaction for now
        if (this.transactions.getTransactions().isEmpty()) {
            this.transactions.getTransactions().add(new Transaction());
        }
        return this.transactions.getTransactions().get(0);
    }

    public String build() throws AccertifyClientException {
        try {
            return mapper.writeValueAsString(transactions);
        } catch (final JsonProcessingException e) {
            throw new AccertifyClientException("Invalid XML", e);
        }
    }
}
