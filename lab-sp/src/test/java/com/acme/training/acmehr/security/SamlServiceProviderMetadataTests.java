package com.acme.training.acmehr.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class SamlServiceProviderMetadataTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishedMetadataAdvertisesExpectedEntityIdAndAcs() throws Exception {
        mockMvc.perform(get("/saml2/metadata/acmehr")
                        .with(request -> {
                            request.setScheme("http");
                            request.setServerName("localhost");
                            request.setServerPort(8000);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "entityID=\"urn:acme:training:sp\"")))
                .andExpect(content().string(containsString(
                        "Location=\"http://localhost:8000/saml/acs\"")));
    }
}
