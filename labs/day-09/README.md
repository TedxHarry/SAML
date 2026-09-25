# Day 9 Lab: Sign the AuthnRequest and Encrypt the Assertion

## What you are going to do

Day 8 proved that AcmeHR can verify SAML signed by Okta.

Day 9 adds two SP-owned key relationships:

~~~text
AcmeHR signs AuthnRequest
    ->
Okta verifies it

Okta encrypts Assertion
    ->
AcmeHR decrypts it
~~~

In this lab you will:

- prove the current pre-Day-9 baseline first
- wait for the Day 9 implementation checkpoint before enabling Okta features
- identify the AcmeHR request-signing public certificate
- identify the AcmeHR encryption public certificate
- inspect SP metadata for signing and encryption keys
- upload only public certificates to Okta
- enable Signed Requests
- prove the Redirect AuthnRequest contains SAMLRequest, SigAlg, and Signature
- prove NameIDPolicy is present
- prove Okta accepts the signed request
- enable Assertion Encryption
- capture an encrypted SAMLResponse
- prove EncryptedAssertion is present
- prove AcmeHR decrypts it and still completes normal SAML validation
- run local negative tests for wrong request-signing trust
- run local negative tests for a wrong decryption key
- document both failures from evidence
- prove that no private key crossed the federation boundary

The final path is:

~~~text
AcmeHR private signing key
        |
        v
signed Redirect AuthnRequest
        |
        v
Okta verifies with AcmeHR public signing certificate
        |
        v
Okta authenticates Priya
        |
        v
Okta encrypts Assertion
        |
        v
Okta signs outer Response
        |
        v
browser POSTs SAMLResponse
        |
        v
AcmeHR verifies Response signature
        |
        v
AcmeHR decrypts with AcmeHR private decryption key
        |
        v
normal SAML validation
        |
        v
AcmeHR application session
~~~

---

# Before you start

You need:

- the completed Day 8 lesson and lab
- the working AcmeHR Training Okta app
- the assigned test user
- Docker with Docker Compose
- browser Developer Tools
- access to the Okta Admin Console
- the Day 9 implementation checkpoint in this repository before the live configuration steps

Your Day 8 baseline must already work:

~~~text
SP-initiated login
    PASS

Okta authentication
    PASS

SAMLResponse reaches /saml/acs
    PASS

AcmeHR SAML validation
    PASS

AcmeHR application session
    ACTIVE
~~~

Do not add request signing or encryption to an already-broken integration.

---

# Safety rule for Day 9

Day 9 introduces SP private keys.

The rule is:

~~~text
public certificate
    can be exchanged

private key
    stays with its owner
~~~

Never upload an AcmeHR private key to Okta.

Never paste a private key into:

- a forum
- a ticket
- a chat
- a public issue
- a browser decoder
- an unapproved troubleshooting site

Any key created specifically for this training lab is disposable lab material.

Never reuse a training private key for a real application, production environment, VPN, TLS, SSH, or code signing.

---

# Part 1: Write the certificate ownership table first

Complete this before configuration:

| Purpose | Private key owner | Public certificate goes to | Operation |
| --- | --- | --- | --- |
| Okta Response / Assertion signing | Okta | AcmeHR | AcmeHR verifies |
| AcmeHR AuthnRequest signing | AcmeHR | Okta | Okta verifies |
| AcmeHR Assertion decryption | AcmeHR | Okta | Okta encrypts, AcmeHR decrypts |

If any row is unclear, stop and review the Day 9 lesson.

Do not guess which certificate belongs in an Okta field.

---

# Part 2: Prove the pre-Day-9 repository baseline

Before Day 9 implementation, the current training SP has:

~~~text
IdP verification credential
    YES

SP request-signing credential
    NO

SP decryption credential
    NO
~~~

The current test IdP metadata says:

~~~text
WantAuthnRequestsSigned="false"
~~~

The current Day 4 AuthnRequest test expects:

~~~text
NameIDPolicy
    ABSENT
~~~

That is the correct starting point.

Day 9 intentionally changes these areas.

---

# Part 3: Do not enable Okta features before AcmeHR is ready

Do not enable:

~~~text
Signed Requests
~~~

or:

~~~text
Assertion Encryption
    Encrypted
~~~

until the Day 9 implementation checkpoint is present.

If Okta requires a signed request before AcmeHR can sign it, login can stop at Okta.

If Okta encrypts the Assertion before AcmeHR has the matching decryption private key, the Response can reach AcmeHR but authentication cannot continue.

Configuration order matters.

---

# Part 4: Day 9 implementation checkpoint

Before the live Okta work, the repository must prove:

~~~text
[ ] AcmeHR has an SP signing credential

[ ] AcmeHR has an SP decryption credential

[ ] credentials load only when Day 9 key material is configured

[ ] AuthnRequest still uses HTTP-Redirect

[ ] signed Redirect request contains SigAlg

[ ] signed Redirect request contains Signature

[ ] AuthnRequest contains NameIDPolicy

[ ] SP metadata advertises signing public-key material

[ ] SP metadata advertises encryption public-key material

[ ] private keys never appear in metadata or learner output

[ ] matching request-signing trust succeeds locally

[ ] different request verification certificate fails locally

[ ] matching Assertion decryption succeeds locally

[ ] different decryption private key fails locally
~~~

If these checks are not yet present, stop the live lab here.

Do not weaken Okta or Spring Security to continue.

---

# Part 5: Keep the two SP key roles separate

Even if one implementation can reuse a key pair, keep two logical roles in your notes:

~~~text
AcmeHR request-signing credential

AcmeHR assertion-decryption credential
~~~

Record:

| Role | Public certificate source | Private key source | SHA-256 fingerprint |
| --- | --- | --- | --- |
| Request signing |  |  |  |
| Assertion encryption / decryption |  |  |  |

A filename does not define a certificate role.

Purpose and key ownership do.

## Create disposable Day 9 credentials outside the repository

The live Docker lab needs real SP-owned key pairs.

Do not put them under the cloned SAML repository.

Use a private training directory in your home folder.

The commands below create two separate RSA key pairs:

~~~text
request-signing key pair

assertion-encryption / decryption key pair
~~~

Keeping them separate makes the certificate roles visible while you learn.

These are disposable training credentials.

Do not reuse them for any real application.

### macOS or Linux

Confirm OpenSSL is available:

~~~bash
openssl version
~~~

Create the private directory and both key pairs:

~~~bash
CRED_DIR="$HOME/.acmehr-training/day9"

mkdir -p "$CRED_DIR"
chmod 700 "$CRED_DIR"

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "$CRED_DIR/sp-signing-private-key.pem"

openssl req \
  -new \
  -x509 \
  -sha256 \
  -days 30 \
  -key "$CRED_DIR/sp-signing-private-key.pem" \
  -subj "/CN=AcmeHR Day 9 Request Signing" \
  -out "$CRED_DIR/sp-signing-certificate.pem"

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "$CRED_DIR/sp-decryption-private-key.pem"

openssl req \
  -new \
  -x509 \
  -sha256 \
  -days 30 \
  -key "$CRED_DIR/sp-decryption-private-key.pem" \
  -subj "/CN=AcmeHR Day 9 Assertion Encryption" \
  -out "$CRED_DIR/sp-decryption-certificate.pem"

chmod 600 \
  "$CRED_DIR/sp-signing-private-key.pem" \
  "$CRED_DIR/sp-decryption-private-key.pem"

chmod 644 \
  "$CRED_DIR/sp-signing-certificate.pem" \
  "$CRED_DIR/sp-decryption-certificate.pem"
~~~

### Windows PowerShell

Confirm OpenSSL is available:

~~~powershell
openssl version
~~~

Create the private directory and both key pairs:

~~~powershell
$CRED_DIR = Join-Path $HOME ".acmehr-training\day9"

New-Item -ItemType Directory -Force -Path $CRED_DIR | Out-Null

openssl genpkey `
  -algorithm RSA `
  -pkeyopt rsa_keygen_bits:2048 `
  -out (Join-Path $CRED_DIR "sp-signing-private-key.pem")

openssl req `
  -new `
  -x509 `
  -sha256 `
  -days 30 `
  -key (Join-Path $CRED_DIR "sp-signing-private-key.pem") `
  -subj "/CN=AcmeHR Day 9 Request Signing" `
  -out (Join-Path $CRED_DIR "sp-signing-certificate.pem")

openssl genpkey `
  -algorithm RSA `
  -pkeyopt rsa_keygen_bits:2048 `
  -out (Join-Path $CRED_DIR "sp-decryption-private-key.pem")

openssl req `
  -new `
  -x509 `
  -sha256 `
  -days 30 `
  -key (Join-Path $CRED_DIR "sp-decryption-private-key.pem") `
  -subj "/CN=AcmeHR Day 9 Assertion Encryption" `
  -out (Join-Path $CRED_DIR "sp-decryption-certificate.pem")
~~~

If `openssl version` is not available, install an OpenSSL CLI for your workstation before continuing.

Do not substitute the repository's committed synthetic test private keys for the live Okta exercise.

Those keys are public test fixtures and provide no secrecy.

## Confirm the private-key format

The Day 9 SP loader expects an unencrypted PKCS#8 RSA private key.

The generated files should start with:

~~~text
-----BEGIN PRIVATE KEY-----
~~~

They should not start with:

~~~text
-----BEGIN RSA PRIVATE KEY-----
~~~

Do not paste the private-key content into your notes.

## Point Spring at paths inside the container

The host directory will be mounted inside the SP container at:

~~~text
/run/acmehr-day9
~~~

Spring must therefore receive the **container paths**, not your host paths.

### macOS or Linux

From the `lab-sp` directory:

~~~bash
export ACMEHR_SAML_SP_SIGNING_PRIVATE_KEY_LOCATION="file:/run/acmehr-day9/sp-signing-private-key.pem"
export ACMEHR_SAML_SP_SIGNING_CERTIFICATE_LOCATION="file:/run/acmehr-day9/sp-signing-certificate.pem"

export ACMEHR_SAML_SP_DECRYPTION_PRIVATE_KEY_LOCATION="file:/run/acmehr-day9/sp-decryption-private-key.pem"
export ACMEHR_SAML_SP_DECRYPTION_CERTIFICATE_LOCATION="file:/run/acmehr-day9/sp-decryption-certificate.pem"

