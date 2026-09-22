# Day 6 Lab: Prove Why AcmeHR Accepts or Rejects a SAML Login

## What you are going to do

Day 5 showed what Okta returned.

Today you will prove why AcmeHR accepts one Response and rejects another.

You will use two evidence sources:

1. A successful live transaction between your Okta app and AcmeHR.
2. Controlled automated tests that create signed training Responses and break one field at a time.

You will:

- capture one clean SP-initiated login
- record AcmeHR's expected issuer, audience, ACS, request ID, and current time
- compare those expected values with the received SAML fields
- prove that a valid transaction creates an authenticated application session
- run a signed known-good Response test
- run separate rejection tests for issuer, audience, destination, recipient, correlation, and time
- distinguish a Response-level error from an Assertion-level error
- explain why an unsigned message fails even when its visible fields look correct
- record the exact failed comparison for each negative test

The lab does not disable any validator.

Each negative test starts with a valid synthetic Response and changes one controlled value.

---

# Before you start

You need:

- the working AcmeHR and Okta integration from Day 3
- the request-capture steps from Day 4
- the Response-decoding steps from Day 5
- the assigned Okta test user
- Docker with Docker Compose for the live transaction
- Docker with Docker Compose for the automated validation tests
- a browser with Developer Tools
- Python 3 for local decoding

Your baseline must still work.

Confirm:

```text
AcmeHR SAML configuration
    Ready

SP-initiated login reaches Okta
    PASS

Browser returns to /saml/acs
    PASS

AcmeHR protected page opens
    PASS
```

Do not begin a failure exercise from an already broken integration.

---

# Safety rules for this lab

A real SAML Response can contain personal and session-related data.

Keep the complete live Response on your own machine.

Do not paste it into:

- a public SAML decoder
- a forum post
- a public issue
- a shared chat
- an unapproved troubleshooting website

The automated tests use synthetic training values such as:

```text
learner@acme.test
https://idp.acme.test
```

They do not need your Okta user's real Response.

Do not copy a production certificate or private key into this repository.

---

# Why this lab uses signed test fixtures

Changing a field inside a captured signed Response damages its XML signature.

For example, if you replace the Audience by hand, the SP can reject the changed message for an invalid signature before audience validation gives useful evidence.

That experiment proves only this:

```text
Signed content changed
        |
        v
Signature no longer verifies
```

It does not isolate the Audience check.

The automated fixture creates a fresh signature after setting each test value. This keeps the signature valid while one selected validation field is wrong.

---

# Part 1: Start the current AcmeHR training SP

Open a terminal in:

```text
lab-sp
```

Use the same Okta Metadata URL that produced your working login.

## macOS or Linux

```bash
export IDP_METADATA_URL='PASTE_YOUR_OKTA_METADATA_URL_HERE'
docker compose up --build
```

## Windows PowerShell

```powershell
$env:IDP_METADATA_URL = "PASTE_YOUR_OKTA_METADATA_URL_HERE"
docker compose up --build
```

Open:

```text
http://localhost:8000
```

Confirm:

```text
SAML configuration
Ready
```

Keep the application terminal open. Its logs provide SP-side evidence if the login fails.

---

# Part 2: Prepare one clean browser transaction

Use a new private or incognito browser window.

Before opening AcmeHR:

1. Open Developer Tools.
2. Select the **Network** tab.
3. Enable **Preserve log**.
4. Clear existing Network entries.
5. Keep Developer Tools open.

Open:

```text
http://localhost:8000/protected
```

Complete the Okta login with the assigned test user.

The browser should return to AcmeHR and show:

```text
Application session: Active
```

Do not close the browser or clear the Network trace.

---

# Part 3: Keep the request and response from the same login

Find both entries in the preserved Network trace:

```text
Request side
    GET to Okta containing SAMLRequest

Response side
    POST to AcmeHR containing SAMLResponse
```

Decode the AuthnRequest with the Day 4 local command.

Decode the SAML Response with the Day 5 local command.

Record the current AuthnRequest ID:

```text
Current AuthnRequest ID
    ARQ...
```

Do not compare the Response with an AuthnRequest captured during an earlier login.

---

# Part 4: Record AcmeHR's expected values

Create this table in private lab notes before reading the Response values.

