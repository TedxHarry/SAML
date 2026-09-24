# Day 8 Lab: Prove SAML Signing and IdP Certificate Trust

## What you are going to do

The SAML login already works.

Today you will open the signature layer and prove how AcmeHR decides whether the SAML message has the required cryptographic protection.

You will:

- record the current Okta Response and Assertion signing settings
- identify the active Okta SAML signing certificate
- record the signature and digest algorithms
- run a fresh working SAML login
- inspect the live Response locally
- identify whether the Response, Assertion, or both are signed
- use the signature Reference URI to identify the signed object
- compare certificate fingerprints from the live message and IdP metadata
- prove that AcmeHR trusts IdP verification material rather than arbitrary KeyInfo content
- test Assertion-only signing
- test Response-only signing
- prove that both valid placements can work with the current Spring Security training SP
- run the existing unsigned-SAML rejection test
- prove that readable, correctly shaped SAML is still rejected when required signature protection is absent
- restore the exact signing configuration you started with
- document the failure using the course troubleshooting method

The evidence chain is:

~~~text
Okta signing configuration
        |
        v
Okta signing private key
        |
        v
Signed Response / Assertion
        |
        v
Browser carries SAMLResponse
        |
        v
AcmeHR trusted IdP certificate
        |
        v
Spring Security signature validation
        |
        v
Remaining SAML validation
        |
        v
AcmeHR application session
~~~

Do not skip a layer.

---

# Before you start

You need:

- the working Okta SAML application from the earlier labs
- the assigned Okta test user
- the working AcmeHR training SP
- the Day 5 local SAMLResponse decoding workflow
- the Day 6 validation understanding
- the Day 7 known-good application state
- Docker with Docker Compose
- Python 3
- a browser with Developer Tools
- access to the Okta Admin Console

Your current baseline must work before you change any signing setting.

Confirm:

~~~text
SP-initiated login reaches Okta
    PASS

Browser returns to /saml/acs
    PASS

AcmeHR protected page opens
    PASS

AcmeHR application session
    ACTIVE
~~~

Do not troubleshoot Day 8 on top of an already-broken SAML baseline.

---

# Safety rule for this lab

You will inspect:

- a live SAML Response
- an IdP signing certificate
- metadata
- certificate fingerprints

The public signing certificate is not a secret.

The live SAML Assertion can still contain identity data.

Keep the complete Assertion on your own machine.

Do not paste it into:

- public SAML decoders
- forums
- public issues
- shared chats
- unapproved troubleshooting sites

Never ask Okta for the private SAML signing key.

Never copy a private signing key into AcmeHR.

For this trust direction:

~~~text
Okta
    keeps private signing key

AcmeHR
    receives public verification material
~~~

---

# Part 1: Record the exact starting configuration

Before changing anything, create a small recovery note.

Open the AcmeHR Training app in Okta.

Go to:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Sign On
~~~

Open the SAML settings for editing and expand the advanced SAML settings.

Record the current values for:

| Setting | Starting value |
| --- | --- |
| Response |  |
| Assertion Signature |  |
| Signature Algorithm |  |
| Digest Algorithm |  |

Do not rely on memory.

These are the values you must restore at the end of the lab.

---

# Part 2: Find the SAML Signing Certificates section

Stay on the app's Sign On tab.

Find:

~~~text
SAML Signing Certificates
~~~

Current Okta Identity Engine documentation says this section lists the certificates available to the app integration.

Identify the certificate marked active.

Record:

~~~text
Active certificate
    ____________________

Status
    ACTIVE

Validity / expiration shown by Okta
    ____________________
~~~

If Okta displays a certificate fingerprint, record it.

If it does not, we will calculate the fingerprint from metadata locally.

Do not generate a new certificate.

Do not activate an inactive certificate.

Certificate rotation belongs to Day 12.

---

# Part 3: Record the trust direction before touching settings

Write this in your notes:

~~~text
Message
    SAML Response / Assertion

Signer
    Okta

Private key owner
    Okta

Verifier
    AcmeHR

Public verification certificate needed by
    AcmeHR
~~~

