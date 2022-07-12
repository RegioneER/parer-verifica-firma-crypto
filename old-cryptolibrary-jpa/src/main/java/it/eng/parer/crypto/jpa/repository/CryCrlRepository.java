package it.eng.parer.crypto.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.eng.parer.crypto.jpa.entity.CryCrl;
import it.eng.parer.crypto.jpa.entity.CryCrlPK;
import java.util.Optional;

public interface CryCrlRepository extends JpaRepository<CryCrl, CryCrlPK> {

    /**
     * Ottieni la CRL identificata dall'id univoco.
     *
     * @param uniqueId
     *            md5 di subjectdn + keyId;
     * 
     * @return Entity della CRL
     */
    public Optional<CryCrl> findByUniqueId(String uniqueId);

}
