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
