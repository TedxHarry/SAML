package com.acme.training.acmehr.security;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class SamlAuthenticationRequestTests {

    private static final String PROTOCOL_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
    private static final String ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void spInitiatedLoginCreatesExpectedRedirectBindingAuthnRequest() throws Exception {
        var result = mockMvc.perform(get("/saml2/authenticate/acmehr")
                        .with(request -> {
                            request.setScheme("http");
                            request.setServerName("localhost");
                            request.setServerPort(8000);
                            return request;
                        }))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        assertThat(location)
                .isNotNull()
                .startsWith("https://idp.acme.test/sso?")
                .contains("SAMLRequest=");

        String encodedRequest = queryParameter(location, "SAMLRequest");
        String relayState = queryParameter(location, "RelayState");

        assertThat(encodedRequest).isNotBlank();
        assertThat(relayState).isNotBlank();

        Document document = parseXml(decodeRedirectRequest(encodedRequest));
        Element authnRequest = document.getDocumentElement();

        assertThat(authnRequest.getLocalName()).isEqualTo("AuthnRequest");
        assertThat(authnRequest.getNamespaceURI()).isEqualTo(PROTOCOL_NS);
        assertThat(authnRequest.getAttribute("ID")).startsWith("ARQ");
        assertThat(authnRequest.getAttribute("Version")).isEqualTo("2.0");
        assertThat(authnRequest.getAttribute("IssueInstant")).isNotBlank();
        assertThat(authnRequest.getAttribute("Destination"))
                .isEqualTo("https://idp.acme.test/sso");
        assertThat(authnRequest.getAttribute("AssertionConsumerServiceURL"))
                .isEqualTo("http://localhost:8000/saml/acs");
        assertThat(authnRequest.getAttribute("ProtocolBinding"))
                .isEqualTo("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        assertThat(authnRequest.getAttribute("ForceAuthn")).isEqualTo("false");
        assertThat(authnRequest.getAttribute("IsPassive")).isEqualTo("false");

        assertThat(document.getElementsByTagNameNS(ASSERTION_NS, "Issuer").item(0).getTextContent().trim())
                .isEqualTo("urn:acme:training:sp");
        assertThat(document.getElementsByTagNameNS(PROTOCOL_NS, "NameIDPolicy").getLength())
                .isZero();
        assertThat(document.getElementsByTagNameNS(PROTOCOL_NS, "RequestedAuthnContext").getLength())
                .isZero();
    }

    @Test
    void protectedPageStartsSamlLoginForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        containsString("/saml2/authenticate?registrationId=acmehr")));
    }

    private static String queryParameter(String location, String name) {
        String rawQuery = URI.create(location).getRawQuery();
        assertThat(rawQuery).isNotNull();

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (name.equals(key)) {
                String value = parts.length == 2 ? parts[1] : "";
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String decodeRedirectRequest(String encodedRequest) throws Exception {
        byte[] compressed = Base64.getDecoder().decode(encodedRequest);
        Inflater inflater = new Inflater(true);
        try (InflaterInputStream input =
                     new InflaterInputStream(new ByteArrayInputStream(compressed), inflater)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        finally {
            inflater.end();
        }
    }

    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
