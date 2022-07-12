package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.PersistenceException;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.DatatypeConverter;

/**
 * The persistent class for the CRY_CRL database table.
 *
 */
@Entity
@Cacheable(true)
@Table(name = "CRY_CRL", uniqueConstraints = { @UniqueConstraint(columnNames = { "UNIQUE_ID", }) })
@IdClass(CryCrlPK.class)
public class CryCrl implements Serializable {

    private static final long serialVersionUID = 1L;
    private String subjectdn;
    private byte[] crl;
    private Date updateData;
    private Date nextExpiration;
    private String keyId;
    private String uniqueId;

    public CryCrl() {
    }

    @Id
    public String getSubjectdn() {
        return this.subjectdn;
    }

    public void setSubjectdn(String subjectdn) {
        this.subjectdn = subjectdn;
    }

    @Lob()
    public byte[] getCrl() {
        return this.crl;
    }

    public void setCrl(byte[] crl) {
        this.crl = crl;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATE_DATA")
    public Date getUpdateData() {
        return this.updateData;
    }

    public void setUpdateData(Date updateData) {
        this.updateData = updateData;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "NEXT_EXPIRATION")
    public Date getNextExpiration() {
        return nextExpiration;
    }

    public void setNextExpiration(Date nextExpiration) {
        this.nextExpiration = nextExpiration;
    }

    @Id
    @Column(name = "SUBJECT_KEY_ID")
    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    @Column(name = "UNIQUE_ID")
    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    @PrePersist
    public void prePersist() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String toHash = subjectdn + keyId;
            md.update(toHash.getBytes());
            uniqueId = DatatypeConverter.printHexBinary(md.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException ex) {
            throw new PersistenceException(ex);
        }
    }

}
