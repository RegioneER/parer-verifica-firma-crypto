package it.eng.parer.crypto.service.helper;

import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.storage.ICRLStorage;
import java.security.cert.X509CRL;

/**
 * Specializzazione dell'interfaccia utilizzata dalla cryptolibrary.
 *
 * @author Snidero_L
 */
public interface CRLHelperLocal extends ICRLStorage {

    /**
     * Ottieni la CRL utilizzando l'id univoco che viene generato prima di persistere l'entità.
     *
     * @param uniqueId
     *            MD5 di subjectdn + keyId;
     * 
     * @return CRL (se esiste)
     * 
     * @throws CryptoStorageException
     *             in caso di errore.
     */
    public X509CRL getByUniqueId(String uniqueId) throws CryptoStorageException;

}