| Validation check | AcmeHR expected value | Expected value source |
| --- | --- | --- |
| IdP issuer | your exact Okta metadata `entityID` | configured IdP metadata |
| Audience | `urn:acme:training:sp` | AcmeHR SP entity ID |
| Destination | `http://localhost:8000/saml/acs` | actual ACS request URL |
| Recipient | `http://localhost:8000/saml/acs` | configured AcmeHR ACS |
| Response `InResponseTo` | current `ARQ...` value | saved AuthnRequest |
| Confirmation `InResponseTo` | current `ARQ...` value | saved AuthnRequest |
| Validity time | current SP UTC time | AcmeHR host clock |

Record the IdP issuer exactly as it appears in the Okta metadata.

Do not write a display label such as:

```text
Okta
```

The validator compares entity identifiers, not display names.

---

# Part 5: Record the received issuer

In the decoded Response, find the outer issuer:

```xml
<saml2:Issuer>...</saml2:Issuer>
```

Find the Assertion issuer too.

Record:

```text
Expected IdP entity ID
    ...

Received Response Issuer
    ...

Received Assertion Issuer
    ...

Comparison
    MATCH / MISMATCH
```

For the working transaction, both received issuers should identify the configured Okta IdP.

The matching text alone does not prove trust. The successful SP result also depends on valid signature verification.

---

# Part 6: Record the received Audience

Inside the Assertion Conditions, find:

```xml
<saml2:AudienceRestriction>
    <saml2:Audience>...</saml2:Audience>
</saml2:AudienceRestriction>
```

Record:

```text
Expected AcmeHR SP entity ID
    urn:acme:training:sp

Received Audience
    ...

Comparison
    MATCH / MISMATCH
```

For the working transaction, the received Audience should include:

```text
urn:acme:training:sp
```

Do not compare Audience with the ACS URL.

---

# Part 7: Record Destination and the actual request URL

Find the Response `Destination`:

```xml
<saml2p:Response
    Destination="http://localhost:8000/saml/acs">
```

In Developer Tools, record the URL of the POST carrying `SAMLResponse`.

Complete:

```text
Response Destination
    ...

Actual browser POST URL
    ...

Comparison
    MATCH / MISMATCH
```

The local baseline should show:

```text
http://localhost:8000/saml/acs
```

Compare the complete scheme, host, port, and path.

---

# Part 8: Record the bearer Recipient

Inside `SubjectConfirmationData`, find:

```xml
Recipient="..."
```

Record:

```text
Configured AcmeHR ACS
    http://localhost:8000/saml/acs

Received bearer Recipient
    ...

Comparison
    MATCH / MISMATCH
```

Destination and Recipient may contain the same URL, but keep them in separate rows.

The first belongs to the Response. The second belongs to bearer subject confirmation.

---

# Part 9: Prove request correlation

Compare all three request identifiers:

```text
Current AuthnRequest ID
    ARQ...

Response InResponseTo
    ARQ...

SubjectConfirmationData InResponseTo
    ARQ...
```

For the current SP-initiated login, expect:

```text
Current AuthnRequest ID
        =
Response InResponseTo
        =
Confirmation InResponseTo
```

If the Response contains an ID from a different transaction, do not call it a match because both values begin with `ARQ`.

Compare the complete strings.

---

# Part 10: Record the SP's current UTC time

Run the time command as soon as possible after the login.

## macOS or Linux

```bash
date -u
```

## Windows PowerShell

```powershell
(Get-Date).ToUniversalTime().ToString("o")
```

Record:

```text
SP UTC time observed
    ...
```

This is approximate evidence because you record it after the validation occurred.

The automated tests provide deterministic time-boundary evidence later in the lab.

---

# Part 11: Record both Assertion time windows

Find the Assertion Conditions:

```xml
<saml2:Conditions
    NotBefore="..."
    NotOnOrAfter="...">
```

Then find the bearer confirmation cutoff:

```xml
<saml2:SubjectConfirmationData
    NotOnOrAfter="...">
```

Record:

```text
Conditions NotBefore
    ...

Conditions NotOnOrAfter
    ...

SubjectConfirmationData NotOnOrAfter
    ...

SP UTC time observed
    ...
```

For the working transaction, the message should be acceptable within the validator's time rules.

Remember the upper boundary:

```text
current time < NotOnOrAfter
```

At the exact `NotOnOrAfter` instant, the normal validity interval has ended. A configured clock-skew allowance affects how the validator evaluates the boundary.

---

# Part 12: Record IssueInstant without treating it as expiry

Find the `IssueInstant` on:

- the outer Response
- the Assertion

Record both values.

Use them to confirm that the objects belong to the current browser transaction.

Do not write:

```text
IssueInstant
    Assertion expiry
```

The validity boundaries come from `NotBefore` and `NotOnOrAfter`.

---

# Part 13: Prove that the live transaction was accepted

Your decoded field comparisons show that the visible values agree.

Now record the SP result:

```text
Browser returned to AcmeHR protected page
    YES

Application session
    Active

Authenticated principal
    <captured NameID value>
```

This proves that AcmeHR accepted the transaction and created an authenticated session.

Seeing `StatusCode=Success` in the decoded Response would not prove that by itself.

---

# Part 14: Complete the successful-transaction worksheet

Fill this table with your own evidence.

| Check | Expected value | Received value | Result |
| --- | --- | --- | --- |
| IdP issuer | Okta metadata entity ID | Response and Assertion issuer | MATCH / MISMATCH |
| Audience | `urn:acme:training:sp` | Assertion Audience | MATCH / MISMATCH |
| Destination | actual ACS POST URL | Response Destination | MATCH / MISMATCH |
| Recipient | configured ACS URL | bearer Recipient | MATCH / MISMATCH |
| Response correlation | current AuthnRequest ID | Response `InResponseTo` | MATCH / MISMATCH |
| Confirmation correlation | current AuthnRequest ID | confirmation `InResponseTo` | MATCH / MISMATCH |
| Conditions start | SP UTC time and allowed skew | `NotBefore` | ACCEPTABLE / NOT ACCEPTABLE |
| Conditions end | SP UTC time and allowed skew | Conditions `NotOnOrAfter` | ACCEPTABLE / NOT ACCEPTABLE |
| Confirmation end | SP UTC time and allowed skew | confirmation `NotOnOrAfter` | ACCEPTABLE / NOT ACCEPTABLE |
| SP result | authenticated session | protected page result | PASS / FAIL |

Store only redacted evidence if these notes will leave your machine.

---

# Part 15: Stop the live application before running tests

Return to the terminal running Docker Compose.

Press:

```text
Ctrl+C
```

Then stop the Compose resources:

```bash
docker compose down
```

The automated tests do not need your Okta metadata URL or a running container.

They use the synthetic IdP configuration under:

```text
lab-sp/src/test/resources
```

---

# Part 16: Understand the automated evidence

The focused validation tests live in:

```text
lab-sp/src/test/java/com/acme/training/acmehr/security/
    SamlResponseValidationTests.java
```

The earlier unsigned-message boundary remains in:

```text
lab-sp/src/test/java/com/acme/training/acmehr/security/
    SamlResponseRejectionTests.java
```

The focused test class uses synthetic values:

```text
IdP entity ID
    https://idp.acme.test

SP entity ID
    urn:acme:training:sp

ACS
    http://localhost:8000/saml/acs

Principal
    learner@acme.test
```

These tests do not call your Okta org.

---

# Part 17: Build the validation-test container

From the `lab-sp` directory, build the dedicated test runner:

```bash
docker compose build validation-tests
```

The test runner uses the same pinned Java 21 and Maven environment as the training SP build.

You do not need Java or Maven installed directly on your computer.

Now run the complete validation test group:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests,SamlResponseRejectionTests test
```

The same command works in PowerShell.

Expected Maven result:

```text
BUILD SUCCESS
```

The summary should report no unexpected failures or errors.

A passing negative test means the application rejected the invalid Response as expected.

It does not mean the invalid Response was accepted.

---

# Part 18: Read the test map before running individual cases

Use this table to connect each test input with its expected result.

| Test case | One changed property | Expected provider result |
| --- | --- | --- |
| known-good signed Response | none | authenticated |
| wrong issuer | Response Issuer | rejected as invalid issuer |
| wrong audience | Assertion Audience | rejected as invalid Assertion |
| wrong destination | Response Destination | rejected as invalid destination |
| wrong recipient | bearer Recipient | rejected as invalid Assertion |
| wrong response correlation | Response `InResponseTo` | rejected as invalid `InResponseTo` |
| wrong confirmation correlation | confirmation `InResponseTo` | rejected as invalid Assertion |
| not-yet-valid Assertion | Conditions `NotBefore` | rejected as invalid Assertion |
| expired Assertion | Conditions `NotOnOrAfter` | rejected as invalid Assertion |
| expired bearer confirmation | confirmation `NotOnOrAfter` | rejected as invalid Assertion |
| unsigned Response and Assertion | signature protection removed | unauthenticated |

Several Assertion-field failures share the high-level `invalid_assertion` category.

The detailed validation message and the changed fixture field identify the specific cause.

---

# Part 19: Prove the known-good signed Response first

Run the positive control:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#validSignedResponseIsAccepted test
```

Expected result:

```text
BUILD SUCCESS
```

Open the test method and confirm that it checks for an authenticated result.

This positive control matters because every negative fixture is derived from the same valid baseline.

If the positive control fails, stop. A negative result from the other tests would not isolate the changed field.

---

# Part 20: Prove issuer rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#wrongIssuerIsRejected test
```

Record:

```text
Expected issuer
    https://idp.acme.test

Received issuer in negative fixture
    <value from the test>

Provider error category
    <value asserted by the test>

Authentication created
    NO
```

Explain the cause in one sentence:

> The Response was rejected because its issuer did not match the configured IdP entity ID.

This test deliberately leaves the outer Response unsigned and signs the Assertion with the trusted test key. That keeps acceptable signature protection in place while allowing the Response issuer validator to be tested directly.

Do not report this as an Audience or signature failure.

---

# Part 21: Prove Audience rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#wrongAudienceIsRejected test
```

Record:

```text
Expected Audience
    urn:acme:training:sp

Received Audience
    <wrong test value>

Provider error category
    invalid_assertion

Authentication created
    NO
```

The fixture remains correctly signed for the synthetic IdP.

The changed Audience is therefore the controlled reason for rejection.

---

# Part 22: Prove Destination rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#wrongDestinationIsRejected test
```

Record:

```text
Actual or expected ACS
    http://localhost:8000/saml/acs

Response Destination
    <wrong test URL>

Provider error category
    invalid_destination

Authentication created
    NO
```

This is a SAML destination-validation failure.

It is different from an HTTP 404 where the POST never reaches the SAML processing endpoint.

---

# Part 23: Prove Recipient rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#wrongRecipientIsRejected test
```

Record:

```text
Expected Recipient
    http://localhost:8000/saml/acs

Received Recipient
    <wrong test URL>

Provider error category
    invalid_assertion

Authentication created
    NO
```

Compare this result with Part 22.

Both cases involve the ACS URL, but they fail at different fields and validator layers.

---

# Part 24: Prove Response correlation rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#wrongResponseInResponseToIsRejected test
```

Record:

```text
Saved AuthnRequest ID
    <expected test request ID>

Response InResponseTo
    <wrong test request ID>

Provider error category
    invalid_in_response_to

Authentication created
    NO
```

The failure says that this Response does not answer the saved AuthnRequest.

It does not say that the user entered the wrong password.

---

# Part 25: Prove confirmation correlation rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#wrongConfirmationInResponseToIsRejected test
```

Record the saved request ID and the value inside `SubjectConfirmationData`.

Expected result:

```text
Provider error category
    invalid_assertion

Authentication created
    NO
```

The outer Response correlation and inner bearer-confirmation correlation are separate checks.

---

# Part 26: Prove a not-yet-valid Assertion rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#notYetValidAssertionIsRejected test
```

Open the test and record:

```text
Validator current time
    ...

Conditions NotBefore
    ...

Clock skew used by the test
    ...

Provider error category
    invalid_assertion
```

Confirm that `NotBefore` is far enough in the future to remain invalid after the allowed test skew.

Do not change your computer clock for this exercise.

---

# Part 27: Prove an expired Conditions rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#expiredAssertionConditionsAreRejected test
```

Record:

```text
Validator current time
    ...

Conditions NotOnOrAfter
    ...

Clock skew used by the test
    ...

Provider error category
    invalid_assertion
```

Confirm that the cutoff is far enough in the past to remain expired after the allowed test skew.

---

# Part 28: Prove an expired bearer confirmation rejection

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#expiredBearerConfirmationIsRejected test
```

This case keeps the Assertion Conditions valid while expiring only:

```text
SubjectConfirmationData NotOnOrAfter
```

Record the two separate upper boundaries and show which one failed.

Do not write only:

```text
The Assertion was expired.
```

Write the location:

```text
The bearer subject confirmation was expired.
```

---

# Part 29: Prove unsigned content remains unauthenticated

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseRejectionTests#unsignedSamlResponseDoesNotCreateAuthenticatedSession test
```

The unsigned fixture contains reasonable-looking values for:

- issuer
- audience
- destination
- recipient
- time conditions
- NameID

