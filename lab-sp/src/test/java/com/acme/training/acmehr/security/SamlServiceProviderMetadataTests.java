package com.acme.training.acmehr.security;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class SamlServiceProviderMetadataTests {

    private static final String METADATA_NS =
            "urn:oasis:names:tc:SAML:2.0:metadata";
    private static final String XML_SIGNATURE_NS =
            "http://www.w3.org/2000/09/xmldsig#";
    private static final String NAME_ID_FORMAT =
            "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";

    /*
     * Public disposable training fixture only.
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

    private static final TestKeyFiles TEST_KEY_FILES = writeTestKeyFiles();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void day9Properties(DynamicPropertyRegistry registry) {
        registry.add(
                SamlRelyingPartyConfig.SP_SIGNING_PRIVATE_KEY_LOCATION_PROPERTY,
                TEST_KEY_FILES::privateKeyLocation);
        registry.add(
                SamlRelyingPartyConfig.SP_SIGNING_CERTIFICATE_LOCATION_PROPERTY,
                TEST_KEY_FILES::certificateLocation);
        registry.add(
                SamlRelyingPartyConfig.SP_DECRYPTION_PRIVATE_KEY_LOCATION_PROPERTY,
                TEST_KEY_FILES::privateKeyLocation);
        registry.add(
                SamlRelyingPartyConfig.SP_DECRYPTION_CERTIFICATE_LOCATION_PROPERTY,
                TEST_KEY_FILES::certificateLocation);
        registry.add(
                SamlRelyingPartyConfig.NAME_ID_FORMAT_PROPERTY,
                () -> NAME_ID_FORMAT);
    }

    @Test
    void publishedMetadataAdvertisesExpectedEntityIdAcsAndDay9PublicKeys()
            throws Exception {

        String metadata = mockMvc.perform(get("/saml2/metadata/acmehr")
                        .with(request -> {
                            request.setScheme("http");
                            request.setServerName("localhost");
                            request.setServerPort(8000);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Document document = parseXml(metadata);
        Element entityDescriptor = document.getDocumentElement();

        assertThat(entityDescriptor.getLocalName()).isEqualTo("EntityDescriptor");
        assertThat(entityDescriptor.getNamespaceURI()).isEqualTo(METADATA_NS);
        assertThat(entityDescriptor.getAttribute("entityID"))
                .isEqualTo("urn:acme:training:sp");

        NodeList acsNodes = document.getElementsByTagNameNS(
                METADATA_NS,
                "AssertionConsumerService");

        assertThat(acsNodes.getLength()).isGreaterThanOrEqualTo(1);

        List<String> acsLocations = new ArrayList<>();
        for (int index = 0; index < acsNodes.getLength(); index++) {
            acsLocations.add(((Element) acsNodes.item(index)).getAttribute("Location"));
        }

        assertThat(acsLocations)
                .contains("http://localhost:8000/saml/acs");

        X509Certificate expectedCertificate = readTestCertificate();
        List<KeyDescriptorEvidence> keys = keyDescriptors(document);

        assertThat(keys)
                .anySatisfy(key -> {
                    assertThat(key.use()).isEqualTo("signing");
                    assertThat(key.certificate()).isEqualTo(expectedCertificate);
                })
                .anySatisfy(key -> {
                    assertThat(key.use()).isEqualTo("encryption");
                    assertThat(key.certificate()).isEqualTo(expectedCertificate);
                });

        assertThat(metadata)
                .doesNotContain("PRIVATE KEY")
                .doesNotContain("BEGIN PRIVATE KEY")
                .doesNotContain(Base64.getEncoder().encodeToString(
                        TEST_PRIVATE_KEY_PEM.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<KeyDescriptorEvidence> keyDescriptors(Document document)
            throws Exception {

        NodeList descriptors = document.getElementsByTagNameNS(
                METADATA_NS,
                "KeyDescriptor");

        List<KeyDescriptorEvidence> result = new ArrayList<>();

        for (int index = 0; index < descriptors.getLength(); index++) {
            Element descriptor = (Element) descriptors.item(index);
            NodeList certificateNodes = descriptor.getElementsByTagNameNS(
                    XML_SIGNATURE_NS,
                    "X509Certificate");

            if (certificateNodes.getLength() == 0) {
                continue;
            }

            String certificateText = certificateNodes.item(0)
                    .getTextContent()
                    .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(certificateText);
            X509Certificate certificate =
                    (X509Certificate) CertificateFactory.getInstance("X.509")
                            .generateCertificate(new ByteArrayInputStream(der));

            result.add(new KeyDescriptorEvidence(
                    descriptor.getAttribute("use"),
                    certificate));
        }

        return result;
    }

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static X509Certificate readTestCertificate() throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(
                        normalizePem(TEST_CERTIFICATE_PEM)
                                .getBytes(StandardCharsets.UTF_8)));
    }

    private static TestKeyFiles writeTestKeyFiles() {
        try {
            Path directory = Files.createTempDirectory("acmehr-day9-metadata-");
            Path privateKey = directory.resolve("sp-private-key.pem");
            Path certificate = directory.resolve("sp-certificate.pem");

            Files.writeString(privateKey, normalizePem(TEST_PRIVATE_KEY_PEM));
            Files.writeString(certificate, normalizePem(TEST_CERTIFICATE_PEM));

            directory.toFile().deleteOnExit();
            privateKey.toFile().deleteOnExit();
            certificate.toFile().deleteOnExit();

            return new TestKeyFiles(
                    privateKey.toUri().toString(),
                    certificate.toUri().toString());
        }
        catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static String normalizePem(String pem) {
        return String.join(
                "\n",
                pem.lines()
                        .map(String::strip)
                        .toList()) + "\n";
    }

    private record KeyDescriptorEvidence(
            String use,
            X509Certificate certificate) {
    }

    private record TestKeyFiles(
            String privateKeyLocation,
            String certificateLocation) {
    }
}
