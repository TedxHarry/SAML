package com.acme.training.acmehr.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;

import static org.assertj.core.api.Assertions.assertThat;

class AcmeHrClaimMapperTests {

    private final AcmeHrClaimMapper mapper = new AcmeHrClaimMapper();

    @Test
    void mapsRequiredClaimsAndPreservesAllGroupValues() {
        var authentication = authentication(Map.of(
                "email", List.of("priya@acme.example"),
                "firstName", List.of("Priya"),
                "lastName", List.of("Shah"),
                "employeeNumber", List.of("E10427"),
                "department", List.of("Finance"),
                "groups", List.of("AcmeHR-Employees", "AcmeHR-Managers")));

        AcmeHrClaimMapper.ClaimMapping mapping = mapper.map(authentication);

        assertThat(mapping.principalName()).isEqualTo("priya@acme.example");
        assertThat(mapping.email()).isEqualTo("priya@acme.example");
        assertThat(mapping.firstName()).isEqualTo("Priya");
        assertThat(mapping.lastName()).isEqualTo("Shah");
        assertThat(mapping.employeeNumber()).isEqualTo("E10427");
        assertThat(mapping.department()).isEqualTo("Finance");
        assertThat(mapping.groups())
                .containsExactly("AcmeHR-Employees", "AcmeHR-Managers");
        assertThat(mapping.missingRequiredClaims()).isEmpty();
    }

    @Test
    void mapsOnlyTheAllowlistedManagerGroupToManagerRole() {
        var authentication = authentication(Map.of(
                "email", List.of("priya@acme.example"),
                "firstName", List.of("Priya"),
                "lastName", List.of("Shah"),
                "employeeNumber", List.of("E10427"),
                "department", List.of("Finance"),
                "groups", List.of(
                        "AcmeHR-Employees",
                        "Corporate-WiFi-Users",
                        "AcmeHR-Managers")));

        AcmeHrClaimMapper.ClaimMapping mapping = mapper.map(authentication);

        assertThat(mapping.mappedRoles()).containsExactly("MANAGER");
        assertThat(mapping.hasRole("MANAGER")).isTrue();
        assertThat(mapper.hasRole(authentication, "MANAGER")).isTrue();
    }

    @Test
    void doesNotMapUnrelatedOrDifferentlyCasedGroupsToManager() {
        var authentication = authentication(Map.of(
                "email", List.of("priya@acme.example"),
                "firstName", List.of("Priya"),
                "lastName", List.of("Shah"),
                "employeeNumber", List.of("E10427"),
                "department", List.of("Finance"),
                "groups", List.of(
                        "Corporate-WiFi-Users",
                        "acmehr-managers",
                        "ACMEHR-MANAGERS")));

        AcmeHrClaimMapper.ClaimMapping mapping = mapper.map(authentication);

        assertThat(mapping.mappedRoles()).isEmpty();
        assertThat(mapping.hasRole("MANAGER")).isFalse();
        assertThat(mapper.hasRole(authentication, "MANAGER")).isFalse();
    }

    @Test
    void reportsRequiredClaimsThatAreMissingOrBlank() {
        var authentication = authentication(Map.of(
                "email", List.of("priya@acme.example"),
                "firstName", List.of("Priya"),
                "lastName", List.of("Shah"),
                "employeeNumber", List.of(" "),
                "groups", List.of("AcmeHR-Employees")));

        AcmeHrClaimMapper.ClaimMapping mapping = mapper.map(authentication);

        assertThat(mapping.employeeNumber()).isNull();
        assertThat(mapping.department()).isNull();
        assertThat(mapping.groups()).containsExactly("AcmeHR-Employees");
        assertThat(mapping.missingRequiredClaims())
                .containsExactly("employeeNumber", "department");
        assertThat(mapping.hasMissingRequiredClaims()).isTrue();
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
