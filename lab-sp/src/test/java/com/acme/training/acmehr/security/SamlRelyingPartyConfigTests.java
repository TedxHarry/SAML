package com.acme.training.acmehr.security;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
class SamlRelyingPartyConfigTests {

    private static final String NAME_ID_FORMAT =
            "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";

    /*
     * Public disposable test fixture copied from SamlSignatureValidationTests.
     * It provides no secrecy and must never be reused outside repository tests.
     */
    private static final String TEST_CERTIFICATE_PEM = """
-----BEGIN CERTIFICATE-----
            MIIDZzCCAk+gAwIBAgIUbQqQF1qqhxKrxOQw4hUiEUNMnd0wDQYJKoZIhvcNAQEL
            BQAwQzEcMBoGA1UEAwwTQWNtZSBEYXkgNiBUZXN0IElkUDEWMBQGA1UECgwNQWNt
            ZSBUcmFpbmluZzELMAkGA1UEBhMCVVMwHhcNMjYwOTIyMTAyMzMxWhcNMzYwOTE5
            MTAyMzMxWjBDMRwwGgYDVQQDDBNBY21lIERheSA2IFRlc3QgSWRQMRYwFAYDVQQK
            DA1BY21lIFRyYWluaW5nMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcNAQEBBQAD
            ggEPADCCAQoCggEBAJ3VGrA5x8FKECQbDD4Bu2bCvJJObjklyfEpPy5XbcAs4Cqv
            Eiixzh4qoA9M0tLePCqlsgmDUK75bCJZAzdso93tshFmyubHSlehOOcyLYem/eoi
            xZdqAwkPzZOYwSMwbfGcNlvKO+b+GZoQrvuMAiMCnyYgXd+hTVHk3DWquAju8/gV
            qP68ChGcXDh/KLgP2+YP/B/RwvLhYv8Y6Q3HaPVxmTv7Q9My+hdw8ply16tMrqDd
            OIJ4UWaLAl0gT0D9EBc15lrf1CfKXgxLVnkcjNOgsBxLzo5qkj2W0V464aX3ymSk
            X29bLAWpFqmRXQlNPO/FD57gXSBXmKZfneXqjecCAwEAAaNTMFEwHQYDVR0OBBYE
            FN6aQ+9sNpcNNSNrh34V2gGhdHqrMB8GA1UdIwQYMBaAFN6aQ+9sNpcNNSNrh34V
            2gGhdHqrMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAIhOAomY
            OrO6aHi8L/z5gcJLu/bxNccFuSmahKDobsMile45agSBSl18m4nZQrcyee8crTDb
            x4IwsutCwyw/RM6gozbx9Hcw18hgXRWss6INB/5Or/5c2HGYNgpL+uuxu4dQmiEY
            kFC6u5QI0bqNCz04XEwz/8z4UINN78jN97H8vSaQe4vnlxOiiHheoZyOUQwv4TFH
            +5VJFHTs++Eox9Y5JDJaIYUTVm0Aq/PNyjRPbQPaKN50GsZ1hdkUV2MouFofi2G5
            advQwyp9EDz/HAmhAaQOXFCcCjqpe/HrOgtsFUqq6mqk3PfPqX0Jl9sFebo7eU2n
            Hmx25ipK0uAO9tM=
            -----END CERTIFICATE-----
            """;

    private static final String TEST_PRIVATE_KEY_PEM = """
-----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCd1RqwOcfBShAk
            Gww+AbtmwrySTm45JcnxKT8uV23ALOAqrxIosc4eKqAPTNLS3jwqpbIJg1Cu+Wwi
            WQM3bKPd7bIRZsrmx0pXoTjnMi2Hpv3qIsWXagMJD82TmMEjMG3xnDZbyjvm/hma
            EK77jAIjAp8mIF3foU1R5Nw1qrgI7vP4Faj+vAoRnFw4fyi4D9vmD/wf0cLy4WL/
            GOkNx2j1cZk7+0PTMvoXcPKZcterTK6g3TiCeFFmiwJdIE9A/RAXNeZa39Qnyl4M
            S1Z5HIzToLAcS86OapI9ltFeOuGl98pkpF9vWywFqRapkV0JTTzvxQ+e4F0gV5im
            X53l6o3nAgMBAAECggEAS65Astmx6AQcg0OY9i6cbqTYCQuknLB7CbFug0kW7jxW
            bQEvouXHYP2tbEi5GrXHgeeb3CXkTVJ6QGoQOcZVOPheFywEBO7wvd4ny+xqmo4/
            WMK9nmIN/I1gVPK9QaNaRK1T/2WUnamgGxj+3s1+xMzgBUcl3DKbQbaMxQsMXfN9
            jc4zii6BM0xYcK4zKPdkIQAPBcPlm+usEvlw39f5IDLXBpUGL5Rf8cpxwsOLq00R
            xWjFb+5a1Su1ugoziDJqNGNG+r0FuyoJ+udt9cyfY5F+2ZwrE0WquwbxWFXNjPTF
            BoaAv7sDxgtxliV1hqHB4UNUMcNM2Eb6KxUlbRdKGQKBgQDQwa3TVYBsXHTRmP0M
            TAzXK7UhXoSZS7SHgjea8d1dKh6Sv+c4i8fcELc2Jqnb2qEv9eVaTPF/6LCV6yFW
            Zs+xX8fBb5+zYUdZ+9R38NXmN5hx3BEtLXLd7qwyK16MkHqFySbTLYe/DD84N1s1
            Kr1i28tubjZ7t5eT62RbpyspQwKBgQDBjSJLVy6iwHk2Cfv/m1iN/9mi7et34EVa
            hGc1WZE58Sw0vIQIw9Vh7uLUgtbQC73OqVF4po0b+D1xlnOdjXJoRAQ8/+CK8ALl
            hiTDVGc6BlwVEfDuwfXjPfdu30DPjOmjspsE7XSryEoC7dDyoc3NCRcAc/9fL0Ee
            YgMmAgCcjQKBgGte8rT8CS2y8DLN6XlltEUHqgYbwz/FfHkmNMtxE1ZTz53TLm4b
            FxTNVC55/GukK7urUef8I0qSuCCj62WxQ6oLhYasjwuIQVa6/DEkoh/jAHmvovYF
            pksX82FqhRrvRNWC/IEpreRJvEqBzluuO/KY8i0+aq9/Ymsma1vow35ZAoGBAJXG
            /KHmvl0Nqv7pbQvZEAca1TUi/hOPBrxMN33uaNa4zeeldls+CHM3pGqlMUxfuasi
            FbzSzeG2EP5EWgWy/rS25by6me2KXAN38h0BxLv/TeS0NIjeqcQHIOG4e/Pg7LBT
            t2hxxNZmMPfhRs9r7NFc1mLwYM8sxyyW1i7kX8rFAoGASv7nyThtlE6ZbnkCjVCV
            At3KwOB8GWxYD53gyRbabjyzYSnBtuJJf3/aBpP7Uixk0QejXp6XHWpHfsV4qZEo
            agFyfi7reDhSWyf8ibfAglXTx5Yl6F4OeZy7cLvrsU+86faymRA/UkslmJJre1dq
            tzXVwUlkOlXLYPvHAIZcB0g=
            -----END PRIVATE KEY-----
            """;

    @Autowired
    private RelyingPartyRegistrationRepository relyingParties;

    @Test
    void loadsAcmeHrRelyingPartyFromTestMetadataWithoutDay9Credentials() {
        RelyingPartyRegistration registration = relyingParties.findByRegistrationId("acmehr");

        assertThat(registration).isNotNull();
        assertThat(registration.getRegistrationId()).isEqualTo("acmehr");
        assertThat(registration.getEntityId()).isEqualTo("urn:acme:training:sp");
        assertThat(registration.getAssertionConsumerServiceLocation())
                .isEqualTo("{baseUrl}/saml/acs");

        assertThat(registration.getSigningX509Credentials()).isEmpty();
        assertThat(registration.getDecryptionX509Credentials()).isEmpty();
        assertThat(registration.isAuthnRequestsSigned()).isFalse();
        assertThat(registration.getNameIdFormat()).isNull();

        var assertingParty = registration.getAssertingPartyMetadata();

        assertThat(assertingParty.getEntityId()).isEqualTo("https://idp.acme.test");
        assertThat(assertingParty.getSingleSignOnServiceLocation())
                .isEqualTo("https://idp.acme.test/sso");
        assertThat(assertingParty.getVerificationX509Credentials())
                .hasSize(1);
    }

