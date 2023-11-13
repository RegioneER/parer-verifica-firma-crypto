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

package it.eng.parer.crypto.service.job;

import java.math.BigDecimal;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.eng.crypto.bean.ConfigBean;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoSignerException;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.storage.ICAStorage;
import it.eng.crypto.storage.ICRLStorage;
import it.eng.crypto.storage.IConfigStorage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;

/**
 * Session Bean implementation class InvokeUpdate. Il job dura circa 20 minuti
 */
@Service
public class CAUpdateJob {

    Logger log = LoggerFactory.getLogger(CAUpdateJob.class);

    @Autowired
    ICAStorage caHelperLocal;

    @Autowired
    ICRLStorage crlHelperLocal;

    @Autowired
    IConfigStorage configHelperLocal;

    @Value("${cron.ca.enable}")
    public boolean enable;

    @Autowired
    SignerUtil signerUtil;

    @Scheduled(cron = "${cron.ca.sched}")
    public void doJob() {
        if (enable) {
            log.atInfo().log("CA Update Job - Started");
            LocalDateTime inizio = LocalDateTime.now();

            this.loadingCa();

            long minutes = Duration.between(inizio, LocalDateTime.now()).toMinutes();
            log.atInfo().log("CA Update Job - Finished in {} minutes", minutes);
        } else {
            log.atInfo().log("CA Update Job - Disabled");
        }
    }

    private void loadingCa() {
        try {
            // TASK UPDATE CERTIFICATI CA e CRL - Scarico tutti i
            // certificati dal CNIPA, le CRL e creo le configurazioni nel DB.
            // Siccome non utilizzo mai la chiave ottengo solamente i valori.
            Collection<X509Certificate> qualifiedCertificate = signerUtil.getQualifiedPrincipalsAndX509Certificates()
                    .values();
            final int size = qualifiedCertificate.size();
            log.atInfo().log("Trovati {} certificati dal CNIPA", size);
            // Utilizzo l'iteratore e rimovo l'elemento per rendere eleggibile al GC il record già processato
            Iterator<X509Certificate> iterator = qualifiedCertificate.iterator();
            int caProcessate = 0;
            while (iterator.hasNext()) {
                this.updateCA(iterator.next());
                log.atInfo().log("Processata CA {} di {} ", ++caProcessate, size);
                iterator.remove();
            }

        } catch (CryptoSignerException e) {
            log.atError().log("Errore nello scarico dei certificati CA dal CNIPA", e);
        }
    }

    private void updateCA(X509Certificate cert) {

        try {
            caHelperLocal.insertCA(cert);

            log.atDebug().log("SubjectDN della CA: {}", cert.getSubjectX500Principal().getName());
            final String subjectKeyId = SignerUtil.getSubjectKeyId(cert);

            // Salvo l'url di recupero CRL
            List<String> urls = getCrlDistributionUrls(cert, subjectKeyId);

            if (isCaActive(cert)) {
                // Creo una nuova configurazione
                int i = 1;
                if (urls != null) {
                    for (String url : urls) {
                        ConfigBean config = new ConfigBean();
                        config.setCrlURL(url);
                        config.setNiOrdUrlDistribCrl(BigDecimal.valueOf(i));
                        config.setSubjectDN(cert.getSubjectX500Principal().getName());
                        config.setKeyId(subjectKeyId);
                        configHelperLocal.upsertConfig(config);
                        i++;
                    }
                } else {
                    log.atWarn().log(
                            "La lista dei punti di distribuzione delle CRL è vuota per la CA - Subject Key ID: {}",
                            subjectKeyId);
                }
            }

            saveCRL(urls);

        } catch (CryptoStorageException e) {
            log.atError().log("Impossibile aggiornare la CA:", e);
        } catch (Exception e) {
            log.atError().log("Errore nell'aggionamento delle CA", e);
        }
    }

    private boolean isCaActive(X509Certificate cert) {
        boolean isCAActive;
        try {
            cert.checkValidity();
            isCAActive = true;
        } catch (CertificateException e) {
            isCAActive = false;
        }
        return isCAActive;
    }

    private List<String> getCrlDistributionUrls(X509Certificate cert, String subjectKeyId) {
        List<String> urls = null;
        try {
            urls = signerUtil.getURLCrlDistributionPoint(cert);
        } catch (CryptoSignerException e) {
            log.atWarn().log("Non è stato possibile recuperare la CRL dal distribution point - Subject Key ID: {}",
                    subjectKeyId);
        }
        return urls;
    }

    private void saveCRL(List<String> distributionPoints) throws CryptoStorageException {
        if (distributionPoints != null) {
            X509CRL crl = signerUtil.getCrlByURL(distributionPoints);
            if (crl != null) {
                // la salvo sul db
                crlHelperLocal.upsertCRL(crl);
            }
        }
    }

}
