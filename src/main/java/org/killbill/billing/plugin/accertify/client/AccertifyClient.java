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
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.killbill.billing.plugin.accertify.core.AccertifyActivator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;
import com.ning.http.client.Response;

public class AccertifyClient {

    private static final String APPLICATION_XML = "application/xml";
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String HEAD = "HEAD";
    private static final String OPTIONS = "OPTIONS";

    private static final String USER_AGENT = "KillBill/1.0";

    private static final ImmutableMap<String, String> DEFAULT_OPTIONS = ImmutableMap.<String, String>of();
    private static final int DEFAULT_HTTP_TIMEOUT_SEC = 10;

    private final String url;
    private final String username;
    private final String password;
    private final String proxyHost;
    private final Integer proxyPort;

    private XmlMapper mapper;
    private AsyncHttpClient httpClient;

    public AccertifyClient(final Properties properties) {
        this.url = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "url");
        this.username = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "username");
        this.password = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "password");
        this.proxyHost = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "proxyHost");

        final String proxyPortString = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "proxyPort");
        this.proxyPort = Strings.isNullOrEmpty(proxyPortString) ? null : Integer.valueOf(proxyPortString);

        final String strictSSLString = properties.getProperty(AccertifyActivator.PROPERTY_PREFIX + "strictSSL");
        final Boolean strictSSL = Strings.isNullOrEmpty(strictSSLString) ? true : Boolean.valueOf(strictSSLString);

        initialize(strictSSL);
    }

    public AccertifyClient(final String url,
                           final String username,
                           final String password,
                           final String proxyHost,
                           final Integer proxyPort,
                           final Boolean strictSSL) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;

        initialize(strictSSL);
    }

    private void initialize(Boolean strictSSL) {
        this.mapper = XmlMapperProvider.get();

        final AsyncHttpClientConfig.Builder cfg = new AsyncHttpClientConfig.Builder();
        cfg.setUserAgent(USER_AGENT);
        if (!strictSSL) {
            cfg.setSSLContext(createSSLContext());
        }
        this.httpClient = new AsyncHttpClient(cfg.build());
    }

    public TransactionResults assess(final String transactions) throws AccertifyClientException {
        final String uri = url;

        return doCall(POST, uri, transactions, DEFAULT_OPTIONS, TransactionResults.class);
    }

    private <T> T doCall(final String verb, final String uri, final String body, final Map<String, String> options, final Class<T> clazz) throws AccertifyClientException {
        final String url;
        try {
            url = getAccertifyUrl(this.url, uri);
        } catch (final URISyntaxException e) {
            throw new AccertifyClientException("Invalid url", e);
        }

        final AsyncHttpClient.BoundRequestBuilder builder = getBuilderWithHeaderAndQuery(verb, url, options);
        if (!GET.equals(verb) && !HEAD.equals(verb)) {
            if (body != null) {
                builder.setBody(body);
            } else {
                builder.setBody("<?xml version=\"1.0\" encoding=\"utf-8\"?><transactions><transactions>");
            }
        }
        try {
            return executeAndWait(builder, DEFAULT_HTTP_TIMEOUT_SEC, clazz);
        } catch (final IOException e) {
            throw new AccertifyClientException(e);
        }
    }

    private <T> T executeAndWait(final AsyncHttpClient.BoundRequestBuilder builder, final int timeoutSec, final Class<T> clazz) throws AccertifyClientException, IOException {
        final Response response;
        final ListenableFuture<Response> futureStatus;
        try {
            futureStatus = builder.execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(final Response response) throws Exception {
                    return response;
                }
            });
            response = futureStatus.get(timeoutSec, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            throw new AccertifyClientException(e);
        } catch (final ExecutionException e) {
            throw new AccertifyClientException(e);
        } catch (final TimeoutException e) {
            throw new AccertifyClientException(e);
        }

        if (response != null && response.getStatusCode() == 401) {
            throw new AccertifyClientException("Unauthorized request: " + response.getResponseBody());
        } else if (response != null && response.getStatusCode() >= 400) {
            throw new AccertifyClientException("Invalid request: " + response.getResponseBody());
        }

        return deserializeResponse(response, clazz);
    }

    private <T> T deserializeResponse(final Response response, final Class<T> clazz) throws AccertifyClientException, IOException {
        final String body = response.getResponseBody();

        try {
            return mapper.readValue(body, clazz);
        } catch (final JsonProcessingException e) {
            final ErrorResponse errorResponse = mapper.readValue(body, ErrorResponse.class);
            throw new AccertifyClientException("Accertify returned an error: " + errorResponse.getMessage());
        }
    }

    private AsyncHttpClient.BoundRequestBuilder getBuilderWithHeaderAndQuery(final String verb, final String url, final Map<String, String> options) {
        final AsyncHttpClient.BoundRequestBuilder builder;

        if (GET.equals(verb)) {
            builder = httpClient.prepareGet(url);
        } else if (POST.equals(verb)) {
            builder = httpClient.preparePost(url);
        } else if (PUT.equals(verb)) {
            builder = httpClient.preparePut(url);
        } else if (DELETE.equals(verb)) {
            builder = httpClient.prepareDelete(url);
        } else if (HEAD.equals(verb)) {
            builder = httpClient.prepareHead(url);
        } else if (OPTIONS.equals(verb)) {
            builder = httpClient.prepareOptions(url);
        } else {
            throw new IllegalArgumentException("Unrecognized verb: " + verb);
        }

        if (username != null || password != null) {
            final Realm.RealmBuilder realm = new Realm.RealmBuilder();
            if (username != null) {
                realm.setPrincipal(username);
            }
            if (password != null) {
                realm.setPassword(password);
            }
            // Unclear why it's now needed
            realm.setUsePreemptiveAuth(true);
            realm.setScheme(Realm.AuthScheme.BASIC);
            builder.setRealm(realm.build());
        }

        builder.addHeader(HttpHeaders.ACCEPT, APPLICATION_XML);
        builder.addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML + "; charset=utf-8");

        for (final String key : options.keySet()) {
            if (options.get(key) != null) {
                builder.addQueryParam(key, options.get(key));
            }
        }

        if (proxyHost != null && proxyPort != null) {
            final ProxyServer proxyServer = new ProxyServer(proxyHost, proxyPort);
            builder.setProxyServer(proxyServer);
        }

        return builder;
    }

    private String getAccertifyUrl(final String location, final String uri) throws URISyntaxException {
        if (uri == null) {
            throw new URISyntaxException("(null)", "AccertifyClient URL misconfigured");
        }

        final URI u = new URI(uri);
        if (u.isAbsolute()) {
            return uri;
        } else {
            return String.format("%s%s", location, uri);
        }
    }

    private static SSLContext createSSLContext() {
        try {
            final TrustManager[] trustManagers = new TrustManager[]{new DummyTrustManager()};
            final SecureRandom secureRandom = new SecureRandom();

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagers, secureRandom);

            return sslContext;
        } catch (final Exception e) {
            throw new Error("Failed to initialize the server-side SSLContext", e);
        }
    }

    private static class DummyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
