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

package it.eng.parer.crypto.service.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
