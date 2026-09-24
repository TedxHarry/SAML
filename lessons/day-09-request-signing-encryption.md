# Day 9: Signed AuthnRequests, Assertion Encryption, and SP Certificates

## What you should understand by the end of today

Day 8 looked at trust in one direction:

~~~text
Okta
    signs SAML Response / Assertion

AcmeHR
    verifies with Okta public certificate
~~~

Today we add the reverse direction and one new confidentiality control.

The two new questions are:

> How can Okta verify that an AuthnRequest really came from AcmeHR?

and:

> How can Okta send an Assertion that only AcmeHR can decrypt?

By the end of Day 9, you should be able to explain:

- why an SP may sign an AuthnRequest
- which side owns the SP request-signing private key
- which side receives the SP request-signing public certificate
- what Okta's Signature Certificate field is for
- what Okta's Signed Requests setting changes
- why signed AuthnRequests over HTTP-Redirect do not look like signed SAML Responses
- what SigAlg and Signature mean in the Redirect URL
- why RelayState can be covered by the Redirect-binding signature
- why Okta Signed Requests requires NameIDPolicy
- what assertion encryption protects
- which side owns the SP decryption private key
- which side receives the SP encryption public certificate
- what Okta's Encryption Certificate field is for
- what Encryption Algorithm and Key Transport Algorithm mean
- why encryption and signing solve different problems
- what EncryptedAssertion looks like
- how Spring Security uses signing and decryption credentials
- how to troubleshoot a wrong request-signing certificate
- how to troubleshoot a wrong decryption key

Today is not certificate rollover.

That belongs to Day 12.

Today is not a deep XML Encryption lesson.

We will learn what an implementation engineer needs to configure and troubleshoot the normal flow.

---

# 1. Start with the direction of trust

Day 8 used:

~~~text
Okta private signing key
        |
        v
SAML Response / Assertion
        |
        v
AcmeHR verifies with Okta public certificate
~~~

Day 9 adds:

~~~text
AcmeHR private signing key
        |
        v
AuthnRequest
        |
        v
Okta verifies with AcmeHR public certificate
~~~

and:

~~~text
AcmeHR public encryption certificate
        |
        v
Okta encrypts Assertion
        |
        v
Browser carries encrypted data
        |
        v
AcmeHR private decryption key
~~~

Three cryptographic relationships now exist in the same SAML integration.

Do not mix their key ownership.

---

# 2. The Day 9 ownership table

Start with this table before touching any certificate.

| Purpose | Private key owner | Public certificate given to | Message direction |
| --- | --- | --- | --- |
| Okta signs Response / Assertion | Okta | AcmeHR | Okta -> AcmeHR |
| AcmeHR signs AuthnRequest | AcmeHR | Okta | AcmeHR -> Okta |
| Okta encrypts Assertion for AcmeHR | AcmeHR | Okta | Okta -> AcmeHR |

Read the last row carefully.

For encryption:

~~~text
Okta uses AcmeHR public key to encrypt

AcmeHR uses AcmeHR private key to decrypt
~~~

Okta never needs AcmeHR's private decryption key.

---

# 3. Why sign an AuthnRequest?

An AuthnRequest contains instructions from the SP to the IdP.

Examples include:

~~~text
Issuer
Destination
AssertionConsumerServiceURL
ProtocolBinding
NameIDPolicy
ForceAuthn, when used
~~~

When Okta is configured to require signed requests, Okta can verify that the request was signed using the private key corresponding to the trusted AcmeHR public certificate.

That gives Okta evidence about the request's origin and integrity.

---

# 4. A signed AuthnRequest does not authenticate Priya

A signed AuthnRequest answers:

> Did this request come from the SP key I trust, and was the protected request data changed?

It does not answer:

> Is Priya really Priya?

Okta still authenticates Priya.

Keep these separate:

~~~text
SP request authentication
    trust in AcmeHR's request

User authentication
    trust in Priya's identity
~~~

---

# 5. The SP owns the request-signing private key

For signed AuthnRequests:

~~~text
AcmeHR
    owns private signing key

Okta
    receives AcmeHR public certificate
~~~

The private key stays with AcmeHR.

Do not upload the SP request-signing private key into Okta.

Okta needs only the public certificate.

---

# 6. Okta's Signature Certificate field

