<img src="https://spring.io/images/spring-logo-9146a4d3298760c2e7e49595184e1975.svg" width="300">


# CRYPTO

Esternalizzazione della cryptolibrary di sacer.

## Console amministratori

Presente pagina per amministratori /admin.

### Ambiente di sviluppo / localhost

- User: admin
- Password: admin
### Openshift

Su Openshift sono stati configurati tre diversi tipo di deploy, questo al fine di distinguere l'applicazione su tre diversi "ambienti" : Sviluppo / Test e PreProduzione. Le credenziali vengono generate attraverso la craezione della nuova applicazione su singolo namespace, per mezzo di un [template]([src/openshift/crypto-template.yml](https://gitlab.ente.regione.emr.it/parer/okd/verificafirma-crypto-config/-/blob/920a88e948b4fd530a1e10f66de3930dc0740ad7/crypto-template.yml)) standard (verifica su secrets).


## Configurazioni

System property 

su jboss:

```xml
<property name="jackson.deserialization.whitelist.packages" value="it.eng.parer"/>
```

altrimenti

```
 -Djackson.deserialization.whitelist.packages=it.eng.parer
```

TSA (jndi):

```java
    @Resource(lookup = "java:global/old-crypto/TSAServiceURL")
    private String TSAServiceURL;
    @Resource(lookup = "java:global/old-crypto/TSAAuthScope")
    private String TSAAuthScope;
    @Resource(lookup = "java:global/old-crypto/TSAUser")
    private String TSAUser;
    @Resource(lookup = "java:global/old-crypto/TSAPass")
    private String TSAPass;
```
## Rilascio su Openshift

Vedere guida per il rilascio [OKD.md](OKD.md).

## Installazione applicazione come servizio/demone

Vedere guida all'installazione [INSTALL.md](INSTALL.md).

# Appendice
## Passaggio a JDK 11 

L'applicazione è basata sulla versione 11 di Java, nata con la versione 8. In fase di migrazione, sono stati effettuati i seguenti passaggi: 

1. include delle dipendenze jaxb e javax.activtion mancanti sul runtime della 11
2. exclude delle dipendenze xml-apis che riferiscono a moduli noti di java e che quindi non devono essere "importate" da moduli di terze parti
3. modifica del pom.xml di progetto con quanto elencato nei primi due punti con l'aggiunta del compilatore per la versione 11 di Java
   
## Spring Boot

Alcuni riferimenti:

- Configurazioni server / altro  https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/reference/htmlsingle/#common-application-properties

