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

package it.eng.parer.crypto.web.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.common.io.Resources;

import it.eng.parer.crypto.service.CertificateService;

/**
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = {
	"cron.ca.enable=false", "cron.crl.enable=false",
	"spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1",
	"logging.level.root=INFO", "logging.level.it.eng.parer.crypto=INFO" })
class CertificateTest {

    private static final String CER_KEY_ID = "6109d2577f03a3d4b1ab9a18cc1333fbd793cee0";
    private static final String CER_SUBJECT_DN = "CN=Servizio di Certificazione per la Firma Digitale - CA2,OU=Servizio di certificazione,O=Lombardia Informatica S.p.A.,C=IT";

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private CertificateService certificateService;

    @Test
    void testBase64() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:firmatario-test.cer");
	byte[] firmatarioBlob = Resources.toByteArray(resource.getURL());
	String encodeToString = Base64.getEncoder().encodeToString(firmatarioBlob);
	byte[] decode = Base64.getDecoder().decode(encodeToString);
	assertEquals(firmatarioBlob.length, decode.length);
    }

    @Test
    void testMimeBase64() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:firmatario-test.cer");
	byte[] firmatarioBlob = Resources.toByteArray(resource.getURL());
	String encodeToString = Base64.getMimeEncoder().encodeToString(firmatarioBlob);
	byte[] decode = Base64.getMimeDecoder().decode(encodeToString);
	assertEquals(firmatarioBlob.length, decode.length);
    }

    @Test
    void testUrlBase64() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:firmatario-test.cer");
	byte[] firmatarioBlob = Resources.toByteArray(resource.getURL());
	String encodeToString = Base64.getUrlEncoder().encodeToString(firmatarioBlob);
	byte[] decode = Base64.getUrlDecoder().decode(encodeToString);
	assertEquals(firmatarioBlob.length, decode.length);
    }

    @Test
    void testAuthKeyId() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:firmatario-test.cer");
	byte[] firmatarioBlob = Resources.toByteArray(resource.getURL());

	String keyId = certificateService.getCertificateKeyId(firmatarioBlob);
	assertEquals(CER_KEY_ID, keyId);
    }

    @Test
    void testAuthSubjectDN() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:firmatario-test.cer");
	byte[] firmatarioBlob = Resources.toByteArray(resource.getURL());

	String subjectDN = certificateService.getCertificateSubjectDN(firmatarioBlob);
	assertEquals(CER_SUBJECT_DN, subjectDN);
    }

    @Test
    void testAddCA() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:ca-blob.cer");
	byte[] caBlob = Resources.toByteArray(resource.getURL());
	certificateService.addCaCertificate(caBlob);

	final String certSubjectDN = "CN=InfoCert Firma Qualificata,OU=Certificatore Accreditato,SERIALNUMBER=07945211006,O=INFOCERT SPA,C=IT";
	final String certKeyId = "30fc217c7cd27c6dbc8cc3ba1350f77aa02bc5b6";

	boolean result = certificateService.existsCaCertificate(certSubjectDN, certKeyId);
	assertTrue(result);
    }
}
