# Day 10 Lab: Separate Assignment, MFA, SAML, JIT, and SCIM

## What you are going to do

Until now, most failures in the course were inside the SAML transaction.

Today you will deliberately create failures around SAML without treating all of them as SAML protocol failures.

You will work through four different situations:

~~~text
Incident 1
    user is not assigned to AcmeHR

Incident 2
    Okta requires additional authentication

Incident 3
    SAML succeeds but AcmeHR first-login account creation fails

Incident 4
    the business requirement needs lifecycle provisioning before or without login
~~~

For every incident, you will answer:

> What is the last step I can prove succeeded?

and:

> What is the first step I can prove failed?

The Day 10 path is:

~~~text
Okta app assignment
        |
        v
Okta authentication policy
        |
        v
Okta authentication
        |
        v
SAMLResponse issued
        |
        v
AcmeHR SAML validation
        |
        v
AcmeHR local account match / JIT
        |
        v
AcmeHR application access
~~~

SCIM is a separate lifecycle path that can create or update the target account without waiting for an interactive SAML login.

---

# Before you start

You need:

- the completed Day 10 lesson
- the restored, working Day 9 course baseline
- the AcmeHR Training Okta app
- your existing assigned test user
- permission to create or use one additional non-production test user
- Okta Admin Console access
- browser Developer Tools
- access to the Okta System Log
- Docker with Docker Compose
- the Day 10 implementation checkpoint before the JIT sections of this lab

Do not use a production user.

Do not change a shared production authentication policy.

Do not change Global Session Policy for this exercise.

---

# Safety rule for Day 10

Today you will change:

- application assignment
- an app sign-in policy assigned only to the training app
- one disposable test user's profile data
- AcmeHR training-account state after the Day 10 implementation exists

You will not change:

- SAML Entity ID
- ACS URL
- Audience
- Recipient
- Okta SAML signing certificate
- AcmeHR request-signing certificate
- AcmeHR decryption certificate
- Global Session Policy

This is deliberate.

The point is to prove that assignment, MFA, JIT, and provisioning are different layers from SAML protocol configuration.

---

# Part 1: Prove the known-good baseline

Use your existing assigned test user.

Start a fresh private or incognito browser session.

Open:

~~~text
http://localhost:8000/protected
~~~

Complete the normal login.

Confirm:

~~~text
Okta app access
    PASS

Okta authentication
    PASS

SAMLResponse reaches /saml/acs
    PASS

AcmeHR SAML validation
    PASS

AcmeHR protected page
    OPENS
~~~

Do not start Day 10 failure testing from an already-broken SAML baseline.

---

# Part 2: Record the current Okta assignment state

Open:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Assignments
~~~

Record:

| Question | Current value |
| --- | --- |
| Existing primary test user assigned? |  |
| Assignment type: Individual or Group? |  |
| Any groups assigned to AcmeHR? |  |
| Is SCIM provisioning currently configured? |  |

Do not change anything yet.

---

# Part 3: Keep app assignment separate from Day 7 group claims

Review the Day 7 claim configuration.

You may still have a SAML claim named:

~~~text
groups
~~~

containing values such as:

~~~text
AcmeHR-Employees
AcmeHR-Managers
~~~

Those claim values are not proof that the Okta application itself is assigned through those groups.

Assignment is visible on the app's:

~~~text
Assignments
~~~

tab.

Claim generation is configured separately on:

~~~text
Sign On
    ->
Attribute Statements
~~~

Do not mix these two pieces of evidence.

---

# Part 4: Create or select a second dedicated test user

For the unassigned-user and JIT incidents, use a second non-production Okta user.

We will call the user:

~~~text
Maya Day10
~~~

If you already have an appropriate disposable test user, reuse it.

Otherwise create one from:

~~~text
Directory
    ->
People
    ->
Add Person
~~~

Use a test username and email address that you control.

Do not add Maya to a group that already grants AcmeHR access.

Do not assign AcmeHR to Maya yet.

Before using Maya for the assignment incident, prove the Okta user itself is usable.

Open Maya under:

~~~text
Directory
    ->
People
~~~

Confirm:

~~~text
Okta user status
    ACTIVE
~~~

If you just created Maya, complete the normal activation or password setup required by your training org.

