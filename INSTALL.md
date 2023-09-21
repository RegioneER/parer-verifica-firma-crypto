# Installazione

Di seguito si riportano i passaggi operativi al fine di installare il microservizio come processo / demone.
Oltre che rilasciabile su architettura Openshift, l'applicazione può essere gestita come processo / demone all'interno di un apposito server.

Esempio di installazione [crypto.zip](src/standalone/crypto.zip).

**Nota:** all'interno del file mancante il jar eseguibile (vedi sotto con dettagli su struttura directory e generazione del jar).

### Unix con systemd

Si riportano i macro titoli con le attività da svolgere per installazione e avvio dell'applicazione come demone

##### Creare directory e relativa sotto-struttura in cui verranno depositati i file applicativi 

Si riporta un esempio in cui la directory principale è **/opt/crypto**

```
|____old-cryptolibrary-web.jar
|____bin
| |____crypto
| |____env.conf
|____db
|____config
| |____logback-spring.xml
| |____application.yaml
|____template
| |____crypto.service
|____logs
```

* old-cryptolibrary-web.jar : jar eseguibile 
* bin : contiene il bash script per effettuare lo start dell'applicativo
* template : file service da editare con i dati mancanti (user = utente creato in precedenza e directory radice dell'installazione, e.g. /opt/crypto)
* config : file di configurazione dell'applicativo (application.yaml) e la configurazione di logback (logback-spring.xml) per i log su file con rolling giornaliero
* logs : log applicativi
* db : l'applicazione utilizza H2 configurato su file (vedi application.yaml), questa directory ospita il DB H2 configurato

Nota : è possibile generare il jar attraverso una maven build con apposito profilo : 

```sh
mvn clean install -DskipTests -Pfatjar
```
Il jar da depositore sotto la direcotory precedentemente creata, lo si può trovare sotto old-cryptolibrary-web/target/.

##### Creare, se già non esiste, un apposito utente/gruppo con diritti di scrittura/lettura/esecuzione su directory/sottodirectory e relativi file, precedentemente creata:

Esempio
```
useradd verificafirma 
chown -R verificafirma /opt/crypto
```
In questo modo solo l'utente creato potra accedere alla directory applicativa.
Inoltre è necessario impostare lo script bash per l'esecuzione.

Esempio

```sh
chmod  u+x /opt/crypto/bin/crypto
```
Di seguito il contenuto della direcotry bin.

**env.conf**
```sh 
# launcher environmet 

APP_BINARY="old-cryptolibrary-web.jar"
APP_JAVA_OPTS="-Xmx4048m"
APP_LOGGING="-Dlogging.config=config/logback-spring.xml -Dlogging.path=logs"
APP_CONFIG="-Dspring.config.location=file:config/application.yaml,classpath:application.yaml"

```

**crypto**
```sh 
#!/bin/bash

# env 
source bin/env.conf
# laucher 
/usr/bin/java $APP_JAVA_OPTS -jar $APP_BINARY $APP_LOGGING $APP_CONFIG
```

#### Creare file sotto /etc/systemd/system

Al fine di controllare il processo applicativo mediante systemd, dovrà essere creato l'apposito file service.
Nella directory template è presente il file .service di esempio:

```sh
# /etc/systemd/system/crypto.service

[Unit]
Description=Verifica firma CRYPTO
After=syslog.target network.target
	 
[Service]
Type=simple
User=<utente con cui viene eseguito il processo>
Restart=on-failure
RestartSec=3s

WorkingDirectory=<directory completa con installazione crypto>

ExecStart=<directory completa con installazione crypto>/bin/crypto 
SuccessExitStatus=143 

[Install] 
WantedBy=multi-user.target
```
Nello specifico, sono da indicare l'utente applicativo e la working directory, ossia, il path completo della directory in cui sono stati riversati i file applicativi.
Si riporta un esempio completo: 
```sh
# /etc/systemd/system/crypto.service

[Unit]
Description=Verifica firma CRYPTO
After=syslog.target network.target
	 
[Service]
Type=simple
User=verificafirma
Restart=on-failure
RestartSec=3s

WorkingDirectory=/opt/crypto

ExecStart=/opt/crypto/bin/crypto 
SuccessExitStatus=143 

[Install] 
WantedBy=multi-user.target
```
Al fine di rendere effettiva l'installazione del nuovo servizio è necessario eseguire il seguente comando: 

```sh
systemctl daemon-reload
```
L'applicazione è quasi pronta per essere eseguita.

Ultimo passagio riguarda le configurazioni applicative sulla base dell'architettura/necessità.
Si riporta la configurazione consigliata (base): 

**application.yaml**
```yaml
server:
  port: 8091
  
# LOGGING
logging: 
  pattern:
    console: "%d %-5p [%c] [%t] [%X{uuid}] %m %n"
  level:
    root: INFO
    org.springframework: INFO
    org.apache: INFO
    org.hibernate: INFO
    it.eng.parer.crypto: INFO
     
spring:
  profiles:
    active: daemon
  security: 
    user:
      name: prova
      password: prova
      roles: ADMIN  
  datasource:
    url: jdbc:h2:/opt/crypto/db/cryptodb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE 
    driverClassName: org.h2.Driver
    username: sa
    password: password
    hikari:
      #idle-timeout: 10000
      maximum-pool-size: 20
      #minimum-idle: 5
      pool-name: ParerCryptoHikariPool
  jpa:
    open-in-view: false
    database-platform: org.hibernate.dialect.H2Dialect
    show_sql: false
    hibernate:
      max_fetch_depth: 3
      ddl-auto: update
      hbm2ddl:
        auto: update
  h2:
    console:
      enabled: true
      path: /admin/h2-console
      settings:
        web-allow-others: true
  main:
    allow-bean-definition-overriding: true
    
# CRON  
cron:
  thread:
    pool:
      size: 2
  ca:
   enable: true
   initial-delay: 180000
   # ogni 15 minuti
   delay: 900000
  crl:
   enable: true    
   initial-delay: 240000
   # ogni 15 minuti
   delay: 900000
```

**logback-spring.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}/}crypto.log}"/>

    <appender name="ROLLING-FILE"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>
        <file>${LOG_FILE}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover -->
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="ROLLING-FILE"/>
    </root>