Current Okta advanced SAML settings include:

~~~text
Signature Certificate
~~~

For signed requests, this is the Service Provider public certificate that Okta uses to validate SAML requests.

It is not:

~~~text
Okta SAML Signing Certificate
~~~

Day 8 and Day 9 use opposite trust directions.

---

# 7. Okta's Signed Requests setting

After a Signature Certificate is uploaded, Okta exposes:

~~~text
Signed Requests
~~~

When enabled, Okta validates SAML requests with that certificate.

An unsigned request no longer satisfies the request contract.

The SP and Okta must agree on the requirement.

---

# 8. Signed Requests changes more than one checkbox

Current Okta documentation states that when Signed Requests is enabled:

- Okta validates SAML requests using the Signature Certificate
- the SAML request must include NameIDPolicy
- Okta dynamically reads SSO URL information from the request
- previously defined static requestable SSO URLs are removed

So this setting is not merely:

> Check the signature too.

It changes which signed request data Okta relies on.

---

# 9. Why NameIDPolicy matters now

Day 4 introduced NameIDPolicy.

Our early training request did not need to depend on it.

Current Okta Signed Requests behavior does.

That means Day 9 implementation needs both:

~~~text
valid request signature
+
NameIDPolicy
~~~

Adding only a signing key is not enough.

---

# 10. Our current AuthnRequest uses HTTP-Redirect

AcmeHR currently sends its AuthnRequest using:

~~~text
HTTP-Redirect
~~~

That matters because SAML request signing is binding-dependent.

A signed Redirect request does not look like the signed Response we studied on Day 8.

---

# 11. Do not search only for ds:Signature

With HTTP-Redirect, the request is:

~~~text
DEFLATE compressed
        |
        v
Base64 encoded
        |
        v
URL encoded
        |
        v
sent as SAMLRequest
~~~

When the Redirect binding is signed, the browser URL contains:

~~~text
SAMLRequest=...

SigAlg=...

Signature=...
~~~

and possibly:

~~~text
RelayState=...
~~~

The decoded AuthnRequest XML can have no embedded ds:Signature and still be a signed Redirect request.

---

# 12. The Redirect signature covers the binding input

At a practical level, the signature input is built from:

~~~text
SAMLRequest

RelayState, if present

SigAlg
~~~

in the binding-defined order.

The result is carried in:

~~~text
Signature
~~~

Do not copy only the decoded XML and expect to reproduce the Redirect signature check.

---

# 13. SigAlg and Signature have different jobs

SigAlg identifies the signing algorithm.

Signature carries the actual signature value.

Simplified:

~~~text
AcmeHR private signing key
        |
        v
sign Redirect request input
        |
        v
Signature parameter

SigAlg
    tells Okta which algorithm applies
~~~

Current Spring Security documentation says RSA-SHA256 is the default AuthnRequest signing algorithm unless asserting-party metadata indicates another supported algorithm.

---

# 14. RelayState can be covered too

If RelayState is present, it is part of the Redirect-binding signing input.

That means RelayState is not always an unrelated value appended after signing.

Changing one of the signed query values can break verification.

Day 11 will cover RelayState behavior in more depth.

---

# 15. URL encoding matters

Redirect-binding verification uses the original URL-encoded values.

Different legal encodings can produce different byte sequences.

That is why the SAML library should perform the signing and verification work.

Do not implement Redirect signatures with custom string manipulation.

---

# 16. What Spring Security needs for request signing

For AcmeHR to sign requests, its RelyingPartyRegistration needs an SP-owned signing credential:

~~~text
private key
+
corresponding X.509 certificate
~~~

Conceptually:

~~~text
RelyingPartyRegistration
    |
    v
signingX509Credentials
    |
    + private key
    + public certificate
~~~

The private key signs.

The public certificate is shared with Okta.

---

# 17. The asserting party's request-signing expectation matters

Spring Security tracks whether the asserting party wants AuthnRequests signed.

If the IdP does not require signing, the registration can indicate that.

If the IdP requires signed requests, AcmeHR needs a signing credential and must generate a valid signature.

Our Day 8 baseline does not yet configure an AcmeHR signing key.

Day 9 implementation will add it deliberately.

---

# 18. Prove the request instead of trusting configuration

After request signing is implemented, browser evidence should include:

~~~text
SAMLRequest
SigAlg
Signature
~~~

Decoded request evidence should include:

~~~text
Issuer
Destination
AssertionConsumerServiceURL
ProtocolBinding
NameIDPolicy
~~~

Both views matter.

The XML tells you what AcmeHR asked for.

The URL shows the Redirect-binding signature evidence.

---

# 19. Wrong request-signing certificate

Suppose:

~~~text
AcmeHR signs with private key A

Okta trusts public certificate B
~~~

Expected path:

~~~text
AcmeHR generates request
    PASS

Browser reaches Okta
    PASS

Okta verifies request signature
    FAIL

User authentication
    may not begin
~~~

Do not change Response signing, Audience, or claims to fix this layer.

---

# 20. Signed request trust does not replace access policy

A valid request signature does not mean:

~~~text
user is assigned
MFA is satisfied
authentication policy allows access
~~~

Those are separate Okta decisions.

Request signing authenticates the SP request.

It does not grant the human user access.

---

# 21. Now add assertion encryption

Signing protects integrity and signer authenticity.

Encryption answers a different question:

> Can someone who sees the SAML Response read the Assertion contents?

Without Assertion Encryption, a Base64-decoded Response normally exposes the Assertion XML.

With Assertion Encryption, the Assertion content is encrypted for AcmeHR.

---

# 22. What an EncryptedAssertion looks like

Unencrypted:

~~~xml
<samlp:Response>
    <saml:Assertion>
        <saml:Subject>...</saml:Subject>
        <saml:AttributeStatement>...</saml:AttributeStatement>
    </saml:Assertion>
</samlp:Response>
~~~

Encrypted:

~~~xml
<samlp:Response>
    <saml:EncryptedAssertion>
        <xenc:EncryptedData>
            ...
        </xenc:EncryptedData>
    </saml:EncryptedAssertion>
</samlp:Response>
~~~

The outer Response remains XML.

The Assertion itself is no longer readable without the correct decryption key.

---

# 23. Base64 decoding is not decryption

The browser still carries SAMLResponse.

You can Base64-decode the outer value.

After decoding, you may see:

~~~text
EncryptedAssertion
CipherData
EncryptedKey
~~~

instead of:

~~~text
NameID
department
groups
~~~

Base64 removed transport encoding.

It did not decrypt the Assertion.

---

# 24. Encryption is not signing

Signing asks:

~~~text
Who signed this?

Was signed content changed?
~~~

Encryption asks:

~~~text
Who can read this?
~~~

An encrypted Assertion is not automatically trusted.

A signed Assertion is not automatically confidential.

A design can require one or both.

---

# 25. The SP owns the decryption private key

For assertion encryption:

~~~text
AcmeHR
    owns decryption private key

Okta
    receives AcmeHR public encryption certificate
~~~

Okta encrypts for AcmeHR.

AcmeHR decrypts.

Never upload AcmeHR's private decryption key to Okta.

---

# 26. Okta's Encryption Certificate field

When Assertion Encryption is set to Encrypted, current Okta settings expose:

~~~text
Encryption Certificate
~~~

This contains the Service Provider public certificate used to encrypt the Assertion.

The corresponding private key stays with AcmeHR.

---

# 27. Signature Certificate and Encryption Certificate are different roles

Okta can show both:

~~~text
Signature Certificate

Encryption Certificate
~~~

For Day 9:

~~~text
Signature Certificate
    AcmeHR public certificate
    Okta verifies AcmeHR-signed requests

Encryption Certificate
    AcmeHR public certificate
    Okta encrypts Assertions for AcmeHR
~~~

An implementation can technically use one key pair for multiple roles.

That does not make signing and encryption the same operation.

Always label the purpose.

---

# 28. Encryption Algorithm and Key Transport Algorithm

Okta exposes both when assertion encryption is enabled.

At a practical level:

~~~text
Encryption Algorithm
    encrypts Assertion content

Key Transport Algorithm
    protects the temporary content-encryption key for AcmeHR
~~~

These are different jobs.

Do not diagnose them as one field.

---

# 29. Why two encryption algorithms appear

Large Assertion XML is normally protected with a symmetric content-encryption key.

That temporary key then needs to be protected for AcmeHR using its public-key material.

Simplified:

~~~text
Assertion
    |
    v
symmetric content encryption
    |
    v