# Day 3 left Okta at its default Name ID format.
# If you kept that default, use Unspecified:
export ACMEHR_SAML_NAME_ID_FORMAT="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"
~~~

If your Day 5 evidence shows a different live NameID `Format` value, use that exact URI instead.

Keep the same working `IDP_METADATA_URL` from your Day 8 setup.

### Windows PowerShell

From the `lab-sp` directory:

~~~powershell
$env:ACMEHR_SAML_SP_SIGNING_PRIVATE_KEY_LOCATION = "file:/run/acmehr-day9/sp-signing-private-key.pem"
$env:ACMEHR_SAML_SP_SIGNING_CERTIFICATE_LOCATION = "file:/run/acmehr-day9/sp-signing-certificate.pem"

$env:ACMEHR_SAML_SP_DECRYPTION_PRIVATE_KEY_LOCATION = "file:/run/acmehr-day9/sp-decryption-private-key.pem"
$env:ACMEHR_SAML_SP_DECRYPTION_CERTIFICATE_LOCATION = "file:/run/acmehr-day9/sp-decryption-certificate.pem"

# Day 3 left Okta at its default Name ID format.
# If you kept that default, use Unspecified:
$env:ACMEHR_SAML_NAME_ID_FORMAT = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"
~~~

If your Day 5 evidence shows a different live NameID `Format` value, use that exact URI instead.

Keep the same working `IDP_METADATA_URL` from your Day 8 setup.

## Verify Compose receives the Day 9 settings

Run:

~~~bash
docker compose config
~~~

Under the `acmehr-training-sp` service, verify that these values are no longer empty:

~~~text
IDP_METADATA_URL

ACMEHR_SAML_SP_SIGNING_PRIVATE_KEY_LOCATION
ACMEHR_SAML_SP_SIGNING_CERTIFICATE_LOCATION

ACMEHR_SAML_SP_DECRYPTION_PRIVATE_KEY_LOCATION
ACMEHR_SAML_SP_DECRYPTION_CERTIFICATE_LOCATION

ACMEHR_SAML_NAME_ID_FORMAT
~~~

The credential values should point to:

~~~text
file:/run/acmehr-day9/...
~~~

Do not point them to:

~~~text
file:/Users/...
file:/home/your-name/...
C:\Users\...
~~~

Those are host paths.

The Java process runs inside the container.

## Start the Day 9 SP with the credentials directory mounted read-only

Stop an earlier training-SP container first.

### macOS or Linux

~~~bash
docker compose down

docker rm -f acmehr-day9 2>/dev/null || true

docker compose run -d \
  --name acmehr-day9 \
  --service-ports \
  --user "$(id -u):$(id -g)" \
  --volume "$CRED_DIR:/run/acmehr-day9:ro" \
  acmehr-training-sp
~~~

The `--user` option lets the container process read the private files that are mode `600` and owned by your workstation user.

### Windows PowerShell

~~~powershell
docker compose down

docker rm -f acmehr-day9 2>$null

docker compose run -d `
  --name acmehr-day9 `
  --service-ports `
  --volume "${CRED_DIR}:/run/acmehr-day9:ro" `
  acmehr-training-sp
~~~

Docker Desktop handles the Windows bind mount.

The `:ro` suffix makes the credential directory read-only inside the container.

## Prove the SP started with the mounted credentials

Open:

~~~text
http://localhost:8000/actuator/health
~~~

Expected:

~~~json
{"status":"UP"}
~~~

Then open:

~~~text
http://localhost:8000/saml2/metadata/acmehr
~~~

At this point the SP is running with the Day 9 signing and decryption credentials.

If the container exits, inspect:

~~~bash
docker logs acmehr-day9
~~~

Common evidence to check first:

~~~text
Could not read PKCS#8 RSA private key
    -> private-key path, format, or read permission

Could not read X.509 certificate
     -> certificate path or certificate format

name-id-format is required
     -> ACMEHR_SAML_NAME_ID_FORMAT is missing

resource does not exist
    -> host path was supplied instead of the mounted container path
~~~

Do not enable Okta Signed Requests or Assertion Encryption until this startup check passes.

## Cleanup after the Day 9 live lab

When the live lab is complete:

~~~bash
docker rm -f acmehr-day9
~~~

The credential files remain in your private home-directory training folder until you deliberately delete them.

Do not copy them into the repository.

---

# Part 6: Inspect AcmeHR SP metadata

Start the training SP with the Day 9 credentials configured.

Open:

~~~text
http://localhost:8000/saml2/metadata/acmehr
~~~

Find the SPSSODescriptor and every KeyDescriptor.

For each KeyDescriptor, record:

~~~text
use
    signing / encryption / unspecified

X509Certificate
    PRESENT

private key
    NEVER PRESENT
~~~

Expected lesson:

~~~text
SP metadata publishes public certificate material

SP metadata never publishes private keys
~~~

---

# Part 7: Calculate the SP metadata fingerprints locally

Create a temporary file named:

~~~text
inspect_sp_metadata_keys.py
~~~

with:

~~~python
import base64
import hashlib
import urllib.request
import xml.etree.ElementTree as ET

MD = "{urn:oasis:names:tc:SAML:2.0:metadata}"
DS = "{http://www.w3.org/2000/09/xmldsig#}"

url = "http://localhost:8000/saml2/metadata/acmehr"
xml = urllib.request.urlopen(url).read()
root = ET.fromstring(xml)

