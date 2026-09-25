# Day 10: Okta Access, Authentication Policy, MFA, JIT, and the SCIM Boundary

## What you should understand by the end of today

Days 3 through 9 concentrated on the SAML transaction itself.

You learned how to prove:

~~~text
AuthnRequest
    leaves AcmeHR

Okta
    authenticates the user

SAMLResponse
    returns to AcmeHR

AcmeHR
    validates the SAML message

AcmeHR
    creates an application session
~~~

Today we add an important troubleshooting skill:

> Not every failure around a SAML application is a SAML protocol failure.

A user can fail before Okta creates a SAMLResponse.

A user can satisfy SAML authentication and still fail because the target application has no local account.

A user can be assigned to the Okta app while the target application account does not yet exist.

A user can see an MFA prompt because of Okta policy without any SAML claim being wrong.

By the end of Day 10, you should be able to separate:

- Okta application assignment
- Okta authentication policy
- Global Session Policy
- MFA and step-up authentication
- SAML authentication
- target-application account matching
- target-application first-login account creation
- SCIM provisioning
- application authorization

The troubleshooting question for today is:

> Which layer failed first?

---

# 1. Start with the complete access path

A normal AcmeHR transaction can contain several different decisions.

~~~text
user wants AcmeHR
        |
        v
Is the user allowed to use the Okta app?
        |
        v
What authentication does Okta require?
        |
        v
Did Okta authenticate the user?
        |
        v
Did Okta issue the SAMLResponse?
        |
        v
Did AcmeHR validate the SAMLResponse?
        |
        v
Does AcmeHR have or create a local account?
        |
        v
What is the user allowed to do inside AcmeHR?
~~~

Every arrow is a different troubleshooting boundary.

Do not call all of them:

~~~text
SAML issue
~~~

That phrase is too broad to be useful.

---

# 2. The four layers we will separate today

For this course, use four simple layers.

| Layer | Main question | Example failure |
| --- | --- | --- |
| Access eligibility | Is the user assigned to the Okta app? | User is not assigned |
| Okta authentication | What must the user prove before Okta allows access? | MFA required |
| SAML federation | Can Okta and AcmeHR exchange and validate SAML correctly? | Wrong Audience |
| Application lifecycle | Does the AcmeHR account exist and have the right state? | JIT account creation fails |

SCIM belongs mainly to the lifecycle layer.

SAML belongs mainly to the federation layer.

Do not use one protocol name for both jobs.

---

# 3. Application assignment happens before SAML success

An Okta app integration can be assigned to:

- an individual user
- a group

Assignment answers:

> Is this Okta user allowed to use this app integration?

If Priya is not assigned, Okta can stop the transaction before AcmeHR receives a valid SAMLResponse.

That means:

~~~text
no successful SAMLResponse
        |
        v
AcmeHR has nothing to validate yet
~~~

Do not troubleshoot Audience, Recipient, signing certificates, or Assertion encryption first when the user is not assigned.

---

# 4. Individual assignment

A direct assignment looks conceptually like this:

~~~text
Priya
    |
    v
AcmeHR Training app
~~~

This is simple for a small test.

It is not usually the best operating model for a large population because every user must be managed individually.

For the course, direct assignment is useful because it makes the access decision easy to see.

---

# 5. Group assignment

A group assignment looks like this:

~~~text
Priya
    |
    v
AcmeHR-Employees group
    |
    v
AcmeHR Training app
~~~

The app is assigned to the group.

Priya receives the app because she is a member of that group.

This is different from the Day 7 group claim.

---

# 6. App assignment group is not automatically a SAML group claim

This distinction is important.

A group can be used to assign the app:

~~~text
group membership
    ->
user receives AcmeHR app
~~~

A group can also be sent as SAML data:

~~~text
group membership
    ->
groups AttributeStatement
    ->
AcmeHR authorization logic
~~~

Those are separate configurations.

A group that grants access to the Okta app is not automatically guaranteed to appear in the SAML assertion.

Day 7 configured the claim behavior separately.

---

# 7. Assignment is also not the same as authorization inside AcmeHR

Suppose Priya is assigned to the AcmeHR Okta app.

That proves:

