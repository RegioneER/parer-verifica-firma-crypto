/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna <p/> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. <p/> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details. <p/> You should
 * have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.crypto.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.parer.crypto.model.ParerCertificate;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.service.helper.CAHelper;

/**
 * Service layer per le operazioni sui certificati.
 *
 * @author Snidero_L
 */
@Service
public class CertificateService {

    @Autowired
    private CAHelper caHelper;

    /**
     * Ottieni l'authKeyId del certificato del firmatario. Questo metodo effettua la decodifica
     * tramite BouncyCastle dello stream di dati codificati DER.
     *
     * @param extvalue byte array del ceritificato del firmatario.
     *
     * @return l'authority key id codificato in HEX.
     */
    public String getCertificateKeyId(byte[] extvalue) {
	try {

	    CertificateFactory fact = CertificateFactory.getInstance("X.509", "BC");
	    // read the certificate
	    X509Certificate x509Cert = (X509Certificate) fact
		    .generateCertificate(new ByteArrayInputStream(extvalue));
	    return SignerUtil.getAuthorityKeyId(x509Cert);

	} catch (NoSuchProviderException | IOException | CertificateException ex) {
	    throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.CERT_IO)
		    .withMessage("Errore di INPUT/OUTPUT, sul certificato");
	}
    }

    /**
     * Ottieni il subject DN del certificato del firmatario.
     *
     * @param derCertificate byte array del ceritificato del firmatario.
     *
     * @return DN dell'authority che ha emesso il certificato.
     */
    public String getCertificateSubjectDN(byte[] derCertificate) {
	try {

	    CertificateFactory fact = CertificateFactory.getInstance("X.509", "BC");
	    // read the certificate
	    X509Certificate x509Cert = (X509Certificate) fact
		    .generateCertificate(new ByteArrayInputStream(derCertificate));
	    return x509Cert.getIssuerX500Principal().getName();

	} catch (NoSuchProviderException | CertificateException ex) {
	    throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.CERT_IO)
		    .withMessage("Errore di INPUT/OUTPUT, sul certificato");
	}
    }

    /**
     * Aggiungi un certificato della CA.
     *
     * @param derCertificate Certificato della CA
     *
     * @return l'oggetto che rappresenta il certificato
     */
    public ParerCertificate addCaCertificate(byte[] derCertificate) {
	try {
	    CertificateFactory fact = CertificateFactory.getInstance("X.509", "BC");
	    // read the certificate
	    X509Certificate x509Cert = (X509Certificate) fact
		    .generateCertificate(new ByteArrayInputStream(derCertificate));
	    caHelper.insertCA(x509Cert);
	    return toParerCertificate(x509Cert);

	} catch (IOException | CertificateException | NoSuchProviderException
		| CryptoStorageException ex) {
	    throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.CERT_EXCEPTION)
		    .withMessage("Errore durante l'inserimento del certificato");
	}
    }

    /**
     * Indica l'esistenza del certificato identificato da subjectDN e keyID.
     *
     * @param subjectDN dn della CA
     * @param keyId     id della CA
     *
     * @return vero se il certificato esiste in banca dati.Falso altrimenti. Non viene effettuato
     *         alcun controllo di validità temporale.
     */
    public boolean existsCaCertificate(String subjectDN, String keyId) {
	try {
	    X500Principal subject = new X500Principal(subjectDN);
	    X509Certificate retriveCA = caHelper.retriveCA(subject, keyId);
	    return retriveCA != null;
	} catch (CryptoStorageException ex) {
	    throw new CryptoParerException(ex).withCode(ParerError.ErrorCode.CERT_EXCEPTION)
		    .withMessage("Errore durante il recupero del certificato");
	}

    }

    private static ParerCertificate toParerCertificate(X509Certificate certificate)
	    throws IOException, CertificateEncodingException {
	ParerCertificate result = new ParerCertificate();
	result.setContent(certificate.getEncoded());
	result.setIssuerDN(certificate.getIssuerX500Principal().getName());
	result.setKeyId(SignerUtil.getAuthorityKeyId(certificate));
	result.setNotBefore(certificate.getNotBefore());
	result.setNotAfter(certificate.getNotAfter());

	result.setSerialNumber(certificate.getSerialNumber());
	result.setSubjectDN(certificate.getSubjectDN().getName());
	return result;
    }
}
