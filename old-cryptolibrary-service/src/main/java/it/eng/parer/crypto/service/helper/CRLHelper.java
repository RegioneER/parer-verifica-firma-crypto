package it.eng.parer.crypto.service.helper;

import it.eng.crypto.CryptoConstants;
import java.security.cert.X509CRL;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.eng.crypto.data.CRLUtil;
import it.eng.crypto.data.SignerUtil;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.parer.crypto.jpa.entity.CryCrl;
import it.eng.parer.crypto.jpa.entity.CryCrlPK;
import it.eng.parer.crypto.jpa.repository.CryCrlRepository;

/**
 * Session Bean implementation class Crl . Il name del service deve essere {@link CryptoConstants#ICRLSTORAGE}
 */
@Service(CryptoConstants.ICRLSTORAGE)
@Transactional
public class CRLHelper implements CRLHelperLocal {

    Logger log = LoggerFactory.getLogger(CRLHelper.class);

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
            // log.error("ERROR inserting CRL", e);
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
                // log.error("retriveCRL ERROR", e);
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
