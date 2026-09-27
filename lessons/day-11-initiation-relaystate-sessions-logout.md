# Day 11: Who Starts Login, Where You Land, and What Logout Ends

## What you should understand by the end of today

Imagine Priya opens `/manager` in AcmeHR. Now imagine she clicks AcmeHR's tile in Okta instead. Both journeys might end at the same assertion consumer service (ACS), but they begin with different evidence. Today you will trace that difference, follow `RelayState`, identify two separate browser sessions, and decide what a logout action actually ended.

Our current AcmeHR SP implements SAML login at `/saml/acs`. It does not explicitly configure SAML Single Logout (SLO). Treat the IdP-initiated and SLO cases below as investigation and design exercises until the lab verifies or implements them. Never report a successful live flow based on this lesson alone.

---

# 1. Start at the first browser action

For **SP-initiated login**, Priya asks AcmeHR for a protected page. AcmeHR creates an `AuthnRequest` and sends her browser to Okta. Okta returns a `SAMLResponse` to the ACS.

For **IdP-initiated login**, Priya starts in Okta and opens the AcmeHR tile. Okta sends a `SAMLResponse` to the ACS without first receiving an AcmeHR `AuthnRequest` for this transaction. This is also called an *unsolicited response*. It does not mean the response is automatically trustworthy.

| Question | SP-initiated | IdP-initiated |
| --- | --- | --- |
| First action | Open an AcmeHR page | Open the AcmeHR tile in Okta |
| AcmeHR `AuthnRequest` | Yes | No |
| Request ID to match with `InResponseTo` | Yes | No |
| Known original AcmeHR page | Often available | Usually absent unless separately conveyed |
| Return destination | Saved request or supported `RelayState` behavior | App landing page or a configured, supported destination |

Ask first: **Did AcmeHR issue an `AuthnRequest` for this browser journey?** A SAML response at the same ACS does not answer that question.

# 2. Match the response to the request when there is one

In the SP-initiated case, capture the outgoing request ID and the returning response's `InResponseTo` value. Check the response and subject confirmation data in context, and check that the SP can find its stored request in this browser's session. A matching string alone is weaker evidence than a match to the outstanding request held by the SP.

In a genuinely IdP-initiated flow there was no outstanding SP request ID. Do not invent one or disable request correlation across all logins to make a failing tile launch work. Whether a particular SP library accepts unsolicited responses, and under what checks, is an implementation decision to verify in the running lab. If it accepts them, it must still validate the configured issuer and signing trust, audience, recipient and destination, time conditions, subject confirmation, and replay protections as applicable. Consider the login CSRF risk: an attacker must not be able to cause someone else's browser to adopt the attacker's identity merely by delivering an otherwise valid response. Spring Security explains why keeping the outgoing request in the browser session helps defend against that case.

A response that lacks a matching request after **AcmeHR started login** is a different incident from an intentional Okta tile launch. Compare the browser trace and session state before changing a setting.

# 3. Follow `RelayState` separately

`RelayState` is a parameter carried alongside the SAML message in the browser binding. It is not an XML claim and it is not `InResponseTo`.

If Priya begins at `/manager`, AcmeHR may retain her original destination in its own session or send appropriate state in `RelayState`. Okta returns the supplied state with the SAML response, and AcmeHR decides where to go after validating login. Inspect the actual implementation: do not assume every SP encodes the page URL directly in `RelayState`.

When Priya starts from the Okta tile, there is no AcmeHR page request to remember. Okta's **Default Relay State** can identify an application resource for that IdP-initiated scenario, but AcmeHR must know how to interpret it. A value in Okta's setting does not by itself create a deep-link feature in AcmeHR.

For a safe design, send a short opaque state value tied to trusted SP-side data where practical. Validate any resulting return path against an allowlist or same-origin rule. Never redirect to an arbitrary URL supplied through `RelayState`; authentication does not make that parameter a safe redirect target.

# 4. Diagnose a wrong landing page

Suppose `/manager` login succeeds but Priya arrives at `/`.

1. Check whether the flow started at `/manager` or at the Okta tile.
2. If AcmeHR started it, inspect the saved request and outgoing and returning `RelayState`; check whether the browser kept the session cookie across the round trip.
3. If Okta started it, inspect the app's Default Relay State and AcmeHR's destination handling. There may be no original AcmeHR path to restore.
4. Confirm that the desired page is allowed for Priya after login. A correct destination cannot grant authorization.

