package com.acme.training.acmehr.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

@Configuration
@ConditionalOnProperty(name = "acmehr.saml.idp-metadata-url")
public class SamlRelyingPartyConfig {

    static final String REGISTRATION_ID = "acmehr";
    static final String SP_ENTITY_ID = "urn:acme:training:sp";
    static final String ACS_LOCATION = "{baseUrl}/login/saml2/sso/{registrationId}";

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(
            @Value("${acmehr.saml.idp-metadata-url}") String idpMetadataUrl) {

        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(idpMetadataUrl)
                .registrationId(REGISTRATION_ID)
                .entityId(SP_ENTITY_ID)
                .assertionConsumerServiceLocation(ACS_LOCATION)
                .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }
}
