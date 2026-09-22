# Day 6: Why the SP Accepts or Rejects an Assertion

## What you should understand by the end of today

On Day 5, we decoded the SAML Response that Okta returned to AcmeHR.

Readable XML is not the same as trusted XML.

Today we answer the next question:

> What must AcmeHR validate before it trusts the login?

By the end of Day 6, you should be able to explain:

- why a successful Okta authentication does not force the SP to accept the Response
- how configured trust data gives the SP its expected values
- how a signature protects the message and connects it to a trusted IdP key
- why `Issuer` must identify the configured IdP
- why `Audience` must include the AcmeHR SP entity ID
- how `Destination` and `Recipient` both involve the ACS but protect different objects
- how `InResponseTo` connects a Response to the AuthnRequest that started the login
- how `NotBefore` and `NotOnOrAfter` limit when an Assertion can be used
- why `IssueInstant` is not an expiration time
- how clock synchronization and clock skew affect time checks
- what the current Spring Security baseline validates by default
- why replay protection is broader than checking timestamps alone
- how to isolate one failed validation instead of guessing

Today is about the SP trust decision.

Day 8 will go deeper into XML signature mechanics and certificates. Day 9 will cover SP signing and encryption keys.

---

# 1. Continue the same transaction

Our requirement is still:

> Priya opens the protected AcmeHR page and signs in through Okta.

Day 5 ended when the browser posted a SAML Response to the AcmeHR ACS.

The application session does not exist yet at that point.

```text
Browser POSTs SAMLResponse
        |
        v
AcmeHR decodes the XML
        |
        v
AcmeHR validates the Response and Assertion
        |
        +---- validation fails ----> reject login
        |
        +---- validation passes ---> create authenticated session
```

The browser is only the carrier. It is not a trusted source of identity.

AcmeHR trusts the result only after its SAML processor accepts the message against the configured IdP and SP values.

---

# 2. Authentication at Okta and acceptance at AcmeHR are separate results

Okta can successfully authenticate Priya and still produce a Response that AcmeHR rejects.

Examples include:

- the Response names a different issuer
- the Assertion is meant for another SP
- the Response points to a different ACS
- the Response does not match the saved AuthnRequest
- the Assertion is too early or expired
- the signature is missing or cannot be verified

The Response status can still say `Success` in some of these cases.

That status reports Okta's protocol result. It does not order AcmeHR to create a session.

AcmeHR owns the final trust decision for AcmeHR.

---

# 3. Validation compares received facts with expected facts

The SP needs two sets of information:

1. **Received facts** from the SAML Response and Assertion.
2. **Expected facts** from AcmeHR configuration, Okta metadata, the saved AuthnRequest, the current request URL, and the SP clock.

The comparison looks like this:

| Received value | Expected value comes from |
| --- | --- |
| Response or Assertion signature | configured IdP verification certificate |
| Response `Issuer` | IdP entity ID in Okta metadata |
| Assertion `Audience` | AcmeHR SP entity ID |
| Response `Destination` | URL where AcmeHR received the Response |
| Subject confirmation `Recipient` | configured AcmeHR ACS URL |
| Response `InResponseTo` | saved AuthnRequest ID |
| Subject confirmation `InResponseTo` | saved AuthnRequest ID |
| `NotBefore` and `NotOnOrAfter` | current SP time, with permitted clock skew |

Validation is not a search for values that look familiar.

It is a set of exact security comparisons.

---

# 4. The AcmeHR values for this course

The current application defines these SP values:

| Purpose | AcmeHR value |
| --- | --- |
| Registration ID inside Spring | `acmehr` |
| SP entity ID | `urn:acme:training:sp` |
| ACS path | `/saml/acs` |
| Local ACS URL | `http://localhost:8000/saml/acs` |

The IdP entity ID and verification certificate come from the metadata location configured through:

```text
IDP_METADATA_URL
```

For a real Okta app, the IdP entity ID often looks similar to:

```text
http://www.okta.com/exk...
```

Do not copy that shortened example. Read the exact value from your Okta metadata and captured Response.

The test metadata uses a separate training-only issuer:

```text
https://idp.acme.test
```

Keep live Okta evidence separate from test-fixture evidence.

---

# 5. A shortened Response with the validation fields

The following XML leaves out signature details so we can focus on the comparisons.

