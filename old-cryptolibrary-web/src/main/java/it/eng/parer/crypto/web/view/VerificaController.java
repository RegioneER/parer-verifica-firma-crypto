/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.eng.parer.crypto.web.view;

import it.eng.parer.crypto.model.CryptoEnums;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadata;
import it.eng.parer.crypto.model.verifica.input.CryptoDataToValidateMetadataFile;
import it.eng.parer.crypto.model.verifica.input.CryptoProfiloVerifica;
import it.eng.parer.crypto.model.verifica.input.TipologiaDataRiferimento;
import it.eng.parer.crypto.service.VerificaFirmaService;
import it.eng.parer.crypto.service.model.CryptoDataToValidateData;
import it.eng.parer.crypto.service.model.CryptoDataToValidateFile;
import it.eng.parer.crypto.web.bean.VerificaFirmaBean;
import it.eng.parer.crypto.web.bean.VerificaFirmaResultBean;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author lorenzo
 */
@Controller
public class VerificaController {

    private static final Logger LOG = LoggerFactory.getLogger(VerificaController.class);

    private static final String RISULTATO_VERIFICA = "risultatoVerifica";

    private static final String CONTENUTO = "contenuto";
    private static final String MARCA = "marca_";
    private static final String FIRMA = "firma_";

    private static final String BUILD_VERSION = "git.build.version";

    private static final String BUILD_TIME = "git.commit.time";

    private static final String CRYPTO_VERSION = "eng-cryptolibrary";

    @Autowired
    private Environment env;

    @Autowired
    private BuildProperties buildProperties;

    @Autowired
    private VerificaFirmaService verificaService;

    @ModelAttribute("version")
    public String getVersion() {
        return env.getProperty(BUILD_VERSION);
    }

    @ModelAttribute("builddate")
    public String getBuilddate() {
        return env.getProperty(BUILD_TIME);
    }

    @ModelAttribute("engcryptolibrary")
    public String getEngcryptolibrary() {
        return buildProperties.get(CRYPTO_VERSION);
    }

    @GetMapping("/verifica")
    public ModelAndView verifica(Model model) {
        model.addAttribute("verificafirmaBean", new VerificaFirmaBean());
        return new ModelAndView("verifica");
    }

