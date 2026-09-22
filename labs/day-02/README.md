# Day 2 Lab: Follow the Browser and Decode the Transport

## What this lab is for

Day 1 taught you **who** is involved in the SAML login.

Day 2 taught you **how the browser carries the transaction**.

In this lab, you will practice that transport layer without configuring Okta yet.

You will:

- inspect a real browser request in Developer Tools
- recognize a redirect
- identify query parameters
- recognize a form POST
- distinguish GET from POST
- identify useful HTTP status codes
- decode Base64 locally
- decode a sample SAMLResponse locally
- reverse the Redirect-binding steps for a sample SAMLRequest
- practice finding the last transport step that succeeded

You are still **not** expected to interpret the SAML XML itself.

That starts later.

---

# Before you begin

You need:

- a desktop browser such as Chrome, Edge, or Firefox
- browser Developer Tools
- a local Python 3 installation for the decoding exercises
- the Day 2 lesson

The commands below use `python`. If your computer starts Python with `python3` or `py` instead, use that command in the same examples.

You do **not** need:

- Okta Admin Console access
- a working SAML application
- certificates
- production SAML messages
- any online SAML decoder

If your browser labels a Network field slightly differently from the screenshots or wording you may see elsewhere, that is fine.

Focus on the evidence, not the exact button location.

---

# Safety rule for this lab

Use only the sample values provided here.

Do not copy production SAML messages, session cookies, passwords, MFA codes, or private keys into this lab.

Do not paste real production SAML messages into public decoder websites.

---

# Part 1: Inspect one normal browser request

We will start with an ordinary web request so you can become comfortable with the Network panel before looking at SAML-shaped traffic.

Open this repository in your browser:

```text
https://github.com/TedxHarry/SAML
```

Then:

1. open Developer Tools
2. open the **Network** tab
3. clear the existing entries if needed
4. reload the page
5. find the main document request for the repository page

Do not worry if many other requests appear.

Modern web pages load scripts, images, fonts, API calls, and other resources.

We only want the main page request.

---

## What to inspect

Find these values for the main document request:

```text
Request URL:
Request Method:
Status Code:
Host:
```

Your browser may display them under **Headers**, **General**, or a similar section.

Write down what you see.

### What should you expect?

<details>
<summary>Check the idea</summary>

The exact status can vary depending on browser state and GitHub behavior, but you should see an HTTP request to GitHub.

The main lesson is that the browser records evidence such as:

- the URL it requested
- the HTTP method
- the returned status
- request and response headers

Do not worry about memorizing every header.

</details>

---

# Part 2: Understand what a redirect trace would tell you

Now imagine the Network panel shows these two requests in order:

```text
Request 1
---------
URL:    https://acmehr.example.com/login
Method: GET
Status: 302

Response header:
Location: https://acme.okta.com/app/example/sso/saml?SAMLRequest=ABC123&RelayState=payroll


Request 2
---------
URL:    https://acme.okta.com/app/example/sso/saml?SAMLRequest=ABC123&RelayState=payroll
Method: GET
Status: 200
```

Do not interpret the SAMLRequest value.

Just follow the browser.

---

## Question 1

Which server returned the redirect?

<details>
<summary>Check your answer</summary>

**AcmeHR**

The first request went to:

```text
https://acmehr.example.com/login
```

and that response returned status 302.

</details>

## Question 2

Where did the server tell the browser to go next?

<details>
<summary>Check your answer</summary>

To the Okta URL in the **Location** response header.

</details>

## Question 3

Did AcmeHR open a backend connection to Okta in this trace?

<details>
<summary>Check your answer</summary>

That is not what this evidence shows.

The trace shows AcmeHR returning a redirect to the **browser**.

The browser then made a new request to Okta.

</details>

## Question 4

Which values are visible as query parameters in the Okta URL?

<details>
<summary>Check your answer</summary>

```text
SAMLRequest=ABC123
RelayState=payroll
```

You do not need to understand their internal meaning yet.

</details>

---

# Part 3: Recognize the return POST

After Priya authenticates, imagine the Network panel shows:

```text
Request
-------
URL:    https://acmehr.example.com/sso
Method: POST
Status: 302

Form data:
SAMLResponse=PHNhbWxwOlJlc3BvbnNlLi4u
RelayState=payroll
```

Answer these questions before checking the solutions.

---

## Question 1

Is the SAMLResponse shown as part of the URL?

<details>
<summary>Check your answer</summary>

No.

In this example, it is shown as **form data** in the POST request.

</details>

## Question 2

Which HTTP method carried the response back to AcmeHR?

<details>
<summary>Check your answer</summary>

**POST**

</details>

## Question 3

What can you prove from this trace?

<details>
<summary>Check your answer</summary>

You can prove that the browser sent a POST request containing a SAMLResponse value to AcmeHR.

You cannot prove from this transport evidence alone that AcmeHR trusted and accepted the SAML message.

</details>

That last distinction matters.

```text
Message reached AcmeHR
        !=
AcmeHR accepted the SAML message
```

---

# Part 4: Inspect a simple HTML form

Here is a simplified form:

```html
<form method="post" action="https://acmehr.example.com/sso">
    <input type="hidden" name="SAMLResponse" value="ABC123" />
    <input type="hidden" name="RelayState" value="payroll" />
</form>
```

Do not study the HTML syntax.

Look only for the three pieces Day 2 taught.

Write your answers first.

---

## What HTTP method will the browser use?

<details>
<summary>Check your answer</summary>

```text
POST
```

because the form says:

```text
method="post"
```

</details>

## Where will the browser send the form?

<details>
<summary>Check your answer</summary>

```text
https://acmehr.example.com/sso
```

because that is the form's `action`.

</details>

## Which field carries the SAML login message?

<details>
<summary>Check your answer</summary>

```text
SAMLResponse
```

</details>

---

# Part 5: Prove to yourself that Base64 is not encryption

Open a terminal or command prompt.

Run:

```text
python -c "import base64; print(base64.b64decode('SGVsbG8=').decode())"
```

You should get:

```text
Hello
```

No password was required.

No decryption key was required.

That is the point.

Base64 is an encoding.

It changes how data is represented.

It does not make the data confidential.

---

## Try the reverse

Run:

```text
python -c "import base64; print(base64.b64encode(b'Hello').decode())"
```

You should get:

```text
SGVsbG8=
```

You can go both directions without a secret key.

That would not be true of properly encrypted data.

---

# Part 6: Decode a sample SAMLResponse locally

Here is a safe training value.

It does not contain real identity data.

```text
PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIElEPSJfZGF5Mi1yZXNwb25zZSIgVmVyc2lvbj0iMi4wIiBJc3N1ZUluc3RhbnQ9IjIwMjYtMDktMjFUMTI6MDA6MDVaIj48L3NhbWxwOlJlc3BvbnNlPg==
```

This represents a small training SAMLResponse sent using the POST-style encoding path we discussed.

Run:

```text
python -c "import base64; v='PHNhbWxwOlJlc3BvbnNlIHhtbG5zOnNhbWxwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiIElEPSJfZGF5Mi1yZXNwb25zZSIgVmVyc2lvbj0iMi4wIiBJc3N1ZUluc3RhbnQ9IjIwMjYtMDktMjFUMTI6MDA6MDVaIj48L3NhbWxwOlJlc3BvbnNlPg=='; print(base64.b64decode(v).decode())"
```

You should see XML beginning with:

```text
<samlp:Response
```

Stop there.

Do **not** study the XML fields yet.

The Day 2 lesson is only proving:

```text
Base64 SAMLResponse
        |
        v
Base64 decode
        |
        v
XML
```

---

# Part 7: Decode a sample Redirect-binding SAMLRequest

The request side is different.

Here is a safe training SAMLRequest value after Redirect-binding preparation and URL encoding:

```text
ZY7BCsIwEER%2Fpew9muYguDSFgpeCXlQ8eJFQAxaSTc1uQP%2FeoEdhTm8ew3TsYlhwKPKgo38Wz9K8YiDGb2GhZMLkeGYkFz2jTHgaDns0K41LTpKmFKAZdxZud%2Fc2Kv9GoLn4zHMiC9WsAnPxI7E4koq02Si9VaY9twa1rrlC363%2Fz%2FQf
```

This value needs more than a simple Base64 decode.

We will reverse the transport steps.

---

## Step 1: URL decode

Run:

```text
python -c "import urllib.parse; v='ZY7BCsIwEER%2Fpew9muYguDSFgpeCXlQ8eJFQAxaSTc1uQP%2FeoEdhTm8ew3TsYlhwKPKgo38Wz9K8YiDGb2GhZMLkeGYkFz2jTHgaDns0K41LTpKmFKAZdxZud%2Fc2Kv9GoLn4zHMiC9WsAnPxI7E4koq02Si9VaY9twa1rrlC363%2Fz%2FQf'; print(urllib.parse.unquote(v))"
```

You should get a Base64-looking value:

```text
ZY7BCsIwEER/pew9muYguDSFgpeCXlQ8eJFQAxaSTc1uQP/eoEdhTm8ew3TsYlhwKPKgo38Wz9K8YiDGb2GhZMLkeGYkFz2jTHgaDns0K41LTpKmFKAZdxZud/c2Kv9GoLn4zHMiC9WsAnPxI7E4koq02Si9VaY9twa1rrlC363/z/Qf
```

That is not readable XML yet.

---

## Step 2: Base64 decode and DEFLATE decompress

Run:

```text
python -c "import base64,zlib; v='ZY7BCsIwEER/pew9muYguDSFgpeCXlQ8eJFQAxaSTc1uQP/eoEdhTm8ew3TsYlhwKPKgo38Wz9K8YiDGb2GhZMLkeGYkFz2jTHgaDns0K41LTpKmFKAZdxZud/c2Kv9GoLn4zHMiC9WsAnPxI7E4koq02Si9VaY9twa1rrlC363/z/Qf'; print(zlib.decompress(base64.b64decode(v), -15).decode())"
```

You should see XML beginning with:

```text
<samlp:AuthnRequest
```

Again, stop there.

The point is not to interpret the request yet.

The point is to prove the transport path:

```text
URL decode
    |
    v
Base64 decode
    |
    v
DEFLATE decompress
    |
    v
XML
```

---

# Part 8: Why the wrong decoder fails

Now compare the two paths.

## POST-style SAMLResponse

```text
Base64 decode
    |
    v
XML
```

## Redirect-binding SAMLRequest

```text
URL decode
    |
    v
Base64 decode
    |
    v
DEFLATE decompress
    |
    v
XML
```

If you try to treat both values exactly the same way, one of them may fail.

That does not automatically mean the SAML message is bad.

It may mean you used the wrong decoding process for that binding.

---

# Part 9: Browser tools may already decode part of the value

This is an important practical detail.

A browser's Developer Tools may show a query parameter after URL decoding it for you.

That means you might see:

```text
/
```

instead of:

```text
%2F
```

Do not blindly URL-decode a value again just because a checklist says "URL decode first."

Look at what the tool is actually showing you.

The correct question is:

> What representation do I currently have?

Then perform only the remaining steps.

---

# Part 10: Read a small transport trace

You are given this evidence:

```text
1. GET https://acmehr.example.com/
   Status: 302

2. GET https://acme.okta.com/...?...SAMLRequest=...
   Status: 200

3. POST https://acmehr.example.com/sso
   Form data includes SAMLResponse
   Status: 500
```

Answer each question before checking.

---

## Did the browser reach AcmeHR initially?

<details>
<summary>Check your answer</summary>

Yes.

Step 1 proves the browser requested AcmeHR.

</details>

## Did AcmeHR redirect the browser toward Okta?

<details>
<summary>Check your answer</summary>

Yes.

Step 1 returned 302 and Step 2 shows the browser requesting Okta.

</details>

## Did the browser return toward AcmeHR with a SAMLResponse?

<details>
<summary>Check your answer</summary>

Yes.

Step 3 shows a POST to AcmeHR containing a SAMLResponse form value.

</details>

## Can you prove AcmeHR accepted the SAML message?

<details>
<summary>Check your answer</summary>

No.

AcmeHR returned status 500 while processing the POST.

You need more application-side evidence to determine the exact cause.

</details>

## What is the last transport step you can prove succeeded?

<details>
<summary>Check your answer</summary>

The browser successfully POSTed the SAMLResponse to AcmeHR.

That is the last transport step you can prove succeeded from this trace.

</details>

---

# Part 11: Do not over-read an HTTP status code

Consider:

```text
POST https://acmehr.example.com/sso
Status: 500
```

A weak conclusion would be:

> The certificate is wrong.

Why is that weak?

<details>
<summary>Check your answer</summary>

Because the HTTP 500 only tells you that the server encountered a problem while handling the request.

It does not identify the exact SAML root cause.

You need application logs or other evidence before deciding whether the problem involves a certificate, configuration, code, or something else.

</details>

Now consider:

```text
GET https://acmehr.example.com/login
Status: 302
Location: https://acme.okta.com/...
```

What useful fact does that prove?

<details>
<summary>Check your answer</summary>

It proves that AcmeHR responded by directing the browser toward Okta.

</details>

---

# Part 12: Separate the two browser sessions

Priya completes SSO and has reached AcmeHR.

Her browser may now have:

```text
Okta-related cookie/session state

and

AcmeHR-related cookie/session state
```

Are those automatically one shared session?

<details>
<summary>Check your answer</summary>

No.

Okta and AcmeHR maintain separate session state.

The browser can hold cookies for both systems.

</details>

Why should you avoid copying active session cookies into a ticket?

<details>
<summary>Check your answer</summary>

An active session cookie can be sensitive and may allow access to a signed-in session.

Do not expose it unnecessarily.

</details>

---

# Part 13: Mini incident 1

An application owner says:

> Priya clicks AcmeHR. The application immediately shows an error. The Network panel never shows a request to Okta.

What is the last step you can prove succeeded?

<details>
<summary>Check your answer</summary>

You can prove the browser reached AcmeHR.

You cannot prove that AcmeHR successfully started the redirect toward Okta.

Start investigating before the Okta authentication step.

</details>

Should you begin by decoding a SAMLResponse?

<details>
<summary>Check your answer</summary>

No.

There is no evidence that a SAMLResponse was created or returned yet.

</details>

---

# Part 14: Mini incident 2

The trace shows:

```text
AcmeHR request              -> succeeded
Redirect to Okta            -> succeeded
Okta page                   -> reached
Authentication              -> completed
POST back to AcmeHR         -> not present
```

What is the last step you can prove succeeded?

<details>
<summary>Check your answer</summary>

Okta authentication completed.

You have not yet proved that the browser completed the return POST to AcmeHR.

</details>

Where should you look next?

<details>
<summary>Check your answer</summary>

Look at what happened after authentication in the browser.

You are trying to determine whether the browser received and submitted the return form toward AcmeHR.

</details>

---

# Part 15: Mini incident 3

The trace shows:

```text
AcmeHR request              -> succeeded
Redirect to Okta            -> succeeded
Okta authentication         -> succeeded
POST SAMLResponse to AcmeHR -> succeeded
AcmeHR returned error       -> yes
```

Which statement is strongest?

### A

SAML definitely succeeded.

### B

Okta authentication and browser return transport succeeded, but AcmeHR may still have rejected or failed while processing the SAML login.

### C

The user was definitely not assigned in Okta.

<details>
<summary>Check your answer</summary>

**B**

The trace proves transport through the return POST.

It does not prove that AcmeHR accepted the SAML message or created an application session.

</details>

---

# Part 16: Build your Day 2 transport map

Without looking at the lesson, fill in the blanks.

```text
1. Browser requests __________________________

2. AcmeHR returns a __________________________

3. Browser follows it to _____________________

4. Okta authenticates ________________________

5. Okta returns an HTML ______________________

6. Browser sends an HTTP _____________________

7. The form contains _________________________

8. AcmeHR processes the ______________________
```

<details>
<summary>Show one good answer</summary>

```text
1. Browser requests AcmeHR

2. AcmeHR returns a redirect

3. Browser follows it to Okta

4. Okta authenticates the user

5. Okta returns an HTML form

6. Browser sends an HTTP POST

7. The form contains SAMLResponse

8. AcmeHR processes the login request
```

</details>

---

# Part 17: Lab challenge

You receive this browser evidence:

```text
Request 1
URL:    https://acmehr.example.com/
Method: GET
Status: 302
Location: https://acme.okta.com/app/acmehr/sso/saml?SAMLRequest=...

Request 2
URL:    https://acme.okta.com/app/acmehr/sso/saml?SAMLRequest=...
Method: GET
Status: 200

Request 3
URL:    https://acmehr.example.com/sso
Method: POST
Form data:
  SAMLResponse=...
Status: 403
```

Write a short engineer's analysis using this format:

```text
Observed symptom:

Last transport step I can prove succeeded:

Evidence:

What I know:

What I do not know yet:

Where I would investigate next:
```

Do not guess the root cause.

<details>
<summary>Show one good analysis</summary>

```text
Observed symptom:
The browser returns to AcmeHR after Okta authentication, but AcmeHR returns 403.

Last transport step I can prove succeeded:
The browser POSTed a SAMLResponse to AcmeHR.

Evidence:
The Network trace shows a POST to https://acmehr.example.com/sso
with SAMLResponse form data.

What I know:
The initial AcmeHR request occurred.
AcmeHR redirected the browser toward Okta.
The browser reached Okta.
The browser later POSTed a SAMLResponse back to AcmeHR.

What I do not know yet:
I do not yet know whether AcmeHR rejected the SAML message,
rejected the user, or denied access for another application-side reason.

Where I would investigate next:
AcmeHR's processing of the returned login, using application-side
evidence together with the SAML message when later lessons teach
those validation details.
```

</details>

The important part is that the analysis stops where the evidence stops.

---

# Part 18: Explain the encoding paths from memory

Without looking above, write the two paths.

## Redirect-binding request

```text
SAML XML
   ->
________________
   ->
________________
   ->
________________
   ->
URL
```

<details>
<summary>Check your answer</summary>

```text
SAML XML
   ->
DEFLATE compress
   ->
Base64 encode
   ->
URL encode
   ->
URL
```

</details>

## POST-binding response

```text
SAML XML
   ->
________________
   ->
HTML form
   ->
Browser POST
```

<details>
<summary>Check your answer</summary>

```text
SAML XML
   ->
Base64 encode
   ->
HTML form
   ->
Browser POST
```

</details>

---

# What evidence you should be able to collect after this lab

At Day 2 level, you should be comfortable collecting:

```text
Request URL
HTTP method
HTTP status
Redirect Location
Query parameters
POST form data
Order of browser requests
Last transport step that succeeded
```

You are not yet expected to diagnose:

- signature validation
- certificates
- Audience
- Destination
- Recipient
- InResponseTo
- NameID
- attribute mapping

Those belong to later lessons.

---

# Explain it back

Imagine a fresher asks:

> Why do I need browser Developer Tools for SAML troubleshooting?

Explain it without using complicated language.

Your answer should include:

- why the browser matters
- what a redirect proves
- what a POST proves
- where SAMLRequest may appear
- where SAMLResponse may appear
- why transport success does not prove SAML acceptance
- why Base64 does not make the message secret

A good explanation might sound like:

> The browser carries the normal SAML login between the application and Okta, so the Network trace lets us see whether each part of that transport actually happened. We can see the redirect toward Okta, query parameters such as SAMLRequest, and the POST back to the application containing SAMLResponse. If the POST reaches the application, that proves the browser delivered it, but it does not prove the application accepted it. Base64 is only an encoding, so we can decode training messages locally without a secret key.

Do not memorize those words.

Say it in your own way.

---

# Day 2 lab completion check

You are ready to close this lab when you can do all of these without copying the answers:

- inspect a normal request in browser Developer Tools
- identify request URL, method, and status
- recognize a redirect and find its next location
- recognize query parameters
- recognize an HTML form POST
- explain where SAMLRequest and SAMLResponse commonly travel
- locally decode a Base64 training value
- locally decode the provided training SAMLResponse
- reverse the provided Redirect-binding SAMLRequest transport steps
- explain Base64, compression, and encryption as different things
- explain why browser tools may already URL-decode a value
- identify the last successful transport step from a trace
- avoid claiming more than the evidence proves
- explain why Okta and AcmeHR browser sessions are separate

Do not move forward because you ran the commands once.

Move forward when you can look at a simple browser trace and explain what actually happened.