```xml
<saml2p:Response
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    Destination="http://localhost:8000/saml/acs"
    ID="id-response-222"
    InResponseTo="ARQ-111"
    IssueInstant="2026-09-22T08:10:00Z"
    Version="2.0">

    <saml2:Issuer>http://www.okta.com/exk...</saml2:Issuer>

    <saml2p:Status>
        <saml2p:StatusCode
            Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
    </saml2p:Status>

    <saml2:Assertion
        ID="id-assertion-333"
        IssueInstant="2026-09-22T08:10:00Z"
        Version="2.0">

        <saml2:Issuer>http://www.okta.com/exk...</saml2:Issuer>

        <saml2:Subject>
            <saml2:NameID>priya@example.com</saml2:NameID>
            <saml2:SubjectConfirmation
                Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                <saml2:SubjectConfirmationData
                    InResponseTo="ARQ-111"
                    NotOnOrAfter="2026-09-22T08:15:00Z"
                    Recipient="http://localhost:8000/saml/acs"/>
            </saml2:SubjectConfirmation>
        </saml2:Subject>

        <saml2:Conditions
            NotBefore="2026-09-22T08:09:00Z"
            NotOnOrAfter="2026-09-22T08:15:00Z">
            <saml2:AudienceRestriction>
                <saml2:Audience>urn:acme:training:sp</saml2:Audience>
            </saml2:AudienceRestriction>
        </saml2:Conditions>
    </saml2:Assertion>
</saml2p:Response>
```

This example is not trusted merely because the values look correct.

The actual message also needs valid signature protection. We will treat that as a required gate today and study how it works on Day 8.

---

# 6. Think in validation groups

The checks are easier to investigate when grouped by the question they answer.

| Group | Question |
| --- | --- |
| Cryptographic trust | Was protected SAML content signed with a key AcmeHR trusts, and is it unchanged? |
| Issuer | Did the configured IdP issue it? |
| Audience | Was this Assertion made for AcmeHR? |
| Endpoint | Did it arrive at the intended AcmeHR ACS? |
| Correlation | Is it answering the AuthnRequest that AcmeHR saved? |
| Time | Is it valid for use now? |
| Protocol and structure | Does it report success and contain usable SAML content? |

These are conceptual groups, not a promise that every SAML library runs every check in this exact order.

The order of processing and the error surfaced first can vary by implementation and configuration.

---

# 7. Signature validation is a required trust gate

The SAML Response passes through the browser.

Anything delivered by a browser must be treated as untrusted input.

A valid XML signature lets AcmeHR check two things:

- the signed content has not changed since it was signed
- the signature can be verified with an IdP verification key that AcmeHR is configured to trust

Depending on the IdP and SP configuration, the signature can protect the Response, the Assertion, or both.

Do not reduce this rule to:

> The outer Response must always be signed.

Spring Security can accept a signed Response without requiring a separate Assertion signature. It can also verify a signed Assertion when the outer Response is not signed. The exact acceptable arrangement belongs to the provider behavior and trust configuration.

What must not happen is accepting identity statements that have no acceptable signature protection.

The current repository already contains a negative test named:

```text
unsignedSamlResponseDoesNotCreateAuthenticatedSession
```

It proves that a plausible-looking unsigned message does not create an authenticated session.

Day 8 will inspect signed objects and certificates in detail.

---

# 8. Issuer answers who created the SAML content

The Response contains an `Issuer`:

```xml
<saml2:Issuer>http://www.okta.com/exk...</saml2:Issuer>
```

The Assertion also contains an `Issuer`.

For this browser SSO flow, both identify the Okta IdP that issued the content.

AcmeHR's configured IdP entity ID comes from Okta metadata. Spring compares the received issuer with that configured entity ID.

```text
Received Response Issuer
        must equal
Configured Okta IdP entity ID
```

The comparison is not based on a display name such as `Okta` or `Acme Okta`.

It uses the exact entity identifier.

Common failure causes include:

- metadata from a different Okta app or environment
- a stale metadata file
- a copied issuer with a missing character
- a Response sent by an IdP that is not registered for this SP configuration

An issuer match is necessary, but it is not enough on its own. An attacker can type an issuer string into XML. Signature validation gives the issuer claim cryptographic meaning.

---

# 9. Audience answers which SP may use the Assertion

The Assertion contains an audience restriction:

```xml
<saml2:AudienceRestriction>
    <saml2:Audience>urn:acme:training:sp</saml2:Audience>
</saml2:AudienceRestriction>
```

For AcmeHR, the expected audience is the configured SP entity ID:

```text
urn:acme:training:sp
```

The relationship is:

```text
Assertion Audience
        must include
AcmeHR SP entity ID
```

Audience does not normally contain the IdP entity ID.

Audience also does not have to be the ACS URL. An SP entity ID is an identifier. An ACS URL is an endpoint.

They happen to be equal in some integrations, but they are deliberately different in this course.

That difference helps us see the two jobs:

```text
SP entity ID
    urn:acme:training:sp
    identifies AcmeHR as a SAML party

ACS URL
    http://localhost:8000/saml/acs
    tells the browser where to POST the Response
```

If Okta sends another application's audience, AcmeHR must reject the Assertion even when the user and issuer are valid.

---

# 10. Destination answers where the Response was sent

`Destination` belongs to the outer Response:

```xml
<saml2p:Response
    Destination="http://localhost:8000/saml/acs">
```

Spring Security compares it with the URL where the Response was received.

For the local course application, the expected URL is:

