package it.eng.parer.crypto.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.eng.parer.crypto.jpa.entity.CryCertificate;
import it.eng.parer.crypto.jpa.entity.CryCertificatePK;

public interface CryCertificateRepository extends JpaRepository<CryCertificate, CryCertificatePK> {

    @Query("SELECT c FROM CryCertificate c WHERE c.active = (:active)")
    public List<CryCertificate> findByActive(@Param("active") String active);
}
