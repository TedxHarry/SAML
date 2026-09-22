# Day 4 Lab: Capture and Read the AuthnRequest

## What you are going to do

You already have a working SAML login from Day 3.

Today you will stop at the request that AcmeHR sends toward Okta and prove what is inside it.

You will:

- start a fresh SP-initiated login
- find the browser request that contains `SAMLRequest`
- copy the complete Okta request URL
- decode the request locally
- identify each important AuthnRequest field
- compare the decoded values with AcmeHR and Okta configuration
- separate the request binding from the requested response binding
- find `RelayState` outside the XML
- compare two requests to see which values stay the same and which values change

The goal is not to memorize XML.

The goal is to look at a real request and explain where every important value came from.

---

# Before you start

You need:

- the working Okta application from Day 3
- the assigned Okta test user
- the AcmeHR training SP running with your Okta Metadata URL
- a browser with Developer Tools
- Python 3 for local decoding
- one known-good Day 3 login

Do not start this lab with a broken Day 3 transaction.

You should already be able to prove:

```text
AcmeHR SAML configuration
    Ready

Browser reaches Okta
    PASS

Browser returns to AcmeHR
    PASS

AcmeHR application session
    Active after login
```

If one of those checks fails, return to the Day 3 lab first.

---

# Safety rule for this lab

Keep the captured request on your own machine.

Do not paste your real Okta request URL, `SAMLRequest`, or `RelayState` into a public decoder, forum post, ticket, or chat.

The command in this lab decodes the request locally with Python.

If you need to share evidence, remove:

- your Okta tenant name
- the complete Okta application URL
- the encoded `SAMLRequest`
- `RelayState`
- cookies, headers, and session values

You can safely discuss the field names and use placeholders for tenant-specific values.

---

# Part 1: Start AcmeHR with the Day 3 configuration

Open a terminal in:

```text
lab-sp
```

Use the same Okta Metadata URL that produced the working Day 3 login.

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

The `--build` option makes sure the container uses the current training SP code.

Open:

```text
http://localhost:8000
```

Confirm:

```text
SAML configuration
Ready
```

Do not continue if the page says:

```text
Waiting for IDP_METADATA_URL
```

---

# Part 2: Prepare a clean browser capture

An active AcmeHR session can prevent `/protected` from starting a new login.

Use a new private or incognito browser window for this lab.

Before opening AcmeHR:

1. Open Developer Tools.
2. Select the **Network** tab.
3. Enable **Preserve log**.
4. Clear the existing Network entries.
5. Keep Developer Tools open.

The exact labels can vary between Chrome, Edge, and Firefox.

You need the browser to keep the redirect entries while it moves between AcmeHR and Okta.

---

# Part 3: Start a new SP-initiated login

In the same private browser window, open:

```text
http://localhost:8000/protected
```

The browser should follow this request-side path:

```text
/protected
    |
    v
/saml2/authenticate?registrationId=acmehr
    |
    v
Okta SSO endpoint with SAMLRequest
```

If Okta asks you to sign in, you can stop on the Okta page while you inspect the request.

If an Okta session already exists, Okta may return to AcmeHR quickly. **Preserve log** should keep the request in the Network trace.

---

# Part 4: Find the redirect that carries SAMLRequest

In the Network trace, find the request whose host belongs to your Okta org and whose URL contains:

```text
SAMLRequest=
```

Select that request.

In the request details, look for **Query String Parameters** or the equivalent section in your browser.

You should see at least:

```text
SAMLRequest
RelayState
```

You should not see a `SAMLResponse` on this request.

This is the request going from AcmeHR toward Okta.

---

# Part 5: Prove that you selected the correct browser request

Before copying anything, check these four facts:

| Check | Expected result |
| --- | --- |
| Request host | Your Okta org |
| HTTP method | `GET` |
| Query parameter | `SAMLRequest` is present |
| Direction | AcmeHR toward Okta |

Do not select:

- the initial `/protected` request
- the local `/saml2/authenticate` request
- the POST returning to `/saml/acs`
- a request containing `SAMLResponse`

The useful entry is the GET request to Okta that contains `SAMLRequest`.

---

# Part 6: Copy the complete Okta request URL

Right-click the selected Network entry and use the browser option similar to:

```text
Copy
    ->
Copy URL
```

Copy the complete URL, including its query string.

It will look similar to:

```text
https://your-okta-domain.okta.com/app/.../sso/saml
    ?SAMLRequest=...
    &RelayState=...
```

Keep this value in your clipboard only long enough to run the local decoder.

Do not add quotation marks or edit the encoded value.

---

# Part 7: Decode the request locally

The decoder below accepts the complete URL. It extracts `SAMLRequest`, performs URL decoding, Base64 decoding, and raw DEFLATE decompression, then prints the XML.

## macOS or Linux

Run:

```bash
python3 -c "import base64,urllib.parse,zlib; u=input('Paste the complete Okta request URL: ').strip(); q=urllib.parse.parse_qs(urllib.parse.urlsplit(u).query); v=q['SAMLRequest'][0]; print(zlib.decompress(base64.b64decode(v),-15).decode('utf-8')); print('\nRelayState outside XML:',q.get('RelayState',['NOT PRESENT'])[0])"
```

When prompted, paste the complete Okta request URL and press Enter.

## Windows PowerShell

Run:

```powershell
py -c "import base64,urllib.parse,zlib; u=input('Paste the complete Okta request URL: ').strip(); q=urllib.parse.parse_qs(urllib.parse.urlsplit(u).query); v=q['SAMLRequest'][0]; print(zlib.decompress(base64.b64decode(v),-15).decode('utf-8')); print('\nRelayState outside XML:',q.get('RelayState',['NOT PRESENT'])[0])"
```

If your Windows installation uses `python` instead of `py`, replace `py` with `python`.

The command should print XML beginning with:

```xml
<saml2p:AuthnRequest
```

It should also print the separate `RelayState` value after the XML.

---

# Part 8: Understand what the decoder reversed

The browser carried an encoded query parameter.

The local command reversed these steps:

```text
Complete Okta request URL
        |
        v
Extract and URL-decode SAMLRequest
        |
        v
Base64 decode
        |
        v
Raw DEFLATE decompress
        |
        v
AuthnRequest XML
```

The value `-15` in the Python command tells zlib to read the raw DEFLATE format used by the SAML HTTP-Redirect binding.

If you remove `-15`, decompression may fail even when the SAML request is valid.

---

# Part 9: First read of the real XML

Your decoded request should look similar to this shortened example:

```xml
<saml2p:AuthnRequest
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    AssertionConsumerServiceURL="http://localhost:8000/saml/acs"
    Destination="https://your-okta-domain.okta.com/app/.../sso/saml"
    ForceAuthn="false"
    ID="ARQ..."
    IsPassive="false"
    IssueInstant="2026-09-22T07:15:30Z"
    ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
    Version="2.0">

    <saml2:Issuer
        xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">
        urn:acme:training:sp
    </saml2:Issuer>
</saml2p:AuthnRequest>
```

Your `ID`, `IssueInstant`, `Destination`, namespace placement, spacing, and attribute order can differ.

Do not compare the XML character by character.

Compare the meaning of each field.

---

# Part 10: Verify the request identity fields

Find these values in your request:

```text
ID
Version
IssueInstant
```

Record them in private lab notes.

| Field | What to verify |
| --- | --- |
| `ID` | Present and unique to this request; current AcmeHR IDs begin with `ARQ` |
| `Version` | `2.0` |
| `IssueInstant` | Recent UTC time for this request |

The `ID` is not Priya's identity and is not the Okta application ID.

The `IssueInstant` is not a configured Okta field and is not an expiration time.

AcmeHR creates both values for the current transaction.

---

# Part 11: Verify the Issuer

Find:

```xml
<saml2:Issuer>urn:acme:training:sp</saml2:Issuer>
```

Expected value:

```text
urn:acme:training:sp
```

This is the AcmeHR SP Entity ID.

It should match the value configured in Okta as:

```text
Audience URI (SP Entity ID)
```

Write the direction in your notes:

```text
AuthnRequest Issuer
    AcmeHR / Service Provider
```

Do not replace it with the Okta issuer.

---

# Part 12: Verify the Destination

Find the `Destination` attribute.

It should contain your Okta SSO endpoint:

```xml
Destination="https://your-okta-domain.okta.com/app/.../sso/saml"
```

Now compare it with the browser request URL that carried `SAMLRequest`.

Ignore the query string for this comparison.

The endpoint should match:

```text
AuthnRequest Destination
        =
Browser request URL before ?SAMLRequest
```

This proves that the SAML message and browser transport point to the same Okta SSO endpoint.

---

# Part 13: Trace Destination back to Okta metadata

AcmeHR did not invent the Okta SSO endpoint.

It loaded the endpoint from the Okta IdP metadata supplied through `IDP_METADATA_URL`.

Open your Okta Metadata URL in a separate browser tab and search the XML for:

```text
SingleSignOnService
```

Find the entry whose `Binding` ends with:

```text
HTTP-Redirect
```

Its `Location` should match the AuthnRequest `Destination`.

The relationship is:

```text
Okta metadata
    HTTP-Redirect SingleSignOnService Location
        |
        v
AcmeHR AuthnRequest Destination
        |
        v
Browser GET request to Okta
```

Your Okta metadata may list an HTTP-POST SSO service before the HTTP-Redirect service.

The AcmeHR training SP explicitly selects HTTP-Redirect for the AuthnRequest.

---

# Part 14: Verify the ACS URL

Find:

```xml
AssertionConsumerServiceURL="http://localhost:8000/saml/acs"
```

Expected value:

```text
http://localhost:8000/saml/acs
```

This is where AcmeHR asks Okta to return the SAML Response.

It should match the Okta application field:

```text
Single sign-on URL
```

Write both directions next to each other:

```text
Destination
    Request goes to Okta

AssertionConsumerServiceURL
    Response returns to AcmeHR
```

If you can explain that difference without reading the labels, you understand the request direction.

---

# Part 15: Verify ProtocolBinding

Find:

```xml
ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
```

The browser carried the request to Okta using HTTP-Redirect binding.

The `ProtocolBinding` field asks Okta to return the response using HTTP-POST binding.

Record the complete pair:

```text
AcmeHR to Okta
    AuthnRequest by HTTP-Redirect

Okta to AcmeHR
    SAML Response requested by HTTP-POST
```

There is no contradiction.

The request transport and requested response transport are two different decisions.

---

# Part 16: Check ForceAuthn and IsPassive

The current Spring Security request writes:

```xml
ForceAuthn="false"
IsPassive="false"
```

For this lab, record them but do not change them.

They are advanced controls:

- `ForceAuthn="false"` means the request is not demanding a fresh IdP authentication regardless of an existing IdP session.
- `IsPassive="false"` means the request is not limited to a no-interaction authentication attempt.

They are not part of the Day 4 configuration exercise.

---

# Part 17: Prove which optional elements are absent

Search the decoded XML for:

```text
NameIDPolicy
```

Expected result:

```text
Not present
```

Now search for:

```text
RequestedAuthnContext
```

Expected result:

```text
Not present
```

The current AcmeHR baseline does not configure either request element.

Their absence is valid.

Do not add XML manually to make the request look more complete.

---

# Part 18: Find RelayState in the correct place

Return to the browser's query parameters or look at the final line printed by the decoder.

You should see a generated value similar to:

```text
RelayState outside XML: 3f4c...-...
```

The exact value changes.

Now search the decoded AuthnRequest XML for:

```text
RelayState
```

Expected result:

```text
Not present in the XML
```

Record the location:

```text
SAMLRequest
    Query parameter containing the encoded AuthnRequest

RelayState
    Separate query parameter beside SAMLRequest
```

Do not describe RelayState as an AuthnRequest XML attribute.

---

# Part 19: Map every value to its source

Complete this table from your own captured request.

| Captured item | Your value | Source |
| --- | --- | --- |
| `ID` |  | Generated for this request |
| `Version` |  | SAML protocol value used by the library |
| `IssueInstant` |  | Current transaction time |
| `Issuer` |  | AcmeHR SP Entity ID |
| `Destination` |  | Okta HTTP-Redirect SSO endpoint from IdP metadata |
| `AssertionConsumerServiceURL` |  | AcmeHR ACS configuration |
| `ProtocolBinding` |  | AcmeHR response-binding configuration |
| `ForceAuthn` |  | Request control set by the SAML library |
| `IsPassive` |  | Request control set by the SAML library |
| `NameIDPolicy` |  | Absent in the current baseline |
| `RequestedAuthnContext` |  | Absent in the current baseline |
| `RelayState` |  | Generated for this transaction outside the XML |

Do not copy tenant-specific values into a public document.

Keep this table in private lab notes.

---

# Part 20: Compare two AuthnRequests

Generate a second request.

The quickest method is to open this local endpoint in a fresh private window or tab while the Network panel is recording:

```text
http://localhost:8000/saml2/authenticate/acmehr
```

Find the new Okta request, copy its complete URL, and decode it with the same local command.

Compare request 1 and request 2.

Expected result:

| Field | Same or different? |
| --- | --- |
| `ID` | Different |
| `IssueInstant` | Different |
| `RelayState` | Different |
| `Version` | Same |
| `Issuer` | Same |
| `Destination` | Same |
| `AssertionConsumerServiceURL` | Same |
| `ProtocolBinding` | Same |

This comparison separates transaction data from configuration data.

Do not expect the encoded `SAMLRequest` strings to match. The XML contains a new ID and timestamp, so the compressed and encoded result also changes.

---

# Part 21: Build the request-side evidence chain

Use your Network trace and decoded XML to complete this checklist:

```text
[ ] Browser requested http://localhost:8000/protected

[ ] AcmeHR redirected to its local SAML authentication endpoint

[ ] Local SAML endpoint redirected the browser to Okta

[ ] Okta request used HTTP GET

[ ] Okta request contained SAMLRequest

[ ] SAMLRequest decoded into AuthnRequest XML

[ ] Issuer was urn:acme:training:sp

[ ] Destination matched the Okta HTTP-Redirect SSO endpoint

[ ] AssertionConsumerServiceURL was http://localhost:8000/saml/acs

[ ] ProtocolBinding requested HTTP-POST

[ ] RelayState was outside the XML
```

This evidence proves that AcmeHR created and sent the expected authentication request.

It does not prove that Okta authenticated the user or that AcmeHR accepted the response.

---

# Part 22: Troubleshoot the capture in the correct order

If you cannot find or decode the request, use these checks in order.

## Check 1: Did AcmeHR load the SAML configuration?

Open:

```text
http://localhost:8000
```

Expected:

```text
SAML configuration
Ready
```

If it is not ready, fix `IDP_METADATA_URL` first.

---

## Check 2: Did an active AcmeHR session bypass login?

If `/protected` opens immediately, AcmeHR may already have an authenticated application session.

Use a new private window or open the SAML start endpoint directly:

```text
http://localhost:8000/saml2/authenticate/acmehr
```

---

## Check 3: Was Preserve log enabled?

The browser moves across several pages quickly.

Without **Preserve log**, earlier redirects may disappear from the Network view.

Enable it, clear the trace, and repeat the request.

---

## Check 4: Did you copy the correct URL?

The copied URL must:

- point to your Okta org
- use HTTP GET
- contain `SAMLRequest=`

Do not give the decoder the AcmeHR `/protected` URL or the ACS POST URL.

---

## Check 5: Does the decoder report a missing SAMLRequest key?

An error similar to:

```text
KeyError: 'SAMLRequest'
```

means the pasted URL did not contain a query parameter named `SAMLRequest`.

Return to the Network trace and copy the complete Okta request URL.

---

## Check 6: Does Base64 or DEFLATE decoding fail?

The common causes are:

- only part of the URL was copied
- the encoded value was edited
- a decoded query value was copied instead of the complete URL
- the wrong browser request was selected
- the raw DEFLATE option `-15` was removed from the command

Capture a new request and copy the complete URL again.

Do not repair the encoded value by guessing characters.

---

## Check 7: Did the request use POST instead of Redirect?

The current AcmeHR training SP explicitly selects HTTP-Redirect for AuthnRequest delivery.

If the browser sends an HTML form containing `SAMLRequest` instead of a redirect query parameter:

1. stop the container
2. rebuild the current training SP with `docker compose up --build`
3. confirm that you are using the current repository version
4. repeat the capture

Do not change the Okta application to work around an old local image.

---

# Part 23: Mini incident

A teammate sends this summary:

```text
The request failed because ProtocolBinding says HTTP-POST,
but the browser used a redirect to reach Okta.
```

Is that diagnosis correct?

<details>
<summary>Check your answer</summary>

No.

The browser used HTTP-Redirect binding to deliver the AuthnRequest to Okta.

Inside that request, `ProtocolBinding` asked Okta to return the SAML Response using HTTP-POST binding.

Those values describe opposite directions of the same transaction.

</details>

---

# Part 24: Lab challenge

Without reopening the lesson, explain this request fragment:

```xml
<saml2p:AuthnRequest
    AssertionConsumerServiceURL="http://localhost:8000/saml/acs"
    Destination="https://your-okta-domain.okta.com/app/.../sso/saml"
    ID="ARQ..."
    IssueInstant="2026-09-22T07:15:30Z"
    ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
    Version="2.0">
    <saml2:Issuer>urn:acme:training:sp</saml2:Issuer>
</saml2p:AuthnRequest>
```

Your explanation should answer:

1. Who created the request?
2. Where is it going?
3. Where should the response return?
4. How should the response return?
5. Which values are generated per request?
6. Which values come from configuration?
7. Where would you look for RelayState?

<details>
<summary>Check your explanation</summary>

AcmeHR created the request because the issuer is the AcmeHR SP Entity ID. The destination is the Okta SSO endpoint. Okta should return the response to the AcmeHR ACS URL using HTTP-POST binding. The request ID and IssueInstant are generated for the current transaction. Issuer, Destination, ACS, and the response binding come from the federation configuration. RelayState is a separate HTTP parameter beside `SAMLRequest`, not part of this XML.

</details>

---

# Evidence you should keep after this lab

Keep a private, redacted record containing:

- the request-side Network sequence
- the HTTP method used to reach Okta
- a redacted Okta destination
- the decoded AuthnRequest XML with tenant details removed
- the field-to-source table from Part 19
- the comparison between two requests
- the completed request-side evidence chain

Do not keep or share browser cookies, authorization headers, or complete live request URLs as course evidence.

---

# Explain it back

Imagine a fresher asks:

> What does AcmeHR send before Okta signs the user in?

Explain it using your captured request.

A clear explanation should sound roughly like this:

> AcmeHR creates an AuthnRequest and sends it to the Okta SSO endpoint through the browser. The request identifies AcmeHR as the issuer, includes a unique request ID and creation time, names the AcmeHR ACS URL for the return, and asks Okta to send the response using HTTP-POST. The request itself travels to Okta using HTTP-Redirect. RelayState travels beside the encoded request as a separate query parameter.

Do not memorize that paragraph.

Point to your evidence and explain each direction in your own words.

---

# Day 4 lab completion check

You are finished with Day 4 only when you can prove all of these:

```text
[ ] I started from a working Day 3 transaction

[ ] I captured a new SP-initiated login with Preserve log enabled

[ ] I found the GET request to Okta containing SAMLRequest

[ ] I copied the complete request URL

[ ] I decoded the request locally

[ ] I identified the AuthnRequest root element

[ ] I explained ID, Version, and IssueInstant

[ ] I proved that the Issuer is the AcmeHR SP Entity ID

[ ] I matched Destination to the Okta HTTP-Redirect SSO endpoint

[ ] I matched AssertionConsumerServiceURL to the AcmeHR ACS

[ ] I explained why ProtocolBinding says HTTP-POST

[ ] I confirmed that NameIDPolicy is absent

[ ] I confirmed that RequestedAuthnContext is absent

[ ] I found RelayState outside the XML

[ ] I compared two requests and identified changing values

[ ] I separated request-side evidence from proof of complete SSO

[ ] I can explain the request without reading a memorized definition
```

Do not continue because the decoder printed XML once.

Continue when you can connect every important field to AcmeHR, Okta, or the current transaction.

---

# Official references

The lab behavior is based on the SAML standard and the framework used by the training SP:

- OASIS, SAML 2.0 Bindings:  
  https://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf

- OASIS, SAML 2.0 Technical Overview:  
  https://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0.html

- Spring Security, Producing AuthnRequests:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication-requests.html

- Okta, Application Integration Wizard SAML field reference:  
  https://help.okta.com/oie/en-us/content/topics/apps/aiw-saml-reference.htm
