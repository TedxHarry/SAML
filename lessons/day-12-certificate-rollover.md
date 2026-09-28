# Day 12: Change a SAML Certificate Without Losing Login

## What you should understand by the end of today

Imagine Okta signs AcmeHR's SAML responses with certificate **A**. AcmeHR trusts A, so Priya can sign in. Okta's administrator now prepares certificate **B** because A is approaching its expiration date. If Okta starts signing with B while AcmeHR still trusts only A, a fresh login can fail signature validation. Priya's existing AcmeHR session might still look fine, so simply refreshing her home page would miss the problem.

Today's question is: **How do we make both sides ready, prove a new login works, and keep a way back if it fails?**

The current training SP does not yet offer a live A/B rollover control. This lesson explains the change and its evidence. The Day 12 lab must first prove any proposed overlap behavior in a controlled test before asking you to change an Okta app.

# 1. Name the certificate before changing anything

The word *certificate* can refer to different keys in this SAML connection. Start by asking who owns the private key and what the other side does with its public certificate.

| Certificate role | Private key owner | Who uses its public certificate? | What can break during a change? |
| --- | --- | --- | --- |
| Okta SAML response signing | Okta | AcmeHR verifies Okta's SAML signature | A new login fails signature validation if AcmeHR lacks the new trusted key |
| AcmeHR AuthnRequest signing | AcmeHR | Okta verifies signed requests | Okta rejects a request signed with a key it does not trust |
| AcmeHR Assertion decryption | AcmeHR | Okta encrypts Assertions to that public key | AcmeHR cannot decrypt an Assertion encrypted to a key it no longer has |
| HTTPS server certificate | Web server or TLS terminator | Browser checks the site's HTTPS connection | Browser or TLS connection fails for its own reasons |

**This lesson's A/B change is the first row: Okta's signing key for the AcmeHR app.** SP request-signing and decryption changes need their own plans. A file called `saml.crt` does not tell you which row it belongs to.

# 2. Record what A and B actually are

For each *public* certificate, record its role, app and environment, SHA-256 fingerprint, and `notBefore` and `notAfter` dates. You can inspect a local PEM file without uploading it to a decoder:

```bash
openssl x509 -in okta-signing-a.pem -noout -dates -fingerprint -sha256
```

Repeat with B. The fingerprint identifies the certificate you inspected; it does not prove which key signed a particular SAML response. A certificate's SHA-256 fingerprint, the algorithm used to sign the certificate, and the `SignatureMethod` inside SAML are three different pieces of evidence. Check the active certificate in the Okta app and a **fresh** SAML response when you need to know what Okta is using.

An approaching `notAfter` date is a reason to schedule the change. The exact failure at expiry depends on the application's validation rules, so test it rather than predicting every SP's behavior from the date alone. Okta exposes an app's available signing certificates under **Applications → Applications → your app → Sign On → SAML Signing Certificates**. Its [certificate management guide](https://help.okta.com/oie/en-us/content/topics/apps/manage-signing-certificates.htm) describes generating and activating a certificate; the listed certificates and the one currently active are different facts.

# 3. Walk through the A/B change

The goal is for AcmeHR to be ready for B **before** Okta first signs a response with B. A possible overlap looks like this:

| Stage | Okta signs new responses with | AcmeHR can verify with | What to prove |
| --- | --- | --- | --- |
| Baseline | A | A | A fresh SP-initiated login succeeds |
| Prepared | A | A and B | A still works; a controlled B-signed response also verifies |
| Switched | B | A and B | A fresh Okta login using B succeeds |
| Retired | B | B | B keeps working after A is removed from trust |

This is a **target sequence**, not a promise that every vendor supports it. A second certificate appearing in Okta's list does not prove it is active, that its metadata includes B, or that the SP can trust two keys at once. Inspect the actual metadata and AcmeHR registration. If the vendor only accepts one verification certificate, arrange a coordinated cutover and a tested rollback instead of claiming there will be an overlap. Okta's [rotation guidance](https://developer.okta.com/docs/guides/updating-saml-cert/main/) warns that activating a new signing certificate before the application accepts it can interrupt user access.

During overlap, a successful AcmeHR login alone cannot prove that Okta switched to B: AcmeHR also still trusts A. Check Okta's active certificate and, where the lab provides a local signature check, verify a fresh response against B's public certificate. Treat any certificate embedded in the SAML message as evidence to compare with the configured trust, not as a new trust source.

# 4. Know what this AcmeHR build can do today

In `lab-sp/src/main/java/com/acme/training/acmehr/security/SamlRelyingPartyConfig.java`, AcmeHR reads `IDP_METADATA_URL` while creating its registration and puts that registration in an `InMemoryRelyingPartyRegistrationRepository`. **This build loads that metadata when the application starts.** Refreshing the browser or changing Okta's certificate does not refresh the registration already in memory. A planned metadata change needs a controlled AcmeHR restart and a new-login test; each running instance must load the intended trust configuration.

Spring Security documents a [separate refreshable metadata repository](https://docs.spring.io/spring-security/reference/servlet/saml2/metadata.html). AcmeHR has not implemented that design. Its optional SP request-signing and decryption settings currently load one key pair for each role. Do not assume those SP-owned credentials overlap during their own rollover either.

For Day 12, a test fixture could prove that AcmeHR accepts A and B as trusted **IdP verification certificates** while Okta still signs with A. That result would establish local SP behavior. A later live trial must still check what the Okta app publishes and which key signs after activation. Keep private keys out of the repository and out of screenshots.

# 5. Prepare a production change before touching the active key

Use this order for the Okta signing-certificate change:

1. **Identify the two parties.** Record the Okta app, its active A fingerprint, the AcmeHR SP or vendor contact, the metadata URL, and the matching test and production environments. Do not carry a test app's certificate into production by accident.
2. **Capture the baseline.** In a fresh browser profile, complete a login that produces a new `SAMLResponse` POST to `/saml/acs`. Confirm that AcmeHR accepts it, then record a redacted trace and the certificate AcmeHR trusts. An existing session is not a rollover test.
3. **Prepare B without switching Okta.** Generate or obtain B in the intended non-production app. Inspect its public certificate and the app's metadata. Agree with the SP owner on how B will be trusted while A keeps working. Check that a metadata download really contains the keys you think it does.
4. **Prove the overlap if the design supports it.** In controlled tests, verify both A-signed and B-signed responses against the planned trust configuration. Also verify that an untrusted key fails. If overlap cannot be proved, document the cutover window and the exact rollback actions.
5. **Schedule and switch.** Confirm the owner, communication window, monitoring, and restore point. If overlap is supported, confirm every AcmeHR instance has loaded A and B before making B active in the non-production Okta app. If B cannot be loaded until Okta activates it, use the coordinated cutover and restart plan; call that a cutover, not overlap.
6. **Prove a fresh B login.** Start with a clean browser session, capture a new SAML transaction, and confirm B signed it as well as AcmeHR accepting it. Repeat the checks for the paths your app uses, including an Okta tile if that flow is enabled and tested.
7. **Retire A deliberately.** Keep A available only for the agreed overlap or rollback window. Remove its trust after B has passed the required tests and monitoring. Then confirm a fresh B login still succeeds.

Coordinate production separately after the non-production rehearsal. The plan should name who changes Okta, who changes AcmeHR or the vendor SP, who watches failures, and who can call a rollback. No one should have to infer the next action from a calendar invite.

# 6. Know when and how to go back

Before activating B, record how to make A active in Okta again and how to restore AcmeHR's known-good A trust. If B logins fail, stop the change, compare the active Okta certificate with AcmeHR's loaded trust, follow the agreed rollback, and prove a **new** A-signed login. Note the time, the affected environment, and the first failed validation step. A successful Okta authentication screen does not prove AcmeHR accepted the Response.

Rollback to A is only sensible if A remains trustworthy and usable. If A's private key is compromised, do not put it back into service for convenience. If A is expired or has already been removed everywhere, the response may require a coordinated emergency cutover or a maintenance window. Preserve the validation checks while the teams repair trust.

## Checkpoint

You are ready for the lab when you can identify the owner of A's private key, explain how AcmeHR can trust B before Okta switches (or why a coordinated cutover is needed), show what a fresh login proves, and name the exact point where you would roll back. You should also be able to explain why this version of AcmeHR needs a restart to load changed IdP metadata.

## Official references

- [Okta: Manage signing certificates](https://help.okta.com/oie/en-us/content/topics/apps/manage-signing-certificates.htm) — app certificates and activation.
- [Okta: Upgrade SAML apps to SHA256](https://developer.okta.com/docs/guides/updating-saml-cert/main/) — example activation and rollback sequence.
- [Spring Security: SAML 2.0 Metadata](https://docs.spring.io/spring-security/reference/servlet/saml2/metadata.html) — static and refreshable metadata approaches.
- [OpenSSL: `openssl-x509`](https://docs.openssl.org/3.5/man1/openssl-x509/) — local certificate date and fingerprint options.