A missing `RelayState` can explain lost navigation context. It does not, by itself, explain an invalid signature or fix a missing `InResponseTo` match.

# 5. Draw two sessions on the board

After successful login, the browser may hold a cookie for Okta's domain and a different cookie for AcmeHR's domain. Okta uses its session to decide whether Priya needs to authenticate again. AcmeHR uses its own application session to remember her authenticated state. The SAML response connects the login transaction; it is not the application session cookie.

| Action | What to inspect |
| --- | --- |
| Close AcmeHR's session only | AcmeHR should require login again; Okta may still sign Priya in without another credential prompt |
| End Okta's session only | A still-valid AcmeHR session may continue serving protected pages |
| Clear or block either browser cookie | Repeat the flow and inspect whether request state or an existing session is lost |

Use separate browser profiles or an isolated test account to observe this. A fresh prompt is evidence about the session and policy in that test, not proof of a universal timeout rule.

# 6. Say exactly what “logout” means

**Local logout** ends the AcmeHR session. A later visit can start a new SAML login, and an active Okta session may complete it without a password prompt. Ending Okta's own session does not automatically revoke every application session.

**SAML Single Logout** is an additional protocol exchange. In an SP-initiated SLO journey, the SP ends its local session and sends a `LogoutRequest` to the IdP; the IdP returns a `LogoutResponse`. In an IdP-initiated SLO journey, the IdP sends a `LogoutRequest` to a participating SP, which validates it, ends the matching local session, and returns a `LogoutResponse`. The request identifies the principal and can contain a `SessionIndex` associated with the login's authentication statement, helping target the right session. Verify signatures, issuer, destination, correlation and status as appropriate to the direction and binding.

Do not promise that SLO terminates every app session on every device. Okta's documented multi-app SLO depends on participation and browser front-channel communication, with specific behavior across Okta sessions. Distinguish “Okta session ended,” “AcmeHR session ended,” and “other app sessions ended” by checking each one.

Spring Security provides SAML SLO support when the IdP supports it and the registration, endpoints and SP signing credentials are configured. The current AcmeHR `SecurityConfig` calls `saml2Login` and `saml2Metadata`, but does not call `saml2Logout` or set up SLO credentials. Therefore **do not attempt to grade SLO as a working feature of this repo yet**. Spring's ordinary `/logout` behavior is a separate topic from configured SAML SLO; test actual session invalidation rather than inferring it from a button label.

# 7. A controlled investigation for the lab

Record one trace beginning at an AcmeHR protected URL and one beginning at the Okta tile. For each, write down the first URL, presence of `AuthnRequest`, request ID if present, ACS URL, `InResponseTo` if present, `RelayState` if present, final URL, and whether AcmeHR created a session. Mask assertions, cookies, personal attributes and tokens before sharing captures.

Then end only the AcmeHR session and retry. In a separate trial, end only the Okta session and revisit an AcmeHR page. Explain the observed behavior using the two-session model. Perform an SLO trial only after the lab explicitly configures and verifies both sides; record the request, response and which sessions actually end.

## Checkpoint

You are ready for the lab when you can explain why an Okta tile response has no SP request ID, why `RelayState` does not replace request correlation, how to distinguish a lost destination from failed authentication, and why “signed out” needs the name of the session that ended.

## Official references

- [Okta: Understanding SAML](https://developer.okta.com/docs/concepts/saml/) — initiation and `RelayState`.
- [Okta: SAML app settings API](https://developer.okta.com/okta-sdk-java/25.0.0/apidocs/apidocs/com/okta/sdk/resource/model/SamlApplicationSettingsSignOn.html) — Default Relay State.
- [Okta: Configure Single Logout](https://developer.okta.com/docs/guides/single-logout/saml2/main/) — participation and front-channel limits.
- [Spring Security: Producing AuthnRequests](https://docs.spring.io/spring-security/reference/servlet/saml2/login/authentication-requests.html) — request storage and session binding.
- [Spring Security: Performing Single Logout](https://docs.spring.io/spring-security/reference/servlet/saml2/logout.html) — SLO prerequisites and local logout distinction.
