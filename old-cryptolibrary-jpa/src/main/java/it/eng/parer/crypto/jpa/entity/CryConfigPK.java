package it.eng.parer.crypto.jpa.entity;

import java.io.Serializable;
import java.math.BigDecimal;

public class CryConfigPK implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1745570075004982722L;

    private String subjectdn;
    private String keyId;
    private BigDecimal niOrdUrlDistribCrl;

    public CryConfigPK() {

    }

    public CryConfigPK(String subjectdn, String keyId, BigDecimal niOrdUrlDistribCrl) {
        super();
        this.subjectdn = subjectdn;
        this.keyId = keyId;
        this.niOrdUrlDistribCrl = niOrdUrlDistribCrl;
    }

    public String getSubjectdn() {
        return subjectdn;
    }

    public void setSubjectdn(String subjectdn) {
        this.subjectdn = subjectdn;
    }

    public BigDecimal getNiOrdUrlDistribCrl() {
        return niOrdUrlDistribCrl;
    }

    public void setNiOrdUrlDistribCrl(BigDecimal niOrdUrlDistribCrl) {
        this.niOrdUrlDistribCrl = niOrdUrlDistribCrl;
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
        result = prime * result + ((niOrdUrlDistribCrl == null) ? 0 : niOrdUrlDistribCrl.hashCode());
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
        CryConfigPK other = (CryConfigPK) obj;
        if (keyId == null) {
            if (other.keyId != null)
                return false;
        } else if (!keyId.equals(other.keyId))
            return false;
        if (niOrdUrlDistribCrl == null) {
            if (other.niOrdUrlDistribCrl != null)
                return false;
        } else if (!niOrdUrlDistribCrl.equals(other.niOrdUrlDistribCrl))
            return false;
        if (subjectdn == null) {
            if (other.subjectdn != null)
                return false;
        } else if (!subjectdn.equals(other.subjectdn))
            return false;
        return true;
    }

}
