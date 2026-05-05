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

package it.eng.parer.crypto.web.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import it.eng.crypto.data.SignerUtil;
import it.eng.parer.crypto.service.util.CommonsHttpClient;

// https://docs.spring.io/spring-boot/docs/1.5.2.RELEASE/reference/htmlsingle/#boot-features-external-config-application-property-files
// SEE 24.6.4 YAML shortcomings
// @PropertySource("classpath:application.properties")
// https://stackoverflow.com/questions/51008382/why-spring-boot-application-doesnt-require-enablewebmvc
// @EnableWebMvc
@Configuration
@ComponentScan("it.eng.parer.crypto.service")
@PropertySource("classpath:git.properties")
// @ImportResource("classpath*:CryptoLibrarySpringConfig.xml")
public class AppConfiguration implements WebMvcConfigurer {

    private final Logger log = LoggerFactory.getLogger(AppConfiguration.class);

    @Value("${cron.thread.pool.size}")
    int threadPoolSize;

    /*
     * Standard httpclient
     */
    // default 60 s
    @Value("${parer.crypto.uriloader.httpclient.timeout:60}")
    int httpClientTimeout;

    // default 60 s
    @Value("${parer.crypto.uriloader.httpclient.timeoutsocket:60}")
    int httpClientSocketTimeout;

    // default 4
    @Value("${parer.crypto.uriloader.httpclient.connectionsmaxperroute:4}")
    int httpClientConnectionsmaxperroute;

    // default 40
    @Value("${parer.crypto.uriloader.httpclient.connectionsmax:40}")
    int httpClientConnectionsmax;

    // defult 60s
    @Value("${parer.crypto.uriloader.httpclient.timetolive:60}")
    long httpClientTimeToLive;

    // default false
    @Value("${parer.crypto.uriloader.httpclient.no-ssl-verify:false}")
    boolean noSslVerify;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // static resources
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // swagger
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * Thread for managing Jobs
     *
     * @return ThreadPoolTaskScheduler
     */
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskScheduler.setPoolSize(threadPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix("job-scarico-CA-CRL-");
        threadPoolTaskScheduler.setThreadPriority(Thread.MIN_PRIORITY);
        log.atDebug().log("Creazione pool di thread per i job di scarico CA/CRL con dimensione "
                + threadPoolSize);
        return threadPoolTaskScheduler;
    }

    /*
     * Utilizzato dai JOB / per ottenere un singleton
     */
    @Bean
    public SignerUtil signerUtil(ApplicationContext applicationContext) {
        return SignerUtil.newInstance(applicationContext);
    }

    @Bean
    public OpenAPI cryptoOpenAPI() {
        return new OpenAPI().info(new Info().title("Verifica firma CRYPTO")
                .description("Microserivice per verifica firma basato su cryptolibrary")
                .version((StringUtils.isNotBlank(getClass().getPackage().getImplementationVersion())
                        ? getClass().getPackage().getImplementationVersion()
                        : "")));
    }

    /** CUSTOM HTTP CLIENT ! **/
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public CommonsHttpClient commonsHttpClient() {
        CommonsHttpClient commonsHttpClient = new CommonsHttpClient();
        // NOTA timeout impostabile (da configurazione!)
        commonsHttpClient.setHttpClientTimeout(httpClientTimeout);
        commonsHttpClient.setHttpClientConnectionsmax(httpClientConnectionsmax);
        commonsHttpClient.setHttpClientSocketTimeout(httpClientSocketTimeout);
        //
        commonsHttpClient.setHttpClientConnectionsmaxperroute(httpClientConnectionsmaxperroute);
        commonsHttpClient.setHttpClientTimeToLive(httpClientTimeToLive);
        //
        commonsHttpClient.setNoSslVerify(noSslVerify);
        return commonsHttpClient;
    }
}
