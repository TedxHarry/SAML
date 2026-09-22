package com.acme.training.acmehr.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class SamlResponseRejectionTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unsignedSamlResponseDoesNotCreateAuthenticatedSession() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String responseXml = """
                <samlp:Response
                    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                    ID="_unsigned-response"
                    Version="2.0"
                    IssueInstant="%s"
                    Destination="http://localhost:8000/saml/acs">
                  <saml:Issuer>https://idp.acme.test</saml:Issuer>
                  <samlp:Status>
                    <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
                  </samlp:Status>
                  <saml:Assertion
                      ID="_unsigned-assertion"
                      Version="2.0"
                      IssueInstant="%s">
                    <saml:Issuer>https://idp.acme.test</saml:Issuer>
                    <saml:Subject>
                      <saml:NameID>learner@acme.test</saml:NameID>
                      <saml:SubjectConfirmation
                          Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                        <saml:SubjectConfirmationData
                            Recipient="http://localhost:8000/saml/acs"
                            NotOnOrAfter="%s"/>
                      </saml:SubjectConfirmation>
                    </saml:Subject>
                    <saml:Conditions
                        NotBefore="%s"
                        NotOnOrAfter="%s">
                      <saml:AudienceRestriction>
                        <saml:Audience>urn:acme:training:sp</saml:Audience>
                      </saml:AudienceRestriction>
                    </saml:Conditions>
                    <saml:AuthnStatement AuthnInstant="%s">
                      <saml:AuthnContext>
                        <saml:AuthnContextClassRef>
                          urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
                        </saml:AuthnContextClassRef>
                      </saml:AuthnContext>
                    </saml:AuthnStatement>
                  </saml:Assertion>
                </samlp:Response>
                """.formatted(
                now,
                now,
                now.plus(5, ChronoUnit.MINUTES),
                now.minus(1, ChronoUnit.MINUTES),
                now.plus(5, ChronoUnit.MINUTES),
                now);

        String encodedResponse = Base64.getEncoder()
                .encodeToString(responseXml.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/saml/acs")
                        .param("SAMLResponse", encodedResponse)
                        .with(request -> {
                            request.setScheme("http");
                            request.setServerName("localhost");
                            request.setServerPort(8000);
                            return request;
                        }))
                .andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
    }
}