If you cannot fill out those five lines correctly, stop here and review the lesson.

This table prevents the most common certificate-direction mistake.

---

# Part 4: Record the current IdP metadata URL

The training SP loads Okta IdP configuration from:

~~~text
IDP_METADATA_URL
~~~

Use the same metadata URL that already works in your AcmeHR environment.

Do not replace it yet.

Record only the URL in your private lab notes.

Do not post tenant-specific metadata URLs publicly.

---

# Part 5: Inspect the certificates published by metadata

We want to answer:

> What public verification certificates does the metadata currently publish?

Create a temporary local file named:

~~~text
inspect_metadata_certs.py
~~~

with:

~~~python
import base64
import hashlib
import urllib.request
import xml.etree.ElementTree as ET

MD = "{urn:oasis:names:tc:SAML:2.0:metadata}"
DS = "{http://www.w3.org/2000/09/xmldsig#}"

url = input("Paste IDP_METADATA_URL: ").strip()
xml = urllib.request.urlopen(url).read()
root = ET.fromstring(xml)

found = 0

for key in root.findall(f".//{MD}KeyDescriptor"):
    cert = key.find(f".//{DS}X509Certificate")
    if cert is None or not cert.text:
        continue

    der = base64.b64decode("".join(cert.text.split()))
    fingerprint = hashlib.sha256(der).hexdigest().upper()
    fingerprint = ":".join(
        fingerprint[i:i + 2]
        for i in range(0, len(fingerprint), 2)
    )

    found += 1
    print(f"Certificate {found}")
    print(f"  use: {key.get('use', 'unspecified')}")
    print(f"  SHA-256: {fingerprint}")

if found == 0:
    print("No X509Certificate values found in KeyDescriptor elements.")
~~~

Run it.

## macOS or Linux

~~~bash
python3 inspect_metadata_certs.py
~~~

## Windows

~~~powershell
py inspect_metadata_certs.py
~~~

If your Windows installation uses python instead of py, use python.

Record every certificate fingerprint returned.

Do not assume the first certificate is automatically the only one that matters.

Metadata can contain more than one key during some trust configurations.

---

# Part 6: Establish a clear temporary Day 8 signing baseline

We want one transaction where both signature locations are easy to inspect.

In the Okta SAML advanced settings, temporarily configure:

~~~text
Response
    Signed

Assertion Signature
    Signed
~~~

Leave these unchanged from the working configuration unless you have a documented reason to test them separately:

~~~text
Signature Algorithm

Digest Algorithm
~~~

Save the SAML configuration.

We are changing only signature placement.

We are not changing:

- ACS
- Audience
- NameID
- claims
- groups
- application assignment
- signing certificate
- authentication policy

That isolation matters.

---

# Part 7: Start a completely fresh transaction

An existing AcmeHR session will not create a new SAML Response.

Close the private/incognito window you used previously.

Open a new private/incognito window.

Open Developer Tools.

Enable:

~~~text
Network
Preserve log
~~~

Then open:

~~~text
http://localhost:8000/protected
~~~

Complete the SAML login.

Confirm:

~~~text
AcmeHR protected page
    OPEN

Application session
    ACTIVE
~~~

This proves the current training SP accepted the new signed transaction.

---

# Part 8: Capture the live SAMLResponse

In the Network trace, find the POST to:

~~~text
http://localhost:8000/saml/acs
~~~

Confirm:

~~~text
HTTP method
    POST

Form field
    SAMLResponse
~~~

Copy only the parsed SAMLResponse form value.

Do not copy cookies.

Do not copy unrelated request headers.

Keep the value local.

---

# Part 9: Inspect signature structure locally

Create a second temporary file:

~~~text
inspect_saml_signature.py
~~~

with:

~~~python
import base64
import hashlib
import xml.etree.ElementTree as ET

DS = "{http://www.w3.org/2000/09/xmldsig#}"
SAML = "{urn:oasis:names:tc:SAML:2.0:assertion}"

value = "".join(
    input("Paste parsed SAMLResponse value: ").strip().split()
)

