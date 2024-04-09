
## 1.10.1 (12-02-2024)

### Bugfix: 1
- [#31069](https://parermine.regione.emilia-romagna.it/issues/31069) Risoluzione errore ORACLE nella verifica firma "SQL Error: 17268, SQLState: 99999 (Year out of range)"

## 1.10.0 (05-12-2023)

### Novità: 1
- [#30313](https://parermine.regione.emilia-romagna.it/issues/30313) Aggiunta di un servizio per sbustare i documenti XML.P7M

## 1.9.0 (13-11-2023)

### Novità: 1
- [#30840](https://parermine.regione.emilia-romagna.it/issues/30840) Aggiornamento documentazione di progetto

## 1.8.0 (02-10-2023)

### Novità: 1
- [#30471](https://parermine.regione.emilia-romagna.it/issues/30471) Utilizzo della modalità fluent di slf4j

## 1.7.0 (16-08-2023)

### Novità: 1
- [#29864](https://parermine.regione.emilia-romagna.it/issues/29864) Aggiornamento librerie obsolete 2023

## 1.6.0 (26-06-2023)

### Bugfix: 2
- [#29789](https://parermine.regione.emilia-romagna.it/issues/29789) Correzione gestione cancellazione dei file temporanei
- [#29612](https://parermine.regione.emilia-romagna.it/issues/29612) Correzione errata valorizzazione del TipoRiferimentoTemporaleUsato in caso di verifica Crypto effettuata alla data di versamento

### Novità: 1
- [#29694](https://parermine.regione.emilia-romagna.it/issues/29694) Introduzione degli attribute su reponse dei servizi (ETag + last-modified) e aggiornamento spring boot

## 1.5.0 (19-05-2023)

### Novità: 1
- [#28516](https://parermine.regione.emilia-romagna.it/issues/28516) Estensione API di verifica firma con introduzione parametro booleano "includeCertificateAndRevocationValues"

## 1.4.1 (23-03-2023)

### Bugfix: 1
- [#29099](https://parermine.regione.emilia-romagna.it/issues/29099)  Correzione nome parametro utilizzato per attivazione pagina amministrativa

## 1.4.0 (28-02-2023)

### Novità: 1
- [#28007](https://parermine.regione.emilia-romagna.it/issues/28007) Modifica parametri di input per object storage

## 1.3.1 (26-10-2022)

### Bugfix: 1
- [#27903](https://parermine.regione.emilia-romagna.it/issues/27903) Correzione meccanismo di gestione di schedulazione Job di aggiornamento CA/CRL

## 1.3.0 (19-10-2022)

### Novità: 1
- [#27362](https://parermine.regione.emilia-romagna.it/issues/27362) Analisi librerie obsolete 2022

## 1.2.8 (20-04-2022)

### Novità: 1
- [#27099](https://parermine.regione.emilia-romagna.it/issues/27099) Aggiornamento Springboot per vulnerabilità Spring4Shell

## 1.2.7 (23-03-2022)

### Bugfix: 1
- [#26968](https://parermine.regione.emilia-romagna.it/issues/26968) Errore durante l'esecuzione dei test di non regressione

## 1.2.6 (21-03-2022)

### Novità: 2
- [#26938](https://parermine.regione.emilia-romagna.it/issues/26938) Restituire il DN del subject oltre a quello dell'issuer
- [#26837](https://parermine.regione.emilia-romagna.it/issues/26837) migrazione DB crl da MySQL a ORACLE

## 1.2.5 (08-02-2022)

### Novità: 2
- [#26738](https://parermine.regione.emilia-romagna.it/issues/26738) creazione del db per il micro-servizio crypto
- [#26664](https://parermine.regione.emilia-romagna.it/issues/26664) Aggiornamento librerie obsolete primo quadrimestre 2021

## 1.2.4 (09-12-2021)

### Novità: 1
- [#26362](https://parermine.regione.emilia-romagna.it/issues/26362)  Gestione system logging attraverso logback

## 1.2.3

### Novità: 1
- [#26271](https://parermine.regione.emilia-romagna.it/issues/26271) Gestire più di una marca temporale su un singolo componente

## 1.2.2

### Bugfix: 1
- [#25764](https://parermine.regione.emilia-romagna.it/issues/25764) Correzione recupero mimetype 

## 1.2.1

### Novità: 1
- [#25401](https://parermine.regione.emilia-romagna.it/issues/25401) Aggiornamento jdk versione 11

## 1.2.0

### Novità: 1
- [#25245](https://parermine.regione.emilia-romagna.it/issues/25245) Aggiornamento immagine Ubi RedHat JDK 11

## 1.1.6 (20-05-2021)

### Novità: 1
- [#25134](https://parermine.regione.emilia-romagna.it/issues/25134) Aggiornamento immagine Ubi RedHat
## 0.0.2-SNAPSHOT (2019-06-06) 

* 6d0db93 | 2019-06-05T18:21:13+02:00 @ abiltiata h2 console parer-svil  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* ff04fc7 | 2019-06-05T17:08:16+02:00 @ openshift config + fix varie  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* 515d413 | 2019-06-04T14:26:32+02:00 @ Gestione Job + JPA  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* caa63f8 | 2019-06-03T17:42:04+02:00 @ Correzione package per Jackson e scope maven  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
* 0e1da79 | 2019-06-03T16:43:43+02:00 @ fix code  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* 515612c | 2019-06-03T16:10:01+02:00 @ fix varie  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* 7dd78de | 2019-05-31T18:39:09+02:00 @ fix code, change packaging layout / have to fix jersey  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* b89d0cc | 2019-05-30T18:49:01+02:00 @ MEV refs #18878 : first commit  (Stefano Sinatti - stefano.sinatti@regione.emilia-romagna.it)
* 78bc4c1 | 2019-06-05T12:27:26+02:00 @ Il servizio restituisce correttamente l'output  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
* 3ecd1e8 | 2019-06-04T11:30:58+02:00 @ Verifica firma con entiry come output  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
* f7cfc83 | 2019-06-03T16:44:30+02:00 @ Inizio compilazione del wrapper  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
* 91f84a0 | 2019-05-31T15:12:24+02:00 @ Refactor delle eccezioni  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
* c823c3e | 2019-05-30T18:21:21+02:00 @ Tolte parecchie classi dal model  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
* aeadc36 | 2019-05-30T16:28:03+02:00 @ Agginti nuovi modelli  (Lorenzo Snidero - Lorenzo.Snidero@Regione.Emilia-Romagna.it)
