package com.acme.training.acmehr.web;

import java.net.URI;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private static final String SAML_LOGIN_START = "/saml2/authenticate/acmehr";

    private final ObjectProvider<RelyingPartyRegistrationRepository> relyingParties;

    public HomeController(ObjectProvider<RelyingPartyRegistrationRepository> relyingParties) {
        this.relyingParties = relyingParties;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        boolean samlConfigured = relyingParties.getIfAvailable() != null;
        boolean applicationSessionActive = isAuthenticated(authentication);

        model.addAttribute("samlConfigured", samlConfigured);
        model.addAttribute("applicationSessionActive", applicationSessionActive);

        return "home";
    }

    @GetMapping("/login")
    @ResponseBody
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
    public String protectedPage(Authentication authentication, Model model) {
        model.addAttribute("principalName", authentication.getName());
        return "protected";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