Expected result:

```text
Authenticated session
    NOT CREATED
```

This test proves that readable and familiar XML fields do not replace signature trust.

---

# Part 30: Understand what each passing test proves

For a positive test:

```text
Test passes
    means
The valid signed Response produced the expected authenticated result
```

For a negative test:

```text
Test passes
    means
The invalid Response was rejected in the expected category
```

Do not read `BUILD SUCCESS` as meaning every fixture was accepted.

Maven reports success because the application behaved as each test expected.

---

# Part 31: Build the negative-test evidence table

Complete this table from the test source and results.

| Test | Expected value | Received value | Error category | Authenticated? |
| --- | --- | --- | --- | --- |
| known-good signed Response | all baseline values | all baseline values | none | yes |
| wrong issuer | `https://idp.acme.test` |  |  | no |
| wrong audience | `urn:acme:training:sp` |  |  | no |
| wrong destination | `http://localhost:8000/saml/acs` |  |  | no |
| wrong recipient | `http://localhost:8000/saml/acs` |  |  | no |
| wrong Response `InResponseTo` | saved request ID |  |  | no |
| wrong confirmation `InResponseTo` | saved request ID |  |  | no |
| future `NotBefore` | valid now |  |  | no |
| expired Conditions | valid now |  |  | no |
| expired confirmation | valid now |  |  | no |
| unsigned content | trusted signature protection | absent |  | no |

The expected and received columns are the proof.

The test name alone is not enough evidence.

---

# Part 32: Distinguish high-level and detailed errors

Spring Security exposes specific Response-level categories such as:

```text
invalid_destination
invalid_in_response_to
```

Several Assertion failures use:

```text
invalid_assertion
```

For those cases, use the detailed validation description and controlled changed field to separate:

- Audience failure
- Recipient failure
- confirmation correlation failure
- not-yet-valid Conditions
- expired Conditions
- expired bearer confirmation

Do not claim that `invalid_assertion` names the exact field by itself.

---

# Part 33: Run the full application verification

After the focused tests pass, run the complete project verification:

```bash
docker compose run --rm validation-tests mvn -B -ntp verify
```

Expected result:

```text
BUILD SUCCESS
```

This checks that the Day 6 fixtures did not break the earlier training SP tests.

The repository's GitHub Actions workflow also runs Maven verification when a `lab-sp` file changes.

---

# Part 34: Troubleshoot a test failure in the correct order

If a validation test fails, use these checks in order.

## Check 1: Does the positive control pass?

Run:

```bash
docker compose run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseValidationTests#validSignedResponseIsAccepted test
```

If this fails, investigate the fixture, signing credential, registration, and current dependency versions before trusting any negative case.

---

## Check 2: Did Maven inside the test container select the method?

Read the Maven output from the container and confirm that it ran the named class and method.

A typing error in the method selector can produce a different failure from the SAML test itself.

Copy the method name from the source file.

---

## Check 3: Is the test container current?

The validation-test service is built from the repository's pinned Maven and Java builder image.

If the repository changed after you built the test runner, rebuild it:

```bash
docker compose build validation-tests
```

If you want to confirm the runtime versions inside the test image, run:

```bash
docker compose run --rm validation-tests java -version
docker compose run --rm validation-tests mvn -version
```

The Java runtime should report Java 21.

---

## Check 4: Did more than one fixture field change?

Compare the negative fixture with the positive control.

For an Audience test, do not also change:

- issuer
- destination
- recipient
- correlation ID
- time window
- signing credential

One changed field gives you one explainable failure.

---

## Check 5: Is the fixture still signed with the trusted test key?

The negative validation field should be set before the test signs the Response or Assertion.

Changing XML text after signing produces a signature failure and invalidates the isolation.

---

## Check 6: Did the time failure exceed allowed skew?

A time just a few seconds outside the boundary may still be accepted when clock skew applies.

The negative fixture must place its invalid timestamp beyond the configured test skew.

Read the test's fixed current time, timestamp, and skew together.

---

# Part 35: Mini incident 1

A signed test Response contains:

```text
Issuer
    https://idp.acme.test

Audience
    urn:another:sp

Destination
    http://localhost:8000/saml/acs
```

The configured SP entity ID is:

```text
urn:acme:training:sp
```

Which comparison fails?

<details>
<summary>Check your answer</summary>

Audience validation fails.

The received Audience names another SP. The issuer and destination shown here match their expected values.

