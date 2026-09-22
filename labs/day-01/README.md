# Day 1 Lab: Understand the SAML Flow Before You Configure Anything

## What this lab is for

Today you are **not** going to configure Okta.

You are also not going to read SAML XML.

Before touching the Admin Console, you need to be able to look at a requirement and understand:

- who the user is
- which system is the Identity Provider
- which system is the Service Provider
- what the browser is doing
- what problem SAML is solving
- whether a requirement belongs to authentication, authorization, or provisioning
- where a login problem may be happening

This is the foundation for everything that comes later.

---

## What you need

You only need:

- the Day 1 lesson
- a text editor, notebook, or paper
- about 30 to 45 minutes

No Okta Admin Console access is required for this lab.

---

# Part 1: Read the project requirement like an engineer

Acme gives you this requirement:

> Employees already sign in to Okta. Acme is onboarding a new HR application called AcmeHR. Employees should use their Okta sign-in to access AcmeHR instead of maintaining a separate AcmeHR password.

Do not think about configuration yet.

Answer these questions first.

### Question 1

Who is trying to access the application?

Write your answer before opening the solution.

<details>
<summary>Check your answer</summary>

The **Acme employee** is the user.

</details>

### Question 2

Which system authenticates the employee?

<details>
<summary>Check your answer</summary>

**Okta** authenticates the employee.

That makes Okta the **Identity Provider (IdP)**.

</details>

### Question 3

Which system is the employee trying to access?

<details>
<summary>Check your answer</summary>

**AcmeHR** is the application the employee wants to access.

That makes AcmeHR the **Service Provider (SP)**.

</details>

### Question 4

What problem is Acme trying to solve?

<details>
<summary>Check your answer</summary>

Acme wants employees to use their existing Okta sign-in to access AcmeHR instead of maintaining another application password.

This is a federation and SSO requirement.

</details>

---

# Part 2: Draw the transaction yourself

Before looking at the answer, draw these four things:

- Employee
- Browser
- AcmeHR
- Okta

Then add arrows showing the high-level login path.

Do not add SAML fields.

Do not add certificates.

Do not add XML.

The purpose is only to understand who the browser moves between.

When you are finished, compare your drawing with this one.

<details>
<summary>Show the reference flow</summary>

```text
Employee
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
   | authenticates the employee
   v
Browser
   |
   | carries the SAML login response
   v
AcmeHR / SP
   |
   | checks the response
   v
AcmeHR application session
```

</details>

Now answer this in one sentence:

> Why is the browser important in this flow?

<details>
<summary>Check your answer</summary>

The browser carries the user and the SAML transaction between AcmeHR and Okta during the normal browser-based SSO flow.

</details>

---

# Part 3: Separate authentication, authorization, and provisioning

An application owner sends you this request:

> We want employees to sign in through Okta. New employees should automatically get an AcmeHR account. HR managers should see the salary-management page. When an employee leaves, their AcmeHR account must be disabled.

There are several different requirements inside that one paragraph.

Classify each one before checking the answers.

## Requirement A

> Employees should sign in through Okta.

Your classification:

```text
____________________________________
```

<details>
<summary>Check your answer</summary>

**Authentication and federation**

</details>

## Requirement B

> New employees should automatically get an AcmeHR account.

Your classification:

```text
____________________________________
```

<details>
<summary>Check your answer</summary>

**Provisioning**

</details>

## Requirement C

> HR managers should see the salary-management page.

Your classification:

```text
____________________________________
```

<details>
<summary>Check your answer</summary>

**Authorization**

</details>

## Requirement D

> When an employee leaves, their AcmeHR account must be disabled.

Your classification:

```text
____________________________________
```

<details>
<summary>Check your answer</summary>

**Provisioning**

</details>

## Why this matters

Imagine all four requirements were reported as:

> "We need SAML."

That would be misleading.

SAML may handle the login and federation part.

It does not automatically solve the full account lifecycle or decide what pages a user can access inside the application.

As an engineer, separate the requirements before deciding what technology or configuration is needed.

---

# Part 4: Check whether you really understand IdP and SP

For each scenario, identify the IdP and SP.

## Scenario 1

> Priya opens AcmeHR. AcmeHR sends her browser to Okta. Okta authenticates her and she returns to AcmeHR.

Write:

```text
IdP: ______________________
SP:  ______________________
```

