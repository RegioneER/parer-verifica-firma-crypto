package it.eng.parer.crypto.web.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class CryptoSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.csrf().disable() // no csrf
                .authorizeRequests() // autorizza
                .antMatchers("/css/**", "/js/**", "/images/**, /webjars/**, /swagger-ui.html").permitAll() // risorse
                                                                                                           // statiche
                .antMatchers("/v2/**").permitAll() // versione 2 (verifica firme)
                .antMatchers("/v1/**").permitAll() // versione 1 (tutto il resto)
                .antMatchers("/actuator/shutdown").hasRole("ADMIN") // solo admin per shutdown
                .antMatchers("/admin/**").hasRole("ADMIN") // solo admin per /admin
                .and() // form login
                .formLogin().defaultSuccessUrl("/admin") // url predefinita
                .and() // logout form
                .logout().deleteCookies("JSESSIONID").logoutSuccessUrl("/").permitAll();

        /*
         * h2 console https://springframework.guru/using-the-h2-database-console-in-spring-boot-with-spring-security/
         */
        http.headers().frameOptions().disable();

    }

}
