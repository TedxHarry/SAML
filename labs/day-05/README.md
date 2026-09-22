# Day 5 Lab: Capture and Read the SAML Response

## What you are going to do

You already captured the AuthnRequest going from AcmeHR to Okta.

Today you will capture the return message and prove how Okta represents the authenticated user.

You will:

- run one clean SP-initiated login
- keep the request and response from the same transaction
- find the POST to the AcmeHR ACS
- copy the `SAMLResponse` form value
- decode it locally without sending identity data to a public tool
- separate the outer Response from the inner Assertion
- identify the Response status and both issuers
- find Subject, NameID, SubjectConfirmation, and AuthnStatement
- compare NameID with the assigned user's Okta application username
- compare NameID with the principal displayed by AcmeHR
- identify fields that belong to the Day 6 validation lesson

The goal is to explain what Okta returned and where each value came from.

---

# Before you start

You need:

- the working AcmeHR and Okta integration from Day 3
- the request-capture skills from Day 4
- the assigned Okta test user
- Docker with Docker Compose
- a browser with Developer Tools
- Python 3 for local decoding

Your Day 3 baseline must still work.

You should be able to prove:

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

Do not continue with a broken baseline.

---

# Safety rule for this lab

A real SAML Response can contain:

- the user's NameID
- user attributes
- group values
- Okta and application identifiers
- session references
- timestamps
- endpoints
- digital signatures

Keep the complete Response on your own machine.

Do not paste it into:

- a public SAML decoder
- a forum post
- a public issue
- a shared chat
- an unapproved website

The decoder in this lab runs locally.

When sharing evidence, replace tenant names, user identifiers, application identifiers, encoded values, RelayState, and session-related values with clear placeholders.

Never share browser cookies or authorization headers.

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

---

# Part 2: Prepare one clean transaction capture

Use a new private or incognito browser window.

Before opening AcmeHR:

1. Open Developer Tools.
2. Select the **Network** tab.
3. Enable **Preserve log**.
4. Clear all existing Network entries.
5. Keep Developer Tools open.

We need the AuthnRequest and SAML Response from the same login.

Do not reuse the request ID recorded during an earlier Day 4 transaction. A new login creates a new request ID.

---

# Part 3: Run the SP-initiated login

Open:

```text
http://localhost:8000/protected
```

Sign in with the Okta test user assigned to **AcmeHR Training**.

Complete any authentication required by your Okta policy.

Let the browser return to AcmeHR.

The protected page should show:

```text
Application session: Active

Authenticated principal
<value from the SAML login>
```

Do not close the private window or clear the Network trace.

---

# Part 4: Find both directions in the Network trace

The same trace should contain:

```text
AcmeHR to Okta
    GET request containing SAMLRequest

Okta to AcmeHR
    POST request containing SAMLResponse
```

Find the GET request to your Okta SSO endpoint first.

Confirm that its query contains:

```text
SAMLRequest
RelayState
```

Then find the POST request to:

```text
http://localhost:8000/saml/acs
```

Confirm that its form data contains:

```text
SAMLResponse
RelayState
```

These entries belong to opposite directions of the same browser transaction.

---

# Part 5: Record the current AuthnRequest ID

Decode the `SAMLRequest` from this transaction using the local Day 4 command.

Record only its `ID` in private lab notes.

It should look similar to:

```text
ARQ...
```

Label it clearly:

```text
Current AuthnRequest ID
    ARQ...
```

You will compare this value with the returned Response `InResponseTo`.

If you use an AuthnRequest ID from a different login, the comparison will fail even when both transactions were valid.

---

# Part 6: Select the ACS POST

Select the Network entry whose request URL is:

```text
http://localhost:8000/saml/acs
```

Verify:

| Check | Expected result |
| --- | --- |
| HTTP method | `POST` |
| Host | `localhost:8000` |
| Path | `/saml/acs` |
| Form field | `SAMLResponse` is present |
| Direction | Okta toward AcmeHR |

Do not select:

- the GET request containing `SAMLRequest`
- the final redirect after ACS processing
- the protected-page GET
- an Okta API request

The useful entry is the browser POST carrying `SAMLResponse` to AcmeHR.

---

# Part 7: Copy only the parsed SAMLResponse value

Open the browser section named **Payload**, **Form Data**, **Request**, or something similar.

Find:

```text
SAMLResponse
```

Copy the value of that field.

Copy the complete value. It will be long.

Use the parsed form-field value shown by the browser, not the complete raw request body.

Do not copy:

- the field name
- `RelayState`
- surrounding quotation marks
- a shortened preview containing `...`

Keep the value in your clipboard only long enough to run the local decoder.

---

# Part 8: Decode and format the Response locally

The HTTP-POST binding carries the Response as Base64.

It does not use the raw DEFLATE step from the Day 4 Redirect-binding request.

## macOS or Linux

Run:

```bash
python3 -c "import base64,xml.dom.minidom as x; v=''.join(input('Paste the SAMLResponse value: ').strip().split()); raw=base64.b64decode(v); print(x.parseString(raw).toprettyxml(indent='  '))"
```

Paste the copied `SAMLResponse` value when prompted and press Enter.

## Windows PowerShell

Run:

```powershell
py -c "import base64,xml.dom.minidom as x; v=''.join(input('Paste the SAMLResponse value: ').strip().split()); raw=base64.b64decode(v); print(x.parseString(raw).toprettyxml(indent='  '))"
```

If Windows uses `python` instead of `py`, replace `py` with `python`.

The output should begin with XML similar to:

```xml
<?xml version="1.0" ?>
<saml2p:Response ...>
```

The command reformats a local copy for reading. Do not use the reformatted output for cryptographic signature verification.

---

# Part 9: Confirm that you decoded a Response

Check the root element.

Expected:

```xml
<saml2p:Response
```

or an equivalent namespace prefix such as:

```xml
<samlp:Response
```

The prefix can differ. The namespace and element name carry the meaning.

Do not continue if the output begins with:

```xml
<saml2p:AuthnRequest
```

That means you copied and decoded the request instead of the response.

---

# Part 10: Separate the outer Response from the inner Assertion

Find the outer element:

```xml
<saml2p:Response>
```

Then find the inner element:

```xml
<saml2:Assertion>
```

Record the hierarchy:

```text
Response
    Status
    Assertion
        Subject
        AuthnStatement
        AttributeStatement, when configured
```

Do not call every field an "Assertion field."

`Status` belongs to the Response. `Subject` and `NameID` belong to the Assertion.

---

# Part 11: Record the Response ID and Assertion ID

Find the `ID` on the outer Response.

Example:

```xml
<saml2p:Response ID="id-response-...">
```

Now find the `ID` on the inner Assertion.

Example:

```xml
<saml2:Assertion ID="id-assertion-...">
```

Record them separately:

```text
Response ID
    ...

Assertion ID
    ...
```

The values should be different.

Neither value is Priya's identifier.

---

# Part 12: Match InResponseTo to the current AuthnRequest

Find the outer Response attribute:

```xml
InResponseTo="ARQ..."
```

Compare it with the current AuthnRequest ID recorded in Part 5.

Expected:

```text
Response InResponseTo
        =
Current AuthnRequest ID
```

Now search for `InResponseTo` inside `SubjectConfirmationData`.

You may see the same request ID again.

Record the comparison, but do not perform the complete request-correlation analysis yet. That belongs to Day 6.

---

# Part 13: Verify the Response destination

Find the outer `Destination`:

```xml
Destination="http://localhost:8000/saml/acs"
```

Compare it with the browser POST URL.

Expected:

```text
Response Destination
        =
Browser POST target
        =
http://localhost:8000/saml/acs
```

This is the return direction.

Do not compare it with the Okta SSO endpoint. The Okta endpoint was the AuthnRequest destination.

---

# Part 14: Identify both Okta issuers

Find the `Issuer` directly inside the Response.

Then find the `Issuer` directly inside the Assertion.

They should both identify the Okta IdP for this integration.

Record them separately:

```text
Response Issuer
    ...

Assertion Issuer
    ...
```

