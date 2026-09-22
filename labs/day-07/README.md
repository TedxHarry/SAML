# Day 7 Lab: Send Claims to AcmeHR and Prove Application Authorization

## What you are going to do

The SAML login already works.

Today you will add identity data to that working login and prove what AcmeHR does with it.

You will:

- prepare profile values for one Okta test user
- create AcmeHR-specific Okta groups
- configure current SAML custom claims on the Okta app
- send email, first name, last name, employee number, and department
- send selected AcmeHR group names as one multi-valued claim
- capture and decode the live SAML Response
- prove the claim names and values in the AttributeStatement
- prove that AcmeHR reads the validated claims
- prove that AcmeHR maps only an approved group value to a manager role
- deliberately break one attribute name
- deliberately remove one required source value
- deliberately break the group filter
- restore the known-good claim configuration after every failure

The evidence chain is:

~~~text
Okta source value
        |
        v
Okta claim expression
        |
        v
SAML AttributeStatement
        |
        v
AcmeHR claim reading
        |
        v
AcmeHR authorization
~~~

Do not skip a layer.

---

# Before you start

You need:

- the working Okta SAML application from Day 3
- the assigned Okta test user
- the working AcmeHR training SP
- the Day 5 SAML Response decoding workflow
- the Day 6 validation understanding
- Docker with Docker Compose
- Python 3
- a browser with Developer Tools

Your Day 6 baseline must still work.

Confirm:

~~~text
AcmeHR SAML configuration
    Ready

SP-initiated login reaches Okta
    PASS

Browser returns to /saml/acs
    PASS

AcmeHR protected page opens
    PASS
~~~

Do not build the claims lab on top of a broken authentication baseline.

---

# Safety rule for this lab

A SAML Assertion can contain personal data.

Today you will intentionally add:

- email
- first name
- last name
- employee number
- department
- group membership

Keep your complete live Assertion on your own machine.

Do not paste it into:

- public SAML decoders
- forums
- public issues
- shared chats
- unapproved troubleshooting websites

If you save evidence, redact tenant-specific and personal values.

Use a dedicated test user rather than a production employee.

---

# Part 1: Keep assignment and claims separate

Do not change the Day 3 application assignment yet.

Keep the same test user assigned directly to the AcmeHR Training application.

That gives us a clean experiment:

~~~text
Application assignment
    stays unchanged

SAML claims
    will change

AcmeHR authorization
    will change only when claim values change
~~~

This separation matters.

If we changed assignment, claim generation, and application authorization at the same time, a failure would be harder to isolate.

---

# Part 2: Prepare the test user's profile values

Open your Okta test user.

Use these training values or equivalent non-production values:

| Profile property | Training value |
| --- | --- |
| email | your test user's email |
| firstName | Priya |
| lastName | Shah |
| employeeNumber | E10427 |
| department | Finance |

The current unified claims model can reference Okta user-profile properties with EL for OIE expressions such as:

~~~text
user.profile.email
user.profile.firstName
user.profile.lastName
user.profile.employeeNumber
user.profile.department
~~~

Confirm that each source property contains a value before creating the claims.

Do not assume the expression is wrong if the underlying profile property is empty.

---

# Part 3: Create or reuse the AcmeHR groups

For this lab, use these Okta groups:

~~~text
AcmeHR-Employees
AcmeHR-Managers
~~~

Optionally, you can also create:

~~~text
AcmeHR-Payroll
~~~

Add the test user to:

~~~text
AcmeHR-Employees
AcmeHR-Managers
~~~

Do not use unrelated organization groups for the exercise.

The prefix gives us an easy, visible filter:

~~~text
AcmeHR-
~~~

---

# Part 4: Prove group membership before configuring the claim

Open the test user's group memberships.

Record:

~~~text
AcmeHR-Employees
    MEMBER

AcmeHR-Managers
    MEMBER
~~~

This proves only Okta membership.

It does not yet prove that either group appears in the SAML Assertion.

That distinction is part of today's lab.

---

# Part 5: Open the current custom-claims interface

Open:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Sign On
~~~

Current Okta Identity Engine documentation manages custom SAML claims in the:

~~~text
Attribute Statements
~~~

section on the Sign On tab.

Use:

~~~text
Add expression
~~~

to create each current-style custom claim.

If you see:

~~~text
Show legacy configuration
~~~

