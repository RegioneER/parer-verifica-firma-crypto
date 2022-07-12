package it.eng.parer.crypto.service.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Modello per effettuare la validazione di una firma.
 *
 * @author Snidero_L
 */
public class CryptoDataToValidateData implements Serializable {

    private static final long serialVersionUID = 6857282680348085868L;

    @Valid
    @NotNull(message = "Il documento principale deve essere valorizzato")
    private CryptoDataToValidateFile contenuto;

    private List<CryptoDataToValidateFile> sottoComponentiFirma = new ArrayList<>();

    private List<CryptoDataToValidateFile> sottoComponentiMarca = new ArrayList<>();

    public CryptoDataToValidateFile getContenuto() {
        return contenuto;
    }

    public void setContenuto(CryptoDataToValidateFile contenuto) {
        this.contenuto = contenuto;
    }

    public List<CryptoDataToValidateFile> getSottoComponentiFirma() {
        return sottoComponentiFirma;
    }

    public void setSottoComponentiFirma(List<CryptoDataToValidateFile> sottoComponentiFirma) {
        this.sottoComponentiFirma = sottoComponentiFirma;
    }

    public List<CryptoDataToValidateFile> getSottoComponentiMarca() {
        return sottoComponentiMarca;
    }

    public void setSottoComponentiMarca(List<CryptoDataToValidateFile> sottoComponentiMarca) {
        this.sottoComponentiMarca = sottoComponentiMarca;
    }

    public boolean addSottoComponenteFirma(CryptoDataToValidateFile sottoComponenteFirma) {
        return this.sottoComponentiFirma.add(sottoComponenteFirma);
    }

    public boolean addSottoComponenteMarca(CryptoDataToValidateFile sottoComponenteMarca) {
        return this.sottoComponentiMarca.add(sottoComponenteMarca);
    }

}
