package com.acme.training.acmehr.web;

import jakarta.servlet.http.HttpServletResponse;

import com.acme.training.acmehr.security.AcmeHrClaimMapper;
import com.acme.training.acmehr.security.AcmeHrClaimMapper.ClaimMapping;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ObjectProvider<RelyingPartyRegistrationRepository> relyingParties;
    private final AcmeHrClaimMapper claimMapper;

    public HomeController(
            ObjectProvider<RelyingPartyRegistrationRepository> relyingParties,
            AcmeHrClaimMapper claimMapper) {
        this.relyingParties = relyingParties;
        this.claimMapper = claimMapper;
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

    @GetMapping("/claims")
    public String claims(Authentication authentication, Model model) {
        if (!(authentication instanceof Saml2AssertionAuthentication)) {
            return "redirect:/protected";
        }

        ClaimMapping mapping = claimMapper.map(authentication);
        addClaimMapping(model, mapping);

        return "claims";
    }

    @GetMapping("/manager")
    public String manager(
            Authentication authentication,
            HttpServletResponse response,
            Model model) {

        if (!(authentication instanceof Saml2AssertionAuthentication)) {
            return "redirect:/protected";
        }

        ClaimMapping mapping = claimMapper.map(authentication);
        boolean accessGranted = mapping.hasRole("MANAGER");

        addClaimMapping(model, mapping);
        model.addAttribute("accessGranted", accessGranted);

        if (!accessGranted) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }

        return "manager";
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

    private void addClaimMapping(Model model, ClaimMapping mapping) {
        model.addAttribute("principalName", mapping.principalName());
        model.addAttribute("email", mapping.email());
        model.addAttribute("firstName", mapping.firstName());
        model.addAttribute("lastName", mapping.lastName());
        model.addAttribute("employeeNumber", mapping.employeeNumber());
        model.addAttribute("department", mapping.department());
        model.addAttribute("groups", mapping.groups());
        model.addAttribute("mappedRoles", mapping.mappedRoles());
        model.addAttribute("missingRequiredClaims", mapping.missingRequiredClaims());
        model.addAttribute("hasMissingRequiredClaims", mapping.hasMissingRequiredClaims());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