xml = base64.b64decode(value)
root = ET.fromstring(xml)

def inspect(owner, label):
    signature = owner.find(f"{DS}Signature")

    print()
    print(label)

    if signature is None:
        print("  Signature: NOT PRESENT")
        return

    print("  Signature: PRESENT")

    signed_info = signature.find(f"{DS}SignedInfo")
    reference = None
    signature_method = None
    digest_method = None

    if signed_info is not None:
        reference = signed_info.find(f"{DS}Reference")
        signature_method = signed_info.find(f"{DS}SignatureMethod")

        if reference is not None:
            digest_method = reference.find(f"{DS}DigestMethod")

    print(
        "  Reference URI:",
        reference.get("URI") if reference is not None else "not found"
    )

    print(
        "  SignatureMethod:",
        signature_method.get("Algorithm")
        if signature_method is not None
        else "not found"
    )

    print(
        "  DigestMethod:",
        digest_method.get("Algorithm")
        if digest_method is not None
        else "not found"
    )

    cert = signature.find(f".//{DS}X509Certificate")

    if cert is None or not cert.text:
        print("  Embedded X509Certificate: NOT PRESENT")
        return

    der = base64.b64decode("".join(cert.text.split()))
    fingerprint = hashlib.sha256(der).hexdigest().upper()
    fingerprint = ":".join(
        fingerprint[i:i + 2]
        for i in range(0, len(fingerprint), 2)
    )

    print("  Embedded certificate SHA-256:", fingerprint)

inspect(root, "Response")

assertions = root.findall(f"{SAML}Assertion")

for index, assertion in enumerate(assertions, start=1):
    inspect(assertion, f"Assertion {index}")
~~~

Run it.

## macOS or Linux

~~~bash
python3 inspect_saml_signature.py
~~~

## Windows

~~~powershell
py inspect_saml_signature.py
~~~

This helper does **not** perform cryptographic signature verification.

It only inspects:

- signature location
- Reference URI
- SignatureMethod
- DigestMethod
- embedded certificate fingerprint if present

Spring Security performs the actual cryptographic verification.

---

# Part 10: Prove both signature locations

Because the temporary lab baseline has both signing controls enabled, the inspector should show:

~~~text
Response
    Signature: PRESENT

Assertion 1
    Signature: PRESENT
~~~

Do not stop there.

Record the Reference URI for each signature.

Then find the XML IDs in the decoded message.

Expected relationship:

~~~text
Response signature Reference URI
    points to Response ID

Assertion signature Reference URI
    points to Assertion ID
~~~

This is your evidence for what each signature protects.

---

# Part 11: Record the actual algorithms

From the inspector output, record:

~~~text
Response SignatureMethod
    ____________________

Response DigestMethod
    ____________________

Assertion SignatureMethod
    ____________________

Assertion DigestMethod
    ____________________
~~~

Now compare those values with the Okta advanced SAML settings.

Do not identify the algorithm from memory.

Use the XML generated by the actual transaction.

---

# Part 12: Compare the embedded certificate fingerprint

If the live signature contains X509Certificate under KeyInfo, record its SHA-256 fingerprint from the script.

Compare it with the fingerprint or fingerprints from IdP metadata.

Complete:

| Evidence source | SHA-256 fingerprint | Match? |
| --- | --- | --- |
| Response KeyInfo, if present |  |  |
| Assertion KeyInfo, if present |  |  |
| IdP metadata certificate |  |  |

If the embedded certificate is not present, do not call the transaction broken.

KeyInfo is message evidence.

The configured IdP trust is what AcmeHR relies on.

---

# Part 13: Why fingerprint matching is useful but not the whole trust decision

Suppose the same fingerprint appears in:

~~~text
live SAML KeyInfo

IdP metadata
~~~

That is useful evidence that the message carries the same public certificate material published through the federation configuration.

It does not mean:

> AcmeHR trusted the message because KeyInfo said so.

The trust direction remains:

~~~text
trusted IdP configuration
        |
        v
verification credential
        |
        v
incoming SAML signature checked
~~~

Do not reverse that logic.

---

