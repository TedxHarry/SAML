# Day 8: Signing and IdP Certificates

## What you should understand by the end of today

On Day 6, AcmeHR validated values such as Issuer, Audience, Destination, Recipient, time, and correlation.

On Day 7, AcmeHR used trusted claims after that validation succeeded.

Today we open one part of the trust decision that we deliberately kept simple before now:

> How can AcmeHR verify that the SAML message really came from the trusted Identity Provider and that protected XML was not changed after it was signed?

By the end of Day 8, you should be able to explain:

- what authenticity means in a SAML transaction
- what integrity means in a SAML transaction
- what a digital signature protects
- why a digital signature does not encrypt the SAML message
- why HTTPS and a SAML signature solve different problems
- which side owns the private signing key
- which side receives the public certificate
- what the IdP signing certificate is used for
- why the certificate itself is not a secret
- the difference between signing the Response and signing the Assertion
- what it means when both are signed
- where ds:Signature appears in the XML
- the purpose of DigestValue and SignatureValue at a practical level
- why a certificate embedded in XML is not automatically trusted
- how Okta exposes SAML signing certificates for an app
- how AcmeHR obtains verification certificates from IdP metadata
- why a valid signature alone does not make a SAML login acceptable
- how a wrong trusted certificate produces a signature-validation failure
- why changing signed XML after signing causes verification to fail
- how to collect evidence before calling a failure a certificate problem

Today is not about signing AuthnRequests.

That belongs to Day 9.

Today is also not about assertion encryption.

That belongs to Day 9.

---

# 1. Start with the problem

Our normal transaction still looks like this:

~~~text
Okta
    |
    | creates SAML Response
    v
Browser
    |
    | carries SAMLResponse
    v
AcmeHR ACS
~~~

The browser is the transport.

The browser is not the authority that AcmeHR trusts for the identity statement.

AcmeHR needs a way to answer:

~~~text
Did trusted Okta create the protected SAML content?

Was that protected content changed after Okta signed it?
~~~

Digital signatures are part of that answer.

---

# 2. Two words you need: authenticity and integrity

These words sound abstract until we attach them to the transaction.

## Authenticity

For this lesson:

> Authenticity means AcmeHR can verify that the protected SAML content was signed by the key associated with the trusted IdP.

In our scenario:

~~~text
Trusted signer
    Okta

Verifier
    AcmeHR
~~~

## Integrity

For this lesson:

> Integrity means the signed content has not been changed without causing signature verification to fail.

If someone modifies protected XML after it was signed, the signature should no longer verify.

---

# 3. A signature is not encryption

This is one of the most important Day 8 distinctions.

A digital signature does not hide the SAML XML.

You can still decode a normal Base64 SAMLResponse and read values such as:

~~~text
Issuer
NameID
Audience
department
groups
~~~

Signing answers:

~~~text
Who signed this protected content?

Has this protected content changed?
~~~

Encryption answers a different question:

~~~text
Who is allowed to read this protected content?
~~~

We handle encryption on Day 9.

---

# 4. Base64 is still not security

Day 2 established that Base64 is an encoding.

That remains true.

A SAMLResponse can be:

~~~text
signed
    and
Base64 encoded
~~~

Those are separate operations.

Base64 makes binary-safe transport easier.

The XML signature provides cryptographic integrity and signer verification.

Do not say:

> The SAMLResponse is secure because it is Base64 encoded.

---

# 5. The browser can carry a signature without owning the signing key

The browser transports the signed SAML message.

It does not need Okta's private signing key.

Think of the roles this way:

~~~text
Okta
    creates SAML XML
    signs protected content

Browser
    carries the result

AcmeHR
    verifies the signature
~~~

The browser is between the signer and verifier, but it is not part of the trust key pair.

---

# 6. The key pair

At a practical level, signing uses two related keys:

~~~text
Private key
    kept by the signer

Public key
    shared with verifiers
~~~

For our outbound Okta SAML application:

~~~text
Okta
    owns private signing key

AcmeHR
    receives public verification certificate
~~~

That direction matters.

Do not send the IdP private signing key to the Service Provider.

The Service Provider does not need it to verify signatures.

---

# 7. What is the IdP signing certificate?

For this course, the IdP signing certificate is an X.509 certificate containing public-key information that AcmeHR can use as part of its configured trust for signatures created by Okta.

Plain meaning:

~~~text
Okta private key
    signs

