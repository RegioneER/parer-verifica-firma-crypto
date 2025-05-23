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

package it.eng.parer.crypto.service;

import org.springframework.stereotype.Service;

import it.eng.parer.crypto.model.exceptions.CryptoParerException;
import it.eng.parer.crypto.model.exceptions.ParerError;
import it.eng.parer.crypto.model.exceptions.ParerErrorDoc;

/**
 * Service layer relativo alla descrizione dei casi di errore.
 *
 * @author Snidero_L
 */
@Service
public class DocService {

    /**
     * Compila il modello relativo all'errore (gestito e non).
     *
     * @param codiceErrore codice da decodificare
     * @param selfLink     link della richiesta
     *
     * @return modello degli errori.
     */
    public ParerErrorDoc compilaErrore(String codiceErrore, String selfLink) {

	String tipologiaErrore = null;
	String descrizioneErrore = null;
	String descrizioneTipologiaErrore = null;
	try {
	    ParerError.ErrorCode errorCode = ParerError.ErrorCode.fromUrlFriendly(codiceErrore);
	    tipologiaErrore = errorCode.exceptionType().name();
	    descrizioneTipologiaErrore = "Errore classificato come: " + tipologiaErrore + " - "
		    + decodificaTipologia(errorCode.exceptionType());
	    descrizioneErrore = decodificaErrore(errorCode);
	} catch (IllegalArgumentException e) {
	    throw new CryptoParerException(e).withCode(ParerError.ErrorCode.RESOURCE_NOT_FOUND)
		    .withMessage("Codice errore " + codiceErrore + " non esistente");
	}

	ParerErrorDoc doc = new ParerErrorDoc();
	doc.setCode(codiceErrore);
	doc.setSummary(descrizioneTipologiaErrore);
	doc.setType(tipologiaErrore);
	doc.setDescription(descrizioneErrore);
	doc.setLink(selfLink);
	return doc;
    }

    /**
     * ************************************************************************ DATABASE degli
     * ERRORI. A tendere dovrebbe risiedere su un vero DB.
     * ***********************************************************************
     */
    /**
     * Per il momento non utilizzo il database
     *
     * @param codice di errore codificato
     *
     * @return descrizione del codice di errore.Non viene mai restituito null.
     */
    private String decodificaErrore(ParerError.ErrorCode codice) {

	final String decodifica;
	switch (codice) {
	case RESOURCE_NOT_FOUND:
	    decodifica = "Risorsa non trovata";
	    break;
	case VALIDATION_ERROR:
	    decodifica = "Errore di validazione";
	    break;
	case GENERIC_ERROR:
	    decodifica = "Errore generico";
	    break;
	case CRL_NOT_FOUND:
	    decodifica = "La lista di revoca non è stata trovata";
	    break;
	case CRL_CRYPTO_STORAGE:
	    decodifica = "Errore durante la memorizzazione della lista di revoca.";
	    break;
	case CRL_EXCEPTION:
	    decodifica = "Errore durante la memorizzazione della lista di revoca.";
	    break;
	case CRL_IO:
	    decodifica = "Errore I/O durante la memorizzazione della lista di revoca.";
	    break;
	case TSP_EXCEPTION:
	    decodifica = "Errore durante la generazione del timestamp.";
	    break;
	case TSP_IO:
	    decodifica = "Errore durante la generazione del timestamp.";
	    break;
	case TSP_PROVIDER_ERROR:
	    decodifica = "Errore durante l'utilizzo del provider per le marche temporali. Controllare le credenziali di accesso.";
	    break;
	case TSD_NOT_FOUND:
	    decodifica = "Il provider per la generazione dei timestamp non risulta raggiungibile.Marca temporale non generata.";
	    break;
	case TSD_PROVIDER_ERROR:
	    decodifica = "Errore durante l'utilizzo del provider per le marche temporali. Controllare le credenziali di accesso.";
	    break;
	case CERT_IO:
	    decodifica = "Errore I/O durante la gestione del certificato.";
	    break;
	case CERT_EXCEPTION:
	    decodifica = "Errore durante la gestione del certificato.";
	    break;
	case CERT_PROVIDER_ERROR:
	    decodifica = "Errore durante la gestione del certificato.";
	    break;
	case SIGNATURE_VERIFICATION_IO:
	    decodifica = "Errore durante la fase di verifica firma.";
	    break;
	case SIGNATURE_WRONG_PARAMETER:
	    decodifica = "Parametro errato durante la fase di verifica firma.";
	    break;
	case SIGNATURE_FORMAT:
	    decodifica = "Errore validazione formato del file elaborato.";
	    break;
	default:
	    decodifica = "Errore non gestito";
	}
	return decodifica;
    }

    /**
     * Anche in questo metodo per il momento non utilizzo il DB
     *
     * @param tipologia tipologia di errore
     *
     * @return decodifica della tipologia. Non viene mai restituito null.
     */
    private String decodificaTipologia(ParerError.ExceptionType tipologia) {

	final String decodifica;
	switch (tipologia) {
	case GENERIC:
	    decodifica = "generico";
	    break;
	case CERTIFICATE:
	    decodifica = "certificato digitale";
	    break;
	case CRL:
	    decodifica = "lista di revoca";
	    break;
	case TIME:
	    decodifica = "emissione marca temporale";
	    break;
	default:
	    decodifica = "non gestito";
	}
	return decodifica;
    }

}
