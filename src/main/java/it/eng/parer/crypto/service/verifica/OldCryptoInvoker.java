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

package it.eng.parer.crypto.service.verifica;

import it.eng.crypto.CryptoConfiguration;
import it.eng.crypto.CryptoConstants;
import it.eng.crypto.FactorySigner;
import it.eng.crypto.TSAConfiguration;
import it.eng.crypto.controller.bean.OutputSignerBean;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoSignerException;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.manager.SignatureManager;
import it.eng.crypto.storage.ICRLStorage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TSPValidationException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.cms.CMSTimeStampedData;
import org.bouncycastle.tsp.cms.CMSTimeStampedDataGenerator;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author Quaranta_M
 */
@Component
public class OldCryptoInvoker {

    private final Logger log = LoggerFactory.getLogger(OldCryptoInvoker.class);

    @Autowired
    private ApplicationContext context;

    @Value("${parer.crypto.TSAServiceURL}")
    private String parerTSAServiceURL;
    @Value("${parer.crypto.TSAAuthScope}")
    private String parerTSAAuthScope;
    @Value("${parer.crypto.TSAUser}")
    private String parerTSAUser;
    @Value("${parer.crypto.TSAPass}")
    private String parerTSAPass;

    @Value("${parer.crypto.trovaCAOnline}")
    private boolean checkCAOnline;

    public OutputSignerBean verFirmeEmbeddedVers(File fileWithSignature, File timeStamp, Date referenceDate,
            boolean useSigninDate, String referenceDateType) {
        SignatureManager versManager = context.getBean("VersamentoManager", SignatureManager.class);
        versManager.setUseSigninTimeAsReferenceDate(useSigninDate);
        versManager.setReferenceDateType(referenceDateType);
        versManager.setSearchCAOnline(checkCAOnline);
        return this.verificaFirmeEmbedded(fileWithSignature, timeStamp, referenceDate, versManager);

    }

    public OutputSignerBean verFirmeDetachedVers(File signedFile, File detachedSignature, Date referenceDate,
            boolean useSigninDate, String referenceDateType) {
        SignatureManager versManager = context.getBean("VersamentoManager", SignatureManager.class);
        versManager.setUseSigninTimeAsReferenceDate(useSigninDate);
        versManager.setReferenceDateType(referenceDateType);
        versManager.setSearchCAOnline(checkCAOnline);
        return this.verificaFirmeDetached(signedFile, detachedSignature, referenceDate, versManager);

    }

    public OutputSignerBean verFirmeEmbeddedChiuV(File fileWithSignature, File timeStamp, Date referenceDate) {
        SignatureManager chiusuraManager = context.getBean("ChiusuraVolManager", SignatureManager.class);
        chiusuraManager.setSearchCAOnline(checkCAOnline);
        return this.verificaFirmeEmbedded(fileWithSignature, timeStamp, referenceDate, chiusuraManager);

    }

    public OutputSignerBean verFirmeDetachedChiuV(File signedFile, File detachedSignature, Date referenceDate) {
        SignatureManager chiusuraManager = context.getBean("ChiusuraVolManager", SignatureManager.class);
        chiusuraManager.setSearchCAOnline(checkCAOnline);
        return this.verificaFirmeDetached(signedFile, detachedSignature, referenceDate, chiusuraManager);

    }

    private OutputSignerBean verificaFirmeEmbedded(File fileWithSignature, File timeStamp, Date referenceDate,
            SignatureManager manager) {
        OutputSignerBean outputSignerBean = null;
        try {
            if (timeStamp == null) {
                outputSignerBean = manager.executeEmbedded(fileWithSignature, referenceDate);

            } else {
                outputSignerBean = manager.executeEmbedded(fileWithSignature, timeStamp, referenceDate);
            }

        } catch (CryptoSignerException ex) {
            log.atError().log("Errore nella verifica delle firme embedded", ex);
        }
        return outputSignerBean;
    }

    private OutputSignerBean verificaFirmeDetached(File signedFile, File detachedSignature, Date referenceDate,
            SignatureManager manager) {
        OutputSignerBean outputSignerBean = null;
        try {

            if (referenceDate != null) {

                outputSignerBean = manager.executeDetached(signedFile, detachedSignature, referenceDate);
            } else {
                outputSignerBean = manager.executeDetached(signedFile, detachedSignature);
            }

        } catch (CryptoSignerException ex) {
            log.atError().log("Errore nella verifica delle firme detached", ex);
        }
        return outputSignerBean;
    }

