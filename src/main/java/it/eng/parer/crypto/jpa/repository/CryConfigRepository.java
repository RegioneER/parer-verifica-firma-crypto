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

package it.eng.parer.crypto.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.eng.parer.crypto.jpa.entity.CryConfig;
import it.eng.parer.crypto.jpa.entity.CryConfigPK;

public interface CryConfigRepository extends JpaRepository<CryConfig, CryConfigPK> {

    @Query("select conf.subjectdn, conf.crlurl, conf.niOrdUrlDistribCrl, conf.keyId "
	    + "from CryConfig conf join CryCrl crl on (conf.subjectdn = crl.subjectdn) "
	    + "where crl.nextExpiration <  SYSDATE() order by conf.subjectdn, conf.niOrdUrlDistribCrl")
    public List<Object[]> joinWithCryCrl();

}