count = 0

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

    count += 1
    print(f"Key {count}")
    print(f"  use: {key.get('use', 'unspecified')}")
    print(f"  SHA-256: {fingerprint}")

if count == 0:
    print("No SP KeyDescriptor certificate values found.")
~~~

Run it.

## macOS or Linux

~~~bash
python3 inspect_sp_metadata_keys.py
~~~

## Windows PowerShell

~~~powershell
py inspect_sp_metadata_keys.py
~~~

Record the signing and encryption fingerprints separately.

---

# Part 8: Record the Okta configuration before changing it

Open:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Sign On
    ->
Edit SAML Settings
    ->
Show Advanced Settings
~~~

Record:

| Setting | Starting value |
| --- | --- |
| Response |  |
| Assertion Signature |  |
| Signature Certificate |  |
| Signed Requests |  |
| Assertion Encryption |  |
| Encryption Algorithm |  |
| Key Transport Algorithm |  |
| Encryption Certificate |  |

This is your recovery note.

---

# Part 9: Configure the AcmeHR request-signing public certificate

In Okta, locate:

~~~text
Signature Certificate
~~~

Upload the AcmeHR request-signing public certificate.

Do not upload:

- the AcmeHR private signing key
- the Okta SAML signing certificate
- an unrelated AcmeHR certificate

Compare the fingerprint with the signing fingerprint from AcmeHR.

---

# Part 10: Prove NameIDPolicy before enabling Signed Requests

Run the Day 9 AuthnRequest tests added by the implementation.

The decoded request must now contain NameIDPolicy.

The Name ID Format configured in Okta must match the request's NameIDPolicy Format.

For the live lab, use the exact NameID `Format` URI you recorded on Day 5.

If you kept the original Day 3 default, that is normally the Unspecified format used in the Docker setup above.

Do not copy the automated test fixture's `emailAddress` format into the live app unless your Okta app is actually configured for EmailAddress.

Do not enable Signed Requests while the generated request still lacks NameIDPolicy.

Current Okta documentation requires NameIDPolicy when Signed Requests is enabled and states that the Name ID format must match when NameIDPolicy is present.

---

# Part 11: Enable Signed Requests

Enable:

~~~text
Signed Requests
~~~

Save the application.

Current Okta behavior now includes:

~~~text
validate SAML requests with Signature Certificate

read requestable SSO URLs dynamically from the signed request
~~~

Okta can remove previously defined static requestable SSO URLs when this setting is enabled.

Do not recreate them automatically.

Understand why they changed first.

---

# Part 12: Capture a fresh signed Redirect AuthnRequest

Close the existing private/incognito window.

Open a new private/incognito window.

Open Developer Tools and enable Preserve log.

Open:

~~~text
http://localhost:8000/protected
~~~

Find the redirect from AcmeHR to Okta.

Before completing authentication, inspect the raw redirect URL.

---

# Part 13: Prove the Redirect signature evidence

The redirect URL should contain:

~~~text
SAMLRequest
SigAlg
Signature
~~~

RelayState may also be present.

Complete:

| Parameter | Present? |
| --- | --- |
| SAMLRequest |  |
| RelayState |  |
| SigAlg |  |
| Signature |  |

If SigAlg or Signature is absent, stop.

That is AcmeHR-side evidence.

Do not blame Okta yet.

---

# Part 14: Decode the request separately

Use the Day 4 local Redirect decoder.

Record:

~~~text
Issuer

Destination

AssertionConsumerServiceURL

ProtocolBinding

NameIDPolicy
~~~

Expected stable values still include:

~~~text
Issuer
    urn:acme:training:sp

AssertionConsumerServiceURL
    http://localhost:8000/saml/acs

ProtocolBinding
    urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST
~~~

Day 9 adds NameIDPolicy.

It should not silently change Entity ID or ACS.

---

# Part 15: Do not require an XML ds:Signature for this proof

Our AuthnRequest uses HTTP-Redirect.

The decoded XML may not contain:

~~~xml
<ds:Signature>
~~~

That does not mean the request is unsigned.

Use:

~~~text
decoded XML
    request contents

raw redirect URL
    SigAlg and Signature
~~~

as separate evidence.

---

# Part 16: Let Okta verify the signed request

Continue the flow.

If Okta accepts the request, the transaction should continue into normal Okta authentication or an existing Okta session.

Record:

~~~text
AcmeHR generated signed Redirect request
    PASS

browser reached Okta
    PASS

Okta request processing continued
    PASS
~~~

A browser merely reaching the Okta domain is not enough.

The flow must continue beyond request rejection.

---

# Part 17: Finish the signed-request baseline

Complete authentication.

Confirm:

~~~text
browser returns to /saml/acs

AcmeHR accepts SAML Response

AcmeHR application session
    ACTIVE
~~~

Do not enable Assertion Encryption until request signing is stable.

---

# Part 18: Configure the pinned-stack encryption baseline

Return to Okta advanced SAML settings.

Before enabling encryption, use this controlled Day 9 signing combination:

~~~text
Response
    Signed

Assertion Signature
    Unsigned
~~~

Then set:

~~~text
Assertion Encryption
    Encrypted
~~~

Okta exposes:

~~~text
Encryption Algorithm

Key Transport Algorithm

Encryption Certificate
~~~

Why are we temporarily using this combination?

The pinned Spring Security 7.1.1 / OpenSAML 5 stack has the open issue documented in Part 25 for some signed inner Assertions after encryption and decryption.

For this Day 9 live exercise, we want to prove the encryption layer without confusing that library issue with a bad certificate.

The outer Response remains signed.

That means AcmeHR still performs SAML signature validation before it trusts the Response.

Do not use:

~~~text
Response
    Unsigned

Assertion Signature
    Unsigned
~~~

That would remove the SAML message-signature protection we are relying on.

This is a controlled training setting for the pinned stack, not a general rule that encrypted Assertions should be unsigned.

Keep the original Response and Assertion Signature values you recorded in Part 8.

You will restore them after the Day 9 live exercise.

---

# Part 19: Upload only the AcmeHR encryption public certificate

For:

~~~text
Encryption Certificate
~~~

upload the AcmeHR public encryption certificate.

Confirm its fingerprint matches the SP encryption credential evidence.

Do not upload:

- the AcmeHR private decryption key
- the Okta SAML signing certificate
- a certificate whose purpose you have not identified

---

# Part 20: Record the actual encryption algorithms

Record:

~~~text
Encryption Algorithm
    ____________________

Key Transport Algorithm
    ____________________
~~~

Use the values actually configured in your Okta org.

Do not fill these from memory.

The repository tests must support the chosen combination.

---

# Part 21: Capture a fresh encrypted SAMLResponse

Start another fresh private/incognito transaction.

Complete the signed-request and Okta authentication flow.

Find the POST to:

~~~text
http://localhost:8000/saml/acs
~~~

Copy the parsed SAMLResponse value locally.

Do not upload it to a public decoder.

---

# Part 22: Decode only the outer Response

Use the Day 5 local Base64 method.

Search the decoded XML for:

~~~text
EncryptedAssertion
~~~

Expected:

~~~text
EncryptedAssertion
    PRESENT
~~~

You should not expect the plaintext NameID, department, or groups inside the encrypted Assertion to be readable from the browser capture.

---

# Part 23: Prove Base64 decoding is not decryption

Record:

~~~text
Base64 decoding
    PASS

outer XML readable
    YES

EncryptedAssertion
    PRESENT

plaintext encrypted Assertion content
    NOT READABLE
~~~

This proves browser-visible confidentiality behavior.

It does not by itself prove AcmeHR can decrypt.

---

# Part 24: Prove AcmeHR verified, decrypted, and authenticated

Confirm:

~~~text
AcmeHR protected page
    OPENS

application session
    ACTIVE
~~~

If Day 7 claims remain configured, open:

~~~text
http://localhost:8000/claims
~~~

Expected:

~~~text
validated claims
    AVAILABLE
~~~

For this pinned-stack live baseline, the successful path is:

~~~text
signed outer Response received
        |
        v
Response signature validation succeeds
        |
        v
EncryptedAssertion decrypts
        |
        v
normal SAML validation succeeds
        |
        v
authenticated application session
~~~

This shows that AcmeHR both trusted the signed outer Response and successfully processed the decrypted Assertion.

---

# Part 25: Keep the success statement precise

A successful encrypted login proves the combined path used by this lab:

~~~text
signed outer Response received
        |
        v
Response signature validation succeeded
        |
        v
EncryptedAssertion decryption succeeded
        |
        v
normal SAML validation succeeded
        |
        v
application session created
~~~

Do not shorten this to:

> Encryption passed.

Several layers passed.

## Important Spring Security 7.1.1 boundary

The focused repository decryption test deliberately uses:

~~~text
signed outer Response
+
encrypted Assertion
~~~

It does not use a signed inner Assertion as the proof fixture.

The repository library review records Spring Security issue #19606 for the pinned Spring Security 7.1.1 / OpenSAML 5 stack. The upstream issue is still open and describes namespace changes during encrypted-Assertion processing that can alter the canonicalized Assertion and cause an otherwise valid inner Assertion signature digest to fail.

That means this lab must not diagnose every signed-and-encrypted-Assertion signature failure as:

~~~text
wrong Okta signing certificate
~~~

First determine whether the failure matches the known library behavior.

Do not work around the issue by:

- disabling signature validation
- trusting unsigned SAML
- writing custom XML Signature verification
- changing certificates without evidence

For Day 9, both the focused local test and the controlled live encryption exercise use a signed outer Response with an encrypted inner Assertion.

Recheck issue #19606 before expanding the lab to rely on a signed inner Assertion as the primary encryption fixture.

Reference:

https://github.com/spring-projects/spring-security/issues/19606

---

# Part 26: Run the focused signed-AuthnRequest tests

The repository now contains:

~~~text
lab-sp/src/test/java/com/acme/training/acmehr/security/SamlAuthenticationRequestTests.java
~~~

Run that class through the Docker test runner.

## macOS or Linux

~~~bash
docker compose --profile test run --rm validation-tests \
  mvn -B -ntp -Dtest=SamlAuthenticationRequestTests test
~~~

## Windows PowerShell

~~~powershell
docker compose --profile test run --rm validation-tests mvn -B -ntp -Dtest=SamlAuthenticationRequestTests test
~~~

