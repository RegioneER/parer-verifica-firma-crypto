package it.eng.parer.crypto.service.model;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author Snidero_L
 */
public class CryptoDataToValidateFile implements Serializable {

    private static final long serialVersionUID = 6602088266356805755L;

    private String nome;
    private File contenuto;

    public CryptoDataToValidateFile() {

    }

    public CryptoDataToValidateFile(String nome, File contenuto) {
        this.nome = nome;
        this.contenuto = contenuto;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public File getContenuto() {
        return contenuto;
    }

    public void setContenuto(File contenuto) {
        this.contenuto = contenuto;
    }

}
