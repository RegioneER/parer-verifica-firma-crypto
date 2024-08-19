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

import java.security.cert.X509CRL;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.eng.crypto.CryptoConstants;
import it.eng.crypto.data.CRLUtil;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.parer.crypto.jpa.entity.CryCrl;
import it.eng.parer.crypto.jpa.entity.CryCrlPK;
import it.eng.parer.crypto.jpa.repository.CryCrlRepository;
import jakarta.persistence.EntityManager;

/**
 * Session Bean implementation class Crl . Il name del service deve essere {@link CryptoConstants#ICRLSTORAGE}
 */
@Service(CryptoConstants.ICRLSTORAGE)
@Transactional
public class CRLHelper implements CRLHelperLocal {

    @Autowired
    CryCrlRepository repository;

    EntityManager em;

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void upsertCRL(X509CRL crl) throws CryptoStorageException {
        try {
            String keyId = SignerUtil.getAuthorityKeyId(crl);
            keyId = keyId == null ? "NON_VALORIZZATO" : keyId;
            CryCrlPK pk = new CryCrlPK(crl.getIssuerX500Principal().getName(), keyId);
            CryCrl entityCrl = null;
            Optional<CryCrl> res = repository.findById(pk);
            if (!res.isPresent()) {
                entityCrl = new CryCrl();
            } else {
                entityCrl = res.get();
            }

            entityCrl.setSubjectdn(crl.getIssuerX500Principal().getName());
            entityCrl.setUpdateData(crl.getThisUpdate());
            entityCrl.setNextExpiration(crl.getNextUpdate());
            entityCrl.setCrl(crl.getEncoded());
            entityCrl.setKeyId(keyId);
            repository.save(entityCrl);

        } catch (Exception e) {
            throw new CryptoStorageException(e);
        }
    }

    /**
     * Ottieni la CRL
     *
     * @param subjectDN
     *            DN del subject
     * @param keyId
     *            id della chiave
     *
     * @return CRL di BouncyCastle
     *
     * @throws CryptoStorageException
     *             eccezione di persistenza
     */
    @Override
    public X509CRL retriveCRL(String subjectDN, String keyId) throws CryptoStorageException {
        X509CRL ret = null;
        keyId = keyId == null ? "NON_VALORIZZATO" : keyId;
        CryCrlPK pk = new CryCrlPK(subjectDN, keyId);
        Optional<CryCrl> res = repository.findById(pk);
        if (res.isPresent()) {
            try {
                ret = CRLUtil.parse(res.get().getCrl());
            } catch (Exception e) {
                throw new CryptoStorageException(e);
            }
        }
        return ret;
    }

    @Override
    public X509CRL getByUniqueId(String uniqueId) throws CryptoStorageException {
        Optional<CryCrl> res = repository.findByUniqueId(uniqueId);
        X509CRL result = null;
        try {
            if (res.isPresent()) {
                result = CRLUtil.parse(res.get().getCrl());
            }
        } catch (Exception e) {
            throw new CryptoStorageException(e);
        }
        return result;
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