<details>
<summary>Check your answer</summary>

```text
IdP: Okta
SP:  AcmeHR
```

</details>

## Scenario 2

> Acme later connects another SaaS application called ExpenseCloud to Okta. ExpenseCloud relies on Okta to authenticate Acme employees.

Write:

```text
IdP: ______________________
SP:  ______________________
```

<details>
<summary>Check your answer</summary>

```text
IdP: Okta
SP:  ExpenseCloud
```

</details>

## Scenario 3

> An engineer says, "Okta is always the SP because the user logs in to Okta first."

Is that correct?

<details>
<summary>Check your answer</summary>

No.

The IdP and SP roles are based on what each system does in the federation relationship, not on which page the user happens to see first.

In our course, Okta authenticates the user, so Okta is the IdP.

The application consumes the SAML login result, so the application is the SP.

</details>

---

# Part 5: Put the login steps in order

The following steps are mixed up.

Put them in the order you think a normal high-level login follows.

```text
A. AcmeHR creates an application session.
B. Okta authenticates the employee.
C. The employee tries to access AcmeHR.
D. AcmeHR receives and checks the SAML login response.
E. The browser is sent toward Okta.
F. The browser carries the SAML login response back to AcmeHR.
```

Write your order:

```text
1. ___
2. ___
3. ___
4. ___
5. ___
6. ___
```

<details>
<summary>Check your answer</summary>

```text
1. C  The employee tries to access AcmeHR.
2. E  The browser is sent toward Okta.
3. B  Okta authenticates the employee.
4. F  The browser carries the SAML login response back to AcmeHR.
5. D  AcmeHR receives and checks the SAML login response.
6. A  AcmeHR creates an application session.
```

</details>

If this order does not feel natural yet, redraw the flow from Part 2.

Do not memorize the letters.

Understand the transaction.

---

# Part 6: Your first troubleshooting exercise

An application owner reports:

> Priya opens AcmeHR and reaches Okta. She successfully completes authentication. After that, she returns to AcmeHR and sees an application error.

Do not guess a certificate problem.

Do not guess an MFA problem.

Do not guess that the user is unassigned.

Use the method from the lesson:

> **What is the last step I can prove succeeded?**

Answer these questions.

### Did Priya reach Okta?

<details>
<summary>Check your answer</summary>

Yes.

The report says she reached Okta.

</details>

### Did Okta authenticate Priya?

<details>
<summary>Check your answer</summary>

Yes.

The report says authentication completed successfully.

</details>

### Did the browser return toward AcmeHR?

<details>
<summary>Check your answer</summary>

Yes.

The report says she returned to AcmeHR.

</details>

### Can we prove that AcmeHR accepted the SAML message?

<details>
<summary>Check your answer</summary>

No.

We only know that the browser returned to AcmeHR and the application displayed an error.

We do not yet know whether AcmeHR accepted or rejected the SAML message.

</details>

### What is the next area you would investigate?

<details>
<summary>Check your answer</summary>

Start with the point where AcmeHR receives and processes the returned SAML message.

At this stage of the course, you do not yet know which SAML field might be wrong.

That is fine.

The important skill today is locating the failure area without guessing the cause.

</details>

This is the first troubleshooting habit you should keep:

```text
Do not start with:
"What setting should I change?"

Start with:
"What is the last step I can prove succeeded?"
```

---

# Part 7: Do not confuse a successful login with provisioning

Consider this case:

> Priya successfully authenticates through Okta. AcmeHR accepts the SAML login, but then displays: "No account found for this user."

What can you already say?

Choose the best answer.

### A

Okta authentication definitely failed.

### B

The SAML login may have succeeded, but the application may not have an account for Priya.

### C

The browser failed to reach AcmeHR.

<details>
<summary>Check your answer</summary>

**B**

If AcmeHR accepted the login but cannot find an application account, the problem may be account creation or account matching.

That is different from saying Okta authentication failed.

Later in the course, we will learn where JIT and SCIM fit into this type of requirement.

</details>

---

# Part 8: Practice with a real project conversation

Imagine an application owner tells you:

> We bought a new SaaS application. It supports SAML. We want employees to use Okta. Accounts should be created before employees start. Managers should get additional permissions.

Before asking for any SAML configuration values, write down the three main requirements you hear.

Try it yourself first.

<details>
<summary>Show one good answer</summary>

