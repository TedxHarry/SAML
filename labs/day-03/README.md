# Day 3 Lab: Build Your First Working Okta SAML SSO

## What you are going to do

This is the first live SAML integration in the course.

You will connect:

```text
Okta
    as the Identity Provider

AcmeHR Training
    as the Service Provider
```

The goal is simple:

> Start the login from AcmeHR, authenticate with Okta, return to AcmeHR, and prove that AcmeHR created its own authenticated application session.

Do not try to understand every SAML field today.

Day 3 is about getting one clean end-to-end transaction working and keeping it as your known-good baseline.

---

# Before you start

You need:

- access to an Okta org where you can create a custom SAML 2.0 application
- one Okta test user that you can assign to the application
- Docker with Docker Compose available
- a browser with Developer Tools
- this course repository on your machine

You do **not** need to install:

- Java
- Maven
- OpenSAML
- a SAML browser extension

The training Service Provider runs in Docker.

Use a dedicated test user and test Okta org for the lab.

---

# The values we already know

AcmeHR owns these Service Provider values.

Use them exactly as shown.

| Purpose | Value |
| --- | --- |
| AcmeHR base URL | `http://localhost:8000` |
| ACS URL | `http://localhost:8000/saml/acs` |
| SP Entity ID | `urn:acme:training:sp` |
| SP metadata | `http://localhost:8000/saml2/metadata/acmehr` |

Remember what the two important SAML values mean:

```text
ACS URL
    Where Okta returns the SAML login response

SP Entity ID
    Which Service Provider this integration represents
```

Do not swap them.

---

# Part 1: Prove the local AcmeHR application works

Open a terminal in the repository and move into:

```text
lab-sp
```

Start the application without an Okta metadata URL:

```bash
docker compose up --build
```

The first build can take longer because Docker must download the Java and Maven images and Maven dependencies.

When the application starts, open:

```text
http://localhost:8000
```

You should see:

```text
AcmeHR Training

SAML configuration
Waiting for IDP_METADATA_URL

AcmeHR application session
Not active
```

Now open:

```text
http://localhost:8000/transaction
```

The transaction stages should show:

```text
NOT CHECKED
```

This is correct.

We have proved only that the Service Provider application is running.

We have **not** configured the federation relationship yet.

---

## Evidence checkpoint 1

Before continuing, you should be able to prove:

```text
AcmeHR application starts
    PASS

Browser can reach AcmeHR
    PASS

Okta SAML configuration loaded
    NOT YET

SAML login
    NOT YET
```

If AcmeHR does not load, fix the local application problem before touching Okta.

Do not troubleshoot Okta when the Service Provider itself is not running.

---

# Part 2: Create the Okta SAML application

Stop the running container with:

```text
Ctrl+C
```

If needed, clean up the stopped Compose environment:

```bash
docker compose down
```

Now open the Okta Admin Console.

Current Okta documentation starts the custom app flow under:

```text
Applications and Resources
        ->
Applications
        ->
Create App Integration
```

Some orgs may show a **Classic experience** option during this flow.

The menu wording can vary slightly.

The important result is that you create a custom:

```text
SAML 2.0
```

app integration.

---

# Part 3: Give the application a clear name

For the application name, use:

```text
AcmeHR Training
```

The app name is only the human-friendly label in Okta.

It is **not** the SP Entity ID.

Continue to the SAML configuration page.

---

# Part 4: Configure the two SP values

On the SAML configuration page, find:

```text
Single sign-on URL
```

Enter:

```text
http://localhost:8000/saml/acs
```

This is the AcmeHR ACS URL.

Now find:

```text
Audience URI (SP Entity ID)
```

Enter:

```text
urn:acme:training:sp
```

For this first lab, keep the normal option that uses the Single sign-on URL for the Recipient and Destination values.

Do not change those fields separately today.

---

## Configuration check

Before moving on, read the values back to yourself:

```text
Where should Okta return the login response?

http://localhost:8000/saml/acs


Which Service Provider does this configuration represent?

urn:acme:training:sp
```

If you cannot answer those two questions without looking at the field labels, review the Day 3 lesson before continuing.

---

# Part 5: Leave later-day settings alone

The Okta SAML configuration contains more options.

For Day 3:

- leave the current Name ID format setting at its normal/default value
- leave the current Application username setting at its normal/default value
- do not add attribute statements
- do not add group attribute statements
- do not configure request signing
- do not configure assertion encryption
- do not change certificate settings

Those are real SAML topics.

They are intentionally taught later.

The goal today is the smallest working federation relationship.

Finish creating the application.

If Okta asks how the application is being used, choose the option appropriate for a customer-created internal application.

---

# Part 6: Assign your test user

Open the new **AcmeHR Training** application.

Go to:

```text
Assignments
```

Assign the Okta test user you plan to use.

Current Okta documentation supports:

```text
Assign
    ->
Assign to People
```

or assignment through a group.

For this first lab, assigning one dedicated test user keeps the evidence simple.

Do not continue until the intended test user is assigned.

---

## Why this matters

Application assignment and SAML configuration are different layers.

```text
SAML configuration
    Defines the federation relationship

Assignment
    Controls which Okta users can use the application
```

A correct ACS URL does not automatically assign a user.

A correct Entity ID does not automatically assign a user.

---

# Part 7: Copy Okta's Metadata URL

Open the application's **Sign On** area.

Current Okta documentation exposes a **Metadata URL** for the SAML application.

Copy that Metadata URL.

It normally points to your Okta org and the SAML application you just created.

Do not copy the AcmeHR ACS URL.

Do not copy the SP metadata URL.

At this point you have two different metadata directions:

```text
Okta Metadata URL
    Okta tells AcmeHR about the IdP

AcmeHR SP metadata URL
    AcmeHR describes the Service Provider
```

For this lab, AcmeHR needs the **Okta Metadata URL**.

---

# Part 8: Give the Okta metadata to AcmeHR

Return to the terminal in:

```text
lab-sp
```

Set the metadata URL in the same terminal where you will run Docker Compose.

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

Do not commit your tenant-specific metadata URL to the repository.

The training SP reads it only at runtime.

---

# Part 9: Confirm that AcmeHR loaded the SAML configuration

Open:

```text
http://localhost:8000
```

You should now see:

```text
SAML configuration
Ready
```

and:

```text
AcmeHR application session
Not active
```

That combination is exactly what we want before the first login.

It means:

```text
The federation configuration is loaded

but

the user has not authenticated to AcmeHR yet
```

---

## If the page does not show Ready

Do not start changing SAML fields.

Check the terminal first.

Ask:

> **What is the last step I can prove succeeded?**

For example:

```text
AcmeHR starts without metadata
    PASS

IDP_METADATA_URL was set
    CHECK

Container restarted after setting it
    CHECK

Okta Metadata URL opens successfully
    CHECK

AcmeHR starts with that metadata
    ?
```

If the Metadata URL was copied incorrectly, fix only that value and retry.

---

# Part 10: Optional SP metadata check

With SAML configured, open:

```text
http://localhost:8000/saml2/metadata/acmehr
```

Your browser will show XML.

Do **not** try to understand the XML yet.

For Day 3, look only for the two values you already know:

```text
urn:acme:training:sp

http://localhost:8000/saml/acs
```

This proves those values are not arbitrary strings from the lab instructions.

They are part of the Service Provider's real SAML configuration.

Then close the metadata tab.

We will work with SAML XML more carefully in later lessons.

---

# Part 11: Prepare browser evidence

Before starting the first login:

1. Open browser Developer Tools.
2. Open the **Network** tab.
3. Enable **Preserve log** if your browser provides that option.
4. Keep the Network tab open.

You learned this workflow on Day 2.

Today we are using it on a real SAML transaction.

Do not try to decode the SAMLRequest or SAMLResponse yet.

We only want to follow the browser path.

---

# Part 12: Start the first SP-initiated login

Start from the Service Provider by opening:

```text
http://localhost:8000/saml2/authenticate/acmehr
```

This is AcmeHR's SAML login-start endpoint.

You are still starting from the Service Provider, so this is an **SP-initiated** flow.

The browser should leave AcmeHR and go to Okta.

Sign in using the test user that you assigned to **AcmeHR Training**.

Complete any authentication requirements configured in your Okta org.

Do not start this test from the Okta application tile.

IdP-initiated SSO is a later lesson.

For Day 3, we want one clean SP-initiated baseline.

---

# Part 13: Let the browser return to AcmeHR

After Okta authentication, the browser should return to:

```text
http://localhost:8000/saml/acs
```

The browser carries the SAML login response back to the Service Provider.

AcmeHR then processes the login through its SAML security layer.

If the login is accepted, Spring Security creates the authenticated application session.

You do not need to inspect the response XML today.

---

# Part 14: Prove the AcmeHR session exists

Return to:

```text
http://localhost:8000
```

You should now see:

```text
AcmeHR application session
Active
```

Open:

```text
http://localhost:8000/protected
```

You should see the protected AcmeHR page and the authenticated principal.

That is stronger evidence than:

```text
I saw the Okta login page
```

or:

```text
My Okta password worked
```

The Service Provider has accepted the SAML login and established its own authenticated application session.

---

# Part 15: Check the Day 3 transaction view

Open:

```text
http://localhost:8000/transaction
```

For the successful Day 3 baseline, you should see:

```text
AcmeHR started the SAML login
PASS

The browser returned to AcmeHR
PASS

The Service Provider accepted the SAML login
PASS

AcmeHR created its application session
PASS
```

Do not read those four lines as four detailed protocol validations.

The training SP performs secure SAML processing internally.

Day 3 intentionally shows only the high-level outcome.

Later lessons will open the validation layers one at a time.

---

# Part 16: Use the browser trace as evidence

Go back to the browser Network tab.

You should be able to identify the broad path:

```text
AcmeHR
    |
    v
SAML login started
    |
    v
Okta
    |
    v
AcmeHR ACS
    |
    v
Authenticated AcmeHR session
```

Look for evidence that the browser:

- started from the AcmeHR SAML login endpoint
- navigated to Okta
- returned to AcmeHR
- sent a request to `/saml/acs`

Do not decode the SAML messages yet.

Today the browser trace answers:

> Where did the transaction go?

Day 4 and Day 5 will answer:

> What was inside the SAML messages?

---

# Part 17: Record the known-good baseline

Before changing anything, record these values in your lab notes:

```text
Okta application
AcmeHR Training

SP Entity ID
urn:acme:training:sp

ACS URL
http://localhost:8000/saml/acs

SP-initiated login
PASS

Browser reached Okta
PASS

Browser returned to AcmeHR
PASS

AcmeHR accepted the SAML login
PASS

AcmeHR application session
PASS
```

This working transaction is important.

Later labs will deliberately change one part of the configuration.

You will compare those failures with this known-good baseline.

---

# Part 18: If the first login fails

Do not randomly edit fields.

Use the transaction path.

Ask:

> **What is the last step I can prove succeeded?**

Use this order.

## Checkpoint 1: Can you reach AcmeHR?

Open:

```text
http://localhost:8000
```

If no:

> Fix the local application or Docker problem first.

---

## Checkpoint 2: Does AcmeHR say SAML configuration is Ready?

If no:

> Check `IDP_METADATA_URL`, the copied Metadata URL, and the container startup output.

Do not change the ACS or Entity ID yet.

---

## Checkpoint 3: Does the browser leave AcmeHR and reach Okta?

If no:

> The login did not reach the Identity Provider. Inspect the browser Network trace and AcmeHR startup/configuration first.

---

## Checkpoint 4: Can the assigned test user authenticate in Okta?

If Okta says the user cannot access the application:

> Check the Okta application assignment before changing SAML endpoint values.

Assignment is a different layer.

---

## Checkpoint 5: Does the browser return to AcmeHR?

If Okta authentication succeeds but the browser does not return correctly:

> Compare the Okta **Single sign-on URL** with the AcmeHR ACS URL.

Expected:

```text
http://localhost:8000/saml/acs
```

---

## Checkpoint 6: Does AcmeHR create an application session?

If the browser returns to AcmeHR but the session is not active:

> Do not say that SSO succeeded just because Okta authentication succeeded.

At that point you have proved:

```text
Okta authentication
PASS
```

but you have **not** proved:

```text
Service Provider acceptance
PASS
```

Use the available AcmeHR output and browser evidence.

Do not disable validation to force the login through.

---

# Part 19: Do not make these troubleshooting changes

For Day 3, do **not** try fixes such as:

- disabling SAML validation
- replacing the signing certificate randomly
- changing Audience until something works
- adding extra attributes
- changing NameID repeatedly
- enabling encryption
- changing several Okta fields at once

You have not learned those layers yet.

A good engineer narrows the failed step before changing configuration.

---

# Part 20: Reset the local lab when needed

To stop the training SP:

```bash
docker compose down
```

Stopping and recreating the container gives the training application a fresh local runtime.

For a completely fresh browser-side test, use a new private/incognito window or clear only the local AcmeHR session cookie.

Do not delete or recreate the Okta application just to reset the local SP.

The working Okta configuration is part of your Day 3 baseline.

---

# What you should now be able to explain

Without looking at the lesson, explain these in your own words:

1. Why does Okta need the AcmeHR ACS URL?
2. Why does Okta need the AcmeHR SP Entity ID?
3. Why are the ACS URL and SP Entity ID different?
4. What does the Okta Metadata URL give to AcmeHR?
5. Why must the test user be assigned to the Okta application?
6. Why does successful Okta authentication not automatically prove successful SSO?
7. What evidence proves that AcmeHR accepted the login?
8. What does SP-initiated mean in the transaction you just ran?
9. If a transaction fails, what is the first troubleshooting question you should ask?

<details>
<summary>Check your explanation</summary>

A strong explanation should sound roughly like this:

- The ACS URL tells Okta where the browser must return the SAML login response.
- The SP Entity ID identifies the Service Provider that the SAML configuration represents.
- One value answers **where**; the other answers **which SP**.
- Okta metadata gives AcmeHR the IdP-side SAML configuration it needs to work with Okta.
- Assignment controls whether the Okta user can use that application.
- Okta can authenticate the user successfully and the Service Provider can still reject the SAML login.
- Reaching the protected AcmeHR page with an active AcmeHR session proves that the Service Provider accepted the login.
- SP-initiated means the SAML flow started from AcmeHR, not from the Okta app tile.
- The first troubleshooting question is: **What is the last step I can prove succeeded?**

</details>

---

# Explain it back

Explain the complete Day 3 transaction as if you were speaking to an application owner:

```text
Priya starts from AcmeHR.

AcmeHR starts the SAML login and sends the browser toward Okta.

Okta authenticates Priya.

The browser returns the SAML login response to the AcmeHR ACS URL.

AcmeHR processes and accepts the SAML login.

Only then does AcmeHR create its own authenticated application session.
```

Now explain why these two values must not be confused:

```text
http://localhost:8000/saml/acs

urn:acme:training:sp
```

If you can explain both parts without memorized definitions, you are ready for Day 4.

---

# Day 3 completion check

You are finished with Day 3 only when you can prove all of these:

```text
[ ] AcmeHR starts successfully in Docker

[ ] AcmeHR shows SAML configuration Ready

[ ] The Okta custom SAML application exists

[ ] Single sign-on URL is the AcmeHR ACS URL

[ ] Audience URI is the AcmeHR SP Entity ID

[ ] Your test user is assigned

[ ] AcmeHR can load the Okta Metadata URL

[ ] The login starts from AcmeHR

[ ] The browser reaches Okta

[ ] The browser returns to /saml/acs

[ ] AcmeHR accepts the SAML login

[ ] AcmeHR application session becomes Active

[ ] The protected page opens

[ ] The transaction view shows the successful Day 3 baseline

[ ] You can explain the flow without reading the field labels
```

Do not continue to Day 4 until you have one known-good Day 3 transaction.

---

# Official references

The lab steps are aligned with the current product and framework documentation:

- Okta, Create an app integration:  
  https://developer.okta.com/docs/guides/create-an-app-integration/saml2/main/

- Spring Security, SAML 2.0 Login Overview:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/overview.html

- Spring Security, Authenticating SAML Responses:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html