# Part 14: Prove the application accepted the cryptographic result

The live login succeeded and the protected page opened.

With the current Spring Security training SP, this means the SAML authentication provider accepted the message, including required signature verification and the other SAML validation checks.

Record:

~~~text
Response reached ACS
    PASS

Required signature protection
    PASS

Remaining SAML validation
    PASS

AcmeHR session created
    PASS
~~~

A successful browser login is not proof of only the signature.

It is proof that the complete SAML authentication path accepted the transaction.

---

# Part 15: Controlled experiment 1, Assertion signed only

Now change one thing.

In Okta advanced SAML settings, set:

~~~text
Response
    Unsigned

Assertion Signature
    Signed
~~~

Do not change:

~~~text
Signature Algorithm
Digest Algorithm
Signing certificate
~~~

Save.

Close the current private/incognito window.

Start a fresh private/incognito window.

Run a new SP-initiated login.

---

# Part 16: Prove Assertion-only signing

Capture the new SAMLResponse.

Run:

~~~text
inspect_saml_signature.py
~~~

Expected structure:

~~~text
Response
    Signature: NOT PRESENT

Assertion 1
    Signature: PRESENT
~~~

Now prove the AcmeHR protected page still opens.

For the current Spring Security training SP, an unsigned Response can be accepted when the Assertion or Assertions relied upon are properly signed and all other validation succeeds.

Record:

~~~text
Response signed
    NO

Assertion signed
    YES

SAML authentication
    PASS

AcmeHR application session
    ACTIVE
~~~

Do not generalize this into:

> Every Service Provider accepts Assertion-only signing.

A vendor can require a particular signature placement.

---

# Part 17: Controlled experiment 2, Response signed only

Change one thing again.

Set:

~~~text
Response
    Signed

Assertion Signature
    Unsigned
~~~

Keep the certificate and algorithms unchanged.

Save.

Start another fresh private/incognito transaction.

Capture the new SAMLResponse.

Run the signature inspector.

Expected structure:

~~~text
Response
    Signature: PRESENT

Assertion 1
    Signature: NOT PRESENT
~~~

Now prove the protected page still opens.

For the current Spring Security training SP, a properly signed enclosing Response can provide the required signature protection when the remaining validation also succeeds.

Record:

~~~text
Response signed
    YES

Assertion signed
    NO

SAML authentication
    PASS

AcmeHR application session
    ACTIVE
~~~

Again, this proves the behavior of this training SP.

It does not override another application's documented signing requirement.

---

# Part 18: Compare all three successful transactions

Complete:

| Transaction | Response signed? | Assertion signed? | AcmeHR result |
| --- | --- | --- | --- |
| Both signed | yes | yes | accepted |
| Assertion only | no | yes | accepted |
| Response only | yes | no | accepted |

Now explain why all three can succeed in the current training SP.

Your answer should include:

> Spring Security requires the SAML transaction to have the required signature protection. A signed Response or signed Assertions can satisfy that requirement according to the provider's validation behavior, while all other SAML checks still apply.

Do not say:

> Signatures are optional.

They are not.

---

# Part 19: Restore your original Okta signing settings

Use the recovery note from Part 1.

Restore the exact starting values for:

~~~text
Response

Assertion Signature

Signature Algorithm

Digest Algorithm
~~~

Save.

Do not leave the training app in a temporary experiment state.

Start one more fresh private/incognito transaction.

Confirm the original baseline still works.

---

# Part 20: The deliberate failure will be local, not a live certificate rotation

We still need to prove a rejection.

Do **not** create that failure by:

- activating another Okta certificate
- deleting the active certificate
- replacing metadata with random trust
- rotating a certificate early
- asking for a private key

Those actions belong to later certificate operations work.

Instead, use the existing local training test that sends a SAML Response with:

~~~text
correct-looking SAML structure

but

no Response signature

and

no Assertion signature
~~~

The expected result is rejection.

---

# Part 21: Build the validation-test container

Open a terminal in:

~~~text
lab-sp/
~~~

Run:

~~~bash
docker compose --profile test build validation-tests
~~~

Expected result:

~~~text
validation-tests image
    BUILT
~~~

If the image does not build, fix the local training environment first.

Do not interpret a Docker build error as a SAML signature failure.

---

# Part 22: Run only the unsigned-SAML rejection test

From:

~~~text
lab-sp/
~~~

run:

~~~bash
docker compose --profile test run --rm validation-tests \
  mvn -B -ntp -Dtest=SamlResponseRejectionTests test
~~~

On PowerShell, you can enter the same command on one line:

~~~powershell
docker compose --profile test run --rm validation-tests mvn -B -ntp -Dtest=SamlResponseRejectionTests test
~~~

The important test is:

~~~text
unsignedSamlResponseDoesNotCreateAuthenticatedSession
~~~

Expected Maven result:

~~~text
BUILD SUCCESS
~~~

That does **not** mean the unsigned SAML login succeeded.

It means the automated test successfully proved that AcmeHR rejected the unsigned login.

---

# Part 23: Read what the rejection fixture contains

Open:

~~~text
lab-sp/src/test/java/com/acme/training/acmehr/security/SamlResponseRejectionTests.java
~~~

Find the test:

~~~text
unsignedSamlResponseDoesNotCreateAuthenticatedSession
~~~

Notice that the fixture still contains realistic SAML values such as:

~~~text
Issuer

Destination

NameID

Recipient

Audience

time conditions

AuthnStatement
~~~

But it contains no valid required signature protection.

That is the point of the test.

The SAML can be:

~~~text
readable
well-formed enough to parse
filled with plausible values
~~~

and still be rejected because trust is missing.

---

# Part 24: Prove the expected failure result

The test asserts two important results:

~~~text
authentication
    NOT CREATED

HTTP result
    redirected through failure handling
~~~

Record:

~~~text
SAML reached AcmeHR test endpoint
    YES

Message contained identity-looking values
    YES

Required signature protection
    NO

Authenticated session
    NO
~~~

This is the controlled failure.

---

# Part 25: Find the last step you can prove succeeded

Use the course troubleshooting method.

For the unsigned test:

~~~text
Observed symptom:
SAML authentication is rejected.

Last confirmed successful step:
The SAMLResponse reached the ACS processing path.

First failed trust requirement:
Required signature protection is absent.

Evidence:
Neither the Response nor the Assertion has a valid required signature.

Root cause:
Unsigned SAML cannot satisfy the training SP's signature requirement.

Single change:
Use a valid Okta-signed Response or signed Assertion according to the integration contract.

Proof after change:
The known-good live Okta transaction creates an authenticated AcmeHR session.
~~~

Do not write:

> Certificate issue.

There was no certificate mismatch in this failure.

The failure was absence of required signature protection.

---

# Part 26: Compare unsigned failure with wrong-certificate failure

Today we are directly testing the unsigned case.

A wrong trusted certificate would fail for a different reason.

Compare:

| Case | Signature present? | Correct verification key? | Expected result |
| --- | --- | --- | --- |
| Valid signed SAML | yes | yes | signature can verify |
| Unsigned SAML | no | not applicable to missing signature | reject |
| Wrong trusted certificate | yes | no | reject |

Do not merge the last two into one diagnosis.

They are different failure modes.

The Day 8 implementation work can add more focused certificate-mismatch fixtures without changing the live Okta certificate.

---

# Part 27: Compare signature failure with Audience failure

From Day 6:

~~~text
Valid signature
    can PASS

Audience
    can still FAIL
~~~

From today:

~~~text
Audience value
    can look correct

Signature protection
    can FAIL
~~~

Complete:

| Check | Question |
| --- | --- |
| Signature | Did trusted signing material protect this SAML object? |
| Issuer | Is the message from the expected SAML issuer? |
| Audience | Is the Assertion intended for this SP? |
| Destination | Did the Response target the expected endpoint? |
| Recipient | Is the bearer confirmation for the expected recipient? |
| Time | Is the Assertion currently usable? |
| Correlation | Does the SP-initiated response match the request? |

A SAML login needs the required checks to agree.

---

# Part 28: Prove that decoding did not verify the signature