```text
http://localhost:8000/saml/acs
```

The scheme, host, port, and path matter.

These are different URLs:

```text
http://localhost:8000/saml/acs
https://localhost:8000/saml/acs
http://localhost:8080/saml/acs
http://localhost:8000/login/saml2/sso/acmehr
```

A destination mismatch often means the Okta Single sign-on URL and the SP processing endpoint do not agree.

In a deployed application behind a reverse proxy, it can also mean the application sees an internal URL while the browser uses an external URL. That case requires correct proxy and forwarded-header configuration. Weakening destination validation is not the fix.

---

# 11. Recipient answers where the bearer Assertion may be presented

`Recipient` appears inside bearer `SubjectConfirmationData`:

```xml
<saml2:SubjectConfirmationData
    Recipient="http://localhost:8000/saml/acs"
    NotOnOrAfter="2026-09-22T08:15:00Z"/>
```

For this flow, the expected recipient is also the AcmeHR ACS URL.

```text
SubjectConfirmationData Recipient
        must equal
Configured AcmeHR ACS URL
```

Destination and Recipient often contain the same text, but they belong to different objects:

| Field | Location | Protects the use of |
| --- | --- | --- |
| `Destination` | Response | the protocol Response at an endpoint |
| `Recipient` | Assertion subject confirmation | the bearer Assertion at an ACS |

Do not treat one as a duplicate that can be ignored.

Both comparisons need the SP and IdP endpoint configuration to agree.

---

# 12. Entity ID, Destination, and Recipient are three different jobs

These three values are a frequent source of configuration mistakes.

| Value | AcmeHR example | Job |
| --- | --- | --- |
| SP entity ID | `urn:acme:training:sp` | identifies the SP |
| Response Destination | `http://localhost:8000/saml/acs` | names the Response delivery endpoint |
| Assertion Recipient | `http://localhost:8000/saml/acs` | limits where the bearer Assertion may be presented |

The expected Audience comes from the first value.

The expected Destination and Recipient come from the ACS relationship.

If you place the ACS URL in the Okta Audience URI field while AcmeHR expects `urn:acme:training:sp`, audience validation fails.

If you place the entity ID in the Okta Single sign-on URL field, the browser does not have a usable ACS endpoint.

Field names matter because the values have different purposes.

---

# 13. Response InResponseTo checks request correlation

On Day 4, AcmeHR created an AuthnRequest with an ID:

```xml
<samlp:AuthnRequest ID="ARQ-111">
```

Spring Security saved information about that outgoing request.

The returned Response says:

```xml
<saml2p:Response InResponseTo="ARQ-111">
```

For the normal SP-initiated flow, the comparison is:

```text
Response InResponseTo
        must equal
Saved AuthnRequest ID
```

This proves correlation, not user identity.

It tells AcmeHR that the Response is answering the login request it started.

Common failure causes include:

- the browser lost the request state or session cookie
- the Response returned to a different application instance without shared request state
- an old Response was posted after its original request state disappeared
- the IdP returned the wrong request ID
- a load balancer sent the request and response through incompatible session storage

Do not solve an `InResponseTo` problem by disabling correlation before you understand why the saved request cannot be found.

---

# 14. Subject confirmation can carry InResponseTo too

The bearer subject confirmation can repeat the same request relationship:

```xml
<saml2:SubjectConfirmationData
    InResponseTo="ARQ-111"
    Recipient="http://localhost:8000/saml/acs"
    NotOnOrAfter="2026-09-22T08:15:00Z"/>
```

For an Assertion returned in response to an AuthnRequest, this `InResponseTo` must also match the original request ID.

The two locations have related but distinct scopes:

| Location | Meaning |
| --- | --- |
| Response `InResponseTo` | this Response answers that request |
| Subject confirmation `InResponseTo` | this bearer confirmation is valid for that request |

The current Spring Security provider has a Response validator for the outer value. OpenSAML's bearer subject-confirmation validator handles the inner value.

---

# 15. SP-initiated and unsolicited Responses differ

Our working transaction is SP-initiated:

```text
AcmeHR creates AuthnRequest
        |
        v
Okta returns correlated Response
```

That flow has an original AuthnRequest ID to match.

An IdP-initiated flow begins at the IdP and can send an unsolicited Response. There is no original SP AuthnRequest in that case, so the correlation rules differ.

Do not remove `InResponseTo` from an SP-initiated troubleshooting discussion by saying:

> SAML also supports IdP-initiated login.

That is a different flow. Day 11 compares SP-initiated and IdP-initiated behavior in detail.

For today's known-good SP-initiated transaction, request correlation is expected.

---

# 16. Time validation uses the SP clock

The IdP writes UTC timestamps into the SAML message.

The SP evaluates them using its own current time.

The important fields are:

| Field | Meaning |
| --- | --- |
| Assertion `IssueInstant` | when the IdP says it created the Assertion |
| Conditions `NotBefore` | earliest permitted use of the Assertion |
| Conditions `NotOnOrAfter` | time at which the Assertion is no longer valid |
| Subject confirmation `NotOnOrAfter` | time at which the bearer confirmation can no longer be used |

All examples in this lesson use UTC timestamps ending in `Z`.

Do not compare only the displayed clock time while ignoring the timezone or offset.

---

# 17. Conditions NotBefore sets the lower boundary

Consider:

```xml
<saml2:Conditions
    NotBefore="2026-09-22T08:09:00Z"
    NotOnOrAfter="2026-09-22T08:15:00Z">
```

Before `08:09:00Z`, the Assertion is not yet valid.

If AcmeHR's clock says `08:07:00Z`, validation should fail unless the permitted clock skew covers the difference.

This failure does not mean Priya entered a wrong password.

It means the SP believes the Assertion is being used before its valid time window.

Likely causes include:

- the SP clock is behind
- the IdP clock is ahead
- the message contains an incorrect timestamp
- the configured clock-skew allowance is smaller than the real clock difference

The first operational fix is accurate time synchronization on both systems.

---

# 18. Conditions NotOnOrAfter sets the upper boundary

The same Conditions element says:

```text
NotOnOrAfter="2026-09-22T08:15:00Z"
```

The name must be read literally:

> Not valid on or after this instant.

At `08:14:59Z`, the Assertion can still be inside its validity window.

At exactly `08:15:00Z`, it is no longer inside that window, subject to the validator's permitted clock skew.

This creates a half-open interval:

```text
NotBefore <= valid time < NotOnOrAfter
```

Do not describe `NotOnOrAfter` as the last valid moment. It is the first instant outside the normal validity interval.

---

# 19. Subject confirmation has its own expiry

The bearer subject confirmation normally contains another upper boundary:

```xml
<saml2:SubjectConfirmationData
    NotOnOrAfter="2026-09-22T08:15:00Z"/>
```

This timestamp limits how long the bearer Assertion can be delivered and confirmed at the recipient.

It is separate from the Assertion Conditions window.

Both may need to pass:

```text
Assertion Conditions valid now
        and
Bearer SubjectConfirmationData valid now
```

If one expires before the other, the earlier failing boundary prevents acceptance.

When diagnosing an expired Assertion, inspect both locations.

---

# 20. IssueInstant is a creation time, not an expiry time

Response and Assertion both carry `IssueInstant`:

```xml
IssueInstant="2026-09-22T08:10:00Z"
```

It records when the IdP says it issued that object.

It does not mean:

```text
valid until 08:10:00Z
```

Validity boundaries come from fields such as `NotBefore` and `NotOnOrAfter`.

`IssueInstant` is still useful during investigation. It helps answer:

- Was the Response created recently?
- Does the timestamp line up with the browser trace?
- Is one system's clock far ahead or behind?
- Are we looking at an old Response from a previous attempt?

Do not calculate an expiry by inventing a lifetime from `IssueInstant` unless a documented validator rule explicitly does that.

---

# 21. Clock skew is a small tolerance, not extra Assertion lifetime

IdP and SP clocks may differ by a small amount even when both use time synchronization.

A validator can allow bounded clock skew when evaluating timestamp rules.

Conceptually:

```text
IdP time: 08:10:03Z
SP time:  08:09:59Z
Difference: 4 seconds
```

A small tolerance prevents a four-second difference from breaking a valid login.

The current Spring Security 7.1.1 assertion-validator builder defaults to five minutes of clock skew unless the application configures another value.

That fact does not mean five minutes is correct for every organization.

Choose a bounded value based on:

- the organization's time-synchronization standard
- observed clock accuracy
- IdP and SP product behavior
- the security effect of widening the acceptance window

Do not increase skew until a badly synchronized clock stops failing.

That hides the operational fault and extends the time around assertion boundaries.

---

# 22. Keep the clocks synchronized

Both sides should use reliable time synchronization.

For the AcmeHR host, record evidence such as:

```bash
date -u
```

On a Linux host that uses `timedatectl`, you can also inspect:

```bash
timedatectl status
```

You are looking for:

- the current UTC time
- whether network time synchronization is active
- a large difference from the Response timestamps

The exact command depends on the operating system and permissions.

Do not change production time settings as part of a training exercise. Capture the evidence and follow the system owner's change process.

---

# 23. Success status is required but not sufficient

The Response contains a protocol status:

```xml
<saml2p:StatusCode
    Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
```

The current Spring Security Response validator requires a success result.

It also requires at least one Assertion.

That still does not replace the remaining checks.

```text
Success status
    does not prove correct audience
    does not prove correct destination
    does not prove request correlation
    does not prove current validity
    does not prove a valid signature
```

Status is one input to the decision, not the whole decision.

---

# 24. Structure must be usable too

Before a trust decision can complete, the input must be parseable SAML with the expected objects.

Examples of structural failures include:

- invalid Base64 in the form field
- malformed XML
- XML that is not a SAML 2.0 Response
- a Response with no Assertion
- an Assertion with no usable Subject or principal identifier

These failures are different from a correct XML document with the wrong Audience.

That distinction matters during troubleshooting:

```text
Could not parse the message
        is not the same as
Parsed the message and rejected a security value
```

Record the error category before changing configuration.

---

# 25. What the current Spring Security baseline validates

AcmeHR uses Spring Security 7.1.1 with OpenSAML 5.

At a high level, the current provider performs these checks:

| Object | Current default checks relevant today |
| --- | --- |
| Response | success status, at least one Assertion, `InResponseTo`, `Destination`, and `Issuer` |
| Signature protection | verifies the applicable Response or Assertion signature using configured verification credentials |
| Assertion conditions | time bounds, `AudienceRestriction`, delegation restriction, and supported condition handling |
| Bearer subject confirmation | recipient, expiry, request correlation when present, and related bearer rules; IP-address checking is skipped by default |
| Principal conversion | requires a usable subject identifier for the default principal mapping |

This is a summary of the current training baseline, not a complete list of every SAML rule in every product.

Spring Security allows validators to be replaced or customized. A different application can therefore behave differently even when it also uses Spring.

Always compare the message with the actual provider version and application configuration.

---

# 26. Do not assume a universal validation order

It is tempting to draw one fixed sequence:

```text
issuer -> audience -> destination -> time -> signature
```

That can mislead you.

Libraries may parse, decrypt, verify signatures, validate Response fields, and validate Assertions in an implementation-specific order.

Spring Security also provides extension points that can change parts of that behavior.

Use the groups from this lesson to reason about trust, but use the actual error and provider documentation to learn which check surfaced first.

If you fix one failure and see a new error, the new error may have been hidden behind the first one.

---

# 27. Do not edit a captured signed Response and repost it

Suppose you decode a real signed Response and change:

```xml
<saml2:Audience>urn:wrong:sp</saml2:Audience>
```

The signature no longer protects the edited content correctly.

When you repost it, signature validation should fail before or alongside the audience problem. That test does not isolate audience validation.

For a controlled negative test, use one of these approaches:

- change the SP's expected value while keeping the signed IdP message unchanged
- change the IdP app configuration and obtain a newly signed Response
- use a test fixture that can create and sign a Response with known test keys

Change one controlled variable and preserve the rest of the transaction.

The Day 6 lab will follow that rule.

---

# 28. A clean audience-failure experiment

Start from a working login where the Assertion contains:

```text
Audience = urn:acme:training:sp
```

Then change one side so the expected and received values disagree.

For example, a controlled test could expect:

```text
urn:acme:training:wrong-sp
```

The evidence chain should show:

1. the Response remains parseable
2. the signature arrangement remains valid for the test fixture
3. the received Audience remains `urn:acme:training:sp`
4. the configured expected SP entity ID is different
5. the login is rejected
6. the validation error identifies the Assertion or audience condition

Then restore the expected entity ID and prove the working case again.

That is stronger evidence than saying, "SSO broke when I changed a setting."

---

# 29. A clean endpoint-failure experiment

Start with agreement:

```text
Response Destination
    http://localhost:8000/saml/acs

Subject confirmation Recipient
    http://localhost:8000/saml/acs

Configured and actual ACS
    http://localhost:8000/saml/acs
```

Then introduce one controlled mismatch.

Examples include a different port or path in a newly issued test Response.

Record which check fails:

- outer Response destination
- bearer subject-confirmation recipient
- HTTP routing before SAML processing begins

These are not interchangeable.

If the POST receives an HTTP 404, the request may never reach the SAML processor. That is a routing failure, not proof of a SAML Destination rejection.

---

# 30. A clean time-failure experiment

Use controlled test timestamps and an explicit test clock if the lab supports one.

Test the boundaries separately:

| Case | Expected result |
| --- | --- |
| current time before `NotBefore`, beyond allowed skew | reject as not yet valid |
| current time inside both windows | continue validation |
| current time at or after Conditions `NotOnOrAfter`, beyond allowed skew | reject as expired |
| current time at or after subject-confirmation `NotOnOrAfter`, beyond allowed skew | reject bearer confirmation |

Do not wait for wall-clock minutes if a deterministic test can control time.

Do not change the server clock to manufacture a lab failure.

The goal is to prove the boundary, not disturb the host.

---

# 31. The existing unsigned-message test

Open:

```text
lab-sp/src/test/java/com/acme/training/acmehr/security/SamlResponseRejectionTests.java
```

The test builds a Response with values that look reasonable:

- expected test issuer
- expected audience
- expected ACS destination and recipient
- a current time window
- a NameID and authentication statement

It deliberately leaves out signature protection.

The important assertion is not a particular error-page message. It is:

```text
the request remains unauthenticated
```

This proves a central lesson:

> Correct-looking fields do not compensate for missing cryptographic trust.

The test does not yet prove each audience, endpoint, correlation, or time failure separately. Those focused tests belong in the Day 6 lab work.

---

# 32. Validation and authorization are different decisions

Suppose the SAML Response passes every validation check.

AcmeHR can now trust that the validated Assertion came from the configured IdP, was intended for AcmeHR, and is acceptable under the checked constraints.

That does not automatically answer:

- May Priya approve payroll?
- Is Priya in the HR administrator group?
- Is Priya's AcmeHR account active?
- Which application roles should Priya receive?

Those are authorization and account-policy questions.

Day 7 studies attributes, groups, and application authorization.

Keep today's decision narrow:

```text
SAML validation
    Can AcmeHR trust this authentication statement?

Application authorization
    What may this trusted principal do in AcmeHR?
```

---

# 33. Correlation and time checks are not complete replay protection

Request correlation and short time windows make reuse harder.

They do not prove that a Response or Assertion ID has never been accepted before.

Complete replay detection normally needs server-side state that remembers previously used message or Assertion identifiers for an appropriate period.

The current Spring Security default assertion validator does not enforce SAML `OneTimeUse` through a replay cache. Its source notes that the default validator has no caching facilities for that condition.

So do not claim:

> Spring's default timestamp checks make every accepted Assertion impossible to replay.

A production replay-control design depends on the SAML provider, request repository, session behavior, deployment topology, and any duplicate-ID cache added by the application or platform.

Today's lesson identifies the boundary. It does not add a replay cache.

---

# 34. Capture enough evidence to prove the failed comparison

For each failed login, collect a small evidence set:

| Evidence | What it answers |
| --- | --- |
| UTC time on the SP | what time the validator used approximately |
| request URL | where the Response actually arrived |
| registration ID | which Spring relying-party configuration was selected |
| expected SP entity ID | what Audience should match |
| expected ACS URL | what Destination and Recipient should match |
| expected IdP entity ID | what Issuer should match |
| saved AuthnRequest ID | what `InResponseTo` should match |
| decoded validation fields | what the IdP sent |
| server-side validation error | which check the provider reports |
| authentication result | whether a session was created |

Mask or remove user attributes, session values, and unrelated identifiers before sharing evidence.

Do not log the entire SAML Response in normal production logs. It can contain personal data and authentication details.

---

# 35. Use a comparison worksheet

Write the expected and received values next to each other.

```text
Check: Audience
Expected: urn:acme:training:sp
Received: urn:acme:training:sp
Result: match

Check: Destination
Expected: http://localhost:8000/saml/acs
Received: http://localhost:8000/saml/acs
Result: match

Check: Recipient
Expected: http://localhost:8000/saml/acs
Received: http://localhost:8000/saml/acs
Result: match

Check: Response InResponseTo
Expected: ARQ-111
Received: ARQ-111
Result: match
```

For time fields, include the SP time and permitted skew:

```text
SP current time: 2026-09-22T08:12:00Z
Conditions NotBefore: 2026-09-22T08:09:00Z
Conditions NotOnOrAfter: 2026-09-22T08:15:00Z
Configured/default skew under test: recorded separately
```

This format makes a mismatch visible and reviewable.

---

# 36. Start with the first concrete error

A failed login may produce several symptoms:

- the browser returns to a login page
- the application shows an authentication error
- no application session appears
- server logs contain a SAML validation exception

The server-side validation error is usually the strongest starting point.

Use this order:

1. Record the exact error and timestamp.
2. Confirm which transaction produced it.
3. Identify the field named by the error.
4. Write the expected and received values.
5. Trace where each value came from.
6. Change one source value.
7. repeat the transaction and compare the result.

Do not change audience, ACS, metadata, clock skew, and certificates at the same time.

Multiple simultaneous changes destroy the evidence that tells you which fix mattered.

---

# 37. What common errors suggest

| Error category | First values to compare |
| --- | --- |
| invalid issuer | Response and Assertion issuer, IdP metadata entity ID, selected registration |
| invalid audience | Assertion Audience, AcmeHR SP entity ID, Okta Audience URI |
| invalid destination | Response Destination, actual request URL, configured ACS, proxy headers |
| invalid subject confirmation | Recipient, subject-confirmation expiry, inner `InResponseTo`, confirmation method |
| invalid `InResponseTo` | Response value, subject-confirmation value, saved AuthnRequest ID, browser session state |
| assertion too early | Conditions `NotBefore`, SP UTC time, allowed skew |
| assertion expired | both `NotOnOrAfter` values, SP UTC time, allowed skew |
| invalid signature | signed object, configured IdP certificate, message integrity |
| malformed response | Base64 transport, XML structure, required Response and Assertion content |

This table gives you a first comparison, not a reason to ignore the full error.

---

# 38. Bad fixes that hide the real problem

Avoid these responses to a validation failure:

- disable Audience validation because the entity IDs differ
- remove Destination checking because a reverse proxy is misconfigured
- accept any issuer because the wrong metadata file was loaded
- set a very large clock skew because the host clock is wrong
- turn off signature validation to make a hand-edited Response pass
- enable unsolicited login to avoid fixing lost request state
- log every full Response in production for convenience

Each action removes or weakens a control that is reporting a real mismatch.

Fix the source of the expected or received value instead.

---

# 39. One message can contain more than one problem

Suppose a test Response has:

```text
wrong Audience
wrong Recipient
expired NotOnOrAfter
missing signature
```

The provider may report only one error first, or it may collect several errors in one validation result.

After fixing the first problem, another failure can appear.

That does not mean the first diagnosis was wrong.

It means the message had more than one invalid property.

For training tests, begin with a known-good fixture and break exactly one property. That produces a clean relationship between cause and result.

---

# 40. Read a working transaction as validation evidence

Capture a successful SP-initiated login and complete this table.

| Check | Received value | Expected source | Match? |
| --- | --- | --- | --- |
| IdP issuer | from Response and Assertion | Okta metadata entity ID | yes/no |
| Audience | from Assertion Conditions | AcmeHR SP entity ID | yes/no |
| Destination | from Response | actual AcmeHR ACS request URL | yes/no |
| Recipient | from SubjectConfirmationData | configured AcmeHR ACS | yes/no |
| Response `InResponseTo` | from Response | captured AuthnRequest ID | yes/no |
| Confirmation `InResponseTo` | from SubjectConfirmationData | captured AuthnRequest ID | yes/no |
| Conditions start | from Assertion | SP UTC time and skew | valid/invalid |
| Conditions end | from Assertion | SP UTC time and skew | valid/invalid |
| Confirmation end | from SubjectConfirmationData | SP UTC time and skew | valid/invalid |
| Signature | from signed SAML object | configured IdP verification credential | valid/invalid |

For signature validity, use provider validation evidence. Seeing `<ds:Signature>` in decoded XML does not prove that cryptographic verification passed.

---

# 41. Practice: audience or endpoint?

Okta is configured with:

```text
Audience URI
http://localhost:8000/saml/acs

Single sign-on URL
http://localhost:8000/saml/acs
```

AcmeHR expects:

```text
SP entity ID
urn:acme:training:sp

ACS
http://localhost:8000/saml/acs
```

Which comparison fails?

<details>
<summary>Check your answer</summary>

Audience validation fails.

Okta sends the Audience URI as the Assertion audience, but AcmeHR expects its SP entity ID:

```text
urn:acme:training:sp
```

The Single sign-on URL is correct for the ACS. The error comes from using the ACS URL in the Audience URI field.

</details>

---

# 42. Practice: Destination or Recipient?

The Response contains:

```text
Destination
http://localhost:8080/saml/acs

Recipient
http://localhost:8000/saml/acs
```

The browser posts to:

```text
http://localhost:8000/saml/acs
```

Which field has the direct mismatch?

<details>
<summary>Check your answer</summary>

The Response `Destination` has the direct mismatch.

It names port `8080`, while the Response arrived on port `8000`.

The bearer `Recipient` matches the course ACS in this example.

Do not report both fields as wrong merely because they usually contain the same URL.

</details>

---

# 43. Practice: read NotOnOrAfter exactly

The Assertion Conditions contain:

```text
NotBefore     08:09:00Z
NotOnOrAfter  08:15:00Z
```

Ignore clock skew for this question.

Is the Assertion inside this Conditions window at exactly `08:15:00Z`?

<details>
<summary>Check your answer</summary>

No.

`NotOnOrAfter="08:15:00Z"` means the Assertion is not valid at `08:15:00Z` or later.

The normal interval ends just before that instant.

</details>

---

# 44. Practice: what does IssueInstant prove?

You see:

```text
IssueInstant = 08:10:00Z
```

Can you conclude that the Assertion expires at `08:10:00Z`?

<details>
<summary>Check your answer</summary>

No.

`IssueInstant` records when the IdP says it issued the object.

Read Conditions and subject-confirmation time fields for the validity boundaries applied to the Assertion.

</details>

---

# 45. Practice: why did the edited Response fail?

A working signed Response was decoded. An engineer changed its Audience and posted the edited XML back to the ACS.

The server reported an invalid signature.

Does this experiment prove that Audience validation is disabled?

<details>
<summary>Check your answer</summary>

No.

Editing signed content invalidated its signature. The message failed before the experiment could isolate the audience comparison.

Use a newly signed Response, a signed test fixture, or a controlled change to the SP's expected value.

</details>

---

# 46. Practice: correlate the request

The captured AuthnRequest contains:

```text
ID = ARQ-111
```

The returned Response contains:

```text
InResponseTo = ARQ-999
```

Both messages otherwise belong to the same browser trace. What should AcmeHR do?

<details>
<summary>Check your answer</summary>

