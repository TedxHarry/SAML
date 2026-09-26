package com.acme.training.acmehr.lifecycle;

import java.util.List;

import com.acme.training.acmehr.lifecycle.AcmeHrTrainingAccountService.JitResult;
import com.acme.training.acmehr.lifecycle.AcmeHrTrainingAccountService.JitStatus;
import com.acme.training.acmehr.lifecycle.AcmeHrTrainingAccountService.TrainingAccount;
import com.acme.training.acmehr.security.AcmeHrClaimMapper.ClaimMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcmeHrTrainingAccountServiceTests {

    private AcmeHrTrainingAccountService service;

    @BeforeEach
    void setUp() {
        service = new AcmeHrTrainingAccountService();
    }

    @Test
    void missingEmployeeNumberFailsJitWithoutCreatingAccount() {
        ClaimMapping mapping = mapping(
                "maya@acme.example",
                null,
                "maya@acme.example",
                "Maya",
                "Patel",
                "Finance");

        JitResult result = service.matchOrCreate(mapping);

        assertThat(result.status()).isEqualTo(JitStatus.FAILED);
        assertThat(result.failed()).isTrue();
        assertThat(result.createdAccount()).isFalse();
        assertThat(result.matchedExistingAccount()).isFalse();
        assertThat(result.matchKey()).isEqualTo("principalName");
        assertThat(result.matchValue()).isEqualTo("maya@acme.example");
        assertThat(result.account()).isNull();
        assertThat(result.errors())
                .containsExactly("employeeNumber is required to create an AcmeHR training account.");

        assertThat(service.findByPrincipalName("maya@acme.example")).isNull();
        assertThat(service.accounts()).isEmpty();
    }

    @Test
    void validClaimMappingCreatesTrainingAccountOnce() {
        ClaimMapping mapping = mapping(
                "maya@acme.example",
                "E20427",
                "maya@acme.example",
                "Maya",
                "Patel",
                "Finance");

        JitResult result = service.matchOrCreate(mapping);

        assertThat(result.status()).isEqualTo(JitStatus.CREATED);
        assertThat(result.createdAccount()).isTrue();
        assertThat(result.failed()).isFalse();
        assertThat(result.matchKey()).isEqualTo("principalName");
        assertThat(result.matchValue()).isEqualTo("maya@acme.example");

        TrainingAccount account = result.account();

        assertThat(account).isNotNull();
        assertThat(account.principalName()).isEqualTo("maya@acme.example");
        assertThat(account.employeeNumber()).isEqualTo("E20427");
        assertThat(account.email()).isEqualTo("maya@acme.example");
        assertThat(account.firstName()).isEqualTo("Maya");
        assertThat(account.lastName()).isEqualTo("Patel");
        assertThat(account.department()).isEqualTo("Finance");

        assertThat(service.accounts()).containsExactly(account);
        assertThat(service.findByPrincipalName("maya@acme.example")).isEqualTo(account);
    }

    @Test
    void secondLoginMatchesExistingAccountBeforeCreateTimeValidation() {
        ClaimMapping firstLogin = mapping(
                "maya@acme.example",
                "E20427",
                "maya@acme.example",
                "Maya",
                "Patel",
                "Finance");

        JitResult created = service.matchOrCreate(firstLogin);

        ClaimMapping secondLoginWithoutEmployeeNumber = mapping(
                "maya@acme.example",
                null,
                "changed-email@acme.example",
                "Changed",
                "Name",
                "Accounting");

        JitResult matched = service.matchOrCreate(secondLoginWithoutEmployeeNumber);

        assertThat(created.status()).isEqualTo(JitStatus.CREATED);
        assertThat(matched.status()).isEqualTo(JitStatus.MATCHED);
        assertThat(matched.matchedExistingAccount()).isTrue();
        assertThat(matched.failed()).isFalse();
        assertThat(matched.account()).isEqualTo(created.account());

        assertThat(service.accounts()).hasSize(1);
        assertThat(service.findByPrincipalName("maya@acme.example"))
                .isEqualTo(created.account());
    }

    @Test
    void repeatedValidLoginDoesNotCreateDuplicateAccount() {
        ClaimMapping mapping = mapping(
                "maya@acme.example",
                "E20427",
                "maya@acme.example",
                "Maya",
                "Patel",
                "Finance");

        JitResult first = service.matchOrCreate(mapping);
        JitResult second = service.matchOrCreate(mapping);

        assertThat(first.status()).isEqualTo(JitStatus.CREATED);
        assertThat(second.status()).isEqualTo(JitStatus.MATCHED);
        assertThat(second.account()).isEqualTo(first.account());
        assertThat(service.accounts()).containsExactly(first.account());
    }

    @Test
    void trainingStateCanRemoveOneAccountOrResetAllAccounts() {
        service.matchOrCreate(mapping(
                "maya@acme.example",
                "E20427",
                "maya@acme.example",
                "Maya",
                "Patel",
                "Finance"));

        service.matchOrCreate(mapping(
                "priya@acme.example",
                "E10427",
                "priya@acme.example",
                "Priya",
                "Shah",
                "Finance"));

        assertThat(service.accounts())
                .extracting(TrainingAccount::principalName)
                .containsExactly("maya@acme.example", "priya@acme.example");

        assertThat(service.removeForTraining("maya@acme.example")).isTrue();
        assertThat(service.removeForTraining("maya@acme.example")).isFalse();
        assertThat(service.findByPrincipalName("maya@acme.example")).isNull();
        assertThat(service.accounts())
                .extracting(TrainingAccount::principalName)
                .containsExactly("priya@acme.example");

        service.resetForTraining();

        assertThat(service.accounts()).isEmpty();
        assertThat(service.findByPrincipalName("priya@acme.example")).isNull();
    }

    @Test
    void jitRejectsMissingValidatedMappingOrPrincipal() {
        assertThatThrownBy(() -> service.matchOrCreate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AcmeHR JIT requires validated SAML claim data.");

        ClaimMapping blankPrincipal = mapping(
                " ",
                "E20427",
                "maya@acme.example",
                "Maya",
                "Patel",
                "Finance");

        assertThatThrownBy(() -> service.matchOrCreate(blankPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AcmeHR JIT requires a non-empty authenticated SAML principal.");

        assertThat(service.accounts()).isEmpty();
    }

    private static ClaimMapping mapping(
            String principalName,
            String employeeNumber,
            String email,
            String firstName,
            String lastName,
            String department) {

        return new ClaimMapping(
                principalName,
                email,
                firstName,
                lastName,
                employeeNumber,
                department,
                List.of("AcmeHR-Employees"),
                List.of(),
                employeeNumber == null ? List.of("employeeNumber") : List.of());
    }
}