Expected Maven result:

~~~text
BUILD SUCCESS
~~~

The Day 9 class now includes:

~~~text
spInitiatedLoginCreatesSignedDay9RedirectBindingAuthnRequest

signedRedirectRequestDoesNotVerifyWithDifferentCertificate
~~~

The first test proves the generated Redirect request contains:

~~~text
SAMLRequest
RelayState
SigAlg
Signature
~~~

and that the actual Redirect-binding signature verifies with the matching AcmeHR public certificate.

It also proves the decoded AuthnRequest still contains the expected:

~~~text
Issuer
Destination
AssertionConsumerServiceURL
ProtocolBinding
NameIDPolicy
~~~

The second test is baseline-first.

It generates one signed Redirect request and keeps that exact request unchanged:

~~~text
same SAMLRequest
same RelayState
same SigAlg
same Signature
~~~

Then it verifies that request twice:

~~~text
matching AcmeHR public certificate
    PASS

different public certificate
    FAIL
~~~

The deliberately different certificate comes from the repository's test IdP metadata.

No second request-signing private key is introduced.

That matters because the failure is isolated to one changed input:

~~~text
verification certificate
~~~

The Java Signature API is used here only as an independent test assertion around Spring Security's generated HTTP-Redirect signature.

Spring Security still performs the actual AuthnRequest signing in the application.

A Maven result of BUILD SUCCESS means the test successfully proved both the known-good and wrong-certificate outcomes.

It does not mean the wrong certificate was accepted.

Do not create this failure by changing the live Okta Signature Certificate.

---

# Part 27: Run the focused encrypted-Assertion tests

The repository now contains:

~~~text
lab-sp/src/test/java/com/acme/training/acmehr/security/SamlEncryptedAssertionTests.java
~~~

The class proves both the known-good and wrong-key cases.

Run it through the same Docker test runner used by the earlier labs.

## macOS or Linux

~~~bash
docker compose --profile test run --rm validation-tests \
  mvn -B -ntp -Dtest=SamlEncryptedAssertionTests test
~~~

## Windows PowerShell

~~~powershell
docker compose --profile test run --rm validation-tests mvn -B -ntp -Dtest=SamlEncryptedAssertionTests test
~~~

Expected Maven result:

~~~text
BUILD SUCCESS
~~~

The two focused tests are:

~~~text
encryptedAssertionDecryptsWithMatchingAcmeHrPrivateKey

encryptedAssertionIsRejectedWithDifferentPrivateKey
~~~

The first proves:

~~~text
Assertion encrypted for AcmeHR public certificate A
        |
        v
AcmeHR private key A
        |
        v
decryption succeeds
        |
        v
normal SAML validation continues
        |
        v
authenticated principal created
~~~

The second first proves that exact encrypted fixture succeeds with matching private key A.

It then changes only the decryption private key:

~~~text
same encrypted SAML
        |
        v
different private key B
        |
        v
DECRYPTION_ERROR
~~~

That baseline-first sequence matters.

Without the successful matching-key baseline, a rejection could come from a broken synthetic Response instead of the deliberately wrong private key.

Also inspect the serialized fixture behavior in the test:

~~~text
EncryptedAssertion
    PRESENT

plaintext Assertion element
    NOT PRESENT
~~~

The test keeps the IdP signing key pair separate from the SP encryption/decryption key pair so the two trust directions are not confused.

Do not create this failure by changing the live Okta Encryption Certificate or replacing AcmeHR's live private key.

---

# Part 28: Document the request-signing failure

~~~text
Observed symptom:
Signed SP-initiated request is rejected before normal sign-in proceeds.

Last confirmed successful step:
AcmeHR generated SAMLRequest, SigAlg, and Signature.

First failed step:
Request-signature verification.

Evidence:
Request signed with key A while verifier trusts certificate B.

Root cause:
Verification certificate does not match AcmeHR request-signing private key.

Single change:
Restore the matching AcmeHR public signing certificate.

Proof after change:
The matching certificate accepts the signed-request path.
~~~

---

# Part 29: Document the decryption failure

~~~text
Observed symptom:
Browser posts an encrypted SAMLResponse but AcmeHR cannot finish login.

Last confirmed successful step:
EncryptedAssertion reaches the ACS.

First failed step:
EncryptedAssertion decryption.

Evidence:
Assertion encrypted for public certificate A while AcmeHR uses private key B.

Root cause:
The decryption private key does not correspond to the public encryption certificate.

Single change:
Restore the matching AcmeHR decryption credential.

Proof after change:
A fresh encrypted transaction decrypts and continues through validation.
~~~

---

# Part 30: Compare the two failure directions

| Question | Request signing | Assertion encryption |
| --- | --- | --- |
| Message direction | AcmeHR -> Okta | Okta -> AcmeHR |
| AcmeHR operation | sign | decrypt |
| Okta operation | verify | encrypt |
| AcmeHR private key | signing key | decryption key |
| Okta-held public cert | Signature Certificate | Encryption Certificate |
| Failure appears | Okta request processing | AcmeHR Response processing |

The same company owns both SP private keys.

The certificate purposes are still different.

---

# Part 31: Prove private keys never crossed the boundary

Confirm:

~~~text
Okta has AcmeHR signing public certificate
    YES

