# SAML Library and Runtime Selection

## Decision

**Decision date:** September 22, 2026

The local AcmeHR training Service Provider will use:

```text
Runtime
    Java 21 LTS

Application framework
    Spring Boot 4.1.1

SAML implementation
    Spring Security 7.1.1
    spring-security-saml2-service-provider

Underlying SAML engine
    OpenSAML 5 through Spring Security

Build
    Maven Wrapper

Delivery
    Docker
```

The learner will not need to learn Java to complete the course.

The Java/Spring stack is an implementation detail behind the training application.

The learner experience remains:

```text
docker compose up
        |
        v
Open AcmeHR Training
        |
        v
Run SAML login
        |
        v
Observe evidence
```

---

# Why this decision matters

The training SP is not just a demo page.

Later lessons depend on it to teach:

- secure SAML Response validation
- AuthnRequest generation
- request correlation
- Audience validation
- Destination validation
- Recipient validation
- time validation
- signed AuthnRequests
- encrypted assertions
- IdP-initiated SSO
- Single Logout
- certificate rollover
- evidence-based troubleshooting

That means the library choice must support the whole course safely.

Code simplicity is useful.

Security correctness is more important.

---

# Non-negotiable requirements

The selected implementation must support or allow us to prove all of these:

```text
SP-initiated SSO

IdP metadata consumption

SP metadata generation

SAML Response signature validation

Trusted IdP issuer validation

Audience validation

Destination validation

Recipient validation

Time-condition validation

Request correlation / InResponseTo

AuthnRequest generation

Signed AuthnRequests

Encrypted assertions

IdP-initiated SSO later

SAML Single Logout later

Clear validation failure information

Dockerized operation
```

We must not implement XML signature validation ourselves.

---

# Candidates reviewed

We reviewed four realistic options:

1. Spring Security SAML2 with OpenSAML
2. Node-SAML
3. Samlify
4. PySAML2

The question was not:

> Which library has the shortest example?

The question was:

> Which stack best supports the security and teaching requirements of this course?

---

# Comparison

| Requirement | Spring Security + OpenSAML | Node-SAML | Samlify | PySAML2 |
|---|---|---|---|---|
| Current maintenance | Yes | Yes | Yes | Current releases, but current security/maintenance concerns reviewed |
| SP-initiated SSO | Yes | Yes | Yes | Yes |
| IdP metadata | Yes | Yes | Yes | Yes |
| SP metadata | Yes | Yes | Yes | Yes |
| Signature validation | Yes | Yes | Yes | Yes |
| Issuer validation | Yes | Yes | Yes | Yes |
| Audience validation | Yes | Yes | Extracted, but full enforcement was not sufficiently proven in our review | Yes |
| Destination validation | Yes | Not sufficiently clear for our course contract | Not sufficiently proven in current source review | Yes |
| Recipient validation | Yes through OpenSAML SubjectConfirmation validation | Current open issue requests this validation | Not sufficiently proven in current source review | Yes |
| Request correlation | Yes | Supported when explicitly enabled | Some support visible, but full enforcement was not sufficiently proven | Yes |
| Signed AuthnRequests | Yes | Yes | Yes | Yes |
| Encrypted assertions | Yes | Yes | Yes | Yes |
| SLO | Yes | Yes | Yes | Yes |
| Structured validation errors useful for teaching | Strong | Moderate | Moderate | Moderate |
| Native dependency burden | Low inside container | Low | Low | Higher due to xmlsec tooling |
| Selected | **Yes** | No | No | No |

A blank or cautious result above does not mean the library has no capability.

It means we did not find enough current evidence to make that capability part of this course's security contract.

For a teaching SP, we should never claim:

> "This validation is happening"

unless we can prove it.

---

# Selected stack: Spring Security SAML2 with OpenSAML

Spring Security's SAML2 Service Provider support uses OpenSAML for SAML processing.

This gives us a strong separation:

```text
Spring Security / OpenSAML
    -> performs security-sensitive SAML processing

Our application code
    -> presents the results in a learner-friendly way
```

That is the architecture we want.

Our code should explain what happened.

It should not reinvent the validation.

---

# Why Spring Security fits this course

## 1. Response validation

Current Spring Security SAML2 support validates SAML responses through OpenSAML.

The current validation path covers the areas we need to teach later, including:

- signatures
- issuer
- Audience restrictions
- Subject confirmation
- time conditions
- response correlation

Current Spring Security error handling also exposes validation categories such as invalid signature, Destination, issuer, and InResponseTo.

That is useful for our transaction diagnostics.

---

## 2. Recipient validation

This was a deciding requirement.

OpenSAML's SubjectConfirmation validation includes checks around:

- Recipient
- NotBefore
- NotOnOrAfter
- Address where applicable
- InResponseTo where applicable

This gives us a library-backed path for the Recipient lesson instead of inventing our own check.

We will still write negative tests before the course claims that a particular validation passed or failed.

---

## 3. Audience validation

Spring Security with OpenSAML supports AudienceRestriction validation.

That lets Day 6 deliberately create an expected-Audience mismatch and show the real validation failure.

The training UI can translate that error into plain language.

The trust decision itself remains inside the SAML library.

---

## 4. Destination and request correlation

Spring Security's SAML Response validation supports Destination and request-correlation checks.

For SP-initiated login, we will preserve the request state required to validate the returning response.

We will not disable correlation merely to simplify the lab.

---

## 5. AuthnRequest generation and signing

Spring Security can create SAML AuthnRequests.

It also supports signed AuthnRequests.

That supports:

- Day 3 first SP-initiated login
- Day 4 AuthnRequest inspection
- Day 9 request-signing exercises

The current Spring Security implementation uses modern signing defaults such as RSA-SHA256.

The final lab configuration will still make the chosen signing behavior explicit.

---

## 6. Encrypted assertions

Spring Security/OpenSAML supports decrypting encrypted SAML content when the relying party has the required decryption credentials.

That capability is intended to support the Day 9 encryption lab.

A current open Spring Security issue, #19606, reports a namespace-hoisting problem during encrypted-assertion processing that can cause signature verification failures for some signed encrypted assertions. The report states that the behavior was reproduced with Spring Security 7.1.1 and OpenSAML 5.

This does not affect the unencrypted Day 3 baseline.

Before the Day 9 encryption lab is finalized, recheck the issue and the current Spring Security release. If the issue still affects the selected version, upgrade to a fixed version or redesign the lab. Do not work around it by weakening signature validation.

Issue:
https://github.com/spring-projects/spring-security/issues/19606

The SP private decryption key will remain protected.

The learner UI will only show whether decryption succeeded or failed.

---

## 7. Metadata

Spring Security supports:

- configuring relying parties from IdP metadata
- publishing SP metadata

That fits our Day 3 setup:

```text
Okta metadata
        |
        v
AcmeHR Training SP

AcmeHR SP metadata
        |
        v
Available for inspection / future configuration use
```

Day 3 will still teach the ACS URL and Entity ID explicitly rather than hiding them behind metadata import.

---

## 8. Single Logout

Spring Security currently supports SAML 2.0 logout flows.

That keeps Day 11 possible without replacing the SP implementation.

SLO will remain disabled from the beginner workflow until that lesson.

---

## 9. Useful failure structure

A course needs more than a Boolean:

```text
login failed
```

We need enough validated information to say:

```text
Signature validation: PASS

Issuer validation: PASS

Audience validation: FAIL
```

Spring Security/OpenSAML provides enough validation structure for us to build this progressively.

The application must only display a specific failure when the underlying library result supports it.

---

# Why Node-SAML was not selected

Node-SAML is actively maintained and has many useful SP capabilities.

It supports areas such as:

- AuthnRequest creation
- Response validation
- metadata
- signed requests
- encrypted assertions
- Audience validation
- time validation
- optional InResponseTo validation
- SLO

It was therefore a serious candidate.

However, a current open Node-SAML issue requests validation of the SAML SubjectConfirmationData **Recipient** against the callback URL.

Recipient validation is a required course topic and a required security behavior in our training SP contract.

That makes Node-SAML a poor baseline for this course today.

We do not want to add our own security-sensitive Recipient validator around a library simply to satisfy the lesson.

There is another configuration concern worth noting.

Node-SAML currently documents request-signature defaults using SHA-1 unless the application explicitly selects stronger algorithms.

That can be configured correctly, but the Recipient gap is the stronger reason for rejection.

Node-SAML also had important SAML signature-verification advisories in 2025 that were fixed in version 5.1.0.

Those fixed advisories do not by themselves disqualify the current release.

They reinforce why we must pin and review security-sensitive dependencies carefully.

---

# Why Samlify was not selected

Samlify is actively maintained and had a 2026 release.

It supports many capabilities we need, including SAML SP operation, metadata, signing, and encryption.

A 2026 security issue involving XML injection was fixed in version 2.13.0.

We inspected the current source rather than assuming feature coverage from marketing-level documentation.

The current code clearly exposes areas such as:

- signatures
- issuer handling
- expiration/time handling
- extracted Audience
- extracted Destination
- extracted InResponseTo

However, from the current source we reviewed, we could not sufficiently prove built-in enforcement for the complete set of:

- Audience
- Destination
- Recipient
- InResponseTo / correlation

at the level required by our training SP contract.

That does **not** mean Samlify cannot be used securely.

It means we do not have enough current evidence to make those validation claims part of this course.

For a security teaching environment, uncertainty is enough reason not to select it.

---

# Why PySAML2 was not selected

PySAML2 is mature and had a recent 7.5.5 release in September 2026.

It supports the broad SAML functionality required by the course.

However, during this review there was a current open security issue concerning a possible XSLT-based denial-of-service path, along with recent community discussion about maintenance.

PySAML2 also relies on external XML security tooling, which adds native dependency complexity.

Docker could hide that complexity from the learner, but there is no reason to choose that extra operational burden while current security questions remain open.

For this course baseline, it is not selected.

This decision can be revisited in the future if the project state changes.

---

# Runtime choice

## Java 21 LTS

The training SP will target Java 21 LTS.

Reasons:

- long-term-support runtime
- comfortably satisfies the Java level required by current Spring Security
- mature container ecosystem
- stable foundation for the duration of the course

The exact JDK container image and digest will be selected and pinned when the Dockerfile is created.

Do not use an unpinned floating production image such as:

```text
latest
```

for the finalized course environment.

---

# Spring versions

Initial implementation baseline:

```text
Spring Boot
4.1.1

Spring Security
7.1.1

SAML module
spring-security-saml2-service-provider
```

Spring Boot 4.1.1 currently manages the Spring Security 7.1.1 dependency line.

We should use Spring Boot dependency management rather than manually mixing arbitrary Spring component versions.

---

# OpenSAML usage rule

We will use OpenSAML through Spring Security's supported SAML integration.

The application may customize supported Spring/OpenSAML validation behavior when a lesson requires it.

The application must not replace the library's security processing with our own XML security implementation.

Do not write custom code for:

- XML Signature canonicalization
- signature-reference validation
- XML Signature verification
- XML encryption internals
- trust decisions based on raw XML string matching

---

# Security configuration rules

The implementation must follow these rules.

## Keep default security validation enabled

Do not disable a validator merely because Day 3 has not taught it yet.

The Day 3 UI can summarize:

```text
Secure SAML validation: PASS
```

Later lessons can expose the individual checks.

---

## Preserve request correlation

For SP-initiated SSO, the application must preserve the request information Spring Security needs for correlation.

Do not switch off InResponseTo checking simply to avoid maintaining request state.

---

## Create the application session only after successful authentication

The local AcmeHR application session must be created only after Spring Security has accepted the SAML authentication.

This separation must remain true:

```text
Okta authentication
        !=
AcmeHR application session
```

---

## Diagnostics must come from real validation results

The teaching UI may translate:

```text
INVALID_DESTINATION
```

into:

```text
The SAML Response was sent with a Destination
that AcmeHR does not accept.
```

But it must not invent a specific diagnosis when the library did not provide evidence for it.

---

## Never trust unvalidated XML

Raw decoded XML is evidence for learning.

It is not a replacement for library validation.

Application access decisions must use the authenticated result produced by Spring Security/OpenSAML.

---

# Capability map to the course

## Day 3

Need:

- SP-initiated SSO
- ACS processing
- IdP metadata
- SP metadata
- secure Response processing
- application session

Spring Security supports these.

---

## Day 4

Need:

- AuthnRequest generation
- ability to capture and display the request

Spring Security supports AuthnRequest creation.

We will expose a safe copy of the latest training request for learning.

---

## Day 5

Need:

- SAML Response / Assertion access
- NameID and attribute visibility

Spring Security authentication objects and OpenSAML processing give us a supported path to these values.

---

## Day 6

Need to teach:

- issuer
- Audience
- Destination
- Recipient
- InResponseTo
- time conditions

The underlying validators support these areas.

Before the lesson is finalized, each failure must be proven with a negative automated fixture.

---

## Day 8

Need:

- IdP signing certificate
- signature validation
- Response versus Assertion signing behavior

Spring Security/OpenSAML provides the validation layer.

The learner UI will expose the relevant result without reimplementing XML Signature.

---

## Day 9

Need:

- signed AuthnRequests
- SP signing key
- encrypted assertions
- SP decryption key

The selected stack supports request signing and decryption credentials.

---

## Day 11

Need:

- SP-initiated login
- IdP-initiated login
- sessions
- SAML Single Logout

The selected stack supports the required building blocks.

The exact Okta SLO behavior will be verified against current Okta documentation when Day 11 is built.

---

## Day 12

Need:

- certificate rollover
- multiple or changing trust credentials
- controlled metadata/certificate refresh

The relying-party configuration can be designed to support controlled trust updates.

We will make the change observable rather than silently refreshing trust in the background.

---

# Negative tests required before we trust the lab

Before the training SP is considered ready, we must prove that the selected stack rejects controlled bad transactions.

Required tests include:

```text
Wrong issuer
    -> rejected

Wrong Audience
    -> rejected

Wrong Destination
    -> rejected

Wrong Recipient
    -> rejected

Wrong InResponseTo / correlation
    -> rejected where applicable

Expired assertion
    -> rejected

Not-yet-valid assertion
    -> rejected

Invalid signature
    -> rejected

Untrusted signing certificate
    -> rejected

Malformed SAML Response
    -> rejected

Encrypted assertion with correct SP key
    -> accepted when encryption lesson enables it

Encrypted assertion with wrong SP key
    -> rejected

Signed AuthnRequest
    -> produced and accepted in the signing lab
```

A course diagnostic cannot say:

```text
Recipient validation: PASS
```

until the corresponding negative test proves that an incorrect Recipient is rejected.

---

# Endpoint design note

Spring Security has standard SAML endpoints.

Our course uses learner-friendly URLs such as:

```text
http://localhost:8000/saml/acs
```

During implementation, we may customize Spring Security's processing URLs so the lab matches the course terminology.

Any customization must use supported Spring Security configuration.

We must not bypass the framework's SAML processing just to obtain a prettier URL.

---

# Build approach

The implementation will use Maven Wrapper.

The repository should eventually contain:

```text
mvnw
mvnw.cmd
.mvn/
pom.xml
```

The wrapper lets the Docker build and contributors use a known Maven version without requiring a manually installed Maven runtime.

The exact Maven Wrapper version will be selected when the implementation begins.

---

# Docker approach

Use a multi-stage Docker build.

Conceptually:

```text
Build stage
    -> compile the Spring Boot application

Runtime stage
    -> Java 21 runtime
    -> copy only required application artifacts
    -> run as a non-root user where practical
```

The final image should:

- expose only the required training port
- use explicit configuration
- avoid unnecessary packages
- avoid embedding production secrets
- have a health check
- use pinned image versions/digests in the finalized course

The exact Dockerfile belongs to the next implementation step.

---

# Dependency review rule

This decision is not permanent merely because it is written here.

Before finalizing the Day 3 lab and before major course releases, recheck:

- Spring Boot release status
- Spring Security release status
- OpenSAML advisories
- transitive dependency advisories
- JDK container advisories
- any security change affecting SAML validation behavior
- Spring Security issue #19606 before enabling the Day 9 encrypted-assertion lab

If a critical issue appears, stop and reassess before telling learners to use the environment.

---

# Decision summary

We are selecting:

> **Spring Boot 4.1.1 + Spring Security 7.1.1 SAML2 Service Provider + OpenSAML 5 on Java 21 LTS**

because it gives the course the strongest verified path for:

- secure validation
- Recipient and SubjectConfirmation checks
- Audience
- Destination
- correlation
- signing
- encryption
- metadata
- SLO
- structured failure evidence
- future course growth

The implementation is more substantial than a tiny Node or Python demo.

Docker hides that complexity from the learner.

For this course, that is the right tradeoff.

---

# Sources checked for this decision

## Spring Security and Spring Boot

- Spring Security SAML2 Service Provider overview:
  https://docs.spring.io/spring-security/reference/servlet/saml2/index.html
- Spring Security SAML2 authentication and Response validation:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html
- Spring Security AuthnRequest configuration:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication-requests.html
- Spring Security SAML2 logout:
  https://docs.spring.io/spring-security/reference/servlet/saml2/logout.html
- Spring Boot dependency versions:
  https://docs.spring.io/spring-boot/4.1/appendix/dependency-versions/coordinates.html

## OpenSAML

- OpenSAML SubjectConfirmation validation API documentation:
  https://shibboleth.net/api/java-opensaml/5.2.1/org/opensaml/saml/saml2/assertion/impl/BearerSubjectConfirmationValidator.html

## Node-SAML

- Node-SAML project:
  https://github.com/node-saml/node-saml
- Current Recipient validation request:
  https://github.com/node-saml/node-saml/issues/405
- GitHub advisory GHSA-4mxg-3p6v-xgq3:
  https://github.com/advisories/GHSA-4mxg-3p6v-xgq3
- GitHub advisory GHSA-m837-g268-mmv7:
  https://github.com/advisories/GHSA-m837-g268-mmv7

## Samlify

- Samlify project:
  https://github.com/tngan/samlify
- Samlify releases:
  https://github.com/tngan/samlify/releases
- GitHub advisory GHSA-34r5-q4jw-r36m:
  https://github.com/advisories/GHSA-34r5-q4jw-r36m

## PySAML2

- PySAML2 project:
  https://github.com/IdentityPython/pysaml2
- PySAML2 package releases:
  https://pypi.org/project/pysaml2/
- Current project issues were reviewed during selection:
  https://github.com/IdentityPython/pysaml2/issues
