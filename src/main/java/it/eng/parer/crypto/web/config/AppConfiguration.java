/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.crypto.web.config;

import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import it.eng.crypto.data.SignerUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

//https://docs.spring.io/spring-boot/docs/1.5.2.RELEASE/reference/htmlsingle/#boot-features-external-config-application-property-files
//SEE 24.6.4 YAML shortcomings
//@PropertySource("classpath:application.properties")
//https://stackoverflow.com/questions/51008382/why-spring-boot-application-doesnt-require-enablewebmvc
//@EnableWebMvc
@Configuration
@ComponentScan("it.eng.parer.crypto.service")
@PropertySource("classpath:git.properties")
// @ImportResource("classpath*:CryptoLibrarySpringConfig.xml")
public class AppConfiguration implements WebMvcConfigurer {

    @Autowired
    Environment env;

    @Value("${cron.thread.pool.size}")
    int threadPoolSize;

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationContext applicationContext;

    /**
     *
     * FIXME : da gestire diversamente (problematica chiamate con '/')
     * https://stackoverflow.com/questions/13482020/encoded-slash-2f-with-spring-requestmapping-path-param-gives-http-400
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setDefaultEncoding(Charset.defaultCharset().name());
        urlPathHelper.setUrlDecode(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // static resources
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // swagger
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
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
        LOG.debug("Creazione pool di thread per i job di scarico CA/CRL con dimensione " + threadPoolSize);
        return threadPoolTaskScheduler;
    }

    /*
     * Utilizzato dai JOB / per ottenere un singleton
     */
    @Bean
    public SignerUtil signerUtil() {
        return SignerUtil.newInstance(applicationContext);
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        List<MediaType> supportedMediaTypes = new ArrayList<>(
                mappingJackson2HttpMessageConverter.getSupportedMediaTypes());

        supportedMediaTypes.add(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        supportedMediaTypes.add(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.ISO_8859_1));

        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(supportedMediaTypes);

        return mappingJackson2HttpMessageConverter;
    }

    @Bean
    public OpenAPI cryptoOpenAPI() {
        return new OpenAPI().info(new Info().title("Verifica firma CRYPTO")
                .description("Microserivice per verifica firma basato su cryptolibrary")
                .version((StringUtils.isNotBlank(getClass().getPackage().getImplementationVersion())
                        ? getClass().getPackage().getImplementationVersion() : "")));
    }
}