~~~text
Okta allows Priya to use this app integration
~~~

It does not automatically prove:

~~~text
Priya is an AcmeHR manager
~~~

In our training application:

~~~text
Okta app assignment
    controls access to the federation path

AcmeHR-Managers claim
    controls the MANAGER application role
~~~

Authentication and authorization remain separate.

---

# 8. A useful unassigned-user troubleshooting question

When Priya cannot access AcmeHR, ask:

> Did Okta ever issue a SAMLResponse for this attempt?

If the answer is no, first investigate the Okta-side decision that stopped the flow.

Assignment is one of the first things to check.

Do not start by decoding a SAMLResponse that does not exist.

---

# 9. What evidence proves assignment?

Useful evidence includes:

- Priya appears on the app Assignments page
- the group that grants AcmeHR access is assigned to the app
- Priya is actually a member of that group
- the System Log shows the app-access or assignment-related event
- a fresh access attempt proceeds past the assignment check

One screenshot of a group name is not enough if Priya is not actually in the group.

---

# 10. Assignment can exist without target-account provisioning

This is another important boundary.

A user can be assigned to an Okta app even when no automated provisioning connection exists.

Conceptually:

~~~text
Okta assignment
    YES

SAML SSO configuration
    YES

SCIM provisioning
    NO
~~~

That can still be a valid SAML integration.

Whether the target application account already exists is a separate question.

---

# 11. Assignment can also trigger provisioning when provisioning is configured

If an app integration has provisioning configured, assignment may participate in the provisioning lifecycle.

For example:

~~~text
assign Priya to app
        |
        v
Okta provisioning connector
        |
        v
create Priya account in target app
~~~

But do not assume this happens merely because SAML SSO is configured.

Provisioning must be configured and supported separately.

---

# 12. A timing detail that matters during troubleshooting

Okta documentation notes an important case:

If users were already assigned to an app integration before provisioning was enabled, those existing assignments are not automatically provisioned merely because provisioning was later turned on.

That can create confusing evidence:

~~~text
Okta assignment
    YES

SCIM now enabled
    YES

target account
    STILL MISSING
~~~

The correct question is not:

> Why did SAML fail to create the user?

The better question is:

> Was this user actually sent through the provisioning flow?

---

# 13. After assignment comes authentication policy

Once the user is eligible to use the app, Okta evaluates authentication requirements.

On Okta Identity Engine, two policy areas matter here:

~~~text
Global Session Policy

App sign-in policy
~~~

They are related but they do different jobs.

---

# 14. What the Global Session Policy does

The Global Session Policy helps establish the Okta session context.

It can control requirements such as:

- whether access is allowed
- which factor conditions can establish the session
- when another challenge is required
- how the Okta session is established

Think of it as an Okta-session decision.

It is not an AcmeHR SAML Audience rule.

---

# 15. What the app sign-in policy does

The app sign-in policy evaluates authentication in the context of the requested application.

It can evaluate conditions such as:

- user or group
- network or location conditions where configured
- risk
- device conditions where configured
- required factor types
- authentication frequency

Conceptually:

~~~text
Priya already has an Okta session
        |
        v
Priya opens AcmeHR
        |
        v
AcmeHR app sign-in policy is evaluated
        |
        v
additional authentication may be required
~~~

---

# 16. Identity Engine can require both policy layers to be satisfied

This is why a simple statement such as:

> The user already logged into Okta, so there should be no MFA.

can be wrong.

An existing Okta session does not automatically satisfy every application policy.

Identity Engine evaluates the required assurance for the transaction.

A stricter AcmeHR app policy can require another authentication step.

---

# 17. MFA is not a SAML claim error

Suppose Priya opens AcmeHR and Okta asks for another factor.

That does not prove:

~~~text
NameID is wrong

groups claim is wrong

Audience is wrong

certificate is wrong
~~~

The MFA prompt occurs before the final SAMLResponse is issued.

The first layer to inspect is:

~~~text
Okta authentication policy
~~~

not the Assertion XML.

---

# 18. Step-up authentication

A common pattern is:

~~~text
user already has Okta session
        |
        v
user requests a more sensitive app
        |
        v
app policy requires higher assurance
        |
        v
Okta asks for another factor
        |
        v
policy satisfied
        |
        v
SAMLResponse issued
~~~

The extra prompt is expected behavior when policy requires it.

Do not remove MFA simply to make the browser flow shorter.

---

# 19. Authentication frequency can also cause another prompt

An app sign-in policy can control how often the user must authenticate.

Examples include behavior such as:

- every time the user accesses the resource
- after a configured interval
- when no Okta global session exists

So two users with apparently similar SAML configuration can see different authentication experiences because their policy conditions or session state differ.

Again, that is not automatically a SAML message problem.

---

# 20. Policy rule order matters

App sign-in policies contain rules.

Okta evaluates the rules in order.

The first applicable rule can determine the authentication requirement.

When troubleshooting unexpected MFA, do not inspect only the policy name.

Inspect:

~~~text
which policy is assigned to AcmeHR

which rule matched

what that rule requires
~~~

That gives you evidence.

---

# 21. A policy change should not require a SAML claim change

Suppose the problem is:

~~~text
Priya gets MFA every time she opens AcmeHR
~~~

Changing this:

~~~text
department claim
~~~

does not address the policy decision.

Changing this:

~~~text
NameID
~~~

does not address the policy decision.

The correct layer is the policy rule and its authentication frequency or assurance requirement.

---

# 22. Authentication context is evidence, not the policy itself

SAML can carry authentication-context information describing the authentication context reported for the user.

For example, the Assertion can contain:

~~~text
AuthnContextClassRef
~~~

This is evidence in the SAML transaction.

It is not the same object as the Okta app sign-in policy.

Keep the relationship clear:

~~~text
Okta policy
    decides what authentication is required

Okta authentication
    satisfies the policy

SAML Assertion
    reports authentication context to the SP
~~~

Do not edit AuthnContextClassRef to solve a policy that is asking the user for MFA.

---

# 23. The browser can fail before any SAMLResponse exists

This gives us a powerful boundary.

~~~text
failure before Okta issues SAMLResponse
    ->
look first at assignment, session, authentication policy, or Okta-side access

failure after SAMLResponse reaches AcmeHR
    ->
look at SAML validation or AcmeHR processing
~~~

This is not a perfect rule for every product scenario.

It is a very useful first troubleshooting split.

---

# 24. Now separate authentication from account existence

Assume Priya:

- is assigned to AcmeHR
- satisfies Okta authentication policy
- receives a valid SAMLResponse
- passes AcmeHR SAML validation

There is still another question:

> Does AcmeHR have a local user account for Priya?

SAML authentication alone does not require every SP to maintain a local account.

But many real applications do.

---

# 25. Existing local-account model

One application design is:

~~~text
administrator or provisioning system
    creates Priya in AcmeHR first

later

Priya signs in through SAML
        |
        v
AcmeHR matches SAML identity
        |
        v
existing local account opens
~~~

In this model, SAML does not create the account.

The account must already exist.

---

# 26. First-login account creation

Another application design is:

~~~text
Priya signs in successfully through SAML
        |
        v
AcmeHR validates SAML
        |
        v
no existing local account found
        |
        v
AcmeHR creates local account
        |
        v
session continues
~~~

This behavior is often described as:

~~~text
Just-In-Time account creation
~~~

For this course, that is the JIT behavior we care about today.

It is an AcmeHR Service Provider behavior.

---

# 27. Important: do not confuse this with Okta inbound JIT

Okta also uses the term Just-In-Time provisioning for a different direction.

Current Okta documentation describes JIT creating users **in Okta** when Okta is receiving authentication from sources such as:

- inbound SAML Identity Providers
- AD delegated authentication
- Desktop SSO

That flow looks like:

~~~text
external IdP
    authenticates user
        |
        v
Okta receives identity
        |
        v
Okta may create Okta user JIT
~~~

That is not our current federation direction.

Our AcmeHR flow is:

~~~text
Okta
    acts as IdP
        |
        v
AcmeHR
    acts as SP
~~~

So our downstream AcmeHR first-login account creation must not be presented as generic Okta JIT.

---

# 28. The SP decides whether it supports downstream SAML JIT

Okta can send identity information in the SAML Assertion.

The target application decides what it does with that information.

A target SP may:

- require a pre-existing account
- match an existing account
- create a new account at first login
- reject the login if required account data is missing

That behavior is application-specific.

SAML itself does not require one universal account-creation policy.

---

# 29. Account matching comes before account creation

A safe first-login design should not blindly create a new account for every successful SAMLResponse.

Conceptually:

~~~text
validated SAML identity
        |
        v
look for matching AcmeHR account
        |
        +---- found ----> use existing account
        |
        +---- not found -> evaluate JIT creation
~~~

The matching identifier must be defined by the application contract.

Do not guess from whatever attribute looks convenient.

---

# 30. NameID can participate in matching, but it is not automatically the right key

Earlier lessons established:

~~~text
NameID
    is not automatically an email address
~~~

The target app may match on:

- NameID
- email
- employeeNumber
- another stable identifier

The correct choice belongs to the SP account model.

Changing the NameID format does not automatically fix a bad account-matching design.

---

# 31. A successful SAML validation can be followed by failed JIT

This is one of today's most important incidents.

~~~text
Okta authentication
    PASS

SAMLResponse received
    PASS

signature and semantic validation
    PASS

AcmeHR account match
    NO MATCH

AcmeHR JIT create
    FAIL
~~~

If that happens, do not call it:

~~~text
SAML authentication failed
~~~

Authentication already succeeded.

The failure is in the application account lifecycle step after federation.

---

# 32. What can make first-login account creation fail?

Examples can include:

- required application profile attribute missing
- duplicate local account conflict
- invalid username format for the application
- application-side uniqueness rule
- local database or service failure
- unsupported account state

Those are AcmeHR-side examples for our training scenario.

They are not claims about universal Okta JIT behavior.

The lesson is the layer boundary.

---

# 33. JIT creates at login time

A typical downstream first-login account-creation model depends on a login transaction.

~~~text
no login
    ->
no first-login creation event
~~~

That makes JIT convenient when:

- account creation can wait until first use
- only a small set of profile data is required
- the application supports reliable matching

But it has limitations.

---

# 34. JIT is weak for pre-provisioning requirements

Suppose the business says:

> Priya must have her AcmeHR account and manager permissions ready at 8:00 AM on her start date before she signs in.

A login-triggered JIT model cannot guarantee account creation before Priya's first login.

That requirement points toward lifecycle provisioning.

---

# 35. JIT is also weak for lifecycle changes that happen without login

Consider:

~~~text
department changes

name changes

manager changes

user leaves company
~~~

If account updates depend only on login, the target account may remain stale until another authentication event happens.

A lifecycle integration can handle these changes separately from SSO.

---

# 36. This is where SCIM enters

SCIM is a provisioning protocol.

It is used to exchange identity lifecycle data between systems.

In an Okta-to-application design, SCIM can support actions such as:

- create users
- update user attributes
- deactivate users
- manage supported group or membership data

The exact supported actions depend on the SCIM integration.

---

# 37. SAML and SCIM solve different problems

Keep this table.

| Question | SAML | SCIM |
| --- | --- | --- |
| Can Priya authenticate to AcmeHR? | Yes | No |
| Can Okta send an authentication assertion? | Yes | No |
| Can AcmeHR validate federation trust? | Yes | No |
| Can an account be created before first login? | Not by SAML alone | Yes, when provisioning is configured |
| Can profile updates be pushed without login? | Not as a lifecycle guarantee | Yes |
| Can deactivation be pushed without waiting for login? | Not as a lifecycle guarantee | Yes |
| Does it replace application authorization? | No | No |

Do not say:

> We use SAML, so provisioning is already handled.

That is incorrect.

---

# 38. A SAML app can also have SCIM provisioning

These protocols can exist together.

~~~text
SAML
    handles SSO

SCIM
    handles account lifecycle
~~~

Current Okta integration tooling supports adding provisioning capabilities to appropriate app integrations, including custom integrations where supported.

That means this is a normal design:

~~~text
Okta assignment
        |
        +----> SCIM creates/updates AcmeHR account
        |
        +----> SAML authenticates Priya when she signs in
~~~

---

# 39. SCIM does not authenticate the browser

A successful SCIM create operation does not log Priya into AcmeHR.

SCIM can create the account.

SAML can authenticate the user.

These are two different transactions.

Troubleshoot them separately.

---

# 40. SAML does not guarantee deprovisioning

Suppose Priya leaves the company and never signs in again.

If your only integration is login-time SAML, there may be no future login event at which AcmeHR learns that the account should be disabled.

A provisioning integration can push that lifecycle change without waiting for another SAML login.

This is one reason SCIM matters.

---

# 41. A practical JIT versus SCIM decision

Use this question:

> Must the target account exist or change even when the user is not logging in?

If yes, lifecycle provisioning is probably required.

Examples:

~~~text
create before first day login

update profile overnight

disable immediately on termination
~~~

Those are stronger SCIM use cases than login-triggered JIT.

---

# 42. Do not turn every lifecycle problem into a claim problem

Suppose AcmeHR must disable Priya immediately when employment ends.

Adding another SAML attribute such as:

~~~text
employmentStatus=terminated
~~~

does not guarantee immediate deprovisioning if Priya never logs in again.

The claim is carried during a SAML transaction.

No transaction means no new assertion.

The business requirement is lifecycle-driven.

---

# 43. Four similar symptoms with four different causes

Consider:

~~~text
Symptom:
Priya cannot use AcmeHR.
~~~

Possible causes:

### Case A

~~~text
Priya not assigned to app
~~~

Layer:

~~~text
Okta access eligibility
~~~

### Case B

~~~text
Priya receives unexpected MFA prompt
~~~

Layer:

~~~text
Okta authentication policy
~~~

### Case C

~~~text
SAMLResponse reaches AcmeHR but Audience is wrong
~~~

Layer:

~~~text
SAML federation validation
~~~

### Case D

~~~text
SAML validation passes but local account creation fails
~~~

Layer:

~~~text
AcmeHR lifecycle / JIT
~~~

The symptom alone does not tell you which layer failed.

Evidence does.

---

# 44. Use the last-success / first-failure method

For every Day 10 incident, write:

~~~text
Last confirmed successful step:
____________________

First failed step:
____________________
~~~

Example:

~~~text
Last confirmed successful step:
Okta authenticated Priya.

First failed step:
Okta denied access because Priya was not assigned to AcmeHR.
~~~

Then stop investigating later layers.

There is no reason to inspect an Assertion that was never issued.

---

# 45. Incident 1: unassigned user

Observed:

~~~text
Priya opens AcmeHR.

Okta stops the flow before AcmeHR receives a SAMLResponse.
~~~

Check in this order:

1. Is Priya directly assigned?
2. Is AcmeHR assigned to a group?
3. Is Priya actually a member of that group?
4. Does the access attempt proceed after the correct assignment exists?
5. What does the System Log show?

Do not change:

- ACS URL
- Audience
- signing certificate
- encryption certificate
- claims

until evidence points there.

---

# 46. Incident 2: unexpected MFA

Observed:

~~~text
Priya can reach AcmeHR.

Okta asks for another authentication factor.
~~~

Check:

1. Which app sign-in policy is applied?
2. Which rule matched?
3. What factor requirement does that rule specify?
4. What authentication frequency is configured?
5. Did a Global Session Policy also participate?
6. What Okta session already existed?

Do not change SAML attributes just because the browser showed an MFA prompt.

---

# 47. Incident 3: successful SAML followed by failed first-login creation

Observed:

~~~text
Okta authentication
    PASS

SAMLResponse
    RECEIVED

AcmeHR validation
    PASS

local AcmeHR account
    NOT CREATED
~~~

Check:

1. What identifier did AcmeHR use for account matching?
2. Was an existing account found?
3. Which fields are required for local account creation?
4. Are those validated values available?
5. Did a duplicate or application-side validation rule block creation?
6. Did the application record a concrete lifecycle error?

Do not report this as a signature or Audience failure when those stages already passed.

---

# 48. Incident 4: account must exist before login

Requirement:

~~~text
AcmeHR account must exist before Priya's first sign-in.
~~~

Ask:

> Can a login-triggered JIT design satisfy this?

No.

The creation event would occur only when Priya signs in.

This is a lifecycle provisioning requirement.

A SCIM-capable integration is the appropriate class of solution when the target app supports it and the business needs pre-provisioning.

---

# 49. Incident 5: terminated account must be disabled without another login

Requirement:

~~~text
Priya leaves at 5:00 PM.

AcmeHR must be disabled even if Priya never signs in again.
~~~

This is another lifecycle requirement.

A SAML assertion cannot carry a new state if no new SAML transaction happens.

Provisioning or another lifecycle mechanism is needed.

---

# 50. Do not confuse assignment removal with target deactivation

Removing Priya's Okta app assignment can stop future Okta access to that app.

That does not automatically prove:

~~~text
AcmeHR local account
    DEACTIVATED
~~~

Whether target deactivation happens depends on provisioning configuration and the target integration.

Always check the target account state separately.

---

# 51. The same user can be correct in one layer and wrong in another

Example:

~~~text
Okta user
    ACTIVE

AcmeHR assignment
    PRESENT

Okta authentication
    PASS

SAML
    VALID

AcmeHR local account
    DISABLED
~~~

That is possible.

Do not let successful federation hide an application-account problem.

---

# 52. A layer table for support tickets

Use this when someone says:

> SSO is broken.

| Evidence question | Result |
| --- | --- |
| User assigned to Okta app? |  |
| App-access policy allowed request? |  |
| MFA / authentication policy satisfied? |  |
| SAMLResponse issued? |  |
| SAMLResponse reached AcmeHR? |  |
| SAML validation passed? |  |
| Existing AcmeHR account matched? |  |
| JIT account creation attempted? |  |
| JIT account creation succeeded? |  |
| SCIM provisioning expected? |  |
| SCIM target account state correct? |  |
| Application authorization allowed requested function? |  |

Do not skip directly to the last row.

---

# 53. What the Okta System Log can help prove

The System Log is useful for Okta-side evidence.

It can help you investigate areas such as:

- user authentication
- MFA
- application access
- policy evaluation
- assignment-related activity
- provisioning events where applicable

It does not replace AcmeHR application logs.

For a failure after AcmeHR receives and validates SAML, you also need SP-side evidence.

---

# 54. What AcmeHR evidence can prove

AcmeHR should be able to separate:

~~~text
SAML authentication accepted

existing local account matched

new account creation attempted

new account creation failed

application authorization denied
~~~

One generic:

~~~text
Login failed
~~~

message is not enough for the Day 10 lab.

The implementation will add only the evidence needed for the lesson.

---

# 55. Troubleshoot from left to right

Use this order:

~~~text
assignment
    |
    v
Okta policy
    |
    v
Okta authentication
    |
    v
SAML issuance
    |
    v
SAML validation
    |
    v
local account match / JIT
    |
    v
application authorization
    |
    v
lifecycle provisioning
~~~

Stop at the first failed layer.

Do not keep changing later layers after you already found the first failure.

---

# 56. What not to change first

If the user is unassigned, do not change:

- SAML certificate
- Audience
- ACS URL
- NameID
- claims

If MFA is unexpected, do not change:

- Assertion attributes
- SP decryption key
- Recipient

If JIT fails after SAML passes, do not change:

- Okta SAML signing certificate
- AuthnRequest SigAlg
- Audience

If pre-provisioning is required, do not try to solve it with:

- another SAML claim
- a different NameID format
- a longer Okta session

Match the fix to the failed layer.

---

# 57. A compact decision tree

~~~text
Did Okta issue a SAMLResponse?
        |
        +---- NO
        |      |
        |      v
        |   check assignment,
        |   session, and auth policy
        |
        +---- YES
               |
               v
        Did AcmeHR validate it?
               |
               +---- NO
               |      |
               |      v
               |   troubleshoot SAML
               |
               +---- YES
                      |
                      v
             Did AcmeHR match/create
             the local account?
                      |
                      +---- NO
                      |      |
                      |      v
                      |   troubleshoot
                      |   account/JIT layer
                      |
                      +---- YES
                             |
                             v
                    Is ongoing lifecycle
                    provisioning required?
                             |
                             +---- YES
                             |      |
                             |      v
                             |   evaluate SCIM
                             |
                             +---- NO
                                    |
                                    v
                               continue app
                               authorization