Now open the Okta Metadata URL used by AcmeHR and find the metadata `entityID`.

Expected relationship:

```text
Okta metadata entityID
        =
Expected Okta IdP issuer
```

The issuer should not be:

```text
urn:acme:training:sp
```

That is the AcmeHR SP Entity ID.

---

# Part 15: Read the Response status

Find:

```xml
<saml2p:Status>
```

Inside it, find `StatusCode`.

For the working transaction, expect:

```text
urn:oasis:names:tc:SAML:2.0:status:Success
```

Record:

```text
Okta protocol status
    Success
```

Do not write:

```text
AcmeHR validation
    Success
```

The Response status was created by Okta. AcmeHR acceptance requires separate evidence.

---

# Part 16: Find Subject and NameID

Inside the Assertion, find:

```xml
<saml2:Subject>
```

Inside `Subject`, find:

```xml
<saml2:NameID ...>...</saml2:NameID>
```

Record two values separately:

```text
NameID value
    ...

NameID format
    ...
```

Do not assume the NameID value is email because it contains an `@` sign.

Do not assume it is the Okta primary login because Priya used that value to sign in.

We will trace it to the application-specific configuration.

---

# Part 17: Find the assigned user's Okta application username

Open the Okta Admin Console.

Go to the **AcmeHR Training** application and open its assignments.

Find the test user used for this login.

Inspect the assigned user's application username. Depending on the Okta Admin Console view, it may appear in the assignment table or when you edit the user's application assignment.

Record:

```text
Assigned user's application username
    ...
```

Now compare:

```text
SAML Assertion NameID value
        =
Assigned user's Okta application username
```

If they match, you have proved the identity-value relationship for this integration.

Do not change the assignment yet.

---

# Part 18: Inspect the Okta Application username rule

Open the SAML settings for **AcmeHR Training**.

Find:

```text
Application username
```

Record the current selection or expression.

Examples can include:

- Okta username
- email
- a user-profile attribute
- a custom expression

The complete relationship is:

```text
Application username rule
        |
        v
Assigned user's application username
        |
        v
NameID value in the Assertion
```

Do not save any configuration change during this inspection.

---

# Part 19: Compare Name ID format separately

In the same Okta SAML settings, find:

```text
Name ID format
```

Compare that selection with the `Format` attribute on the captured NameID.

Record:

```text
Okta Name ID format setting
    ...

Captured NameID Format
    ...
```

The two should align.

Keep this separate from the value comparison:

```text
Name ID format
    Describes the identifier format

Application username
    Supplies the identifier value
```

Changing the format label does not choose a different user attribute by itself.

---

# Part 20: Compare NameID with the AcmeHR principal

Return to:

```text
http://localhost:8000/protected
```

Record the value shown under:

```text
Authenticated principal
```

Now compare:

```text
First Assertion NameID
        =
AcmeHR authenticated principal
```

The current training SP uses Spring Security's normal mapping, where the principal name comes from the first Assertion's NameID.

This comparison proves how the SAML subject becomes the visible application principal in the training baseline.

It does not decide whether that identifier is the best production design for a real application.

---

# Part 21: Inspect SubjectConfirmation

Inside `Subject`, find:

```xml
<saml2:SubjectConfirmation
    Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
```

Record the method:

```text
SubjectConfirmation method
    bearer
```

Inside it, find `SubjectConfirmationData` and record:

```text
InResponseTo
Recipient
NotOnOrAfter
```

For direction only, expect:

```text
InResponseTo
    Current AuthnRequest ID

Recipient
    http://localhost:8000/saml/acs

NotOnOrAfter
    A future cutoff time in the captured transaction
```

Do not decide whether the time was valid by visual inspection alone.

Day 6 will validate these fields.

---

# Part 22: Inspect AuthnStatement

Find:

```xml
<saml2:AuthnStatement ...>
```

Record:

```text
AuthnInstant
    ...

SessionIndex
    ...
```

Then find:

```xml
<saml2:AuthnContextClassRef>
```

Record its value.

Do not treat `SessionIndex` as:

- the Response ID
- the Assertion ID
- the AcmeHR browser-session cookie
- Priya's user ID

Do not use `AuthnContextClassRef` alone to conclude which exact MFA factors Priya completed.

---

# Part 23: Check for AttributeStatement

Search the Assertion for:

```text
AttributeStatement
```

The Day 3 baseline did not configure attribute or group statements.

Your result may be:

```text
AttributeStatement
    Absent
```

That does not mean NameID is missing.

NameID belongs to `Subject`. Additional user claims belong to `AttributeStatement`.

Day 7 will add and test those claims.

---

# Part 24: Record the signature location without judging it

Search for:

```text
Signature
```

If present, identify whether it appears directly under:

- the Response
- the Assertion
- both

Record only the location.

Do not conclude:

```text
Signature element exists
    therefore
Signature is valid
```

Cryptographic verification requires the trusted certificate and signed XML processing performed by the Service Provider.

Day 8 covers signing and certificate trust.

---

# Part 25: Identify Day 6 fields without analyzing them yet

Locate these fields in the Response or Assertion:

```text
Audience
Destination
Recipient
InResponseTo
NotBefore
NotOnOrAfter
IssueInstant
```

Record where each field appears.

Do not change the Okta application or local clock during this lab.

Day 6 will explain:

- what AcmeHR expects
- how each field is validated
- what a mismatch looks like
- why clock handling matters

---

# Part 26: Compare RelayState in both directions

Return to the Network trace.

Record the `RelayState` beside the AuthnRequest.

Then record the `RelayState` in the ACS POST form data.

Expected:

```text
Request RelayState
        =
Response RelayState
```

Now search the decoded SAML Response XML for:

```text
RelayState
```

Expected:

```text
Not present in the XML
```

RelayState travels beside both SAML messages as a separate HTTP parameter.

---

# Part 27: Complete the field-to-source table

Fill this table with your captured values in private lab notes.

| Field | Your value | Source |
| --- | --- | --- |
| Current AuthnRequest `ID` |  | AcmeHR generated it |
| Response `ID` |  | Okta generated it |
| Assertion `ID` |  | Okta generated it |
| Response `InResponseTo` |  | Refers to AcmeHR AuthnRequest ID |
| Response `Destination` |  | AcmeHR ACS configured in Okta |
| Response `Issuer` |  | Okta IdP entity ID |
| Assertion `Issuer` |  | Okta IdP entity ID |
| Response `StatusCode` |  | Protocol result reported by Okta |
| NameID value |  | Assigned user's application username |
| NameID `Format` |  | Okta Name ID format setting |
| `SubjectConfirmation` method |  | Okta Assertion structure |
| `AuthnInstant` |  | Authentication event reported by Okta |
| `SessionIndex` |  | IdP session reference |
| `AuthnContextClassRef` |  | Authentication context reported by Okta |
| `AttributeStatement` |  | Okta attribute configuration, when present |
| Returned `RelayState` |  | Same state carried beside the request |

This table is the main Day 5 evidence.

---

# Part 28: Build the complete return-side evidence chain

Complete this checklist from your own transaction:

```text
[ ] Okta authentication completed

[ ] Browser sent POST to http://localhost:8000/saml/acs

[ ] POST contained SAMLResponse

[ ] SAMLResponse Base64-decoded into Response XML

[ ] Response contained a Success status

[ ] Response contained an Assertion

[ ] Response and Assertion issuers identified Okta

[ ] InResponseTo matched the current AuthnRequest ID

[ ] Response Destination was the AcmeHR ACS

[ ] Assertion contained Subject and NameID

[ ] NameID matched the assigned user's application username

[ ] NameID matched the principal displayed by AcmeHR

[ ] AcmeHR protected page opened with an active application session
```

The final line proves more than the decoded message alone. It proves that AcmeHR accepted the login and created an authenticated application session.

---

# Part 29: Troubleshoot the capture in the correct order

If you cannot find or decode the Response, use these checks in order.

## Check 1: Did the browser complete Okta authentication?

If the browser never leaves Okta, there may be:

- an authentication failure
- an assignment problem
- an Okta policy requirement
- an application-access error

Do not search for an ACS POST that never occurred.

---

## Check 2: Was Preserve log enabled?

The browser can move through the ACS quickly.

Without **Preserve log**, the POST entry can disappear when AcmeHR redirects to the protected page.

Enable it, clear the trace, and repeat the complete login in a fresh private window.

---

## Check 3: Did you select the correct request?

The correct Network entry must:

- use HTTP POST
- target `http://localhost:8000/saml/acs`
- contain a form field named `SAMLResponse`

The GET request to Okta contains `SAMLRequest`, not `SAMLResponse`.

---

## Check 4: Was the complete field value copied?

An error such as:

```text
Incorrect padding
```

usually means the Base64 value was incomplete or altered.

Copy the complete parsed `SAMLResponse` field again.

Do not copy a shortened preview containing `...`.

---

## Check 5: Did you copy the raw form body instead?

The main decoder expects only the parsed `SAMLResponse` value.

If you copied the complete raw form body containing both fields, use this local command instead.

### macOS or Linux

```bash
python3 -c "import base64,urllib.parse,xml.dom.minidom as x; body=input('Paste the raw form body: ').strip(); q=urllib.parse.parse_qs(body); raw=base64.b64decode(q['SAMLResponse'][0]); print(x.parseString(raw).toprettyxml(indent='  '))"
```

### Windows PowerShell

```powershell
py -c "import base64,urllib.parse,xml.dom.minidom as x; body=input('Paste the raw form body: ').strip(); q=urllib.parse.parse_qs(body); raw=base64.b64decode(q['SAMLResponse'][0]); print(x.parseString(raw).toprettyxml(indent='  '))"
```

Use one method for the representation you copied. Do not repeatedly URL-decode a parsed form-field value.

---

## Check 6: Did you use the Day 4 DEFLATE decoder?

An error from `zlib.decompress(..., -15)` can mean you applied the Redirect-binding request decoder to a POST-binding Response.

For this Response, use Base64 decoding without DEFLATE.

---

## Check 7: Does the Response contain an error status?

If the XML contains a non-success status and no usable Assertion, the decoder worked.

The message itself reports a SAML error.

Record the status code and the last successful browser step.

Do not invent an Assertion that Okta did not return.

---

## Check 8: Does the Response contain EncryptedAssertion?

The Day 3 baseline did not enable assertion encryption.

If you see:

```xml
<saml2:EncryptedAssertion>
```

the current Okta configuration differs from the baseline.

Return to the recorded Day 3 configuration. Do not add a decryption workaround during Day 5.

Encryption belongs to Day 9.

---

## Check 9: Is the NameID value unexpected?

Check in this order:

1. captured NameID value
2. assigned user's application username
3. application username rule
4. Name ID format setting
5. identifier expected by AcmeHR

Do not change the Name ID format when the actual problem is the application username value.

---

# Part 30: Mini incident 1

A teammate says:

```text
The Response ID is priya@example.com, so Okta identified the user correctly.
```

Is that correct?

<details>
<summary>Check your answer</summary>

No.

The Response `ID` identifies the Response message.

The user identifier appears in `NameID` inside the Assertion's `Subject`.

</details>

---

# Part 31: Mini incident 2

You capture:

```text
StatusCode
    Success

Browser POST to /saml/acs
    Present

Protected AcmeHR page
    Not reached
```

Can you report a successful end-to-end login?

<details>
<summary>Check your answer</summary>

No.

You proved that Okta returned a successful SAML protocol result and that the browser delivered it to the ACS.

You have not proved that AcmeHR accepted the message or created its application session.

The next investigation belongs on the Service Provider processing side.

</details>

---

# Part 32: Mini incident 3

The Okta application is configured with:

```text
Name ID format
    EmailAddress

Application username
    employeeNumber
```

The Response contains:

```xml
<saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">
    E10427
</saml2:NameID>
```

Which setting controls the value `E10427`?

<details>
<summary>Check your answer</summary>

The Application username configuration controls the value.

The Name ID format setting controls the `Format` label.

If AcmeHR expects an email value, inspect the application username design rather than changing only the format label.

</details>

---

# Part 33: Lab challenge

Using only your captured evidence, explain this complete relationship:

```text
AcmeHR AuthnRequest ID
        |
        v
Response InResponseTo

Okta metadata entityID
        |
        v
Response and Assertion Issuer

Okta Application username rule
        |
        v
Assigned user's application username
        |
        v
Assertion NameID
        |
        v
AcmeHR authenticated principal
```

Your explanation should identify which values came from AcmeHR, which came from Okta configuration, and which were generated for the current transaction.

Do not answer with only protocol definitions. Point to the values you captured.

---

# Evidence you should keep after this lab

Keep a private, redacted record containing:

- the GET request carrying `SAMLRequest`
- the POST request carrying `SAMLResponse`
- the current AuthnRequest ID
- the Response `InResponseTo` comparison
- separate Response and Assertion IDs
- redacted Response and Assertion issuers
- Response status
- NameID value and format
- assigned user's Okta application username
- AcmeHR authenticated principal
- SubjectConfirmation method and field names
- AuthnStatement field names
- signature location, if present
- the completed field-to-source table
- the complete return-side evidence chain

Do not store the full unredacted Response in the repository.

---

# Explain it back

Imagine a fresher asks:

> What did Okta return, and how did AcmeHR know which user it represented?

A clear explanation should sound roughly like this:

> Okta returned a SAML Response through a browser POST to the AcmeHR ACS. The Response reported the protocol status and contained an Assertion. Inside the Assertion, the Subject's NameID identified the user. That NameID value matched the user's Okta application username, and the current AcmeHR Spring Security mapping exposed the same value as the authenticated principal. AcmeHR still had to validate and process the Response before creating its application session.

Do not memorize that paragraph.

Use the request ID, issuers, NameID, Okta assignment, and AcmeHR principal from your own transaction.

---

# Day 5 lab completion check

You are finished with Day 5 only when you can prove all of these:

```text
[ ] I started from a working Day 3 baseline

[ ] I captured one complete SP-initiated transaction

[ ] I kept the AuthnRequest and Response from the same login

[ ] I found the POST to http://localhost:8000/saml/acs

[ ] I copied the complete parsed SAMLResponse value

[ ] I decoded and formatted the Response locally

[ ] I separated the outer Response from the inner Assertion

[ ] I recorded different Response and Assertion IDs

[ ] I matched InResponseTo to the current AuthnRequest ID

[ ] I matched Destination to the AcmeHR ACS

[ ] I matched the issuers to the Okta IdP entity ID

[ ] I read the Response status without confusing it with SP acceptance

[ ] I found Subject and NameID

[ ] I separated the NameID value from its format

[ ] I matched NameID to the assigned user's Okta application username

[ ] I matched NameID to the principal displayed by AcmeHR

[ ] I located SubjectConfirmation and AuthnStatement

[ ] I checked whether AttributeStatement was present

[ ] I recorded the signature location without claiming validation

[ ] I kept RelayState outside the XML

[ ] I identified the fields reserved for Day 6 validation

[ ] I can explain what the decoded Response proves and what it does not prove
```

Do not continue because you found one email-looking value in the XML.

Continue when you can trace the returned identity from Okta application configuration to NameID and then to the AcmeHR principal.

---

# Official references

The lab steps and field relationships are aligned with the SAML standard, Okta configuration, and the Spring Security behavior used by AcmeHR:

- OASIS, Assertions and Protocols for SAML 2.0:  
  https://docs.oasis-open.org/security/saml/v2.0/saml-core-2.0-os.pdf

- OASIS, SAML 2.0 Bindings:  
  https://docs.oasis-open.org/security/saml/v2.0/saml-bindings-2.0-os.pdf

- Okta, Create an app integration:  
  https://developer.okta.com/docs/guides/create-an-app-integration/saml2/main/

- Okta, Application Integration Wizard SAML field reference:  
  https://help.okta.com/oie/en-us/content/topics/apps/aiw-saml-reference.htm

- Spring Security, SAML 2.0 Login Overview:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html

- Spring Security, Authenticating SAML Responses:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html
