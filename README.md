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

# Requisiti e librerie utilizzate

Requisiti minimi per installazione: 

- Sistema operativo : consigliato Linux server (in alternativa compatibilità con Windows server)
- Java versione 17 (OpenJDK / Oracle)
- Kubernetes / Docker : se rilasciato attraverso container oppure si esegue una build del progetto attraverso il profilo maven **uber-jar** per ottenere il JAR eseguibile (vedi paragrafi precendeti)

# Librerie utilizzate


|  GroupId | ArtifactId  | Version |
|:---:|:---:|:---:|
|be.fedict.eid-tsl|eid-tsl-core|1.0.0-20100816|
|ch.qos.logback|logback-classic|1.4.8|
|ch.qos.logback|logback-core|1.4.8|
|com.adobe.xmp|xmpcore|5.1.2|
|com.drewnoakes|metadata-extractor|2.6.2|
|com.fasterxml.jackson.core|jackson-annotations|2.14.3|
|com.fasterxml.jackson.core|jackson-core|2.14.3|
|com.fasterxml.jackson.core|jackson-databind|2.14.3|
|com.fasterxml.jackson.dataformat|jackson-dataformat-yaml|2.14.3|
|com.fasterxml.jackson.datatype|jackson-datatype-jdk8|2.14.3|
|com.fasterxml.jackson.datatype|jackson-datatype-jsr310|2.14.3|
|com.fasterxml.jackson.module|jackson-module-parameter-names|2.14.3|
|com.fasterxml|classmate|1.5.1|
|com.google.code.findbugs|jsr305|3.0.2|
|com.google.errorprone|error_prone_annotations|2.18.0|
|com.google.guava|failureaccess|1.0.1|
|com.google.guava|guava|32.1.1-jre|
|com.google.guava|listenablefuture|9999.0-empty-to-avoid-conflict-with-guava|
|com.google.j2objc|j2objc-annotations|2.8|
|com.googlecode.juniversalchardet|juniversalchardet|1.0.3|
|com.googlecode.mp4parser|isoparser|1.0.2|
|com.h2database|h2|2.1.214|
|com.itextpdf|itextpdf|5.2.1|
|com.jayway.jsonpath|json-path|2.7.0|
|com.oracle.database.jdbc|ojdbc11|21.7.0.0|
|com.pff|java-libpst|0.8.1|
|com.sun.activation|jakarta.activation|1.2.2|
|com.sun.istack|istack-commons-runtime|4.1.2|
|com.sun.xml.messaging.saaj|saaj-impl|3.0.2|
|com.uwyn|jhighlight|1.0|
|com.vaadin.external.google|android-json|0.0.20131108.vaadin1|
|com.zaxxer|HikariCP|5.0.1|
|commons-codec|commons-codec|1.15|
|commons-httpclient|commons-httpclient|3.1|
|commons-io|commons-io|2.4|
|commons-lang|commons-lang|2.4|
|de.l3s.boilerpipe|boilerpipe|1.1.0|
|edu.ucar|netcdf|4.2.20|
|edu.ucar|unidataCommon|4.2.20|
|io.micrometer|micrometer-commons|1.10.9|
|io.micrometer|micrometer-core|1.10.9|
|io.micrometer|micrometer-observation|1.10.9|
|io.micrometer|micrometer-registry-prometheus|1.10.9|
|io.netty|netty-buffer|4.1.94.Final|
|io.netty|netty-codec-dns|4.1.94.Final|
|io.netty|netty-codec-http2|4.1.94.Final|
|io.netty|netty-codec-http|4.1.94.Final|
|io.netty|netty-codec-socks|4.1.94.Final|
|io.netty|netty-codec|4.1.94.Final|
|io.netty|netty-common|4.1.94.Final|
|io.netty|netty-handler-proxy|4.1.94.Final|
|io.netty|netty-handler|4.1.94.Final|
|io.netty|netty-resolver-dns-classes-macos|4.1.94.Final|
|io.netty|netty-resolver-dns-native-macos|osx-x86_64|
|io.netty|netty-resolver-dns|4.1.94.Final|
|io.netty|netty-resolver|4.1.94.Final|
|io.netty|netty-transport-classes-epoll|4.1.94.Final|
|io.netty|netty-transport-native-epoll|linux-x86_64|
|io.netty|netty-transport-native-unix-common|4.1.94.Final|
|io.netty|netty-transport|4.1.94.Final|
|io.projectreactor.netty|reactor-netty-core|1.1.9|
|io.projectreactor.netty|reactor-netty-http|1.1.9|
|io.projectreactor|reactor-core|3.5.8|
|io.prometheus|simpleclient|0.16.0|
|io.prometheus|simpleclient_common|0.16.0|
|io.prometheus|simpleclient_tracer_common|0.16.0|
|io.prometheus|simpleclient_tracer_otel|0.16.0|
|io.prometheus|simpleclient_tracer_otel_agent|0.16.0|
|io.swagger.core.v3|swagger-annotations-jakarta|2.2.9|
|io.swagger.core.v3|swagger-core-jakarta|2.2.9|
|io.swagger.core.v3|swagger-models-jakarta|2.2.9|
|it.eng.parer|cryptolibrary|1.12.7|
|it.eng.parer|eng-mityclib-api|1.0.2|
|it.eng.parer|eng-mityclib-tsa|1.0.2|
|it.eng.parer|eng-mityclib-xades|1.0.2|
|it.eng.parer|eng-sec-provider|1.0.0|
|it.eng.parer|verificafirma-crypto-beans|1.4.0|
|jakarta.activation|jakarta.activation-api|2.1.2|
|jakarta.annotation|jakarta.annotation-api|2.1.1|
|jakarta.inject|jakarta.inject-api|2.0.0|
|jakarta.persistence|jakarta.persistence-api|3.1.0|
|jakarta.transaction|jakarta.transaction-api|2.0.1|
|jakarta.validation|jakarta.validation-api|3.0.2|
|jakarta.xml.bind|jakarta.xml.bind-api|4.0.0|
|jakarta.xml.soap|jakarta.xml.soap-api|3.0.0|
|javax.activation|activation|1.1|
|javax.mail|mail|1.4.7|
|javax.xml.bind|jaxb-api|2.3.0|
|jdom|jdom|1.0|
|net.bytebuddy|byte-buddy-agent|1.12.23|
|net.bytebuddy|byte-buddy|1.12.23|
|net.jcip|jcip-annotations|1.0|
|net.logstash.logback|logstash-logback-encoder|7.2|
|net.minidev|accessors-smart|2.4.11|
|net.minidev|json-smart|2.4.11|
|net.sourceforge.jmatio|jmatio|1.0|
|org.antlr|antlr4-runtime|4.10.1|
|org.apache.commons|commons-compress|1.8.1|
|org.apache.commons|commons-lang3|3.12.0|
|org.apache.httpcomponents|httpclient|4.5.14|
|org.apache.httpcomponents|httpcore|4.4.16|
|org.apache.james|apache-mime4j-core|0.7.2|
|org.apache.james|apache-mime4j-dom|0.7.2|
|org.apache.logging.log4j|log4j-api|2.19.0|
|org.apache.logging.log4j|log4j-to-slf4j|2.19.0|
|org.apache.pdfbox|fontbox|1.8.8|
|org.apache.pdfbox|jempbox|1.8.8|
|org.apache.pdfbox|pdfbox|1.8.8|
|org.apache.poi|poi-ooxml-schemas|3.11|
|org.apache.poi|poi-ooxml|3.11|
|org.apache.poi|poi-scratchpad|3.11|
|org.apache.poi|poi|3.11|
|org.apache.santuario|xmlsec|1.4.5|
|org.apache.taglibs|taglibs-standard-impl|1.2.5|
|org.apache.taglibs|taglibs-standard-jstlel|1.2.5|
|org.apache.taglibs|taglibs-standard-spec|1.2.5|
|org.apache.tika|tika-core|1.7|
|org.apache.tika|tika-parsers|1.7|
|org.apache.tomcat.embed|tomcat-embed-core|10.1.11|
|org.apache.tomcat.embed|tomcat-embed-el|10.1.11|
|org.apache.tomcat.embed|tomcat-embed-websocket|10.1.11|
|org.apache.xmlbeans|xmlbeans|2.6.0|
|org.apiguardian|apiguardian-api|1.1.2|
|org.aspectj|aspectjrt|1.9.19|
|org.aspectj|aspectjweaver|1.9.19|
|org.assertj|assertj-core|3.23.1|
|org.attoparser|attoparser|2.0.6.RELEASE|
|org.bouncycastle|bcmail-jdk16|1.46|
|org.bouncycastle|bcprov-jdk16|1.46|
|org.bouncycastle|bctsp-jdk16|1.46|
|org.ccil.cowan.tagsoup|tagsoup|1.2.1|
|org.checkerframework|checker-qual|3.33.0|
|org.eclipse.angus|angus-activation|2.0.1|
|org.gagravarr|vorbis-java-core|0.6|
|org.gagravarr|vorbis-java-tika|0.6|
|org.glassfish.jaxb|jaxb-runtime|2.3.8|
|org.glassfish.jaxb|txw2|4.0.3|
|org.hamcrest|hamcrest|2.2|
|org.hdrhistogram|HdrHistogram|2.1.12|
|org.hibernate.common|hibernate-commons-annotations|6.0.6.Final|
|org.hibernate.orm|hibernate-core|6.1.7.Final|
|org.hibernate.validator|hibernate-validator|8.0.1.Final|
|org.jboss.logging|jboss-logging|3.5.3.Final|
|org.jboss|jandex|2.4.2.Final|
|org.junit.jupiter|junit-jupiter-api|5.9.3|
|org.junit.jupiter|junit-jupiter-engine|5.9.3|
|org.junit.jupiter|junit-jupiter-params|5.9.3|
|org.junit.jupiter|junit-jupiter|5.9.3|
|org.junit.platform|junit-platform-commons|1.9.3|
|org.junit.platform|junit-platform-engine|1.9.3|
|org.jvnet.staxex|stax-ex|2.1.0|
|org.latencyutils|LatencyUtils|2.0.3|
|org.mockito|mockito-core|4.8.1|
|org.mockito|mockito-junit-jupiter|4.8.1|
|org.objenesis|objenesis|3.2|
|org.opentest4j|opentest4j|1.2.0|
|org.ow2.asm|asm-debug-all|4.1|
|org.ow2.asm|asm|9.3|
|org.reactivestreams|reactive-streams|1.0.4|
|org.skyscreamer|jsonassert|1.5.1|
|org.slf4j|jul-to-slf4j|2.0.7|
|org.slf4j|slf4j-api|2.0.7|
|org.springdoc|springdoc-openapi-starter-common|2.1.0|
|org.springdoc|springdoc-openapi-starter-webmvc-api|2.1.0|
|org.springdoc|springdoc-openapi-starter-webmvc-ui|2.1.0|
|org.springframework.boot|spring-boot-actuator-autoconfigure|3.0.9|
|org.springframework.boot|spring-boot-actuator|3.0.9|
|org.springframework.boot|spring-boot-autoconfigure|3.0.9|
|org.springframework.boot|spring-boot-starter-actuator|3.0.9|
|org.springframework.boot|spring-boot-starter-aop|3.0.9|
|org.springframework.boot|spring-boot-starter-data-jpa|3.0.9|
|org.springframework.boot|spring-boot-starter-jdbc|3.0.9|
|org.springframework.boot|spring-boot-starter-json|3.0.9|
|org.springframework.boot|spring-boot-starter-logging|3.0.9|
|org.springframework.boot|spring-boot-starter-reactor-netty|3.0.9|
|org.springframework.boot|spring-boot-starter-security|3.0.9|
|org.springframework.boot|spring-boot-starter-test|3.0.9|
|org.springframework.boot|spring-boot-starter-thymeleaf|3.0.9|
|org.springframework.boot|spring-boot-starter-tomcat|3.0.9|
|org.springframework.boot|spring-boot-starter-validation|3.0.9|
|org.springframework.boot|spring-boot-starter-web|3.0.9|
|org.springframework.boot|spring-boot-starter-webflux|3.0.9|
|org.springframework.boot|spring-boot-starter|3.0.9|
|org.springframework.boot|spring-boot-test-autoconfigure|3.0.9|
|org.springframework.boot|spring-boot-test|3.0.9|
|org.springframework.boot|spring-boot|3.0.9|
|org.springframework.data|spring-data-commons|3.0.8|
|org.springframework.data|spring-data-jpa|3.0.8|
|org.springframework.security|spring-security-config|6.0.5|
|org.springframework.security|spring-security-core|6.0.5|
|org.springframework.security|spring-security-crypto|6.0.5|
|org.springframework.security|spring-security-web|6.0.5|
|org.springframework.ws|spring-ws-core|4.0.5|
|org.springframework.ws|spring-xml|4.0.5|
|org.springframework|spring-aop|6.0.11|
|org.springframework|spring-aspects|6.0.11|
|org.springframework|spring-beans|6.0.11|
|org.springframework|spring-context|6.0.11|
|org.springframework|spring-core|6.0.11|
|org.springframework|spring-expression|6.0.11|
|org.springframework|spring-jcl|6.0.11|
|org.springframework|spring-jdbc|6.0.11|
|org.springframework|spring-orm|6.0.11|
|org.springframework|spring-oxm|6.0.11|
|org.springframework|spring-test|6.0.11|
|org.springframework|spring-tx|6.0.11|
|org.springframework|spring-web|6.0.11|
|org.springframework|spring-webflux|6.0.11|
|org.springframework|spring-webmvc|6.0.11|
|org.thymeleaf.extras|thymeleaf-extras-springsecurity6|3.1.1.RELEASE|
|org.thymeleaf|thymeleaf-spring6|3.1.1.RELEASE|
|org.thymeleaf|thymeleaf|3.1.1.RELEASE|
|org.tukaani|xz|1.5|
|org.unbescape|unbescape|1.1.6.RELEASE|
|org.webjars.bower|google-code-prettify|1.0.5|
|org.webjars|bootstrap|3.4.1|
|org.webjars|jquery|3.6.4|
|org.webjars|swagger-ui|4.18.2|
|org.xmlunit|xmlunit-core|2.9.1|
|org.yaml|snakeyaml|1.33|
|rome|rome|1.0|
|xalan|serializer|2.7.2|
|xalan|xalan|2.7.2|
|xerces|xercesImpl|2.10.0|


