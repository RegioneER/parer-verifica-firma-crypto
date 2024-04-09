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
import java.util.Date;
import java.util.TimeZone;

import it.eng.parer.crypto.jpa.entity.converter.NeverendingDateConverter;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

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
        // document why this constructor is empty
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

    /**
     * Secondo specifica RFC5280 https://tools.ietf.org/html/rfc5280#section-5.1.2.5 le date sarebbero normalmente
     * espresse in UTC/GMT Il sistema però persistente con il TZ locale (ossia GMT+01), esiste un caso "particolare" di
     * timestamp : 9999/31/12 23:59:59 UTC che per un hard limit di ORACLE DB non può essere persisto (la sua
     * conversione in GMT+01 lo trasforma in 10000/01/01 00:59:59 GMT+01) che non può essere persistito e/o letto
     */
    @PrePersist
    void preInsert() {
        this.expirationDate = NeverendingDateConverter.verifyOverZoneId(this.expirationDate,
                TimeZone.getTimeZone("UTC").toZoneId());
    }

    @PreUpdate
    void preUpdate() {
        this.expirationDate = NeverendingDateConverter.verifyOverZoneId(this.expirationDate,
                TimeZone.getTimeZone("UTC").toZoneId());
    }

}
