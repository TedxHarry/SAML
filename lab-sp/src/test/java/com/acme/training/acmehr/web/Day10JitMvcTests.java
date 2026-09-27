package com.acme.training.acmehr.web;

import java.util.List;
import java.util.Map;

import com.acme.training.acmehr.lifecycle.AcmeHrTrainingAccountService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "acmehr.saml.idp-metadata-url=classpath:idp-metadata.xml")
@AutoConfigureMockMvc
class Day10JitMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AcmeHrTrainingAccountService trainingAccountService;

    @BeforeEach
    void resetTrainingAccounts() {
        trainingAccountService.resetForTraining();
    }

    @Test
    void nonSamlAuthenticationDoesNotRunJit() throws Exception {
        mockMvc.perform(get("/protected")
                        .with(user("maya@acme.example")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("NOT ACCEPTED")))
                .andExpect(content().string(containsString("NOT EVALUATED")))
                .andExpect(content().string(containsString("DENIED")));

        org.assertj.core.api.Assertions.assertThat(trainingAccountService.accounts()).isEmpty();
    }

    @Test
    void validatedSamlWithoutEmployeeNumberShowsSamlPassAndJitFailure() throws Exception {
        mockMvc.perform(get("/protected")
                        .with(authentication(samlAuthentication(
                                "maya@acme.example",
                                Map.of(
                                        "email", List.of("maya@acme.example"),
                                        "firstName", List.of("Maya"),
                                        "lastName", List.of("Patel"),
                                        "department", List.of("Finance"),
                                        "groups", List.of("AcmeHR-Employees"))))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("SAML authentication")))
                .andExpect(content().string(containsString("PASS")))
                .andExpect(content().string(containsString("JIT FAILED")))
                .andExpect(content().string(containsString("DENIED")))
                .andExpect(content().string(containsString(
                        "employeeNumber is required to create an AcmeHR training account.")))
                .andExpect(content().string(containsString("Local training account present")))
                .andExpect(content().string(containsString("NO")));

        org.assertj.core.api.Assertions.assertThat(
                        trainingAccountService.findByPrincipalName("maya@acme.example"))
                .isNull();
    }

    @Test
    void validatedSamlWithEmployeeNumberCreatesLocalTrainingAccount() throws Exception {
        mockMvc.perform(get("/protected")
                        .with(authentication(samlAuthentication(
                                "maya@acme.example",
                                completeAttributes()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PASS")))
                .andExpect(content().string(containsString("JIT CREATED")))
                .andExpect(content().string(containsString("ALLOWED")))
                .andExpect(content().string(containsString("principalName")))
                .andExpect(content().string(containsString("maya@acme.example")))
                .andExpect(content().string(containsString("E20427")));

        org.assertj.core.api.Assertions.assertThat(
                        trainingAccountService.findByPrincipalName("maya@acme.example"))
                .isNotNull();
        org.assertj.core.api.Assertions.assertThat(trainingAccountService.accounts()).hasSize(1);
    }

    @Test
    void secondSamlLoginMatchesExistingAccountWithoutCreatingDuplicate() throws Exception {
        var authentication = samlAuthentication(
                "maya@acme.example",
                completeAttributes());

        mockMvc.perform(get("/protected")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JIT CREATED")));

        mockMvc.perform(get("/protected")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JIT MATCHED")))
                .andExpect(content().string(containsString("ALLOWED")))
                .andExpect(content().string(containsString(
                        "AcmeHR did not create a duplicate account.")));

        org.assertj.core.api.Assertions.assertThat(trainingAccountService.accounts()).hasSize(1);
    }

    private static Map<String, List<Object>> completeAttributes() {
        return Map.of(
                "email", List.of("maya@acme.example"),
                "firstName", List.of("Maya"),
                "lastName", List.of("Patel"),
                "employeeNumber", List.of("E20427"),
                "department", List.of("Finance"),
                "groups", List.of("AcmeHR-Employees"));
    }

    private static Saml2AssertionAuthentication samlAuthentication(
            String principalName,
            Map<String, List<Object>> attributes) {

        Saml2ResponseAssertionAccessor assertion = new TestAssertionAccessor(
                principalName,
                attributes);

        return new Saml2AssertionAuthentication(
                principalName,
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
            return List.of("session-day10");
        }

        @Override
        public Map<String, List<Object>> getAttributes() {
            return attributes;
        }

        @Override
        public String getResponseValue() {
            return "synthetic-day10-post-authentication-fixture";
        }
    }
}
