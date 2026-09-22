package com.acme.training.acmehr.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<RelyingPartyRegistrationRepository> relyingParties) throws Exception {

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/actuator/health", "/error").permitAll()
                .requestMatchers("/protected", "/claims", "/manager").authenticated()
                .anyRequest().permitAll());

        if (relyingParties.getIfAvailable() != null) {
            http.saml2Login(saml2 -> saml2.loginProcessingUrl("/saml/acs"));
            http.saml2Metadata(Customizer.withDefaults());
        }

        return http.build();
    }
}