Then prove Maya can authenticate to Okta itself without using the AcmeHR application.

Do not continue if Maya is:

~~~text
Staged
Pending user action
Locked out
Suspended
Deactivated
~~~

Otherwise a user-status or credential problem could be mistaken for an AcmeHR assignment failure.

---

# Part 5: Give Maya the normal Day 7 profile values except one JIT field

Prepare Maya with the profile values used by the Day 7 claims.

Use values such as:

| Attribute | Training value |
| --- | --- |
| firstName | Maya |
| lastName | Patel |
| email | Maya's test email |
| department | Finance |
| employeeNumber | leave empty for now |

The missing employeeNumber is intentional.

It will be used later for the AcmeHR JIT failure.

For the assignment incident, the profile value does not matter yet because Maya should not get far enough for AcmeHR to process a successful SAMLResponse.

---

# Part 6: Prove Maya is not assigned

Return to:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Assignments
~~~

Search for Maya.

Confirm:

~~~text
Maya assigned directly
    NO

Maya receives AcmeHR through assigned group
    NO
~~~

If Maya receives the app through a group, use another test user that does not.

Do not remove a shared group's assignment merely to manufacture this incident.

---

# Part 7: Start the unassigned-user incident

Open a fresh private or incognito browser session.

Open:

~~~text
http://localhost:8000/protected
~~~

Authenticate as Maya if Okta asks for identity credentials.

Observe what happens.

The exact screen order can depend on existing Okta session state.

The important evidence question is:

> Did this attempt produce a successful SAMLResponse for AcmeHR?

---

# Part 8: Capture the browser evidence

Open Developer Tools.

Use:

~~~text
Network
Preserve log
~~~

For the failed attempt, check whether the browser POSTed a SAMLResponse to:

~~~text
http://localhost:8000/saml/acs
~~~

Record:

~~~text
successful SAMLResponse POST to AcmeHR
    YES / NO
~~~

For an assignment denial, expected:

~~~text
NO
~~~

Do not troubleshoot AcmeHR Audience or Recipient when no successful SAMLResponse reached AcmeHR.

---

# Part 9: Check the Okta System Log

Open:

~~~text
Reports
    ->
System Log
~~~

Filter or search using:

- Maya's username
- AcmeHR Training
- the time of the failed access attempt

Record the event or events that explain the Okta-side decision.

Do not depend on an event name from memory.

Use the actual event in your org.

Your evidence should show that the failure belongs to Okta app access or assignment rather than AcmeHR SAML validation.

---

# Part 10: Write the first incident record

Complete:

~~~text
Observed symptom:
________________________________

Last confirmed successful step:
________________________________

First failed step:
________________________________

SAMLResponse successfully issued to AcmeHR?
    YES / NO

Failed layer:
    Okta app assignment / access eligibility

Evidence:
________________________________

What I will not change:
    ACS URL
    Audience
    claims
    signing certificates
~~~

Do not continue until you can explain why this is not an Audience failure.

---

# Part 11: Assign Maya directly to AcmeHR

Now fix only the failed layer.

Open:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Assignments
    ->
Assign
    ->
Assign to People
~~~

Find Maya.

Click:

~~~text
Assign
~~~

Review the assignment data.

Save the assignment and finish.

Do not add Maya to an unrelated group just to make the app appear.

---

# Part 12: Prove the assignment exists

On the AcmeHR Assignments tab, confirm Maya appears.

Record:

~~~text
Maya
    ASSIGNED

Type
    Individual
~~~

You can also use the System Log.

Okta documents an assignment event such as:

~~~text
Add user to application membership
~~~

for successful user assignment.

Use the event actually shown in your org.

---

# Part 13: Retry only the assignment layer

Start a fresh private or incognito session.

Open:

~~~text
http://localhost:8000/protected
~~~

Authenticate as Maya.

At this point the assignment failure should be gone.

Do not expect the later JIT portion to succeed yet because employeeNumber is still intentionally empty.

Your immediate proof is:

~~~text
Okta no longer stops Maya as unassigned
~~~

---

# Part 14: Record the original AcmeHR app sign-in policy

Before the MFA exercise, record the app's current policy.

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
User authentication
~~~

Record:

| Question | Starting value |
| --- | --- |
| Assigned app sign-in policy |  |
| Does this policy serve other apps? |  |

