package com.acme.training.acmehr.web;

import jakarta.servlet.http.HttpServletResponse;

import com.acme.training.acmehr.lifecycle.AcmeHrTrainingAccountService;
import com.acme.training.acmehr.lifecycle.AcmeHrTrainingAccountService.JitResult;
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
    private final AcmeHrTrainingAccountService trainingAccountService;

    public HomeController(
            ObjectProvider<RelyingPartyRegistrationRepository> relyingParties,
            AcmeHrClaimMapper claimMapper,
            AcmeHrTrainingAccountService trainingAccountService) {
        this.relyingParties = relyingParties;
        this.claimMapper = claimMapper;
        this.trainingAccountService = trainingAccountService;
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
    public String protectedPage(
            Authentication authentication,
            HttpServletResponse response,
            Model model) {

        model.addAttribute("principalName", authentication.getName());

        if (!(authentication instanceof Saml2AssertionAuthentication)) {
            model.addAttribute("samlAccepted", false);
            model.addAttribute("jitEvaluated", false);
            model.addAttribute("jitStatus", "NOT_EVALUATED");
            model.addAttribute("localAccountPresent", false);
            model.addAttribute("applicationAccessAllowed", false);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return "protected";
        }

        ClaimMapping mapping = claimMapper.map(authentication);
        JitResult jitResult = trainingAccountService.matchOrCreate(mapping);

        addClaimMapping(model, mapping);
        addJitEvidence(model, jitResult);

        if (jitResult.failed()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }

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

    private void addJitEvidence(Model model, JitResult jitResult) {
        model.addAttribute("samlAccepted", true);
        model.addAttribute("jitEvaluated", true);
        model.addAttribute("jitStatus", jitResult.status().name());
        model.addAttribute("jitMatchKey", jitResult.matchKey());
        model.addAttribute("jitMatchValue", jitResult.matchValue());
        model.addAttribute("jitErrors", jitResult.errors());
        model.addAttribute("trainingAccount", jitResult.account());
        model.addAttribute("localAccountPresent", jitResult.account() != null);
        model.addAttribute("applicationAccessAllowed", !jitResult.failed());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