do not switch to the legacy interface for this lab.

We are using the current claims model first.

---

# Part 6: Add the email claim

Add an expression with:

~~~text
Name
    email

Expression
    user.profile.email
~~~

Save it.

Read the two sides aloud:

~~~text
AcmeHR claim name
    email

Okta source
    user.profile.email
~~~

They are not the same kind of value.

---

# Part 7: Add firstName and lastName

Create:

~~~text
Name
    firstName

Expression
    user.profile.firstName
~~~

Then:

~~~text
Name
    lastName

Expression
    user.profile.lastName
~~~

Save each expression.

Use the exact claim names shown here for the training application.

Do not change their case.

---

# Part 8: Add employeeNumber

Create:

~~~text
Name
    employeeNumber

Expression
    user.profile.employeeNumber
~~~

Save it.

This claim is required by the AcmeHR training contract.

Expected training value:

~~~text
E10427
~~~

The claim name is employeeNumber.

The value is E10427.

Keep those concepts separate.

---

# Part 9: Add department

Create:

~~~text
Name
    department

Expression
    user.profile.department
~~~

Save it.

Expected training value:

~~~text
Finance
~~~

At this point the application should have five single-valued claims configured:

~~~text
email
firstName
lastName
employeeNumber
department
~~~

---

# Part 10: Add the current groups expression

Create one more claim.

Use:

~~~text
Name
    groups
~~~

For the expression, use:

~~~text
user.getGroups({'group.profile.name': 'AcmeHR-', 'operator': 'STARTS_WITH'}).![profile.name]
~~~

Read the expression from left to right:

~~~text
user.getGroups(...)
    find groups for this user

group.profile.name
    filter using the group name

AcmeHR-
    keep names that start with this prefix

.![profile.name]
    return only each matching group's name
~~~

The expected result for our test user is a collection containing:

~~~text
AcmeHR-Employees
AcmeHR-Managers
~~~

The order is not an authorization contract.

AcmeHR should check whether the collection contains a required value.

---

# Part 11: Why we use this expression

The current Okta Expression Language supports filtering user groups by properties including:

- group ID
- group source ID
- group type
- group profile name

The group-name filter supports STARTS_WITH.

The collection projection:

~~~text
.![profile.name]
~~~

turns the returned group objects into a list of group names.

That is what AcmeHR needs for this training authorization example.

Do not use the older Groups.startsWith(...) style for this new claim.

Those functions belong to the legacy group-claims configuration model.

---

# Part 12: Record the known-good claim contract

Before testing, write this table in your private lab notes.

| SAML claim | Okta expression | Expected result |
| --- | --- | --- |
| email | user.profile.email | test user's email |
| firstName | user.profile.firstName | Priya |
| lastName | user.profile.lastName | Shah |
| employeeNumber | user.profile.employeeNumber | E10427 |
| department | user.profile.department | Finance |
| groups | user.getGroups(...).![profile.name] | AcmeHR-Employees, AcmeHR-Managers |

This is the IdP-to-SP data contract for the lab.

---

# Part 13: Do not restart AcmeHR just because claims changed

You changed the Okta application's SAML claim configuration.

You did not change:

- the SP Entity ID
- the ACS URL
- the Okta Metadata URL
- the IdP signing certificate

The running AcmeHR instance can use the new claims on the next SAML login.

You need a new authentication transaction, not a new federation configuration.

---

# Part 14: Start a fresh transaction

Use a new private or incognito browser window.

Open Developer Tools.

Enable:

~~~text
Network
Preserve log
~~~

Clear old entries.

Open:

~~~text
http://localhost:8000/protected
~~~

Complete the Okta authentication.

Confirm that the protected page still opens.

This proves that adding claims did not break the established SAML authentication flow.

---

# Part 15: Find the returned SAMLResponse

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

Copy only the parsed SAMLResponse field value.

Do not copy cookies or authentication headers.

---

# Part 16: Decode the Response locally

## macOS or Linux

Run:

~~~bash
python3 -c "import base64,xml.dom.minidom as x; v=''.join(input('Paste the SAMLResponse value: ').strip().split()); raw=base64.b64decode(v); print(x.parseString(raw).toprettyxml(indent='  '))"
~~~

## Windows PowerShell

Run:

~~~powershell
py -c "import base64,xml.dom.minidom as x; v=''.join(input('Paste the SAMLResponse value: ').strip().split()); raw=base64.b64decode(v); print(x.parseString(raw).toprettyxml(indent='  '))"
~~~

If your Windows installation uses python instead of py, replace py with python.

Keep the decoded XML local.

---

# Part 17: Find the AttributeStatement

Search the decoded Assertion for:

~~~xml
<saml2:AttributeStatement>
~~~

You should find attributes for the claim names you configured.

Do not begin with AcmeHR application code.

First prove what Okta actually sent.

---

# Part 18: Prove each single-valued claim

Record the received values.

Expected structure:

~~~xml
<saml2:Attribute Name="email">
    <saml2:AttributeValue>...</saml2:AttributeValue>
</saml2:Attribute>

<saml2:Attribute Name="firstName">
    <saml2:AttributeValue>Priya</saml2:AttributeValue>
</saml2:Attribute>

<saml2:Attribute Name="lastName">
    <saml2:AttributeValue>Shah</saml2:AttributeValue>
</saml2:Attribute>

<saml2:Attribute Name="employeeNumber">
    <saml2:AttributeValue>E10427</saml2:AttributeValue>
</saml2:Attribute>

<saml2:Attribute Name="department">
    <saml2:AttributeValue>Finance</saml2:AttributeValue>
</saml2:Attribute>
~~~

Namespace prefixes and XML formatting can differ.

Compare names and values, not whitespace.

---

# Part 19: Prove that groups is multi-valued

Find:

~~~xml
<saml2:Attribute Name="groups">
~~~

Expected values include:

~~~text
AcmeHR-Employees
AcmeHR-Managers
~~~

The XML can contain more than one AttributeValue inside the same groups attribute.

Record:

~~~text
Attribute count
    one groups attribute

Value count
    two expected group values
~~~

Do not call the two values two separate claims.

---

# Part 20: Complete the IdP-side evidence table

Fill this from the live Assertion.

| Claim | Source value | Expression | Received SAML value | Match? |
| --- | --- | --- | --- | --- |
| email | test-user email | user.profile.email |  | yes/no |
| firstName | Priya | user.profile.firstName |  | yes/no |
| lastName | Shah | user.profile.lastName |  | yes/no |
| employeeNumber | E10427 | user.profile.employeeNumber |  | yes/no |
| department | Finance | user.profile.department |  | yes/no |
| groups | user group membership | user.getGroups(...).![profile.name] |  | yes/no |

Do not continue to application authorization until the SAML claim layer is correct.

---

# Part 21: Open the Day 7 AcmeHR claims view

The Day 7 training SP exposes a claims view at:

~~~text
http://localhost:8000/claims
~~~

It is an authenticated page.

If you open it without an AcmeHR session, the SAML login begins.

After authentication, the page should show the values AcmeHR read from the validated SAML principal.

Expected fields:

~~~text
Authenticated principal
Email
First name
Last name
Employee number
Department
Groups
Mapped AcmeHR roles
~~~

This page is not parsing an untrusted browser POST manually.

It reads attributes from the authenticated SAML result created after Spring Security validation.

---

# Part 22: Compare the Assertion with the AcmeHR claims view

Complete this table.

| Value | SAML Assertion | AcmeHR claims page | Match? |
| --- | --- | --- | --- |
| email |  |  | yes/no |
| firstName |  |  | yes/no |
| lastName |  |  | yes/no |
| employeeNumber |  |  | yes/no |
| department |  |  | yes/no |
| groups |  |  | yes/no |

If the Assertion is correct but AcmeHR displays the wrong value, the next investigation belongs to the application mapping layer.

Do not keep changing Okta.

---

# Part 23: Understand the AcmeHR role allowlist

The training application does not turn every group string into an application authority.

Its Day 7 mapping is intentionally small:

~~~text
AcmeHR-Managers
    ->
MANAGER
~~~

An unrelated group does not become an AcmeHR role.

This is the authorization boundary:

~~~text
Validated SAML group
        |
        v
Explicit AcmeHR allowlist mapping
        |
        v
Application role
~~~

---

# Part 24: Prove the manager role

On:

~~~text
http://localhost:8000/claims
~~~

confirm that the received groups include:

~~~text
AcmeHR-Managers
~~~

Then confirm the mapped role shows:

~~~text
MANAGER
~~~

This proves the application consumed the validated group claim and applied its own authorization mapping.