Do not edit the shared default policy.

Okta documents that new apps can use a shared default policy.

Changing that shared policy can affect other apps.

---

# Part 15: Confirm the test user can satisfy an MFA exercise

Use your original primary test user for the MFA incident.

Before creating a two-factor rule, confirm the user already has two allowed authentication factor types available in your training org.

If the user does not, do not create a fake result.

You can still test reauthentication frequency with a one-factor rule, but record it as:

~~~text
reauthentication
~~~

not:

~~~text
MFA
~~~

For the full Day 10 MFA proof, use a test user that can satisfy two factor types.

Now create one temporary native Okta group used only to target the policy rule.

Open:

~~~text
Directory
    ->
Groups
    ->
Add Group
~~~

Create:

~~~text
Name
    Day10-MFA-Test

Description
    Temporary group for the AcmeHR Day 10 policy exercise
~~~

Do not name this group with the `AcmeHR-` prefix.

The Day 7 SAML groups claim intentionally selects names beginning with `AcmeHR-`.

Using `Day10-MFA-Test` keeps the MFA targeting group out of that training claim.

Open the new group and assign only the primary Day 10 test user.

Confirm:

~~~text
Day10-MFA-Test
    contains primary test user

Maya
    does not need to be in this group
~~~

---

# Part 16: Create a dedicated Day 10 app sign-in policy

Open:

~~~text
Security
    ->
Authentication Policies
    ->
App sign-in
    ->
Create policy
~~~

Create:

~~~text
Name
    AcmeHR Day 10 Lab Policy

Description
    Temporary policy used only by the SAML training application
~~~

Save it.

Do not modify the shared default policy.

---

# Part 17: Add the Day 10 step-up rule

Open the new policy.

Go to:

~~~text
Rules
    ->
Add rule
~~~

Create a rule named:

~~~text
Day10 step-up
~~~

Set the rule's user or group condition so it applies to:

~~~text
Day10-MFA-Test
~~~

and not to all users in the org.

For the full MFA exercise, configure:

~~~text
IF
    User's group membership includes Day10-MFA-Test

THEN
    Access is Allowed

AND
    User must authenticate with Any 2 factor types

AND
    Prompt for authentication Every time user signs in to resource
~~~

Leave unrelated device, network, or risk conditions unchanged unless your tenant requires them.

Save the rule.

Move it above the catch-all rule if necessary so it can match first.

---

# Part 18: Assign only AcmeHR to the temporary policy

Use either current Okta path.

From the policy:

~~~text
Security
    ->
Authentication Policies
    ->
App sign-in
    ->
AcmeHR Day 10 Lab Policy
    ->
Applications
    ->
Add app
~~~

or from the app:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Sign On
    ->
User authentication
    ->
Edit
~~~

Select:

~~~text
AcmeHR Day 10 Lab Policy
~~~

Save.

Confirm the policy is assigned to AcmeHR Training.

Do not add other applications to this temporary policy.

---

# Part 19: Start the unexpected-MFA incident

Use a fresh private or incognito browser session.

Open:

~~~text
http://localhost:8000/protected
~~~

Sign in with the primary assigned test user.

Observe the authentication prompts.

If the rule requires two factor types, record the factor challenges that Okta actually presents.

Do not assume the exact prompt sequence from the policy text.

Existing authenticator enrollment and Global Session Policy can affect what you see.

---

# Part 20: Prove the policy decision happens before the final SAMLResponse

Keep the browser Network panel open.

Before completing the required authentication, check whether AcmeHR has already received the successful SAMLResponse for this transaction.

Expected while the policy challenge is still pending:

~~~text
successful POST to /saml/acs
    NOT YET
~~~

After the policy requirement is satisfied:

~~~text
successful POST to /saml/acs
    PRESENT
~~~

This separates:

~~~text
Okta authentication policy
~~~

from:

~~~text
AcmeHR SAML validation
~~~

---

# Part 21: Check the policy evidence in Okta

Open the System Log.

Search around the time of the MFA or reauthentication attempt.

Record:

- the user
- AcmeHR Training
- the authentication event
- any policy or rule information the event exposes
- the outcome

Also record:

~~~text
Policy assigned to AcmeHR
    AcmeHR Day 10 Lab Policy

Rule intended to match
    Day10 step-up
~~~

Use the actual event evidence to support your conclusion.

---

# Part 22: Write the MFA incident record

Complete:

~~~text
Observed symptom:
User sees an additional authentication challenge.

Last confirmed successful step:
________________________________

First failed or pending step:
Okta app authentication requirement

SAMLResponse already issued at that point?
    YES / NO

Matched app policy:
________________________________

Matched rule evidence:
________________________________

Failed or challenged layer:
    Okta authentication policy

What I will not change:
    SAML claims
    Audience
    Recipient
    SAML certificates
~~~

If the challenge is required by policy, it is not a SAML claim defect.

---

# Part 23: Restore the original AcmeHR app sign-in policy

Return AcmeHR Training to the exact policy you recorded in Part 14.

Use:

~~~text
Applications and Resources
    ->
Applications
    ->
AcmeHR Training
    ->
Sign On
    ->
User authentication
    ->
Edit
~~~

or switch it from the policy's Applications tab.

Save.

Confirm AcmeHR is no longer assigned to:

~~~text
AcmeHR Day 10 Lab Policy
~~~

---

# Part 24: Delete the temporary policy only after AcmeHR is removed

Return to:

~~~text
Security
    ->
Authentication Policies
    ->
App sign-in
~~~

Confirm the Day 10 policy has no apps assigned.

Then delete:

~~~text
AcmeHR Day 10 Lab Policy
~~~

Do not leave a temporary MFA policy attached to the training application.

Now clean up the temporary targeting group.

Open:

~~~text
Directory
    ->
Groups
    ->
Day10-MFA-Test
~~~

Remove the primary test user from the group.

Then delete the temporary group.

Confirm:

~~~text
AcmeHR Day 10 Lab Policy
    DELETED

Day10-MFA-Test
    DELETED
~~~

The group existed only to isolate the policy experiment.

---

# Part 25: Prove the normal authentication experience is restored

Start a fresh transaction with the primary test user.

Confirm:

~~~text
original AcmeHR app sign-in policy
    RESTORED

fresh SAML login
    PASS

AcmeHR protected page
    OPENS
~~~

The policy experiment should not require any SAML claim or certificate change.

---

# Part 26: Day 10 AcmeHR JIT implementation checkpoint

Do not run the next live JIT incident until the repository provides an application-side training account model.

The implementation must prove these behaviors locally before you depend on them in Okta:

~~~text
[ ] SAML authentication is accepted before AcmeHR JIT logic runs

[ ] AcmeHR can distinguish an existing local training account from no local account

[ ] the JIT match key is explicit

[ ] JIT creation uses only validated SAML data

[ ] employeeNumber is required for the training-account creation contract

[ ] missing employeeNumber does not become a fake SAML signature or Audience failure

[ ] failed JIT blocks AcmeHR application access that requires a local account

[ ] successful JIT creates the training account once

[ ] later logins match the existing account instead of creating duplicates

[ ] learner-visible evidence separates SAML PASS from JIT PASS / FAIL

[ ] training-account state can be reset safely for the lab
~~~

If these checks are not implemented yet, stop the JIT portion here.

Do not pretend the current Spring authenticated principal is already a complete lifecycle implementation.

---

# Part 27: Confirm Maya has no AcmeHR local training account

After the Day 10 implementation checkpoint exists, use the application-side training-account view documented by that implementation.

Confirm:

~~~text
Maya SAML identity
    known to Okta

Maya AcmeHR local training account
    NOT PRESENT
~~~

Do not delete or alter the primary user's account to create this incident.

---

# Part 28: Keep Maya assigned but leave employeeNumber empty

Confirm:

~~~text
Okta app assignment
    PRESENT

Maya employeeNumber
    EMPTY
~~~

Keep the normal Day 7 employeeNumber claim expression configured.

The expected SAML result is that the application receives no usable employeeNumber value for Maya.

Do not change the claim name.

We want one missing source value, not a different claim contract.

---

# Part 29: Start the failed-JIT transaction

Open a fresh private or incognito session.

Open:

~~~text
http://localhost:8000/protected
~~~

Authenticate as Maya.

Capture the SAMLResponse locally using the same safe workflow from earlier days.

Do not upload the Assertion to a public decoder.

