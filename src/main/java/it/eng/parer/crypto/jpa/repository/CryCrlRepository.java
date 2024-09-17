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

package it.eng.parer.crypto.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.eng.parer.crypto.jpa.entity.CryCrl;
import it.eng.parer.crypto.jpa.entity.CryCrlPK;
import java.util.Optional;

public interface CryCrlRepository extends JpaRepository<CryCrl, CryCrlPK> {

    /**
     * Ottieni la CRL identificata dall'id univoco.
     *
     * @param uniqueId
     *            md5 di subjectdn + keyId;
     *
     * @return Entity della CRL
     */
    public Optional<CryCrl> findByUniqueId(String uniqueId);

}
