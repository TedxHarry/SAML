package com.acme.training.acmehr.security;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import javax.xml.namespace.QName;

import net.shibboleth.shared.xml.SerializeSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureSupport;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamlSignatureValidationTests {

    private static final String REGISTRATION_ID = "acmehr";
    private static final String IDP_ENTITY_ID = "https://idp.acme.test";
    private static final String IDP_SSO_URL = "https://idp.acme.test/sso";
    private static final String SP_ENTITY_ID = "urn:acme:training:sp";
    private static final String ACS_URL = "http://localhost:8000/saml/acs";
    private static final String AUTHN_REQUEST_ID = "_acmehr-request-day8";
    private static final String PRINCIPAL = "learner@acme.test";
    private static final Duration TEST_CLOCK_SKEW = Duration.ofSeconds(30);

    /*
     * Public training fixture only.
     *
     * The certificate and private key below are committed deliberately so the
     * Day 8 signature tests are reproducible. They provide no secrecy and must
     * never be reused for a real application, tenant, environment, or certificate.
     */
    private static final String SIGNING_CERTIFICATE_PEM = """
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

    private static final String SIGNING_PRIVATE_KEY_PEM = """
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

    private static final String UNTRUSTED_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDWzCCAkOgAwIBAgIUZlLInkfJZTA+YqpzBSoKhGWZ/LMwDQYJKoZIhvcNAQEL
            BQAwPTEWMBQGA1UEAwwNQWNtZSBUZXN0IElkUDEWMBQGA1UECgwNQWNtZSBUcmFp
            bmluZzELMAkGA1UEBhMCVVMwHhcNMjYwOTIyMDY1NDM2WhcNMzYwOTE5MDY1NDM2
            WjA9MRYwFAYDVQQDDA1BY21lIFRlc3QgSWRQMRYwFAYDVQQKDA1BY21lIFRyYWlu
            aW5nMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
            ALysnG6LSFk3y5PzEXWijmzY6HSOeyH7024BMP1+SlVQaeU/LqQbi/Mw3+lVqTu5
            myBwjyjcKeYVenf5jEJwn7dbqA2a77bt6mv9xi4gjQ7nPmW3Ithd/QPmgm3VT8pK
            8mO9GXnZKBDL61u4cxPMZoKVLS3Pc0aIEEFDBV9qLcHAESG2EZV7rqNYd824Hes2
            zwedzjtHncG0kH5zlyb4SAdJUj6prfLRBOWXSbfAMT8ChdBO0sqjLsvNnCOlQLmA
            WmsKpxJjaNYzZnIJaC73V/7X7fDGf76RvJeIxkEy+8ln7n7ohOG2jCfHkjvXbVfn
            uOi+UcADUUmtbhoZxjhZ5hUCAwEAAaNTMFEwHQYDVR0OBBYEFPfK7u0s+LX6Ub/U
            IsmiXhnqoq2EMB8GA1UdIwQYMBaAFPfK7u0s+LX6Ub/UIsmiXhnqoq2EMA8GA1Ud
            EwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAJzgRLaQuylkkbGe34A+wlKd
            OKaFK4phBAVQYMrqgtHR3UAY82mzhJ2HJ7Ue1WNGhPFxjnFvNZZ6xNOIJ4wJ14md
            LuS3qRLcvg9V7ItYfaf3F8/2hUMmj5rP7ifcnn/bM+bNzH3RoVzljuNcQHMSNpjz6
            8WpYNtIMQf9VTFNORPDtZC28+z2tdr3c7Z9zL2KPfmugblkTAyY2faTx0gcX+gCz
            +fnpU1qyYvpKygD5iRY8tDn3JtAJoIJKc93r461BGdXp3TABAB5eLYex1CNiaudB
            3wbDcrZEXke4a0YT4ltPgeyg6THaDB+lEC3AMQlh4dvKclP+2QltpsUkRD2S5s=
            -----END CERTIFICATE-----
            """;

    /*
     * Public, disposable Day 12 signing fixtures. These private keys provide
     * no secrecy and must never be used for a tenant or another application.
     */
    private static final String ROLLOVER_B_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDbTCCAlWgAwIBAgIUQmQHR0wuuU/kPHobw/opEzKLzWIwDQYJKoZIhvcNAQEL
            BQAwRjEfMB0GA1UEAwwWQWNtZSBEYXkgMTIgQiBUZXN0IElkUDEWMBQGA1UECgwN
            QWNtZSBUcmFpbmluZzELMAkGA1UEBhMCVVMwHhcNMjYxMDAyMTAxMzUxWhcNMzYw
            OTI5MTAxMzUxWjBGMR8wHQYDVQQDDBZBY21lIERheSAxMiBCIFRlc3QgSWRQMRYw
            FAYDVQQKDA1BY21lIFRyYWluaW5nMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcN
            AQEBBQADggEPADCCAQoCggEBALKk+GBZ+SNCx3PwzcSYzBgQSVSPP/Pd4XPST02L
            A+XIZkSAC0CyDkvgSnTzXPqYDZqrb0S9xuflGmBXkNwq75R/dE4O4afS5z2VKs36
            YeVdQAWdKo27OepRmWTGptQqUlkwjXThqmYKM6qgCn7rpiNTAjKs1n7SRyNuPIUc
            oP0BEUhE1TfzA7z2w7tgVRIHvq+NdOixu8OZxa7LO4NSUn5JuXGR1mPG6gfi4k+E
            VA0G8te6Ya0gQyoJ8zdHdKrb4+M6hmYk65lim43InV3ubeWvb4EwS7HZ6BuDmqy+
            eQCB2iYMgZxBpS3upBFIPT0UUh/LZ1Dm16ub+Z4qpCD0Z/cCAwEAAaNTMFEwHQYD
            VR0OBBYEFCHa0/Gb3AMWy7L26OEYD68S6eepMB8GA1UdIwQYMBaAFCHa0/Gb3AMW
            y7L26OEYD68S6eepMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEB
            ADyRipqEHhOWMejGLrBYMfMU7E7c4BDiLZ1fonbY3O5p2Ja1P4CPf+N+ZF/GHkdq
            4pAIyePFivaz0kiQikYfjA3XRwSoC/Yj/DdDa4Wo9wpkUUNku6in8RqI6c/8lWXT
            G/5CnR5PZ7f+1AeVzgDCaPY81HPH5rLXNRw8ksD+cEUMoD2J47oiTmHLOpTCYeq+
            foiAjiN0l2rj52R4HmsO+dlOLRy3BB2z620eNJF1cqHxHsYjOz48yOoCdsO2aFS2
            +noU2kPz7nT6x6lujtjzICAaDTMZOQ8MTDBlJpR6MzRsW4kAYxGfahCjrueRI/Uc
            rjv+8XgO+eafwzTms+OkeOg=
            -----END CERTIFICATE-----
            """;

    private static final String ROLLOVER_B_PRIVATE_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCypPhgWfkjQsdz
            8M3EmMwYEElUjz/z3eFz0k9NiwPlyGZEgAtAsg5L4Ep081z6mA2aq29Evcbn5Rpg
            V5DcKu+Uf3RODuGn0uc9lSrN+mHlXUAFnSqNuznqUZlkxqbUKlJZMI104apmCjOq
            oAp+66YjUwIyrNZ+0kcjbjyFHKD9ARFIRNU38wO89sO7YFUSB76vjXTosbvDmcWu
            yzuDUlJ+SblxkdZjxuoH4uJPhFQNBvLXumGtIEMqCfM3R3Sq2+PjOoZmJOuZYpuN
            yJ1d7m3lr2+BMEux2egbg5qsvnkAgdomDIGcQaUt7qQRSD09FFIfy2dQ5term/me
            KqQg9Gf3AgMBAAECggEAJIgr6rg7hIRxeTozOhLtGbaq6EnrEBm9swu8/+R/xYu3
            riZpJq/C0K1rTIM/7lcN4SVRucL9XAqz3CPMEdoN6FYwGI5egw1UEHniqQCc6GSr
            ZPqA6z7wVwSc08jz8Ms+z9Jn+xDy4a8QZcIYo1/ZN7wP5QEHVCP4PDycz7PNEC/B
            2LbgQ1yCi2xl8GLC+jZRca/1omqfv9Jajnxk+Xs9+KOa6Kj+ERcYIJtnEMfkEUni
            4cfja55gZKoBOH5f+CV4Td6d94zwpmeRzpSbb1MddebykIOCg94rDXlZTy7nKjYy
            g6B+Vosh5nnpS4hGqg24TC/rnXu8uKlM8zKPncuvMQKBgQDq9P/sN3r/XByLMGMc
            gOJIIozHx3SQUOjC+Qwozxi8O1EB6+TK4XPSoIKRcR5wEuMUFmhoBUP/RHARDnQI
            nQfTpsdCvw0zFwMxnWYhvZp2zPMjjl206lUEW/584Hi70recWX9jhMqP8KVyXvaV
            G6Fe3/f3wx2f9K2KK+jy0c6hgwKBgQDCpNuG5C8DMf9KSt7mVcD09xq+bcFPxVK3
            vTdPGxBgSF3F63OJAVQF81ynlizzlGfeYSXdn7ocfUxymLFWADNVb1HDXOw92eRl
            Mu1GaRNZOKponHtZ7vhSgZW4SB+ROU0mbAfTunNK2JXy5qpmL4AfpK2oEV7RUn6f
            MNyq7CNZfQKBgEZ6xcZLAjdVny5VjnV/Z+Fxk79d4mZkDt5lrLMVJHtaY7tq0o/V
            P1QgV+pe/11pHPrqmdkSM0qAcgl7x2zKBg4ESmOIQeJgddHNQFTAtnQKmKjCzPM3
            E9eh7N3yy+SzmeZppl/o9oZlDowXVmp2BlsaXhzRR7Kyx9fZwiAMtaoXAoGAb6RL
            XiWHaZfFzAEBtK+/C0Kojk05sd2GQmk/ThpB3FfloV4ZWJ3wabFala0nf1bB9OVX
            6LRy9WBQ9vHp1WAsEXbWOO4Veqx9uiXpvpcKSASeiX4nqj/NItW84IRNxuhM/hq8
            qo6pDmcIKthvKElafcvg5yN/dSSSCBDooQjshakCgYBXo6DjuM8hUQT5bzuOPUEO
            qW7pdAz3A2ZZqvvCVtxq7hY/FMp6acmb0nmt4uc6QB8dp1wcAkikkF4gGzHb4oga
            dOfyhORf5knH3hOFzDgS8UXEdayyqV9W2jMNuZDefoj9j+oLOAIuxMIyVZp+MRKi
            rrzWefwQXJiK5ImZOcadPQ==
            -----END PRIVATE KEY-----
            """;

    private static final String ROLLOVER_C_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDbTCCAlWgAwIBAgIUEG6Mh/F1bKCeRvBvVcavuxbrIpUwDQYJKoZIhvcNAQEL
            BQAwRjEfMB0GA1UEAwwWQWNtZSBEYXkgMTIgQyBUZXN0IElkUDEWMBQGA1UECgwN
            QWNtZSBUcmFpbmluZzELMAkGA1UEBhMCVVMwHhcNMjYxMDAyMTAxMzUxWhcNMzYw
            OTI5MTAxMzUxWjBGMR8wHQYDVQQDDBZBY21lIERheSAxMiBDIFRlc3QgSWRQMRYw
            FAYDVQQKDA1BY21lIFRyYWluaW5nMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcN
            AQEBBQADggEPADCCAQoCggEBAMO6ZR2pk0fKNAhqao8HRuPO+A210HzB/rSvKqyo
            pb0CINbg1zA29/STfbAk91+Jq51IC+BjdNsNX54cwLrUNQMmt14CgM6bvlZpuQ1V
            rENQVy89grCl1MzUwIMcgr5MlxnXM7ULkPGW8sVDg9kK3AHu+0hKfWu7pWJEqyMb
            UZ5nZBUlka9oIIyyjdZO5qniZSSeKH7gKQ2RqfzKCdY7jKvExhvXcv486Hvv5W3A
            gRsEKdf02cnQCW2s3ShvUPxkjk2IcGJsQ3sVh1iqG+f4D9pJxyecsH62reOOzPKF
            Bp0YFyXUVSPoz2n4BdwCH6318/JXMJiwfouJ5y3jnGjmTosCAwEAAaNTMFEwHQYD
            VR0OBBYEFBBX2IdsnVVxXuCMsIxGLW9ZOEAMMB8GA1UdIwQYMBaAFBBX2IdsnVVx
            XuCMsIxGLW9ZOEAMMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEB
            AKY98nG1tuECe4i9UyEWNW9i4/1p0U3DbMmO+Kj4fKFLNC6IU+p5ukoruYtQ/qGL
            cpHYRzKvGed2mjPQd0sDSp+dq19qYcfZMVxnKIPq18ZL14YadQjvDTIlgiw/1Hdp
            9yR3r7GXuuaBdo5RjJFcNJDkgYGk4ZAcpLS3P05tCk2KsLR9d5d0/U0c/aSVVzXz
            CgVbY+sDTisS00wLYrBnkQBp6c5LXOhFvqT2UZ9vvPg6C92tZnBu0befo8ztzTb1
            CIv4qSXXbsfr+gVYnckUq+0clNyscawgJvIvdRpasNV+XFCrGw5gsljNMlaNXbKG
            Y3JnVWblcbulKDQCc/8G31s=
            -----END CERTIFICATE-----
            """;

    private static final String ROLLOVER_C_PRIVATE_KEY_PEM = """
            -----BEGIN PRIVATE KEY-----
            MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDDumUdqZNHyjQI
            amqPB0bjzvgNtdB8wf60ryqsqKW9AiDW4NcwNvf0k32wJPdfiaudSAvgY3TbDV+e
            HMC61DUDJrdeAoDOm75WabkNVaxDUFcvPYKwpdTM1MCDHIK+TJcZ1zO1C5DxlvLF
            Q4PZCtwB7vtISn1ru6ViRKsjG1GeZ2QVJZGvaCCMso3WTuap4mUknih+4CkNkan8
            ygnWO4yrxMYb13L+POh77+VtwIEbBCnX9NnJ0AltrN0ob1D8ZI5NiHBibEN7FYdY
            qhvn+A/aSccnnLB+tq3jjszyhQadGBcl1FUj6M9p+AXcAh+t9fPyVzCYsH6Liect
            45xo5k6LAgMBAAECggEAR7VB94CZo3sa3hxyxs0BNVQuAAPlCtgeI39rkI9HDXy6
            oE8Gt7Tj2hixOzgH41oyZDCxnTeC6AA50FkzaR92/p0QICKlo0xfCxS3xgFl19GW
            54lPGL3gvVyS3VY0NlkkIMT9vi4rH7/QWOI362w1l4XBTUZxNeetG1gSJQ4MtI4C
            LiVTTBe/ZtKjXfM3vZjc/TKSWcAoJhK2XeFxXYJ5fNDiAkjPLgSXJWUU+jXr3rT6
            wQVQ5GnGjg0tE1dKISmBsv16k/WjdV+2vr1fCmcougd9623VYU5KXSf7gpwDUFOI
            SNIRtNC+MKwOBr80WWkk4DfvSsp7khDmaL6ZbMq4lQKBgQD+56wmxkNfctz+RdRz
            TsOqlRdMHYN0gnmOVzW/Wdjk8t8K6mqNZBoJ1GvYJhDHr+82xkqwsR40fy0zlPAf
            7OQlnuwhPJGGj6lObXWFgZQoIroacWN/0K6+AjPiMRhPaTRQ4nsJbbIGsPVeGYFb
            ZZEP1IXRipjxCQPu5YjUSoYTjQKBgQDEkaTL7NZk25xAIarPuQu7xSfxHqvVmTI2
            7K5MXOb1wR0kvWHQacf5dBXRnMJ2Pw4A7J0NcowA2asB5maSyfqC+t0CxBTiUmDn
            Qk1UvYzMQUpnd+FF+GhFkOgZ+gH2kZVvKVMNiD7NRVzDWutPo6Z29H0Wi7K35sXq
            XKI3HzYYdwKBgQCoew4hLshXLT9+XT9X24aemB6m85bwilC30VK4IDWo1hKwT1KQ
            E8rWFm4VlstegR3WkWfKs7boMer5fgbcwyHk787ZBQSW8RuRt+2GiagYgyOI2MtQ
            Lulgs2oBpjuQOVQX5io2iCe0HoB/atJCS7Z+xRSR9E60eiX8YAB5eKx3/QKBgQCy
            pQ7Zimgah4AMxMxBNpKUVw0C1PYkDLOXOSj7G5+Hj7dV0YvY5pooerjtpIMTBiFK
            87+UHhthFnGVK3jjRQ8YBLfhsKSuP2H0KsyvDAmvBFODj267sZPKTXSzTwSDuzHN
            MghaDw3MbpJstO+QlFFQYMhiOhn1ipUqdn+yivoV5wKBgHj1qAekK06A+3Wbjt0j
            AORrOjkr7eEFQK+vQhErAs8BAWwnNHRYHqoSwh+j3v+EF/Nyu2K9Q6Gmy5oDreXT
            8gnKPaOiB4A8MMu+SYxEkHpPALFwhhZJVKD48+eO4u922V0uPxWD539D/Qhdpalx
            wvTdkwI44IQgG7rtvao81vS0
            -----END PRIVATE KEY-----
            """;

    private static final X509Certificate SIGNING_CERTIFICATE;
    private static final PrivateKey SIGNING_PRIVATE_KEY;
    private static final X509Certificate UNTRUSTED_CERTIFICATE;
    private static final X509Certificate ROLLOVER_B_CERTIFICATE;
    private static final PrivateKey ROLLOVER_B_PRIVATE_KEY;
    private static final X509Certificate ROLLOVER_C_CERTIFICATE;
    private static final PrivateKey ROLLOVER_C_PRIVATE_KEY;

    static {
        OpenSamlInitializationService.initialize();
        SIGNING_CERTIFICATE = readCertificate(SIGNING_CERTIFICATE_PEM);
        SIGNING_PRIVATE_KEY = readPrivateKey(SIGNING_PRIVATE_KEY_PEM);
        UNTRUSTED_CERTIFICATE = readCertificate(UNTRUSTED_CERTIFICATE_PEM);
        ROLLOVER_B_CERTIFICATE = readCertificate(ROLLOVER_B_CERTIFICATE_PEM);
        ROLLOVER_B_PRIVATE_KEY = readPrivateKey(ROLLOVER_B_PRIVATE_KEY_PEM);
        ROLLOVER_C_CERTIFICATE = readCertificate(ROLLOVER_C_CERTIFICATE_PEM);
        ROLLOVER_C_PRIVATE_KEY = readPrivateKey(ROLLOVER_C_PRIVATE_KEY_PEM);
    }

    @Test
    void tamperedSignedResponseIsRejectedAsInvalidSignature() {
        String signedResponse = serialize(sign(validResponse()));

        Authentication baseline = authenticate(signedResponse, SIGNING_CERTIFICATE);
        assertThat(baseline.isAuthenticated()).isTrue();
        assertThat(baseline.getName()).isEqualTo(PRINCIPAL);

        String tamperedResponse = signedResponse.replace(PRINCIPAL, "tampered@acme.test");
        assertThat(tamperedResponse).isNotEqualTo(signedResponse);

        Saml2AuthenticationException error = assertThrows(
                Saml2AuthenticationException.class,
                () -> authenticate(tamperedResponse, SIGNING_CERTIFICATE));

        assertThat(error.getSaml2Error().getErrorCode())
                .isEqualTo(Saml2ErrorCodes.INVALID_SIGNATURE);
    }

    @Test
    void responseSignedByCertificateThatAcmeHrDoesNotTrustIsRejected() {
        String signedResponse = serialize(sign(validResponse()));

        Authentication baseline = authenticate(signedResponse, SIGNING_CERTIFICATE);
        assertThat(baseline.isAuthenticated()).isTrue();

        assertThat(UNTRUSTED_CERTIFICATE).isNotEqualTo(SIGNING_CERTIFICATE);

        Saml2AuthenticationException error = assertThrows(
                Saml2AuthenticationException.class,
                () -> authenticate(signedResponse, UNTRUSTED_CERTIFICATE));

        assertThat(error.getSaml2Error().getErrorCode())
                .isEqualTo(Saml2ErrorCodes.INVALID_SIGNATURE);
    }

    @Test
    void metadataTrustAcceptsAAndBAcrossRolloverButRejectsRemovedOrUnknownKeys(
            @TempDir Path tempDir) throws Exception {

        RelyingPartyRegistration aOnly = registrationFromMetadata(
                tempDir.resolve("a-only.xml"), SIGNING_CERTIFICATE);
        RelyingPartyRegistration overlap = registrationFromMetadata(
                tempDir.resolve("overlap.xml"), SIGNING_CERTIFICATE, ROLLOVER_B_CERTIFICATE);
        RelyingPartyRegistration bOnly = registrationFromMetadata(
                tempDir.resolve("b-only.xml"), ROLLOVER_B_CERTIFICATE);

        assertVerificationCertificates(aOnly, SIGNING_CERTIFICATE);
        assertVerificationCertificates(overlap, SIGNING_CERTIFICATE, ROLLOVER_B_CERTIFICATE);
        assertVerificationCertificates(bOnly, ROLLOVER_B_CERTIFICATE);

        String signedByA = serialize(sign(validResponse(), SIGNING_CERTIFICATE, SIGNING_PRIVATE_KEY));
        String signedByB = serialize(sign(validResponse(), ROLLOVER_B_CERTIFICATE, ROLLOVER_B_PRIVATE_KEY));
        String signedByC = serialize(sign(validResponse(), ROLLOVER_C_CERTIFICATE, ROLLOVER_C_PRIVATE_KEY));

        assertAccepted(aOnly, signedByA);
        assertInvalidSignature(aOnly, signedByB);
        assertAccepted(overlap, signedByA);
        assertAccepted(overlap, signedByB);
        assertInvalidSignature(overlap, signedByC);
        assertInvalidSignature(bOnly, signedByA);
        assertAccepted(bOnly, signedByB);
    }

    private static RelyingPartyRegistration registrationFromMetadata(
            Path metadataPath, X509Certificate... verificationCertificates) throws Exception {

        StringBuilder keys = new StringBuilder();
        for (X509Certificate certificate : verificationCertificates) {
            keys.append("""
                    <md:KeyDescriptor use="signing">
                      <ds:KeyInfo><ds:X509Data><ds:X509Certificate>%s</ds:X509Certificate></ds:X509Data></ds:KeyInfo>
                    </md:KeyDescriptor>
                    """.formatted(Base64.getEncoder().encodeToString(certificate.getEncoded())));
        }

        String metadata = """
                <?xml version="1.0" encoding="UTF-8"?>
                <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
                        xmlns:ds="http://www.w3.org/2000/09/xmldsig#" entityID="%s">
                  <md:IDPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol"
                          WantAuthnRequestsSigned="false">
                    %s
                    <md:SingleSignOnService Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
                            Location="%s"/>
                  </md:IDPSSODescriptor>
                </md:EntityDescriptor>
                """.formatted(IDP_ENTITY_ID, keys, IDP_SSO_URL);
        Files.writeString(metadataPath, metadata, StandardCharsets.UTF_8);

        RelyingPartyRegistrationRepository repository = new SamlRelyingPartyConfig()
                .relyingPartyRegistrationRepository(
                        metadataPath.toUri().toString(),
                        new MockEnvironment(),
                        new DefaultResourceLoader());
        RelyingPartyRegistration registration = Objects.requireNonNull(
                repository.findByRegistrationId(REGISTRATION_ID));
        assertThat(registration.getAssertionConsumerServiceLocation())
                .isEqualTo("{baseUrl}/saml/acs");

        // The servlet resolves {baseUrl} from the request before validation.
        return registration.mutate().assertionConsumerServiceLocation(ACS_URL).build();
    }

    private static void assertVerificationCertificates(
            RelyingPartyRegistration registration, X509Certificate... expected) {
        assertThat(registration.getAssertingPartyMetadata().getVerificationX509Credentials()
                .stream().map(Saml2X509Credential::getCertificate).toList())
                .containsExactlyInAnyOrder(expected);
    }

    private static void assertAccepted(RelyingPartyRegistration registration, String signedResponse) {
        Authentication authentication = authenticate(signedResponse, registration);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(PRINCIPAL);
    }

    private static void assertInvalidSignature(
            RelyingPartyRegistration registration, String signedResponse) {
        Saml2AuthenticationException error = assertThrows(
                Saml2AuthenticationException.class,
                () -> authenticate(signedResponse, registration));
        assertThat(error.getSaml2Error().getErrorCode())
                .isEqualTo(Saml2ErrorCodes.INVALID_SIGNATURE);
    }

    private static Authentication authenticate(
            String samlResponse,
            X509Certificate trustedVerificationCertificate) {

        return authenticate(samlResponse, relyingPartyRegistration(trustedVerificationCertificate));
    }

    private static Authentication authenticate(
            String samlResponse, RelyingPartyRegistration registration) {

        OpenSaml5AuthenticationProvider provider = new OpenSaml5AuthenticationProvider();
        provider.setAssertionValidator(OpenSaml5AuthenticationProvider.AssertionValidator.builder()
                .clockSkew(TEST_CLOCK_SKEW)
                .build());

        return Objects.requireNonNull(provider.authenticate(
                authenticationToken(samlResponse, registration)));
    }

    private static Saml2AuthenticationToken authenticationToken(
            String samlResponse,
            RelyingPartyRegistration registration) {

        AbstractSaml2AuthenticationRequest request = mock(AbstractSaml2AuthenticationRequest.class);
        when(request.getId()).thenReturn(AUTHN_REQUEST_ID);

        return new Saml2AuthenticationToken(
                registration,
                samlResponse,
                request);
    }

    private static RelyingPartyRegistration relyingPartyRegistration(
            X509Certificate trustedVerificationCertificate) {

        Saml2X509Credential verification =
                Saml2X509Credential.verification(trustedVerificationCertificate);

        return RelyingPartyRegistration.withRegistrationId(REGISTRATION_ID)
                .entityId(SP_ENTITY_ID)
                .assertionConsumerServiceLocation(ACS_URL)
                .assertingPartyMetadata(party -> party
                        .entityId(IDP_ENTITY_ID)
                        .singleSignOnServiceLocation(IDP_SSO_URL)
                        .singleSignOnServiceBinding(Saml2MessageBinding.REDIRECT)
                        .wantAuthnRequestsSigned(false)
                        .verificationX509Credentials(credentials -> credentials.add(verification)))
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
        response.getAssertions().add(validAssertion(now));

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
        authnStatement.setSessionIndex("_session-day8");
        assertion.getAuthnStatements().add(authnStatement);

        return assertion;
    }

    private static Subject subject(Instant now) {
        NameID nameId = build(NameID.DEFAULT_ELEMENT_NAME);
        nameId.setValue(PRINCIPAL);

        SubjectConfirmationData data = build(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
        data.setRecipient(ACS_URL);
        data.setInResponseTo(AUTHN_REQUEST_ID);
        data.setNotOnOrAfter(now.plus(Duration.ofMinutes(2)));

        SubjectConfirmation confirmation = build(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
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

        AudienceRestriction restriction = build(AudienceRestriction.DEFAULT_ELEMENT_NAME);
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

    private static Response sign(Response response) {
        return sign(response, SIGNING_CERTIFICATE, SIGNING_PRIVATE_KEY);
    }

    private static Response sign(Response response, X509Certificate certificate, PrivateKey privateKey) {
        BasicCredential credential =
                CredentialSupport.getSimpleCredential(certificate, privateKey);
        credential.setEntityId(IDP_ENTITY_ID);
        credential.setUsageType(UsageType.SIGNING);

        SignatureSigningParameters parameters = new SignatureSigningParameters();
        parameters.setSigningCredential(credential);
        parameters.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        parameters.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        parameters.setSignatureCanonicalizationAlgorithm(
                SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        try {
            SignatureSupport.signObject(response, parameters);
            return response;
        }
        catch (MarshallingException | SignatureException | SecurityException ex) {
            throw new IllegalStateException("Could not sign the synthetic SAML Response", ex);
        }
    }

    private static String serialize(XMLObject object) {
        try {
            return SerializeSupport.nodeToString(XMLObjectSupport.marshall(object));
        }
        catch (MarshallingException ex) {
            throw new IllegalStateException("Could not serialize the synthetic SAML Response", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends XMLObject> T build(QName elementName) {
        return (T) Objects.requireNonNull(
                        XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(elementName))
                .buildObject(elementName);
    }

    private static X509Certificate readCertificate(String pem) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(removePemEnvelope(
                    pem,
                    "-----BEGIN CERTIFICATE-----",
                    "-----END CERTIFICATE-----"));

            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not read the test certificate", ex);
        }
    }

    private static PrivateKey readPrivateKey(String pem) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(removePemEnvelope(
                    pem,
                    "-----BEGIN PRIVATE KEY-----",
                    "-----END PRIVATE KEY-----"));

            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        }
        catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not read the test private key", ex);
        }
    }

    private static String removePemEnvelope(String pem, String begin, String end) {
        return pem.replace(begin, "").replace(end, "").replaceAll("\\s", "");
    }
}
