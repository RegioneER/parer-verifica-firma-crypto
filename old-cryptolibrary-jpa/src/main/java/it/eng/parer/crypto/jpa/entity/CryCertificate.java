package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * The persistent class for the CRY_CERTIFICATE database table.
 * 
 */
@Entity
@Cacheable(true)
@Table(name = "CRY_CERTIFICATE")
@IdClass(CryCertificatePK.class)
public class CryCertificate implements Serializable {
    private static final long serialVersionUID = 1L;
    private String subjectdn;
    private String keyId;
    private String active;
    private byte[] certificate;
    private Date expirationDate;

    public CryCertificate() {
    }

    @Id
    public String getSubjectdn() {
        return this.subjectdn;
    }

    public void setSubjectdn(String subjectdn) {
        this.subjectdn = subjectdn;
    }

    public String getActive() {
        return this.active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    @Lob()
    public byte[] getCertificate() {
        return this.certificate;
    }

    public void setCertificate(byte[] certificate) {
        this.certificate = certificate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EXPIRATION_DATE")
    public Date getExpirationDate() {
        return this.expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
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