</configuration>
```


Questo era l'ultimo passaggio per rendere operativo l'applicativo.
Per un primo test è sufficiente eseguire il comando:

```sh
systemctl start crypto
```
Per verificarne lo stato: 
```sh
systemctl status crypto

● crypto.service - Verifica firma CRYPTO
   Loaded: loaded (/etc/systemd/system/crypto.service; disabled; vendor preset: enabled)
   Active: active (running) since Fri 2020-07-10 15:02:01 CEST; 3s ago
 Main PID: 22565 (java)
    Tasks: 29 (limit: 4915)
   CGroup: /system.slice/crypto.service
           └─22565 /usr/bin/java -jar old-cryptolibrary-web.jar -Dlogging.config=/opt/crypto/config/logback-spring.xml -Dlogging.path=/opt/crypto/logs -Xmx2048m --spring.config.location=file:///opt/crypto/config/application.yaml,classpa

lug 10 15:02:01 kubuhp systemd[1]: Started Verifica firma CRYPTO.
lug 10 15:02:02 kubuhp java[22565]: LOGBACK: No context given for c.q.l.core.rolling.SizeAndTimeBasedRollingPolicy@1146147158
lug 10 15:02:02 kubuhp java[22565]: ================================================================
lug 10 15:02:02 kubuhp java[22565]:          @CRYPTO v.1.1.2-SNAPSHOT
lug 10 15:02:02 kubuhp java[22565]: ================================================================
```
Per lo stop del processo: 
```sh
systemctl stop crypto
```
Si consiglia inoltre di impostare il processo al boot del server: 
```sh
systemctl enable crypto
```
Il microservizio di verifica firma CRYPTO è quindi operativo e contattabile all'indirizzo : http://localhost:8091, host e porta secondo l'esempio completo di configurazione sopra riportata.

# Appendice

Riferimento / Guida : https://www.baeldung.com/spring-boot-app-as-a-service