---

# Part 30: Prove SAML authentication succeeded first

Use the Day 10 application evidence.

Confirm:

~~~text
Okta authentication
    PASS

SAMLResponse reached AcmeHR
    PASS

SAML signature validation
    PASS

SAML semantic validation
    PASS
~~~

Then inspect the JIT result.

Expected:

~~~text
existing AcmeHR account match
    NO

JIT creation attempted
    YES

JIT creation
    FAIL

reason
    employeeNumber required
~~~

This is the central Day 10 proof.

---

# Part 31: Do not call the JIT failure a SAML authentication failure

Write:

~~~text
SAML authentication
    PASS

AcmeHR account lifecycle
    FAIL
~~~

Do not write:

~~~text
SAML failed because employeeNumber was missing
~~~

The SAML transaction already passed its security validation.

The application account contract failed afterward.

---

# Part 32: Write the failed-JIT incident record

Complete:

~~~text
Observed symptom:
________________________________

Last confirmed successful step:
AcmeHR accepted the SAML authentication.

First failed step:
AcmeHR local account creation.

Evidence:
employeeNumber missing from the validated identity data.

Root cause:
AcmeHR JIT account contract requires employeeNumber.

Single change:
Restore Maya's employeeNumber source value.

What I will not change:
    SAML signing certificate
    Audience
    Recipient
    AuthnRequest signature
~~~

---

# Part 33: Fix only the missing JIT source value

Open Maya's Okta profile.

Set:

~~~text
employeeNumber
    E20427
~~~

Do not change:

- app assignment
- NameID
- claim name
- SAML certificates
- authentication policy

We are changing one source value only.

---

# Part 34: Start a fresh login and prove JIT succeeds

Use a new private or incognito transaction.

Authenticate as Maya.

Confirm:

~~~text
SAML authentication
    PASS

existing AcmeHR account match
    NO

JIT creation
    PASS

AcmeHR local account
    CREATED

application access
    ALLOWED
~~~

Record the local account identifier created by the training implementation.

Do not record secrets or session cookies.

---

# Part 35: Prove the second login matches instead of creating a duplicate

Sign out of the local training session using the existing course method.

Start another fresh SAML login as Maya.

Expected:

~~~text
SAML authentication
    PASS

existing AcmeHR account match
    YES

JIT new-account creation
    NOT NEEDED

duplicate account
    NOT CREATED
~~~

This proves matching happens before account creation.

---

# Part 36: Compare the assignment and JIT incidents

Fill in:

| Question | Unassigned incident | JIT incident |
| --- | --- | --- |
| Okta user exists? | Yes | Yes |
| App assigned? | No | Yes |
| Okta authentication may occur? | Depends on session and flow | Yes |
| Successful SAMLResponse reaches AcmeHR? | No | Yes |
| SAML validation passes? | Not reached | Yes |
| AcmeHR local account step reached? | No | Yes |
| Failed layer | Okta access eligibility | AcmeHR lifecycle |

This is why:

~~~text
user cannot get into AcmeHR
~~~

is not enough information for a support ticket.

---

# Part 37: Inspect whether SCIM provisioning exists

Open the AcmeHR app settings.

Look for:

~~~text
Provisioning
~~~

Record:

~~~text
SCIM or other automated provisioning configured
    YES / NO
~~~

Do not enable SCIM merely to complete this lab.

The Day 10 SCIM exercise is a decision exercise unless the repository later provides a real training SCIM endpoint.

---

# Part 38: Read the business requirement before choosing JIT or SCIM

Scenario A:

> Maya's AcmeHR account may be created when she signs in for the first time.

Which model can satisfy it?

~~~text
AcmeHR first-login JIT
~~~

Scenario B:

> Maya starts Monday at 8:00 AM. Her AcmeHR account must already exist before she signs in.

Can login-triggered JIT guarantee this?

~~~text
NO
~~~

This requirement needs pre-provisioning.

SCIM is the appropriate class of solution when AcmeHR exposes a compatible provisioning interface.

---

# Part 39: Test the profile-update requirement

Scenario:

~~~text
Friday
    Maya.department = Finance

Saturday
    HR changes Maya.department = Accounting

Maya does not sign in during the weekend.

Business requirement:
    AcmeHR must show Accounting before Monday morning.
