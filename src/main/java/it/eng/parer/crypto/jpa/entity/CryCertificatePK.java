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

/**
 * The persistent class for the CRY_CERTIFICATE database table.
 *
 */
public class CryCertificatePK implements Serializable {

    private static final long serialVersionUID = -4257394662679517282L;
    private String subjectdn;
    private String keyId;

    public String getSubjectdn() {
        return this.subjectdn;
    }

    public CryCertificatePK() {
        super();
    }

    public CryCertificatePK(String subjectdn, String keyId) {
        super();
        this.subjectdn = subjectdn;
        this.keyId = keyId;
    }

    public void setSubjectdn(String subjectdn) {
        this.subjectdn = subjectdn;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keyId == null) ? 0 : keyId.hashCode());
        result = prime * result + ((subjectdn == null) ? 0 : subjectdn.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CryCertificatePK other = (CryCertificatePK) obj;
        if (keyId == null) {
            if (other.keyId != null) {
                return false;
            }
        } else if (!keyId.equals(other.keyId)) {
            return false;
        }
        if (subjectdn == null) {
            if (other.subjectdn != null) {
                return false;
            }
        } else if (!subjectdn.equals(other.subjectdn)) {
            return false;
        }
        return true;
    }

}
