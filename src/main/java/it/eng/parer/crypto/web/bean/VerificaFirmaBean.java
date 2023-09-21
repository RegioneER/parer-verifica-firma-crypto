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

package it.eng.parer.crypto.web.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * DTO per la pagina di verifica firma manuale.
 *
 * @author Snidero_L
 */
public class VerificaFirmaBean implements Serializable {

    private static final long serialVersionUID = -4277775905954308303L;

    private boolean abilitaControlloCrl = true;
    private boolean abilitaControlloCatenaTrusted = true;
    private boolean abilitaControlloCa = true;
    private boolean abilitaControlloCrittografico = true;
    // il tag html5 restiutisce la data in formato iso
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataRiferimento;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime oraRiferimento;
    private boolean verificaAllaDataFirma;
    @NotNull(message = "Il file da verificare non può essere vuoto")
    private transient MultipartFile fileDaVerificare;
    private transient List<MultipartFile> marcheDetached;
    private transient List<MultipartFile> firmeDetached;
    private boolean includiRaw = false;

    public boolean isAbilitaControlloCrl() {
        return abilitaControlloCrl;
    }

    public void setAbilitaControlloCrl(boolean abilitaControlloCrl) {
        this.abilitaControlloCrl = abilitaControlloCrl;
    }

    public boolean isAbilitaControlloCatenaTrusted() {
        return abilitaControlloCatenaTrusted;
    }

    public void setAbilitaControlloCatenaTrusted(boolean abilitaControlloCatenaTrusted) {
        this.abilitaControlloCatenaTrusted = abilitaControlloCatenaTrusted;
    }

    public boolean isAbilitaControlloCa() {
        return abilitaControlloCa;
    }

    public void setAbilitaControlloCa(boolean abilitaControlloCa) {
        this.abilitaControlloCa = abilitaControlloCa;
    }

    public boolean isAbilitaControlloCrittografico() {
        return abilitaControlloCrittografico;
    }

    public void setAbilitaControlloCrittografico(boolean abilitaControlloCrittografico) {
        this.abilitaControlloCrittografico = abilitaControlloCrittografico;
    }

    public LocalDate getDataRiferimento() {
        return dataRiferimento;
    }

    public void setDataRiferimento(LocalDate dataRiferimento) {
        this.dataRiferimento = dataRiferimento;
    }

    public LocalTime getOraRiferimento() {
        return oraRiferimento;
    }

    public void setOraRiferimento(LocalTime oraRiferimento) {
        this.oraRiferimento = oraRiferimento;
    }

    public boolean isVerificaAllaDataFirma() {
        return verificaAllaDataFirma;
    }

    public void setVerificaAllaDataFirma(boolean verificaAllaDataFirma) {
        this.verificaAllaDataFirma = verificaAllaDataFirma;
    }

    public MultipartFile getFileDaVerificare() {
        return fileDaVerificare;
    }

    public void setFileDaVerificare(MultipartFile fileDaVerificare) {
        this.fileDaVerificare = fileDaVerificare;
    }

    public List<MultipartFile> getMarcheDetached() {
        return marcheDetached;
    }

    public void setMarcheDetached(List<MultipartFile> marcheDetached) {
        this.marcheDetached = marcheDetached;
    }

    public List<MultipartFile> getFirmeDetached() {
        return firmeDetached;
    }

    public void setFirmeDetached(List<MultipartFile> firmeDetached) {
        this.firmeDetached = firmeDetached;
    }

    public boolean isIncludiRaw() {
        return includiRaw;
    }

    public void setIncludiRaw(boolean includiRaw) {
        this.includiRaw = includiRaw;
    }

}
