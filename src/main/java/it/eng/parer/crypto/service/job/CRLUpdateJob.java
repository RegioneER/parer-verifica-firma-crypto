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

import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import it.eng.crypto.bean.ConfigBean;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.storage.ICRLStorage;
import it.eng.crypto.storage.IConfigStorage;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Session Bean implementation class InvokeUpdate
 */
@Service
public class CRLUpdateJob {

    Logger logger = LoggerFactory.getLogger(CRLUpdateJob.class);

    @Autowired
    IConfigStorage configHelper;

    @Autowired
    ICRLStorage crlHelper;

    @Value("${cron.crl.enable}")
    boolean enable;

    @Autowired
    SignerUtil signerUtil;

    @Scheduled(cron = "${cron.crl.sched}")
    public void doJob() {
        if (enable) {
            logger.info("CRL Update Job - Started");
            LocalDateTime inizio = LocalDateTime.now();

            this.loadingCrl();

            long minutes = Duration.between(inizio, LocalDateTime.now()).toMinutes();
            logger.info("CRL Update Job - Finished in {} minutes", minutes);
        } else {
            logger.info("CRL Update Job - Disabled");
        }
    }

    private void loadingCrl() {
        try {
            // TASK SCARICO CRL PROSSIME ALLA SCADENZA - Recupero tutti i
            // distribution point del sistema per le CA attive e con data di
            // scadenza successiva alla data odierna
            // Le configurazioni sono ordinate per subjectDN e numero d'ordine
            // del distribution point
            List<ConfigBean> crlConfig = configHelper.retriveAllConfig();
            if (crlConfig != null) {
                logger.debug("CRL Update Job - Trovate {} configurazioni", crlConfig.size());
                // Per ogni configurazione invio un messaggio
                Map<String, List<String>> map = new HashMap<String, List<String>>();
                for (ConfigBean config : crlConfig) {
                    List<String> distrPoints = null;
                    if ((distrPoints = map.get(config.getSubjectDN() + "|" + config.getKeyId())) != null) {
                        distrPoints.add(config.getCrlURL());
                    } else {
                        distrPoints = new ArrayList<>();
                        distrPoints.add(config.getCrlURL());
                        map.put(config.getSubjectDN() + "|" + config.getKeyId(), distrPoints);
                    }
                }
                int crlProcessate = 0;
                for (Map.Entry<String, List<String>> urls : map.entrySet()) {
                    updateCRL(urls.getValue(), urls.getKey());
                    logger.info("Processata CRL {} di {} ", ++crlProcessate, map.entrySet().size());
                }
            }
        } catch (CryptoStorageException e) {
            logger.error("Errore nel reperimento delle configurazioni CRL", e);
        }
    }

    private void updateCRL(List<String> distributionPoints, String key) {
        // In base all'url passato in ingresso recupero la CRL.
        X509CRL crl = signerUtil.getCrlByURL(distributionPoints);
        try {
            if (crl != null) {
                // la salvo sul db
                crlHelper.upsertCRL(crl);
            }
            logger.debug("Inviato update delle CRL per il subject {}", key);

        } catch (CryptoStorageException e) {
            logger.error("Non è stato possibile salvare la CRL nel db", e);
        } catch (Exception e) {
            logger.error("Errore nell'update :" + key, e);
        }
    }

}
