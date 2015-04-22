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

package org.killbill.billing.plugin.accertify.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.accertify.core.AccertifyActivator;
import org.killbill.billing.plugin.util.http.HttpClient;
import org.killbill.billing.plugin.util.http.InvalidRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.ning.http.client.Response;

public class AccertifyClient extends HttpClient {

    private static final String DEFAULT_EMPTY_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?><transactions><transactions>";

    public AccertifyClient(final Properties properties) throws GeneralSecurityException {
        super(properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "url"),
              properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "username"),
              properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "password"),
              properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "proxyHost"),
              getIntegerProperty(properties, "proxyPort"),
              getBooleanProperty(properties, "strictSSL"));
    }

    @Override
    protected ObjectMapper createObjectMapper() {
        return XmlMapperProvider.get();
    }

    public TransactionResults assess(@Nullable final String transactions) throws AccertifyClientException {
        final String uri = url;
        final String body = Objects.firstNonNull(transactions, DEFAULT_EMPTY_BODY);

        try {
            return doCall(POST, uri, body, DEFAULT_OPTIONS, TransactionResults.class);
        } catch (final InterruptedException e) {
            throw new AccertifyClientException(e);
        } catch (final ExecutionException e) {
            throw new AccertifyClientException(e);
        } catch (final TimeoutException e) {
            throw new AccertifyClientException(e);
        } catch (final IOException e) {
            throw new AccertifyClientException(e);
        } catch (final URISyntaxException e) {
            throw new AccertifyClientException(e);
        } catch (final InvalidRequest e) {
            throw new AccertifyClientException(e);
        }
    }

    @Override
    protected <T> T deserializeResponse(final Response response, final Class<T> clazz) throws IOException {
        final String body = response.getResponseBody();

        try {
            return mapper.readValue(body, clazz);
        } catch (final JsonProcessingException e) {
            final ErrorResponse errorResponse = mapper.readValue(body, ErrorResponse.class);
            throw new IOException("Accertify returned an error: " + errorResponse.getMessage());
        }
    }

    private static Integer getIntegerProperty(final Properties properties, final String key) {
        final String property = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + key);
        return Strings.isNullOrEmpty(property) ? null : Integer.valueOf(property);
    }

    private static Boolean getBooleanProperty(final Properties properties, final String key) {
        final String property = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + key);
        return Strings.isNullOrEmpty(property) ? true : Boolean.valueOf(property);
    }
}
