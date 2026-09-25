package com.acme.training.acmehr.lifecycle;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.acme.training.acmehr.security.AcmeHrClaimMapper.ClaimMapping;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AcmeHrTrainingAccountService {

    static final String MATCH_KEY = "principalName";
    static final String REQUIRED_JIT_ATTRIBUTE = "employeeNumber";

    private final Map<String, TrainingAccount> accountsByPrincipal = new ConcurrentHashMap<>();

    public JitResult matchOrCreate(ClaimMapping mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException(
                    "AcmeHR JIT requires validated SAML claim data.");
        }

        String principalName = requirePrincipalName(mapping.principalName());

        TrainingAccount existing = accountsByPrincipal.get(principalName);
        if (existing != null) {
            return JitResult.matched(existing);
        }

        if (!StringUtils.hasText(mapping.employeeNumber())) {
            return JitResult.failed(
                    principalName,
                    REQUIRED_JIT_ATTRIBUTE + " is required to create an AcmeHR training account.");
        }

        TrainingAccount candidate = new TrainingAccount(
                principalName,
                mapping.employeeNumber(),
                mapping.email(),
                mapping.firstName(),
                mapping.lastName(),
                mapping.department());

        TrainingAccount createdOrExisting = accountsByPrincipal.putIfAbsent(principalName, candidate);

        if (createdOrExisting != null) {
            return JitResult.matched(createdOrExisting);
        }

        return JitResult.created(candidate);
    }

    public TrainingAccount findByPrincipalName(String principalName) {
        if (!StringUtils.hasText(principalName)) {
            return null;
        }

        return accountsByPrincipal.get(principalName);
    }

    public Collection<TrainingAccount> accounts() {
        return accountsByPrincipal.values().stream()
                .sorted(Comparator.comparing(TrainingAccount::principalName))
                .toList();
    }

    public boolean removeForTraining(String principalName) {
        if (!StringUtils.hasText(principalName)) {
            return false;
        }

        return accountsByPrincipal.remove(principalName) != null;
    }

    public void resetForTraining() {
        accountsByPrincipal.clear();
    }

    private static String requirePrincipalName(String principalName) {
        if (!StringUtils.hasText(principalName)) {
            throw new IllegalArgumentException(
                    "AcmeHR JIT requires a non-empty authenticated SAML principal.");
        }

        return principalName;
    }

    public enum JitStatus {
        MATCHED,
        CREATED,
        FAILED
    }

    public record JitResult(
            JitStatus status,
            String matchKey,
            String matchValue,
            TrainingAccount account,
            List<String> errors) {

        static JitResult matched(TrainingAccount account) {
            return new JitResult(
                    JitStatus.MATCHED,
                    MATCH_KEY,
                    account.principalName(),
                    account,
                    List.of());
        }

        static JitResult created(TrainingAccount account) {
            return new JitResult(
                    JitStatus.CREATED,
                    MATCH_KEY,
                    account.principalName(),
                    account,
                    List.of());
        }

        static JitResult failed(String principalName, String error) {
            return new JitResult(
                    JitStatus.FAILED,
                    MATCH_KEY,
                    principalName,
                    null,
                    List.of(error));
        }

        public boolean matchedExistingAccount() {
            return status == JitStatus.MATCHED;
        }

        public boolean createdAccount() {
            return status == JitStatus.CREATED;
        }

        public boolean failed() {
            return status == JitStatus.FAILED;
        }
    }

    public record TrainingAccount(
            String principalName,
            String employeeNumber,
            String email,
            String firstName,
            String lastName,
            String department) {
    }
}
