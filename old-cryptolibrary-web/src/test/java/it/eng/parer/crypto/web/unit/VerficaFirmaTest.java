package it.eng.parer.crypto.web.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import it.eng.crypto.data.type.SignerType;
import it.eng.crypto.exception.CryptoSignerException;
import it.eng.crypto.utils.VerificheEnums;
import it.eng.parer.crypto.model.CryptoEnums;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.CryptoAroFirmaComp;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidate;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;
import it.eng.parer.crypto.model.verifica.input.CryptoDocumentoVersato;
import it.eng.parer.crypto.model.verifica.input.CryptoProfiloVerifica;
import it.eng.parer.crypto.model.verifica.input.TipologiaDataRiferimento;
import it.eng.parer.crypto.service.CertificateService;
import it.eng.parer.crypto.service.CrlService;
import it.eng.parer.crypto.service.VerificaFirmaService;
import it.eng.parer.crypto.service.model.CryptoDataToValidateData;
import it.eng.parer.crypto.service.model.CryptoDataToValidateFile;
import it.eng.parer.crypto.web.Util;

/**
 *
 * @author Snidero_L
 */
@SpringBootTest(properties = { "cron.ca.enable=false", "cron.crl.enable=false",
        "spring.datasource.url=jdbc:h2:mem:cryptodb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.crypto=INFO" })
@TestInstance(Lifecycle.PER_CLASS)
public class VerficaFirmaTest {

    @Autowired
    private VerificaFirmaService verificaFirmaService;

    private final Date dataTest = new Date();

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CrlService crlService;