```text
1. Authentication / federation
   Employees should use Okta to sign in.

2. Provisioning
   Accounts should exist before employees start.

3. Authorization
   Managers need additional application permissions.
```

</details>

Now answer:

> Which of those requirements should you avoid assuming SAML will solve by itself?

<details>
<summary>Check your answer</summary>

Do not assume SAML by itself will provide full account lifecycle provisioning.

Also do not assume that sending identity information automatically creates the correct application permissions.

Those are separate design questions.

</details>

---

# Part 9: Explain the architecture without using jargon

Pretend the AcmeHR application owner does not work in IAM.

Explain the setup without using these terms:

- federation
- assertion
- protocol
- claim
- IdP
- SP

Try to explain it in two or three sentences.

<details>
<summary>Show an example</summary>

> Employees use Okta to prove who they are. After the employee signs in, Okta sends AcmeHR a trusted login message through the browser. AcmeHR checks that message and, if it accepts it, signs the employee into the application.

</details>

If you can explain the design without hiding behind technical vocabulary, you probably understand it.

---

# Part 10: Now explain it as an engineer

This time, use the correct SAML roles.

Complete this explanation in your own words:

> Okta is the __________ because ________________________________.

> AcmeHR is the __________ because ______________________________.

> The browser _________________________________________________.

> SAML helps _________________________________________________.

Do not copy the lesson.

Say it naturally.

<details>
<summary>Show one possible answer</summary>

> Okta is the **Identity Provider** because it authenticates the employee.

> AcmeHR is the **Service Provider** because it is the application the employee wants to access and it relies on the SAML login from Okta.

> The browser carries the user and the SAML transaction between AcmeHR and Okta.

> SAML helps AcmeHR rely on Okta for the employee's login instead of requiring a separate application password.

</details>

---

# Lab challenge

You are the IAM engineer joining an application onboarding call.

The application owner says:

> Our application is called BenefitsCloud. Employees already have Okta accounts. We want Okta login. Users must have a BenefitsCloud account before they can sign in. Only Benefits Administrators should be able to approve benefit changes.

Without configuring anything, produce this short analysis:

```text
User:
Identity Provider:
Service Provider:

Authentication / federation requirement:

Provisioning requirement:

Authorization requirement:

What I would not assume SAML handles automatically:
```

Complete it yourself before opening the reference answer.

<details>
<summary>Show the reference answer</summary>

```text
User:
Acme employee

Identity Provider:
Okta

Service Provider:
BenefitsCloud

Authentication / federation requirement:
Employees should authenticate to BenefitsCloud through Okta.

Provisioning requirement:
The BenefitsCloud account must exist before the user can sign in.

Authorization requirement:
Only Benefits Administrators should be able to approve benefit changes.

What I would not assume SAML handles automatically:
I would not assume SAML creates and maintains the BenefitsCloud
account lifecycle or automatically grants the Benefits Administrator
permission.
```

</details>

---

# Evidence you should be able to produce after this lab

You should now be able to take a simple application requirement and produce:

```text
User
IdP
SP
Browser role
Authentication requirement
Authorization requirement
Provisioning requirement
High-level SAML flow
Last proven successful step during a simple failure
```

That is enough for Day 1.

You are not expected to troubleshoot individual SAML fields yet.

---

# Explain it back

Without looking at the lesson or the answers above, explain this scenario out loud:

> Priya opens AcmeHR. AcmeHR relies on Okta for SAML SSO.

Your explanation should cover:

- who Priya is
- why Okta is the IdP
- why AcmeHR is the SP
- what the browser does
- what happens after Okta authenticates Priya
- why authentication is different from provisioning
- what question you would ask first if the login failed

Do not try to use impressive terminology.

Explain it as if you were helping a new team member understand the project.

---

# Day 1 lab completion check

You are ready to close this lab when you can do all of these without looking up the answers:

- identify the user, IdP, SP, and browser
- draw the high-level SAML login flow
- separate authentication, authorization, and provisioning requirements
- explain why Okta authentication success does not prove the application accepted the SAML login
- explain why a missing application account is not automatically an authentication failure
- start troubleshooting with **"What is the last step I can prove succeeded?"**
- explain the AcmeHR design in plain language
- explain the same design again using the correct SAML roles

Do not move to Day 2 because you finished the page.

Move to Day 2 when the flow makes sense without needing to memorize it.
