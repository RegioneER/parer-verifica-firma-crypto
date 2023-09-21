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

package it.eng.parer.crypto.web.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.common.io.Resources;

import it.eng.parer.crypto.model.ParerCRL;
import it.eng.parer.crypto.service.CrlService;

/**
 * Test di unità per il servizio di gestione delle CRL.
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
class CrlTest {

    @Autowired
    private CrlService crlService;

    @Autowired
    private ResourceLoader resourceLoader;

    private static final String CRL_SUBJECT_DN = "CN=Servizio di Certificazione per la Marcatura Temporale,OU=Servizio di certificazione,O=Lombardia Informatica S.p.A.,C=IT";
    private static final String CRL_KEY_ID = "00b1247664016f660863044457f267c3944454dc";

    private static String CRL_UNIQUE_ID = ParerCRL.calcolaUniqueId(CRL_SUBJECT_DN, CRL_KEY_ID);

    @Test
    void testAddCrlByUrl() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:crl/lispa.crl");
        List<String> urls = new ArrayList<>();
        // utilizzo la risorsa da file (nel caso non sia accessibile via rete)
        urls.add(resource.getURL().toString());
        urls.add(
                "ldap://ldap.crs.lombardia.it/cn%3DServizio%20di%20Certificazione%20per%20la%20Marcatura%20Temporale%2Cou%3DServizio%20di%20certificazione%2Co%3DLombardia%20Informatica%20S.p.A.%2Cc%3DIT?certificateRevocationList");
        urls.add("http://ca.lispa.it/TSA/CRL");
        ParerCRL crl = crlService.addCrlByURL(urls);
        assertEquals(CRL_SUBJECT_DN, crl.getSubjectDN());
        assertEquals(CRL_KEY_ID, crl.getKeyId());
        assertEquals(CRL_SUBJECT_DN, crl.getPrincipalName());
    }

    @Test
    void testAddCrlCedacriByUrl() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:crl/cedacri.crl");
        List<String> urls = new ArrayList<>();
        urls.add("http://www.cedacricert.it/crl/crlEU2019.crl");
        urls.add(resource.getURL().toString());
        ParerCRL crl = crlService.addCrlByURL(urls);
        assertEquals("4F2A6C3222EAC18E9DBFC997F49AC05B94F540AA".toLowerCase(), crl.getKeyId());
    }

    @Test
    void testRetrieveCrlByUniqueId() throws IOException {
        // mi serve per popolare la tabella su h2... non è elegante
        testAddCrlByUrl();
        ParerCRL crl = crlService.getCRL(CRL_UNIQUE_ID);
        assertEquals(CRL_SUBJECT_DN, crl.getSubjectDN());
        assertEquals(CRL_KEY_ID, crl.getKeyId());
    }

    @Test
    void testRetrieveCrlByDnAndId() throws IOException {
        // mi serve per popolare la tabella su h2... non è elegante
        testAddCrlByUrl();
        ParerCRL crl = crlService.getCrl(CRL_SUBJECT_DN, CRL_KEY_ID);
        assertEquals(CRL_SUBJECT_DN, crl.getPrincipalName());
    }

    @Test
    void testAddCrlByBlob() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:p7m_b64.xml.p7m.actalis-crl.cer");
        byte[] crlBlob = Resources.toByteArray(resource.getURL());
        crlService.addCRL(crlBlob);
        final String subjectDN = "CN=Actalis S.p.A. - Direzione Commerciale Firma,OU=Certification Service Provider,O=Actalis S.p.A.,C=IT";
        final String authKeyId = "a078dbfacff761773800b7530cac40103111ed4a";
        ParerCRL crl = crlService.getCrl(subjectDN, authKeyId);
        assertNotNull(crl);

    }

}