Okta public certificate
    lets AcmeHR verify
~~~

The certificate is public trust material.

It is not the secret half of the key pair.

---

# 8. The certificate is not the signature

These are separate objects.

~~~text
Certificate
    contains public-key and certificate information

Signature
    cryptographic value created for specific signed XML
~~~

One signing certificate can be used to verify many SAML messages created while the corresponding private key is active.

Every SAML Response has its own XML content and signature value.

---

# 9. The private key is the sensitive object

The signing private key must remain under the signer's control.

If an unauthorized party obtains the private signing key, they may be able to create signatures that verifiers accept as belonging to that signer.

So the ownership table for Day 8 is:

| Item | Owner / location | Share with partner? |
| --- | --- | --- |
| Okta signing private key | Okta | no |
| Okta signing public certificate | shared trust material | yes |
| AcmeHR copy of trusted IdP certificate | AcmeHR trust configuration | yes, it is public material |
| SAML XML signature | inside the SAML message | carried by browser |

Do not confuse public with unimportant.

The public certificate is not secret, but AcmeHR must still trust the correct one.

---

# 10. What does signing actually protect?

A SAML XML signature applies to a particular signed XML object.

The important practical question is:

> Which XML element is signed?

Common answers are:

~~~text
Response

Assertion

Response and Assertion
~~~

Do not stop at:

> The SAML is signed.

Find the signed object.

---

# 11. Response signing

A shortened Response can look like this:

~~~xml
<samlp:Response ID="_response123" ...>

    <saml:Issuer>...</saml:Issuer>

    <ds:Signature>
        ...
    </ds:Signature>

    <saml:Assertion ID="_assertion456">
        ...
    </saml:Assertion>

</samlp:Response>
~~~

Here the ds:Signature is a child of the Response.

Plain meaning:

> The Response element is the signed object.

When the Response signature is valid, the signed Response content is cryptographically protected according to the XML signature.

---

# 12. Assertion signing

A shortened message can instead look like:

~~~xml
<samlp:Response ID="_response123" ...>

    <saml:Issuer>...</saml:Issuer>

    <saml:Assertion ID="_assertion456">

        <saml:Issuer>...</saml:Issuer>

        <ds:Signature>
            ...
        </ds:Signature>

        <saml:Subject>
            ...
        </saml:Subject>

    </saml:Assertion>

</samlp:Response>
~~~

Here the ds:Signature is inside the Assertion.

Plain meaning:

> The Assertion is the signed object.

Values inside that signed Assertion are protected by that signature.

---

# 13. Response and Assertion can both be signed

You can also see:

~~~text
Response
    signed

Assertion
    signed
~~~

That produces two XML signatures.

Do not describe them as duplicate text.

They protect different XML elements.

When troubleshooting, inspect both locations separately.

---

# 14. Current Okta signing controls

For a custom SAML app built with Okta's Application Integration Wizard, the advanced SAML settings include separate controls named:

~~~text
Response

Assertion Signature
~~~

The Response setting controls whether Okta digitally signs the SAML authentication Response.

The Assertion Signature setting controls whether Okta digitally signs the Assertion.

The same advanced settings also expose:

~~~text
Signature Algorithm

Digest Algorithm
~~~

So when an application owner says:

> We require signed SAML.

you still need to ask:

~~~text
Response signed?

Assertion signed?

Both?
~~~

---

# 15. SAML HTTP-POST requires signature protection

Our SAML Response returns to AcmeHR using HTTP-POST.

For the Web Browser SSO profile, an assertion delivered this way must be protected by digital signature.

That protection can be achieved by:

~~~text
signing the Assertion

or

signing the enclosing Response

or

signing both
~~~

This is why a correct Service Provider must not accept an unprotected SAML login just because the XML is readable and the fields look correct.

---

# 16. What our Spring Security baseline accepts

The current AcmeHR training SP uses Spring Security 7.1 with OpenSAML.

At a high level, its default processing verifies signatures on the SAML Response and Assertions.

The current Spring Security behavior requires signature protection such that:

~~~text
Response is signed

or

all Assertions relied upon are signed
~~~

If neither the Response nor the Assertions have valid required signature protection, authentication fails.

This is why our earlier unsigned-response negative test was important even before Day 8 explained the mechanism.

---

# 17. Signature location changes what is cryptographically protected

Do not assume a signature in one place magically signs every unrelated XML node.

If the Assertion is the signed object:

~~~text
Assertion content
    protected by Assertion signature

outer Response fields
    not automatically part of that Assertion signature
~~~

If the Response is the signed object, the signed Response protects the content included by that signature reference, including the enclosed Assertion as part of that signed XML structure.

The Service Provider must still process the message according to SAML validation rules.

---

# 18. The signature does not replace the Day 6 checks

Suppose the signature is cryptographically valid.

AcmeHR still has to evaluate values such as:

~~~text
Issuer

Audience

Destination

Recipient

InResponseTo

NotBefore

NotOnOrAfter
~~~

A valid signature means:

> The signed content matches the signature created with the trusted signing key.

It does not mean:

> Every value inside the message is acceptable for AcmeHR.

Day 6 and Day 8 work together.

---

# 19. A correctly signed wrong Audience is still wrong

Imagine Okta signs this Assertion correctly:

~~~text
Audience
    urn:another:sp
~~~

AcmeHR expects:

~~~text
urn:acme:training:sp
~~~

The signature can be valid.

The Audience is still wrong.

Correct diagnosis:

~~~text
Signature verification
    PASS

Audience validation
    FAIL
~~~

Do not call every SAML rejection a certificate problem.

---

# 20. A bad signature should stop trust before application use

Now reverse the situation.

Suppose the values look perfect:

~~~text
Issuer
    expected

Audience
    expected

Destination
    expected

department
    Finance
~~~

but signature verification fails.

AcmeHR must not say:

> The values look correct, so continue.

The message has failed a trust requirement.

Claims from an untrusted message must not be used for application authorization.

---

# 21. A practical look inside ds:Signature

A shortened XML signature can contain structures such as:

~~~xml
<ds:Signature>

    <ds:SignedInfo>

        <ds:CanonicalizationMethod ... />

        <ds:SignatureMethod ... />

        <ds:Reference URI="#_assertion456">

            <ds:DigestMethod ... />

            <ds:DigestValue>
                ...
            </ds:DigestValue>

        </ds:Reference>

    </ds:SignedInfo>

    <ds:SignatureValue>
        ...
    </ds:SignatureValue>

    <ds:KeyInfo>
        ...
    </ds:KeyInfo>

</ds:Signature>
~~~

Today you only need to understand the jobs of these pieces.

We are not implementing XML signature processing ourselves.

---

# 22. Reference tells you what is being signed

Look at:

~~~xml
<ds:Reference URI="#_assertion456">
~~~

Conceptually, that reference points to the XML object whose ID is:

~~~text
_assertion456
~~~

If that ID belongs to the Assertion, the signature is referencing that Assertion.

This is more useful evidence than assuming the signed object from the filename, browser URL, or where the XML happened to be copied from.

---

# 23. DigestValue is an integrity checkpoint

At a practical level:

~~~text
signed XML content
    |
    v
digest algorithm
    |
    v
DigestValue
~~~

If the protected XML content changes, the calculated digest no longer matches what was signed.

That is one part of how XML signature validation detects tampering.

Do not manually calculate this in application code.

The SAML/XML security library does that work.

---

# 24. SignatureValue is verified with the trusted public key

At a practical level, SignatureValue is the cryptographic result that the verifier checks using the corresponding trusted public key.

When verification succeeds, it supports the conclusion that the signature was created with the corresponding private key, assuming that private key remains under the trusted signer's control.

The simplified idea is:

~~~text
Signer
    uses private key

Verifier
    uses trusted public key
~~~

Do not interpret SignatureValue as an encrypted copy of the entire Assertion.

It is a signature value, not the SAML payload.

---

# 25. Why XML signature validation is not a string comparison

Correct XML signature validation involves XML-specific processing.

It is not safe to do something like:

~~~text
Find SignatureValue
Compare text
Trust message
~~~

Libraries need to handle details such as:

- signature references
- transforms
- canonicalized XML
- digest verification
- signature algorithms
- trusted verification credentials

The training SP relies on Spring Security and OpenSAML for this.

We do not write our own XML signature verifier.

---

# 26. Canonicalization exists, but we are not going deep today

XML can have formatting differences that do not necessarily change its logical structure.

XML signatures therefore use canonicalization rules as part of the signing process.

You will see a CanonicalizationMethod inside SignedInfo.

For Day 8, remember only:

> XML signature libraries need a defined representation of XML before cryptographic comparison.

