# Day 12 Lab: Rehearse an Okta Signing Certificate Change

## What you are going to do

Okta signs AcmeHR's SAML messages with certificate A. You will identify A, inspect the metadata AcmeHR uses, and write the checks for adding certificate B. Then you will use a controlled A/B test to decide whether a non-production Okta cutover can proceed.

**The A/B test and a live overlap control are still being built in this repository.** You can complete Parts 1–3 now. Parts 4–5 are gates for the subsequent implementation and a supervised non-production trial. Do not mark them passed from this guide alone, and do not activate B in Okta until the relevant gate is satisfied.

This lab changes **Okta's IdP signing certificate for AcmeHR**. It does not rotate AcmeHR's request-signing or Assertion-decryption keys, or the site's HTTPS certificate. Read the [Day 12 lesson](../../lessons/day-12-certificate-rollover.md) first.

# Part 1: Capture a fresh baseline while A is active

Use the working AcmeHR configuration from Day 11, your assigned non-production Okta app and test user, Docker Compose, a browser with Developer Tools, Python 3, and OpenSSL. From `lab-sp`, start the app with `docker compose up --build` and check that `http://localhost:8000/actuator/health` reports `UP`. Keep the existing `IDP_METADATA_URL` value; do not paste a tenant URL into this repository.

Close all private windows in your browser, open a new one, and request `http://localhost:8000/protected`. Preserve the Network log. Complete a login and find a **new** `SAMLResponse` POST to `/saml/acs`. Confirm that `/protected` shows `SAML PASS`. An already-open AcmeHR page proves only that an earlier session still exists.

In Okta Admin Console, open **Applications → Applications → your non-production app → Sign On → SAML Signing Certificates**. Identify the certificate marked active for *this app*. Call it A. Save its public certificate locally as `okta-signing-a.pem`, then inspect it:

```bash
openssl x509 -in okta-signing-a.pem -noout -dates -fingerprint -sha256
```

Record the exact SHA-256 fingerprint and validity dates. Keep the private key with Okta; you need only public certificates in this lab. The certificate's SHA-256 fingerprint is its identifier here, not proof of which key signed a particular Response.

| Baseline evidence | Your observation |
| --- | --- |
| Okta app and environment |  |
| A marked active for that app? |  |
| A SHA-256 fingerprint and `notAfter` |  |
| Fresh `/saml/acs` POST and `/protected` result |  |
| AcmeHR instance and startup time |  |

# Part 2: Inspect AcmeHR's loaded trust source

Record the configured `IDP_METADATA_URL` in your private notes. Rerun the local metadata fingerprint helper from [Day 8, Part 5](../day-08/README.md#part-5-inspect-the-certificates-published-by-metadata) against that URL. Its output lists public certificates in signing-capable `KeyDescriptor` entries. Compare the fingerprints with A. If they differ, first check that you have the correct app, environment, and metadata endpoint. Do not assume that every certificate in Okta's list appears in this metadata, or that the first certificate in metadata is the active signer.

Inspect `lab-sp/src/main/java/com/acme/training/acmehr/security/SamlRelyingPartyConfig.java`. It calls `RelyingPartyRegistrations.fromMetadataLocation(...)` while constructing an `InMemoryRelyingPartyRegistrationRepository`. The current AcmeHR process keeps the registration it built at startup. Record which metadata was available when this instance started. A changed Okta page or a browser refresh does not update that process's trusted credentials; a deliberate restart and fresh login check are required to load changed metadata.

| Trust-source evidence | Your observation |
| --- | --- |
| Metadata endpoint and app/environment match? |  |
| Signing-capable metadata fingerprints |  |
| A fingerprint present? |  |
| B fingerprint present? If so, where was B obtained? |  |
| Number of AcmeHR instances that would need updated trust |  |

You may inspect a public B certificate if one has already been prepared for the correct non-production app. Save it separately as `okta-signing-b.pem` and run the same `openssl x509` command with that filename. Seeing B in the Okta certificate list does **not** mean B is active or included in the metadata AcmeHR loaded.

# Part 3: Write the proposed change record

Before anyone changes the active signer, fill in the owners and evidence. Write “unknown” where an answer needs a vendor or administrator. Never put a private key, raw SAML response, cookie, MFA code, or unmasked identity data in the record.

| Question | Proposed answer |
| --- | --- |
| Who activates B in this Okta app? |  |
| Who provides B's public certificate or metadata to the SP owner? |  |
| How will AcmeHR trust A and B at the same time, if supported? |  |
| How will every running SP instance load the intended trust? |  |
| Who checks fresh login failures, and during what window? |  |
| How will you identify B as the actual signer after activation? |  |
| Who can restore A, and is A still safe and usable? |  |
| When can A be removed from SP trust? |  |

If the Okta metadata contains only the active A, merely generating B will not establish overlap in this build. Arrange a tested way to load both trusted public certificates, or plan an explicit coordinated cutover with a downtime and rollback window. Do not treat a planned restart as automatic metadata refresh.

# Part 4: Controlled A/B test gate

When the repository includes a focused rollover test, run it against disposable test keys and the same registration path that AcmeHR will use. The test must produce freshly signed SAML messages. The Day 8 wrong-certificate test already shows rejection for one mismatched key; it does not establish the A+B overlap below.

| SP IdP verification trust | Message signer | Expected result | Observed test result |
| --- | --- | --- | --- |
| A | A | Accept |  |
| A | B | Reject invalid signature |  |
| A and B | A | Accept |  |
| A and B | B | Accept |  |
| B | A | Reject invalid signature |  |
| B | B | Accept |  |
| A and B | Untrusted C | Reject invalid signature |  |

Keep issuer, audience, destination, subject confirmation, request correlation, and time conditions valid in each trial so a result isolates signature trust. Record which public keys the registration actually loaded. A configured A+B list with no B-signed acceptance test is an incomplete result. A certificate copied from `KeyInfo` inside a message must never become trusted merely because the message supplies it.

**Gate:** Until these cases pass for the intended configuration, stop before changing Okta's active certificate. If the SP or vendor cannot accept two IdP verification keys, document a coordinated cutover and rehearse both failure and rollback under that design instead. A test with disposable keys establishes AcmeHR behavior; it does not prove what the live Okta app publishes.

# Part 5: Supervised non-production change, after the gate

Use the approved change record and the actual metadata behavior observed in Part 2:

1. Confirm a fresh login works while A is active and save a redacted baseline. Confirm the active A fingerprint and the A/B test result.
2. Prepare B for the correct Okta app. If an overlap is possible, load A and B into every AcmeHR instance **before** making B active. Restart instances when their metadata or registration input changes, then verify A still works. If B cannot be loaded before activation, follow the coordinated cutover plan instead.
3. Have the Okta owner activate B in the non-production app during the agreed window. Close all private browser windows, start a new one, and make a new `/protected` login. Confirm a new ACS POST and an accepted AcmeHR session. Verify B is active in Okta and establish that B signed the **new** SAML message using a trusted local signature check or a B-only validation in a controlled environment. An embedded certificate fingerprint alone is not signature verification.
4. If login fails, stop the change. Compare the active signer, the public certificate fingerprints, the trust loaded by each AcmeHR instance, and the first validation failure. Restore A only if it remains safe and usable; restore the known-good SP trust, then prove a **fresh** login while A is active. Do not weaken SAML validation to make the login pass.
5. After B succeeds for the required paths and the agreed monitoring window, remove A from SP verification trust and restart affected instances. Prove a new B-signed login again. Record who retired A and when. If an Okta tile is part of your deployment, test that path separately; Day 11 did not establish its live acceptance for every tenant.

| Cutover evidence | Your observation |
| --- | --- |
| Gate passed or approved coordinated cutover plan |  |
| A login after loading proposed trust |  |
| B active in the correct Okta app |  |
| Fresh message signed by B; method of proof |  |
| Fresh AcmeHR login accepted on each instance |  |
| A removed from trust; B login still works |  |
| Rollback performed, if needed; fresh A login |  |

For production, repeat the approval, owner, instance, monitoring, and rollback planning against the **production** app and its own certificate fingerprints. The non-production result does not identify production's keys or prove its instances were updated. If A is compromised or unusable, do not reactivate it; use the emergency cutover plan from the lesson.

## What counts as complete today

Parts 1–3 produce a baseline and a reviewable change record. The complete Day 12 rollover claim requires passing Part 4 and recording the observed Part 5 cutover and retirement in an authorized non-production environment. If access or implementation is missing, leave those rows open and name the missing gate. Keep only redacted notes; restore the original Okta signing state after any supervised rehearsal that did not proceed to B retirement.

## Official references

- [Okta: Manage signing certificates](https://help.okta.com/oie/en-us/content/topics/apps/manage-signing-certificates.htm) — available and active certificates for an app.
- [Okta: Upgrade SAML apps to SHA256](https://developer.okta.com/docs/guides/updating-saml-cert/main/) — activation and revert example; it warns of interrupted access when the application lacks the new certificate.
- [Spring Security: SAML 2.0 Metadata](https://docs.spring.io/spring-security/reference/servlet/saml2/metadata.html) — metadata registration and a separate refreshable repository design.
- [OpenSSL: `openssl-x509`](https://docs.openssl.org/3.5/man1/openssl-x509/) — certificate dates and SHA-256 fingerprint options.