ciphertext

temporary content key
    |
    v
protected for AcmeHR public key
    |
    v
EncryptedKey
~~~

The library performs this.

Do not implement XML Encryption yourself.

---

# 30. Spring Security's decryption credential

Spring Security registers SP-owned decryption material in the RelyingPartyRegistration.

Conceptually:

~~~text
decryptionX509Credentials
    |
    + AcmeHR private key
    + AcmeHR certificate
~~~

When EncryptedAssertion arrives, Spring Security uses the matching private key to decrypt it.

---

# 31. Spring Security processing order helps diagnosis

At a high level, the current processing sequence includes:

~~~text
Receive Response
        |
        v
Check Response signature
        |
        v
Decrypt EncryptedAssertion
        |
        v
Validate Response
        |
        v
Check Assertion signature
        |
        v
Decrypt encrypted Assertion elements if present
        |
        v
Validate Assertion
        |
        v
Create authenticated principal
~~~

If EncryptedAssertion decryption fails, AcmeHR cannot continue to normal Assertion validation.

---

# 32. Encryption does not remove the signature requirement

Suppose decryption succeeds.

That only proves AcmeHR could recover the encrypted content.

It does not prove trusted Okta created it.

Keep the controls separate:

~~~text
Encryption
    confidentiality

Signature
    integrity and signer verification
~~~

Normal SAML validation still follows.

---

# 33. Signed and encrypted Assertion

When an Assertion is both signed and encrypted, the conceptual order is:

~~~text
Create Assertion
        |
        v
Sign Assertion
        |
        v
Encrypt signed Assertion
        |
        v
Send EncryptedAssertion
~~~

AcmeHR processes in the reverse protection order:

~~~text
decrypt
        |
        v
verify signature
~~~

Do not try to inspect an inner Assertion signature before the Assertion has been decrypted.

---

# 34. The outer Response can remain readable and signed

You can see:

~~~text
Response
    readable
    ds:Signature visible

EncryptedAssertion
    ciphertext
~~~

Assertion encryption does not mean every part of the Response is hidden.

That distinction is useful when collecting browser evidence.

---

# 35. Wrong decryption key

Suppose:

~~~text
Okta encrypts using public certificate A

AcmeHR tries to decrypt with private key B
~~~

Expected path:

~~~text
Browser reaches ACS
    PASS

EncryptedAssertion received
    PASS

Assertion decryption
    FAIL

Assertion validation
    NOT REACHED

Application session
    NOT CREATED
~~~

Do not change NameID, Audience, or claims before fixing decryption.

---

# 36. Wrong encryption certificate

Suppose AcmeHR now owns key pair B, but Okta still encrypts using old public certificate A.

If AcmeHR no longer has matching private key A:

~~~text
Okta encryption
    succeeds

browser transport
    succeeds

AcmeHR decryption
    fails
~~~

This is an SP encryption-key mismatch.

It is not an IdP signing-certificate problem.

---

# 37. Request signing and decryption fail on opposite sides

Request-signature failure:

~~~text
AcmeHR -> Okta

Okta is verifier
~~~

Decryption failure:

~~~text
Okta -> AcmeHR

AcmeHR is decryptor
~~~

Ask:

> Who is failing to process which message?

That quickly narrows the certificate role.

---

# 38. Complete certificate ownership table

By Day 9:

| Certificate purpose | Public certificate location | Private key location | Operation |
| --- | --- | --- | --- |
| IdP SAML signing | AcmeHR trust | Okta | AcmeHR verifies Response / Assertion |
| SP request signing | Okta Signature Certificate | AcmeHR | Okta verifies AuthnRequest |
| SP assertion encryption | Okta Encryption Certificate | AcmeHR | Okta encrypts, AcmeHR decrypts |
| HTTPS server | browser / TLS trust | web server or TLS termination | protects transport |

These can all be X.509 certificates.

They are not interchangeable.

---

# 39. Certificate filename is weak evidence

Files named:

~~~text
saml.crt
app.pem
okta.cer
sp-cert.pem
~~~

do not prove purpose.

Instead ask:

~~~text
Who owns the private key?

Who has the public certificate?

What operation uses it?

Which message direction?
~~~

Then identify the role.

---

# 40. SP metadata can publish SP public keys

SAML metadata can advertise Service Provider public key material for purposes such as:

~~~text
signing
encryption
~~~

Spring Security can publish relying-party metadata from the RelyingPartyRegistration.

Day 9 lab work will compare published AcmeHR key information with what Okta is configured to trust and use.

---

# 41. KeyDescriptor use matters

A metadata KeyDescriptor can identify a key as:

~~~text
signing
encryption
~~~

or omit the use value.

Do not grab the first certificate and assume its role.

Read the surrounding metadata.

---

# 42. Signed Requests can make request endpoint data authoritative

With Okta Signed Requests enabled, Okta reads SSO URL information dynamically from the signed request.

That means the request signature protects more than a label saying:

> AcmeHR sent this.

It protects request-provided protocol data that Okta may rely on.

Understand the generated request before enabling the setting.

---

# 43. A valid signed request can still fail later

Examples:

~~~text
Request signature
    PASS

NameIDPolicy
    missing
~~~

or:

~~~text
Request signature
    PASS

user assignment
    FAIL
~~~

or:

~~~text
Request signature
    PASS

authentication policy
    DENY
~~~

A valid request signature proves one layer.

It does not make the full sign-in successful.

---

# 44. Successful decryption can still fail later

Example:

~~~text
EncryptedAssertion decryption
    PASS

Assertion signature
    PASS

Audience
    FAIL
~~~

The login still fails.

Decryption makes the Assertion readable.

It does not make the Assertion acceptable.

---

# 45. Encryption failure can hide later failures

Suppose the encrypted Assertion also has a wrong Audience.

If AcmeHR cannot decrypt it, Audience validation cannot become the useful first failure.

Fix:

~~~text
decryption
~~~

then run a new transaction.

Only after decryption succeeds can later validation evidence matter.

---

# 46. Do not break several certificate paths at once

Bad experiment:

~~~text
wrong request-signing certificate
+
wrong encryption certificate
+
wrong Audience
~~~

Good sequence:

~~~text
Experiment 1
    request-signing mismatch

restore

Experiment 2
    decryption mismatch

restore

Experiment 3
    semantic SAML validation
~~~

One failed layer at a time.

---

# 47. Request-signing evidence checklist

For our signed Redirect request, collect:

~~~text
SAMLRequest
    present

SigAlg
    present

Signature
    present

RelayState
    expected state

decoded Issuer
    expected

Destination
    expected

ACS
    expected

NameIDPolicy
    present

Okta Signature Certificate
    matches AcmeHR signing key pair
~~~

Do not use absence of ds:Signature in decoded XML as proof of failure.

---

# 48. Encryption evidence checklist

For encrypted Assertion:

~~~text
SAMLResponse reaches ACS
    yes/no

outer Response decodes
    yes/no

EncryptedAssertion present
    yes/no

Okta Encryption Certificate
    fingerprint / source

AcmeHR decryption credential
    fingerprint / source

matching private key available
    yes/no

decryption
    pass/fail

signature validation
    pass/fail

Audience and other validation
    pass/fail
~~~

Keep the whole chain visible.

---

# 49. Wrong request-signing certificate troubleshooting record

~~~text
Observed symptom:
SP-initiated login reaches Okta but the request is rejected.

Last confirmed successful step:
AcmeHR generated a Redirect AuthnRequest with SAMLRequest, SigAlg, and Signature.

First failed step:
Okta request-signature validation.

Evidence:
AcmeHR signs using key pair A while Okta trusts certificate B.

Root cause:
Okta's public verification certificate does not correspond to AcmeHR's signing private key.

Single change:
Configure the matching AcmeHR public signing certificate.

Proof after change:
A fresh signed request is accepted and Okta continues the login flow.
~~~

---

# 50. Wrong decryption key troubleshooting record

~~~text
Observed symptom:
Okta authentication succeeds, but AcmeHR cannot finish SAML login.

Last confirmed successful step:
Browser POSTed a Response containing EncryptedAssertion.

First failed step:
EncryptedAssertion decryption.

Evidence:
Okta encrypts using certificate A while AcmeHR uses private key B.

Root cause:
AcmeHR's private decryption key does not match the public encryption certificate configured in Okta.

Single change:
Restore the matching AcmeHR decryption credential.

Proof after change:
A fresh encrypted transaction decrypts and continues through normal validation.
~~~

---

# 51. Mini incident 1

Decoded AuthnRequest XML contains no ds:Signature.

The browser URL contains:

~~~text
SAMLRequest
SigAlg
Signature
~~~

Is the request unsigned?

<details>
<summary>Check your answer</summary>

No.

With HTTP-Redirect, the signature is carried at the binding level through the query-string signature parameters.

</details>

---

# 52. Mini incident 2

Okta Signed Requests is enabled.

AcmeHR sends a cryptographically valid signed request without NameIDPolicy.

What should you investigate?

<details>
<summary>Check your answer</summary>

The request contract.

Current Okta documentation requires NameIDPolicy when Signed Requests is enabled.

Do not rotate the signing certificate first.

</details>

---

# 53. Mini incident 3

Okta asks for Signature Certificate.

A teammate uploads Okta's own SAML signing certificate.

Correct?

<details>
<summary>Check your answer</summary>

No.

For Signed Requests, Okta needs AcmeHR's public signing certificate.

Okta's SAML signing certificate belongs to the opposite Response / Assertion trust direction.

</details>

---

# 54. Mini incident 4

Okta asks for Encryption Certificate.

What should AcmeHR supply?

<details>
<summary>Check your answer</summary>

AcmeHR supplies the public encryption certificate.

The corresponding private decryption key stays with AcmeHR.

</details>

---

# 55. Mini incident 5

You Base64-decode SAMLResponse and see EncryptedAssertion but cannot see NameID.

Is Base64 broken?

<details>
<summary>Check your answer</summary>

No.

The transport encoding was removed successfully.

The Assertion itself still requires cryptographic decryption.

</details>

---

# 56. Mini incident 6

AcmeHR decrypts successfully and then rejects Audience.

Did encryption work?

<details>
<summary>Check your answer</summary>

Yes.

Decryption passed.

Audience validation failed later.

</details>

---

# 57. Mini incident 7

The same AcmeHR key pair is configured for signing and decryption.

Does that make signing and encryption the same operation?

<details>
<summary>Check your answer</summary>

No.

A library can reuse a key pair for more than one credential role.

The purpose and message direction remain different.

</details>

---

# 58. Mini incident 8

The request signature verifies but Priya is not assigned to AcmeHR.

Should Okta allow access because the request is trusted?

<details>
<summary>Check your answer</summary>

No.

Request signature validation and user assignment are separate decisions.

</details>

---

# 59. Mini incident 9

Okta encrypts with the correct AcmeHR certificate, but AcmeHR trusts the wrong Okta Response-signing certificate.

What can happen?

<details>
<summary>Check your answer</summary>

Decryption can succeed and signature verification can still fail.

Those are separate trust relationships.

</details>

---

# 60. Mini incident 10

A teammate proposes fixing decryption by disabling Assertion Encryption permanently.

What is missing?

<details>
<summary>Check your answer</summary>

That removes the security requirement instead of diagnosing it.

Compare the public Encryption Certificate configured in Okta with the private decryption credential configured in AcmeHR.

</details>

---

# 61. Day 9 troubleshooting sequence

For request signing:

~~~text
1. Did AcmeHR generate AuthnRequest?

2. Which binding carried it?

3. For Redirect, are SAMLRequest, SigAlg, and Signature present?

4. Does decoded XML contain expected Issuer, Destination, ACS, and NameIDPolicy?

5. Which AcmeHR private key signed?

6. Which public Signature Certificate does Okta trust?

7. Did Okta accept the request?

8. What fails next?
~~~

For encryption:

~~~text
1. Did Okta authenticate the user?

2. Did browser POST SAMLResponse?

3. Does outer XML contain EncryptedAssertion?

4. Which public Encryption Certificate did Okta use?

5. Does AcmeHR have the matching private key?

6. Did decryption succeed?

7. Did signature validation then succeed?

8. What fails next?
~~~

---

# 62. The Day 9 engineering habit

When someone says:

> SP certificate problem.

translate it into:

~~~text
Which SP certificate?

Request signing or encryption?

What message direction?

Who owns the private key?

Who has the public certificate?

Which operation failed?

What is the last step we can prove succeeded?
~~~

Never troubleshoot a certificate only by filename.

Troubleshoot by purpose and direction.

---

# 63. What Day 10 will add

Day 9 is about protocol trust and confidentiality.

