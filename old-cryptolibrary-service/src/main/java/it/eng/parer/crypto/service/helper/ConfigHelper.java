package it.eng.parer.crypto.service.helper;

import it.eng.crypto.CryptoConstants;
import it.eng.crypto.bean.ConfigBean;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.provider.MyProvider;
import it.eng.crypto.storage.IConfigStorage;
import it.eng.parer.crypto.jpa.entity.CryConfig;
import it.eng.parer.crypto.jpa.entity.CryConfigPK;
import it.eng.parer.crypto.jpa.repository.CryConfigRepository;

import java.math.BigDecimal;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;

import javax.persistence.EntityManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session Bean implementation class Config. Il nome deve essere {@link CryptoConstants#ICONFIGSTORAGE }
 */
@Service(CryptoConstants.ICONFIGSTORAGE)
@Transactional
public class ConfigHelper implements IConfigStorage {

    Logger log = LoggerFactory.getLogger(ConfigHelper.class);

    @Autowired
    CryConfigRepository repository;

    EntityManager em;

    @PostConstruct
    public void initBC() {

        Security.removeProvider("MyXMLDSig");
        Provider[] provider = Security.getProviders();
        for (int i = 0; i < provider.length; i++) {
            if (provider[i].getName().equals("XMLDSig")) {
                Security.insertProviderAt(new MyProvider(), i + 1);
            }
        }
        Security.addProvider(new MyProvider());

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    public void deleteConfig(String subjectDN, String keyId, BigDecimal nmOrdine) throws CryptoStorageException {
        CryConfigPK pk = new CryConfigPK(subjectDN, keyId, nmOrdine);
        Optional<CryConfig> res = repository.findById(pk);
        if (res.isPresent()) {
            repository.delete(res.get());
            repository.flush();
        }

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void upsertConfig(ConfigBean conf) throws CryptoStorageException {
        CryConfigPK pk = new CryConfigPK(conf.getSubjectDN(), conf.getKeyId(), conf.getNiOrdUrlDistribCrl());
        CryConfig config = null;
        Optional<CryConfig> res = repository.findById(pk);
        if (!res.isPresent()) {
            config = new CryConfig();
        } else {
            config = res.get();
        }
        config.setCrlurl(conf.getCrlURL());
        config.setSubjectdn(conf.getSubjectDN());
        config.setNiOrdUrlDistribCrl(conf.getNiOrdUrlDistribCrl());
        config.setKeyId(conf.getKeyId());

        repository.save(config);
    }

    public List<ConfigBean> retriveAllConfig() throws CryptoStorageException {
        List<ConfigBean> lista = new ArrayList<ConfigBean>();
        @SuppressWarnings("unchecked")
        // List<Object[]> configurazioni = em.createNativeQuery("select conf.subjectdn, conf.crlurl,
        // conf.ni_ord_url_distrib_crl, conf.SUBJECT_KEY_ID from cry_config conf join cry_crl crl on (conf.subjectdn =
        // crl.subjectdn) where crl.NEXT_EXPIRATION < sysdate order by conf.subjectdn,
        // conf.NI_ORD_URL_DISTRIB_CRL").getResultList();
        List<Object[]> configurazioni = repository.joinWithCryCrl();
        if (configurazioni != null) {
            for (Object[] conf : configurazioni) {
                ConfigBean bean = new ConfigBean();
                bean.setSubjectDN((String) conf[0]);
                bean.setCrlURL((String) conf[1]);
                bean.setNiOrdUrlDistribCrl((BigDecimal) conf[2]);
                bean.setKeyId((String) conf[3]);
                lista.add(bean);
            }
        }
        return lista;
    }

    public ConfigBean retriveConfig(String subjectDN, String keyId, BigDecimal nmOrdine) throws CryptoStorageException {
        CryConfigPK pk = new CryConfigPK(subjectDN, keyId, nmOrdine);
        Optional<CryConfig> c = repository.findById(pk);
        ConfigBean bean = null;
        if (c.isPresent()) {
            bean = new ConfigBean();
            bean.setCrlURL(c.get().getCrlurl());
            bean.setSubjectDN(c.get().getSubjectdn());
            bean.setNiOrdUrlDistribCrl(c.get().getNiOrdUrlDistribCrl());
            bean.setKeyId(c.get().getKeyId());
        }
        return bean;
    }

    /**
     * Setter utilizzato da JUnit
     *
     * @param em
     *            entityManager
     */
    public void setEm(EntityManager em) {
        this.em = em;
    }

}
