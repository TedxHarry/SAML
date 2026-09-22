package com.acme.training.acmehr.web;

import java.net.URI;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private static final String SAML_LOGIN_START = "/saml2/authenticate/acmehr";

    private final ObjectProvider<RelyingPartyRegistrationRepository> relyingParties;

    public HomeController(ObjectProvider<RelyingPartyRegistrationRepository> relyingParties) {
        this.relyingParties = relyingParties;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        boolean samlConfigured = relyingParties.getIfAvailable() != null;
        boolean applicationSessionActive = isAuthenticated(authentication);

        return """
                AcmeHR Training
                Status: Application is running
                SAML configuration: %s
                Application session: %s

                Sign in with Okta:
                %s

                Protected page:
                /protected
                """.formatted(
                samlConfigured ? "Ready" : "Waiting for IDP_METADATA_URL",
                applicationSessionActive ? "Active" : "Not active",
                samlConfigured ? "/login" : "Unavailable until IDP_METADATA_URL is configured");
    }

    @GetMapping("/login")
    public ResponseEntity<String> login() {
        if (relyingParties.getIfAvailable() == null) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("""
                            AcmeHR Training

                            SAML login is not configured yet.
                            Set IDP_METADATA_URL and restart the application.
                            """);
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(SAML_LOGIN_START))
                .build();
    }

    @GetMapping("/protected")
    public String protectedPage(Authentication authentication) {
        return """
                AcmeHR Training
                Signed in successfully
                Application session: Active
                Authenticated principal: %s
                """.formatted(authentication.getName());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
