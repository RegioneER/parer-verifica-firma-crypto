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

/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package it.eng.parer.crypto.web.util;

public class EndPointCostants {

    private EndPointCostants() {
	throw new IllegalStateException("Utility class");
    }

    public static final String URL_ADMIN_BASE = "/admin";
    public static final String URL_API_BASE = "/api";

    public static final String RESOURCE_ERRORS = "/errors";
    public static final String RESOURCE_REPORT_VERIFICA = "/report-verifica";
    public static final String RESOURCE_FILEXML = "/file-xml";
    public static final String RESOURCE_UNSIGNED_P7M = "/unsigned-p7m";
    public static final String RESOURCE_INFOS = "/infos";
    public static final String RESOURCE_CERTIFICATE = "/certificate";
    public static final String RESOURCE_CRL = "/crl";
    public static final String RESOURCE_TST = "/tst";
    public static final String RESOURCE_TSD = "/tsd";

    public static final String URL_APP_INFOS = URL_API_BASE + RESOURCE_INFOS;
    public static final String URL_REPORT_VERIFICA = URL_API_BASE + RESOURCE_REPORT_VERIFICA;
    public static final String URL_FILEXML = URL_API_BASE + RESOURCE_FILEXML;
    public static final String URL_UNSIGNEDP7M = URL_API_BASE + RESOURCE_UNSIGNED_P7M;
    public static final String URL_CRL = URL_API_BASE + RESOURCE_CRL;
    public static final String URL_TSD = URL_API_BASE + RESOURCE_TSD;
    public static final String URL_TST = URL_API_BASE + RESOURCE_TST;
    public static final String URL_ERRORS = URL_API_BASE + RESOURCE_ERRORS;

    // ROLES
    public static final String ROLE_ADMIN = "ADMIN";

    // eTAG
    public static final String ETAG_RV10 = "RVv1.0";

}
