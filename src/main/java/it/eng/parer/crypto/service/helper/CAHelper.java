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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.eng.crypto.CryptoConstants;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.storage.ICAStorage;
import it.eng.parer.crypto.jpa.entity.CryCertificate;
import it.eng.parer.crypto.jpa.entity.CryCertificatePK;
import it.eng.parer.crypto.jpa.repository.CryCertificateRepository;
import jakarta.persistence.EntityManager;

/**
 * Session Bean implementation class CA. Il nome deve essere {@link CryptoConstants#ICASTORAGE}.
 */
@Service(CryptoConstants.ICASTORAGE)
@Transactional
public class CAHelper implements ICAStorage {

    @Autowired
    CryCertificateRepository repository;

    EntityManager em;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void insertCA(X509Certificate certificate) throws CryptoStorageException {
        try {
            String subjectDN = certificate.getSubjectX500Principal().getName();

            final String keyId = SignerUtil.getSubjectKeyId(certificate);
            if (keyId == null) {
                throw new CryptoStorageException("Il certificato con DN " + subjectDN
                        + " non contiene l'estensione Subject Key Identifier (OID 2.5.29.14). Impossibile censirlo tra le CA valide.");
            }

            boolean active = true;

            Date data = certificate.getNotAfter();
            byte[] dati = certificate.getEncoded();
            try {
                certificate.checkValidity();
                active = true;
            } catch (CertificateException e) {
                active = false;
            }
            boolean isNew = false;
            CryCertificatePK pk = new CryCertificatePK(subjectDN, keyId);
            CryCertificate cert = null;
            Optional<CryCertificate> res = repository.findById(pk);
            if (!res.isPresent()) {
                cert = new CryCertificate();
                isNew = true;
            } else {
                cert = res.get();
            }
            cert.setActive(active ? "Y" : "N");
            cert.setCertificate(dati);
            cert.setExpirationDate(data);
            cert.setSubjectdn(subjectDN);
            cert.setKeyId(keyId);
            if (isNew) {
                repository.save(cert);
            }
        } catch (IOException | CertificateEncodingException e) {
            throw new CryptoStorageException("Errore durante l'inserimento della CA", e);
        }

    }

    @Override
    public boolean isActive(X509Certificate certificate, String keyId) throws CryptoStorageException {
        CryCertificatePK pk = new CryCertificatePK(certificate.getSubjectX500Principal().getName(), keyId);
        Optional<CryCertificate> c = repository.findById(pk);
        if (c.isPresent()) {
            return c.get().getActive().equals("Y");
        } else {
            return false;
        }
    }

    @Override
    public List<X509Certificate> retriveActiveCA() throws CryptoStorageException {
        List<X509Certificate> ret = new ArrayList<X509Certificate>();
        List<CryCertificate> certificati = repository.findByActive("Y");
        try {
            if (certificati != null) {
                CertificateFactory factorys = CertificateFactory.getInstance("X509",
                        BouncyCastleProvider.PROVIDER_NAME);
                for (CryCertificate cert : certificati) {
                    if (cert.getCertificate() != null) {
                        X509Certificate certificate = (X509Certificate) factorys
                                .generateCertificate(new ByteArrayInputStream(cert.getCertificate()));
                        if (certificate != null) {
                            ret.add(certificate);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new CryptoStorageException(e);
        }
        return ret;
    }

    @Override
    public X509Certificate retriveCA(X500Principal subject, String keyId) throws CryptoStorageException {
        X509Certificate ret = null;
        CryCertificatePK pk = new CryCertificatePK(subject.getName(), keyId);
        Optional<CryCertificate> res = repository.findById(pk);
        try {
            if (res.isPresent()) {
                CertificateFactory factorys = CertificateFactory.getInstance("X509",
                        BouncyCastleProvider.PROVIDER_NAME);
                ret = (X509Certificate) factorys
                        .generateCertificate(new ByteArrayInputStream(res.get().getCertificate()));
            }
        } catch (Exception e) {
            throw new CryptoStorageException(e);
        }
        return ret;
    }

    /**
     * Setter utilizzato da JUnit
     *
     * @param em
     *            entityManager
     */
    public void setEm(EntityManager em) {
        this.em = em;
    }

}
