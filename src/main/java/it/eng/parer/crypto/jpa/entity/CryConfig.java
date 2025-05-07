/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna <p/> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. <p/> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details. <p/> You should
 * have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

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
	// document why this constructor is empty
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
