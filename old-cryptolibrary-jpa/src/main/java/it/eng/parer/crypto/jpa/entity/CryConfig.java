package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.*;

/**
 * The persistent class for the CRY_CONFIG database table.
 * 
 */
@Entity
@Cacheable(true)
@Table(name = "CRY_CONFIG")
@IdClass(CryConfigPK.class)
public class CryConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String subjectdn;
    private String crlurl;
    private BigDecimal niOrdUrlDistribCrl;
    private String keyId;

    public CryConfig() {
    }

    @Id
    public String getSubjectdn() {
        return this.subjectdn;
    }

    public void setSubjectdn(String subjectdn) {
        this.subjectdn = subjectdn;
    }

    public String getCrlurl() {
        return this.crlurl;
    }

    public void setCrlurl(String crlurl) {
        this.crlurl = crlurl;
    }

    @Id
    @Column(name = "NI_ORD_URL_DISTRIB_CRL")
    public BigDecimal getNiOrdUrlDistribCrl() {
        return niOrdUrlDistribCrl;
    }

    public void setNiOrdUrlDistribCrl(BigDecimal niOrdUrlDistribCrl) {
        this.niOrdUrlDistribCrl = niOrdUrlDistribCrl;
    }

    @Id
    @Column(name = "SUBJECT_KEY_ID")
    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

}