It does not prove that Okta itself created the AcmeHR role.

---

# Part 25: Open the manager-only page

Open:

~~~text
http://localhost:8000/manager
~~~

Expected result:

~~~text
Access granted
~~~

The complete evidence chain is:

~~~text
Okta group membership
    AcmeHR-Managers
        |
        v
SAML groups claim
    AcmeHR-Managers
        |
        v
AcmeHR mapping
    MANAGER
        |
        v
/manager
    allowed
~~~

Record all four layers.

---

# Part 26: Prove assignment is still a separate decision

Remember that the test user remained directly assigned to the Okta application.

The user can therefore still reach the application even if we later break the groups claim.

That lets us prove:

~~~text
Application assigned
    YES

Manager group claim delivered
    can be YES or NO

Manager authorization
    depends on the validated claim
~~~

The same group does not have to control all three decisions.

---

# Part 27: Controlled failure 1, wrong attribute name

Break only one thing.

In the Okta custom claims configuration, temporarily change:

~~~text
employeeNumber
~~~

to:

~~~text
employeeId
~~~

Keep the expression unchanged:

~~~text
user.profile.employeeNumber
~~~

Do not change Priya's profile value.

Start a completely new SAML login in a fresh private window.

Capture and decode the new Response.

---

# Part 28: Prove why the wrong-name test fails

In the new Assertion, prove:

~~~text
employeeId
    E10427
~~~

Now open:

~~~text
http://localhost:8000/claims
~~~

AcmeHR should report its expected employeeNumber as missing.

The value is correct.

The name is wrong.

Record:

~~~text
Expected claim name
    employeeNumber

Received claim name
    employeeId

Received value
    E10427

SAML authentication
    PASS

Required AcmeHR claim
    MISSING
~~~

Do not change the employee-number source value.

---

# Part 29: Restore employeeNumber

Change the claim name back to:

~~~text
employeeNumber
~~~

Start a new login.

Prove:

~~~text
SAML claim
    employeeNumber = E10427

AcmeHR claim
    employeeNumber = E10427
~~~

Never carry a deliberate failure into the next experiment.

---

# Part 30: Controlled failure 2, missing required source value

Now test a different failure.

Temporarily clear the test user's:

~~~text
department
~~~

profile value.

Do not delete the department claim expression.

The expression remains:

~~~text
user.profile.department
~~~

Start a new SAML transaction.

---

# Part 31: Prove the missing-source behavior

Current Okta documentation states that when a SAML attribute-statement expression evaluates to empty, Okta omits that claim from the SAML Response.

Inspect the live Assertion.

Expected result:

~~~text
department claim
    not present
~~~

Do not assume it will appear as an empty string.

Use the actual Assertion as evidence.

Now open the AcmeHR claims page.

Expected application result:

~~~text
Department
    MISSING
~~~

while:

~~~text
SAML authentication
    still accepted
~~~

This proves that successful authentication and complete application profile data are separate results.

---

# Part 32: Restore department

Set the test user's department back to:

~~~text
Finance
~~~

Start another fresh login.

Prove:

~~~text
SAML
    department = Finance

AcmeHR
    department = Finance
~~~

---

# Part 33: Controlled failure 3, incorrect group filter

Now break only the groups expression.

Temporarily change:

~~~text
user.getGroups({'group.profile.name': 'AcmeHR-', 'operator': 'STARTS_WITH'}).![profile.name]
~~~

to a prefix that the test user does not have:

~~~text
user.getGroups({'group.profile.name': 'NoSuchAcmeGroup-', 'operator': 'STARTS_WITH'}).![profile.name]
~~~

Do not remove the test user from AcmeHR-Managers.

The membership stays correct.

Only claim selection is wrong.

---

# Part 34: Prove the group-filter failure

Start a fresh SAML login.

Decode the new Response.

Because the expression returns no matching groups, the groups claim can be omitted.

Record:

~~~text
Okta membership
    AcmeHR-Managers = YES

groups expression match
    none

SAML groups claim
    absent or no AcmeHR-Managers value

AcmeHR MANAGER role
    NOT GRANTED
~~~

The application assignment remains unchanged.

That is the isolation we wanted.

---

# Part 35: Prove authorization is denied

Open:

~~~text
http://localhost:8000/manager
~~~

Expected result:

~~~text
Access denied
~~~

This result does not mean SAML authentication failed.

Prove the difference:

~~~text
AcmeHR authenticated session
    ACTIVE

Validated manager group
    NOT PRESENT

MANAGER role
    NOT GRANTED

Manager page
    DENIED
~~~

This is an authorization failure after successful authentication.

---

# Part 36: Restore the correct groups expression

Restore:

~~~text
user.getGroups({'group.profile.name': 'AcmeHR-', 'operator': 'STARTS_WITH'}).![profile.name]
~~~

Start one more fresh SAML transaction.

Prove:

~~~text
groups
    contains AcmeHR-Managers

Mapped role
    MANAGER

/manager
    ALLOWED
~~~

This is your restored known-good Day 7 baseline.

---

# Part 37: Why a fresh login is required after claim changes

The application session was created from a particular validated SAML Response.

Changing Okta claim configuration does not rewrite the claims inside an already-created AcmeHR session.

After every claim or profile change:

1. start a fresh private/incognito window
2. trigger a new SP-initiated SAML login
3. capture the new Response
4. compare the new application result

Do not test a new claim configuration against an old session and conclude that Okta ignored the change.

---

# Part 38: Build the complete evidence chain

For the final working transaction, complete this table.

| Layer | Evidence |
| --- | --- |
| Okta profile | department = Finance, employeeNumber = E10427 |
| Okta groups | user is in AcmeHR-Managers |
| Claim expressions | current profile and groups expressions saved |
| SAML Assertion | expected attributes and group values present |
| SAML validation | AcmeHR login accepted |
| AcmeHR claims | received values displayed correctly |
| Role mapping | AcmeHR-Managers -> MANAGER |
| Authorization | /manager allowed |

This is stronger evidence than:

> The group exists in Okta.

---

# Part 39: Troubleshoot from the last proven layer

Use this order.

## Case A: source value is wrong

Example:

~~~text
Okta department
    Marketing

Expected
    Finance
~~~

Fix the source data.

Do not change the SAML attribute name.

---

## Case B: source is correct, claim missing

Check:

~~~text
Claim expression
Claim name
Expression syntax
Live Assertion
~~~

Do not change AcmeHR authorization yet.

---

## Case C: claim is present, AcmeHR says missing

Check:

~~~text
Exact claim name
Case
Application attribute reading
Single-value vs multi-value handling
~~~

Do not change Okta authentication policy.

---

## Case D: groups claim is correct, manager page denied

Check:

~~~text
Exact received group value
AcmeHR allowlist mapping
Mapped MANAGER role
Authorization rule on /manager
~~~

At this point the IdP may already be correct.

---

# Part 40: Do not use these fixes

Do not respond to a Day 7 failure by:

- changing the ACS
- changing the Audience
- replacing the IdP certificate
- disabling signature validation
- changing several claim expressions at once
- sending every Okta group just to make one group appear
- automatically turning every incoming group into an application role
- changing application assignment to hide a bad claim expression

Those changes attack different layers.

Fix the failed comparison.

---

# Part 41: Keep legacy configuration in context

If your org or an older application shows:

~~~text
Attribute Statements
Group Attribute Statements
~~~

under a legacy section, remember:

~~~text
Legacy UI
    can still produce SAML attributes

Current lab
    uses Add expression with EL for OIE
~~~

Do not migrate a production application merely because the training lab uses the current interface.

A migration is a separate change that requires testing.

---

# Part 42: Name and value challenge

You receive:

~~~xml
<saml2:Attribute Name="employeeId">
    <saml2:AttributeValue>E10427</saml2:AttributeValue>
</saml2:Attribute>
~~~

AcmeHR expects:

~~~text
employeeNumber
~~~

Which part is wrong?

<details>
<summary>Check your answer</summary>

The claim name is wrong for the AcmeHR contract.

The value E10427 can be correct while the application still cannot find the required employeeNumber claim.

</details>

---

# Part 43: Membership and claim challenge

Priya is a member of:

~~~text
AcmeHR-Managers
~~~

but the SAML Assertion does not contain that value.

Which statement is proven?

<details>
<summary>Check your answer</summary>

The Okta group membership is proven.

The group-claim generation is not working as expected.

Membership alone does not prove claim inclusion.

</details>

---

# Part 44: Claim and role challenge

The validated Assertion contains:

~~~text
groups = AcmeHR-Managers
~~~

