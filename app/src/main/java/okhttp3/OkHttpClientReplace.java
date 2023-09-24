/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import static androidx.media3.common.util.Util.castNonNull;
import static androidx.media3.datasource.HttpUtil.buildRangeRequestHeader;
import static com.lzy.okgo.https.HttpsUtils.UnSafeHostnameVerifier;
import static java.lang.Math.min;
import static okhttp3.internal.Util.checkDuration;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaLibraryInfo;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.Assertions;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSourceException;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.HttpUtil;
import androidx.media3.datasource.TransferListener;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.SSL.InternalSSLSocketFactory;
import com.google.common.base.Predicate;
import com.google.common.net.HttpHeaders;

import org.conscrypt.Conscrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.internal.Util;
import okhttp3.internal.cache.InternalCache;
import okhttp3.internal.platform.Platform;
import okhttp3.internal.proxy.NullProxySelector;
import okhttp3.internal.tls.CertificateChainCleaner;
import okhttp3.internal.tls.OkHostnameVerifier;
import okio.Sink;
import okio.Source;

public class OkHttpClientReplace extends OkHttpClient {

    public static final class Builder {
        final List<Interceptor> interceptors = new ArrayList<>();
        final List<Interceptor> networkInterceptors = new ArrayList<>();
        Dispatcher dispatcher;
        @Nullable
        Proxy proxy;
        List<Protocol> protocols;
        List<ConnectionSpec> connectionSpecs;
        EventListener.Factory eventListenerFactory;
        ProxySelector proxySelector;
        CookieJar cookieJar;
        @Nullable
        Cache cache;
        @Nullable
        InternalCache internalCache;
        SocketFactory socketFactory;
        @Nullable
        SSLSocketFactory sslSocketFactory;
        @Nullable
        CertificateChainCleaner certificateChainCleaner;
        HostnameVerifier hostnameVerifier;
        CertificatePinner certificatePinner;
        Authenticator proxyAuthenticator;
        Authenticator authenticator;
        ConnectionPool connectionPool;
        Dns dns;
        boolean followSslRedirects;
        boolean followRedirects;
        boolean retryOnConnectionFailure;
        int callTimeout;
        int connectTimeout;
        int readTimeout;
        int writeTimeout;
        int pingInterval;

        public Builder() {
            dispatcher = new Dispatcher();
            protocols = DEFAULT_PROTOCOLS;
            connectionSpecs = DEFAULT_CONNECTION_SPECS;
            eventListenerFactory = EventListener.factory(EventListener.NONE);
            proxySelector = ProxySelector.getDefault();
            if (proxySelector == null) {
                proxySelector = new NullProxySelector();
            }
            cookieJar = CookieJar.NO_COOKIES;
            socketFactory = SocketFactory.getDefault();
            hostnameVerifier = OkHostnameVerifier.INSTANCE;
            certificatePinner = CertificatePinner.DEFAULT;
            proxyAuthenticator = Authenticator.NONE;
            authenticator = Authenticator.NONE;
            connectionPool = new ConnectionPool();
            dns = Dns.SYSTEM;
            followSslRedirects = true;
            followRedirects = true;
            retryOnConnectionFailure = true;
            callTimeout = 0;
            connectTimeout = 10_000;
            readTimeout = 10_000;
            writeTimeout = 10_000;
            pingInterval = 0;
        }

        Builder(OkHttpClientReplace okHttpClient) {
            this.dispatcher = okHttpClient.dispatcher;
            this.proxy = okHttpClient.proxy;
            this.protocols = okHttpClient.protocols;
            this.connectionSpecs = okHttpClient.connectionSpecs;
            this.interceptors.addAll(okHttpClient.interceptors);
            this.networkInterceptors.addAll(okHttpClient.networkInterceptors);
            this.eventListenerFactory = okHttpClient.eventListenerFactory;
            this.proxySelector = okHttpClient.proxySelector;
            this.cookieJar = okHttpClient.cookieJar;
            this.internalCache = okHttpClient.internalCache;
            this.cache = okHttpClient.cache;
            this.socketFactory = okHttpClient.socketFactory;
            this.sslSocketFactory = okHttpClient.sslSocketFactory;
            this.certificateChainCleaner = okHttpClient.certificateChainCleaner;
            this.hostnameVerifier = okHttpClient.hostnameVerifier;
            this.certificatePinner = okHttpClient.certificatePinner;
            this.proxyAuthenticator = okHttpClient.proxyAuthenticator;
            this.authenticator = okHttpClient.authenticator;
            this.connectionPool = okHttpClient.connectionPool;
            this.dns = okHttpClient.dns;
            this.followSslRedirects = okHttpClient.followSslRedirects;
            this.followRedirects = okHttpClient.followRedirects;
            this.retryOnConnectionFailure = okHttpClient.retryOnConnectionFailure;
            this.callTimeout = okHttpClient.callTimeout;
            this.connectTimeout = okHttpClient.connectTimeout;
            this.readTimeout = okHttpClient.readTimeout;
            this.writeTimeout = okHttpClient.writeTimeout;
            this.pingInterval = okHttpClient.pingInterval;
        }

