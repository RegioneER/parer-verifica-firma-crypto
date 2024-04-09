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

package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import it.eng.parer.crypto.jpa.entity.converter.NeverendingDateConverter;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

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
        // document why this constructor is empty
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

    /**
     * Secondo specifica RFC5280 https://tools.ietf.org/html/rfc5280#section-5.1.2.5 le date sarebbero normalmente
     * espresse in UTC/GMT Il sistema però persistente con il TZ locale (ossia GMT+01), esiste un caso "particolare" di
     * timestamp : 9999/31/12 23:59:59 UTC che per un hard limit di ORACLE DB non può essere persisto (la sua
     * conversione in GMT+01 lo trasforma in 10000/01/01 00:59:59 GMT+01) che non può essere persistito e/o letto
     */
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
        //
        this.updateData = NeverendingDateConverter.verifyOverZoneId(this.updateData,
                TimeZone.getTimeZone("UTC").toZoneId());
        this.nextExpiration = NeverendingDateConverter.verifyOverZoneId(this.nextExpiration,
                TimeZone.getTimeZone("UTC").toZoneId());
    }

    @PreUpdate
    void preUpdate() {
        this.updateData = NeverendingDateConverter.verifyOverZoneId(this.updateData,
                TimeZone.getTimeZone("UTC").toZoneId());
        this.nextExpiration = NeverendingDateConverter.verifyOverZoneId(this.nextExpiration,
                TimeZone.getTimeZone("UTC").toZoneId());
    }
}