    /**
     * Genera il file TSD (CMSTimeStampedData) per il contenuto passato.
     *
     * @param content
     *            contenuto per il quale generare il TSD
     *
     * @return CMSTimeStampedData del contenuto
     *
     * @throws CMSException
     *             eccezione durante l'applicazione dei metodi di BC
     * @throws IOException
     *             errore generico di I/O
     * @throws NoSuchAlgorithmException
     *             algoritmo per il digest non fornito dalla JVM o BC
     * @throws NoSuchProviderException
     *             provider non fornito dalla JVM o BC
     * @throws TSPValidationException
     *             problema di validazione del timestamp "staccato" dalla TSA
     * @throws TSPException
     *             eccezione generica legata al timestamp "staccato" dalla TSA
     */
    public CMSTimeStampedData generateTSD(byte[] content) throws CMSException, IOException, NoSuchAlgorithmException,
            NoSuchProviderException, TSPValidationException, TSPException {
        CMSTimeStampedDataGenerator tsdGenerator = new CMSTimeStampedDataGenerator();
        return tsdGenerator.generate(requestTST(content), content);
    }

    private TimeStampResponse postTSTRequest(String postUrl, byte[] encodedRequest,
            HttpClientBuilder customClientBuilder) throws IOException, TSPException {
        TimeStampResponse timeStampResponse = null;

        CloseableHttpClient httpclient = customClientBuilder.build();
        try {

            // POST method con configurazione "expect continue" (https://httpstatusdogs.com/100-continue)
            HttpPost post = new HttpPost(postUrl);
            RequestConfig enableExpectContinue = RequestConfig.custom().setExpectContinueEnabled(true).build();
            post.setConfig(enableExpectContinue);

            // aggiungi l'entità
            ByteArrayEntity ent = new ByteArrayEntity(encodedRequest);
            ent.setContentType("application/timestamp-query");
            post.setEntity(ent);

            CloseableHttpResponse response = httpclient.execute(post);
            try {

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    InputStream instream = entity.getContent();
                    try {
                        timeStampResponse = new TimeStampResponse(instream);
                    } finally {
                        instream.close();
                    }
                }
            } finally {
                response.close();
            }

        } finally {
            httpclient.close();
        }