Detailed canonicalization behavior and XML signature attack classes belong in the advanced course.

---

# 27. What is KeyInfo?

A signature can contain KeyInfo with information such as an X.509 certificate.

Example shape:

~~~xml
<ds:KeyInfo>
    <ds:X509Data>
        <ds:X509Certificate>
            MIID...
        </ds:X509Certificate>
    </ds:X509Data>
</ds:KeyInfo>
~~~

That certificate text is public material.

But there is a critical trust rule:

> A certificate appearing inside a message does not automatically make that certificate trusted.

---

# 28. Why embedded certificate does not create trust by itself

Imagine an attacker creates:

~~~text
their own private key

their own certificate

their own fake SAML Assertion

a valid signature using their own key
~~~

If AcmeHR accepted any certificate merely because it appeared inside KeyInfo, the attacker could provide the key needed to verify their own fake message.

So AcmeHR needs trust configured independently.

In our course:

~~~text
Trusted IdP metadata
    supplies Okta verification certificate

incoming signed SAML
    must verify against trusted verification material
~~~

That is the trust relationship.

---

# 29. Where Okta exposes the signing certificate

In the current Okta Identity Engine Admin Console:

~~~text
Applications
    ->
Applications
    ->
AcmeHR Training
    ->
Sign On
    ->
SAML Signing Certificates
~~~

That section lists signing certificates available for the app integration.

Current Okta guidance recommends keeping the app-scoped certificate active.

From the certificate Actions menu, the admin can work with items such as the IdP metadata or certificate.

This is the IdP-side trust material AcmeHR needs.

---

# 30. Active certificate matters

Okta can have more than one signing certificate available for an app.

The important operational question is:

> Which certificate is signing the SAML messages AcmeHR is receiving now?

Do not select a certificate just because it appears first in an old document or screenshot.

Confirm the active certificate for the app.

The active signing certificate and the certificate trusted by AcmeHR must agree.

Certificate rollover is handled in depth on Day 12.

---

# 31. Metadata is our normal trust exchange

Our training SP is configured with:

~~~text
IDP_METADATA_URL
~~~

SamlRelyingPartyConfig loads the Okta IdP metadata through Spring Security's RelyingPartyRegistrations support.

That metadata supplies the asserting-party configuration, including verification credentials published by the IdP.

Simplified:

~~~text
Okta metadata
    |
    | IdP identity
    | SSO endpoint
    | public signing certificate
    v
AcmeHR RelyingPartyRegistration
~~~

This avoids manually copying a private key because AcmeHR never needs Okta's private signing key.

---

# 32. What AcmeHR does with the trusted certificate

Spring Security calls this verification material.

Conceptually:

~~~text
Incoming SAML signature
        +
trusted Okta public certificate
        |
        v
cryptographic verification
        |
        +--> valid
        |
        +--> invalid
~~~

Spring Security's relying-party configuration can use an asserting party's verification X.509 credentials for this purpose.

That is the Service Provider side of Day 8.

---

# 33. Do not confuse the IdP signing certificate with Okta's Signature Certificate field

Okta's Application Integration Wizard also has an advanced setting named:

~~~text
Signature Certificate
~~~

That field is for a certificate supplied by the Service Provider so Okta can validate signed requests and related SLO messages.

That is the opposite trust direction.

For Day 8:

~~~text
Okta signs Response / Assertion
AcmeHR verifies
~~~

For Day 9 signed AuthnRequests:

~~~text
AcmeHR signs AuthnRequest
Okta verifies
~~~

The word certificate appears in both configurations.

The ownership direction tells you which one you are looking at.

---

# 34. Trust direction table

Use this table before changing any certificate.

| Message | Signer | Private key owner | Verifier | Public certificate needed by |
| --- | --- | --- | --- | --- |
| SAML Response / Assertion | Okta | Okta | AcmeHR | AcmeHR |
| AuthnRequest, Day 9 | AcmeHR | AcmeHR | Okta | Okta |

If you cannot fill out this table, do not start replacing certificates yet.

---

# 35. Signature Algorithm and Digest Algorithm are different fields

Okta exposes both:

~~~text
Signature Algorithm

Digest Algorithm
~~~

At a practical level:

~~~text
Digest Algorithm
    produces the digest for referenced content

Signature Algorithm
    defines the cryptographic signing operation
~~~

For new configurations, use modern SHA-256-based settings supported by both sides rather than selecting SHA-1 for compatibility without a documented requirement.

Do not change algorithms during a certificate troubleshooting exercise unless the evidence points to an algorithm mismatch.

---

# 36. Certificate details worth recording

When troubleshooting an IdP signing certificate, record concrete values.

Useful evidence includes:

~~~text
Certificate source
    Okta app Sign On / metadata

Status
    active or inactive

Fingerprint
    exact certificate fingerprint

Subject
    certificate subject

Issuer
    certificate issuer

Not Before
    certificate validity start

Not After
    certificate validity end
~~~

The exact enforcement of certificate validity and certificate-chain rules depends on the Service Provider's trust implementation.

Do not claim:

> It is expired, therefore this specific SP definitely rejected it.

unless the actual SP evidence shows that.

---

# 37. Fingerprint is useful for comparison

Two certificates can have similar names.

A certificate fingerprint gives you a concrete way to compare:

~~~text
Certificate Okta is using

Certificate AcmeHR trusts
~~~

If the fingerprints differ, that is important evidence.

It still does not tell you by itself why the wrong certificate was configured.

But it narrows the problem.

---

# 38. Wrong trusted certificate

Consider this failure:

~~~text
Okta signs with
    Certificate B private key

AcmeHR trusts
    Certificate A public key
~~~

AcmeHR cannot verify Okta's new signature with the old trusted key.

Expected layer:

~~~text
Response reaches ACS
    PASS

SAML XML can be decoded
    PASS

Signature verification
    FAIL

Application session
    NOT CREATED
~~~

This is a signature-trust failure.

Do not fix Audience, NameID, or group claims.

---

# 39. Changing signed XML after signing

Suppose a captured Assertion originally contains:

~~~text
department = Finance
~~~

After Okta signs it, someone edits the XML to:

~~~text
department = Payroll
~~~

If department is inside the signed object, that edit changes the protected content.

The original signature should no longer verify.

This is the integrity property we care about.

For testing, do not edit a signed live message and then claim:

> The department validator rejected Payroll.

The first failure can simply be the damaged signature.

Day 6 used the same rule when building controlled negative fixtures.

---

# 40. The last successful step still matters

A user reports:

> Okta login works, but AcmeHR says SAML failed.

Do not start by replacing certificates.

Use evidence.

Example:

~~~text
Browser reached Okta
    PASS

User authenticated
    PASS

Browser POSTed SAMLResponse to ACS
    PASS

Signature verification
    FAIL

Audience validation
    not reached or not trusted as the root cause
~~~

Now a certificate/signature investigation is justified.

---

# 41. What to inspect in the browser

For the current live transaction:

1. find the POST to the AcmeHR ACS
2. copy the parsed SAMLResponse value
3. decode it locally
4. find ds:Signature
5. determine whether it is under Response, Assertion, or both
6. find the Reference URI
7. identify the ID of the signed object
8. note SignatureMethod and DigestMethod
9. inspect X509Certificate only as message evidence, not as automatic trust

Do not upload the production assertion to a public decoder.

---

# 42. What to inspect in Okta

For the AcmeHR app, useful Day 8 evidence is under:

~~~text
Sign On
    ->
SAML Signing Certificates
~~~

Record:

~~~text
active certificate

certificate fingerprint

certificate validity dates

IdP metadata for the active trust
~~~

Then compare that with what AcmeHR is configured to trust.

Do not generate or activate a new certificate merely to inspect the current one.

---

# 43. What to inspect on the Service Provider

For AcmeHR, ask:

~~~text
Where did the trusted IdP certificate come from?

Which certificate is currently in the relying-party verification configuration?

Was metadata changed?

Was the application restarted or trust configuration reloaded?

What exact signature-validation error was returned?
~~~

In our current training SP, the IdP metadata URL is the normal source of asserting-party trust configuration.

---

# 44. HTTPS does not replace SAML signatures

Our local training URL uses HTTP for simplicity.

A production SAML integration should use HTTPS according to the deployment requirements.

But even with HTTPS, SAML signature validation still matters.

HTTPS protects a network connection.

A SAML signature protects the signed SAML object from the signer to the verifier.

The two controls operate at different layers.

Do not disable SAML signature validation because the ACS uses HTTPS.

---

# 45. SAML signatures do not replace HTTPS either

The reverse mistake is also wrong.

