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

package it.eng.parer.crypto.service.helper;

import java.math.BigDecimal;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.eng.crypto.CryptoConstants;
import it.eng.crypto.bean.ConfigBean;
import it.eng.crypto.exception.CryptoStorageException;
import it.eng.crypto.provider.MyProvider;
import it.eng.crypto.storage.IConfigStorage;
import it.eng.parer.crypto.jpa.entity.CryConfig;
import it.eng.parer.crypto.jpa.entity.CryConfigPK;
import it.eng.parer.crypto.jpa.repository.CryConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;

/**
 * Session Bean implementation class Config. Il nome deve essere
 * {@link CryptoConstants#ICONFIGSTORAGE }
 */
@Service(CryptoConstants.ICONFIGSTORAGE)
@Transactional
public class ConfigHelper implements IConfigStorage {

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

    public void deleteConfig(String subjectDN, String keyId, BigDecimal nmOrdine)
	    throws CryptoStorageException {
	CryConfigPK pk = new CryConfigPK(subjectDN, keyId, nmOrdine);
	Optional<CryConfig> res = repository.findById(pk);
	if (res.isPresent()) {
	    repository.delete(res.get());
	    repository.flush();
	}

    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void upsertConfig(ConfigBean conf) throws CryptoStorageException {
	CryConfigPK pk = new CryConfigPK(conf.getSubjectDN(), conf.getKeyId(),
		conf.getNiOrdUrlDistribCrl());
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

    public ConfigBean retriveConfig(String subjectDN, String keyId, BigDecimal nmOrdine)
	    throws CryptoStorageException {
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
     * @param em entityManager
     */
    public void setEm(EntityManager em) {
	this.em = em;
    }

}