## Lista licenze in uso


 * agpl_v3     : GNU Affero General Public License (AGPL) version 3.0
 * apache_v2   : Apache License version 2.0
 * bsd_2       : BSD 2-Clause License
 * bsd_3       : BSD 3-Clause License
 * cddl_v1     : COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0
 * epl_only_v1 : Eclipse Public License - v 1.0
 * epl_only_v2 : Eclipse Public License - v 2.0
 * epl_v1      : Eclipse Public + Distribution License - v 1.0
 * epl_v2      : Eclipse Public License - v 2.0 with Secondary License
 * eupl_v1_1   : European Union Public License v1.1
 * fdl_v1_3    : GNU Free Documentation License (FDL) version 1.3
 * gpl_v1      : GNU General Public License (GPL) version 1.0
 * gpl_v2      : GNU General Public License (GPL) version 2.0
 * gpl_v3      : GNU General Public License (GPL) version 3.0
 * lgpl_v2_1   : GNU General Lesser Public License (LGPL) version 2.1
 * lgpl_v3     : GNU General Lesser Public License (LGPL) version 3.0
 * mit         : MIT-License

# Supporto

Mantainer del progetto è [Engineering Ingegneria Informatica S.p.A.](https://www.eng.it/).

# Contributi

Se interessati a crontribuire alla crescita del progetto potete scrivere all'indirizzo email <a href="mailto:areasviluppoparer@regione.emilia-romagna.it">areasviluppoparer@regione.emilia-romagna.it</a>.

# Credits

Progetto di proprietà di [Regione Emilia-Romagna](https://www.regione.emilia-romagna.it/) sviluppato a cura di [Engineering Ingegneria Informatica S.p.A.](https://www.eng.it/).

# Licenza

Questo progetto è rilasciato sotto licenza GNU Affero General Public License v3.0 or later ([LICENSE.txt](LICENSE.txt)).

# Appendice

## Spring Boot 3.x

Alcuni riferimenti:

- Migrazione Spring boot versione 3 https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide

