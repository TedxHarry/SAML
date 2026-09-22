# Day 7: Claims, Attributes, Groups, and Application Authorization

## What you should understand by the end of today

On Day 6, AcmeHR decided whether it could trust the SAML Response.

Today we ask the next question:

> After AcmeHR trusts the login, what user information did Okta send, and what should AcmeHR do with it?

By the end of Day 7, you should be able to explain:

- the difference between NameID and SAML attributes
- what an AttributeStatement contains
- the difference between an attribute name and its value
- why a claim name must match what the application expects
- how single-valued and multi-valued attributes appear in SAML
- how Okta Expression Language supplies claim values
- how the current Okta unified claims experience differs from older Attribute Statements screens
- how user.getGroups(...) can return selected group information
- why sending a group does not automatically grant an application role
- why Okta assignment, SAML group claims, and application authorization are three separate decisions
- how to troubleshoot missing, empty, incorrect, or over-broad claims
- why authentication, authorization, and provisioning must remain separate concepts

Today is about identity data and application decisions after the SAML message has already passed validation.

We are not changing certificates, signatures, or encryption today.

---

# 1. Start after the Day 6 trust decision

Our transaction is still the same:

~~~text
Priya opens AcmeHR
        |
        v
Okta authenticates Priya
        |
        v
Okta returns SAML Response
        |
        v
AcmeHR validates the Response
        |
        v
AcmeHR trusts the authenticated principal
~~~

Day 7 begins here:

~~~text
Validated principal
        |
        v
Read SAML attributes
        |
        v
Map values to application data
        |
        v
Make application authorization decisions
~~~

Do not skip the validation step.

An application should not use untrusted claim values to grant permissions.

---

# 2. NameID and attributes have different jobs

Day 5 introduced NameID.

In the current training baseline, NameID becomes the visible authenticated principal name.

Example:

~~~xml
<saml2:Subject>
    <saml2:NameID>priya@acme.example</saml2:NameID>
</saml2:Subject>
~~~

That answers:

> Which subject is this Assertion about?

Additional user data normally appears elsewhere in the Assertion:

~~~xml
<saml2:AttributeStatement>
    ...
</saml2:AttributeStatement>
~~~

That section can carry values such as:

- email
- first name
- last name
- employee number
- department
- groups

Plain meaning:

> NameID identifies the subject. Attributes provide additional information about that subject.

Do not treat NameID as a container for every user property.

---

# 3. What is a SAML attribute?

A SAML attribute has a name and one or more values.

Example:

~~~xml
<saml2:Attribute Name="department">
    <saml2:AttributeValue>Finance</saml2:AttributeValue>
</saml2:Attribute>
~~~

Read it as:

~~~text
Attribute name
    department

Attribute value
    Finance
~~~

The application does not receive a vague "user profile."

It receives named values.

That means both sides must agree on the contract.

---

# 4. The application defines the claim contract

Suppose AcmeHR requires:

~~~text
email
firstName
lastName
employeeNumber
department
groups
~~~

Okta must send claim names that AcmeHR understands.

If AcmeHR expects:

~~~text
employeeNumber
~~~

but Okta sends:

~~~text
employeeId
~~~

the user can still authenticate successfully while the application fails to find the expected value.

That is not a signature problem.

That is not an Audience problem.

That is a data-contract problem.

---

# 5. Attribute name and source value are separate

Consider this Okta claim design:

~~~text
SAML attribute name
    department

Okta expression
    user.profile.department
~~~

Those two sides have different jobs.

~~~text
department
    What AcmeHR sees as the claim name

user.profile.department
    Where Okta gets the value
~~~

If Priya's profile contains:

~~~text
department = Finance
~~~

the Assertion can contain:

~~~xml
<saml2:Attribute Name="department">
    <saml2:AttributeValue>Finance</saml2:AttributeValue>
</saml2:Attribute>
~~~

Do not confuse the external SAML claim name with the internal Okta profile variable.

---

# 6. Current Okta custom claims experience

Okta's unified claims-generation experience for custom applications became generally available in Production on July 30, 2025, and is the current claims model used in this course.

For SAML applications, this provides a common claims interface for user profile values, groups, and other supported claim sources.

In a current Identity Engine org, you may see the application's SAML claim configuration under an area such as:

~~~text
Application
    ->
Sign On
    ->
SAML Attributes
    ->
Edit
~~~

Some org configurations can show the relevant controls under an Authentication tab instead.

The important concept is not the exact tab label.

The current model is:

~~~text
Claim name
        +
Expression or supported claim source
        |
        v
SAML AttributeStatement
~~~

This course teaches that current model first.

---

# 7. Why older screenshots can look different

Older Okta documentation and existing applications can still show fields named:

~~~text
Attribute Statements
Group Attribute Statements
~~~

Those controls can still produce valid SAML attributes.

They are not a different SAML protocol.

They are an older configuration experience for creating similar output.

When reading an old implementation guide, translate it into the SAML result:

~~~text
What attribute name is sent?

What value or values are sent?

Where do those values come from?
~~~

Do not memorize one admin-console layout as if it were part of SAML itself.

---

# 8. Okta Expression Language supplies claim values

Okta Expression Language lets the claim configuration read values from supported profile and application contexts.

In the current unified claims interface, Okta uses Expression Language for Identity Engine. User-profile examples include:

~~~text
user.profile.email
user.profile.firstName
user.profile.lastName
user.profile.department
~~~

If your org has a custom employee-number attribute, the exact variable name depends on that profile schema.

For example, an org might use:

~~~text
user.profile.employeeNumber
~~~

Do not copy a variable name merely because another tenant uses it.

Open the user's profile schema and confirm the actual variable name.

---

# 9. The same visible value can come from different sources

Suppose the SAML Assertion contains:

~~~text
department = Finance
~~~

That does not tell you by itself whether Finance came from:

- an Okta user profile attribute
- an application user profile attribute
- an expression
- an imported directory value
- an entitlement or other supported source

When troubleshooting, record both sides:

~~~text
Received SAML value
    Finance

Configured Okta expression
    user.profile.department
~~~

Then inspect the source value.

---

# 10. Single-valued attributes

A normal single-valued claim can look like:

~~~xml
<saml2:Attribute Name="email">
    <saml2:AttributeValue>priya@acme.example</saml2:AttributeValue>
</saml2:Attribute>
~~~

Spring Security can expose the validated AttributeStatement values to the authenticated principal.

For a value that is expected to be single-valued, application code can read the first value.

That convenience does not change the SAML model.

SAML attributes can contain multiple values.

---

# 11. Multi-valued attributes

A groups claim can look like:

~~~xml
<saml2:Attribute Name="groups">
    <saml2:AttributeValue>AcmeHR-Employees</saml2:AttributeValue>
    <saml2:AttributeValue>AcmeHR-Managers</saml2:AttributeValue>
</saml2:Attribute>
~~~

This is one attribute named groups with two values.

Do not describe it as two different attributes merely because two AttributeValue elements exist.

The application should treat the value as a collection.

---

# 12. Do not silently collapse a multi-valued claim

Suppose Priya belongs to:

~~~text
AcmeHR-Employees
AcmeHR-Managers
~~~

If application code reads only the first group value, it can miss information that matters for authorization.

For a known single-valued claim such as department, reading the first value can be reasonable.

For a known multi-valued claim such as groups, read the complete list.

The application contract must define which shape it expects.

---

# 13. Missing and empty are not the same design question

An application can encounter several different situations:

~~~text
Claim not present

Claim present with no usable value

Claim present with an unexpected value

Claim present with several values
~~~

Do not reduce all four cases to:

> The attribute is wrong.

For a required employee number, AcmeHR may reject access if the claim is absent.

For an optional middle name, AcmeHR may continue without it.

The application owner must define which attributes are required and what a missing value means.

---

# 14. Do not assume how Okta will represent a missing source

If Priya's Okta profile has no department value, inspect the generated Assertion.

Do not assume in advance that Okta must produce:

~~~xml
<saml2:Attribute Name="department">
    <saml2:AttributeValue></saml2:AttributeValue>
</saml2:Attribute>
~~~

The actual output depends on the claim configuration and source value.

The correct troubleshooting evidence is the Assertion that was actually generated.

---

# 15. Treat claim names as exact application contract values

Consider:

~~~text
department
Department
DEPARTMENT
~~~

A receiving application can treat those as different names.

SAML does not force the application to normalize them into one property.

If AcmeHR expects:

~~~text
department
~~~

send that exact name unless the application contract says otherwise.

The same principle applies to group and role values.

---

# 16. Attribute NameFormat is not the attribute value

You may see:

~~~xml
<saml2:Attribute
    Name="department"
    NameFormat="urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified">
~~~

Read the pieces separately:

~~~text
Name
    department

NameFormat
    unspecified

Value
    Finance
~~~

Changing NameFormat does not change Finance into another department.

Most application integrations care first about the exact attribute name and values they expect.

Use another NameFormat only when the Service Provider explicitly requires it.

---

# 17. What is a group claim?

A group claim is simply an attribute whose values represent selected group information.

Example:

~~~xml
<saml2:Attribute Name="groups">
    <saml2:AttributeValue>AcmeHR-Employees</saml2:AttributeValue>
    <saml2:AttributeValue>AcmeHR-Managers</saml2:AttributeValue>
</saml2:Attribute>
~~~

The word groups is not magical.

AcmeHR must be configured or coded to recognize that claim name and interpret its values.

Another application might expect:

~~~text
memberOf
roles
groups
securityGroups
~~~

The application contract decides.

---

# 18. Prefer selected groups over every group

A user can belong to many groups that have nothing to do with AcmeHR.

Sending every group can expose unnecessary organization information and make authorization harder to reason about.

For this course, use AcmeHR-specific groups such as:

~~~text
AcmeHR-Employees
AcmeHR-Managers
AcmeHR-Payroll
~~~

Then send only the groups needed by AcmeHR.

The principle is simple:

> Send the application the identity data it actually needs.

---

# 19. Current group expressions with user.getGroups(...)

Okta's current Expression Language supports user.getGroups(...) for retrieving group information about a user.

It can filter by supported properties such as:

~~~text
group.id
group.source.id
group.type
group.profile.name
~~~

A simple training expression that selects groups whose names begin with AcmeHR- can be written using the group-name criterion and a collection projection.

Conceptually:

~~~text
user.getGroups(...)
        |
        v
selected group objects
        |
        v
project profile.name
        |
        v
array of group names
~~~

The Day 7 lab will use the exact expression and verify the generated values.

The important lesson now is that group selection happens before the application receives the SAML attribute.

---

# 20. Why collection projection matters

user.getGroups(...) returns group objects.

AcmeHR normally does not need the entire Okta group object.

It may only need the name.

A collection projection lets the expression transform:

~~~text
group objects
~~~

into:

~~~text
group names
~~~

For example, the current Identity Engine expression model supports projecting profile.name from the returned groups.

That makes the final SAML claim a simple list that the application can consume.

---

# 21. Group source matters when names are ambiguous

Suppose two connected systems both contain a group named:

~~~text
Engineering Users
~~~

The group name alone may not tell you which source you intended.

Okta's user.getGroups(...) criteria can filter by group source ID, group type, or group ID when stronger identification is needed.

Use the simplest filter that is still unambiguous.

For a small training tenant, an AcmeHR-specific prefix is easy to understand.

For a production design with duplicate names across sources, use stronger criteria.

---

# 22. Legacy group claim functions are not the primary pattern

Older configurations can use group-claims-only functions or the traditional Group Attribute Statements filter controls.

