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
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.crypto.data.CRLUtil;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.parer.crypto.model.ParerCRL;
import it.eng.parer.crypto.model.ParerRevokedCertificate;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError.ErrorCode;
import it.eng.parer.crypto.service.helper.CRLHelperLocal;

/**
 * Servizio di gestione delle CRL.
 *
 * @author Snidero_L
 */
@Service
public class CrlService {

    @Autowired
    private CRLHelperLocal cRLHelper;

    @Autowired
    private SignerUtil signerUtil;

    /**
     * Carica una nuova crl.
     *
     * @param urls
     *            lista di url da cui scaricare la nuova crl.
     *
     * @return Modello del Parer per le crl.
     */
    public ParerCRL addCrlByURL(List<String> urls) {
        try {
            X509CRL cryptoCrl = signerUtil.getCrlByURL(urls);
            if (cryptoCrl == null) {
                throw new CryptoParerException().withCode(ErrorCode.CRL_NOT_FOUND)
                        .withMessage("CRL non trovata (url input: " + String.join(",", urls) + ")");
            }
            cRLHelper.upsertCRL(cryptoCrl);

            return toParerCrl(cryptoCrl);
        } catch (CRLException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_EXCEPTION)
                    .withMessage("CRL encoding error (url input: " + String.join(",", urls) + ")");
        } catch (CryptoStorageException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_CRYPTO_STORAGE)
                    .withMessage("Errore nel recupero e/o inserimento della CRL da/sul DB (url input: "
                            + String.join(",", urls) + ")");
        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_IO).withMessage(
                    "Errore durante il parsing del numero di CRL (url input: " + String.join(",", urls) + ")");
        }
    }

    /**
     * Ottieni una crl già scaricata.
     *
     * @param subjectDN
     *            Distinguished Name della CA che emette la CRL.
     * @param keyId
     *            Authority Key Identifier (in hex) del certificato.
     *
     * @return Modello del Parer per le crl.
     */
    public ParerCRL getCrl(String subjectDN, String keyId) {
        try {
            X509CRL cryptoCrl = cRLHelper.retriveCRL(subjectDN, keyId);
            if (cryptoCrl == null) {
                throw new CryptoParerException().withCode(ErrorCode.CRL_NOT_FOUND)
                        .withMessage("CRL non trovata (subjectDN: [" + subjectDN + "] keyId: [" + keyId + "])")
                        .withDetail(
                                "Errore di tipo cryptostoragexception durante il recupero della CRL con i parametri");
            }

            return toParerCrl(cryptoCrl);

        } catch (CryptoStorageException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_CRYPTO_STORAGE)
                    .withMessage("Errore nel recupero e/o inserimento della CRL da/sul DB (subjectDN: [" + subjectDN
                            + "] keyId: [" + keyId + "])");
        } catch (CRLException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_EXCEPTION)
                    .withMessage("CRL encoding error (subjectDN: [" + subjectDN + "] keyId: [" + keyId + "])");
        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_IO)
                    .withMessage("Errore durante il parsing del numero di CRL (subjectDN: [" + subjectDN + "] keyId: ["
                            + keyId + "])");
        }
    }

    /**
     * Ottieni la ParerCRL.
     *
     * @param uniqueId
     *            MD5 di subjectdn + keyId
     *
     * @return ParerCRL
     */
    public ParerCRL getCRL(String uniqueId) {
        try {
            X509CRL cryptoCrl = cRLHelper.getByUniqueId(uniqueId);
            if (cryptoCrl == null) {
                throw new CryptoParerException().withCode(ErrorCode.CRL_NOT_FOUND)
                        .withMessage("CRL non trovata (uniqueId: [" + uniqueId + "])").withDetail(
                                "Errore di tipo cryptostoragexception durante il recupero della CRL con i parametri");
            }

            return toParerCrl(cryptoCrl);
        } catch (CryptoStorageException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_CRYPTO_STORAGE).withMessage(
                    "Errore nel recupero e/o inserimento della CRL da/sul DB (uniqueId: [" + uniqueId + "])");
        } catch (CRLException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_EXCEPTION)
                    .withMessage("CRL encoding error (subjectDN:  (uniqueId: [" + uniqueId + "])");
        } catch (IOException ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_IO)
                    .withMessage("Errore durante il parsing del numero di CRL  (uniqueId: [" + uniqueId + "])");
        }
    }

    /**
     * Inserimento del blob di una CRL.
     *
     * @param crlBlob
     *            blob della CRL
     */
    public void addCRL(byte[] crlBlob) {
        try {
            X509CRL crl = CRLUtil.parse(crlBlob);
            cRLHelper.upsertCRL(crl);
        } catch (Exception ex) {
            throw new CryptoParerException(ex).withCode(ErrorCode.CRL_IO)
                    .withMessage("Errore durante l'inserimento dalla CRL)");
        }
    }

    private static ParerCRL toParerCrl(X509CRL cryptoCrl) throws CRLException, IOException {
        ParerCRL parerCrl = new ParerCRL();

        parerCrl.setThisUpdate(cryptoCrl.getThisUpdate());
        parerCrl.setNextUpdate(cryptoCrl.getNextUpdate());
        parerCrl.setEncoded(cryptoCrl.getEncoded());

        X500Principal issuerX500Principal = cryptoCrl.getIssuerX500Principal();
        if (issuerX500Principal != null) {
            parerCrl.setPrincipalName(issuerX500Principal.getName());
            // uno dei 2 andrà eliminato.
            parerCrl.setSubjectDN(issuerX500Principal.getName());
        }
        parerCrl.setExtensionValueOidSpecifico(cryptoCrl.getExtensionValue("2.5.29.35"));

        String authorityKeyId = SignerUtil.getAuthorityKeyId(cryptoCrl);
        parerCrl.setKeyId(authorityKeyId);

        Set<? extends X509CRLEntry> revokedCertificates = cryptoCrl.getRevokedCertificates();
        if (revokedCertificates != null) {
            for (X509CRLEntry revokedCertificate : revokedCertificates) {
                ParerRevokedCertificate parerRevokedCertificate = new ParerRevokedCertificate();
                parerRevokedCertificate.setRevocationDate(revokedCertificate.getRevocationDate());
                parerRevokedCertificate.setSerialNumber(revokedCertificate.getSerialNumber());
                parerCrl.addRevokedCertificate(parerRevokedCertificate);
            }
        }

        byte[] crlNumByte = cryptoCrl.getExtensionValue(X509Extension.cRLNumber.getId());
        parerCrl.setNumBytes(crlNumByte);

        BigInteger crlNum = null;
        if (crlNumByte != null) {
            crlNum = org.bouncycastle.asn1.x509.CRLNumber.getInstance(X509ExtensionUtil.fromExtensionValue(crlNumByte))
                    .getValue();
        }

        parerCrl.setCrlNum(crlNum);

        return parerCrl;

    }
}