You successfully decoded the live SAMLResponse before Spring Security's result was discussed.

That proves:

~~~text
Base64 decoding
    can recover XML
~~~

It does not prove:

~~~text
signature
    VALID
~~~

The Python signature inspector also does not prove validity.

It only shows signature structure.

Write this distinction in your notes:

~~~text
Decoded XML
    evidence about message contents

Signature inspector
    evidence about signature placement and metadata

Spring Security acceptance
    evidence that cryptographic and SAML validation succeeded
~~~

---

# Part 29: Prove the browser is not the signer

From the Network trace:

~~~text
Browser
    POSTs SAMLResponse to AcmeHR
~~~

From the trust model:

~~~text
Okta
    owns signing private key
~~~

Therefore:

~~~text
Browser
    transports

Okta
    signs

AcmeHR
    verifies
~~~

Do not describe the browser as signing the Assertion.

Do not describe Priya as signing the Assertion.

---

# Part 30: Prove that KeyInfo alone does not establish trust

If your live XML contains:

~~~xml
<ds:KeyInfo>
    <ds:X509Data>
        <ds:X509Certificate>
            ...
        </ds:X509Certificate>
    </ds:X509Data>
</ds:KeyInfo>
~~~

answer:

> Why does AcmeHR not simply trust whatever certificate is included here?

Expected explanation:

~~~text
An attacker could create their own key pair,
sign their own fake SAML,
and include their own certificate.

Trust therefore has to come from the configured IdP relationship,
not merely from certificate text supplied by the incoming message.
~~~

This is the difference between:

~~~text
certificate present
~~~

and:

~~~text
certificate trusted
~~~

---

# Part 31: Build the Day 8 evidence table

Complete this from your actual lab.

| Question | Your evidence |
| --- | --- |
| What were the original Okta Response signing settings? |  |
| What were the original Assertion signing settings? |  |
| What SignatureMethod did the live XML contain? |  |
| What DigestMethod did the live XML contain? |  |
| What was the active Okta signing certificate? |  |
| What certificate fingerprint did metadata publish? |  |
| Did KeyInfo contain a certificate? |  |
| Did its fingerprint match trusted metadata? |  |
| Did both-signed login work? |  |
| Did Assertion-only login work? |  |
| Did Response-only login work? |  |
| Was unsigned SAML rejected locally? |  |
| Were the original Okta settings restored? |  |

Do not mark a row complete without evidence.

---

# Part 32: Mini challenge, classify the certificate

You are shown this certificate:

~~~text
Certificate A
    configured in Okta SAML Signing Certificates
~~~

Who owns the corresponding private key?

<details>
<summary>Check your answer</summary>

Okta owns the corresponding private signing key for outbound SAML signing.

AcmeHR needs the public verification certificate, not the private key.

</details>

---

# Part 33: Mini challenge, classify the signed object

Your inspector shows:

~~~text
Response
    Signature: NOT PRESENT

Assertion 1
    Signature: PRESENT

Reference URI
    #_assertion123
~~~

What is signed?

<details>
<summary>Check your answer</summary>

The Assertion whose ID is _assertion123 is the signed object.

The outer Response is not separately signed.

</details>

---

# Part 34: Mini challenge, separate trust from placement

The correct Okta certificate is configured, but a vendor requires:

~~~text
Assertion Signature
    Signed
~~~

Your XML contains only a Response signature.

Is the certificate necessarily wrong?

<details>
<summary>Check your answer</summary>

No.

The certificate can be correct while the signature-placement contract is wrong for that vendor.

Check what object the vendor requires to be signed.

</details>

---

# Part 35: Mini challenge, separate transport from message security

A teammate says:

> Our production ACS uses HTTPS, so we do not need SAML signatures.

What is the correction?

<details>
<summary>Check your answer</summary>

HTTPS protects the transport connection.

The SAML signature protects the signed SAML object and lets the SP verify the trusted signer.

They are different controls.

</details>

---

# Part 36: Explain it back

Without looking at the lesson, explain this complete transaction:

~~~text
Priya starts AcmeHR login
        |
        v
