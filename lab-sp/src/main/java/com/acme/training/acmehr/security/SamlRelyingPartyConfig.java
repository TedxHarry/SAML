package com.acme.training.acmehr.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.saml2.core.Saml2X509Credential;
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

    static final String SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY =
            "acmehr.saml.sp-signing-private-key-location";
    static final String SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY =
            "acmehr.saml.sp-signing-certificate-location";
    static final String SP_DECRYPTION_PRIVATE_KEY_LOCATION_PROPERTY =
            "acmehr.saml.sp-decryption-private-key-location";
    static final String SP_DECRYPTION_CERTIFICATE_LOCATION_PROPERTY =
            "acmehr.saml.sp-decryption-certificate-location";
    static final String NAME_ID_FORMAT_PROPERTY =
            "acmehr.saml.name-id-format";

    @Bean
    RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(
            @Value("${acmehr.saml.idp-metadata-url}") String idpMetadataUrl,
            Environment environment,
            ResourceLoader resourceLoader) {

        String signingPrivateKeyLocation =
                environment.getProperty(SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY);
        String signingCertificateLocation =
                environment.getProperty(SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY);
        String decryptionPrivateKeyLocation =
                environment.getProperty(SP_DECRYPTION_PRIVATE_KEY_LOCATION_PROPERTY);
        String decryptionCertificateLocation =
                environment.getProperty(SP_DECRYPTION_CERTIFICATE_LOCATION_PROPERTY);
        String nameIdFormat =
                environment.getProperty(NAME_ID_FORMAT_PROPERTY);

        Saml2X509Credential signingCredential = optionalCredential(
                signingPrivateKeyLocation,
                signingCertificateLocation,
                SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY,
                SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY,
                resourceLoader,
                CredentialPurpose.SIGNING);

        Saml2X509Credential decryptionCredential = optionalCredential(
                decryptionPrivateKeyLocation,
                decryptionCertificateLocation,
                SP_DECRYPTION_PRIVATE_KEY_LOCATION_PROPERTY,
                SP_DECRYPTION_CERTIFICATE_LOCATION_PROPERTY,
                resourceLoader,
                CredentialPurpose.DECRYPTION);

        RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations
                .fromMetadataLocation(idpMetadataUrl)
                .registrationId(REGISTRATION_ID)
                .entityId(SP_ENTITY_ID)
                .assertionConsumerServiceLocation(ACS_LOCATION)
                .assertingPartyMetadata(party -> party
                        .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT));

        if (signingCredential != null) {
            if (!StringUtils.hasText(nameIdFormat)) {
                throw new IllegalStateException(
                        NAME_ID_FORMAT_PROPERTY
                                + " is required when the SP signing credential is configured.");
            }

            builder.signingX509Credentials(credentials -> credentials.add(signingCredential))
                    .authnRequestsSigned(true)
                    .nameIdFormat(nameIdFormat);
        }

        if (decryptionCredential != null) {
            builder.decryptionX509Credentials(credentials -> credentials.add(decryptionCredential));
        }

        RelyingPartyRegistration registration = builder.build();

        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    private static Saml2X509Credential optionalCredential(
            String privateKeyLocation,
            String certificateLocation,
            String privateKeyProperty,
            String certificateProperty,
            ResourceLoader resourceLoader,
            CredentialPurpose purpose) {

        boolean privateKeyConfigured = StringUtils.hasText(privateKeyLocation);
        boolean certificateConfigured = StringUtils.hasText(certificateLocation);

        if (!privateKeyConfigured && !certificateConfigured) {
            return null;
        }

        if (privateKeyConfigured != certificateConfigured) {
            throw new IllegalStateException(
                    privateKeyProperty + " and " + certificateProperty
                            + " must either both be configured or both be omitted.");
        }

        PrivateKey privateKey = readPrivateKey(resourceLoader.getResource(privateKeyLocation));
        X509Certificate certificate =
                readCertificate(resourceLoader.getResource(certificateLocation));

        return switch (purpose) {
            case SIGNING -> Saml2X509Credential.signing(privateKey, certificate);
            case DECRYPTION -> Saml2X509Credential.decryption(privateKey, certificate);
        };
    }

    private static PrivateKey readPrivateKey(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            return Objects.requireNonNull(
                    RsaKeyConverters.pkcs8().convert(input),
                    "Private key converter returned no key for " + resource.getDescription());
        }
        catch (IOException | RuntimeException ex) {
            throw new IllegalStateException(
                    "Could not read PKCS#8 RSA private key from " + resource.getDescription(),
                    ex);
        }
    }

    private static X509Certificate readCertificate(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(input);
        }
        catch (IOException | GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Could not read X.509 certificate from " + resource.getDescription(),
                    ex);
        }
    }

    private enum CredentialPurpose {
        SIGNING,
        DECRYPTION
    }

    static class IdpMetadataConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String metadataUrl = context.getEnvironment().getProperty(IDP_METADATA_PROPERTY);
            return StringUtils.hasText(metadataUrl);
        }
    }
}
