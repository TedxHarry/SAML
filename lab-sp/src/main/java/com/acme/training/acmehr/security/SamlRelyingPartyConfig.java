package com.acme.training.acmehr.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.StringUtils;

@Configuration
@Conditional(SamlRelyingPartyConfig.IdpMetadataConfiguredCondition.class)
public class SamlRelyingPartyConfig {

    static final String REGISTRATION_ID = "acmehr";
    static final String SP_ENTITY_ID = "urn:acme:training:sp";
    static final String ACS_LOCATION = "{baseUrl}/saml/acs";
    static final String IDP_METADATA_PROPERTY = "acmehr.saml.idp-metadata-url";

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(
            @Value("${acmehr.saml.idp-metadata-url}") String idpMetadataUrl) {

        RelyingPartyRegistration registration = RelyingPartyRegistrations
                .fromMetadataLocation(idpMetadataUrl)
                .registrationId(REGISTRATION_ID)
                .entityId(SP_ENTITY_ID)
                .assertionConsumerServiceLocation(ACS_LOCATION)
                .assertingPartyMetadata(party -> party
                        .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT))
                .build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    static class IdpMetadataConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String metadataUrl = context.getEnvironment().getProperty(IDP_METADATA_PROPERTY);
            return StringUtils.hasText(metadataUrl);
        }
    }
}
