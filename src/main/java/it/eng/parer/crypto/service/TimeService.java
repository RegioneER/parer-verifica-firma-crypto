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

package it.eng.parer.crypto.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.cms.CMSTimeStampedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.parer.crypto.model.ParerTSD;
import it.eng.parer.crypto.model.ParerTST;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.service.verifica.OldCryptoInvoker;

/**
 *
 * @author Snidero_L
 */
@Service
public class TimeService {

    private final Logger log = LoggerFactory.getLogger(TimeService.class);

    @Autowired
    private OldCryptoInvoker cryptoInvoker;

    /**
     * Ottieni un TimestampToken per il contenuto.
     *
     * @param content
     *            documento
     *
     * @return Oggetto contenente il timestamp token.
     *
     * @throws CryptoParerException
     *             per i vari casi di errore.
     */
    public ParerTST getTst(byte[] content) throws CryptoParerException {
        try {
            log.debug("Richiedo TST per un byteArray di {} elementi", content.length);
            TimeStampToken requestTST = cryptoInvoker.requestTST(content);
            if (requestTST == null) {
                throw new CryptoParerException().withCode(ParerError.ErrorCode.TSP_EXCEPTION)
                        .withMessage("RequestTST è vuoto").withDetail(
                                "Per qualche motivo la richiesta di un nuovo timestamp ha prodotto un timestamp vuoto.");
            }

            return toParerTST(requestTST);

        } catch (TSPException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_EXCEPTION)
                    .withMessage("Errore di tipo TSPException durante la richiesta di un timestamp");
        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_IO)
                    .withMessage("Errore di INPUT/OUTPUT durante la richiesta di un timestamp");
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_PROVIDER_ERROR).withMessage(
                    "Errore di tipo NoSuchAlgorithm o NoSuchProvider durante la richiesta di un timestamp");
        }

    }

    /**
     * Ottieni un TSD per il contenuto. Il file tsd contiene il documento originale a cui è stata applicata la marca
     * temporale più la marca temporale stessa.
     *
     * @param content
     *            documento originale
     *
     * @return documento originale + marca
     *
     * @throws CryptoParerException
     *             per i vari casi di errore.
     */
    public ParerTSD getTsd(byte[] content) throws CryptoParerException {
        try {
            log.debug("Richiedo TSD per un byteArray di {} elementi", content.length);
            CMSTimeStampedData generateTSD = cryptoInvoker.generateTSD(content);
            if (generateTSD == null) {
                throw new CryptoParerException().withCode(ParerError.ErrorCode.TSP_EXCEPTION)
                        .withMessage("CMSTimeStampedData è vuoto")
                        .withDetail("Per qualche motivo il contenuto marcato risulta vuoto.");
            }

            ParerTSD parerTsd = new ParerTSD();
            parerTsd.setEncoded(generateTSD.getEncoded());
            for (TimeStampToken tst : generateTSD.getTimeStampTokens()) {
                ParerTST parerTst = toParerTST(tst);
                parerTsd.addTimeStampToken(parerTst);
            }
            return parerTsd;

        } catch (TSPException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_EXCEPTION)
                    .withMessage("Errore di tipo TSPException durante la richiesta di un timestamp");

        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSP_IO)
                    .withMessage("Errore di INPUT/OUTPUT durante la richiesta di un timestamp");

        } catch (CMSException | NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.TSD_PROVIDER_ERROR).withMessage(
                    "Errore di tipo NoSuchAlgorithm o NoSuchProvider durante la richiesta di un timestamp");
        }
    }

    private ParerTST toParerTST(TimeStampToken requestTST) throws IOException {
        ParerTST parerTst = new ParerTST();
        parerTst.setEncoded(requestTST.getEncoded());
        ParerTST.TimeStampInfo timeStampInfo = parerTst.create();
        if (requestTST.getTimeStampInfo() != null) {
            timeStampInfo.setGenTime(requestTST.getTimeStampInfo().getGenTime());
        }
        parerTst.setTimeStampInfo(timeStampInfo);
        return parerTst;
    }

}
