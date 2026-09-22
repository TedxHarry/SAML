package com.acme.training.acmehr.web;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class Day7ClaimsAuthorizationMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void claimsPageRendersValidatedClaimsAndMappedManagerRole() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(authentication(authentication(Map.of(
                                "email", List.of("priya@acme.example"),
                                "firstName", List.of("Priya"),
                                "lastName", List.of("Shah"),
                                "employeeNumber", List.of("E10427"),
                                "department", List.of("Finance"),
                                "groups", List.of("AcmeHR-Employees", "AcmeHR-Managers"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AcmeHR SAML Claims")))
                .andExpect(content().string(containsString("priya@acme.example")))
                .andExpect(content().string(containsString("Priya")))
                .andExpect(content().string(containsString("Shah")))
                .andExpect(content().string(containsString("E10427")))
                .andExpect(content().string(containsString("Finance")))
                .andExpect(content().string(containsString("AcmeHR-Employees")))
                .andExpect(content().string(containsString("AcmeHR-Managers")))
                .andExpect(content().string(containsString("MANAGER")));
    }

    @Test
    void claimsPageShowsMissingRequiredClaimWithoutFailingAuthentication() throws Exception {
        mockMvc.perform(get("/claims")
                        .with(authentication(authentication(Map.of(
                                "email", List.of("priya@acme.example"),
                                "firstName", List.of("Priya"),
                                "lastName", List.of("Shah"),
                                "employeeNumber", List.of("E10427"),
                                "groups", List.of("AcmeHR-Employees"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Application session: Active")))
                .andExpect(content().string(containsString("Required AcmeHR claim missing")))
                .andExpect(content().string(containsString("department")))
                .andExpect(content().string(containsString("MISSING")));
    }

    @Test
    void managerPageAllowsExactAllowlistedManagerGroup() throws Exception {
        mockMvc.perform(get("/manager")
                        .with(authentication(authentication(Map.of(
                                "email", List.of("priya@acme.example"),
                                "firstName", List.of("Priya"),
                                "lastName", List.of("Shah"),
                                "employeeNumber", List.of("E10427"),
                                "department", List.of("Finance"),
                                "groups", List.of("AcmeHR-Employees", "AcmeHR-Managers"))))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Access granted")))
                .andExpect(content().string(containsString("AcmeHR-Managers")))
                .andExpect(content().string(containsString("MANAGER")))
                .andExpect(content().string(containsString("ALLOWED")));
    }

    @Test
    void managerPageReturnsForbiddenWhenAuthenticatedUserLacksManagerGroup() throws Exception {
        mockMvc.perform(get("/manager")
                        .with(authentication(authentication(Map.of(
                                "email", List.of("priya@acme.example"),
                                "firstName", List.of("Priya"),
                                "lastName", List.of("Shah"),
                                "employeeNumber", List.of("E10427"),
                                "department", List.of("Finance"),
                                "groups", List.of("AcmeHR-Employees", "acmehr-managers"))))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Access denied")))
                .andExpect(content().string(containsString("Authentication succeeded")))
                .andExpect(content().string(containsString("DENIED")))
                .andExpect(content().string(containsString("No application roles mapped")));
    }

    private static Saml2AssertionAuthentication authentication(
            Map<String, List<Object>> attributes) {

        Saml2ResponseAssertionAccessor assertion = new TestAssertionAccessor(
                "priya@acme.example",
                attributes);

        return new Saml2AssertionAuthentication(
                "priya@acme.example",
                assertion,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                "acmehr");
    }

    private record TestAssertionAccessor(
            String nameId,
            Map<String, List<Object>> attributes)
            implements Saml2ResponseAssertionAccessor {

        @Override
        public String getNameId() {
            return nameId;
        }

        @Override
        public List<String> getSessionIndexes() {
            return List.of("session-1");
        }

        @Override
        public Map<String, List<Object>> getAttributes() {
            return attributes;
        }

        @Override
        public String getResponseValue() {
            return "test-saml-response";
        }
    }
}
