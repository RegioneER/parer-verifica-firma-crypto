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

package it.eng.parer.crypto.web.integration;

import static it.eng.parer.crypto.web.util.EndPointCostants.URL_CRL;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_ERRORS;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_FILEXML;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_REPORT_VERIFICA;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_TSD;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_TST;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_UNSIGNEDP7M;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.io.Resources;

import it.eng.crypto.data.type.SignerType;
import it.eng.crypto.utils.VerificheEnums;
import it.eng.parer.crypto.model.CryptoEnums;
import it.eng.parer.crypto.model.CryptoSignedP7mUri;
import it.eng.parer.crypto.model.ParerCRL;
import it.eng.parer.crypto.model.ParerTSD;
import it.eng.parer.crypto.model.ParerTST;
import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.model.exceptions.ParerErrorDoc;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidate;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateBody;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateDataUri;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadataFile;
import it.eng.parer.crypto.model.verifica.input.CryptoDocumentoVersato;
import it.eng.parer.crypto.model.verifica.input.CryptoProfiloVerifica;
import it.eng.parer.crypto.model.verifica.input.TipologiaDataRiferimento;
import it.eng.parer.crypto.service.CertificateService;
import it.eng.parer.crypto.service.CrlService;
import it.eng.parer.crypto.web.Util;

/**
 * Bozza per i test di integrazione sull'applicazione.
 *
 * @author Snidero_L
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
	"cron.ca.enable=false", "cron.crl.enable=false",
	"spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1",
	"logging.level.root=DEBUG", "logging.level.it.eng.parer.crypto=INFO",
	"logging.level.org.springframework.web.client.RestTemplate=DEBUG" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CrlService crlService;

    private String uriWithCtxPath;

    /**
     * Aggiungo manualmente alcuni dei certificati CA/CRL che mi servono per verificare le firme. Il
     * database delle CA e CRL è vuoto inizialmente.
     *
     * @throws IOException nel caso non trovi il file
     */
    @BeforeEach
    void fillTrustedCADatabase() throws IOException {
	Resource resource = resourceLoader.getResource("classpath:ca-blob.cer");
	Resource tsaInfoCertResource = resourceLoader
		.getResource("classpath:tsa/inforcert-tsa-ca.cer");
	Resource actalisCaResource = resourceLoader
		.getResource("classpath:p7m_b64.xml.p7m.actalis-ca.cer");
	Resource actalisCRLResource = resourceLoader
		.getResource("classpath:p7m_b64.xml.p7m.actalis-crl.cer");
	Resource postecomCaResource = resourceLoader
		.getResource("classpath:firma.tsr.p7m.postecom-ca.cer");
	Resource postecomCRLResource = resourceLoader
		.getResource("classpath:firma.tsr.p7m.postecom-crl.cer");
	Resource arubaCaResource = resourceLoader
		.getResource("classpath:cadesBES_Controfirma_CadesT-ca.cer");
	Resource arubaTsaResource = resourceLoader
		.getResource("classpath:cadesBES_Controfirma_CadesT-tsa.cer");
	Resource telecomCaResource = resourceLoader
		.getResource("classpath:cades+cades_crl_non_conforme.pdf.p7m-arubapec-ca.cer");
	Resource arubapecCaResource = resourceLoader
		.getResource("classpath:cades+cades_crl_non_conforme.pdf.p7m-telecom-ca.cer");
	Resource actalisXadesCaResource = resourceLoader
		.getResource("classpath:xades_con_signeddataobjectproperties.xml-actalis-ca.cer");
	Resource postecertCaResource = resourceLoader
		.getResource("classpath:cades_bes.pdf.p7m-postecert-ca.cer");
	Resource infocertStranoCaResource = resourceLoader
		.getResource("classpath:pades-infocert-ca-strano.cer");
	Resource multicertifyActalisTsaResource = resourceLoader
		.getResource("classpath:tsa/test_non_firma.xml.tsa.cer");
	Resource multicertifyActalisCaResource = resourceLoader
		.getResource("classpath:test_non_firma.xml.ca.cer");
	Resource multicertifyActalisCRLResource = resourceLoader
		.getResource("classpath:test_non_firma.xml.crl.cer");
	Resource p7mmd5Resource = resourceLoader.getResource("classpath:p7m_md5.pdf.p7m.cer");
	Resource pdfFirmeMultipleErroreCrittoCRLResource = resourceLoader
		.getResource("classpath:testPdfFirmeMultipleErroreCritto-crl.crl");

	byte[] caBlob = Resources.toByteArray(resource.getURL());
	byte[] tsaInfoCertBlob = Resources.toByteArray(tsaInfoCertResource.getURL());
	byte[] actalisCaBlob = Resources.toByteArray(actalisCaResource.getURL());
	byte[] actalisCrlBlob = Resources.toByteArray(actalisCRLResource.getURL());
	byte[] postecomCaBlob = Resources.toByteArray(postecomCaResource.getURL());
	byte[] postecomCrlBlob = Resources.toByteArray(postecomCRLResource.getURL());
	byte[] arubaCaBlob = Resources.toByteArray(arubaCaResource.getURL());
	byte[] arubaTsaBlob = Resources.toByteArray(arubaTsaResource.getURL());
	byte[] telecomCaBlob = Resources.toByteArray(telecomCaResource.getURL());
	byte[] arubapecCaBlob = Resources.toByteArray(arubapecCaResource.getURL());
	byte[] actalisXadesCaBlob = Resources.toByteArray(actalisXadesCaResource.getURL());
	byte[] postecertCaBlob = Resources.toByteArray(postecertCaResource.getURL());
	byte[] infocertStranoCaBlob = Resources.toByteArray(infocertStranoCaResource.getURL());
	byte[] multicertifyActalisTsaResourceTsaBlob = Resources
		.toByteArray(multicertifyActalisTsaResource.getURL());
	byte[] multicertifyActalisTsaResourceCaBlob = Resources
		.toByteArray(multicertifyActalisCaResource.getURL());
	byte[] multicertifyActalisCRLResourceBlob = Resources
		.toByteArray(multicertifyActalisCRLResource.getURL());
	byte[] p7mmd5ResourceBlob = Resources.toByteArray(p7mmd5Resource.getURL());
	byte[] pdfFirmeMultipleErroreCrittoCRLBlob = Resources
		.toByteArray(pdfFirmeMultipleErroreCrittoCRLResource.getURL());

	certificateService.addCaCertificate(caBlob);
	certificateService.addCaCertificate(tsaInfoCertBlob);
	certificateService.addCaCertificate(actalisCaBlob);
	crlService.addCRL(actalisCrlBlob);
	certificateService.addCaCertificate(postecomCaBlob);
	crlService.addCRL(postecomCrlBlob);
	certificateService.addCaCertificate(arubaCaBlob);
	certificateService.addCaCertificate(arubaTsaBlob);
	certificateService.addCaCertificate(telecomCaBlob);
	certificateService.addCaCertificate(arubapecCaBlob);
	certificateService.addCaCertificate(actalisXadesCaBlob);
	certificateService.addCaCertificate(postecertCaBlob);
	certificateService.addCaCertificate(infocertStranoCaBlob);
	certificateService.addCaCertificate(multicertifyActalisTsaResourceTsaBlob);
	certificateService.addCaCertificate(multicertifyActalisTsaResourceCaBlob);
	certificateService.addCaCertificate(p7mmd5ResourceBlob);
	crlService.addCRL(multicertifyActalisCRLResourceBlob);
	crlService.addCRL(pdfFirmeMultipleErroreCrittoCRLBlob);

	// define base URL
	final String rootUri = restTemplate.getRootUri();
	uriWithCtxPath = rootUri.concat(rootUri.endsWith("/") ? StringUtils.EMPTY : "/"); // complete
											  // URL
    }

    @Test
    void testGetErrorDocument() {

	ResponseEntity<ParerErrorDoc> entity = restTemplate
		.getForEntity(URL_ERRORS + "/unhandled-exception", ParerErrorDoc.class);
	ParerErrorDoc errorDocument = entity.getBody();
	assertNotNull(errorDocument.getDescription());
	assertEquals(ParerError.ErrorCode.GENERIC_ERROR.urlFriendly(), errorDocument.getCode());
    }

    /**
     * Analogo al test precedente ma utilizzando il multipart (endpoint /api)
     *
     * @throws IOException
     */
    @Test
    void testVerificaFirmaMultipart() throws IOException {
	File fileFirmato = ResourceUtils
		.getFile("classpath:firme/xml_sig_controfirma_cert_rev.xml");

	FileSystemResource fileFirmatoRes = new FileSystemResource(fileFirmato);
	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();

	Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
	metadata.setTipologiaDataRiferimento(
		TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("metadati", metadata);
	body.add("contenuto", fileFirmatoRes);

	HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

	CryptoAroCompDoc componente = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);

	Util.assertNumeroDiFirmeOK(componente, 2);
	Util.assertNumeroDiMarcheOK(componente, 0);
	Util.assertNumeroDiBusteOK(componente, 1);
	Util.assertNumeroDiControfirmeOK(componente, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XML_DSIG,
		Util.XMLSHA256RSA, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.XML_DSIG,
		Util.XMLSHA256RSA, 1);
	Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoControlli.CRL,
		VerificheEnums.EsitoControllo.CERTIFICATO_REVOCATO);
	Util.assertControlliMarcheOK(componente);
	Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
	Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
		VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);

    }

    /**
     * Analogo al test precedente ma utilizzando il multipart (endpoint /api)
     *
     * @throws IOException
     */
    @Test
    void testVerificaFirmaMultipartP7m() throws IOException {
	File fileFirmato = ResourceUtils.getFile("classpath:firme/cades_bes.pdf.p7m");

	FileSystemResource fileFirmatoRes = new FileSystemResource(fileFirmato);
	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();

	Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
	metadata.setTipologiaDataRiferimento(
		TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("metadati", metadata);
	body.add("contenuto", fileFirmatoRes);

	HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

	CryptoAroCompDoc componente = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);

	Util.assertNumeroDiFirmeOK(componente, 1);
	Util.assertNumeroDiMarcheOK(componente, 0);
	Util.assertNumeroDiBusteOK(componente, 1);
	Util.assertNumeroDiControfirmeOK(componente, 0);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES,
		Util.SHA256RSA, 1);
	Util.assertControlliFirmeOK(componente);
	Util.assertControlliMarcheOK(componente);
	Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);

    }

    /**
     * Analogo al test precedente ma passando un file binario
     *
     * @throws IOException
     */
    @Test
    void testVerificaFirmaMultipartNoMetadata() throws IOException {

	File fileFirmato = ResourceUtils
		.getFile("classpath:firme/xml_sig_controfirma_cert_rev.xml");

	FileSystemResource fileFirmatoRes = new FileSystemResource(fileFirmato);

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("contenuto", fileFirmatoRes);

	HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

	CryptoAroCompDoc componente = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);

	Util.assertNumeroDiFirmeOK(componente, 2);
	Util.assertNumeroDiMarcheOK(componente, 0);
	Util.assertNumeroDiBusteOK(componente, 1);
	Util.assertNumeroDiControfirmeOK(componente, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XML_DSIG,
		Util.XMLSHA256RSA, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.XML_DSIG,
		Util.XMLSHA256RSA, 1);
	Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoControlli.CRL, VerificheEnums.EsitoControllo.NON_NECESSARIO);
	Util.assertControlliMarcheOK(componente);

    }

    @Test
    void testVerificaFirmaMultipartTimestampDetached() throws Exception {
	File fileFirmato = ResourceUtils.getFile("classpath:firme/cades_T_1.pdf.p7m");

	FileSystemResource fileFirmatoRes = new FileSystemResource(fileFirmato);

	File marcaDetached = ResourceUtils.getFile("classpath:firme/cades_T_1.pdf.p7m.tsr");

	FileSystemResource marcaDetachedRes = new FileSystemResource(marcaDetached);

	List<FileSystemResource> marcheDetachedList = new ArrayList<>();

	marcheDetachedList.add(marcaDetachedRes);

	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();
	metadata.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());
	metadata.setComponentePrincipale(new CryptoDataToValidateMetadataFile("cades_t1.p7m"));
	metadata.setSottoComponentiMarca(Arrays.asList(new CryptoDataToValidateMetadataFile[] {
		new CryptoDataToValidateMetadataFile("cades_t1.tsr") }));
	metadata.setUuid("testVerificaFirmaMultipartTimestampDetached");

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("metadati", metadata);
	body.add("contenuto", fileFirmatoRes);
	body.add("marche", marcaDetachedRes);

	HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
	CryptoAroCompDoc componente = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);

	Util.assertNumeroDiFirmeOK(componente, 3);
	Util.assertNumeroDiMarcheOK(componente, 2);

	Util.assertNumeroDiBusteOK(componente, 2);

	Util.assertNumeroDiControfirmeOK(componente, 0);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T,
		Util.SHA256RSA, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES,
		Util.SHA256RSA, 2);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES,
		Util.SHA256RSA, 2);
	Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSR,
		Util.SHA1RSA, 1);
	Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.CADES_T,
		Util.SHA256RSA, 1);

	Util.assertControlliFirmeOK(componente);
	Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(1));

	/*
	 * Se non passo i metadati il valore predefinito è il seguente: - contenuto per il file
	 * principale - firma_0 ... firma_n-1 per le firme detached - marca_0 ... marca_n-1 per le
	 * marche detached
	 */
	assertEquals("cades_t1.p7m", componente.getAroMarcaComps().get(1).getIdMarca());
	assertEquals("cades_t1.tsr", componente.getAroMarcaComps().get(0).getIdMarca());

	Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoRifTemporale.MT_VERS_NORMA,
		componente.getAroMarcaComps().get(1).getTmMarcaTemp());
    }

    @Test
    void testVerificaFirmaMultipartTimestampDetachedNoMetadata() throws Exception {

	File fileFirmato = ResourceUtils.getFile("classpath:firme/cades_T_1.pdf.p7m");

	FileSystemResource fileFirmatoRes = new FileSystemResource(fileFirmato);

	File marcaDetached = ResourceUtils.getFile("classpath:firme/cades_T_1.pdf.p7m.tsr");

	FileSystemResource marcaDetachedRes = new FileSystemResource(marcaDetached);

	List<FileSystemResource> marcheDetachedList = new ArrayList<>();

	marcheDetachedList.add(marcaDetachedRes);

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

	body.add("contenuto", fileFirmatoRes);
	body.add("marche", marcaDetachedRes);

	HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
	CryptoAroCompDoc componente = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);

	Util.assertNumeroDiFirmeOK(componente, 3);
	Util.assertNumeroDiMarcheOK(componente, 2);

	Util.assertNumeroDiBusteOK(componente, 2);

	Util.assertNumeroDiControfirmeOK(componente, 0);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T,
		Util.SHA256RSA, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES,
		Util.SHA256RSA, 2);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES,
		Util.SHA256RSA, 2);
	Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSR,
		Util.SHA1RSA, 1);
	Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.CADES_T,
		Util.SHA256RSA, 1);

	Util.assertControlliFirmeOK(componente);
	Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(1));

	/*
	 * Se non passo i metadati il valore predefinito è il seguente: - contenuto per il file
	 * principale - firma_0 ... firma_n-1 per le firme detached - marca_0 ... marca_n-1 per le
	 * marche detached
	 */
	assertEquals("contenuto", componente.getAroMarcaComps().get(1).getIdMarca());
	assertEquals("marca_0", componente.getAroMarcaComps().get(0).getIdMarca());

	Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoRifTemporale.MT_VERS_NORMA,
		componente.getAroMarcaComps().get(1).getTmMarcaTemp());

    }

    @Test
    void testInputIncoerente() throws FileNotFoundException {
	File fileFirmato = ResourceUtils.getFile("classpath:firme/cades_T_1.pdf.p7m");

	FileSystemResource fileFirmatoRes = new FileSystemResource(fileFirmato);

	File marcaDetached = ResourceUtils.getFile("classpath:firme/cades_T_1.pdf.p7m.tsr");

	FileSystemResource marcaDetachedRes = new FileSystemResource(marcaDetached);

	List<FileSystemResource> marcheDetachedList = new ArrayList<>();

	marcheDetachedList.add(marcaDetachedRes);

	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();
	metadata.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());
	metadata.setComponentePrincipale(new CryptoDataToValidateMetadataFile("cades_t1.p7m"));
	metadata.setSottoComponentiMarca(Arrays.asList(new CryptoDataToValidateMetadataFile[] {
		new CryptoDataToValidateMetadataFile("cades_t1.tsr") }));
	metadata.setUuid("testInputIncoerente");

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("metadati", metadata);
	body.add("contenuto", fileFirmatoRes);
	body.add("marche", marcaDetachedRes);
	body.add("marche", marcaDetachedRes);

	HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());

	assertThrows(CryptoParerException.class, () -> {
	    restTemplate.postForObject(URL_REPORT_VERIFICA, entity, CryptoAroCompDoc.class);
	});
    }

    @Test
    void testVerificaFirmaJson() {

	CryptoDataToValidateDataUri data = new CryptoDataToValidateDataUri();
	data.setContenuto(URI.create(uriWithCtxPath + "cades_T_1.pdf.p7m"));
	data.setMarche(Arrays.asList(URI.create(uriWithCtxPath + "cades_T_1.pdf.p7m.tsr")));

	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();
	metadata.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());
	metadata.setComponentePrincipale(new CryptoDataToValidateMetadataFile("cades_t1.p7m"));
	metadata.setSottoComponentiMarca(Arrays.asList(new CryptoDataToValidateMetadataFile[] {
		new CryptoDataToValidateMetadataFile("cades_t1.tsr") }));
	metadata.setUuid("testVerificaFirmaJson");

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	CryptoDataToValidateBody body = new CryptoDataToValidateBody();
	body.setData(data);
	body.setMetadata(metadata);

	HttpEntity<CryptoDataToValidateBody> entity = new HttpEntity<>(body, headers);

	CryptoAroCompDoc componente = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);

	Util.assertNumeroDiFirmeOK(componente, 3);
	Util.assertNumeroDiMarcheOK(componente, 2);

	Util.assertNumeroDiBusteOK(componente, 2);

	Util.assertNumeroDiControfirmeOK(componente, 0);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T,
		Util.SHA256RSA, 1);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES,
		Util.SHA256RSA, 2);
	Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES,
		Util.SHA256RSA, 2);
	Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSR,
		Util.SHA1RSA, 1);
	Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.CADES_T,
		Util.SHA256RSA, 1);

	Util.assertControlliFirmeOK(componente);
	Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(1));

	/*
	 * Se non passo i metadati il valore predefinito è il seguente: - contenuto per il file
	 * principale - firma_0 ... firma_n-1 per le firme detached - marca_0 ... marca_n-1 per le
	 * marche detached
	 */
	assertEquals("cades_t1.p7m", componente.getAroMarcaComps().get(1).getIdMarca());
	assertEquals("cades_t1.tsr", componente.getAroMarcaComps().get(0).getIdMarca());

	Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
		VerificheEnums.TipoRifTemporale.MT_VERS_NORMA,
		componente.getAroMarcaComps().get(1).getTmMarcaTemp());
    }

    @Test
    void testConfigurazioneNonCoerente() throws IOException {
	Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_pem_sha256.pdf.p7m");
	CryptoDataToValidate input = new CryptoDataToValidate();
	input.setContenuto(
		new CryptoDocumentoVersato("P7M_2", Resources.toByteArray(fileFirmato.getURL())));

	TipologiaDataRiferimento configurazioneNonCoerente = new TipologiaDataRiferimento(
		CryptoEnums.TipoRifTemporale.RIF_TEMP_VERS, true, 42L);

	input.setTipologiaDataRiferimento(configurazioneNonCoerente);

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoDataToValidate> entity = new HttpEntity<>(input, headers);

	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	Assertions.assertThrows(CryptoParerException.class, () -> {
	    restTemplate.postForObject(URL_REPORT_VERIFICA, entity, CryptoAroCompDoc.class);
	});

    }

    @Test
    void testConfigurazioneNulla() throws IOException {
	Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_pem_sha256.pdf.p7m");
	CryptoDataToValidate input = new CryptoDataToValidate();
	input.setContenuto(
		new CryptoDocumentoVersato("P7M_3", Resources.toByteArray(fileFirmato.getURL())));
	input.setTipologiaDataRiferimento(null);

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoDataToValidate> entity = new HttpEntity<>(input, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	assertThrows(CryptoParerException.class, () -> {
	    restTemplate.postForObject(URL_REPORT_VERIFICA, entity, CryptoAroCompDoc.class);
	});
    }

    @Test
    void testProfiloDefault() {

	CryptoDataToValidateDataUri data = new CryptoDataToValidateDataUri();
	data.setContenuto(URI.create(uriWithCtxPath + "p7m_pem_sha256.pdf.p7m"));

	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();

	CryptoProfiloVerifica profiloVerifica = CryptoProfiloVerifica.profiloDefault();
	metadata.setProfiloVerifica(profiloVerifica);

	CryptoDataToValidateBody body = new CryptoDataToValidateBody();
	body.setData(data);
	body.setMetadata(metadata);

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoDataToValidateBody> entity = new HttpEntity<>(body, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	CryptoAroCompDoc postForObject = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);
	assertEquals(profiloVerifica, postForObject.getProfiloValidazione());

    }

    @Test
    void testProfiloCustom() {

	CryptoDataToValidateDataUri data = new CryptoDataToValidateDataUri();
	data.setContenuto(URI.create(uriWithCtxPath + "p7m_pem_sha256.pdf.p7m"));

	CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();

	CryptoProfiloVerifica profiloVerificaCustom = new CryptoProfiloVerifica();
	profiloVerificaCustom.setControlloCatenaTrustAbilitato(false);
	profiloVerificaCustom.setControlloCertificatoAbilitato(false);
	profiloVerificaCustom.setControlloCrittograficoAbilitato(true);
	profiloVerificaCustom.setControlloCrlAbilitato(true);
	metadata.setProfiloVerifica(profiloVerificaCustom);

	CryptoDataToValidateBody body = new CryptoDataToValidateBody();
	body.setData(data);
	body.setMetadata(metadata);

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoDataToValidateBody> entity = new HttpEntity<>(body, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	CryptoAroCompDoc postForObject = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
		CryptoAroCompDoc.class);
	assertEquals(profiloVerificaCustom, postForObject.getProfiloValidazione());

    }

    @Test
    void configurazioneDataRiferimentoNonCoerente() throws IOException {
	Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_pem_sha256.pdf.p7m");
	CryptoDataToValidate input = new CryptoDataToValidate();
	input.setContenuto(
		new CryptoDocumentoVersato("P7M_3", Resources.toByteArray(fileFirmato.getURL())));
	input.setTipologiaDataRiferimento(new TipologiaDataRiferimento(
		CryptoEnums.TipoRifTemporale.RIF_TEMP_VERS, true, 42L));
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoDataToValidate> entity = new HttpEntity<>(input, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());

	CryptoParerException ex = assertThrows(CryptoParerException.class, () -> {
	    restTemplate.postForObject(URL_REPORT_VERIFICA, entity, CryptoAroCompDoc.class);
	});
	assertEquals(ParerError.ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    @Test
    void testValidationError() {
	CryptoDataToValidate input = new CryptoDataToValidate();
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoDataToValidate> entity = new HttpEntity<>(input, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());

	CryptoParerException ex = assertThrows(CryptoParerException.class, () -> {
	    restTemplate.postForObject(URL_REPORT_VERIFICA, entity, CryptoAroCompDoc.class);
	});
	assertEquals(ParerError.ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    @Test
    @Deprecated
    @Disabled(value = "API da dismettere")
    void testTimestamp() {
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	byte[] fileVerSerie = {
		67, 105, 97, 111, 33 };

	Resource resource = new ByteArrayResource(fileVerSerie) {
	    @Override
	    public String getFilename() {
		return "testTst.txt";
	    }
	};

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("description", "TEST-TEST");
	body.add("file", resource);
	HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

	ParerTST postForObject = restTemplate.postForObject(URL_TST, requestEntity, ParerTST.class);

	assertNotNull(postForObject.getTimeStampInfo().getGenTime());

    }

    @Test
    @Deprecated
    @Disabled(value = "API da dismettere")
    void testTSD() {
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	byte[] fileVerSerie = {
		67, 105, 97, 111, 33 };

	Resource resource = new ByteArrayResource(fileVerSerie) {
	    @Override
	    public String getFilename() {
		return "testTsd.txt";
	    }
	};

	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	body.add("description", "TEST-TSD");
	body.add("file", resource);
	HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
	ParerTSD postForObject = restTemplate.postForObject(URL_TSD, requestEntity, ParerTSD.class);

	assertNotNull(postForObject.getTimeStampTokens()[0].getTimeStampInfo().getGenTime());

    }

    @Test
    void testXmlExtraction() throws IOException {
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	Resource resource = resourceLoader.getResource("classpath:firme/p7m_b64.xml.p7m");
	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

	body.add("xml-p7m", resource);
	HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
	String actualValue = restTemplate.postForObject(URL_FILEXML, requestEntity, String.class);

	Resource resourceSbustato = resourceLoader
		.getResource("classpath:firme/p7m_b64_sbustato.xml");
	byte[] readAllBytes = Files.readAllBytes(resourceSbustato.getFile().toPath());
	String expectedValue = new String(readAllBytes);

	assertEquals(expectedValue, actualValue);

    }

    @Test
    void testP7mExtractionMultipart() throws IOException {
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	Resource resource = resourceLoader.getResource("classpath:firme/p7m_pem_sha256.pdf.p7m");
	MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

	body.add("signed-p7m", resource);
	HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	Resource actualValue = restTemplate.postForObject(URL_UNSIGNEDP7M, requestEntity,
		Resource.class);

	Resource resourceSbustato = resourceLoader
		.getResource("classpath:firme/p7m_pem_sha256_sbustato.pdf");
	byte[] readAllBytes = Files.readAllBytes(resourceSbustato.getFile().toPath());

	assertEquals(DatatypeConverter.printBase64Binary(readAllBytes), DatatypeConverter
		.printBase64Binary(IOUtils.toByteArray(actualValue.getInputStream())));

    }

    @Test
    void testP7mExtractionJson() throws IOException {

	CryptoSignedP7mUri data = new CryptoSignedP7mUri();
	data.setUri(URI.create(uriWithCtxPath + "cades_T_1.pdf.p7m"));

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoSignedP7mUri> requestEntity = new HttpEntity<>(data, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	Resource actualValue = restTemplate.postForObject(URL_UNSIGNEDP7M, requestEntity,
		Resource.class);

	Resource resourceSbustato = resourceLoader
		.getResource("classpath:firme/cades_T_1_sbustato.pdf");
	byte[] readAllBytes = Files.readAllBytes(resourceSbustato.getFile().toPath());

	assertEquals(DatatypeConverter.printBase64Binary(readAllBytes), DatatypeConverter
		.printBase64Binary(IOUtils.toByteArray(actualValue.getInputStream())));

    }

    @Test
    void testP7mExtractionNotValidJson() {

	CryptoSignedP7mUri data = new CryptoSignedP7mUri();

	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	HttpEntity<CryptoSignedP7mUri> requestEntity = new HttpEntity<>(data, headers);
	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());

	CryptoParerException ex = assertThrows(CryptoParerException.class, () -> {
	    restTemplate.postForObject(URL_UNSIGNEDP7M, requestEntity, Resource.class);
	});
	assertEquals(ParerError.ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    /**
     * Effettuo un test sul recupero della CRL del firmatario. Partendo da un db vuoto ho la
     * necessità di inserire prima la CRL che sto cercando. Essendo un test di integrazione lo
     * effettuo tramite le API fornite.
     */
    @Disabled(value = "API da dismettere")
    @Test
    @Deprecated
    void testSearchCrl() throws IOException {
	// Fase 1: inserisco la CRL refernziata dal firmatario
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	ObjectMapper mapper = new ObjectMapper();
	ArrayNode createArrayNode = mapper.createArrayNode();
	createArrayNode.add(
		"ldap://ldap.crs.lombardia.it/cn%3DCDP2%2Cou%3DCRL%20CA%20Firma%20Digitale%202%2Co%3DLombardia%20Informatica%20S.p.A.%2Cc%3DIT?certificateRevocationList");
	createArrayNode.add("http://ca.lispa.it/CAFD2/CDP2");

	HttpEntity<String> requestEntity = new HttpEntity<String>(createArrayNode.toString(),
		headers);

	restTemplate.getRestTemplate().setErrorHandler(new CryptoErrorHandler());
	final ParerCRL expectedCrl = restTemplate.postForObject(URL_CRL, requestEntity,
		ParerCRL.class);

	// Fase 2: effettuo la ricerca utilizzando il certificato del firmatario.
	Resource resource = resourceLoader.getResource("classpath:firmatario-test.cer");

	byte[] blobFilePerFirma = Resources.toByteArray(resource.getURL());

	String certificatoFirmatarioBase64 = Base64.getUrlEncoder()
		.encodeToString(blobFilePerFirma);

	String url = URL_CRL + "?certifFirmatarioBase64UrlEncoded=" + certificatoFirmatarioBase64;

	ResponseEntity<ParerCRL> crlEntity = restTemplate.getForEntity(url, ParerCRL.class);
	ParerCRL parerCrl = crlEntity.getBody();
	assertEquals(expectedCrl.getSubjectDN(), parerCrl.getSubjectDN());
    }

    /**
     * Gestione degli errori della cryptolibrary.
     */
    private class CryptoErrorHandler extends DefaultResponseErrorHandler {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@SuppressWarnings("resource")
	@Override
	public void handleError(URI url, HttpMethod method, ClientHttpResponse response)
		throws IOException {
	    // avoid unmapped field (see datetime on RestExceptionResponse)
	    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	    throw objectMapper.readValue(response.getBody(), CryptoParerException.class);
	}
    }

}