    /**
     * Aggiungo manualmente alcuni dei certificati CA/CRL che mi servono per verificare le firme. Il database delle CA e
     * CRL è vuoto inizialmente.
     *
     * @throws IOException
     *             nel caso non trovi il file
     */
    @BeforeAll
    public void fillTrustedCADatabase() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:ca-blob.cer");
        Resource tsaInfoCertResource = resourceLoader.getResource("classpath:tsa/inforcert-tsa-ca.cer");
        Resource actalisCaResource = resourceLoader.getResource("classpath:p7m_b64.xml.p7m.actalis-ca.cer");
        Resource actalisCRLResource = resourceLoader.getResource("classpath:p7m_b64.xml.p7m.actalis-crl.cer");
        Resource postecomCaResource = resourceLoader.getResource("classpath:firma.tsr.p7m.postecom-ca.cer");
        Resource postecomCRLResource = resourceLoader.getResource("classpath:firma.tsr.p7m.postecom-crl.cer");
        Resource arubaCaResource = resourceLoader.getResource("classpath:cadesBES_Controfirma_CadesT-ca.cer");
        Resource arubaTsaResource = resourceLoader.getResource("classpath:cadesBES_Controfirma_CadesT-tsa.cer");
        Resource telecomCaResource = resourceLoader
                .getResource("classpath:cades+cades_crl_non_conforme.pdf.p7m-arubapec-ca.cer");
        Resource arubapecCaResource = resourceLoader
                .getResource("classpath:cades+cades_crl_non_conforme.pdf.p7m-telecom-ca.cer");
        Resource actalisXadesCaResource = resourceLoader
                .getResource("classpath:xades_con_signeddataobjectproperties.xml-actalis-ca.cer");
        Resource postecertCaResource = resourceLoader.getResource("classpath:cades_bes.pdf.p7m-postecert-ca.cer");
        Resource infocertStranoCaResource = resourceLoader.getResource("classpath:pades-infocert-ca-strano.cer");
        Resource multicertifyActalisTsaResource = resourceLoader
                .getResource("classpath:tsa/test_non_firma.xml.tsa.cer");
        Resource multicertifyActalisCaResource = resourceLoader.getResource("classpath:test_non_firma.xml.ca.cer");
        Resource multicertifyActalisCRLResource = resourceLoader.getResource("classpath:test_non_firma.xml.crl.cer");
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
        byte[] multicertifyActalisTsaResourceTsaBlob = Resources.toByteArray(multicertifyActalisTsaResource.getURL());
        byte[] multicertifyActalisTsaResourceCaBlob = Resources.toByteArray(multicertifyActalisCaResource.getURL());
        byte[] multicertifyActalisCRLResourceBlob = Resources.toByteArray(multicertifyActalisCRLResource.getURL());
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
    }

    @Test
    public void testVerificaCadesBesPadesT() throws Exception {
        Resource cadesBesPadesT = resourceLoader.getResource("classpath:firme/cades_bes+pades_t.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();

        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(cadesBesPadesT.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc output = verificaFirmaService.verificaFirma(input);
        assertNotNull(output);
        // FIXME assertEquals(2, output.getAroBustaCrittogs().size());
    }

    @Test
    public void testXadesT_BES() throws Exception {

        // File fileWithSignature = new File(FILE_FOLDER + "/xades_T_1.xml");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xades_T_1.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 3);
        Util.assertNumeroDiMarcheOK(componente, 2);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 2);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XADES_T, Util.XMLSHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.XADES_T, Util.XMLSHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.XADES_BES, Util.XMLSHA256RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.XADES_T, Util.SHA256RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.XADES_T, Util.SHA256RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(1).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(2), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(2).getDtFirma());

    }

    @Test
    public void testXML_SIG_CertRevocatoDataVersamento() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/xml_sig_controfirma_cert_rev.xml");
        // Util.useDataVers(versManager);
        // versManager.setUseSigninTimeAsReferenceDate(false);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xml_sig_controfirma_cert_rev.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XML_DSIG, Util.XMLSHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.XML_DSIG, Util.XMLSHA256RSA, 1);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_VERS,
                dataTest);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1), VerificheEnums.TipoRifTemporale.DATA_VERS,
                dataTest);

    }

    @Test
    public void testXML_SIG_CertRevocatoDataRiferimento() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/xml_sig_controfirma_cert_rev.xml");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // versManager.setUseSigninTimeAsReferenceDate(false);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xml_sig_controfirma_cert_rev.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XML_DSIG, Util.XMLSHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.XML_DSIG, Util.XMLSHA256RSA, 1);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.CERTIFICATO_REVOCATO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);

    }

    @Test
    public void testP7M_PEM_SHA256() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/p7m_pem_sha256.pdf.p7m");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_pem_sha256.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.P7M, Util.SHA256RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.NON_AMMESSO_DELIB_45_CNIPA);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
                VerificheEnums.EsitoControllo.CERTIFICATO_SCADUTO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_VERS,
                dataTest);

    }

    /**
     * firma xml: il certificato risulta scaduta dopo il 11/03/2012, la firma non ha data di firma
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testXML_SIG() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/xml_sig_enveloping.xml");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xml_sig_enveloping.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XML_DSIG, Util.XMLSHA256RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.NON_AMMESSO_DELIB_45_CNIPA);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
                VerificheEnums.EsitoControllo.CERTIFICATO_SCADUTO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_VERS,
                dataTest);

    }

    /**
     * 1 firme: CadesBES utilizzando il trasferimento multipart
     *
     * @throws Exception
     */
    @Test
    public void testCadesBESMultipart() throws Exception {

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_bes.pdf.p7m");

        CryptoDataToValidateData data = new CryptoDataToValidateData();
        data.setContenuto(new CryptoDataToValidateFile("documento-principale", fileFirmato.getFile()));
        CryptoDataToValidateMetadata metadata = new CryptoDataToValidateMetadata();

        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        metadata.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(data, metadata);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
    }

    /**
     * 1 firme: CadesBES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testCadesBES() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_bes.pdf.p7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_bes.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.RIF_TEMP_VERS);
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // input.setReferenceDate(rifVersato.getTime());
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
    }

    @Test
    public void test2CadesBES_CRL_Telecom() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades+cades_crl_non_conforme.pdf.p7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades+cades_crl_non_conforme.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.RIF_TEMP_VERS);
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // input.setReferenceDate(rifVersato.getTime());
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 2);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES, Util.SHA256RSA, 2);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
    }

    /**
     * 1 firme: CadesBES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testCadesBES_Controfirme() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_bes_controfirma.doc.p7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_bes_controfirma.doc.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.RIF_TEMP_VERS);
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // input.setReferenceDate(rifVersato.getTime());
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES, Util.SHA256RSA, 1);
        CryptoAroFirmaComp aroFirmaFiglio = componente.getAroFirmaComps().get(0).getAroControfirmaFirmaFiglios().get(0)
                .getAroFirmaFiglio();
        assertEquals("IT:SSSGPP47D19G570I", aroFirmaFiglio.getCdFirmatario());

        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
    }

    @Test
    public void testCadesBES_Controfirma_CadesT() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_bes_controfirma_cades_t.pdf.p7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_bes_controfirma_cades_t.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 1);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_T, Util.SHA256RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    /**
     * 1 firme: CadesBES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testCadesBES_Pades_T() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_bes+pades_t.pdf.p7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_bes+pades_t.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 1);
        Util.assertNumeroDiBusteOK(componente, 2);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.PADES, Util.SHA256RSA, 2);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.PADES, Util.SHA256RSA, 2);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    @Test
    public void testP7M_B64_DataFirma() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/p7m_b64.xml.p7m");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_b64.xml.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        input.setProfiloVerifica(new CryptoProfiloVerifica().setControlloCrlAbilitato(true));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.P7M, Util.SHA1RSA, 1);
        Util.assertControlliFirmeOK(componente);

        Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoControlli.CATENA_TRUSTED, VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoControlli.CATENA_TRUSTED_ABILITATO, VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoControlli.CERTIFICATO, VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoControlli.CRITTOGRAFICO, VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoControlli.CRITTOGRAFICO_ABILITATO, VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(0).getDtFirma());
    }

    @Test
    public void testP7M_B64_RifTemp() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/p7m_b64.xml.p7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_b64.xml.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.P7M, Util.SHA1RSA, 1);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
                VerificheEnums.EsitoControllo.CERTIFICATO_SCADUTO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
    }

    @Test
    public void testAbilitazioni() throws Exception {

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_b64.xml.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));
        input.setProfiloVerifica(new CryptoProfiloVerifica().setControlloCatenaTrustAbilitato(false)
                .setControlloCertificatoAbilitato(false));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.P7M, Util.SHA1RSA, 1);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.DISABILITATO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
                VerificheEnums.EsitoControllo.DISABILITATO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.RIF_TEMP_VERS, rifVersato);
    }

    /**
     * 3 firme: CADES_T + 2 CadesBES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testCadesT_BES() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_T_1.pdf.p7m");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_T_1.pdf.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 3);
        Util.assertNumeroDiMarcheOK(componente, 1);
        Util.assertNumeroDiBusteOK(componente, 2);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T, Util.SHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES, Util.SHA256RSA, 2);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES, Util.SHA256RSA, 2);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.CADES_T, Util.SHA256RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(1).getDtFirma());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(2), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(2).getDtFirma());
    }

    @Test
    public void testCadesT_BES_TSR() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_T_1.pdf.p7m");
        // File marcaDetached = new File(FILE_FOLDER + "/cades_T_1.pdf.p7m.tsr");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, sottoComponente, marcaDetached, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_T_1.pdf.p7m");
        Resource marcaDetached = resourceLoader.getResource("classpath:firme/cades_T_1.pdf.p7m.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("tsr-versato", Resources.toByteArray(marcaDetached.getURL())) }));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_VERS);
        // input.setReferenceDate(dataTest.getTime());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 3);
        Util.assertNumeroDiMarcheOK(componente, 2);
        // Util.assertNumeroDiFirmeSottoComponenteOK(sottoComponente, 0);
        // Util.assertNumeroDiMarcheSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiBusteOK(componente, 2);
        // Util.assertNumeroDiBusteSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T, Util.SHA256RSA, 1);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES, Util.SHA256RSA, 2);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES, Util.SHA256RSA, 2);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.CADES_T, Util.SHA256RSA, 1);
        // Util.assertFormatoMarcaOK(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0),
        // SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(1));
        assertEquals("documento-principale", componente.getAroMarcaComps().get(1).getIdMarca());
        assertEquals("tsr-versato", componente.getAroMarcaComps().get(0).getIdMarca());

        // Util.assertControlloMarcaKO(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0),
        // VerificheEnums.TipoControlli.CATENA_TRUSTED, VerificheEnums.EsitoControllo.NEGATIVO);
        // Util.assertControlloMarcaKO(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0),
        // VerificheEnums.TipoControlli.CRL, VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(1).getTmMarcaTemp());
        // Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
        // VerificheEnums.TipoRifTemporale.MT_VERS_NORMA,
        // sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0).getTmMarcaTemp());
        // Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(2),
        // VerificheEnums.TipoRifTemporale.MT_VERS_NORMA,
        // sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    @Test
    public void testFileNonFirmato_TSR() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/test_non_firma.xml");
        // File marcaDetached = new File(FILE_FOLDER + "/test_non_firma.xml.tsr");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, sottoComponente, marcaDetached, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/test_non_firma.xml");
        Resource marcaDetached = resourceLoader.getResource("classpath:firme/test_non_firma.xml.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("tsr-versato", Resources.toByteArray(marcaDetached.getURL())) }));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_VERS);
        // input.setReferenceDate(dataTest.getTime());

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 0);
        Util.assertNumeroDiMarcheOK(componente, 1);
        assertEquals("tsr-versato", componente.getAroMarcaComps().get(0).getIdMarca());
        // Util.assertNumeroDiFirmeSottoComponenteOK(sottoComponente, 0);
        // Util.assertNumeroDiMarcheSottoComponenteOK(sottoComponente, 1);
        // Util.assertNumeroDiBusteSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        // Util.assertFormatoMarcaOK(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0),
        // SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSR, Util.SHA1RSA, 1);

        Util.assertControlliFirmeOK(componente);
        // Util.assertControlliMarcaOK(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0));
        Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(0));

    }

    @Test
    public void testP7M_TSR() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/firma.tsr.p7m");
        // File marcaDetached = new File(FILE_FOLDER + "/firma.tsr.p7m.tsr");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, sottoComponente, marcaDetached, dataTest);
        //
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/firma.tsr.p7m");
        Resource marcaDetached = resourceLoader.getResource("classpath:firme/firma.tsr.p7m.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("tsr-detached", Resources.toByteArray(marcaDetached.getURL())) }));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 2);
        Util.assertNumeroTotaleComponentiVersati(componente, 2);
        // Util.assertNumeroDiFirmeSottoComponenteOK(sottoComponente, 0);
        // Util.assertNumeroDiMarcheSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiBusteOK(componente, 2);
        // Util.assertNumeroDiBusteSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        // Util.assertFormatoMarcaOK(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0),
        // SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.TSR, Util.SHA1RSA, 2);
        Util.assertControlloConformitaMarca(componente.getAroMarcaComps().get(1),
                VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO);
        Util.assertControlliFirmeOK(componente);

        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    @Test
    public void testP7M_MD5() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/p7m_md5.pdf.p7m");
        // Util.useDataVers(versManager);
        // versManager.setUseSigninTimeAsReferenceDate(false);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/p7m_md5.pdf.p7m");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.P7M, Util.MD5RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.NON_AMMESSO_DELIB_45_CNIPA);
        // Util.assertControlloFirmaSpecificoOK(componente.getAroFirmaComps().get(0),
        // VerificheEnums.TipoControlli.CATENA_TRUSTED, VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
                VerificheEnums.EsitoControllo.CERTIFICATO_ERRATO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_VERS,
                dataTest);
    }

    @Test
    public void testTSD_Cades_BES() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/tsd_cades_bes.p7m.tsd");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/tsd_cades_bes.p7m.tsd");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 1);
        Util.assertNumeroDiBusteOK(componente, 2);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 2);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSD, Util.SHA256RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    @Test
    public void testTSR_VersatoComeComponente() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_T_1.pdf.p7m.tsr");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_T_1.pdf.p7m.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 0);
        Util.assertNumeroDiMarcheOK(componente, 1);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertControlloConformitaMarca(componente.getAroMarcaComps().get(0),
                VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);

    }

    @Test
    public void testTSD_Cades_T() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/tsd_cades_T.pdf.p7m.tsd");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/tsd_cades_T.pdf.p7m.tsd");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 3);
        Util.assertNumeroDiMarcheOK(componente, 2);
        Util.assertNumeroDiBusteOK(componente, 3);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T, Util.SHA256RSA, 2);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES, Util.SHA256RSA, 3);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES, Util.SHA256RSA, 3);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.TSD, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.CADES_T, Util.SHA256RSA, 2);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(1));
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(1).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(2),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    @Test
    public void testTSD_Cades_T_TSR() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/tsd_cades_T.pdf.p7m.tsd");
        // File marcaDetached = new File(FILE_FOLDER + "/tsd_cades_T.pdf.p7m.tsd.tsr");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, sottoComponente, marcaDetached, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/tsd_cades_T.pdf.p7m.tsd");
        Resource marcaDetached = resourceLoader.getResource("classpath:firme/tsd_cades_T.pdf.p7m.tsd.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("marca-detached", Resources.toByteArray(marcaDetached.getURL())) }));

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 3);
        Util.assertNumeroDiMarcheOK(componente, 3);
        Util.assertNumeroDiBusteOK(componente, 3);
        // Util.assertNumeroDiFirmeSottoComponenteOK(sottoComponente, 0);
        // Util.assertNumeroDiMarcheSottoComponenteOK(sottoComponente, 1);
        // Util.assertNumeroDiBusteSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_T, Util.SHA256RSA, 2);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(1), SignerType.CADES_BES, Util.SHA256RSA, 3);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(2), SignerType.CADES_BES, Util.SHA256RSA, 3);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(1), SignerType.TSD, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(2), SignerType.CADES_T, Util.SHA256RSA, 2);
        // Util.assertFormatoMarcaOK(sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().get(0),
        // SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getMarche("marca-detached").get(0), SignerType.TSR, Util.SHA1RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(1), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloMarcaKO(componente.getAroMarcaComps().get(1), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcaOK(componente.getAroMarcaComps().get(2));
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(2).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(1).getTmMarcaTemp());
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(2),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(1).getTmMarcaTemp());
    }

    @Test
    public void testMarcheMultiple() throws Exception {

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/328730788_C3");
        Resource marcaDetached1 = resourceLoader.getResource("classpath:firme/328730788_C3_SC1");
        Resource marcaDetached2 = resourceLoader.getResource("classpath:firme/328730788_C3_SC2");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("marca-detached1", Resources.toByteArray(marcaDetached1.getURL())),
                new CryptoDocumentoVersato("marca-detached2", Resources.toByteArray(marcaDetached2.getURL())) }));

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 2);
        Util.assertNumeroDiMarcheOK(componente, 2);

        long expectedTimestamp = 1_545_058_728_000L;
        long actualTimestamp = componente.getAroMarcaComps().get(0).getTmMarcaTemp().getTime();
        assertEquals(expectedTimestamp, actualTimestamp);

        String expectedIdMarca = "marca-detached1";
        String actualIdMarca = componente.getAroMarcaComps().get(0).getIdMarca();
        assertEquals(expectedIdMarca, actualIdMarca);

    }

    /**
     * 3 firme: CADES_T + 2 CadesBES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testPades() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/pades.pdf");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/pades.pdf");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(new CryptoDocumentoVersato("pades", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_FIRMA);
        //// Date dataRiferimento = Util.getDate(10, Calendar.JUNE, 2014);
        // input.setReferenceDate(dataTest.getTime());
        // input.setUseSigningDate(true);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.PADES, Util.SHA256RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(0).getDtFirma());
    }

    /**
     * Valida alla data di firma
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testPDFSig() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/pdf_dsig.pdf");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/pdf_dsig.pdf");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(new CryptoDocumentoVersato("test-pdf-dsig", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_FIRMA);
        // input.setReferenceDate(dataTest.getTime());
        // input.setUseSigningDate(true);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.PDF_DSIG, Util.SHA1RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(0).getDtFirma());
    }

    @Test
    public void testPDFSigScaduta() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/pdf_dsig_scaduta.pdf");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/pdf_dsig_scaduta.pdf");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("test-pdf-dsig-scaduto", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_FIRMA);
        // input.setReferenceDate(dataTest.getTime());
        // input.setUseSigningDate(true);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.PDF_DSIG, Util.SHA1RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.NON_AMMESSO_DELIB_45_CNIPA);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CATENA_TRUSTED,
                VerificheEnums.EsitoControllo.NEGATIVO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.CRL_NON_SCARICABILE);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(0).getDtFirma());
    }

    /**
     * 3 firme: CADES_T + 2 CadesBES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testCadesBES_ErroreCrittografico() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/cades_bes_errore_critto.p7m");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_bes_errore_critto.p7m");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(new CryptoDocumentoVersato("cades-bes", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_FIRMA);
        // input.setReferenceDate(dataTest.getTime());
        // input.setUseSigningDate(true);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.CADES_BES, Util.SHA256RSA, 1);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRITTOGRAFICO);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0), VerificheEnums.TipoRifTemporale.DATA_FIRMA,
                componente.getAroFirmaComps().get(0).getDtFirma());
    }

    /**
     * 1 firme: M7M + 1 marca
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testM7M() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/m7m_ok.pdf.m7m");
        // Util.useRifTempVers(versManager);
        // Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // verificaFirme(componente, fileWithSignature, rifVersato);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/m7m_ok.pdf.m7m");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.MT_VERS_NORMA);
        Date rifVersato = Util.getDate(8, Calendar.SEPTEMBER, 2013);
        // input.setReferenceDate(rifVersato.getTime());

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 1);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.M7M, Util.SHA1RSA, 1);
        Util.assertFormatoMarcaOK(componente.getAroMarcaComps().get(0), SignerType.M7M, Util.SHA1RSA, 1);
        Util.assertControlliFirmeOK(componente);
        Util.assertControlliMarcheOK(componente);
        Util.assetRifTemporaleFirmaOK(componente.getAroFirmaComps().get(0),
                VerificheEnums.TipoRifTemporale.MT_VERS_NORMA, componente.getAroMarcaComps().get(0).getTmMarcaTemp());
    }

    @Test
    public void testMarcaSconosciuta() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/test_non_firma.xml");
        // File marcaDetached = new File(FILE_FOLDER + "/test_non_firma.xml");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, sottoComponente, marcaDetached, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/test_non_firma.xml");
        Resource marcaDetached = resourceLoader.getResource("classpath:firme/test_non_firma.xml");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("documento-secondario", Resources.toByteArray(marcaDetached.getURL())) }));

        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 0);
        Util.assertNumeroDiMarcheOK(componente, 1);
        // Util.assertNumeroDiFirmeSottoComponenteOK(sottoComponente, 0);
        // Util.assertNumeroDiMarcheSottoComponenteOK(sottoComponente, 1);
        // Util.assertNumeroDiBusteSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);

        Util.assertControlloConformitaMarca(componente.getMarche("documento-secondario").get(0),
                VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO);
    }

    @Test
    public void testFirmaSconosciuta() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/test_non_firma.xml");
        // File firmaDetached = new File(FILE_FOLDER + "/test_non_firma.xml");
        // Util.useDataVers(versManager);
        // verificaFirmeDetached(componente, fileWithSignature, sottoComponente, firmaDetached, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/test_non_firma.xml");
        Resource fileDetached = resourceLoader.getResource("classpath:firme/test_non_firma.xml");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiFirma(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("sottocomponente", Resources.toByteArray(fileDetached.getURL())) }));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        // FIXME!! ATTUALMENTE IL SOTTOCOMPONENTE MUORE DENTRO LA VERIFICA FIRMA!!!
        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        assertEquals(1, componente.getFirme("sottocomponente").size());
        assertEquals(0, componente.getMarche("sottocomponente").size());
        // Util.assertNumeroDiFirmeSottoComponenteOK(sottoComponente, 1);
        // Util.assertNumeroDiMarcheSottoComponenteOK(sottoComponente, 0);
        // Util.assertNumeroDiBusteSottoComponenteOK(sottoComponente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        // Util.assertControlloConformitaFirma(sottoComponente.getAroBustaCrittogs().get(0).getAroFirmaComps().get(0),
        // VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.FORMATO_NON_CONOSCIUTO);
    }

    /**
     * firma xml con tag SignedDataObjectProperties
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testXML_With_SignedDataObjectProperties() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/xades_con_signeddataobjectproperties.xml");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);
        //
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xades_con_signeddataobjectproperties.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date dataRif = Util.getDate(20, Calendar.FEBRUARY, 2016);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(dataRif.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XADES_BES, Util.XMLSHA256RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.POSITIVO);
        Util.assertControlliFirmeOK(componente);
        // Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
        // VerificheEnums.EsitoControllo.CERTIFICATO_SCADUTO);
        // Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
        // VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
    }

    /**
     * firma xml che noi la riconosciamo come xml dsig ma che il Laboratorio di Pievesestina asserisce essere
     * validamente una XADES
     *
     * @throws CryptoSignerException
     */
    @Test
    public void testXML_SIG_conteso() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/xml_sig_conteso.xml");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xml_sig_conteso.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiBusteOK(componente, 1);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        Util.assertFormatoFirmaOK(componente.getAroFirmaComps().get(0), SignerType.XML_DSIG, Util.XMLSHA256RSA, 1);
        Util.assertControlloConformitaFirma(componente.getAroFirmaComps().get(0),
                VerificheEnums.EsitoControllo.NON_AMMESSO_DELIB_45_CNIPA);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CERTIFICATO,
                VerificheEnums.EsitoControllo.CERTIFICATO_SCADUTO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(0), VerificheEnums.TipoControlli.CRL,
                VerificheEnums.EsitoControllo.NON_NECESSARIO);
        Util.assertControlliMarcheOK(componente);
    }

    /**
     * 2 firme e 2 marche. TSA non italiana. Firme solo su parte del documento
     */
    @Test
    public void testPdfNullPointerEx_comunitaEuropea() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/pdfNullPointerEx_comunitaEuropea.pdf");
        // Util.useDataVers(versManager);
        // verificaFirme(componente, fileWithSignature, dataTest);

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/pdfNullPointerEx_comunitaEuropea.pdf");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaDataVersamento(dataTest.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);
        Util.assertNumeroDiFirmeOK(componente, 0);
        // Util.assertControlloFirmaErrore(componente.getAroFirmaComps().get(0));

    }

    /**
     * PDF non conforme. Probabilmente è un cades. Questo è un caso di test che utilizza la stessa procedura della
     * verifica firma; esegue un versamento fittizio passando anche dalla validazione EIDAS.
     */
    @Test
    public void testPdfNonConforme() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/pades_non_conforme.pdf");
        // final List<File> noDetached = Collections.emptyList();
        // VerificaFirmeFormatiDto verificaTest = verificaFirme(fileWithSignature, noDetached, noDetached, dataTest,
        // true, dataTest, true, true);
        // ComponenteVers componenteVers = verificaTest.getComponenteVers();
        // AroCompDoc componente = componenteVers.getAcdEntity();

        Resource fileFirmato = resourceLoader.getResource("classpath:firme/pades_non_conforme.pdf");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        Util.assertNumeroDiFirmeOK(componente, 1);
        Util.assertNumeroDiMarcheOK(componente, 0);
        Util.assertNumeroDiControfirmeOK(componente, 0);
        // FIXME Util.assertControlloConformitaFirma(componente.getAroBustaCrittogs().get(0).getAroFirmaComps().get(0),
        // VerificheEnums.EsitoControllo.FORMATO_NON_CONFORME);
    }

    @Test
    public void testPdfFirmeMultipleErroreCritto() throws Exception {
        // File fileWithSignature = new File(FILE_FOLDER + "/pades_3_firme_errore_contr_crittografico.pdf");
        // Date dataRiferimento = new SimpleDateFormat(Constants.DATE_FORMAT_DATE_TYPE).parse("12/12/2017");
        // verificaFirme(componente, fileWithSignature, dataRiferimento);

        Resource fileFirmato = resourceLoader
                .getResource("classpath:firme/pades_3_firme_errore_contr_crittografico.pdf");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date rifVersato = Util.getDate(12, Calendar.DECEMBER, 2017);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(rifVersato.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        // sono 3 firme
        Util.assertNumeroDiFirmeOK(componente, 3);

        // la seconda viene valutata come negatica sul controllo crittografico.
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(1), VerificheEnums.TipoControlli.CRITTOGRAFICO);
        Util.assertControlloFirmaKO(componente.getAroFirmaComps().get(1),
                VerificheEnums.TipoControlli.CRITTOGRAFICO_ABILITATO);
    }

    @Test
    public void testMimeTypeOffice() throws IOException {
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/office_non_firmato");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        String expetedMimeType = "application/msword";
        String actualMimeType = componente.getTikaMimeComponentePrincipale();
        assertEquals(expetedMimeType, actualMimeType);

    }

    @Test
    public void testConfigurazioneNonCoerente() throws IOException {
        CryptoDataToValidate input = new CryptoDataToValidate();
        assertThrows(IllegalArgumentException.class, () -> {
            TipologiaDataRiferimento configurazioneNonCoerente = new TipologiaDataRiferimento(
                    CryptoEnums.TipoRifTemporale.RIF_TEMP_VERS, true, 0L);
            input.setTipologiaDataRiferimento(configurazioneNonCoerente);
        });
    }

    @Test
    public void testInputParameters() throws IOException {
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(42L));
        input.setContenuto(new CryptoDocumentoVersato("id_42", "test".getBytes()));
        input.setUuid("TEST-UUID");
        ObjectMapper mapper = new ObjectMapper();
        String inputAsJsonString = mapper.writeValueAsString(input);
        final String expectedOutput = "{\"contenuto\":{\"nome\":\"id_42\",\"contenuto\":\"dGVzdA==\"},\"uuid\":\"TEST-UUID\",\"sottoComponentiFirma\":[],\"sottoComponentiMarca\":[],\"profiloVerifica\":{\"controlloCrittograficoAbilitato\":true,\"controlloCatenaTrustAbilitato\":true,\"controlloCertificatoAbilitato\":true,\"controlloCrlAbilitato\":true},\"tipologiaDataRiferimento\":{\"referenceDateType\":\"RIF_TEMP_VERS\",\"useSigningDate\":false,\"dataRiferimento\":42}}";
        assertEquals(expectedOutput, inputAsJsonString);
    }

    @Test
    public void testVersione() throws IOException {
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/xades_con_signeddataobjectproperties.xml");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        Date dataRif = Util.getDate(20, Calendar.FEBRUARY, 2016);
        input.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataSpecifica(dataRif.getTime()));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);

        assertNotNull(componente.getValidatorVersion());
    }

    @Test
    public void testMimeComponentePrincipale() throws IOException {
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cades_T_1.pdf.p7m");
        Resource marcaDetached = resourceLoader.getResource("classpath:firme/cades_T_1.pdf.p7m.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));
        input.setSottoComponentiMarca(Arrays.asList(new CryptoDocumentoVersato[] {
                new CryptoDocumentoVersato("tsr-versato", Resources.toByteArray(marcaDetached.getURL())) }));
        // input.setReferenceDateType(CryptoEnums.TipoRifTemporale.DATA_VERS);
        // input.setReferenceDate(dataTest.getTime());

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);
        String tikaMimeComponentePrincipale = componente.getTikaMimeComponentePrincipale();
        assertEquals("application/pdf", tikaMimeComponentePrincipale);
    }

    @Test
    public void testSubjectCA() throws IOException {
        Resource fileFirmato = resourceLoader.getResource("classpath:firme/cavor.xml.p7m");
        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("documento-principale", Resources.toByteArray(fileFirmato.getURL())));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);
        String expectedSubjectCA = "CN=ArubaPEC S.p.A. NG CA 3,OU=Certification AuthorityC,O=ArubaPEC S.p.A.,C=IT";
        String actualSubjectCA = componente.getAroFirmaComps().get(0).getFirCertifFirmatario().getFirCertifCa()
                .getFirIssuer().getDlDnSubjectCertifCa();
        assertEquals(expectedSubjectCA, actualSubjectCA);
    }

    @Test
    public void testComponenteTsr() throws IOException {
        Resource componenteTsrResource = resourceLoader.getResource("classpath:componente.tsr");

        CryptoDataToValidate input = new CryptoDataToValidate();
        input.setContenuto(
                new CryptoDocumentoVersato("componente-tsr", Resources.toByteArray(componenteTsrResource.getURL())));

        CryptoAroCompDoc componente = verificaFirmaService.verificaFirma(input);
        // sono 3 firme
        Util.assertNumeroDiMarcheOK(componente, 1);

    }

    /**
     * Genera l'xml dell'output del sistema di verifica. Utilizza l'introspector (forse a questo punto non serve più...)
     *
     * @param response
     *            documento generato dalla verifica firma crypto
     *
     * @return stringa dell'xml
     *
     * @throws JAXBException
     *             eccezione JAXB
     */
    public static String generateReport(CryptoAroCompDoc response) throws JAXBException {
        StringWriter tmpStringWriter = new StringWriter();

        JAXBContext jc = JAXBContext.newInstance(response.getClass());
        Marshaller tmpGenericMarshaller = jc.createMarshaller();
        JAXBIntrospector introspector = jc.createJAXBIntrospector();
        if (null == introspector.getElementName(response)) {
            JAXBElement jaxbElement = new JAXBElement(new QName("ROOT"), response.getClass(), response);
            tmpGenericMarshaller.marshal(jaxbElement, tmpStringWriter);
        } else {
            tmpGenericMarshaller.marshal(response, tmpStringWriter);
        }
        return tmpStringWriter.toString();
    }

}
