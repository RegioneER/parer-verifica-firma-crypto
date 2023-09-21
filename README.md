<img src="src/docs/spring-boot.png" width="300">
<br/><br/>

# Verifica Firma CRYPTO 

Fonte template redazione documento:  https://www.makeareadme.com/.


# Descrizione

Microservizio realizzato per effettuare verifica e validazione di documenti con firma digitale. <br/>
Realizzato attraverso framework [Spring Boot](https://spring.io/projects/spring-boot) (versione 3.x) e [OpenJDK 17](https://openjdk.org/projects/jdk/17/), utilizza la libreria proprietaria **Cryptolibray** con la quale sono realizzate le logiche di validazione e verifica della firma digitale.

# Installazione

Di seguito verranno riportati sotto alcuni paragrafi, le modalità possibili con cui è possibile rendere operativo il microservizio. 
## Rilascio su RedHat Openshift

Vedere specifica guida per il rilascio [OKD.md](OKD.md).

### Openshift template

Per la creazione dell'applicazione con risorse necessarie correlate sotto Openshift (https://www.redhat.com/it/technologies/cloud-computing/openshift) viene fornito un apposito template (la solzuzione, modificabile, è basata su Oracle DB) [template](src/main/openshift/verifica-firma-crypto-template.yml).

## Installazione applicazione come servizio/demone

Vedere guida all'installazione [INSTALL.md](INSTALL.md).

# Utilizzo

Basandosi su spring boot, il seguente progetto è dotato di una sorta di "launcher" ossia, una semplice classe Java con main che ne permette l'esecuzione. Inoltre il progetto permette una gestione "profilata" delle configurazioni con il quale eseguire in locale l'applicazione, come previsto dalla dinamiche di spring boot stesso:
- default : è il profilo "standard" di spring boot quello con cui normalmente viene eseguito il processo applicativo; 
- h2: profilo legato al db h2 (è quello di **riferimento**) del progetto, con db in **memoria** (vedi [application-h2.yaml](src/main/resource/application-h2.yaml));
- oracle: profilo legato al db oracle (vedi [application-oracle.yaml](src/main/resource/application-oracle.yaml)); in particolare **username** e **password** dovranno essere fornite allo start dell'applicazione in una delle possibili modalità previste vedi https://www.baeldung.com/spring-boot-command-line-arguments; nel caso specifico, attraverso l'IDE utilizzato con la modalità ```-Ddbusername=user -Ddbpassword=password```

Le configurazioni sono legate ai file yaml che sono gestiti come previsto dai meccanismi di overrinding messi a disposizione, vedi https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config. 

### Esempio override configurazioni

Nel seguente esempio, si riporta una casistica di esecuzione dell'applicazione (attraverso apposito jar) con override delle configurazioni base:

```java
$ java -jar myproject.jar --spring.config.location=\
    optional:classpath:/default.properties,\
    optional:classpath:/override.properties
```
Nota: nell'esempio sopra riportato, si utilizzano file di tipo properties (default previsto da spring boot), mentre in questo caso si è scelto lo standard YAML, non cambiano le dinamiche descritte ma semplicemente l'estenzione (.yaml).

## Migrazione OpenJDK 17

Nei seguenti paragrafi vengono riportate alcune note importanti legate al passaggio alla versione 17 della OpenJDK.
#### Risoluzione java.base unnamede module

Nel passaggio alla versione 17 della OpenJDK runtime si è presentata la seguente eccezione: 

```bash
java.lang.IllegalAccessError: class es.mityc.firmaJava.libreria.utilidades.URIEncoder (in unnamed module @0x3b2c72c2)
cannot access class sun.security.action.GetPropertyAction (in module java.base) because module java.base does not export sun.security.action to unnamed module @0x3b2c72c2
```
Tale problematica deriva dalla recente introduzione (a partire dalla versione 9 della OpenJDK) di un sistema così detto a "moduli" (vedere [documentazione ufficiale](https://openjdk.org/projects/jigsaw/spec/sotms/)) per cui, l'applicazione non può avere accesso, se non esplicitamente indicato, al modulo java.base/sun.security.action. Per tale motivo all'interno dell'artifact prodotto (jar eseguibile) occorre esplicitare l'export di tale modulo: 

```bash
--add-exports java.base/sun.security.action=ALL-UNNAMED
```
la modifica viene attraverso i plugin maven:

```bash
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <target>17</target>
        <source>17</source>
        <compilerArgs>
                <compilerArg>--add-exports</compilerArg>
                <compilerArg>java.base/sun.security.action=ALL-UNNAMED</compilerArg>
            </compilerArgs>
    </configuration>
</plugin>
[...]
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifest>
                <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
            </manifest>
            <!-- export legacy module -->
            <!-- @see java.lang.IllegalAccessError: class
            es.mityc.firmaJava.libreria.utilidades.URIEncoder (in unnamed module @0x3b2c72c2) -->
            <!-- cannot access class sun.security.action.GetPropertyAction (in module java.base)
            because module java.base does not export sun.security.action to unnamed module
            @0x3b2c72c2 -->
            <manifestEntries>
                <Add-Exports>java.base/sun.security.action</Add-Exports>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```
#### Maven plugin Jacoco/Surefire

Come riportato nel paragrafo precedete [Risoluzione java.base unnamede module](#### Risoluzione java.base unnamede module), a seguito dell'introduzione del comando "--add-exports" che permette all'applicazione di accedere al modulo desiderato, è necessaria la modifica per l'esecuzione del profiler **jacoco.exe** introducendo la seguente property su pom.xml:

```xml
<argLine>--add-exports java.base/sun.security.action=ALL-UNNAMED</argLine>
```
utilizzata dai plugin Jacoco e Surefire (https://www.eclemma.org/jacoco/trunk/doc/prepare-agent-mojo.html#propertyName) per la produzione del site con il risultato della coverage dei test di unità.

## Console amministratori

Presente pagina per amministratori /admin.

### Ambiente di sviluppo / localhost

- User: admin
- Password: admin


## Docker build

Per effettuare una build del progetto via Docker è stato predisposto lo standard [Dockerfile](Dockerfile) e una directory [docker_build](docker_build) con all'interno i file da integrare all'immagine base <strong>registry.access.redhat.com/ubi8/openjdk-11</strong>.
La directory [docker_build](docker_build) è strutturata come segue: 
```bash
|____README.md
|____certs
| |____README.md

```
al fine di integrare certificati non presenti di default nell'immagine principale è stata introdotta la sotto-directory [docker_build/certs](docker_build/certs) in cui dovranno essere inseriti gli appositi certificati che verranno "trustati" in fase di build dell'immagine.
La compilazione dell'immagine può essere eseguita con il comando: 
```bash
docker build -t <registry> -f ./Dockerfile --build-arg EXTRA_CA_CERTS_DIR=docker_build/certs .
```

# Supporto

Progetto a cura di [Engineering Ingegneria Informatica S.p.A.](https://www.eng.it/).

# Contributi

Se interessati a crontribuire alla crescita del progetto potete scrivere all'indirizzo email <a href="mailto:areasviluppoparer@regione.emilia-romagna.it">areasviluppoparer@regione.emilia-romagna.it</a>.

# Autori

Proprietà intellettuale del progetto di [Regione Emilia-Romagna](https://www.regione.emilia-romagna.it/) e [Polo Archivisitico](https://poloarchivistico.regione.emilia-romagna.it/).

# Licenza

Questo progetto è rilasciato sotto licenza GNU Affero General Public License v3.0 or later ([LICENSE.txt](LICENSE.txt)).

# Appendice

## Spring Boot 3.x

Alcuni riferimenti:

- Migrazione Spring boot versione 3 https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide

