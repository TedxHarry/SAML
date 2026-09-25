# Practical SAML 2.0 for Okta Implementation Engineers

A beginner-first, hands-on engineering course for learning SAML 2.0 through real Okta implementation, integration, support, operations, and troubleshooting work.

The goal is not protocol-history memorization. The goal is to build the understanding and practical reasoning needed to receive a new SAML application requirement, design the integration, configure Okta, work with the application team, test the flow, troubleshoot failures from evidence, manage certificate changes, and produce a production-ready handoff.

## Who this course is for

This course is designed for a learner who is new to SAML and wants to become capable of working as an:

- Okta implementation engineer
- IAM integration engineer
- application integration engineer
- support and troubleshooting engineer
- automation and integration practitioner

No previous SAML knowledge is assumed.

## What the learner should be able to do at the end

After completing the lessons and labs, the learner should be able to:

- identify the Identity Provider, Service Provider, browser, user, and trust relationship
- distinguish SP-initiated and IdP-initiated SSO
- distinguish Entity ID, ACS URL, and IdP SSO URL
- configure a SAML application in Okta from a real application requirement
- exchange and interpret IdP and SP metadata
- read an AuthnRequest
- read a SAML Response and Assertion
- understand NameID and how Okta Application username affects its value
- configure user and group claims
- understand Audience, Destination, Recipient, InResponseTo, and time conditions
- distinguish Assertion signing, Response signing, AuthnRequest signing, and assertion encryption
- identify certificate ownership and private-key responsibility
- plan and execute certificate rollover safely
- separate Okta assignment, authentication policy, MFA, and SP-side SAML validation
- understand JIT and why it is different from SCIM provisioning
- distinguish the Okta session from the application session
- understand RelayState, logout, and SAML Single Logout
- keep test and production trust configurations separate
- troubleshoot by following evidence instead of guessing
- produce a production-quality SAML implementation handoff

## The engineering method used throughout the course

Every important topic follows the same pattern:

1. Start with a realistic requirement.
2. Explain the problem in plain language.
3. Introduce only the concepts needed for that problem.
4. Show who communicates with whom.
5. Show what happens in the browser.
6. Inspect the actual SAML message when appropriate.
7. Map the protocol fields to Okta configuration.
8. Run a successful transaction.
9. Break one realistic thing deliberately.
10. Gather browser, SAML, Okta, and SP evidence.
11. Find the first failed step.
12. Fix only that layer.
13. Repeat the transaction.
14. Prove why the fix worked.
15. Explain the root cause in plain language.

The troubleshooting question used across the entire course is:

> **What is the last step I can prove succeeded?**

This keeps troubleshooting evidence-driven.

## Continuous project scenario

The course uses one continuous project rather than unrelated examples.

**Scenario:** Acme is onboarding a SaaS HR application. Okta is the workforce Identity Provider. Employees must sign in to the application through SAML. The application requires identity attributes and groups, supports test and production environments, and introduces additional requirements as the course progresses.

The same company, application, users, environments, certificates, and troubleshooting story continue through the course.

## Continuous lab architecture

The learner will work with the same basic architecture throughout the course:

```text
Acme employee
     |
     v
  Browser
     |
     | AuthnRequest
     v
Local Training SP
     |
     | Browser redirect
     v
  Okta IdP
     |
     | Authentication and policy
     v
  Browser
     |
     | SAMLResponse
     v
   SP ACS
     |
     | SAML validation
     v
Application session
```

The local Service Provider will use a maintained SAML library rather than hand-written XML signature validation.

The final library will be selected and pinned only after checking its current maintenance state, security advisories, supported SAML validations, and compatibility with the lab requirements. The SP harness will be Dockerized so the learner can focus on SAML rather than native dependency setup.

---

# 15-Day Course Path

## Day 1: SAML Foundations

**Question:** What problem does SAML solve, and who does what?

Topics:

- authentication and federation
- user and browser
- Identity Provider (IdP)
- Service Provider (SP)
- trust relationship
- SAML at a high level
- SAML versus OIDC and OAuth at a practical level
- authentication versus provisioning
- introduction to the Acme project

Hands-on focus:

- identify IdP and SP from requirements
- draw the transaction
- classify requirements as authentication, authorization, or provisioning concerns

---

## Day 2: HTTP and Browser Foundations

**Question:** How does the browser actually carry SAML messages?

Topics:

- HTTP request and response
- redirects
- GET and POST
- form POST
- query parameters
- URL encoding
- browser cookies and sessions
- HTTP status codes
- front-channel communication
- Base64
- DEFLATE
- HTTP-Redirect binding
- HTTP-POST binding
- introduction to browser tracing

Hands-on focus:

- inspect browser Network activity
- follow redirects
- inspect POST requests
- locally decode sample SAMLRequest and SAMLResponse values
- understand why Base64 is not encryption

---

## Day 3: First Working Okta SAML SSO

**Question:** Can we make one real SAML login work end to end?

Topics:

- creating a minimal Okta SAML application
- local training SP
- SP Entity ID
- Okta Audience URI
- ACS URL
- Okta Single sign-on URL
- metadata exchange
- Redirect and POST bindings
- application assignment

Hands-on focus:

- configure Okta and the local SP
- run the first successful SP-initiated SSO
- observe the complete transaction without dissecting every SAML field yet

This is the first working flow. The learner must be able to produce a real transaction before later lessons ask them to dissect it.

---

## Day 4: Understanding the AuthnRequest

**Question:** What exactly does the SP ask Okta to do?

Topics:

- AuthnRequest
- ID
- Version
- IssueInstant
- Issuer
- Destination
- AssertionConsumerServiceURL
- ProtocolBinding
- NameIDPolicy
- RequestedAuthnContext where relevant
- RelayState introduction

Hands-on focus:

- capture the AuthnRequest from the working Day 3 flow
- decode it locally
- map important XML values back to SP and Okta configuration
- determine which component created each value

---

## Day 5: SAML Response, Assertion, Subject, and NameID

**Question:** What does Okta return after authentication?

Topics:

- SAML Response
- Assertion
- Response ID
- Issuer
- Status
- Subject
- NameID
- NameID format
- Okta Application username
- SubjectConfirmation
- AuthnStatement
- AttributeStatement introduction

Hands-on focus:

- capture and decode the real Day 3 SAMLResponse
- distinguish Response from Assertion
- identify NameID
- map the NameID value to Okta Application username
- identify which values came from Okta configuration

---

## Day 6: Why the SP Accepts or Rejects an Assertion

**Question:** What does the SP validate before trusting the login?

Topics:

- trusted issuer
- AudienceRestriction
- SP Entity ID relationship
- Destination
- Recipient
- InResponseTo
- request correlation
- NotBefore
- NotOnOrAfter
- IssueInstant
- clock synchronization
- reasonable clock skew

Hands-on focus:

- inspect validation evidence from the working transaction
- deliberately break Audience or endpoint configuration
- investigate expired or not-yet-valid assertions
- prove exactly which validation failed

---

## Day 7: Claims, Attributes, Groups, and Application Authorization

**Question:** How does identity data reach the application?

Topics:

- NameID versus attributes
- custom claims
- attribute name and value
- missing and empty attributes
- case sensitivity
- single-valued and multi-valued attributes
- current Okta custom-claims experience
- Okta Expression Language for claims
- group claims
- `user.getGroups(...)` concepts
- legacy Attribute Statements
- legacy Group Attribute Statements
- application-side authorization

Hands-on focus:

- send email, first name, last name, employee ID, department, and groups
- troubleshoot a wrong attribute name
- troubleshoot a missing required value
- troubleshoot an incorrect group filter

---

## Day 8: Signing and IdP Certificates

**Question:** How does the SP verify that the SAML message really came from the trusted IdP and was not changed?

Topics:

- authenticity
- integrity
- XML signatures at an implementation level
- Assertion signing
- Response signing
- Response and Assertion signing
- Okta's signing behavior
- IdP signing certificate
- public certificate
- private signing key
- HTTPS versus SAML message validation

Hands-on focus:

- identify the signed object
- inspect the signing certificate
- understand which side owns the private key
- break the SP's trusted certificate or signing expectation
- diagnose signature validation failure

---

## Day 9: Signed AuthnRequests, Encryption, and SP Certificates

**Question:** What changes when the SP also owns signing or decryption keys?

Topics:

- signed AuthnRequest
- SP request-signing certificate
- Okta validation of signed requests
- assertion encryption
- SP encryption certificate
- SP decryption private key
- signing versus encryption
- trust direction
- certificate ownership

Hands-on focus:

- enable signed AuthnRequests
- investigate a wrong request-signing certificate
- enable encrypted assertions
- diagnose an SP decryption failure
- build a certificate ownership table

---

## Day 10: Okta Access, Authentication Policy, MFA, JIT, and SCIM Boundary

**Question:** Which failures are actually SAML failures, and which belong to another Okta or lifecycle layer?

Topics:

- individual assignment
- group assignment
- application access
- Global Session Policy
- app authentication policy
- MFA
- authentication context
- JIT provisioning
- account matching
- first-login account creation
- SAML authentication versus lifecycle provisioning
- JIT versus SCIM

Hands-on focus:

- diagnose an unassigned-user failure
- diagnose unexpected MFA without changing SAML claims
- diagnose successful authentication followed by failed JIT
- determine when SCIM is the correct solution instead of SAML

---

## Day 11: SP-Initiated, IdP-Initiated, RelayState, Sessions, and Logout

**Question:** Why can one initiation flow work while another fails?

Topics:

- SP-initiated SSO
- IdP-initiated SSO
- unsolicited SAMLResponse
- request correlation differences
- RelayState
- Default Relay State
- Okta session
- SP application session
- browser cookies
- local logout
- SAML Single Logout
- LogoutRequest
- LogoutResponse
- SessionIndex

Hands-on focus:

- compare SP-initiated and IdP-initiated transactions
- troubleshoot one flow working while the other fails
- troubleshoot a RelayState destination problem
- observe Okta and SP sessions separately
- run a controlled SLO exercise where supported by the lab

---

## Day 12: Certificate Rollover and Production Change

**Question:** How do we change trust in production without causing an outage?

Topics:

- certificate expiration
- certificate ownership
- next signing certificate
- metadata update
- trust overlap
- vendor coordination
- test and production separation
- rollback planning
- emergency expiration response

Hands-on focus:

- simulate certificate A currently trusted
- introduce certificate B
- exchange new metadata or public certificate
- test overlap
- activate the new signing certificate
- prove the new trust path works
- retire certificate A only after validation

---

## Day 13: Troubleshooting Like an Engineer

**Question:** How do we locate a SAML failure without guessing?

Core method:

> **What is the last step I can prove succeeded?**

Evidence sources:

1. browser evidence
2. SAML message evidence
3. Okta and SP logs

Transaction checkpoints:

```text
SP generated AuthnRequest
        |
Browser reached Okta
        |
Okta authenticated user
        |
Okta generated SAMLResponse
        |
Browser posted SAMLResponse to ACS
        |
SP validated signature
        |
SP validated issuer and audience
        |
SP validated endpoints and time
        |
SP processed identity data
        |
SP created application session
```

Hands-on focus:

For every incident, document:

```text
Observed symptom:
Last confirmed successful step:
First failed step:
Evidence:
Root cause:
Single change:
Proof after change:
```

---

## Day 14: Blind Production Incidents

**Question:** Can the learner diagnose a problem without being told which setting is wrong?

Incident types include:

- wrong ACS
- wrong Entity ID or Audience
- wrong Destination
- wrong Recipient
- wrong NameID
- missing attribute
- wrong group claim
- user not assigned
- unexpected MFA
- assertion expired
- assertion not yet valid
- server clock problem
- wrong signing certificate
- old certificate still trusted
- Response versus Assertion signing mismatch
- signed AuthnRequest failure
- encryption/decryption failure
- RelayState problem
- SP-initiated works but IdP-initiated fails
- IdP-initiated works but SP-initiated fails
- JIT failure
- SAML validated but application session was not created
- test works but production fails
- test metadata or certificates used in production
- SLO unsupported or misconfigured

The learner receives symptoms and evidence, not the answer.

---

## Day 15: End-to-End Capstone

**Question:** Can the learner run a real SAML onboarding project independently?

Starting requirement:

Acme is onboarding a new HR SaaS application. Okta is the workforce Identity Provider. The vendor provides test and production SP metadata. Employees use SP-initiated SSO. The application requires identity attributes and group information. The vendor supports JIT, optional assertion encryption, and Single Logout. Security requires signed assertions. A production certificate change is also planned.

The learner independently produces:

- discovery questions
- assumptions
- IdP/SP classification
- transaction design
- metadata mapping
- Entity ID and ACS mapping
- NameID design
- attribute and group-claim design
- signing design
- encryption decision
- certificate ownership table
- assignment approach
- authentication-policy considerations
- JIT and SCIM decision
- session and logout expectations
- test and production matrix
- certificate-rollover plan
- acceptance tests
- troubleshooting evidence
- operations handoff

The reference implementation is shown only after the learner attempts the design.

---

# Topics intentionally deferred to the advanced course

The following subjects are valuable, but they should not slow down a fresher who is learning the common engineering path:

- deeper XML Signature internals
- canonicalization internals
- XML Signature Wrapping
- NameID canonicalization and comment-truncation attack classes
- Artifact binding
- ECP where relevant
- IdP discovery
- multiple IdPs
- federation brokers and hubs
- Okta inbound federation
- advanced AuthnContext
- ForceAuthn
- IsPassive
- deeper persistent and transient NameID design
- complex attribute namespaces
- advanced SLO
- metadata automation
- certificate automation
- multi-tenant SP architecture

These will live under `advanced/` after the core course is complete.

---

# Planned repository structure

The course will be built incrementally. Directories and files will be added only when their content is ready.

Teaching diagrams that explain a lesson concept should live directly inside that lesson, next to the explanation they support. The `diagrams/` folder is reserved only for shared reference diagrams that are reused across multiple lessons, such as an end-to-end transaction map, certificate ownership map, troubleshooting decision tree, or certificate rollover reference.

```text
SAML/
|
├── README.md
|
├── lessons/
|   ├── README.md
|   ├── day-01-saml-foundations.md
|   ├── day-02-http-browser-foundations.md
|   ├── day-03-first-working-sso.md
|   ├── day-04-authnrequest.md
|   ├── day-05-response-assertion-nameid.md
|   ├── day-06-saml-validation.md
|   ├── day-07-claims-attributes-groups-authorization.md
|   ├── day-08-signing-certificates.md
|   ├── day-09-request-signing-encryption.md
|   ├── day-10-okta-access-jit-scim.md
|   ├── day-11-flows-sessions-logout.md
|   ├── day-12-certificate-rollover.md
|   ├── day-13-troubleshooting-methodology.md
|   ├── day-14-blind-incidents.md
|   └── day-15-capstone.md
|
├── labs/
|   ├── README.md
|   ├── day-01/
|   ├── day-02/
|   ├── day-03/
|   ├── ...
|   └── day-15/
|
├── lab-sp/
|   ├── README.md
|   ├── library-selection.md
|   ├── pom.xml
|   ├── Dockerfile
|   ├── compose.yml
|   └── src/
|       ├── main/
|       └── test/
|
├── diagrams/
|   └── README.md
|
├── scripts/
|   ├── README.md
|   ├── python/
|   └── powershell/
|
├── troubleshooting/
|   ├── README.md
|   ├── transaction-timeline.md
|   ├── symptom-map.md
|   └── evidence-template.md
|
├── reference/
|   ├── 15-day-plan.md
|   ├── terminology.md
|   ├── saml-message-map.md
|   ├── certificate-ownership.md
|   ├── project-intake-checklist.md
|   └── engineer-confidence-checklist.md
|
├── advanced/
|   └── README.md
|
└── .github/
    └── workflows/
        ├── lab-sp-build.yml
        └── course-quality.yml
```

---

# Build discipline

This repository will **not** be populated with all 15 lessons at once.

The build sequence is:

```text
Course structure
      |
      v
Build one file
      |
      v
Review the file
      |
      v
Pressure-test technical accuracy
      |
      v
Fix problems
      |
      v
Close the file
      |
      v
Only then create the next file
```

For the lesson sequence:

```text
Build Day 1
   |
Review Day 1
   |
Pressure-test Day 1
   |
Correct Day 1
   |
Close Day 1
   |
Only then start Day 2
```

Quality takes priority over speed.

## Quality rules for every lesson

A lesson is not complete until:

- every important term is defined before it is relied upon
- the learner understands the problem before seeing protocol detail
- no lab depends on material taught later
- diagrams accurately show who communicates with whom
- real Okta terminology is used
- the successful path is shown before the failure path
- break-and-fix exercises use realistic failures
- troubleshooting relies on evidence
- the root cause is explained, not merely patched
- temporary lab changes are restored
- private keys, passwords, MFA codes, session cookies, and sensitive production assertions are never exposed
- current Okta behavior is checked against current official documentation when version-sensitive
- the learner ends the day with an **Explain it back** checkpoint

## Core correctness rules

Throughout the course:

- SAML authentication is not provisioning.
- JIT is not SCIM.
- Base64 is not encryption.
- Signing and encryption are different.
- Entity ID is not the ACS URL.
- ACS URL is not the IdP SSO URL.
- NameID is not automatically an email address.
- A successful Okta authentication does not prove that the SP accepted the SAML assertion.
- A valid signature alone does not prove that the assertion is acceptable.
- Audience, issuer, Destination, Recipient, time conditions, and correlation still matter.
- SP-initiated and IdP-initiated SSO are not identical transactions.
- Okta assignment failure, authentication-policy failure, and SP assertion-validation failure are different layers.
- Okta session and SP application session are different states.
- SAML Single Logout is not universally supported.
- HTTPS does not replace SAML message validation.
- SAML signature validation must use a maintained SAML library rather than hand-written XML signature logic.
- Certificate rollover is a coordinated operational change, not simply replacing a certificate file.

---

# Build status

- [x] Course architecture designed
- [x] 15-day progression pressure-tested
- [x] Continuous lab architecture defined
- [x] Advanced topics separated from the beginner core
- [x] Day 1 lesson
- [x] Day 1 lab
- [x] Day 1 teaching diagrams integrated into the lesson
- [x] Day 1 review and pressure test
- [x] Day 1 closed
- [x] Day 2 lesson
- [x] Day 2 lab
- [x] Day 2 teaching diagrams integrated into the lesson
- [x] Day 2 review and pressure test
- [x] Day 2 closed
- [x] Day 3 lesson
- [x] Day 3 training SP stack selected and documented
- [x] Day 3 training SP implementation
- [x] Day 3 automated SAML and security checks
- [x] Day 3 Docker build and startup smoke test
- [x] Day 3 lab
- [x] Day 3 final review and pressure test
- [x] Day 3 course artifact closed
- [x] Day 4 AuthnRequest lesson
- [x] Day 4 AuthnRequest lab
- [x] Day 4 generated AuthnRequest verification
- [x] Day 4 review and pressure test
- [x] Day 4 closed
- [x] Day 5 Response, Assertion, and NameID lesson
- [x] Day 5 Response lab
- [x] Day 5 review and pressure test
- [x] Day 5 closed
- [x] Day 6 SAML validation lesson
- [x] Day 6 validation lab
- [x] Day 6 signed positive and focused negative validation tests
- [x] Day 6 Docker validation-test runner
- [x] Day 6 review and pressure test
- [x] Day 6 closed
- [x] Day 7 claims, attributes, groups, and authorization lesson
- [x] Day 7 claims and authorization lab
- [x] Day 7 current Okta custom-claims UI and Expression Language verification
- [x] Day 7 validated-claim mapper and explicit AcmeHR manager-role allowlist
- [x] Day 7 claims and manager learner views
- [x] Day 7 mapper and MVC authorization tests
- [x] Day 7 final review and pressure test
- [x] Day 7 closed
- [x] Day 8 signing and IdP certificates lesson
- [x] Day 8 signing and certificate lab
- [x] Day 8 current Okta signing controls and SAML Signing Certificates verification
- [x] Day 8 Response, Assertion, and both-signing live lab design
- [x] Day 8 unsigned, tampered-signature, and wrong-trusted-certificate tests
- [x] Day 8 metadata signing-key inspection and synthetic test-key safety review
- [x] Day 8 final review and pressure test
- [x] Day 8 closed
- [x] Day 9 signed AuthnRequests, Assertion encryption, and SP certificates lesson
- [x] Day 9 signed-request and encrypted-Assertion live lab
- [x] Day 9 SP signing and decryption credential configuration
- [x] Day 9 Docker credential-path and read-only mount workflow
- [x] Day 9 Redirect signature, NameIDPolicy, and wrong-verification-certificate tests
- [x] Day 9 SP metadata signing/encryption public-key and private-key-boundary tests
- [x] Day 9 matching and wrong decryption-key tests
- [x] Day 9 current Okta Signed Requests, NameIDPolicy, and Assertion Encryption verification
- [x] Day 9 Spring Security 7.1.1 encrypted-Assertion issue boundary documented
- [x] Day 9 final review and pressure test
- [x] Day 9 closed
- [ ] Day 10 and later content

Days 1 through 9 are now closed as course artifacts. Learners must still complete each live Okta lab and preserve the required evidence before moving to the next day.
