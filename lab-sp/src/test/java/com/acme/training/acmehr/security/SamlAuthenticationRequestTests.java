package com.acme.training.acmehr.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class SamlAuthenticationRequestTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void spInitiatedLoginRedirectsBrowserToConfiguredIdpWithSamlRequest() throws Exception {
        mockMvc.perform(get("/saml2/authenticate/acmehr"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        allOf(
                                startsWith("https://idp.acme.test/sso?"),
                                containsString("SAMLRequest="))));
    }
    @Test
    void protectedPageStartsSamlLoginForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        containsString("/saml2/authenticate?registrationId=acmehr")));
    }

}