Okta has AcmeHR encryption public certificate
    YES

Okta has AcmeHR private signing key
    NO

Okta has AcmeHR private decryption key
    NO

AcmeHR has Okta private SAML signing key
    NO
~~~

This is a required Day 9 result.

---

# Part 32: Complete the Day 9 evidence table

| Question | Your evidence |
| --- | --- |
| AcmeHR request-signing certificate fingerprint |  |
| Who owns its private key? |  |
| Okta Signature Certificate fingerprint |  |
| SigAlg present? |  |
| Signature present? |  |
| NameIDPolicy present? |  |
| Did Okta accept the signed request? |  |
| AcmeHR encryption certificate fingerprint |  |
| Who owns its private key? |  |
| Okta Encryption Certificate fingerprint |  |
| Encryption Algorithm |  |
| Key Transport Algorithm |  |
| EncryptedAssertion present? |  |
| Did AcmeHR decrypt and authenticate? |  |
| SamlAuthenticationRequestTests BUILD SUCCESS? |  |
| Matching AcmeHR signing certificate verified the same Redirect? |  |
| Different verification certificate rejected the same Redirect? |  |
| SamlEncryptedAssertionTests BUILD SUCCESS? |  |
| Matching AcmeHR decryption key accepted locally? |  |
| Different private key returned DECRYPTION_ERROR? |  |

Do not mark a row complete without evidence.

---

# Part 33: Mini challenge, Signature Certificate

Okta asks for Signature Certificate.

Who owns the corresponding private key?

<details>
<summary>Check your answer</summary>

AcmeHR.

Okta receives only the public certificate so it can verify AcmeHR-signed SAML requests.

</details>

---

# Part 34: Mini challenge, Encryption Certificate

Okta asks for Encryption Certificate.

Who owns the corresponding private key?

<details>
<summary>Check your answer</summary>

AcmeHR.

Okta encrypts with the AcmeHR public certificate, and AcmeHR keeps the private key for decryption.

</details>

---

# Part 35: Mini challenge, signed Redirect

Decoded AuthnRequest has no ds:Signature.

The URL has SAMLRequest, SigAlg, and Signature.

What does that tell you?

<details>
<summary>Check your answer</summary>

The HTTP-Redirect request has binding-level signature evidence.

Do not declare it unsigned from the XML alone.

</details>

---

# Part 36: Mini challenge, first failure at Okta

Browser reaches Okta with SAMLRequest, SigAlg, and Signature.

Okta rejects before user authentication.

Which layer do you inspect first?

<details>
<summary>Check your answer</summary>

Inspect signed-request processing:

- AcmeHR signing credential
- Okta Signature Certificate
- NameIDPolicy
- actual Redirect signature parameters

No SAML Response exists yet, so Assertion decryption is not the first layer.

</details>

---

# Part 37: Mini challenge, first failure at AcmeHR

Okta authenticates the user.

Browser POSTs SAMLResponse containing EncryptedAssertion.

AcmeHR cannot create a session.

What do you inspect first?

<details>
<summary>Check your answer</summary>

Inspect Assertion decryption:

- Okta Encryption Certificate
- matching AcmeHR private decryption key
- exact decryption result

Do not change NameID or claims first.

</details>

---

# Part 38: Explain it back

Explain this transaction:

~~~text
AcmeHR signs AuthnRequest
        |
        v
browser carries signed Redirect request
        |
        v
Okta verifies AcmeHR signature
        |
        v
Okta authenticates Priya
        |
        v
Okta encrypts Assertion
        |
        v
Okta signs outer Response
        |
        v
browser carries SAMLResponse
        |
        v
AcmeHR verifies Response signature
        |
        v
AcmeHR decrypts Assertion
        |
        v
normal SAML validation
        |
        v
application session
~~~

Your explanation must identify:

- request-signing private key owner
- Signature Certificate holder
- SigAlg
- Signature
- NameIDPolicy
- why decoded Redirect XML may not contain ds:Signature
- encryption public certificate holder
- decryption private key owner
- EncryptedAssertion
- Encryption Algorithm
- Key Transport Algorithm
- why Base64 decoding is not decryption
- why decryption does not replace signature validation
- why signature validation does not replace Audience and the other Day 6 checks
- how the last successful step separates request-signature failure from decryption failure

---

# Part 39: Restore the stable post-lab baseline

Do not leave the integration in the temporary pinned-stack encryption configuration.

First, keep your Day 9 evidence.

Then restore the Okta values you recorded in Part 8, including:

~~~text
Response

Assertion Signature

Signed Requests

Assertion Encryption

Signature Certificate, if it changed

Encryption settings, if they were introduced only for this exercise
~~~

The goal is to return to the same known-good Okta signing/encryption baseline you had before the Day 9 live changes.

Then stop the temporary Day 9 credential-mounted container:

~~~bash
docker rm -f acmehr-day9
~~~

Restart the normal training SP using the same method you used before the Day 9 live exercise.

Run a fresh SP-initiated login.

Confirm:

~~~text
fresh SP-initiated login
    PASS

AcmeHR application session
    ACTIVE
~~~

The Day 9 credential files may remain in your private home-directory training folder for later reference.

Do not move them into the repository.

Day 9 proves the advanced trust paths without forcing the known Spring 7.1.1 signed-encrypted-Assertion edge case to become the baseline for later lessons.