but AcmeHR does not map that value to any internal role.

Should /manager be allowed?

<details>
<summary>Check your answer</summary>

No.

The validated claim is data.

AcmeHR must explicitly map the approved value to its MANAGER role before manager authorization should succeed.

</details>

---

# Part 45: Missing claim challenge

The test user's department is empty.

The configured expression is:

~~~text
user.profile.department
~~~

The new Assertion does not contain a department claim.

Is that automatically proof of an Okta defect?

<details>
<summary>Check your answer</summary>

No.

Current Okta documentation says that a SAML attribute-statement expression that evaluates to empty is omitted from the SAML Response.

Restore or populate the source value, then repeat the transaction.

</details>

---

# Part 46: Authentication or authorization challenge

You prove:

~~~text
Okta authentication
    PASS

SAML validation
    PASS

Application session
    ACTIVE

groups contains AcmeHR-Managers
    NO

/manager
    DENIED
~~~

What failed?

<details>
<summary>Check your answer</summary>

Manager authorization failed because the required validated group value was not present.

Authentication already succeeded.

Do not describe this as a failed Okta sign-in.

</details>

---

# Explain it back

Explain the working Day 7 flow as if an application developer asks:

> How does Priya's department and manager access get from Okta into AcmeHR?

Your answer should connect all of these:

~~~text
Okta profile
    department = Finance

Okta membership
    AcmeHR-Managers

Claim expressions
    user.profile.department
    user.getGroups(...).![profile.name]

SAML Assertion
    department = Finance
    groups contains AcmeHR-Managers

AcmeHR mapping
    AcmeHR-Managers -> MANAGER

Application result
    /manager allowed
~~~

Then explain what would happen if the groups claim disappeared while the user remained assigned to the app.

Do not answer only:

> Okta sends the groups.

Explain the application-side authorization decision too.

---

# Day 7 lab completion check

You are finished with Day 7 only when you can prove all of these:

~~~text
[ ] The Day 6 SAML baseline still works

[ ] The test user has email, firstName, lastName, employeeNumber, and department values

[ ] The test user belongs to AcmeHR-Employees

[ ] The test user belongs to AcmeHR-Managers

[ ] The Okta app still has its original user assignment

[ ] I configured current custom claims under the Sign On tab

[ ] email uses user.profile.email

[ ] firstName uses user.profile.firstName

[ ] lastName uses user.profile.lastName

[ ] employeeNumber uses user.profile.employeeNumber

[ ] department uses user.profile.department

[ ] groups uses user.getGroups(...) with a profile.name projection

[ ] I captured a new SAML Response after the claim changes

[ ] The live AttributeStatement contains the five expected profile claims

[ ] The groups attribute contains the expected AcmeHR group values

[ ] I proved that groups is one multi-valued SAML attribute

[ ] The AcmeHR claims page displays the validated attributes

[ ] AcmeHR maps AcmeHR-Managers to MANAGER

[ ] The manager page is allowed with the correct group claim

[ ] I changed employeeNumber to employeeId and proved the name mismatch

[ ] I restored employeeNumber

[ ] I cleared department and proved the empty source caused the claim to be omitted

[ ] I restored department

[ ] I broke only the group filter and proved the manager role disappeared

[ ] The user remained authenticated while manager authorization was denied

[ ] I restored the correct group filter

[ ] I repeated the login and restored the known-good Day 7 result

[ ] I can distinguish application assignment, claim generation, and application authorization

[ ] I can explain the full source -> claim -> application mapping chain
~~~

Do not finish Day 7 because the Okta claim editor saved successfully.

Finish when the live Assertion and AcmeHR application behavior prove the complete data and authorization path.

---

# Official references

The lab uses the current Okta claims model and the SAML support in the training SP:

- Okta, Configure custom claims for app integrations:  
  https://help.okta.com/oie/en-us/content/topics/apps/federated-claims-overview.htm

- Okta, Expression Language in Identity Engine:  
  https://developer.okta.com/docs/reference/okta-expression-language-in-identity-engine/

- Okta, Expression Language overview and group functions:  
  https://developer.okta.com/docs/reference/okta-expression-language/

- Okta, 2026 Identity Engine release notes:  
  https://developer.okta.com/docs/release-notes/2026-okta-identity-engine/

- Spring Security, Authenticating SAML Responses:  
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html