A signed SAML Assertion does not mean:

> The browser transport no longer needs HTTPS.

Production endpoints still need appropriate transport security.

Think:

~~~text
HTTPS
    protects transport connection

SAML signature
    protects signed SAML content and proves signer possession of the trusted key
~~~

Use both where the deployment requires them.

---

# 46. Signature success does not tell you which human authenticated

The cryptographic signature tells AcmeHR about the signer of the SAML object.

In our scenario, that signer is Okta.

The subject identity comes from the validated SAML Assertion:

~~~text
NameID
attributes
authentication statement
~~~

Do not say:

> Priya signed the SAML Assertion.

Priya did not.

Okta signed it.

---

# 47. IdP certificate is not the user's certificate

The certificate used for outbound SAML signing belongs to the federation trust between Okta and AcmeHR.

It is not:

- Priya's personal certificate
- Priya's MFA credential
- Priya's browser certificate
- the AcmeHR HTTPS server certificate
- the SP request-signing certificate used on Day 9

Keep those certificate roles separate.

---

# 48. App signing certificate versus HTTPS certificate

A certificate can appear in several parts of one project.

Example:

~~~text
acmehr.example.com HTTPS certificate
    protects TLS for the website

Okta SAML signing certificate
    verifies Okta-signed SAML

AcmeHR SAML request-signing certificate
    Day 9, verifies SP-signed requests at Okta
~~~

They can all be X.509 certificates.

That does not make them interchangeable.

Purpose and key ownership matter.

---

# 49. Certificate mismatch incident

Suppose the evidence shows:

~~~text
Okta active SAML signing certificate
    fingerprint B

AcmeHR trusted verification certificate
    fingerprint A

Browser reaches ACS
    yes

Signature validation
    fails
~~~

Your working hypothesis is now specific:

> AcmeHR is verifying Okta's SAML with a different public certificate than the one associated with the active Okta signing key.

The next change should be limited to the trust configuration.

Do not also change:

- Audience
- ACS
- NameID
- claims
- group expressions
- authentication policy

---

# 50. Signing placement mismatch incident

Suppose a vendor says:

> We require Assertion signing.

Okta is configured:

~~~text
Response
    Signed

Assertion Signature
    Unsigned
~~~

The SAML message still has signature protection, but it does not meet that vendor's stated placement requirement.

The correct investigation is:

~~~text
What did the SP require?

What object did Okta sign?

What object did the SP actually reject?
~~~

Do not reduce the issue to:

> Certificate bad.

The certificate can be correct while the signature-placement contract is wrong.

---

# 51. Algorithm mismatch incident

Suppose the certificate matches, but the SP rejects the signature algorithm.

Now compare:

~~~text
Okta Signature Algorithm

Okta Digest Algorithm

SP supported / required algorithms

actual SignatureMethod in XML

actual DigestMethod in XML
~~~

Again, do not rotate the certificate if the key itself is not the mismatch.

One failed layer, one targeted change.

---

# 52. What not to do

Do not solve a Day 8 failure by:

- disabling signature validation
- accepting any certificate found in KeyInfo
- trusting any issuer that can produce a cryptographic signature
- changing Audience to make a signature error disappear
- changing the ACS because a certificate fingerprint differs
- uploading an IdP private key to the SP
- using Base64 decoding as proof of trust
- changing several signing settings together
- copying a certificate from an unrelated Okta app without proving it is the active signer
- editing signed XML and then diagnosing a later validation field

Those shortcuts destroy the evidence chain.

---

# 53. A practical signature evidence table

For one successful transaction, build this table.

| Question | Evidence |
| --- | --- |
| Who created the SAML Response? | Okta issuer and configured IdP relationship |
| What XML object is signed? | location of ds:Signature and Reference URI |
| Which algorithm is used? | SignatureMethod |
| Which digest is used? | DigestMethod |
| Which certificate does Okta show as active? | SAML Signing Certificates |
| Which certificate does AcmeHR trust? | IdP metadata / verification credential |
| Did signature verification pass? | SP authentication result / test evidence |
| Did semantic validation also pass? | issuer, audience, endpoint, time, correlation evidence |
| Was an application session created? | protected AcmeHR page |

Do not skip the last two rows.

Signature validation is one part of the transaction.

---

# 54. Success path in plain English

A healthy Day 8 transaction is:

~~~text
Okta creates the Response
        |
        v
