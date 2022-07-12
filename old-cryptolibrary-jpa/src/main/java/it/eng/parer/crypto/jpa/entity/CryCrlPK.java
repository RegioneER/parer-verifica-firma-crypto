package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;

/**
 * The persistent class for the CRY_CERTIFICATE database table.
 * 
 */

public class CryCrlPK implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -8919563301298567032L;

    private String subjectdn;
    private String keyId;

    public String getSubjectdn() {
        return this.subjectdn;
    }

    public CryCrlPK() {
        super();
    }

    public CryCrlPK(String subjectdn, String keyId) {
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CryCrlPK other = (CryCrlPK) obj;
        if (keyId == null) {
            if (other.keyId != null)
                return false;
        } else if (!keyId.equals(other.keyId))
            return false;
        if (subjectdn == null) {
            if (other.subjectdn != null)
                return false;
        } else if (!subjectdn.equals(other.subjectdn))
            return false;
        return true;
    }

}