        return timeStampResponse;

    }

    private HttpClientBuilder configureCustomBuilder(TSAConfiguration tsaConfiguration,
            CryptoConfiguration cryptoConfiguration) {

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        HttpClientBuilder customClientBuilder = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider);

        // Autorizzazione per il TSA
        if (tsaConfiguration.isTSAAuth()) {
            Credentials credential = new UsernamePasswordCredentials(tsaConfiguration.getTSAUser(),
                    tsaConfiguration.getTSAPass());
            credentialsProvider.setCredentials(new AuthScope(tsaConfiguration.getTSAAuthScope(), AuthScope.ANY_PORT),
                    credential);

        }

        // Autorizzazione per il proxy
        if (cryptoConfiguration.isProxy()) {
            Credentials credential = cryptoConfiguration.isNTLSAuth()
                    ? new NTCredentials(cryptoConfiguration.getProxyUser(), cryptoConfiguration.getProxyPassword(),
                            cryptoConfiguration.getUserHost(), cryptoConfiguration.getUserDomain())
                    : new UsernamePasswordCredentials(cryptoConfiguration.getProxyUser(),
                            cryptoConfiguration.getProxyPassword());
            HttpHost proxy = new HttpHost(cryptoConfiguration.getProxyHost(), cryptoConfiguration.getProxyPort());
            credentialsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()), credential);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            customClientBuilder.setRoutePlanner(routePlanner);
        }
        customClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

        return customClientBuilder;

    }

    /**
     * Ottieni la marca temporale per il byte array passato in input.
     *
     * @param content
     *            byte array da marcare
     *
     * @return TimeStampToken di BC
     *
     * @throws IOException
     *             in caso di generico errore di I/O
     * @throws NoSuchAlgorithmException
     *             per il calcolo dello SHA-256
     * @throws NoSuchProviderException
     *             in caso non sia presente BC
     * @throws TSPValidationException
     *             validazione dopo l'emissione della marca temporale errata
     * @throws TSPException
     *             eccezione durante la generazione della marca
     */
    public TimeStampToken requestTST(byte[] content) throws IOException, NoSuchAlgorithmException,
            NoSuchProviderException, TSPValidationException, TSPException {
        MessageDigest md = getDigestInstance("SHA-256");

        byte[] fingerprints = md.digest(content);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true);

        TimeStampRequest request = reqGen.generate(TSPAlgorithms.SHA256, fingerprints);
        log.atDebug().log(String.valueOf(request.getMessageImprintDigest().length));

        byte[] encRequest = request.getEncoded();

        Map<String, String> tsaParams = new HashMap<>();
        tsaParams.put("TSAServiceURL", parerTSAServiceURL);
        tsaParams.put("TSAAuthScope", parerTSAAuthScope);
        tsaParams.put("TSAUser", parerTSAUser);
        tsaParams.put("TSAPass", parerTSAPass);

        TSAConfiguration tsaConfiguration = new TSAConfiguration(tsaParams);
        CryptoConfiguration cryptoConfiguration = context.getBean(CryptoConstants.CRYPTO_CONFIGURATION,
                CryptoConfiguration.class);
        // Configura credenziali ed, eventualmente, proxy
        HttpClientBuilder httpClientBuilder = configureCustomBuilder(tsaConfiguration, cryptoConfiguration);

        TimeStampResponse postTSTRequest = postTSTRequest(tsaConfiguration.getTSAServiceURL(), encRequest,
                httpClientBuilder);

        if (postTSTRequest == null) {
            throw new IllegalArgumentException("Errore durante la validazione della marca temporale");
        }

        try {
            postTSTRequest.validate(request);
        } catch (TSPValidationException ex) {
            log.atError().log("Errore durante la validazione della marca temporale", ex);
            throw ex;
        } catch (TSPException ex) {
            log.atError().log("Marca non conforme", ex);
            throw ex;
        }

        log.atDebug().log("TimestampResponse validated");

        return postTSTRequest.getTimeStampToken();

    }

    private MessageDigest getDigestInstance(String digest) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest md = null;
        try {

            md = MessageDigest.getInstance(digest, BouncyCastleProvider.PROVIDER_NAME);

        } catch (NoSuchAlgorithmException ex) {
            log.atError().log("Errore nel reperimento del MessageDigest SHA-256 nel provider BC", ex);
            throw ex;
        } catch (NoSuchProviderException ex) {
            log.atError().log("Errore nel reperimento del provider BC", ex);
            throw ex;
        }
        return md;
    }

    public boolean isCertificateValid(X509Certificate certificate, Date when)
            throws CertificateNotYetValidException, Exception {
        boolean valid = false;
        X509CRL crl = null;

        // controllo la validità del certificato
        certificate.checkValidity();
        FactorySigner.registerSpringContext(this.context);
        ICRLStorage crlStorage = FactorySigner.getInstanceCRLStorage();

        try {
            crl = crlStorage.retriveCRL(certificate.getIssuerX500Principal().getName(),
                    SignerUtil.getAuthorityKeyId(certificate));

        } catch (CryptoStorageException e) {
            // Si è verificato un errore durante il recupero della CRL storicizzata provo a scaricare la CRL
        }
        if (crl != null && crl.getNextUpdate().after(when)) {
            valid = !crl.isRevoked(certificate);
            return valid;
        } else {
            SignerUtil signerUtil = SignerUtil.newInstance();
            try {
                // Recupero l'URL del certificato
                List<String> url = signerUtil.getURLCrlDistributionPoint(certificate);
                crl = signerUtil.getCrlByURL(url);
                if (crl == null) {
                    throw new CryptoSignerException();
                }
            } catch (CryptoSignerException e) {
                valid = false;
                throw new Exception("Impossibile recuperare una CRL valida");
            }
            // salvo la crl recuperata dal distribution point
            try {
                crlStorage.upsertCRL(crl);
            } catch (CryptoStorageException e) {
                // Si è verificato un errore durante il recupero della CRL storicizzata continuo ..
            }
            // Controllo al validità del certificato
            valid = !crl.isRevoked(certificate);
            return valid;
        }
    }

}