Okta signs the Response, Assertion, or both
        |
        v
Browser carries SAMLResponse
        |
        v
AcmeHR receives it at ACS
        |
        v
AcmeHR uses trusted Okta verification certificate
        |
        v
Signature verification passes
        |
        v
Other SAML validation passes
        |
        v
Authenticated AcmeHR session created
~~~

That is the relationship you need to be able to explain.

---

# 55. Failure path in plain English

A certificate mismatch looks like:

~~~text
Okta signs with private key B
        |
        v
Browser carries message
        |
        v
AcmeHR verifies with public key A
        |
        v
Signature verification fails
        |
        v
SAML authentication rejected
        |
        v
No authenticated application session
~~~

The browser can still have reached the correct ACS.

The XML can still be readable.

The claims can still look correct.

Trust still failed.

---

# 56. Mini incident 1

You decode a SAMLResponse and find:

~~~text
Response
    no ds:Signature

Assertion
    has ds:Signature
~~~

Is the message automatically unsigned?

<details>
<summary>Check your answer</summary>

No.

The Assertion is signed.

You still need to verify that this signature placement satisfies the Service Provider's requirements and that the signature verifies with trusted IdP verification material.

</details>

---

# 57. Mini incident 2

The Response signature verifies successfully.

The Audience is:

~~~text
urn:wrong:sp
~~~

Can AcmeHR accept the login because the signature is valid?

<details>
<summary>Check your answer</summary>

No.

Signature verification and Audience validation answer different questions.

The signed content can be authentic and unchanged while still being intended for another Service Provider.

</details>

---

# 58. Mini incident 3

Okta's active signing certificate fingerprint is different from the certificate AcmeHR currently trusts.

The browser reaches the ACS, and signature validation fails.

What should you investigate first?

<details>
<summary>Check your answer</summary>

Investigate the IdP signing trust configuration.

Confirm that AcmeHR has the public certificate corresponding to the active Okta signing key.

Do not start by changing claims or Audience.

</details>

---

# 59. Mini incident 4

A developer says:

> I decoded the SAMLResponse, so the message is valid.

What is wrong with that statement?

<details>
<summary>Check your answer</summary>

Decoding only turns the Base64 transport value back into XML.

It does not verify the XML signature, issuer, audience, endpoint values, time conditions, or request correlation.

</details>

---

# 60. Mini incident 5

A developer edits the department value inside a signed Assertion and then submits the modified message.

Signature validation fails.

Can they conclude that AcmeHR rejected the department value?

<details>
<summary>Check your answer</summary>

No.

Changing content inside the signed object invalidated the signature.

The test no longer isolates department handling.

</details>

---

# 61. Mini incident 6

You see an X509Certificate inside ds:KeyInfo.

Can AcmeHR trust it solely because it was included with the signature?

<details>
<summary>Check your answer</summary>

No.

Trust must come from the configured federation relationship, such as the IdP verification certificate obtained through trusted metadata or explicit configuration.

Otherwise an attacker could sign with their own key and include their own certificate.

</details>

---

# 62. Mini incident 7

A teammate asks for the Okta SAML private key so AcmeHR can verify the signature.

What should you say?

<details>
<summary>Check your answer</summary>

AcmeHR does not need Okta's private signing key.

Okta keeps the private key.

AcmeHR verifies with the corresponding trusted public certificate.

</details>

---

# 63. Mini incident 8

The AcmeHR website uses HTTPS.

A teammate proposes disabling SAML signature validation because TLS already protects traffic.

Is that correct?

<details>
<summary>Check your answer</summary>

No.

HTTPS and the SAML XML signature protect different layers.

The Service Provider must still verify the required SAML signature protection.

</details>

---

# 64. Troubleshooting checklist

When signature validation fails, use this order.

## Check 1: Did the Response reach the ACS?

If not, you are not yet at signature validation.

## Check 2: Which object is signed?

Record:

~~~text
Response

Assertion

both
~~~

## Check 3: Which signing algorithms are present?

Record SignatureMethod and DigestMethod.

## Check 4: Which certificate is active in Okta?

Use the app's SAML Signing Certificates section.

## Check 5: Which certificate does AcmeHR trust?

Check the metadata or verification configuration.

## Check 6: Do the certificates match?

Use concrete certificate evidence such as the fingerprint.

## Check 7: Did someone alter the signed XML?

A changed signed fixture produces a signature failure.