        /**
         * Sets the default timeout for complete calls. A value of 0 means no timeout, otherwise values
         * must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
         *
         * <p>The call timeout spans the entire call: resolving DNS, connecting, writing the request
         * body, server processing, and reading the response body. If the call requires redirects or
         * retries all must complete within one timeout period.
         */
        public OkHttpClientReplace.Builder callTimeout(long timeout, TimeUnit unit) {
            callTimeout = checkDuration("timeout", timeout, unit);
            return this;
        }

        /**
         * Sets the default timeout for complete calls. A value of 0 means no timeout, otherwise values
         * must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
         *
         * <p>The call timeout spans the entire call: resolving DNS, connecting, writing the request
         * body, server processing, and reading the response body. If the call requires redirects or
         * retries all must complete within one timeout period.
         */
        @SuppressLint("NewApi")
        public OkHttpClientReplace.Builder callTimeout(Duration duration) {
            callTimeout = checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * Sets the default connect timeout for new connections. A value of 0 means no timeout,
         * otherwise values must be between 1 and {@link Integer#MAX_VALUE} when converted to
         * milliseconds.
         *
         * <p>The connect timeout is applied when connecting a TCP socket to the target host.
         * The default value is 10 seconds.
         */
        public OkHttpClientReplace.Builder connectTimeout(long timeout, TimeUnit unit) {
            connectTimeout = checkDuration("timeout", timeout, unit);
            return this;
        }

        /**
         * Sets the default connect timeout for new connections. A value of 0 means no timeout,
         * otherwise values must be between 1 and {@link Integer#MAX_VALUE} when converted to
         * milliseconds.
         *
         * <p>The connect timeout is applied when connecting a TCP socket to the target host.
         * The default value is 10 seconds.
         */
        @SuppressLint("NewApi")
        public OkHttpClientReplace.Builder connectTimeout(Duration duration) {
            connectTimeout = checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * Sets the default read timeout for new connections. A value of 0 means no timeout, otherwise
         * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
         *
         * <p>The read timeout is applied to both the TCP socket and for individual read IO operations
         * including on {@link Source} of the {@link Response}. The default value is 10 seconds.
         *
         * @see Socket#setSoTimeout(int)
         * @see Source#timeout()
         */
        public OkHttpClientReplace.Builder readTimeout(long timeout, TimeUnit unit) {
            readTimeout = checkDuration("timeout", timeout, unit);
            return this;
        }

        /**
         * Sets the default read timeout for new connections. A value of 0 means no timeout, otherwise
         * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
         *
         * <p>The read timeout is applied to both the TCP socket and for individual read IO operations
         * including on {@link Source} of the {@link Response}. The default value is 10 seconds.
         *
         * @see Socket#setSoTimeout(int)
         * @see Source#timeout()
         */
        @SuppressLint("NewApi")
        public OkHttpClientReplace.Builder readTimeout(Duration duration) {
            readTimeout = checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * Sets the default write timeout for new connections. A value of 0 means no timeout, otherwise
         * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
         *
         * <p>The write timeout is applied for individual write IO operations.
         * The default value is 10 seconds.
         *
         * @see Sink#timeout()
         */
        public OkHttpClientReplace.Builder writeTimeout(long timeout, TimeUnit unit) {
            writeTimeout = checkDuration("timeout", timeout, unit);
            return this;
        }

        /**
         * Sets the default write timeout for new connections. A value of 0 means no timeout, otherwise
         * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
         *
         * <p>The write timeout is applied for individual write IO operations.
         * The default value is 10 seconds.
         *
         * @see Sink#timeout()
         */
        @SuppressLint("NewApi")
        public OkHttpClientReplace.Builder writeTimeout(Duration duration) {
            writeTimeout = checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * Sets the interval between HTTP/2 and web socket pings initiated by this client. Use this to
         * automatically send ping frames until either the connection fails or it is closed. This keeps
         * the connection alive and may detect connectivity failures.
         *
         * <p>If the server does not respond to each ping with a pong within {@code interval}, this
         * client will assume that connectivity has been lost. When this happens on a web socket the
         * connection is canceled and its listener is {@linkplain WebSocketListener#onFailure notified
         * of the failure}. When it happens on an HTTP/2 connection the connection is closed and any
         * calls it is carrying {@linkplain java.io.IOException will fail with an IOException}.
         *
         * <p>The default value of 0 disables client-initiated pings.
         */
        public OkHttpClientReplace.Builder pingInterval(long interval, TimeUnit unit) {
            pingInterval = checkDuration("interval", interval, unit);
            return this;
        }

        /**
         * Sets the interval between HTTP/2 and web socket pings initiated by this client. Use this to
         * automatically send ping frames until either the connection fails or it is closed. This keeps
         * the connection alive and may detect connectivity failures.
         *
         * <p>If the server does not respond to each ping with a pong within {@code interval}, this
         * client will assume that connectivity has been lost. When this happens on a web socket the
         * connection is canceled and its listener is {@linkplain WebSocketListener#onFailure notified
         * of the failure}. When it happens on an HTTP/2 connection the connection is closed and any
         * calls it is carrying {@linkplain java.io.IOException will fail with an IOException}.
         *
         * <p>The default value of 0 disables client-initiated pings.
         */
        @SuppressLint("NewApi")
        public OkHttpClientReplace.Builder pingInterval(Duration duration) {
            pingInterval = checkDuration("timeout", duration.toMillis(), TimeUnit.MILLISECONDS);
            return this;
        }

        /**
         * Sets the HTTP proxy that will be used by connections created by this client. This takes
         * precedence over {@link #proxySelector}, which is only honored when this proxy is null (which
         * it is by default). To disable proxy use completely, call {@code proxy(Proxy.NO_PROXY)}.
         */
        public OkHttpClientReplace.Builder proxy(@Nullable Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Sets the proxy selection policy to be used if no {@link #proxy proxy} is specified
         * explicitly. The proxy selector may return multiple proxies; in that case they will be tried
         * in sequence until a successful connection is established.
         *
         * <p>If unset, the {@link ProxySelector#getDefault() system-wide default} proxy selector will
         * be used.
         */
        public OkHttpClientReplace.Builder proxySelector(ProxySelector proxySelector) {
            if (proxySelector == null) throw new NullPointerException("proxySelector == null");
            this.proxySelector = proxySelector;
            return this;
        }

        /**
         * Sets the handler that can accept cookies from incoming HTTP responses and provides cookies to
         * outgoing HTTP requests.
         *
         * <p>If unset, {@linkplain CookieJar#NO_COOKIES no cookies} will be accepted nor provided.
         */
        public OkHttpClientReplace.Builder cookieJar(CookieJar cookieJar) {
            if (cookieJar == null) throw new NullPointerException("cookieJar == null");
            this.cookieJar = cookieJar;
            return this;
        }

        /**
         * Sets the response cache to be used to read and write cached responses.
         */
        void setInternalCache(@Nullable InternalCache internalCache) {
            this.internalCache = internalCache;
            this.cache = null;
        }

        /**
         * Sets the response cache to be used to read and write cached responses.
         */
        public OkHttpClientReplace.Builder cache(@Nullable Cache cache) {
            this.cache = cache;
            this.internalCache = null;
            return this;
        }

        /**
         * Sets the DNS service used to lookup IP addresses for hostnames.
         *
         * <p>If unset, the {@link Dns#SYSTEM system-wide default} DNS will be used.
         */
        public OkHttpClientReplace.Builder dns(Dns dns) {
            if (dns == null) throw new NullPointerException("dns == null");
            this.dns = dns;
            return this;
        }

        /**
         * Sets the socket factory used to create connections. OkHttp only uses the parameterless {@link
         * SocketFactory#createSocket() createSocket()} method to create unconnected sockets. Overriding
         * this method, e. g., allows the socket to be bound to a specific local address.
         *
         * <p>If unset, the {@link SocketFactory#getDefault() system-wide default} socket factory will
         * be used.
         */
        public OkHttpClientReplace.Builder socketFactory(SocketFactory socketFactory) {
            if (socketFactory == null) throw new NullPointerException("socketFactory == null");
            this.socketFactory = socketFactory;
            return this;
        }

        /**
         * Sets the socket factory used to secure HTTPS connections. If unset, the system default will
         * be used.
         *
         * @deprecated {@code SSLSocketFactory} does not expose its {@link X509TrustManager}, which is
         * a field that OkHttp needs to build a clean certificate chain. This method instead must
         * use reflection to extract the trust manager. Applications should prefer to call {@link
         * #sslSocketFactory(SSLSocketFactory, X509TrustManager)}, which avoids such reflection.
         */
        public OkHttpClientReplace.Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if (sslSocketFactory == null)
                throw new NullPointerException("sslSocketFactory == null");
            this.sslSocketFactory = sslSocketFactory;
            this.certificateChainCleaner = Platform.get().buildCertificateChainCleaner(sslSocketFactory);
            return this;
        }

        /**
         * Sets the socket factory and trust manager used to secure HTTPS connections. If unset, the
         * system defaults will be used.
         *
         * <p>Most applications should not call this method, and instead use the system defaults. Those
         * classes include special optimizations that can be lost if the implementations are decorated.
         *
         * <p>If necessary, you can create and configure the defaults yourself with the following code:
         *
         * <pre>   {@code
         *
         *   TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
         *       TrustManagerFactory.getDefaultAlgorithm());
         *   trustManagerFactory.init((KeyStore) null);
         *   TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
         *   if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
         *     throw new IllegalStateException("Unexpected default trust managers:"
         *         + Arrays.toString(trustManagers));
         *   }
         *   X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
         *
         *   SSLContext sslContext = SSLContext.getInstance("TLS");
         *   sslContext.init(null, new TrustManager[] { trustManager }, null);
         *   SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
         *
         *   OkHttpCliens client = new OkHttpCliens.Builder()
         *       .sslSocketFactory(sslSocketFactory, trustManager)
         *       .build();
         * }</pre>
         */
        public OkHttpClientReplace.Builder sslSocketFactory(
                SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
            if (sslSocketFactory == null)
                throw new NullPointerException("sslSocketFactory == null");
            if (trustManager == null) throw new NullPointerException("trustManager == null");
            this.sslSocketFactory = sslSocketFactory;
            this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
            return this;
        }

        /**
         * Sets the verifier used to confirm that response certificates apply to requested hostnames for
         * HTTPS connections.
         *
         * <p>If unset, a default hostname verifier will be used.
         */
        public OkHttpClientReplace.Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier == null)
                throw new NullPointerException("hostnameVerifier == null");
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        /**
         * Sets the certificate pinner that constrains which certificates are trusted. By default HTTPS
         * connections rely on only the {@link #sslSocketFactory SSL socket factory} to establish trust.
         * Pinning certificates avoids the need to trust certificate authorities.
         */
        public OkHttpClientReplace.Builder certificatePinner(CertificatePinner certificatePinner) {
            if (certificatePinner == null)
                throw new NullPointerException("certificatePinner == null");
            this.certificatePinner = certificatePinner;
            return this;
        }

        /**
         * Sets the authenticator used to respond to challenges from origin servers. Use {@link
         * #proxyAuthenticator} to set the authenticator for proxy servers.
         *
         * <p>If unset, the {@linkplain Authenticator#NONE no authentication will be attempted}.
         */
        public OkHttpClientReplace.Builder authenticator(Authenticator authenticator) {
            if (authenticator == null) throw new NullPointerException("authenticator == null");
            this.authenticator = authenticator;
            return this;
        }

        /**
         * Sets the authenticator used to respond to challenges from proxy servers. Use {@link
         * #authenticator} to set the authenticator for origin servers.
         *
         * <p>If unset, the {@linkplain Authenticator#NONE no authentication will be attempted}.
         */
        public OkHttpClientReplace.Builder proxyAuthenticator(Authenticator proxyAuthenticator) {
            if (proxyAuthenticator == null)
                throw new NullPointerException("proxyAuthenticator == null");
            this.proxyAuthenticator = proxyAuthenticator;
            return this;
        }

        /**
         * Sets the connection pool used to recycle HTTP and HTTPS connections.
         *
         * <p>If unset, a new connection pool will be used.
         */
        public OkHttpClientReplace.Builder connectionPool(ConnectionPool connectionPool) {
            if (connectionPool == null) throw new NullPointerException("connectionPool == null");
            this.connectionPool = connectionPool;
            return this;
        }

        /**
         * Configure this client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
         *
         * <p>If unset, protocol redirects will be followed. This is different than the built-in {@code
         * HttpURLConnection}'s default.
         */
        public OkHttpClientReplace.Builder followSslRedirects(boolean followProtocolRedirects) {
            this.followSslRedirects = followProtocolRedirects;
            return this;
        }

        /**
         * Configure this client to follow redirects. If unset, redirects will be followed.
         */
        public OkHttpClientReplace.Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * Configure this client to retry or not when a connectivity problem is encountered. By default,
         * this client silently recovers from the following problems:
         *
         * <ul>
         *   <li><strong>Unreachable IP addresses.</strong> If the URL's host has multiple IP addresses,
         *       failure to reach any individual IP address doesn't fail the overall request. This can
         *       increase availability of multi-homed services.
         *   <li><strong>Stale pooled connections.</strong> The {@link ConnectionPool} reuses sockets
         *       to decrease request latency, but these connections will occasionally time out.
         *   <li><strong>Unreachable proxy servers.</strong> A {@link ProxySelector} can be used to
         *       attempt multiple proxy servers in sequence, eventually falling back to a direct
         *       connection.
         * </ul>
         * <p>
         * Set this to false to avoid retrying requests when doing so is destructive. In this case the
         * calling application should do its own recovery of connectivity failures.
         */
        public OkHttpClientReplace.Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
            this.retryOnConnectionFailure = retryOnConnectionFailure;
            return this;
        }

        /**
         * Sets the dispatcher used to set policy and execute asynchronous requests. Must not be null.
         */
        public OkHttpClientReplace.Builder dispatcher(Dispatcher dispatcher) {
            if (dispatcher == null) throw new IllegalArgumentException("dispatcher == null");
            this.dispatcher = dispatcher;
            return this;
        }

        /**
         * Configure the protocols used by this client to communicate with remote servers. By default
         * this client will prefer the most efficient transport available, falling back to more
         * ubiquitous protocols. Applications should only call this method to avoid specific
         * compatibility problems, such as web servers that behave incorrectly when HTTP/2 is enabled.
         *
         * <p>The following protocols are currently supported:
         *
         * <ul>
         *     <li><a href="http://www.w3.org/Protocols/rfc2616/rfc2616.html">http/1.1</a>
         *     <li><a href="https://tools.ietf.org/html/rfc7540">h2</a>
         *     <li><a href="https://tools.ietf.org/html/rfc7540#section-3.4">h2 with prior knowledge
         *         (cleartext only)</a>
         * </ul>
         *
         * <p><strong>This is an evolving set.</strong> Future releases include support for transitional
         * protocols. The http/1.1 transport will never be dropped.
         *
         * <p>If multiple protocols are specified, <a
         * href="http://tools.ietf.org/html/draft-ietf-tls-applayerprotoneg">ALPN</a> will be used to
         * negotiate a transport. Protocol negotiation is only attempted for HTTPS URLs.
         *
         * <p>{@link Protocol#HTTP_1_0} is not supported in this set. Requests are initiated with {@code
         * HTTP/1.1}. If the server responds with {@code HTTP/1.0}, that will be exposed by {@link
         * Response#protocol()}.
         *
         * @param protocols the protocols to use, in order of preference. If the list contains {@link
         *                  Protocol#H2_PRIOR_KNOWLEDGE} then that must be the only protocol and HTTPS URLs will not
         *                  be supported. Otherwise the list must contain {@link Protocol#HTTP_1_1}. The list must
         *                  not contain null or {@link Protocol#HTTP_1_0}.
         */
        public OkHttpClientReplace.Builder protocols(List<Protocol> protocols) {
            // Create a private copy of the list.
            protocols = new ArrayList<>(protocols);

            // Validate that the list has everything we require and nothing we forbid.
            if (!protocols.contains(Protocol.H2_PRIOR_KNOWLEDGE)
                    && !protocols.contains(Protocol.HTTP_1_1)) {
                throw new IllegalArgumentException(
                        "protocols must contain h2_prior_knowledge or http/1.1: " + protocols);
            }
            if (protocols.contains(Protocol.H2_PRIOR_KNOWLEDGE) && protocols.size() > 1) {
                throw new IllegalArgumentException(
                        "protocols containing h2_prior_knowledge cannot use other protocols: " + protocols);
            }
            if (protocols.contains(Protocol.HTTP_1_0)) {
                throw new IllegalArgumentException("protocols must not contain http/1.0: " + protocols);
            }
            if (protocols.contains(null)) {
                throw new IllegalArgumentException("protocols must not contain null");
            }

            // Remove protocols that we no longer support.
            protocols.remove(Protocol.SPDY_3);

            // Assign as an unmodifiable list. This is effectively immutable.
            this.protocols = Collections.unmodifiableList(protocols);
            return this;
        }

        public OkHttpClientReplace.Builder connectionSpecs(List<ConnectionSpec> connectionSpecs) {
            this.connectionSpecs = Util.immutableList(connectionSpecs);
            return this;
        }

        /**
         * Returns a modifiable list of interceptors that observe the full span of each call: from
         * before the connection is established (if any) until after the response source is selected
         * (either the origin server, cache, or both).
         */
        public List<Interceptor> interceptors() {
            return interceptors;
        }

        public OkHttpClientReplace.Builder addInterceptor(Interceptor interceptor) {
            if (interceptor == null) throw new IllegalArgumentException("interceptor == null");
            interceptors.add(interceptor);
            return this;
        }

        /**
         * Returns a modifiable list of interceptors that observe a single network request and response.
         * These interceptors must call {@link Interceptor.Chain#proceed} exactly once: it is an error
         * for a network interceptor to short-circuit or repeat a network request.
         */
        public List<Interceptor> networkInterceptors() {
            return networkInterceptors;
        }

        public OkHttpClientReplace.Builder addNetworkInterceptor(Interceptor interceptor) {
            if (interceptor == null) throw new IllegalArgumentException("interceptor == null");
            networkInterceptors.add(interceptor);
            return this;
        }

        /**
         * Configure a single client scoped listener that will receive all analytic events
         * for this client.
         *
         * @see EventListener for semantics and restrictions on listener implementations.
         */
        public OkHttpClientReplace.Builder eventListener(EventListener eventListener) {
            if (eventListener == null) throw new NullPointerException("eventListener == null");
            this.eventListenerFactory = EventListener.factory(eventListener);
            return this;
        }

        /**
         * Configure a factory to provide per-call scoped listeners that will receive analytic events
         * for this client.
         *
         * @see EventListener for semantics and restrictions on listener implementations.
         */
        public OkHttpClientReplace.Builder eventListenerFactory(EventListener.Factory eventListenerFactory) {
            if (eventListenerFactory == null) {
                throw new NullPointerException("eventListenerFactory == null");
            }
            this.eventListenerFactory = eventListenerFactory;
            return this;
        }

        //这个函数编译成smali代码后替换到okhttpclient里。然后放到res/raw/okhttp_inject.dex
        public OkHttpClientReplace build() {
            hostnameVerifier = UnSafeHostnameVerifier;
            try {
                X509TrustManager tm = Conscrypt.getDefaultX509TrustManager();
                SSLContext sslContext = SSLContext.getInstance("TLS", App.conscrypt);
                sslContext.init(null, new X509TrustManager[]{tm}, null);
                sslSocketFactory(new InternalSSLSocketFactory(sslContext.getSocketFactory()), tm);
                //没有全局加上OkHttpClient dns选项
                //这里实际代码是直接return new OkHttpClient(this);
                OkHttpClient okHttpClient = new OkHttpClient();
                okHttpClient.toString();
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    @UnstableApi
    /**
     * An {@link HttpDataSource} that delegates to Square's {@link Call.Factory}.
     *
     * <p>Note: HTTP request headers will be set using all parameters passed via (in order of decreasing
     * priority) the {@code dataSpec}, {@link #setRequestProperty} and the default parameters used to
     * construct the instance.
     */
    public static class OkHttpDataSource extends BaseDataSource implements HttpDataSource {

        static {
            MediaLibraryInfo.registerModule("goog.exo.okhttp");
        }

        private final Call.Factory callFactory;
        private final RequestProperties requestProperties;
        @Nullable
        private final String userAgent;
        @Nullable
        private final CacheControl cacheControl;
        @Nullable
        private final RequestProperties defaultRequestProperties;
        @Nullable
        private Predicate<String> contentTypePredicate;
        @Nullable
        private DataSpec dataSpec;
        @Nullable
        private Response response;
        @Nullable
        private InputStream responseByteStream;
        private boolean opened;
        private long bytesToRead;
        private long bytesRead;

        /**
         * @deprecated Use {@link Factory} instead.
         */
        @SuppressWarnings("deprecation")
        @Deprecated
        public OkHttpDataSource(Call.Factory callFactory) {
            this(callFactory, /* userAgent= */ null);
        }

        /**
         * @deprecated Use {@link Factory} instead.
         */
        @SuppressWarnings("deprecation")
        @Deprecated
        public OkHttpDataSource(Call.Factory callFactory, @Nullable String userAgent) {
            this(callFactory, userAgent, /* cacheControl= */ null, /* defaultRequestProperties= */ null);
        }

        /**
         * @deprecated Use {@link Factory} instead.
         */
        @Deprecated
        public OkHttpDataSource(
                Call.Factory callFactory,
                @Nullable String userAgent,
                @Nullable CacheControl cacheControl,
                @Nullable RequestProperties defaultRequestProperties) {
            this(
                    callFactory,
                    userAgent,
                    cacheControl,
                    defaultRequestProperties,
                    /* contentTypePredicate= */ null);
        }

        private @OptIn(markerClass = UnstableApi.class) OkHttpDataSource(
                Call.Factory callFactory,
                @Nullable String userAgent,
                @Nullable CacheControl cacheControl,
                @Nullable RequestProperties defaultRequestProperties,
                @Nullable Predicate<String> contentTypePredicate) {
            super(/* isNetwork= */ true);
            this.callFactory = Assertions.checkNotNull(callFactory);
            this.userAgent = userAgent;
            this.cacheControl = cacheControl;
            this.defaultRequestProperties = defaultRequestProperties;
            this.contentTypePredicate = contentTypePredicate;
            this.requestProperties = new RequestProperties();
        }

        /**
         * @deprecated Use {@link Factory#setContentTypePredicate(Predicate)} instead.
         */
        @Deprecated
        public void setContentTypePredicate(@Nullable Predicate<String> contentTypePredicate) {
            this.contentTypePredicate = contentTypePredicate;
        }

        @Override
        @Nullable
        public Uri getUri() {
            return response == null ? null : Uri.parse(response.request().url().toString());
        }

        @Override
        public int getResponseCode() {
            return response == null ? -1 : response.code();
        }

        @Override
        public Map<String, List<String>> getResponseHeaders() {
            return response == null ? Collections.emptyMap() : response.headers().toMultimap();
        }

        @Override
        public void setRequestProperty(String name, String value) {
            Assertions.checkNotNull(name);
            Assertions.checkNotNull(value);
            requestProperties.set(name, value);
        }

        @Override
        public void clearRequestProperty(String name) {
            Assertions.checkNotNull(name);
            requestProperties.remove(name);
        }

        @Override
        public void clearAllRequestProperties() {
            requestProperties.clear();
        }

        @Override
        public long open(DataSpec dataSpec) throws HttpDataSourceException {
            this.dataSpec = dataSpec;
            bytesRead = 0;
            bytesToRead = 0;
            transferInitializing(dataSpec);

            Request request = makeRequest(dataSpec);
            Response response;
            ResponseBody responseBody;
            try {
                this.response = callFactory.newCall(request).execute();
                response = this.response;
                responseBody = Assertions.checkNotNull(response.body());
                responseByteStream = responseBody.byteStream();
            } catch (IOException e) {
                throw HttpDataSourceException.createForIOException(
                        e, dataSpec, HttpDataSourceException.TYPE_OPEN);
            }

            int responseCode = response.code();

            // Check for a valid response code.
            if (!response.isSuccessful()) {
                if (responseCode == 416) {
                    long documentSize =
                            HttpUtil.getDocumentSize(response.headers().get(HttpHeaders.CONTENT_RANGE));
                    if (dataSpec.position == documentSize) {
                        opened = true;
                        transferStarted(dataSpec);
                        return dataSpec.length != C.LENGTH_UNSET ? dataSpec.length : 0;
                    }
                }

                byte[] errorResponseBody;
                try {
                    errorResponseBody = androidx.media3.common.util.Util.toByteArray(Assertions.checkNotNull(responseByteStream));
                } catch (IOException e) {
                    errorResponseBody = androidx.media3.common.util.Util.EMPTY_BYTE_ARRAY;
                }
                Map<String, List<String>> headers = response.headers().toMultimap();
                closeConnectionQuietly();
                @Nullable
                IOException cause =
                        responseCode == 416
                                ? new DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
                                : null;
                throw new InvalidResponseCodeException(
                        responseCode, response.message(), cause, headers, dataSpec, errorResponseBody);
            }

            // Check for a valid content type.
            @Nullable MediaType mediaType = responseBody.contentType();
            String contentType = mediaType != null ? mediaType.toString() : "";
            if (contentTypePredicate != null && !contentTypePredicate.apply(contentType)) {
                closeConnectionQuietly();
                throw new InvalidContentTypeException(contentType, dataSpec);
            }

            // If we requested a range starting from a non-zero position and received a 200 rather than a
            // 206, then the server does not support partial requests. We'll need to manually skip to the
            // requested position.
            long bytesToSkip = responseCode == 200 && dataSpec.position != 0 ? dataSpec.position : 0;

            // Determine the length of the data to be read, after skipping.
            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesToRead = dataSpec.length;
            } else {
                long contentLength = responseBody.contentLength();
                bytesToRead = contentLength != -1 ? (contentLength - bytesToSkip) : C.LENGTH_UNSET;
            }

            opened = true;
            transferStarted(dataSpec);

            try {
                skipFully(bytesToSkip, dataSpec);
            } catch (HttpDataSourceException e) {
                closeConnectionQuietly();
                throw e;
            }

            return bytesToRead;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws HttpDataSourceException {
            try {
                return readInternal(buffer, offset, length);
            } catch (IOException e) {
                throw HttpDataSourceException.createForIOException(
                        e, castNonNull(dataSpec), HttpDataSourceException.TYPE_READ);
            }
        }

        @Override
        public void close() {
            if (opened) {
                opened = false;
                transferEnded();
                closeConnectionQuietly();
            }
        }

        /**
         * Establishes a connection.
         */
        private Request makeRequest(DataSpec dataSpec) throws HttpDataSourceException {
            long position = dataSpec.position;
            long length = dataSpec.length;

            @Nullable HttpUrl url = HttpUrl.parse(dataSpec.uri.toString());
            if (url == null) {
                throw new HttpDataSourceException(
                        "Malformed URL",
                        dataSpec,
                        PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK,
                        HttpDataSourceException.TYPE_OPEN);
            }

            Request.Builder builder = new Request.Builder().url(url);
            if (cacheControl != null) {
                builder.cacheControl(cacheControl);
            }

            Map<String, String> headers = new HashMap<>();
            if (defaultRequestProperties != null) {
                headers.putAll(defaultRequestProperties.getSnapshot());
            }

            headers.putAll(requestProperties.getSnapshot());
            headers.putAll(dataSpec.httpRequestHeaders);

            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }

            @Nullable String rangeHeader = buildRangeRequestHeader(position, length);
            if (rangeHeader != null) {
                builder.addHeader(HttpHeaders.RANGE, rangeHeader);
            }
            if (userAgent != null) {
                builder.addHeader(HttpHeaders.USER_AGENT, userAgent);
            }
            if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) {
                builder.addHeader(HttpHeaders.ACCEPT_ENCODING, "identity");
            }

            @Nullable RequestBody requestBody = null;
            if (dataSpec.httpBody != null) {
                requestBody = RequestBody.create(null, dataSpec.httpBody);
            } else if (dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST) {
                // OkHttp requires a non-null body for POST requests.
                requestBody = RequestBody.create(null, androidx.media3.common.util.Util.EMPTY_BYTE_ARRAY);
            }
            builder.method(dataSpec.getHttpMethodString(), requestBody);
            return builder.build();
        }

        /**
         * Attempts to skip the specified number of bytes in full.
         *
         * @param bytesToSkip The number of bytes to skip.
         * @param dataSpec    The {@link DataSpec}.
         * @throws HttpDataSourceException If the thread is interrupted during the operation, or an error
         *                                 occurs while reading from the source, or if the data ended before skipping the specified
         *                                 number of bytes.
         */
        private void skipFully(long bytesToSkip, DataSpec dataSpec) throws HttpDataSourceException {
            if (bytesToSkip == 0) {
                return;
            }
            byte[] skipBuffer = new byte[4096];
            try {
                while (bytesToSkip > 0) {
                    int readLength = (int) min(bytesToSkip, skipBuffer.length);
                    int read = castNonNull(responseByteStream).read(skipBuffer, 0, readLength);
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedIOException();
                    }
                    if (read == -1) {
                        throw new HttpDataSourceException(
                                dataSpec,
                                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                                HttpDataSourceException.TYPE_OPEN);
                    }
                    bytesToSkip -= read;
                    bytesTransferred(read);
                }
                return;
            } catch (IOException e) {
                if (e instanceof HttpDataSourceException) {
                    throw (HttpDataSourceException) e;
                } else {
                    throw new HttpDataSourceException(
                            dataSpec,
                            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                            HttpDataSourceException.TYPE_OPEN);
                }
            }
        }

        /**
         * Reads up to {@code length} bytes of data and stores them into {@code buffer}, starting at index
         * {@code offset}.
         *
         * <p>This method blocks until at least one byte of data can be read, the end of the opened range
         * is detected, or an exception is thrown.
         *
         * @param buffer     The buffer into which the read data should be stored.
         * @param offset     The start offset into {@code buffer} at which data should be written.
         * @param readLength The maximum number of bytes to read.
         * @return The number of bytes read, or {@link C#RESULT_END_OF_INPUT} if the end of the opened
         * range is reached.
         * @throws IOException If an error occurs reading from the source.
         */
        private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
            if (readLength == 0) {
                return 0;
            }
            if (bytesToRead != C.LENGTH_UNSET) {
                long bytesRemaining = bytesToRead - bytesRead;
                if (bytesRemaining == 0) {
                    return C.RESULT_END_OF_INPUT;
                }
                readLength = (int) min(readLength, bytesRemaining);
            }

            int read = castNonNull(responseByteStream).read(buffer, offset, readLength);
            if (read == -1) {
                return C.RESULT_END_OF_INPUT;
            }

            bytesRead += read;
            bytesTransferred(read);
            return read;
        }

        /**
         * Closes the current connection quietly, if there is one.
         */
        private void closeConnectionQuietly() {
            if (response != null) {
                Assertions.checkNotNull(response.body()).close();
                response = null;
            }
            responseByteStream = null;
        }

        @UnstableApi
        /** {@link DataSource.Factory} for {@link OkHttpDataSource} instances. */
        public static final class Factory implements HttpDataSource.Factory {

            private final RequestProperties defaultRequestProperties;
            private final Call.Factory callFactory;

            @Nullable
            private String userAgent;
            @Nullable
            private TransferListener transferListener;
            @Nullable
            private CacheControl cacheControl;
            @Nullable
            private Predicate<String> contentTypePredicate;

            /**
             * Creates an instance.
             *
             * @param callFactory A {@link Call.Factory} (typically an {@link OkHttpClient}) for use by the
             *                    sources created by the factory.
             */
            public Factory(Call.Factory callFactory) {
                this.callFactory = callFactory;
                defaultRequestProperties = new RequestProperties();
            }

            @Override
            public final Factory setDefaultRequestProperties(Map<String, String> defaultRequestProperties) {
                this.defaultRequestProperties.clearAndSet(defaultRequestProperties);
                return this;
            }

            /**
             * Sets the user agent that will be used.
             *
             * <p>The default is {@code null}, which causes the default user agent of the underlying {@link
             * OkHttpClient} to be used.
             *
             * @param userAgent The user agent that will be used, or {@code null} to use the default user
             *                  agent of the underlying {@link OkHttpClient}.
             * @return This factory.
             */
            public Factory setUserAgent(@Nullable String userAgent) {
                this.userAgent = userAgent;
                return this;
            }

            /**
             * Sets the {@link CacheControl} that will be used.
             *
             * <p>The default is {@code null}.
             *
             * @param cacheControl The cache control that will be used.
             * @return This factory.
             */
            public Factory setCacheControl(@Nullable CacheControl cacheControl) {
                this.cacheControl = cacheControl;
                return this;
            }

            /**
             * Sets a content type {@link Predicate}. If a content type is rejected by the predicate then a
             * {@link InvalidContentTypeException} is thrown from {@link
             * OkHttpDataSource#open(DataSpec)}.
             *
             * <p>The default is {@code null}.
             *
             * @param contentTypePredicate The content type {@link Predicate}, or {@code null} to clear a
             *                             predicate that was previously set.
             * @return This factory.
             */
            public Factory setContentTypePredicate(@Nullable Predicate<String> contentTypePredicate) {
                this.contentTypePredicate = contentTypePredicate;
                return this;
            }

            /**
             * Sets the {@link TransferListener} that will be used.
             *
             * <p>The default is {@code null}.
             *
             * <p>See {@link DataSource#addTransferListener(TransferListener)}.
             *
             * @param transferListener The listener that will be used.
             * @return This factory.
             */
            public Factory setTransferListener(@Nullable TransferListener transferListener) {
                this.transferListener = transferListener;
                return this;
            }

            @Override
            public OkHttpDataSource createDataSource() {
                OkHttpDataSource dataSource =
                        new OkHttpDataSource(
                                callFactory, userAgent, cacheControl, defaultRequestProperties, contentTypePredicate);
                if (transferListener != null) {
                    dataSource.addTransferListener(transferListener);
                }
                return dataSource;
            }
        }
    }
}