Okta authenticates Priya
        |
        v
Okta creates SAMLResponse
        |
        v
Okta signs Response / Assertion
        |
        v
Browser carries the message
        |
        v
AcmeHR uses trusted IdP verification material
        |
        v
Signature verification
        |
        v
Remaining SAML validation
        |
        v
AcmeHR session
~~~

Your explanation must include:

- who owns the signing private key
- who receives the public certificate
- which XML object was signed in your live transaction
- how you proved that from the XML
- where AcmeHR's trusted verification material came from
- why KeyInfo alone is not a trust source
- why decoding is not verification
- why HTTPS does not replace the XML signature
- why a valid signature still does not replace Audience or the other Day 6 checks
- why the unsigned fixture was rejected
- why the successful test command returned BUILD SUCCESS even though the SAML authentication was intentionally rejected

If you cannot explain one of those, return to the evidence from that layer.

---

# Part 37: Restore and prove the known-good state

Before ending the lab, confirm the exact Okta settings from Part 1 are restored.

Then run one fresh SP-initiated login.

Confirm:

~~~text
Original Response setting
    RESTORED

Original Assertion Signature setting
    RESTORED

Original Signature Algorithm
    RESTORED

Original Digest Algorithm
    RESTORED

Original active signing certificate
    UNCHANGED

AcmeHR protected page
    OPENS

AcmeHR application session
    ACTIVE
~~~

Do not continue to Day 9 with an experimental signing configuration still active.

---

# Day 8 lab completion check

Do not mark the lab complete until you can prove:

~~~text
[ ] I recorded the original Okta signing settings before changing them

[ ] I identified the active Okta SAML signing certificate

[ ] I recorded the IdP metadata certificate fingerprint or fingerprints

[ ] I captured a fresh live SAMLResponse

[ ] I identified whether the Response was signed

[ ] I identified whether the Assertion was signed

[ ] I matched each signature Reference URI to the signed XML ID

[ ] I recorded SignatureMethod

[ ] I recorded DigestMethod

[ ] I compared the embedded certificate fingerprint, when present, with metadata

[ ] I can explain why KeyInfo does not establish trust by itself

[ ] I proved a both-signed transaction works

[ ] I proved an Assertion-only signed transaction works with this training SP

[ ] I proved a Response-only signed transaction works with this training SP

[ ] I ran the unsigned-SAML rejection test

[ ] I understand why BUILD SUCCESS means the rejection test passed

[ ] I proved unsigned SAML does not create an authenticated session

[ ] I documented the failure using last-success / first-failure evidence

[ ] I restored the original Okta signing settings

[ ] I proved the restored live SAML login works

[ ] I can explain the complete signer -> certificate -> verifier trust direction
~~~

The lab is not complete because you saw a certificate.

It is complete when you can prove:

~~~text
who signed

what was signed

which key stayed private

which certificate was trusted

how the signature requirement was enforced

what failed when signature protection was absent
~~~

---

# Save your Day 8 evidence

Keep these in your private training notes:

1. original Okta signing settings
2. active signing certificate details
3. metadata SHA-256 fingerprint output
4. one redacted both-signed signature-inspector output
5. one redacted Assertion-only inspector output
6. one redacted Response-only inspector output
7. unsigned rejection test result
8. completed Day 8 evidence table
9. completed troubleshooting record
10. final restored-baseline proof

Do not save:

- Okta passwords
- MFA codes
- session cookies
- production assertions
- private keys

---

# Official references for this lab

- Okta, Application Integration Wizard SAML field reference:
  https://help.okta.com/oie/en-us/content/topics/apps/aiw-saml-reference.htm

- Okta, Manage signing certificates:
  https://help.okta.com/oie/en-us/Content/Topics/Apps/manage-signing-certificates.htm

- Okta, Create SAML app integrations:
  https://help.okta.com/oie/en-us/content/topics/apps/apps_app_integration_wizard_saml.htm

- Spring Security 7.1.1, SAML 2.0 Login Overview:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html

- Spring Security 7.1.1, Authenticating SAML Responses:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html