    @Test
    void loadsDay9SigningAndDecryptionCredentials(@TempDir Path tempDir) throws Exception {
        TestKeyFiles files = writeTestKeyFiles(tempDir);
        MockEnvironment environment = day9Environment(files);

        RelyingPartyRegistration registration =
                registration(environment);

        assertThat(registration.getSigningX509Credentials()).hasSize(1);
        assertThat(registration.getDecryptionX509Credentials()).hasSize(1);
        assertThat(registration.isAuthnRequestsSigned()).isTrue();
        assertThat(registration.getNameIdFormat()).isEqualTo(NAME_ID_FORMAT);

        assertThat(registration.getSigningX509Credentials().iterator().next().getCertificate())
                .isEqualTo(registration.getDecryptionX509Credentials().iterator().next().getCertificate());
    }

    @Test
    void requiresNameIdFormatWhenSigningCredentialIsConfigured(@TempDir Path tempDir)
            throws Exception {

        TestKeyFiles files = writeTestKeyFiles(tempDir);
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        SamlRelyingPartyConfig.SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY,
                        files.privateKeyLocation())
                .withProperty(
                        SamlRelyingPartyConfig.SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY,
                        files.certificateLocation());

        assertThatThrownBy(() -> registration(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(SamlRelyingPartyConfig.NAME_ID_FORMAT_PROPERTY)
                .hasMessageContaining("required when the SP signing credential is configured");
    }

    @Test
    void rejectsIncompleteSigningCredentialPair(@TempDir Path tempDir) throws Exception {
        TestKeyFiles files = writeTestKeyFiles(tempDir);
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        SamlRelyingPartyConfig.SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY,
                        files.privateKeyLocation())
                .withProperty(
                        SamlRelyingPartyConfig.NAME_ID_FORMAT_PROPERTY,
                        NAME_ID_FORMAT);

        assertThatThrownBy(() -> registration(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        SamlRelyingPartyConfig.SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY)
                .hasMessageContaining(
                        SamlRelyingPartyConfig.SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY)
                .hasMessageContaining("must either both be configured or both be omitted");
    }

    private static RelyingPartyRegistration registration(MockEnvironment environment) {
        SamlRelyingPartyConfig config = new SamlRelyingPartyConfig();

        RelyingPartyRegistrationRepository repository =
                config.relyingPartyRegistrationRepository(
                        "classpath:idp-metadata.xml",
                        environment,
                        new DefaultResourceLoader());

        return repository.findByRegistrationId("acmehr");
    }

    private static MockEnvironment day9Environment(TestKeyFiles files) {
        return new MockEnvironment()
                .withProperty(
                        SamlRelyingPartyConfig.SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY,
                        files.privateKeyLocation())
                .withProperty(
                        SamlRelyingPartyConfig.SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY,
                        files.certificateLocation())
                .withProperty(
                        SamlRelyingPartyConfig.SP_DECRYPTION_PRIVATE_KEY_LOCATION_PROPERTY,
                        files.privateKeyLocation())
                .withProperty(
                        SamlRelyingPartyConfig.SP_DECRYPTION_CERTIFICATE_LOCATION_PROPERTY,
                        files.certificateLocation())
                .withProperty(
                        SamlRelyingPartyConfig.NAME_ID_FORMAT_PROPERTY,
                        NAME_ID_FORMAT);
    }

    private static TestKeyFiles writeTestKeyFiles(Path tempDir) throws Exception {
        Path privateKey = tempDir.resolve("day9-test-private-key.pem");
        Path certificate = tempDir.resolve("day9-test-certificate.pem");

        Files.writeString(privateKey, normalizePem(TEST_PRIVATE_KEY_PEM));
        Files.writeString(certificate, normalizePem(TEST_CERTIFICATE_PEM));

        return new TestKeyFiles(
                privateKey.toUri().toString(),
                certificate.toUri().toString());
    }

    private static String normalizePem(String pem) {
        return String.join(
                "\n",
                pem.lines()
                        .map(String::strip)
                        .toList()) + "\n";
    }

    private record TestKeyFiles(
            String privateKeyLocation,
            String certificateLocation) {
    }
}
