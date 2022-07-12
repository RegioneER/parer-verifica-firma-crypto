package it.eng.parer.crypto.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.eng.parer.crypto.jpa.entity.CryConfig;
import it.eng.parer.crypto.jpa.entity.CryConfigPK;

public interface CryConfigRepository extends JpaRepository<CryConfig, CryConfigPK> {

    @Query("select conf.subjectdn, conf.crlurl, conf.niOrdUrlDistribCrl, conf.keyId "
            + "from CryConfig conf join CryCrl crl on (conf.subjectdn = crl.subjectdn) "
            + "where crl.nextExpiration <  SYSDATE() order by conf.subjectdn, conf.niOrdUrlDistribCrl")
    public List<Object[]> joinWithCryCrl();

}