Those can still appear in existing integrations.

Current Okta documentation recommends user.getGroups(...) with collection projections instead of the older group-claims-only functions for new Expression Language designs.

So this course will teach:

~~~text
Current pattern first
    user.getGroups(...)

Legacy pattern later
    older group claim functions or filter-only screens
~~~

The SAML application still receives attribute values either way.

---

# 23. Three different group-related decisions

This distinction is critical.

Consider the group:

~~~text
AcmeHR-Managers
~~~

It can participate in three different decisions.

**Decision 1: Okta application assignment**

~~~text
Is Priya allowed to use the AcmeHR Okta application?
~~~

**Decision 2: SAML claim generation**

~~~text
Should Okta send AcmeHR-Managers in the SAML Assertion?
~~~

**Decision 3: AcmeHR authorization**

~~~text
What permission does AcmeHR grant when it receives AcmeHR-Managers?
~~~

These decisions can use the same group name.

They are still different layers.

---

# 24. Assignment does not automatically create a group claim

Suppose Priya receives the AcmeHR application because she belongs to:

~~~text
AcmeHR-Employees
~~~

That proves the group influenced assignment.

It does not prove the group was sent in the SAML Assertion.

The claim configuration must include it.

Always inspect the returned AttributeStatement.

---

# 25. A group claim does not automatically grant a role

Suppose the Assertion contains:

~~~text
groups = AcmeHR-Managers
~~~

That proves Okta sent the value.

It does not prove that AcmeHR granted manager permissions.

AcmeHR still needs an authorization rule such as:

~~~text
If validated groups contains AcmeHR-Managers
    grant application role MANAGER
~~~

Without that application-side mapping, the group is only data.

---

# 26. Do not blindly convert every incoming group into authority

An application should not automatically trust every arbitrary group string as an internal permission name.

A safer design uses explicit mapping.

Example:

~~~text
Incoming SAML value
    AcmeHR-Managers

Mapped AcmeHR role
    MANAGER
~~~

and:

~~~text
Incoming SAML value
    AcmeHR-Payroll

Mapped AcmeHR role
    PAYROLL_VIEW
~~~

An unrelated group such as:

~~~text
Corporate-WiFi-Users
~~~

should not become an AcmeHR authority merely because it appears in the Assertion.

---

# 27. Authentication and authorization are different

Day 6 answered:

> Can AcmeHR trust this authentication statement?

Day 7 adds:

> What may this trusted principal do in AcmeHR?

That is authorization.

~~~mermaid
flowchart TD
    A["Validated SAML login"]
    C["Read trusted claims"]
    M["Map approved claim values"]
    R["Application roles"]
    P["Application permissions"]

    A --> C
    C --> M
    M --> R
    R --> P
~~~

A valid login can produce a user with no privileged role.

That can be exactly the correct result.

---

# 28. Provisioning is another separate layer

SAML attributes are delivered during federation.

They do not by themselves create a long-term provisioning system.

Keep these concepts separate:

~~~text
Authentication
    Who signed in?

Authorization
    What may the authenticated principal do?

Provisioning
    What account and profile data should exist in the target system over time?
~~~

A SAML application can also support JIT account creation at login, and a separate provisioning protocol such as SCIM can maintain accounts outside login.

Those topics are handled more deeply on Day 10.

Do not call a SAML attribute statement "provisioning."

---

# 29. Example AcmeHR claim contract

For the next lab, imagine the AcmeHR application owner provides this requirement:

| Claim | Shape | Required? | AcmeHR use |
| --- | --- | --- | --- |
| email | one value | yes | display and contact |
| firstName | one value | yes | display |
| lastName | one value | yes | display |
| employeeNumber | one value | yes | employee record link |
| department | one value | yes | business context |
| groups | multiple values | no | application role mapping |

That is much better than:

> Send whatever Okta has.

A good federation design starts with the application's data contract.

---

# 30. Example Assertion after adding claims

A shortened Assertion might contain:

~~~xml
<saml2:AttributeStatement>

    <saml2:Attribute Name="email">
        <saml2:AttributeValue>priya@acme.example</saml2:AttributeValue>
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

    <saml2:Attribute Name="groups">
        <saml2:AttributeValue>AcmeHR-Employees</saml2:AttributeValue>
        <saml2:AttributeValue>AcmeHR-Managers</saml2:AttributeValue>
    </saml2:Attribute>

</saml2:AttributeStatement>
~~~

Do not memorize the XML order.

Read each attribute as:

~~~text
Name
Values
Application meaning
~~~

---

# 31. The application can read validated attributes

Spring Security's SAML authentication support exposes attributes extracted from validated AttributeStatements.

For a single-valued attribute, application code can retrieve one value.

For a multi-valued attribute, it can retrieve the complete collection.

That gives the training SP a safe progression:

~~~text
Validate Assertion
        |
        v
Read validated attributes
        |
        v
Display safe diagnostics
        |
        v
Map selected group values to AcmeHR roles
~~~

The Day 7 lab will extend the training application in that direction.

---

# 32. Current claim preview is useful but not final proof

Okta can preview generated SAML assertion content during configuration.

That helps answer:

~~~text
Did this expression produce the value I expected?
~~~

It does not prove:

~~~text
Did the browser receive the claim?

Did AcmeHR validate the Response?

Did AcmeHR read the expected claim name?

Did AcmeHR grant the intended role?
~~~

Use preview for configuration feedback.

Use the real transaction for end-to-end evidence.

---

# 33. Troubleshoot attributes from the last proven step

Suppose login succeeds but AcmeHR says:

~~~text
Employee number missing
~~~

Do not change the certificate or ACS.

Ask:

> What is the last step I can prove succeeded?

Then check:

~~~text
SAML validation
    PASS

AttributeStatement present
    ?

employeeNumber claim present
    ?

employeeNumber value correct
    ?

AcmeHR expects the same claim name
    ?

AcmeHR mapping reads that claim
    ?
~~~

The failure is after authentication.

Troubleshoot the data layer.

---

# 34. Troubleshoot a wrong attribute name

Suppose the Assertion contains:

~~~xml
<saml2:Attribute Name="employeeId">
    <saml2:AttributeValue>E10427</saml2:AttributeValue>
</saml2:Attribute>
~~~

AcmeHR expects:

~~~text
employeeNumber
~~~

The value can be perfectly correct while the contract still fails.

Evidence:

~~~text
Received name
    employeeId

Expected name
    employeeNumber

Received value
    E10427
~~~

Fix the claim name or the documented application mapping.

Do not change the user's employee number.

---

# 35. Troubleshoot a missing value

Suppose the configured claim is:

~~~text
department
    <- user.profile.department
~~~

but the Assertion has no usable department.

Check in this order:

~~~text
Does Priya's source profile contain department?

Does the claim expression reference the correct variable?

Does the Okta preview show the value?

Does the live Assertion contain the attribute?

Does AcmeHR read the same attribute name?
~~~

Do not start by editing the Service Provider's SAML validation settings.

---

# 36. Troubleshoot a missing group

Suppose Priya belongs to:

~~~text
AcmeHR-Managers
~~~

but the groups claim contains only:

~~~text
AcmeHR-Employees
~~~

Check:

~~~text
Actual Okta group membership

user.getGroups(...) selection criteria

group source and type, if relevant

collection projection

live SAML groups values
~~~

Do not assume that membership and claim inclusion are the same operation.

---

# 37. Troubleshoot correct claims but denied access

Suppose the validated Assertion contains:

~~~text
groups
    AcmeHR-Managers
~~~

but Priya still sees:

~~~text
Access denied
~~~

At that point, the claim-delivery layer may be correct.

Investigate AcmeHR authorization:

~~~text
Does the application map AcmeHR-Managers to MANAGER?