</details>

---

# Part 36: Mini incident 2

The browser POST reaches:

```text
http://localhost:8000/saml/acs
```

The Response says:

```text
Destination
    http://localhost:8080/saml/acs
```

What should you record?

<details>
<summary>Check your answer</summary>

Record a Destination mismatch.

The port in the Response is `8080`, while the request reached port `8000`.

Do not call this an Audience failure.

</details>

---

# Part 37: Mini incident 3

The Response `InResponseTo` matches the saved request ID.

The bearer confirmation `InResponseTo` contains a different ID.

Can AcmeHR ignore the inner mismatch because the outer value passed?

<details>
<summary>Check your answer</summary>

No.

Response correlation and bearer subject-confirmation correlation are separate checks.

The invalid bearer confirmation must not be used to establish the authenticated subject.

</details>

---

# Part 38: Mini incident 4

The validator time is:

```text
08:15:00Z
```

Conditions contain:

```text
NotOnOrAfter="08:15:00Z"
```

Ignore clock skew for this question. Is the Assertion still inside its Conditions window?

<details>
<summary>Check your answer</summary>

No.

`NotOnOrAfter` excludes the named instant. The normal validity interval ended at `08:15:00Z`.

</details>

---

# Part 39: Mini incident 5

A negative test changes the Audience after the Response was signed.

The provider reports an invalid signature.

Can the engineer report that the Audience validator rejected the message?

<details>
<summary>Check your answer</summary>

No.

The edit damaged the signature. Build the wrong Audience into the fixture first, then sign that fixture with the trusted test key.

</details>

---

# Part 40: Explain the complete acceptance decision

Use your successful live transaction and one automated negative test.

Your explanation should identify:

1. the value AcmeHR expected
2. where that expected value came from
3. the value the Response or Assertion carried
4. whether the values matched
5. the provider result
6. whether an authenticated session was created

Example structure:

> AcmeHR expected the Audience `urn:acme:training:sp` because that is its configured SP entity ID. The negative fixture carried a different Audience while keeping the trusted signature and other validation fields correct. Spring Security rejected the Assertion, so no authenticated result was created. Restoring the expected Audience made the positive control pass.

Use the exact values from your test rather than memorizing the example.

---

# Day 6 lab completion check

You are finished with Day 6 only when you can prove all of these:

```text
[ ] I started from a working live Okta login

[ ] I kept the AuthnRequest and Response from the same transaction

[ ] I recorded AcmeHR's expected IdP issuer

[ ] I matched the Response and Assertion issuers to the configured IdP

[ ] I matched Audience to urn:acme:training:sp

[ ] I matched Response Destination to the actual ACS POST URL

[ ] I matched bearer Recipient to the configured ACS

[ ] I matched both InResponseTo locations to the current AuthnRequest ID

[ ] I recorded Conditions NotBefore and NotOnOrAfter

[ ] I recorded SubjectConfirmationData NotOnOrAfter separately

[ ] I used IssueInstant as issuance evidence, not as expiry

[ ] I proved that the live transaction created an AcmeHR session

[ ] I ran the known-good signed Response test first

[ ] I proved issuer rejection

[ ] I proved Audience rejection with a correctly signed fixture

[ ] I proved Destination rejection

[ ] I proved Recipient rejection separately

[ ] I proved Response and confirmation correlation failures separately

[ ] I proved a not-yet-valid Conditions failure

[ ] I proved an expired Conditions failure

[ ] I proved an expired bearer-confirmation failure

[ ] I proved that unsigned content remains unauthenticated

[ ] I recorded expected value, received value, error category, and result for every case

[ ] I ran the complete Maven verification successfully through the Docker test runner

[ ] I can explain why BUILD SUCCESS on a negative test means the invalid message was rejected
```

Do not finish with only a list of passing test names.

Finish when you can explain the comparison that each test proved.

---

# Official references

The lab checks follow the SAML browser SSO profile and the Spring Security provider used by AcmeHR:

- OASIS, Assertions and Protocols for SAML 2.0:  
  https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf

- OASIS, Profiles for SAML 2.0:  
  https://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf

- OASIS, SAML 2.0 Errata 05:  
  https://docs.oasis-open.org/security/saml/v2.0/sstc-saml-approved-errata-2.0.html

- Spring Security, Authenticating SAML Responses:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html

- Spring Security 7.1.1, SAML error codes:  
  https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/saml2/core/Saml2ErrorCodes.html