~~~

Can login-triggered JIT guarantee the update during the weekend?

~~~text
NO
~~~

Why?

~~~text
no login
    ->
no first-login or login-time event
~~~

A lifecycle provisioning mechanism can push profile updates independently of SAML login.

---

# Part 40: Test the termination requirement

Scenario:

~~~text
5:00 PM
    Maya leaves the company

Maya never signs into AcmeHR again

Requirement
    AcmeHR account must be disabled immediately
~~~

Can a future SAML Assertion solve this if no future login occurs?

~~~text
NO
~~~

This is lifecycle provisioning.

Okta SCIM integrations can support deactivation when the integration and target support it.

Do not add another SAML claim and call the lifecycle requirement solved.

---

# Part 41: Understand assignment removal versus target deactivation

If Maya is unassigned from AcmeHR in Okta:

~~~text
future Okta access to app
    can stop
~~~

But that does not by itself prove:

~~~text
Maya's AcmeHR local account
    DEACTIVATED
~~~

When provisioning is configured, Okta can send deactivation to supported downstream integrations.

When provisioning is not configured, inspect the target account separately.

Never infer target deactivation only from the Okta Assignments tab.

---

# Part 42: One provisioning timing trap

Okta documents a useful troubleshooting detail.

If provisioning is enabled on an app that already has assigned users, Okta does not automatically provision those existing assignments merely because provisioning was switched on.

That means this state can exist:

~~~text
user assigned
    YES

provisioning now enabled
    YES

target account
    STILL NOT CREATED
~~~

Do not report:

~~~text
SAML did not create the account
~~~

Check whether the user was actually sent through the provisioning flow.

---

# Part 43: Build the Day 10 layer table from your evidence

Complete this table.

| Layer | Evidence from your lab | PASS / FAIL example |
| --- | --- | --- |
| Okta assignment | Assignments tab / System Log |  |
| Okta app authentication | policy and rule evidence |  |
| SAML issuance | browser network trace |  |
| AcmeHR SAML validation | AcmeHR evidence |  |
| AcmeHR account match | Day 10 account evidence |  |
| AcmeHR JIT | Day 10 account evidence |  |
| SCIM provisioning | Provisioning configuration / lifecycle evidence |  |
| Application authorization | AcmeHR role/access result |  |

This table is the main Day 10 troubleshooting artifact.

---

# Part 44: Use the decision tree without guessing

For each incident, walk this path:

~~~text
Did Okta issue a successful SAMLResponse?
        |
        +---- NO
        |      |
        |      v
        |   inspect assignment,
        |   session, and policy
        |
        +---- YES
               |
               v
        Did AcmeHR validate it?
               |
               +---- NO
               |      |
               |      v
               |   inspect SAML
               |
               +---- YES
                      |
                      v
             Did AcmeHR match or
             create the account?
                      |
                      +---- NO
                      |      |
                      |      v
                      |   inspect JIT /
                      |   account lifecycle
                      |
                      +---- YES
                             |
                             v
                    Must account changes
                    happen without login?
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

Do not skip to the last box.

---

# Part 45: Mini challenge, app assignment versus SAML group claim

Priya has:

~~~text
groups claim
    AcmeHR-Managers
~~~

but she is not assigned to the AcmeHR Okta application.

Does the SAML group claim rescue the assignment failure?

<details>
<summary>Check your answer</summary>

No.

The claim can matter only in a SAMLResponse that Okta successfully issues.

App assignment and SAML claim generation are separate controls.

</details>

---

# Part 46: Mini challenge, MFA versus claim failure

Priya reaches Okta and receives an MFA challenge before AcmeHR gets a SAMLResponse.

Should you change:

~~~text
department
groups
NameID
~~~

first?

<details>
<summary>Check your answer</summary>

No.

Inspect the Okta authentication policy and matched rule first.

The claim layer has not yet produced the final SAMLResponse for AcmeHR.

</details>

---

# Part 47: Mini challenge, SAML PASS and JIT FAIL

Evidence says:

~~~text
signature
    PASS

Audience
    PASS

Recipient
    PASS

InResponseTo
    PASS

AcmeHR JIT
    FAIL
~~~

Is changing the IdP signing certificate a reasonable first fix?

<details>
<summary>Check your answer</summary>

