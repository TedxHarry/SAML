package com.acme.training.acmehr.security;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import net.shibboleth.shared.xml.SerializeSupport;
import org.junit.jupiter.api.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.encryption.Encrypter;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.encryption.support.DataEncryptionParameters;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.opensaml.xmlsec.encryption.support.EncryptionException;
import org.opensaml.xmlsec.encryption.support.KeyEncryptionParameters;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureSupport;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamlEncryptedAssertionTests {

    private static final String REGISTRATION_ID = "acmehr";
    private static final String IDP_ENTITY_ID = "https://idp.acme.test";
    private static final String IDP_SSO_URL = "https://idp.acme.test/sso";
    private static final String SP_ENTITY_ID = "urn:acme:training:sp";
    private static final String ACS_URL = "http://localhost:8000/saml/acs";
    private static final String AUTHN_REQUEST_ID = "_acmehr-request-day9";
    private static final String PRINCIPAL = "learner@acme.test";
    private static final Duration TEST_CLOCK_SKEW = Duration.ofSeconds(30);

    /*
     * Public disposable training fixtures only.
     * These keys are committed only for reproducible repository tests.
     * They provide no secrecy and must never be reused in a real environment.
     */
    private static final String IDP_SIGNING_CERTIFICATE_PEM = """
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

    private static final String IDP_SIGNING_PRIVATE_KEY_PEM = """
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

    private static final String SP_ENCRYPTION_CERTIFICATE_PEM = """
-----BEGIN CERTIFICATE-----
MIIDOzCCAiOgAwIBAgIUeBa6mcCpvDEwxh4QvOmxJXKk76IwDQYJKoZIhvcNAQEL
BQAwTTEmMCQGA1UEAwwdQWNtZSBEYXkgOSBTUCBFbmNyeXB0aW9uIFRlc3QxFjAU
BgNVBAoMDUFjbWUgVHJhaW5pbmcxCzAJBgNVBAYTAlVTMB4XDTI2MDkyNDAwMjYz
MloXDTM2MDkyMjAwMjYzMlowTTEmMCQGA1UEAwwdQWNtZSBEYXkgOSBTUCBFbmNy
eXB0aW9uIFRlc3QxFjAUBgNVBAoMDUFjbWUgVHJhaW5pbmcxCzAJBgNVBAYTAlVT
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkQU8YYlP8dfwnbBwB/pm
u3fYnHL2xl8cTaWVGfWE2DmjgU3gFNqvbOVdVArEM/Tr59boKyqYMS5OjbZy6BqR
Q129rpPVytwRYUW+GnrC4mPPx7ExaThQ9jy+mpnhLjNRub66akvxF4MZY8VcGsYO
UuJxgM6u6Ezikr/cywdVnmkIvcbUHvdi86zTRq2tlGkftq0WmpC9nBDx+CXax1zS
DYtlDh62M2UEqyg5OlLzT+nm6jhGX047iYmBfckAZqpvPrlFQGwLTAusQ9HmkduB
nKojErP3oBcjEsqaIQddmdy2LCgmRNPr3o222syTArNuu4DDd2rVi/Tk7dQS3zqR
GQIDAQABoxMwETAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQAX
pI7ua6AtYFWEKXqEoKKaDQ2cxuZA3RkspPDunbHop8xS9xBZeBxQQTss+ncU7Tcb
7gR9IHreKi2oISFiWuBQxvj/EFKY4MkqTFX924ewFAk1p6ljtpFzJ41ietTiLtST
lFvjteLIPGr6bTcO2tH6/7bmle+Sn5oXI+4GLJtx6exZb8y/mzmeczDTC5XsoGnq
7gf97izHFh41hUQPW09iu1eHvk6PR4yWVSCBCM2Tgw6+imtFES9wKA7A7nufjTge
y0kKjqCckoBf/0CKL5gGanGgPATqzbtcBWKJk+LNRQWJPCcUCrvFQu3JNpadtcHw
PUSF9xOYOdHnVz43KEio
-----END CERTIFICATE-----
            """;

    private static final String SP_DECRYPTION_PRIVATE_KEY_PEM = """
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCRBTxhiU/x1/Cd
sHAH+ma7d9iccvbGXxxNpZUZ9YTYOaOBTeAU2q9s5V1UCsQz9Ovn1ugrKpgxLk6N
tnLoGpFDXb2uk9XK3BFhRb4aesLiY8/HsTFpOFD2PL6ameEuM1G5vrpqS/EXgxlj
xVwaxg5S4nGAzq7oTOKSv9zLB1WeaQi9xtQe92LzrNNGra2UaR+2rRaakL2cEPH4
JdrHXNINi2UOHrYzZQSrKDk6UvNP6ebqOEZfTjuJiYF9yQBmqm8+uUVAbAtMC6xD
0eaR24GcqiMSs/egFyMSypohB12Z3LYsKCZE0+vejbbazJMCs267gMN3atWL9OTt
1BLfOpEZAgMBAAECggEAElKTR66B5Yg3PDEt4nAqeS6h9VhYIgWjzZKpZTt+QpR7
uktUz8W2it9q3IK0uezMvoDKWiPaUUJsxd5io70xk82c+NjwVt5ZqszVQUK360MZ
nj0exVDKd+YCXgWpq/zrazi0FFiOeD5F37NwmOhjqGi4VwImGq8WJFzt4FAHqwkh
E3xPLsMjnDPC4NrP26vmyTCp9JnB7tzZjv3Cc8wPGyeVxah3/mOQ0vjR2yVru9LN
cDZtqFEkF3AD4sthL9CK1JgB4o9klBHiuUh8wAb/lAgp7q64+iDEQVhkOhh6hw58
yvBvIxjg6nttA10uR1u6wAr2xqdj/DpwbwaDenNFhQKBgQDBTWmTeaBa9e1+cCXt
aoqN1bM/tiXQVab37sCYTPbtt0XO02pqOVy6IhoAFRbMDZ9Ai0qas9VKkYdxgDHI
s4PiVslBoSov/lvVU8TseGUQ2I41/WWfLbmQ5dxWp5W9takYEZ8wBxDC3FKK4YpB
BYVB+SOCfrdz22Gv8JWTSPaOpQKBgQDADs06fb2p+E9bAJozk7aYdhcDRehdEyEl
hMCFP81lqedqhUymmUC3OQPPPZLRN5/HgW2X6/itbaTKoGgT2nRTQdVlM5yANzAs
qkez4QAqGr0U7qO9qmsJ96v3unC7XiUzEkveHgqEHhWfkCqYRwZ1a8xGPzWZddHY
8V+oHMsCZQKBgQCZpiveUpY41zSRBRnQrpg1h4D0ipdiE0Ml3jxukvbQG5a8gOYc
Sz4+oa6jFCBL0CQezNlfQMSwgh70BB8OVkrllXBh33ZGMdvTJMPp5wUGr+vSVHn7
XgAbgepzYsAuzH/9JryvE16qVTCzPfWEG5xBxYQJTSoLSH1MuWJbcWqyFQKBgB6D
O3/LclKKrLDci6MeGKhoOoe+l4gR/fYj/SIuXMeGEsoNm3vsZSe22zAVrDmpCK19
lpfIWZ3UCZZXnbFpx4lisDB/3qFQI0s1Umb1nD0UOm4U69a6OH9A5gVZjYd3EZnn
kj4br7gFv5G2Kc4/mnsVQUL3z+Ato8U/s97VlDsxAoGAVlRuZ0jMGS5DzrpyekNT
3e0n7/TtIhxTM+EHBTNNJvxnR7udhQgQMoi4RxcO57+O7vQBGc1NlOyGfDe4VPoB
RpiU2YqrJlZa1O8PLAre3dZm6+gQ3F1PP8TnZOds2ejDJGaK8BVAG5vtckxgZaGB
GOXoVhEkrrvaJ2keewOJcn0=
-----END PRIVATE KEY-----
            """;

    private static final SecretKey CONTENT_ENCRYPTION_KEY = new SecretKeySpec(
            Base64.getDecoder().decode(
                    "shOnwNMoCv88HKMEa91+FlYoD5RNvzMTAL5LGxZKIFk="),
            "AES");

    private static final X509Certificate IDP_SIGNING_CERTIFICATE;
    private static final PrivateKey IDP_SIGNING_PRIVATE_KEY;
    private static final X509Certificate SP_ENCRYPTION_CERTIFICATE;
    private static final PrivateKey SP_DECRYPTION_PRIVATE_KEY;
    private static final PrivateKey WRONG_DECRYPTION_PRIVATE_KEY;

    static {
        OpenSamlInitializationService.initialize();
        IDP_SIGNING_CERTIFICATE = readCertificate(IDP_SIGNING_CERTIFICATE_PEM);
        IDP_SIGNING_PRIVATE_KEY = readPrivateKey(IDP_SIGNING_PRIVATE_KEY_PEM);
        SP_ENCRYPTION_CERTIFICATE = readCertificate(SP_ENCRYPTION_CERTIFICATE_PEM);
        SP_DECRYPTION_PRIVATE_KEY = readPrivateKey(SP_DECRYPTION_PRIVATE_KEY_PEM);
        WRONG_DECRYPTION_PRIVATE_KEY = generateWrongPrivateKey();
    }

    @Test
    void encryptedAssertionDecryptsWithMatchingAcmeHrPrivateKey() {
        String samlResponse = encryptedSignedResponse();

        Authentication authentication =
                authenticate(samlResponse, SP_DECRYPTION_PRIVATE_KEY);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(PRINCIPAL);
    }

    @Test
    void encryptedAssertionIsRejectedWithDifferentPrivateKey() {
        String samlResponse = encryptedSignedResponse();

        Authentication baseline =
                authenticate(samlResponse, SP_DECRYPTION_PRIVATE_KEY);
        assertThat(baseline.isAuthenticated()).isTrue();

        Saml2AuthenticationException error = assertThrows(
                Saml2AuthenticationException.class,
                () -> authenticate(samlResponse, WRONG_DECRYPTION_PRIVATE_KEY));

        assertThat(error.getSaml2Error().getErrorCode())
                .isEqualTo(Saml2ErrorCodes.DECRYPTION_ERROR);
    }

    private static String encryptedSignedResponse() {
        Response response = validResponse();

        Assertion assertion = validAssertion(response.getIssueInstant());
        EncryptedAssertion encryptedAssertion = encrypt(assertion);
        response.getEncryptedAssertions().add(encryptedAssertion);

        sign(response);

        String xml = serialize(response);

        assertThat(xml).contains("EncryptedAssertion");
        assertThat(xml).doesNotContain("<saml2:Assertion");

        return xml;
    }

    private static Authentication authenticate(
            String samlResponse,
            PrivateKey decryptionPrivateKey) {

        OpenSaml5AuthenticationProvider provider =
                new OpenSaml5AuthenticationProvider();
        provider.setAssertionValidator(
                OpenSaml5AuthenticationProvider.AssertionValidator.builder()
                        .clockSkew(TEST_CLOCK_SKEW)
                        .build());

        return Objects.requireNonNull(
                provider.authenticate(authenticationToken(
                        samlResponse,
                        decryptionPrivateKey)));
    }

    private static Saml2AuthenticationToken authenticationToken(
            String samlResponse,
            PrivateKey decryptionPrivateKey) {

        AbstractSaml2AuthenticationRequest request =
                mock(AbstractSaml2AuthenticationRequest.class);
        when(request.getId()).thenReturn(AUTHN_REQUEST_ID);

        return new Saml2AuthenticationToken(
                relyingPartyRegistration(decryptionPrivateKey),
                samlResponse,
                request);
    }

    private static RelyingPartyRegistration relyingPartyRegistration(
            PrivateKey decryptionPrivateKey) {

        Saml2X509Credential verification =
                Saml2X509Credential.verification(IDP_SIGNING_CERTIFICATE);

        Saml2X509Credential decryption =
                Saml2X509Credential.decryption(
                        decryptionPrivateKey,
                        SP_ENCRYPTION_CERTIFICATE);

        return RelyingPartyRegistration.withRegistrationId(REGISTRATION_ID)
                .entityId(SP_ENTITY_ID)
                .assertionConsumerServiceLocation(ACS_URL)
                .decryptionX509Credentials(
                        credentials -> credentials.add(decryption))
                .assertingPartyMetadata(party -> party
                        .entityId(IDP_ENTITY_ID)
                        .singleSignOnServiceLocation(IDP_SSO_URL)
                        .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT)
                        .wantAuthnRequestsSigned(false)
                        .verificationX509Credentials(
                                credentials -> credentials.add(verification)))
                .build();
    }

    private static Response validResponse() {
        Instant now = Instant.now();

        Response response = build(Response.DEFAULT_ELEMENT_NAME);
        response.setID("_response-" + UUID.randomUUID());
        response.setVersion(SAMLVersion.VERSION_20);
        response.setIssueInstant(now);
        response.setDestination(ACS_URL);
        response.setInResponseTo(AUTHN_REQUEST_ID);
        response.setIssuer(issuer(IDP_ENTITY_ID));
        response.setStatus(successStatus());

        return response;
    }

    private static Assertion validAssertion(Instant now) {
        Assertion assertion = build(Assertion.DEFAULT_ELEMENT_NAME);
        assertion.setID("_assertion-" + UUID.randomUUID());
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(now);
        assertion.setIssuer(issuer(IDP_ENTITY_ID));
        assertion.setSubject(subject(now));
        assertion.setConditions(conditions(now));

        AuthnStatement authnStatement = build(AuthnStatement.DEFAULT_ELEMENT_NAME);
        authnStatement.setAuthnInstant(now);
        authnStatement.setSessionIndex("_session-day9");
        assertion.getAuthnStatements().add(authnStatement);

        return assertion;
    }

    private static Subject subject(Instant now) {
        NameID nameId = build(NameID.DEFAULT_ELEMENT_NAME);
        nameId.setValue(PRINCIPAL);

        SubjectConfirmationData data =
                build(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        data.setRecipient(ACS_URL);
        data.setInResponseTo(AUTHN_REQUEST_ID);
        data.setNotOnOrAfter(now.plus(Duration.ofMinutes(2)));

        SubjectConfirmation confirmation =
                build(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        confirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        confirmation.setSubjectConfirmationData(data);

        Subject subject = build(Subject.DEFAULT_ELEMENT_NAME);
        subject.setNameID(nameId);
        subject.getSubjectConfirmations().add(confirmation);

        return subject;
    }

    private static Conditions conditions(Instant now) {
        Audience audience = build(Audience.DEFAULT_ELEMENT_NAME);
        audience.setURI(SP_ENTITY_ID);

        AudienceRestriction restriction =
                build(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        restriction.getAudiences().add(audience);

        Conditions conditions = build(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(now.minus(Duration.ofMinutes(2)));
        conditions.setNotOnOrAfter(now.plus(Duration.ofMinutes(2)));
        conditions.getAudienceRestrictions().add(restriction);

        return conditions;
    }

    private static Issuer issuer(String entityId) {
        Issuer issuer = build(Issuer.DEFAULT_ELEMENT_NAME);
        issuer.setValue(entityId);
        return issuer;
    }

    private static Status successStatus() {
        StatusCode statusCode = build(StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(StatusCode.SUCCESS);

        Status status = build(Status.DEFAULT_ELEMENT_NAME);
        status.setStatusCode(statusCode);
        return status;
    }

    private static EncryptedAssertion encrypt(Assertion assertion) {
        DataEncryptionParameters data = new DataEncryptionParameters();
        data.setEncryptionCredential(new BasicCredential(CONTENT_ENCRYPTION_KEY));
        data.setAlgorithm(
                EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256);

        Credential publicCredential =
                CredentialSupport.getSimpleCredential(
                        SP_ENCRYPTION_CERTIFICATE,
                        null);

        KeyEncryptionParameters key = new KeyEncryptionParameters();
        key.setEncryptionCredential(publicCredential);
        key.setAlgorithm(
                EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP);

        Encrypter encrypter = new Encrypter(data, key);
        encrypter.setKeyPlacement(Encrypter.KeyPlacement.PEER);

        try {
            return encrypter.encrypt(assertion);
        }
        catch (EncryptionException ex) {
            throw new IllegalStateException(
                    "Could not encrypt the synthetic SAML Assertion",
                    ex);
        }
    }

    private static void sign(Response response) {
        BasicCredential credential =
                CredentialSupport.getSimpleCredential(
                        IDP_SIGNING_CERTIFICATE,
                        IDP_SIGNING_PRIVATE_KEY);
        credential.setEntityId(IDP_ENTITY_ID);
        credential.setUsageType(UsageType.SIGNING);

        SignatureSigningParameters parameters =
                new SignatureSigningParameters();
        parameters.setSigningCredential(credential);
        parameters.setSignatureAlgorithm(
                SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        parameters.setSignatureReferenceDigestMethod(
                SignatureConstants.ALGO_ID_DIGEST_SHA256);
        parameters.setSignatureCanonicalizationAlgorithm(
                SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        try {
            SignatureSupport.signObject(response, parameters);
        }
        catch (MarshallingException | SignatureException | SecurityException ex) {
            throw new IllegalStateException(
                    "Could not sign the synthetic SAML Response",
                    ex);
        }
    }

    private static String serialize(XMLObject object) {
        try {
            return SerializeSupport.nodeToString(
                    XMLObjectSupport.marshall(object));
        }
        catch (MarshallingException ex) {
            throw new IllegalStateException(
                    "Could not serialize the synthetic SAML Response",
                    ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends XMLObject> T build(QName elementName) {
        return (T) Objects.requireNonNull(
                        XMLObjectProviderRegistrySupport.getBuilderFactory()
                                .getBuilder(elementName))
                .buildObject(elementName);
    }

    private static X509Certificate readCertificate(String pem) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(
                    removePemEnvelope(
                            pem,
                            "-----BEGIN CERTIFICATE-----",
                            "-----END CERTIFICATE-----"));

            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Could not read the test certificate",
                    ex);
        }
    }

    private static PrivateKey readPrivateKey(String pem) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(
                    removePemEnvelope(
                            pem,
                            "-----BEGIN PRIVATE KEY-----",
                            "-----END PRIVATE KEY-----"));

            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Could not read the test private key",
                    ex);
        }
    }

    private static PrivateKey generateWrongPrivateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair().getPrivate();
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "Could not generate the wrong-key test fixture",
                    ex);
        }
    }

    private static String removePemEnvelope(
            String pem,
            String begin,
            String end) {

        return pem.replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s", "");
    }
}
