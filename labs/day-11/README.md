# Day 11 Lab: Compare Login Starts, RelayState, Sessions, and Logout

## What you are going to do

Today, take two routes to the same AcmeHR application: start at AcmeHR and start at the Okta tile. Keep a record of what the browser actually sends. Then check which session survives a local logout and an Okta sign-out.

The code you have today supports the AcmeHR-started SAML login and has a local sign-out button. Its `SecurityConfig` does not enable SAML Single Logout (SLO). Test the Okta tile rather than assuming it works. The SLO section is a design checkpoint until both sides are configured and tested.

Your guiding question is:

> Who started this login, and which session ended?

---

# Before you start

You need the [Day 11 lesson](../../lessons/day-11-initiation-relaystate-sessions-logout.md), the working Day 10 AcmeHR baseline, your assigned non-production Okta user, the Okta app tile if your org exposes it, browser Developer Tools, and access to the Okta Admin Console if you want to inspect Default Relay State. Run the existing SP from `lab-sp` with `docker compose up --build` and wait for `http://localhost:8000/actuator/health` to report `UP`.

If you cannot launch the Okta tile, record that limitation and complete the AcmeHR-started and session parts. Do not create a fake SAML response to imitate Okta.

Use one browser profile for a single comparison, and a fresh private profile when the instructions call for a clean start. In the Network panel, turn on **Preserve log** before navigation. Do not share raw SAML responses, cookies, passwords, MFA codes, private keys, or unmasked personal data.

# Part 1: Prove the existing AcmeHR-started flow

1. Open a fresh private window and request `http://localhost:8000/protected`. Complete the Okta sign-in as your assigned lab user.
2. In the Network panel, find the browser's trip to Okta. Look for `SAMLRequest` in the redirect URL and `RelayState` alongside it. If you decode the request using the earlier local-only lab method, record just the `AuthnRequest` ID, not the assertion or secrets.
3. Find the POST to `http://localhost:8000/saml/acs`. Check for `SAMLResponse` and, if present, `RelayState` in the form data. Decode locally as in the earlier labs to inspect `InResponseTo` in the response and subject confirmation. Do not upload the response to a public decoder.
4. On AcmeHR's home page, confirm **AcmeHR application session: Active**. On `/protected`, check for `SAML PASS` and the account result expected from Day 10. If JIT fails, fix that existing baseline first.

Write down whether the response refers to this browser's outstanding request. A copied matching ID by itself would not prove the browser had the saved request.

| AcmeHR-started observation | Your result |
| --- | --- |
| First page requested |  |
| Outgoing `AuthnRequest` seen? |  |
| Request ID, abbreviated |  |
| Outgoing and returning `RelayState` present? |  |
| `InResponseTo` matches the request? |  |
| Final page and AcmeHR session state |  |

# Part 2: Test the Okta tile from a clean start

Close that private window. Open a **new** private window, sign in to Okta, and click the AcmeHR Training tile if available. Preserve the network trace.

Look for the first request to AcmeHR. Did the browser POST `SAMLResponse` to `/saml/acs` without an earlier AcmeHR `SAMLRequest` in this window? Record whether `InResponseTo` is absent, whether `RelayState` is present, the final URL, and whether AcmeHR shows an active session. Do not infer an SP request from an Okta login prompt: find the actual SAML request in the trace.

| Okta-tile observation | Your result |
| --- | --- |
| Tile available and selected? |  |
| AcmeHR `AuthnRequest` before ACS POST? |  |
| ACS POST received? |  |
| `InResponseTo` present? |  |
| `RelayState` present? |  |
| Final page and AcmeHR session state |  |

If the tile succeeds, note that **this setup** accepted the unsolicited response; the absence of `InResponseTo` is expected when no SP request existed. If it fails, preserve the error and find the first failed step: tile availability, ACS delivery, SP registration lookup, message validation, or session creation. Do not turn off signature checks, destination checks, or request matching for SP-started logins to make it pass. A successful `/protected` login does not prove the tile must work.

# Part 3: Check where `RelayState` sends you

Open a new private window and start again at `http://localhost:8000/claims`. After login, record the final URL. Look at the outgoing and returning `RelayState` values, but do not assume they contain `/claims` as plain text. The SP may keep the original page in its own session instead.

Now compare this with your Okta-tile trial. In the training app's Okta **Sign On** settings, record the current **Default Relay State** if the setting is available. The tile has no AcmeHR page to remember. If you control this non-production app and want one reversible trial, save the original value, set a same-app path such as `/claims`, launch the tile in a new private window, and record what actually changes. Restore the original value immediately afterward. If Okta or AcmeHR rejects or ignores that value, record that outcome; do not claim the setting implements a deep link on its own.

Do not try an external URL as a destination. `RelayState` is navigation state; it is not a substitute for `InResponseTo` or a reason to accept an unsafe redirect.

# Part 4: End AcmeHR's session while keeping Okta signed in

Start a working AcmeHR login in one browser profile. Open Okta in another tab **of that same profile** and confirm that Okta still recognizes the lab user. Keep the Okta tab open.

Return to `http://localhost:8000/`. Confirm **AcmeHR application session: Active**, then click **Sign out of AcmeHR**. In the Network panel, confirm the form sent a POST to `/logout`. The form includes a CSRF token; do not copy its value into your notes. This is local logout. You should not see a SAML `LogoutRequest` sent to Okta.

Spring Security may send you to its login page after the POST. Go back to `http://localhost:8000/` and confirm **Not active**. Check the Okta tab separately: is the test user still signed in? Then request `/protected` again in the same profile. If Okta still has a session, the browser may complete a new SAML login without asking for credentials. Confirm a **new** `SAMLResponse` POST rather than relying on the lack of a password prompt.

If the button does not appear while AcmeHR shows an active session, the POST fails, or the home page still says **Active** after sign-out, record the first failed step. Do not treat a page reload, a bare GET to `/logout`, or deleting a browser cookie as proof that the server ended its session.

# Part 5: End Okta's session and check AcmeHR

Establish both sessions again in a fresh profile. Sign out using Okta's own user menu, without using AcmeHR's local logout. Before trying a new SAML login, revisit an already-open AcmeHR protected page and its home page in the same profile. Record whether the AcmeHR session is still active. If an Okta policy or configured logout feature changes the result, report the observed behavior; the two sessions are still separate things to inspect.

| Trial | Okta session after action | AcmeHR session after action | Fresh ACS POST on next AcmeHR login? |
| --- | --- | --- | --- |
| Local AcmeHR logout |  |  |  |
| Okta user sign-out |  |  |  |

Check the two domains' cookies in Developer Tools only to see which site owns them. Do not copy cookie values into your notes. Removing a browser cookie is a different experiment from asking a server to end its session.

# Part 6: SLO design checkpoint

Inspect `lab-sp/src/main/java/com/acme/training/acmehr/security/SecurityConfig.java`. It enables `saml2Login` and metadata, but does not call `saml2Logout`. The current course does not give you a verified SP SLO endpoint, SP SLO signing setup, or an Okta SLO configuration to exercise here. Do **not** enable Okta SLO simply because this lab mentions it.

On paper, draw the evidence you would need for a later controlled trial:

| SP-initiated SLO | IdP-initiated SLO |
| --- | --- |
| AcmeHR ends its local session and sends a signed `LogoutRequest` to Okta | Okta sends a `LogoutRequest` to AcmeHR's configured SLO endpoint |
| Okta validates it, ends the applicable Okta session, and returns a `LogoutResponse` | AcmeHR validates it, ends the matching local session, and returns a `LogoutResponse` |
| Verify response correlation, status, and each session separately | Verify issuer, signature, session targeting, and each session separately |

For either direction, check the relevant metadata endpoints, trust material, principal and `SessionIndex` handling, and the actual sessions that end. A `LogoutResponse` is evidence of a protocol exchange, not proof that every other application session on every device ended. Okta's multi-app SLO has participation and browser front-channel limits. A live SLO trial belongs **after** implementation, configuration, and focused validation tests.

# Part 7: Explain the result to another learner

Use your traces to finish these sentences:

1. “AcmeHR started this login because I saw ___ before the ACS POST.”
2. “The Okta tile started this login because ___, and the SP observed ___.”
3. “`RelayState` affected ___; request correlation came from ___.”
4. “After local logout, the ___ session ended, while ___.”
5. “We cannot claim SLO works here yet because ___.”

Your answer should say what you observed. If the tile or local logout did not work, explain the first point of failure and leave that row unresolved until you have tested a fix.

## Restore the training state

Restore Okta's original Default Relay State if you changed it. Leave Okta SLO disabled unless it was already part of your approved training baseline. Confirm `http://localhost:8000/protected` still completes a normal SP-initiated login in a fresh window. Keep only redacted observations in your notes.

## Official references

- [Okta: Understanding SAML](https://developer.okta.com/docs/concepts/saml/) — who starts login and what `RelayState` carries.
- [Okta: Configure Single Logout](https://developer.okta.com/docs/guides/single-logout/saml2/main/) — SLO participation and front-channel behavior.
- [Spring Security: Producing AuthnRequests](https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication-requests.html) — stored requests and session binding.
- [Spring Security: Handling Logouts](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html) — local logout and session invalidation.
- [Spring Security: Performing Single Logout](https://docs.spring.io/spring-security/reference/servlet/saml2/logout.html) — SLO prerequisites and protocol endpoints.
