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

import java.io.InputStream;
import java.io.Serializable;

public class CryptoP7mUnsigned implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private String fileType;
    private transient InputStream data;

    public CryptoP7mUnsigned(String fileName, String fileType, InputStream data) {
	this.fileName = fileName;
	this.fileType = fileType;
	this.data = data;
    }

    public String getFileName() {
	return fileName;
    }

    public String getFileType() {
	return fileType;
    }

    public InputStream getData() {
	return data;
    }
}
