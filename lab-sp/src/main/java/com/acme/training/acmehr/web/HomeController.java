package com.acme.training.acmehr.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

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

    @GetMapping("/protected")
    public String protectedPage(Authentication authentication, Model model) {
        model.addAttribute("principalName", authentication.getName());
        return "protected";
    }

    @GetMapping("/transaction")
    public String transaction(Authentication authentication, Model model) {
        boolean applicationSessionActive = isAuthenticated(authentication);

        model.addAttribute("loginStarted", applicationSessionActive);
        model.addAttribute("responseReturned", applicationSessionActive);
        model.addAttribute("samlAccepted", applicationSessionActive);
        model.addAttribute("applicationSessionActive", applicationSessionActive);

        return "transaction";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
