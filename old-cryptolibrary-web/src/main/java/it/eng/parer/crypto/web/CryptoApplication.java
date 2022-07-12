package it.eng.parer.crypto.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import it.eng.parer.crypto.web.config.CustomBanner;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaRepositories("it.eng.parer.crypto.jpa.repository")
@EntityScan("it.eng.parer.crypto.jpa.entity")
@ImportResource("classpath*:CryptoLibrarySpringConfig.xml")
public class CryptoApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        /**
         * https://stackoverflow.com/questions/13482020/encoded-slash-2f-with-spring-requestmapping-path-param-gives-http-400
         */
        // System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

        SpringApplication application = new SpringApplication(CryptoApplication.class);
        application.setBanner(new CustomBanner());
        application.run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CryptoApplication.class);
    }

}
