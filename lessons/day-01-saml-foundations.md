# Day 1: SAML Foundations

## What you should understand by the end of today

By the end of Day 1, you should be able to look at a simple SAML requirement and answer:

- Who is the user?
- Which system is the Identity Provider?
- Which system is the Service Provider?
- What is the browser doing?
- What problem is SAML solving?
- What is authentication?
- What is authorization?
- What is provisioning?
- Which of those jobs does SAML handle?
- When someone says "the login is failing," what part of the flow might actually be failing?

You do not need to read SAML XML today.

You do not need to know certificates, signatures, Audience, ACS, NameID, or RelayState yet.

Today is about understanding the flow first.

---

## Our project

We will use the same project throughout this course.

Acme has an HR application called **AcmeHR**.

Employees already sign in to Okta.

Acme does not want employees to maintain a separate password just for AcmeHR. The application supports SAML 2.0.

The requirement is:

> Employees should open AcmeHR and sign in using Okta.

This is a **single sign-on (SSO)** requirement. In this project, SSO means AcmeHR relies on the employee's Okta sign-in instead of asking the employee to maintain a separate AcmeHR password.

That sounds simple, but before configuring anything, an engineer needs to understand what each system is responsible for.

---

## Start with the problem

Before we continue, one word needs to be clear.

**Federation** means one system relies on another trusted identity system to authenticate the user instead of handling the whole login by itself.

Without federation, an application may manage its own login.

That can mean:

1. the user opens AcmeHR
2. AcmeHR asks for a username and password
3. AcmeHR checks those credentials
4. AcmeHR decides whether to let the user in

Now Acme wants Okta to handle the login instead.

The application should trust Okta to authenticate the employee.

This is where SAML comes in.

---

## What is SAML?

SAML stands for **Security Assertion Markup Language**.

For now, think of SAML as a standard way for one system to tell another system:

> "I authenticated this user. Here is the information you need to decide whether to let them into your application."

In our project:

- Okta authenticates the employee
- Okta sends a SAML message
- AcmeHR receives that message
- AcmeHR checks it
- AcmeHR creates its own application session if everything is acceptable

An **application session** is simply AcmeHR's own signed-in state for the user after it accepts the login.

That is the basic idea.

We will spend the rest of the course opening each part of that flow.

---

## The three main participants

There are three participants you must understand first.

### 1. The user

The user is the employee trying to access AcmeHR.

Example:

> Priya works at Acme and wants to open the HR application.

Priya is the user.

### 2. The Identity Provider

The **Identity Provider**, usually shortened to **IdP**, is the system that authenticates the user.

In this course:

> Okta is the IdP.

Okta may ask the user for a password and, depending on Acme's policy, another verification step. Using more than one type of verification is called **multi-factor authentication (MFA)**.

The important point is that Okta performs the authentication.

### 3. The Service Provider

The **Service Provider**, usually shortened to **SP**, is the application the user wants to access.

In our project:

> AcmeHR is the SP.

AcmeHR trusts Okta as its Identity Provider.

After Okta authenticates the user, AcmeHR receives the SAML message and decides whether to create an application session.

---

## Where does the browser fit?

The browser is not the IdP and it is not the SP.

The browser carries the user between them.

At a high level, the flow looks like this:

**Question answered:** Who talks to whom during a normal SAML login?

```text
User
 |
 v
Browser
 |
 v
AcmeHR / SP
 |
 | sends the browser to Okta
 v
Okta / IdP
 |
 | authenticates the user
 v
Browser
 |
 | carries the SAML Response
 v
AcmeHR / SP
 |
 | validates the SAML message
 v
Application session
```

One important beginner point:

> Okta normally does not open a direct backend connection to AcmeHR just to complete the normal browser SSO transaction.

The browser carries the SAML messages between the systems.

We will see exactly how that works on Day 2.

---

## What does "trust" mean here?

AcmeHR is not blindly trusting every message it receives.

The application must be configured to trust the correct Identity Provider.

Later in the course, you will learn how the SP checks things such as:

- who issued the SAML message
- whether the message was changed
- whether the message was intended for this application
- whether it is still valid
- whether it belongs to the expected login transaction

Do not worry about those fields today.

For now, remember:

> SAML SSO works because the application is configured to trust a specific Identity Provider and validates the SAML message it receives.

Authentication at Okta is only one part of the transaction.

---

## Authentication, authorization, and provisioning are different jobs

These three terms are often mixed together.

As an engineer, you need to separate them early.

### Authentication

Authentication answers:

> Who is this user?

Example:

Priya enters her credentials in Okta and completes MFA.

Okta confirms that the person signing in is Priya.

That is authentication.

### Authorization

Authorization answers:

> What is this user allowed to do?

Example:

Priya may be allowed to open AcmeHR but may not be allowed to access the payroll administrator page.

That is authorization.

SAML can carry information such as groups or roles that the application may use when making authorization decisions.

But the application still decides how that information maps to permissions.

### Provisioning

Provisioning answers:

> Does the user's account exist in the application, and what should happen to that account during the user's lifecycle?

Examples:

- create Priya's account
- update her department
- disable her account when she leaves
- remove application access

SAML is not a full lifecycle provisioning protocol.

Later we will cover two common ways account creation and lifecycle can be handled. **JIT**, or just-in-time provisioning, can create an account when the user first signs in. **SCIM** is commonly used to create, update, and disable accounts through a provisioning interface.

---

## A simple comparison

```text
Question                              Main concern
-------------------------------------------------------------
Who is the user?                      Authentication
What can the user do?                 Authorization
Does the account exist and stay       Provisioning
correct through its lifecycle?
```

Keep these separate.

A lot of bad troubleshooting starts when someone treats all three as the same problem.

---

## What SAML does in our project

For the AcmeHR integration, SAML mainly helps with federation and authentication between Okta and the application.

A simplified transaction is:

```text
Priya wants AcmeHR
        |
        v
AcmeHR sends her toward Okta
        |
        v
Okta authenticates Priya
        |
        v
Okta creates a SAML Response
        |
        v
Browser carries it to AcmeHR
        |
        v
AcmeHR validates it
        |
        v
AcmeHR creates its own session
```

Later we will open every one of those steps.

---

## What SAML does not automatically do

A working SAML login does not automatically mean:

- the user was pre-created in the application
- the user's department was synchronized
- the user's account will be disabled when they leave
- application permissions are automatically correct
- Okta and the application share the same session
- every application supports logout in the same way

Those are different design questions.

This distinction will matter throughout the course.

---

## SAML versus OIDC and OAuth

You do not need to become an OAuth or OIDC expert in this course.

You only need enough context to avoid mixing the protocols.

### SAML

SAML is commonly used for enterprise browser-based federation and SSO.

In our course, Okta sends a SAML Response to the application after authentication.

### OIDC

OpenID Connect, usually called **OIDC**, is also used for authentication and federation.

It is commonly seen in modern web and mobile applications.

OIDC extends OAuth 2.0 with user authentication and SSO. It commonly uses an **ID token** to carry information about the authenticated user instead of using a SAML Response.

### OAuth

OAuth 2.0 is mainly about delegated authorization.

A simple example is:

> An application receives limited permission to call an API on behalf of a user.

That permission is usually represented by an access token with a defined scope.

OAuth 2.0 by itself is not the same thing as user authentication. OIDC adds the identity and sign-in layer on top of OAuth 2.0.

For this course, the important distinction is:

```text
SAML    -> federation and authentication using SAML messages
OIDC    -> authentication using identity tokens
OAuth   -> delegated authorization for protected resources or APIs
```

Real systems can use these technologies in different combinations.

Do not choose a protocol based only on which one sounds newer.

Choose based on what the application supports and what the integration requires.

---

## A requirement can contain more than one problem

Imagine an application owner tells you:

> We need SSO through Okta. The user must be created automatically. Finance users should get the Finance Admin role.

There are three different concerns inside that one sentence.

### "We need SSO through Okta"

That is the authentication and federation requirement.

SAML may solve that part.

### "The user must be created automatically"

That is a provisioning requirement.

SAML alone does not automatically provide full lifecycle provisioning.

### "Finance users should get the Finance Admin role"

That is an authorization requirement.

SAML may carry a group or role claim, but the application decides how that value becomes application access.

This is how an engineer should break down a requirement before touching the Okta Admin Console.

---

## Think like the engineer before configuring

Before creating a SAML application, ask simple questions.

For AcmeHR:

### Who authenticates the employee?

Okta.

Therefore Okta is the IdP.

### Which application is the employee trying to access?

AcmeHR.

Therefore AcmeHR is the SP.

### Who carries the transaction between them?

The user's browser.

### What are we trying to achieve?

SSO into AcmeHR using the user's Okta authentication.

### Do we also need account creation or deactivation?

That is a separate lifecycle question.

### Does AcmeHR need groups or other user information?

That is an attribute and application-authorization question that we will handle later.

This small habit prevents a lot of confusion.

---

## A first look at a real support conversation

An application owner says:

> The SAML login is broken.

That is not enough information.

A better engineer starts narrowing the timeline.

Ask:

> Did the user reach Okta?

Then:

> Did Okta authenticate the user?

Then:

> Did the browser return to the application?

Then:

> Did the application reject the SAML message, or did it create a session?

You are already using the troubleshooting method that will stay with us for the entire course:

> **What is the last step I can prove succeeded?**

You do not need to know every SAML field yet to start thinking this way.

---

## Common beginner confusion

### "Okta authenticated the user, so SAML worked."

Not necessarily.

Okta authentication can succeed and the application can still reject the SAML message.

Later we will see failures involving the application identifier, destination, time conditions, certificates, attributes, and other checks.

### "The user can authenticate, so their application account must exist."

Not necessarily.

Authentication and provisioning are different.

### "The user is in the right Okta group, so the application permission must be correct."

Not necessarily.

A group may affect Okta assignment, may be sent as a SAML claim, or may be interpreted by the application.

Those are different layers.

### "SAML and OAuth are basically the same thing."

No.

They solve different problems even though both appear in identity integrations.

---

## Day 1 exercise: classify the requirement

Read each requirement and decide which area it belongs to **before opening the answer**.

### Requirement 1

> Employees should sign in to AcmeHR using Okta.

<details>
<summary>Check your answer</summary>

**Authentication and federation**

</details>

### Requirement 2

> New employees should have an AcmeHR account before their first day.

<details>
<summary>Check your answer</summary>

**Provisioning**

</details>

### Requirement 3

> Only members of the HR-Managers group should see salary reports.

<details>
<summary>Check your answer</summary>

**Authorization**

</details>

### Requirement 4

> When an employee leaves Acme, their AcmeHR account must be disabled.

<details>
<summary>Check your answer</summary>

**Provisioning**

</details>

### Requirement 5

> AcmeHR should trust Okta to authenticate employees.

<details>
<summary>Check your answer</summary>

**Federation**

</details>

If you can separate these requirements correctly, you are already thinking more clearly about the integration.

---

## Day 1 exercise: identify the roles

Requirement:

> Acme employees open AcmeHR. AcmeHR redirects them to Okta for login. After successful authentication, they return to AcmeHR.

Before opening the answer, identify the user, IdP, SP, and browser's role.

<details>
<summary>Check your answer</summary>

```text
User                 -> Acme employee
Identity Provider    -> Okta
Service Provider     -> AcmeHR
Transaction carrier  -> Browser
```

</details>

Do not move forward until this feels obvious.

---

## What you should not memorize today

Do not try to memorize protocol fields.

You do not need to know:

- AuthnRequest XML
- SAML Response XML
- Assertion structure
- ACS details
- Audience
- Destination
- Recipient
- InResponseTo
- NameID
- signatures
- certificates

Those concepts will make more sense after you understand the browser flow and build a working transaction.

---

## Day 1 checkpoint

Before moving to Day 2, you should be able to explain all of these without reading the lesson:

1. What problem is Acme trying to solve with SAML?
2. Why is Okta the IdP?
3. Why is AcmeHR the SP?
4. What does the browser do?
5. What is authentication?
6. What is authorization?
7. What is provisioning?
8. Why does a successful Okta login not automatically prove the application accepted the SAML transaction?
9. Why is SAML not a replacement for lifecycle provisioning?
10. At a high level, how is SAML different from OIDC and OAuth?

If any answer still feels unclear, review that section before moving on.

---

## Explain it back

Imagine a new application owner asks:

> What exactly is happening when we say AcmeHR uses Okta for SAML SSO?

Explain it naturally, as if you were speaking to them.

A good answer could sound like this:

> AcmeHR is the Service Provider and Okta is the Identity Provider. When the employee tries to access AcmeHR, the browser is sent to Okta for authentication. After Okta authenticates the employee, Okta creates a SAML Response and the browser sends it back to AcmeHR. AcmeHR validates that message before creating its own application session. SAML handles the federation and login exchange, but things like full account lifecycle provisioning and application permissions are separate concerns.

Do not memorize those exact words.

If you can explain the same idea correctly in your own words, you understand Day 1.

---

## Day 1 completion standard

Day 1 is complete when you can:

- identify the user, IdP, SP, and browser in a requirement
- explain SAML in plain language
- explain the high-level login flow
- separate authentication, authorization, and provisioning
- explain why SAML SSO does not automatically provide lifecycle management
- distinguish SAML, OIDC, and OAuth at a practical high level
- begin troubleshooting by asking what the last proven successful step was
- explain the AcmeHR scenario in your own words

Day 2 will build the browser and HTTP knowledge needed to see how the SAML transaction actually moves between the SP and Okta.