Is that role permitted on this page?

Is the application reading all group values?

Is the expected value case-sensitive?
~~~

Do not keep changing Okta when the application already received the correct claim.

---

# 38. Troubleshoot too many groups

Suppose AcmeHR receives:

~~~text
AcmeHR-Employees
AcmeHR-Managers
VPN-Users
All-Contractors
Building-3
Finance-SharedDrive
...
~~~

The login may still work.

The design is still poor if AcmeHR does not need those groups.

Narrow the claim to the groups required for AcmeHR.

Benefits include:

- less unnecessary identity data
- simpler troubleshooting
- clearer authorization mapping
- lower risk of accidentally interpreting unrelated group names

---

# 39. Group claims and entitlements are not identical

Current Okta unified claims can also support entitlement-related claims when the required Identity Governance features are enabled.

An entitlement represents an application-specific permission assignment.

A group represents membership in a group object.

Those concepts can be related, but do not call every group an entitlement.

This core course uses groups for the Day 7 authorization example because they are easy to see and troubleshoot.

Entitlement-specific design should use the entitlement model when the application and Okta governance configuration require it.

---

# 40. Evidence for a successful Day 7 transaction

A complete claim investigation should connect all four layers.

~~~text
Okta source data
        |
        v
Claim expression
        |
        v
SAML AttributeStatement
        |
        v
AcmeHR application mapping
~~~

For one attribute, record:

~~~text
Okta profile value
    Finance

Expression
    user.profile.department

SAML claim
    department = Finance

AcmeHR value
    Finance
~~~

For one group-based role, record:

~~~text
Okta membership
    AcmeHR-Managers

SAML groups claim
    AcmeHR-Managers

AcmeHR mapped role
    MANAGER

Application permission
    manager page allowed
~~~

That chain proves where the decision came from.

---

# 41. Practice: NameID or attribute?

You capture:

~~~xml
<saml2:NameID>priya@acme.example</saml2:NameID>

<saml2:Attribute Name="department">
    <saml2:AttributeValue>Finance</saml2:AttributeValue>
</saml2:Attribute>
~~~

Which value identifies the subject in the current baseline?

<details>
<summary>Check your answer</summary>

NameID identifies the subject.

department is an additional attribute about that subject.

</details>

---

# 42. Practice: assignment or claim?

Priya receives the AcmeHR Okta application because she belongs to AcmeHR-Employees.

Can you conclude that the SAML Assertion contains:

~~~text
groups = AcmeHR-Employees
~~~

<details>
<summary>Check your answer</summary>

No.

Group-based application assignment and group-claim generation are separate configurations.

Inspect the AttributeStatement to prove what Okta actually sent.

</details>

---

# 43. Practice: claim or authorization?

The validated Assertion contains:

~~~text
groups = AcmeHR-Managers
~~~

Does that fact alone prove Priya can open the AcmeHR manager page?

<details>
<summary>Check your answer</summary>

No.

It proves that the validated SAML data contains that group value.

AcmeHR must map the value to an application role and authorize that role for the manager page.

</details>

---

# 44. Practice: single or multi-valued?

The Assertion contains:

~~~xml
<saml2:Attribute Name="groups">
    <saml2:AttributeValue>AcmeHR-Employees</saml2:AttributeValue>
    <saml2:AttributeValue>AcmeHR-Payroll</saml2:AttributeValue>
</saml2:Attribute>
~~~

How many SAML attributes are shown, and how many values?

<details>
<summary>Check your answer</summary>

One attribute is shown.

Its name is groups.

It contains two values.

</details>

---

# 45. Practice: where is the failure?

You prove:

~~~text
Okta login
    PASS

SAML validation
    PASS

department in Assertion
    Finance

AcmeHR expects department
    Finance

AcmeHR page still shows department missing
~~~

Where should you investigate next?

<details>
<summary>Check your answer</summary>

Investigate the AcmeHR attribute-reading or application mapping code.

