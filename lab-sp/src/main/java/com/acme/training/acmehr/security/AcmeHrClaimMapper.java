package com.acme.training.acmehr.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AcmeHrClaimMapper {

    static final String EMAIL = "email";
    static final String FIRST_NAME = "firstName";
    static final String LAST_NAME = "lastName";
    static final String EMPLOYEE_NUMBER = "employeeNumber";
    static final String DEPARTMENT = "department";
    static final String GROUPS = "groups";

    static final String MANAGER_GROUP = "AcmeHR-Managers";
    static final String MANAGER_ROLE = "MANAGER";

    private static final List<String> REQUIRED_CLAIMS = List.of(
            EMAIL,
            FIRST_NAME,
            LAST_NAME,
            EMPLOYEE_NUMBER,
            DEPARTMENT);

    public ClaimMapping map(Authentication authentication) {
        if (!(authentication instanceof Saml2AssertionAuthentication samlAuthentication)) {
            throw new IllegalArgumentException(
                    "AcmeHR claim mapping requires an authenticated SAML assertion.");
        }

        Saml2ResponseAssertionAccessor assertion = samlAuthentication.getCredentials();

        String email = firstString(assertion, EMAIL);
        String firstName = firstString(assertion, FIRST_NAME);
        String lastName = firstString(assertion, LAST_NAME);
        String employeeNumber = firstString(assertion, EMPLOYEE_NUMBER);
        String department = firstString(assertion, DEPARTMENT);
        List<String> groups = stringValues(assertion, GROUPS);

        List<String> missingRequiredClaims = new ArrayList<>();
        addIfMissing(missingRequiredClaims, EMAIL, email);
        addIfMissing(missingRequiredClaims, FIRST_NAME, firstName);
        addIfMissing(missingRequiredClaims, LAST_NAME, lastName);
        addIfMissing(missingRequiredClaims, EMPLOYEE_NUMBER, employeeNumber);
        addIfMissing(missingRequiredClaims, DEPARTMENT, department);

        List<String> mappedRoles = groups.contains(MANAGER_GROUP)
                ? List.of(MANAGER_ROLE)
                : List.of();

        return new ClaimMapping(
                authentication.getName(),
                email,
                firstName,
                lastName,
                employeeNumber,
                department,
                groups,
                mappedRoles,
                List.copyOf(missingRequiredClaims));
    }

    public boolean hasRole(Authentication authentication, String role) {
        return map(authentication).mappedRoles().contains(role);
    }

    private static String firstString(Saml2ResponseAssertionAccessor assertion, String name) {
        Object value = assertion.getFirstAttribute(name);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }

    private static List<String> stringValues(Saml2ResponseAssertionAccessor assertion, String name) {
        List<Object> values = assertion.getAttribute(name);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null)
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private static void addIfMissing(List<String> missingClaims, String name, String value) {
        if (!StringUtils.hasText(value) && REQUIRED_CLAIMS.contains(name)) {
            missingClaims.add(name);
        }
    }

    public record ClaimMapping(
            String principalName,
            String email,
            String firstName,
            String lastName,
            String employeeNumber,
            String department,
            List<String> groups,
            List<String> mappedRoles,
            List<String> missingRequiredClaims) {

        public boolean hasMissingRequiredClaims() {
            return !missingRequiredClaims.isEmpty();
        }

        public boolean hasRole(String role) {
            return mappedRoles.contains(role);
        }
    }
}