## Check 8: After signature succeeds, what fails next?

Return to the Day 6 validation chain.

Do not stop troubleshooting merely because the signature passed.

---

# 65. The Day 8 engineering habit

When someone says:

> SAML certificate issue.

translate that into testable questions:

~~~text
Which message?

Which signed object?

Which signer?

Which active certificate?

Which trusted certificate?

Which fingerprint?

Which signature algorithm?

What exact verification error?

What is the last step we can prove succeeded?
~~~

That turns a vague certificate problem into evidence.

---

# 66. What Day 9 will add

Today the trust direction is:

~~~text
Okta
    signs SAML Response / Assertion

AcmeHR
    verifies with Okta public certificate
~~~

Day 9 adds the reverse signing direction:

~~~text
AcmeHR
    signs AuthnRequest

Okta
    verifies with AcmeHR public certificate
~~~

Day 9 also adds assertion encryption:

~~~text
Okta
    encrypts for AcmeHR

AcmeHR
    decrypts with AcmeHR private key
~~~

Do not mix those key pairs.

---

# Explain it back

Explain this scenario in your own words:

> Priya authenticates to Okta. Okta returns a SAML Response through her browser. The Assertion contains her employee number and AcmeHR groups. How does AcmeHR know it can trust the signed SAML content?

A complete explanation should include:

1. Okta creates the SAML message
2. Okta owns the private signing key
3. Okta signs the Response, Assertion, or both according to the integration configuration
4. Priya's browser only transports the SAMLResponse
5. AcmeHR has trusted Okta public verification material
6. AcmeHR verifies the XML signature with the SAML library
7. a signature failure stops the SAML authentication
8. a signature success does not replace issuer, audience, endpoint, time, or correlation validation
9. only after all required validation succeeds can AcmeHR create its authenticated application session and use trusted claims

Then explain why AcmeHR must not simply trust a certificate included inside ds:KeyInfo.

---

# Day 8 completion standard

You are ready for the Day 8 lab when you can explain all of these without memorizing a diagram:

~~~text
[ ] I can distinguish authenticity from integrity

[ ] I can explain why signing is not encryption

[ ] I can explain why Base64 is not signature validation

[ ] I know Okta owns the outbound SAML signing private key

[ ] I know AcmeHR receives public verification material

[ ] I can identify Response signing

[ ] I can identify Assertion signing

[ ] I can identify when both are signed

[ ] I can locate ds:Signature in XML

[ ] I can use Reference URI to identify the signed object

[ ] I understand DigestValue at a practical level

[ ] I understand SignatureValue at a practical level

[ ] I know why KeyInfo does not create trust by itself

[ ] I can find SAML Signing Certificates in the Okta app

[ ] I can explain how AcmeHR receives IdP verification certificates through metadata

[ ] I can explain why a valid signature does not replace Audience or other SAML validation

[ ] I can explain why changing signed XML damages the signature

[ ] I can distinguish the IdP signing certificate from the SP request-signing certificate

[ ] I can distinguish a SAML signing certificate from an HTTPS certificate

[ ] I can troubleshoot a wrong trusted certificate from evidence

[ ] I can explain why AcmeHR must never receive Okta's private signing key
~~~

Do not finish Day 8 with:

> Certificates make SAML secure.

Finish when you can identify the signer, verifier, signed object, trusted certificate, and exact failed layer.

---

# Official references used for this lesson

- Okta, Application Integration Wizard SAML field reference:
  https://help.okta.com/oie/en-us/content/topics/apps/aiw-saml-reference.htm

- Okta, Manage signing certificates:
  https://help.okta.com/oie/en-us/Content/Topics/Apps/manage-signing-certificates.htm

- Okta, Understanding SAML:
  https://developer.okta.com/docs/concepts/saml/

- Spring Security 7.1.1, SAML 2.0 Login Overview:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html

- Spring Security 7.1.1, Authenticating SAML Responses:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html

- Spring Security, SAML metadata and verification credentials:
  https://docs.spring.io/spring-security/reference/servlet/saml2/metadata.html

- OASIS, SAML V2.0 Profiles:
  https://docs.oasis-open.org/security/saml/v2.0/saml-profiles-2.0-os.pdf

- OASIS, SAML V2.0 Errata 05:
  https://docs.oasis-open.org/security/saml/v2.0/errata05/os/saml-v2.0-errata05-os.html