    @PostMapping(value = "/verifica", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView verifica(@ModelAttribute @Valid VerificaFirmaBean verificafirmaBean, BindingResult errors,
            Model model) {
        // Output
        VerificaFirmaResultBean risultato = new VerificaFirmaResultBean();

        // Input
        CryptoDataToValidateMetadata metadati = new CryptoDataToValidateMetadata();
        CryptoDataToValidateData dati = new CryptoDataToValidateData();
        try {
            // compila metadati
            compilaMetadati(verificafirmaBean, metadati);
            MDC.put("uuid", metadati.getUuid());
            // compila dati
            compilaDati(verificafirmaBean, dati);

            CryptoAroCompDoc verificaFirma = verificaService.verificaFirma(dati, metadati);
            String reportTree = creaStringaXml(verificaFirma);
            risultato.setReportTree(reportTree);

        } catch (Exception ex) {
            // imposta errore
            LOG.error("Errore durante la conversione dell'oggetto", ex);
            risultato.setWithErrors(true);
        } finally {
            // pulisci dati
            pulisciDati(dati);

        }

        model.addAttribute(RISULTATO_VERIFICA, risultato);
        return new ModelAndView("risultati_verifica");

    }

    private String creaStringaXml(CryptoAroCompDoc output) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(CryptoAroCompDoc.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter sw = new StringWriter();
        marshaller.marshal(output, sw);
        return sw.toString();
    }

    private void compilaMetadati(VerificaFirmaBean input, CryptoDataToValidateMetadata metadati) {

        List<MultipartFile> firme = input.getFirmeDetached();
        List<MultipartFile> marche = input.getMarcheDetached();

        metadati.setComponentePrincipale(new CryptoDataToValidateMetadataFile(CONTENUTO));
        List<CryptoDataToValidateMetadataFile> sottoComponentiFirma = new ArrayList<>();
        for (int i = 0; i < firme.size(); i++) {
            if (!firme.get(i).isEmpty()) {
                sottoComponentiFirma.add(new CryptoDataToValidateMetadataFile(FIRMA + i));
            }
        }
        List<CryptoDataToValidateMetadataFile> sottoComponentiMarca = new ArrayList<>();
        for (int i = 0; i < marche.size(); i++) {
            if (!marche.get(i).isEmpty()) {
                sottoComponentiMarca.add(new CryptoDataToValidateMetadataFile(MARCA + i));
            }
        }
        metadati.setSottoComponentiFirma(sottoComponentiFirma);
        metadati.setSottoComponentiMarca(sottoComponentiMarca);

        metadati.setUuid("verifica-manuale");

        CryptoProfiloVerifica profiloVerifica = new CryptoProfiloVerifica();
        profiloVerifica.setControlloCatenaTrustAbilitato(input.isAbilitaControlloCatenaTrusted());
        profiloVerifica.setControlloCertificatoAbilitato(input.isAbilitaControlloCa());
        profiloVerifica.setControlloCrittograficoAbilitato(input.isAbilitaControlloCrittografico());
        profiloVerifica.setControlloCrlAbilitato(input.isAbilitaControlloCrl());

        metadati.setProfiloVerifica(profiloVerifica);

        Date dataRiferimento = null;
        LocalDate dataRiferimentoForm = input.getDataRiferimento();
        LocalTime oraRiferimentoForm = input.getOraRiferimento();

        if (dataRiferimentoForm != null) {
            if (oraRiferimentoForm == null) {
                oraRiferimentoForm = LocalTime.MIN;
            }
            LocalDateTime atDate = oraRiferimentoForm.atDate(dataRiferimentoForm);
            ZonedDateTime zatDate = atDate.atZone(ZoneId.systemDefault());
            dataRiferimento = Date.from(zatDate.toInstant());
        }

        if (input.isVerificaAllaDataFirma()) {
            metadati.setTipologiaDataRiferimento(TipologiaDataRiferimento.verificaAllaDataDiFirma());
        } else {
            if (dataRiferimento != null) {
                metadati.setTipologiaDataRiferimento(
                        TipologiaDataRiferimento.verificaAllaDataSpecifica(dataRiferimento.getTime()));
            }
        }
    }

    private void compilaDati(VerificaFirmaBean input, CryptoDataToValidateData dati) throws IOException {
        MultipartFile contenuto = input.getFileDaVerificare();
        List<MultipartFile> firme = input.getFirmeDetached();
        List<MultipartFile> marche = input.getMarcheDetached();

        CryptoDataToValidateFile signedFile = new CryptoDataToValidateFile();
        List<CryptoDataToValidateFile> detachedSignature = new ArrayList<>();
        List<CryptoDataToValidateFile> detachedTimeStamp = new ArrayList<>();

        final String suffix = ".crypto";

        File principale = File.createTempFile(CONTENUTO, suffix);
        contenuto.transferTo(principale);
        signedFile.setNome(CONTENUTO);
        signedFile.setContenuto(principale);

        for (int i = 0; i < firme.size(); i++) {
            if (!firme.get(i).isEmpty()) {
                MultipartFile firma = firme.get(i);
                File sig = File.createTempFile(FIRMA, suffix);
                firma.transferTo(sig);
                detachedSignature.add(new CryptoDataToValidateFile(FIRMA + i, sig));
            }
        }

        for (int i = 0; i < marche.size(); i++) {
            if (!marche.get(i).isEmpty()) {
                MultipartFile marca = marche.get(i);
                File ts = File.createTempFile(MARCA, suffix);
                marca.transferTo(ts);

                detachedTimeStamp.add(new CryptoDataToValidateFile(MARCA + i, ts));
            }
        }

        dati.setContenuto(signedFile);
        dati.setSottoComponentiFirma(detachedSignature);
        dati.setSottoComponentiMarca(detachedTimeStamp);

    }

    private void pulisciDati(CryptoDataToValidateData dati) {
        final String noDelete = "Impossibile eliminare ";

        CryptoDataToValidateFile signedFile = dati.getContenuto();
        List<CryptoDataToValidateFile> detachedSignature = dati.getSottoComponentiFirma();
        List<CryptoDataToValidateFile> detachedTimeStamp = dati.getSottoComponentiMarca();

        if (signedFile.getContenuto() != null && !signedFile.getContenuto().delete()) {
            LOG.warn(noDelete + signedFile.getContenuto().getName());
        }

        detachedSignature.forEach(s -> {
            if (!s.getContenuto().delete()) {
                LOG.warn(noDelete + s.getContenuto().getName());
            }
        });
        detachedTimeStamp.forEach(s -> {
            if (!s.getContenuto().delete()) {
                LOG.warn(noDelete + s.getContenuto().getName());
            }
        });
    }

}
