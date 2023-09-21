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

package it.eng.parer.crypto.web.config.security;

import static it.eng.parer.crypto.web.util.EndPointCostants.RESOURCE_INFOS;
import static it.eng.parer.crypto.web.util.EndPointCostants.ROLE_ADMIN;
import static it.eng.parer.crypto.web.util.EndPointCostants.URL_ADMIN_BASE;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/*
 * since spring boot 2.7.0 
 * https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
 * 
 * Note: questa configurazione (default) gestisce la sicurezza legata alla API/UI esposte
 * Le regole su URI pattern match definiscono la "catena" dei permessi, quindi l'ordine è importante.  
 */
@Configuration
@ConditionalOnProperty(name = "parer.crypto.admin-ui.enabled", havingValue = "true", matchIfMissing = true)
public class BaseSecurityConfig {

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

        http.csrf().disable() // disable csrf
                .authorizeHttpRequests() // rule on single path
                .requestMatchers(new AntPathRequestMatcher(URL_ADMIN_BASE + RESOURCE_INFOS)).authenticated().and()
                .httpBasic() // basic auth
                .and() // solo admin
                .authorizeHttpRequests().requestMatchers(new AntPathRequestMatcher(URL_ADMIN_BASE + "/**"))
                .hasRole(ROLE_ADMIN).requestMatchers(new AntPathRequestMatcher("/actuator/shutdown"))
                .hasRole(ROLE_ADMIN).and() // permit all
                .authorizeHttpRequests().anyRequest().permitAll().and() // form login
                .formLogin().defaultSuccessUrl(URL_ADMIN_BASE) // url predefinita
                .permitAll().and() // logout form
                .logout().deleteCookies("JSESSIONID").logoutSuccessUrl("/").permitAll(); // enable

        /*
         * h2 console https://springframework.guru/using-the-h2-database-console-in-spring-boot- with-spring-security/
         */
        http.headers().frameOptions().disable();

        return http.build();
    }

}
