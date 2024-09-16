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

package it.eng.parer.crypto.service.helper;

import java.io.IOException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ApacheClientHelper {

    /*
     * Standard httpclient
     */
    // default 60 s
    @Value("${parer.crypto.uriloader.httpclient.timeout:60}")
    int httpClientTimeout;

    // default 60 s
    @Value("${parer.crypto.uriloader.httpclient.timeoutsocket:60}")
    int httpClientSocketTimeout;

    // default 4
    @Value("${parer.crypto.uriloader.httpclient.connectionsmaxperroute:4}")
    int httpClientConnectionsmaxperroute;

    // default 40
    @Value("${parer.crypto.uriloader.httpclient.connectionsmax:40}")
    int httpClientConnectionsmax;

    // defult 60s
    @Value("${parer.crypto.uriloader.httpclient.timetolive:60}")
    long httpClientTimeToLive;

    // default false
    @Value("${parer.crypto.uriloader.httpclient.no-ssl-verify:false}")
    boolean noSslVerify;

    private CloseableHttpClient client;

    @PostConstruct
    public void init() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
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
            httpClientBuilder
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        }

        client = httpClientBuilder.build();
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

    @PreDestroy
    public void destroy() throws IOException {
        client.close();
    }

    public CloseableHttpClient client() {
        return client;
    }

}