~~~

---

# 58. Explain it back

By the end of today, you should be able to explain this without reading the lesson:

~~~text
Assignment
    decides whether the Okta user can use the app integration.

Global Session Policy
    helps establish the Okta session context.

App sign-in policy
    decides what authentication the app request requires.

MFA
    can be a policy result before SAML is issued.

SAML
    authenticates the federated user to the SP.

AcmeHR JIT
    is target-side first-login account creation after successful SAML.

SCIM
    handles account lifecycle independently from interactive login.
~~~

Then answer:

> If a user is not assigned, which layer failed?

> If MFA appears unexpectedly, which layer do you inspect?

> If SAML validates but account creation fails, did authentication fail?

> If the account must exist before first login, is JIT enough?

> If a user must be disabled without another login, what class of integration do you need?

If you can answer those clearly, you are ready for the Day 10 lab.

---

# 59. What Day 10 deliberately does not cover

Today does not teach:

- IdP-initiated SSO
- RelayState behavior
- Okta versus AcmeHR session logout
- SAML Single Logout

Those belong to Day 11.

Today also does not teach certificate rollover.

That belongs to Day 12.

Keeping those topics separate prevents one troubleshooting incident from becoming five different lessons at once.

---

# 60. Day 10 evidence rule

For every Day 10 failure, collect evidence in this order:

~~~text
1. What was the user trying to do?

2. What is the last step I can prove succeeded?

3. What is the first step I can prove failed?

4. Which layer owns that decision?

5. What one configuration or application behavior should I inspect?

6. What should I leave unchanged?

7. How will I prove the fix?
~~~

This is the same troubleshooting discipline you used for SAML validation.

Today you are applying it to the systems around SAML.

---

# Official references used for Day 10

Current Okta behavior was checked against these official references while building this lesson:

- Okta policies and rules:
  https://help.okta.com/oie/en-us/Content/Topics/identity-engine/policies/about-policies.htm

- Global session policies:
  https://help.okta.com/oie/en-us/Content/Topics/identity-engine/policies/about-okta-sign-on-policies.htm

- App sign-in policy rules:
  https://help.okta.com/oie/en-us/content/topics/identity-engine/policies/add-app-sign-on-policy-rule.htm

- Assign app integrations:
  https://help.okta.com/en-us/content/topics/apps/apps-assign-applications.htm

- Assign app integrations and provisioning behavior:
  https://help.okta.com/en-us/content/topics/provisioning/lcm/lcm-user-app-assign.htm

- Learn about app integrations, SSO, and SCIM provisioning:
  https://help.okta.com/oie/en-us/content/topics/apps/apps-overview-learn-about.htm

- Add SCIM provisioning to app integrations:
  https://help.okta.com/en-us/Content/Topics/Apps/Apps_App_Integration_Wizard_SCIM.htm

- SAML app integrations, including Okta as IdP and Okta as SP:
  https://help.okta.com/okta_help.htm?id=ext-apps-about-saml

- Add a SAML Identity Provider and inbound Okta JIT behavior:
  https://help.okta.com/oie/en-us/Content/Topics/Security/idp-add-saml.htm

---

# Day 10 checkpoint

You are ready for the lab when these statements are clear:

~~~text
[ ] App assignment is not the same as a SAML group claim.

[ ] App assignment is not the same as target-account provisioning.

[ ] Global Session Policy and app sign-in policy are different layers.

[ ] An MFA prompt can be a correct policy result.

[ ] A failure before SAMLResponse issuance is not yet an SP SAML-validation failure.

[ ] SAML authentication can succeed before target-account JIT fails.

[ ] The course's AcmeHR JIT is SP-side first-login account creation.

[ ] Okta's documented inbound JIT is a different direction.

[ ] JIT depends on a login event.

[ ] SCIM can manage lifecycle without waiting for login.

[ ] SAML and SCIM can exist in the same app integration.

[ ] Removing Okta assignment does not by itself prove the target account was deactivated.

[ ] I can use last-success / first-failure evidence to identify the failed layer.
~~~

Do not continue because the terminology sounds familiar.

Continue when you can explain why each statement is true.
