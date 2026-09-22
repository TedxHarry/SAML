package com.acme.training.acmehr.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
class SamlRelyingPartyConfigTests {

    @Autowired
    private RelyingPartyRegistrationRepository relyingParties;

    @Test
    void loadsAcmeHrRelyingPartyFromTestMetadata() {
        RelyingPartyRegistration registration = relyingParties.findByRegistrationId("acmehr");

        assertThat(registration).isNotNull();
        assertThat(registration.getRegistrationId()).isEqualTo("acmehr");
        assertThat(registration.getEntityId()).isEqualTo("urn:acme:training:sp");
        assertThat(registration.getAssertionConsumerServiceLocation())
                .isEqualTo("{baseUrl}/saml/acs");

        var assertingParty = registration.getAssertingPartyMetadata();

        assertThat(assertingParty.getEntityId()).isEqualTo("https://idp.acme.test");
        assertThat(assertingParty.getSingleSignOnServiceLocation())
                .isEqualTo("https://idp.acme.test/sso");
        assertThat(assertingParty.getVerificationX509Credentials())
                .hasSize(1);
    }
}
