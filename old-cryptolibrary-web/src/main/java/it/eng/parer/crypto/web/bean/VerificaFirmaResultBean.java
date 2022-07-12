package it.eng.parer.crypto.web.bean;

import java.io.Serializable;

/**
 * DTO per mappare l'esito del servizio di verifica firma.
 *
 * @author Snidero_L
 */
public class VerificaFirmaResultBean implements Serializable {

    private static final long serialVersionUID = -8962897347751304790L;

    private String reportTree;

    private boolean withErrors = false;

    public String getReportTree() {
        return reportTree;
    }

    public void setReportTree(String reportTree) {
        this.reportTree = reportTree;
    }

    public boolean isWithErrors() {
        return withErrors;
    }

    public void setWithErrors(boolean withErrors) {
        this.withErrors = withErrors;
    }

}