Day 10 moves to:

~~~text
application assignment
authentication policy
MFA
JIT
SCIM boundary
~~~

A signed and decrypted SAML transaction can still be followed by a separate access or lifecycle failure.

Keep those layers separate.

---

# Explain it back

Explain this scenario in your own words:

> Priya opens AcmeHR. AcmeHR sends a signed AuthnRequest to Okta using HTTP-Redirect. Okta verifies the request, authenticates Priya, encrypts the signed Assertion for AcmeHR, and returns the SAMLResponse. AcmeHR decrypts and validates it.

A complete explanation should include:

1. AcmeHR owns the AuthnRequest signing private key
2. Okta receives AcmeHR's public Signature Certificate
3. the Redirect request carries SigAlg and Signature query parameters
4. no ds:Signature in decoded Redirect XML does not prove the request is unsigned
5. Okta Signed Requests requires NameIDPolicy
6. Okta can rely on signed request endpoint information
7. AcmeHR owns the decryption private key
8. Okta receives AcmeHR's public Encryption Certificate
9. Base64 decoding does not decrypt EncryptedAssertion
10. AcmeHR decrypts using its private key
11. decryption does not replace signature validation
12. signature validation does not replace Audience, endpoint, time, or correlation validation
13. Okta's signing certificate, AcmeHR request-signing certificate, and AcmeHR encryption certificate have different purposes
14. private keys stay with their owners

Then explain how the last successful step distinguishes:

~~~text
wrong request-signing certificate
~~~

from:

~~~text
wrong decryption key
~~~

---

# Day 9 completion standard

You are ready for the Day 9 lab when you can explain:

~~~text
[ ] why an SP signs an AuthnRequest

[ ] AcmeHR owns the request-signing private key

[ ] Okta receives AcmeHR's public Signature Certificate

[ ] what Okta Signed Requests changes

[ ] Signed Requests requires NameIDPolicy

[ ] our AuthnRequest uses HTTP-Redirect

[ ] Redirect request signing uses SigAlg and Signature query parameters

[ ] why ds:Signature may be absent from decoded Redirect AuthnRequest XML

[ ] RelayState can be part of the Redirect signature input

[ ] the SAML library must perform request signing

[ ] how a wrong SP request-signing certificate fails

[ ] what Assertion Encryption protects

[ ] AcmeHR owns the decryption private key

[ ] Okta receives AcmeHR's public Encryption Certificate

[ ] Encryption Algorithm and Key Transport Algorithm are different

[ ] how to identify EncryptedAssertion

[ ] Base64 decoding is not decryption

[ ] Spring Security uses decryption credentials from RelyingPartyRegistration

[ ] decryption does not replace signature validation

[ ] signature validation does not replace normal SAML validation

[ ] how a wrong decryption key fails

[ ] how to fill out the complete certificate ownership table

[ ] how to distinguish IdP signing from both SP-owned certificate roles

[ ] public certificates can be exchanged but private keys stay with their owner

[ ] how to identify the last successful step before request-signature or decryption failure
~~~

Do not finish Day 9 with:

> We need three certificates.

Finish when you can explain:

~~~text
what each certificate is for

who owns its private key

who receives its public certificate

which message operation uses it

where failure appears when the relationship is wrong
~~~

---

# Official references used for this lesson

- Okta, Application Integration Wizard SAML field reference:
  https://help.okta.com/oie/en-us/content/topics/apps/aiw-saml-reference.htm

- Okta, Create SAML app integrations:
  https://help.okta.com/oie/en-us/Content/Topics/Apps/Apps_App_Integration_Wizard_SAML.htm

- Spring Security 7.1.1, SAML 2.0 Login Overview:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html

- Spring Security, Producing AuthnRequests:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication-requests.html

- Spring Security, Authenticating SAML Responses and decryption:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html

- Spring Security, SAML 2.0 Metadata:
  https://docs.spring.io/spring-security/reference/servlet/saml2/metadata.html

- OASIS, Bindings for the OASIS Security Assertion Markup Language (SAML) V2.0:
  https://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf

- OASIS, SAML V2.0 Technical Overview:
  https://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html

- OASIS, SAML V2.0 Errata:
  https://docs.oasis-open.org/security/saml/v2.0/errata05/os/saml-v2.0-errata05-os.html
