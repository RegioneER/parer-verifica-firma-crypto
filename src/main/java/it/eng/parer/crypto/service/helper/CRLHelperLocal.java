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

package it.eng.parer.crypto.service.helper;

import java.security.cert.X509CRL;

import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.storage.ICRLStorage;

/**
 * Specializzazione dell'interfaccia utilizzata dalla cryptolibrary.
 *
 * @author Snidero_L
 */
public interface CRLHelperLocal extends ICRLStorage {

    /**
     * Ottieni la CRL utilizzando l'id univoco che viene generato prima di persistere l'entità.
     *
     * @param uniqueId
     *            MD5 di subjectdn + keyId;
     * 
     * @return CRL (se esiste)
     * 
     * @throws CryptoStorageException
     *             in caso di errore.
     */
    public X509CRL getByUniqueId(String uniqueId) throws CryptoStorageException;

}