---

# Day 9 lab completion check

Do not mark Day 9 complete until you can prove:

~~~text
[ ] I filled out the certificate ownership table first

[ ] I proved the pre-Day-9 SP had no signing or decryption credential

[ ] I waited for the Day 9 implementation checkpoint before changing Okta

[ ] I created separate disposable Day 9 signing and decryption key pairs outside the repository

[ ] I confirmed both private keys use PKCS#8 BEGIN PRIVATE KEY format

[ ] I set the Day 9 Spring resource locations to file:/run/acmehr-day9/... container paths

[ ] I set ACMEHR_SAML_NAME_ID_FORMAT to the live NameID Format URI recorded on Day 5

[ ] I mounted the credential directory read-only into the SP container

[ ] I proved /actuator/health returns UP with the Day 9 credentials loaded

[ ] I identified AcmeHR request-signing public certificate

[ ] I identified AcmeHR encryption public certificate

[ ] I proved SP metadata contains only public key material

[ ] I matched SP metadata key roles and fingerprints

[ ] I uploaded only the request-signing public certificate to Signature Certificate

[ ] I proved NameIDPolicy before enabling Signed Requests

[ ] I captured SAMLRequest, SigAlg, and Signature

[ ] I can explain why decoded Redirect XML may have no ds:Signature

[ ] I proved Okta accepted the signed request

[ ] I recorded my original Response and Assertion Signature settings before the encryption exercise

[ ] I used Response Signed and Assertion Signature Unsigned for the pinned-stack live encryption proof

[ ] I kept SAML signature validation active through the signed outer Response

[ ] I uploaded only the public encryption certificate to Encryption Certificate

[ ] I recorded Encryption Algorithm and Key Transport Algorithm

[ ] I captured a Response containing EncryptedAssertion

[ ] I proved Base64 decoding did not reveal encrypted Assertion plaintext

[ ] I proved AcmeHR decrypted and created an authenticated session

[ ] I ran SamlAuthenticationRequestTests

[ ] I proved the same signed Redirect verifies with the matching AcmeHR certificate

[ ] I proved the same signed Redirect does not verify with a different certificate

[ ] I ran SamlEncryptedAssertionTests

[ ] I proved the matching AcmeHR private key decrypts successfully

[ ] I proved a different private key is rejected with DECRYPTION_ERROR

[ ] I understand why the focused test uses a signed outer Response with an encrypted Assertion on the pinned Spring Security 7.1.1 stack

[ ] I documented both failures from last-success / first-failure evidence

[ ] I proved no private key crossed the federation boundary

[ ] I restored the original Part 8 Okta signing and encryption settings

[ ] I stopped the temporary Day 9 credential-mounted container

[ ] I proved a fresh login still works after restoration

[ ] I can explain every certificate by purpose, owner, holder, and operation
~~~

Day 9 is not complete because Signed Requests is checked.

It is complete when you can prove:

~~~text
what AcmeHR signed

what Okta verified

what Okta encrypted

what AcmeHR decrypted

which key pair belongs to each operation

where the transaction stops when one relationship is wrong
~~~

---

# Save your Day 9 evidence

Keep in your private training notes:

1. certificate ownership table
2. proof that the Day 9 credentials were created outside the repository
3. container credential-location values, but never private-key contents
4. live ACMEHR_SAML_NAME_ID_FORMAT value matched to Day 5 NameID evidence
5. original Response and Assertion Signature settings
6. temporary Day 9 Response Signed / Assertion Signature Unsigned evidence
7. /actuator/health UP result with the Day 9 credentials loaded
8. SP metadata signing and encryption fingerprints
9. redacted signed Redirect query showing parameter names
10. SigAlg value
11. decoded AuthnRequest with NameIDPolicy
12. Okta Signature Certificate fingerprint
13. successful signed-request flow
14. Okta Encryption Certificate fingerprint
15. Encryption Algorithm
16. Key Transport Algorithm
17. redacted outer SAMLResponse showing EncryptedAssertion
18. successful encrypted-login result
19. SamlAuthenticationRequestTests result
20. matching-certificate Redirect verification proof
21. different-certificate Redirect rejection proof
22. SamlEncryptedAssertionTests result
23. matching-key decryption proof
24. wrong-key DECRYPTION_ERROR proof
25. both troubleshooting records
26. restored Part 8 settings and final fresh-login proof

Do not save:

- passwords
- MFA codes
- session cookies
- full production assertions
- any real private key
- disposable training private keys in screenshots or shared notes

---

# Official references for this lab

- Okta, Application Integration Wizard SAML field reference:
  https://help.okta.com/oie/en-us/content/topics/apps/aiw-saml-reference.htm

- Okta, Create SAML app integrations:
  https://help.okta.com/oie/en-us/Content/Topics/Apps/Apps_App_Integration_Wizard_SAML.htm

- Spring Security 7.1.1, SAML 2.0 Login Overview:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html

- Spring Security, Producing AuthnRequests:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication-requests.html

- Spring Security, Authenticating SAML Responses:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html

- Spring Security, SAML 2.0 Metadata:
  https://docs.spring.io/spring-security/reference/servlet/saml2/metadata.html

- OASIS, SAML V2.0 Bindings:
  https://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf
