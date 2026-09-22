package com.acme.training.acmehr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=")
@AutoConfigureMockMvc
class AcmeHrTrainingSpApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationStartsWithoutIdpMetadata() {
        assertThat(applicationContext
                .getBeansOfType(RelyingPartyRegistrationRepository.class))
                .isEmpty();
    }

    @Test
    void homePageExplainsThatSamlIsNotConfigured() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Waiting for IDP_METADATA_URL")));
    }

    @Test
    void loginFailsClearlyWhenIdpMetadataIsMissing() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("SAML login is not configured yet.")));
    }

    @Test
    void transactionPageDoesNotClaimUnprovenSuccess() throws Exception {
        mockMvc.perform(get("/transaction"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NOT CHECKED")));
    }
}
