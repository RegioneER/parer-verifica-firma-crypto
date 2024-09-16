/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.crypto.service.util;

import java.io.IOException;
import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;

public class CommonsHttpClient implements Serializable {

    private static final long serialVersionUID = -8044270255490903928L;

    /** The default connection timeout (1 minute) */
    private static final int TIMEOUT_CONNECTION = 60;

    /** The default socket timeout (1 minute) */
    private static final int TIMEOUT_SOCKET = 60;

    /** The default value of maximum connections in time (20) */
    private static final int CONNECTIONS_MAX_TOTAL = 20;

    /** The default value of maximum connections per route (2) */
    private static final int CONNECTIONS_MAX_PER_ROUTE = 2;

    /** The default connection total time to live (TTL) (1 minute) */
    private static final int CONNECTION_TIME_TO_LIVE = 60;

    /** The default ssl verify or not */
    private static final boolean NO_SSL_VERIFY = false;

    // default 60 s
    int httpClientTimeout = TIMEOUT_CONNECTION;

    // default 60 s
    int httpClientSocketTimeout = TIMEOUT_SOCKET;

    // default 4
    int httpClientConnectionsmaxperroute = CONNECTIONS_MAX_PER_ROUTE;

    // default 40
    int httpClientConnectionsmax = CONNECTIONS_MAX_TOTAL;

    // defult 60s
    long httpClientTimeToLive = CONNECTION_TIME_TO_LIVE;

    // default false
    boolean noSslVerify = NO_SSL_VERIFY;

    private transient CloseableHttpClient client;

    public CommonsHttpClient() {
        // empty
    }

    /**
     * init method
     */
    public void init() {
        if (client == null) {
            createHttpClient();
        }
    }

    private void createHttpClient() {
        // client
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        // config
        RequestConfig config = RequestConfig.custom().setConnectTimeout(httpClientTimeout * 1000)
                .setConnectionRequestTimeout(httpClientTimeout * 1000).setSocketTimeout(httpClientSocketTimeout * 1000)
                .build();

        httpClientBuilder.setDefaultRequestConfig(config)
                .setConnectionTimeToLive(httpClientTimeToLive, TimeUnit.MILLISECONDS)
                .setConnectionManager(getConnectionManager());
        // ssl
        if (noSslVerify) {
            try {
                httpClientBuilder
                        .setSSLContext(
                                new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                throw new CryptoParerException(e).withCode(ParerError.ErrorCode.GENERIC_ERROR)
                        .withMessage("Eccezione generica in fase di creazione client");
            }
        }

        client = httpClientBuilder.build();
    }

    /**
     * destroy method
     * 
     * @throws IOException
     *             generic exception
     */
    public void destroy() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private PoolingHttpClientConnectionManager getConnectionManager() {

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultMaxPerRoute(httpClientConnectionsmaxperroute);
        connManager.setMaxTotal(httpClientConnectionsmax);
        connManager.setDefaultSocketConfig(getSocketConfig());

        return connManager;
    }

    private SocketConfig getSocketConfig() {
        SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
        socketConfigBuilder.setSoTimeout(httpClientTimeout * 1000);
        return socketConfigBuilder.build();
    }

    public CloseableHttpClient getHttpClient() {
        return client;
    }

    public int getHttpClientTimeout() {
        return httpClientTimeout;
    }

    public void setHttpClientTimeout(int httpClientTimeout) {
        this.httpClientTimeout = httpClientTimeout;
    }

    public int getHttpClientSocketTimeout() {
        return httpClientSocketTimeout;
    }

    public void setHttpClientSocketTimeout(int httpClientSocketTimeout) {
        this.httpClientSocketTimeout = httpClientSocketTimeout;
    }

    public int getHttpClientConnectionsmaxperroute() {
        return httpClientConnectionsmaxperroute;
    }

    public void setHttpClientConnectionsmaxperroute(int httpClientConnectionsmaxperroute) {
        this.httpClientConnectionsmaxperroute = httpClientConnectionsmaxperroute;
    }

    public int getHttpClientConnectionsmax() {
        return httpClientConnectionsmax;
    }

    public void setHttpClientConnectionsmax(int httpClientConnectionsmax) {
        this.httpClientConnectionsmax = httpClientConnectionsmax;
    }

    public long getHttpClientTimeToLive() {
        return httpClientTimeToLive;
    }

    public void setHttpClientTimeToLive(long httpClientTimeToLive) {
        this.httpClientTimeToLive = httpClientTimeToLive;
    }

    public boolean isNoSslVerify() {
        return noSslVerify;
    }

    public void setNoSslVerify(boolean noSslVerify) {
        this.noSslVerify = noSslVerify;
    }

}