AcmeHR should reject the SP-initiated login because the Response does not answer the saved AuthnRequest ID.

Investigate the IdP response, saved request state, browser session, and application-instance routing.

Do not rewrite the captured Response by hand because that would also affect signature validity.

</details>

---

# 47. Practice: clock skew or clock repair?

The SP clock is eleven minutes behind the correct time. Assertion validation passes only after an engineer changes allowed skew from five minutes to fifteen minutes.

Is the work complete?

<details>
<summary>Check your answer</summary>

No.

The large skew hides a broken clock and widens the acceptance boundary.

Restore reliable time synchronization, verify the measured difference, and then choose the smallest justified skew according to the system's security standard.

</details>

---

# 48. Day 6 checkpoint

Before moving to the lab, you should be able to answer:

1. Why is decoded XML not automatically trusted?
2. What is the difference between Okta authentication success and AcmeHR acceptance?
3. Where does AcmeHR get its expected IdP issuer?
4. What value should the Assertion Audience include?
5. Why is the SP entity ID not the same kind of value as the ACS URL?
6. What does Response `Destination` protect?
7. What does bearer `Recipient` protect?
8. Why can Destination and Recipient contain the same URL but remain separate checks?
9. What should Response `InResponseTo` match in an SP-initiated flow?
10. Where else can `InResponseTo` appear?
11. Why does unsolicited login have different correlation rules?
12. What does Conditions `NotBefore` mean?
13. What does Conditions `NotOnOrAfter` mean at the exact boundary?
14. Where is the other common `NotOnOrAfter` field?
15. Why is `IssueInstant` not an expiration time?
16. Why do IdP and SP clocks need synchronization?
17. What problem is bounded clock skew meant to handle?
18. Why should skew not hide a clock that is minutes wrong?
19. What does `StatusCode=Success` prove and not prove?
20. Why does editing signed XML fail to isolate an audience test?
21. Which validation checks does the current Spring baseline perform?
22. Why are correlation and time checks not a complete duplicate-ID replay cache?
23. What evidence should you capture before changing configuration?
24. Why should a negative test break only one property?

If one answer is unclear, return to that field and write its received value beside its expected source.

---

# Explain it back

Imagine a new engineer asks:

> Okta says the user signed in, so why did AcmeHR reject the login?

A clear explanation could sound like:

> Okta authentication and SP acceptance are separate results. AcmeHR validates the returned SAML content before creating a session. It verifies acceptable signature protection, compares the issuer with the configured Okta entity ID, checks that the Assertion audience includes the AcmeHR SP entity ID, checks Destination and Recipient against the ACS, correlates InResponseTo with the saved AuthnRequest, and evaluates the Assertion's time limits using the SP clock and bounded skew. A mismatch in any required check can reject the login even when the Response status says Success.

Do not memorize those exact words.

Use a captured transaction to point to each received field and its expected value.

---

# Day 6 completion standard

The Day 6 lesson is complete when you can:

- separate a readable SAML message from a trusted one
- explain why the SP makes its own acceptance decision
- identify the configured source of every expected value
- connect IdP issuer to Okta metadata
- connect Audience to `urn:acme:training:sp`
- connect Destination and Recipient to the AcmeHR ACS without confusing their scopes
- connect both `InResponseTo` locations to the original AuthnRequest in the SP-initiated flow
- evaluate `NotBefore` and both common `NotOnOrAfter` locations
- read the `NotOnOrAfter` boundary correctly
- use `IssueInstant` as issuance evidence rather than an invented expiry
- explain the purpose and cost of clock skew
- distinguish clock-skew tolerance from clock repair
- state what Spring Security 7.1.1 validates in the current baseline
- explain why a hand-edited signed Response is not a clean field-validation test
- separate validation from authorization
- state the replay-protection boundary of the default provider
- collect expected, received, error, and result evidence for one failed check
- restore a known-good configuration after a negative test

The Day 6 lab will turn these comparisons into evidence from the application and focused rejection tests.

---

# Official references used for this lesson

- [OASIS: Assertions and Protocols for SAML 2.0](https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf)
- [OASIS: Profiles for SAML 2.0](https://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf)
- [OASIS: SAML 2.0 Errata 05](https://docs.oasis-open.org/security/saml/v2.0/sstc-saml-approved-errata-2.0.html)
- [OASIS: SAML 2.0 Technical Overview](https://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html)
- [Spring Security: Authenticating SAML Responses](https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html)
- [Spring Security 7.1.1: SAML error codes](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/saml2/core/Saml2ErrorCodes.html)
- [Spring Security source: OpenSaml5AuthenticationProvider](https://github.com/spring-projects/spring-security/blob/2907aa83207d5a22cf4ddd87d1596f17ba6764eb/saml2/saml2-service-provider/src/opensaml5Main/java/org/springframework/security/saml2/provider/service/authentication/OpenSaml5AuthenticationProvider.java)