The IdP already sent the expected validated claim.

Changing the Okta authentication policy, certificate, Audience, or ACS would not address this evidence.

</details>

---

# 46. Day 7 checkpoint

Before moving to the lab, you should be able to answer:

1. What is the difference between NameID and an AttributeStatement?
2. What are the two basic parts of a SAML attribute?
3. Why can the correct value under the wrong attribute name still fail?
4. What does an Okta claim expression do?
5. Why should you verify the actual profile variable name before using it?
6. What is the difference between a single-valued and multi-valued claim?
7. Why should group values be read as a collection?
8. Why should you not assume how a missing source value appears in SAML?
9. Why can case matter for a claim name?
10. What is a group claim?
11. Why should you normally send only application-relevant groups?
12. What does user.getGroups(...) do at a high level?
13. Why can group source or ID matter when names are duplicated?
14. Why is the current user.getGroups(...) model preferred over legacy group-claim-only functions for new designs?
15. Why is Okta application assignment separate from group-claim generation?
16. Why is a received group separate from application authorization?
17. Why should the SP use an explicit allowlist mapping from claim values to roles?
18. Why is SAML claim delivery not the same as provisioning?
19. What does Okta claim preview prove?
20. What additional evidence proves the application consumed the claim correctly?
21. What should you inspect when the claim is correct but access is still denied?

If one answer is unclear, return to the four-layer evidence chain:

~~~text
Source
    ->
Expression
    ->
SAML claim
    ->
Application mapping
~~~

---

# Explain it back

Imagine an application owner asks:

> We already have SAML working. How do we send department and manager access to AcmeHR?

A clear explanation could sound like:

> First we define the data contract that AcmeHR expects. Okta can send department as a SAML attribute from the user profile and can send selected AcmeHR group names as a multi-valued groups attribute. The validated Assertion only delivers those values. AcmeHR still has to read them and explicitly map approved group values, such as AcmeHR-Managers, to application roles. Okta application assignment, SAML group claims, and AcmeHR authorization remain separate decisions.

Do not memorize those exact words.

Use the claim source, SAML AttributeStatement, and application mapping to explain the complete path.

---

# Day 7 completion standard

The Day 7 lesson is complete when you can:

- distinguish NameID from additional attributes
- read Attribute name and AttributeValue separately
- define an application claim contract before configuring Okta
- explain how Okta expressions source user-profile values
- recognize the current unified claims experience
- translate older Attribute Statements screens into the same SAML output model
- distinguish single-valued and multi-valued claims
- explain missing versus unexpected values
- treat claim names and role values as exact application contract values
- explain user.getGroups(...) at a practical level
- explain why collection projection is useful for group names
- choose application-relevant group filters instead of sending every group
- distinguish assignment, claim generation, and authorization
- explain why a group claim does not grant an application role by itself
- map approved incoming group values to explicit AcmeHR roles
- keep authentication, authorization, and provisioning separate
- troubleshoot source, expression, SAML output, and application mapping in that order
- collect evidence that connects Okta source data to the final AcmeHR authorization result

The Day 7 lab will configure the claims, inspect the live Assertion, extend AcmeHR to display validated attributes, and prove group-to-role authorization with controlled failures.

---

# Official references used for this lesson

- Okta Identity Engine 2026 release notes, unified claims generation:
  https://developer.okta.com/docs/release-notes/2026-okta-identity-engine/

- Okta Expression Language:
  https://developer.okta.com/docs/reference/okta-expression-language/

- Okta Expression Language in Identity Engine:
  https://developer.okta.com/docs/reference/okta-expression-language-in-identity-engine/

- Okta federated claims with entitlements:
  https://developer.okta.com/docs/guides/federated-claims/main/

- Okta create a SAML app integration:
  https://developer.okta.com/docs/guides/create-an-app-integration/saml2/main/

- Spring Security, authenticating SAML Responses and reading attributes:
  https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication.html