No.

The SAML security checks already passed.

Investigate the AcmeHR account-creation contract and its required data.

</details>

---

# Part 48: Mini challenge, pre-provisioning

Requirement:

~~~text
Account must exist before first login.
~~~

Which is the stronger fit?

~~~text
login-time JIT
or
SCIM provisioning
~~~

<details>
<summary>Check your answer</summary>

SCIM provisioning, when the target supports it.

Login-time JIT cannot create the account before the triggering login.

</details>

---

# Part 49: Mini challenge, termination

Requirement:

~~~text
Target account must be disabled even if the user never logs in again.
~~~

Can SAML login-time claims guarantee that?

<details>
<summary>Check your answer</summary>

No.

Without another login there is no new SAML Assertion.

Use a lifecycle provisioning mechanism such as SCIM when supported.

</details>

---

# Part 50: Restore the Day 10 training state

Before finishing, confirm:

~~~text
AcmeHR original app sign-in policy
    RESTORED

temporary Day 10 app sign-in policy
    REMOVED

primary test user's existing assignment
    UNCHANGED

Maya assignment
    record whether you are keeping or removing it

Maya employeeNumber
    record final training value

SAML certificates
    UNCHANGED

SAML Entity ID / ACS / Audience
    UNCHANGED

fresh primary-user SAML login
    PASS
~~~

If Maya was created only for Day 10, you may remove her AcmeHR assignment after saving your evidence.

Do not delete a shared or production user.

---

# Part 51: Prove the final SAML baseline again

Use the original primary test user.

Start a new private or incognito session.

Open:

~~~text
http://localhost:8000/protected
~~~

Confirm:

~~~text
Okta access
    PASS

normal authentication policy
    RESTORED

SAMLResponse
    ISSUED

AcmeHR validation
    PASS

protected page
    OPENS
~~~

Day 10 must end with a known-good federation baseline.

---

# Part 52: Explain it back

Explain each line:

~~~text
Okta assignment
    decides whether the user is eligible to use the app integration

Okta app sign-in policy
    decides what authentication the app request requires

MFA
    can happen before the final SAMLResponse is issued

SAML
    authenticates the federated identity to AcmeHR

AcmeHR JIT
    can create the target application's local account after valid SAML

SCIM
    can manage the target-account lifecycle without waiting for login
~~~

Then explain:

> Why did the unassigned-user incident stop before AcmeHR SAML validation?

> Why did the MFA incident not require a claim change?

> How did you prove SAML passed before JIT failed?

> Why can JIT not guarantee pre-provisioning?

> Why can SAML alone not guarantee immediate deactivation without another login?

---

# Day 10 lab completion check

Do not mark Day 10 complete until you can prove:

~~~text
[ ] I started from a working SAML baseline.

[ ] I recorded the original AcmeHR assignment state.

[ ] I used a dedicated unassigned test user rather than changing a shared group assignment.

[ ] I proved the unassigned attempt did not produce a successful SAMLResponse to AcmeHR.

[ ] I used the Okta System Log to support the assignment diagnosis.

[ ] I fixed only the assignment layer and proved the assignment was restored.

[ ] I recorded the original AcmeHR app sign-in policy.

[ ] I did not edit the shared default app sign-in policy.

[ ] I proved Maya's Okta user was Active and usable before testing assignment failure.

[ ] I created a temporary Day10-MFA-Test group without using the AcmeHR- claim prefix.

[ ] I put only the primary test user in Day10-MFA-Test.

[ ] I created a temporary policy used only by AcmeHR Training.

[ ] I targeted the Day10 step-up rule to Day10-MFA-Test.

[ ] I proved the additional authentication challenge belonged to Okta policy, not SAML claims.

[ ] I restored the original app sign-in policy and removed the temporary policy.

[ ] I removed the temporary Day10-MFA-Test group.

[ ] I waited for the Day 10 AcmeHR JIT implementation checkpoint before running the JIT incident.

[ ] I proved SAML validation passed before the AcmeHR JIT failure.

[ ] I proved missing employeeNumber caused the training-account creation failure.

[ ] I restored employeeNumber and proved a fresh JIT account creation succeeded.

[ ] I proved the next login matched the existing account instead of creating a duplicate.

[ ] I inspected whether SCIM provisioning is configured.

[ ] I can explain why pre-provisioning is not a login-time JIT use case.

[ ] I can explain why profile updates without login require lifecycle provisioning.

[ ] I can explain why deactivation without another login requires lifecycle provisioning.

[ ] I did not change SAML certificates, ACS, Entity ID, Audience, or Recipient for any Day 10 incident.

[ ] I restored the normal authentication-policy baseline.

[ ] I proved a final fresh SAML login works.
~~~

---

# Save your Day 10 evidence

Keep in your private training notes:

1. original AcmeHR assignment state
2. Maya Active-user evidence
3. Maya unassigned evidence
4. browser proof that no successful SAMLResponse reached AcmeHR during the assignment failure
5. relevant System Log evidence for the assignment incident
6. Maya direct-assignment proof
7. original AcmeHR app sign-in policy
8. temporary Day10-MFA-Test group membership
9. temporary Day 10 policy and rule settings
10. browser evidence of the additional authentication challenge
11. relevant System Log policy/authentication evidence
12. proof that the original app sign-in policy was restored
13. proof that the temporary policy and Day10-MFA-Test group were removed
14. Day 10 JIT implementation-checkpoint result
15. Maya local-account state before JIT
16. SAML PASS / JIT FAIL evidence
17. missing employeeNumber JIT failure evidence
18. restored employeeNumber value
19. JIT PASS and local-account creation evidence
20. second-login existing-account match evidence
21. current AcmeHR Provisioning-tab state
22. JIT versus SCIM decision notes
23. completed Day 10 layer table
24. final restored SAML baseline proof

Do not save:

- passwords
- MFA codes
- session cookies
- full production SAML assertions
- production personal data

Use redacted screenshots and non-production test identities.

---

# Official references for this lab

Current Okta behavior used by this lab was checked against:

- Assign app integrations:
  https://help.okta.com/en-us/content/topics/apps/apps-assign-applications.htm

- Manage app integration assignments:
  https://help.okta.com/en-us/Content/Topics/Apps/apps-manage-assignments.htm

- Assign an app integration to a user:
  https://help.okta.com/en-us/content/topics/provisioning/lcm/lcm-assign-app-user.htm

- Create an app sign-in policy:
  https://help.okta.com/oie/en-us/Content/Topics/identity-engine/policies/create-auth-policy.htm

- Add an app sign-in policy rule:
  https://help.okta.com/oie/en-us/content/topics/identity-engine/policies/add-app-sign-on-policy-rule.htm

- Assign apps to an app sign-in policy:
  https://help.okta.com/oie/en-us/content/topics/identity-engine/policies/share-auth-policies.htm

- App sign-in policies:
  https://help.okta.com/oie/en-us/Content/Topics/identity-engine/policies/about-app-sign-on-policies.htm

- Configure app integration settings:
  https://help.okta.com/oie/en-us/Content/Topics/Apps/apps-configure-settings.htm

- Add users manually:
  https://help.okta.com/oie/en-us/content/topics/users-groups-profiles/usgp-add-users.htm

- Create a native Okta group:
  https://help.okta.com/oie/en-us/content/topics/users-groups-profiles/usgp-groups-create.htm

- Manually assign people to a group:
  https://help.okta.com/oie/en-us/content/topics/users-groups-profiles/usgp-assign-group-people.htm

- Add and update users with Okta inbound JIT:
  https://help.okta.com/oie/en-us/content/topics/users-groups-profiles/usgp-add-users-jit.htm

- Add SCIM provisioning to app integrations:
  https://help.okta.com/en-us/Content/Topics/Apps/Apps_App_Integration_Wizard_SCIM.htm

- SCIM provisioning settings and supported operations:
  https://help.okta.com/en-us/content/topics/apps/oiw/provisioning-settings.htm

- Configure provisioning for an app integration:
  https://help.okta.com/en-us/content/topics/provisioning/lcm/lcm-provision-application.htm

---

# Final Day 10 question

Someone opens a ticket:

~~~text
Priya cannot use AcmeHR.
SSO is broken.
~~~

Your first response should not be:

~~~text
Change the SAML configuration.
~~~

Your first job is to identify:

~~~text
last confirmed successful step

first failed step

layer that owns that decision
~~~

Only then choose what to change.
