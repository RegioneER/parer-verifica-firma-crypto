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

package it.eng.parer.crypto.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import it.eng.crypto.data.type.SignerType;
import it.eng.crypto.utils.VerificheEnums;
import it.eng.parer.crypto.model.verifica.CryptoAroCompDoc;
import it.eng.parer.crypto.model.verifica.CryptoAroContrFirmaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroContrMarcaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroFirmaComp;
import it.eng.parer.crypto.model.verifica.CryptoAroMarcaComp;

/**
 *
 * @author Quaranta_M
 */
public class Util {

    public final static String SHA256RSA = "SHA256withRSA";
    public final static String XMLSHA256RSA = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    public final static String SHA1RSA = "SHA1withRSA";
    public final static String MD5RSA = "MD5withRSA";

    /**
     * Ritorna true se tutti i controlli di firma hanno ritornato valore POSITIVO
     *
     * @param firma
     *
     * @return
     */
    public static void assertControlliFirmeOK(CryptoAroCompDoc componente) {
	for (CryptoAroFirmaComp firma : componente.getAroFirmaComps()) {
	    assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(),
		    firma.getTiEsitoVerifFirma());
	    for (CryptoAroContrFirmaComp controllo : firma.getAroContrFirmaComps()) {
		assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(),
			controllo.getTiEsitoContrFirma());
	    }
	}
    }

    public static void assertControlliMarcheOK(CryptoAroCompDoc componente) {
	for (CryptoAroMarcaComp marca : componente.getAroMarcaComps()) {
	    assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(),
		    marca.getTiEsitoVerifMarca());
	    for (CryptoAroContrMarcaComp controllo : marca.getAroContrMarcaComps()) {
		assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(),
			controllo.getTiEsitoContrMarca());
	    }
	}
    }

    public static void assertControlliMarcaOK(CryptoAroMarcaComp marca) {
	assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(), marca.getTiEsitoVerifMarca());
	for (CryptoAroContrMarcaComp controllo : marca.getAroContrMarcaComps()) {
	    assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(),
		    controllo.getTiEsitoContrMarca());
	}
    }

    private static void assertAlgoFirma(CryptoAroFirmaComp firma, String algoAtteso) {
	assertEquals(algoAtteso, firma.getDsAlgoFirma());
    }

    private static void assertAlgoMarca(CryptoAroMarcaComp marca, String algoAtteso) {
	assertEquals(algoAtteso, marca.getDsAlgoMarca());
    }

    public static void assertFormatoFirmaOK(CryptoAroFirmaComp firma, SignerType formatoAtteso,
	    String algoAtteso, int pgBusta) {
	assertTrue(firma.getTiFormatoFirma().equalsIgnoreCase(formatoAtteso.name()));
	assertAlgoFirma(firma, algoAtteso);
	assertEquals(pgBusta, firma.getPgBusta().intValue());
    }

    public static void assertFormatoMarcaOK(CryptoAroMarcaComp marca, SignerType formatoAtteso,
	    String algoAtteso, int pgBusta) {
	assertTrue(marca.getTiFormatoMarca().equalsIgnoreCase(formatoAtteso.name()));
	assertAlgoMarca(marca, algoAtteso);
	assertEquals(pgBusta, marca.getPgBusta().intValue());
    }

    public static void assertControlloFirmaKO(CryptoAroFirmaComp firma,
	    VerificheEnums.TipoControlli tipoControllo) {
	assertEquals(VerificheEnums.EsitoControllo.NEGATIVO.name(), firma.getTiEsitoVerifFirma());
	for (CryptoAroContrFirmaComp controllo : firma.getAroContrFirmaComps()) {
	    if (controllo.getTiContr().equals(tipoControllo.name())) {
		assertEquals(VerificheEnums.EsitoControllo.NEGATIVO.name(),
			controllo.getTiEsitoContrFirma());
		break;
	    }
	}
    }

    /**
     * Permette di valutare un esisito specifico su una firma considerata valida
     *
     * @param firma
     * @param tipoControllo
     * @param esitoAtteso
     */
    public static void assertControlloFirmaSpecificoOK(CryptoAroFirmaComp firma,
	    VerificheEnums.TipoControlli tipoControllo, VerificheEnums.EsitoControllo esitoAtteso) {
	assertEquals(VerificheEnums.EsitoControllo.POSITIVO.name(), firma.getTiEsitoVerifFirma());
	for (CryptoAroContrFirmaComp controllo : firma.getAroContrFirmaComps()) {
	    if (controllo.getTiContr().equals(tipoControllo.name())) {
		assertEquals(esitoAtteso.name(), controllo.getTiEsitoContrFirma());
		break;
	    }
	}
    }

    /**
     * Tutti i controlli sulla firma sono in ERRORE.
     *
     * @param firma busta relativa alla firma da verificare.
     */
    public static void assertControlloFirmaErrore(CryptoAroFirmaComp firma) {
	assertEquals(VerificheEnums.EsitoControllo.NEGATIVO.name(), firma.getTiEsitoVerifFirma());
	for (CryptoAroContrFirmaComp controllo : firma.getAroContrFirmaComps()) {
	    assertEquals(VerificheEnums.EsitoControllo.ERRORE.name(),
		    controllo.getTiEsitoContrFirma());

	}
    }

    public static void assertControlloFirmaKO(CryptoAroFirmaComp firma,
	    VerificheEnums.TipoControlli tipoControllo,
	    VerificheEnums.EsitoControllo esitoControllo) {
	assertEquals(VerificheEnums.EsitoControllo.NEGATIVO.name(), firma.getTiEsitoVerifFirma());
	for (CryptoAroContrFirmaComp controllo : firma.getAroContrFirmaComps()) {
	    if (controllo.getTiContr().equals(tipoControllo.name())) {
		assertEquals(esitoControllo.name(), controllo.getTiEsitoContrFirma());
		break;
	    }
	}
    }

    public static void assertControlloMarcaKO(CryptoAroMarcaComp marca,
	    VerificheEnums.TipoControlli tipoControllo,
	    VerificheEnums.EsitoControllo esitoControllo) {
	assertEquals(VerificheEnums.EsitoControllo.WARNING.name(), marca.getTiEsitoVerifMarca());
	for (CryptoAroContrMarcaComp controllo : marca.getAroContrMarcaComps()) {
	    if (controllo.getTiContr().equals(tipoControllo.name())) {
		assertEquals(esitoControllo.name(), controllo.getTiEsitoContrMarca());
		break;
	    }
	}
    }

    public static void assertNumeroDiFirmeOK(CryptoAroCompDoc componente, int firmeAttese) {
	assertNotNull(componente.getAroFirmaComps());
	assertEquals(firmeAttese, componente.getAroFirmaComps().size());
    }

    public static void assertNumeroDiMarcheOK(CryptoAroCompDoc componente, int marcheAttese) {
	assertNotNull(componente.getAroMarcaComps());
	assertEquals(marcheAttese, componente.getAroMarcaComps().size());
    }

    /**
     * Numero totale di componenti versati distinti (1 principale e n sottocomponenti)
     *
     * @param componentiVersatiAttesi numero atteso
     */
    public static void assertNumeroTotaleComponentiVersati(CryptoAroCompDoc componente,
	    int componentiVersatiAttesi) {
	Set<String> componentiDistinti = new HashSet<>();
	for (CryptoAroFirmaComp firme : componente.getAroFirmaComps()) {
	    componentiDistinti.add(firme.getIdFirma());
	}
	for (CryptoAroMarcaComp marca : componente.getAroMarcaComps()) {
	    componentiDistinti.add(marca.getIdMarca());
	}
	assertEquals(componentiVersatiAttesi, componentiDistinti.size());
    }

    /**
     * Asserisce che la data utilizzata come riferimento temporale é pari a quella attesa. L'ordine
     * di priorità nell'utilizzo della data di riferimento é:
     * <ol>
     * <li>Marca embedded (MT_VERS_NORMA)</li>
     * <li>Marca TSD (MT_VERS_NORMA)</li>
     * <li>Marca detached (MT_VERS_NORMA)</li>
     * <li>Data versata come riferimento temporale (RIF_TEMP_VERS)</li>
     * <li>Data di firma (DATA_FIRMA)</li>
     * <li>Data versamento (DATA_VERS)</li>
     * </ol>
     *
     * @param componente
     * @param tipoRifTemporale
     * @param rifTemporale
     */
    public static void assetRifTemporaleFirmaOK(CryptoAroFirmaComp componente,
	    VerificheEnums.TipoRifTemporale tipoRifTemporale, Date rifTemporale) {
	assertEquals(tipoRifTemporale.name(), componente.getTiRifTempUsato());
	assertEquals(rifTemporale, componente.getTmRifTempUsato());
    }

    public static Date getDate(int giorno, int mese, int anno) {
	Calendar cal = Calendar.getInstance();
	cal.set(anno, mese, giorno);
	return cal.getTime();

    }

    public static void assertNumeroDiBusteOK(CryptoAroCompDoc componente, int busteAttese) {
	int buste = 0;
	for (CryptoAroFirmaComp firma : componente.getAroFirmaComps()) {
	    if (firma.getPgBusta().intValue() > buste) {
		buste = firma.getPgBusta().intValue();
	    }
	}
	for (CryptoAroMarcaComp marca : componente.getAroMarcaComps()) {
	    if (marca.getPgBusta().intValue() > buste) {
		buste = marca.getPgBusta().intValue();
	    }
	}
	assertEquals(busteAttese, buste);
	// FIXME assertEquals(busteAttese, componente.getAroBustaCrittogs().size());
    }

    public static void assertNumeroDiControfirmeOK(CryptoAroCompDoc componente,
	    int controfirmeAttese) {
	int numControfirmeTrovate = 0;
	for (CryptoAroFirmaComp firma : componente.getAroFirmaComps()) {
	    if (firma.getAroControfirmaFirmaFiglios().size() > 0) {
		numControfirmeTrovate++;
	    }
	}
	assertEquals(controfirmeAttese, numControfirmeTrovate);
    }

    public static void assertControlloConformitaFirma(CryptoAroFirmaComp firma,
	    VerificheEnums.EsitoControllo esitoControllo) {
	assertEquals(esitoControllo.name(), firma.getTiEsitoContrConforme());
    }

    public static void assertControlloConformitaMarca(CryptoAroMarcaComp marca,
	    VerificheEnums.EsitoControllo esitoControllo) {
	assertEquals(esitoControllo.name(), marca.getTiEsitoContrConforme());
    }

    public static void assertNumeroDiFirmeSottoComponenteOK(CryptoAroCompDoc sottoComponente,
	    int firmeAttese) {
	// FIXME assertEquals(firmeAttese,
	// sottoComponente.getAroBustaCrittogs().get(0).getAroFirmaComps().size());
    }

    public static void assertNumeroDiMarcheSottoComponenteOK(CryptoAroCompDoc sottoComponente,
	    int marcheAttese) {
	// FIXME assertEquals(marcheAttese,
	// sottoComponente.getAroBustaCrittogs().get(0).getAroMarcaComps().size());
    }

    public static void assertNumeroDiBusteSottoComponenteOK(CryptoAroCompDoc sottoComponente,
	    int busteAttese) {
	// FIXME assertEquals(busteAttese, sottoComponente.getAroBustaCrittogs().size());
    }

}
