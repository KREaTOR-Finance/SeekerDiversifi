# Diversifi Security Audit (2026-03-12)

## Scope
- Android app code under `app/src/main/java`
- Wallet connect/sign path, swap payload path, RPC path, local storage
- Build/manifest configuration relevant to production security

## Security Level
- **Current level:** **Level 2 - Developing**
- **Score:** **6.4 / 10**
- **Interpretation:** Suitable for controlled testing with low-value wallets; not yet hardening-complete for high-value production use.

## Findings

### DIV-SEC-001 (High): Swap transaction payload is not independently validated before signing
**Evidence**
- [SessionViewModel.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/presentation/viewmodel/SessionViewModel.kt:505)
- [JupiterClient.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/solana/rpc/JupiterClient.kt:64)

**Issue**
- The app signs and submits the base64 swap transaction returned by Jupiter without decoding and enforcing local invariants (expected input/output mints, expected user accounts, maximum allowed slippage/fees, blockhash freshness).

**Risk**
- If upstream response content is unexpected or compromised, users can be prompted to sign transactions that do not match intended constraints.

**Recommendation**
- Decode transaction message pre-sign.
- Enforce explicit constraints: payer, token program accounts, allowed program IDs, output mint, min out amount, max fee/slippage, recent blockhash age.
- Fail closed on any mismatch.

---

### DIV-SEC-002 (High): Signing account binding is not enforced
**Evidence**
- [SeedVaultManager.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/solana/seedvault/SeedVaultManager.kt:96)
- [SeedVaultManager.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/solana/seedvault/SeedVaultManager.kt:111)

**Issue**
- `signTransaction` accepts an `account` parameter but does not enforce that the active authorized account used by wallet adapter matches that address.

**Risk**
- User may sign from a different wallet account than expected if wallet default/account selection changes.

**Recommendation**
- Bind authorization to the selected account (address-scoped authorize/reauthorize where supported).
- Validate returned authorized account equals requested `account.publicKey` before signing.
- Surface a blocking mismatch error.

---

### DIV-SEC-003 (Medium): Wallet auth token and app settings are stored in plain SharedPreferences
**Evidence**
- [SeedVaultManager.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/solana/seedvault/SeedVaultManager.kt:20)
- [AppModule.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/core/di/AppModule.kt:68)
- [SettingsRepository.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/data/repository/SettingsRepository.kt:9)

**Issue**
- Sensitive wallet session token (`auth_token`) and configuration values are persisted unencrypted.

**Risk**
- On compromised/rooted devices, token/session data is easier to extract.

**Recommendation**
- Migrate to `EncryptedSharedPreferences` + `MasterKey` for token and sensitive settings.
- Rotate/clear existing unencrypted prefs during migration.

---

### DIV-SEC-004 (Medium): Raw transaction payloads and puzzle answers are stored unencrypted in Room
**Evidence**
- [RoomEntities.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/data/local/entity/RoomEntities.kt:32)
- [RoomEntities.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/data/local/entity/RoomEntities.kt:39)
- [SessionViewModel.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/presentation/viewmodel/SessionViewModel.kt:220)

**Issue**
- `rawData` (serialized transaction) and `puzzleAnswer` are persisted in plaintext local DB.

**Risk**
- Local compromise exposes transaction context and challenge answers; not ideal for secure-by-default posture.

**Recommendation**
- Store only minimum needed fields for resume.
- Remove persisted puzzle answers.
- Encrypt local DB or sensitive columns if persistence is required.

---

### DIV-SEC-005 (Medium): Session progresses without confirmed on-chain finality
**Evidence**
- [SessionViewModel.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/presentation/viewmodel/SessionViewModel.kt:203)
- [SessionViewModel.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/presentation/viewmodel/SessionViewModel.kt:238)

**Issue**
- Flow increments progress and can complete session after obtaining signature bytes, while confirmation status is only opportunistically checked once.

**Risk**
- UI/session may report success while transactions are still pending/failed, creating operational and user-risk ambiguity.

**Recommendation**
- Add explicit confirmation polling with timeout/backoff.
- Gate progression (or mark as failed/retry) based on confirmation result.

---

### DIV-SEC-006 (Low): Broad token input acceptance increases user-footgun risk
**Evidence**
- [SessionViewModel.kt](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/java/com/diversify/presentation/viewmodel/SessionViewModel.kt:518)

**Issue**
- Any base58-like mint address is accepted as supported token input.

**Risk**
- Users can accidentally select unsafe/illiquid/malicious mints.

**Recommendation**
- Keep curated allowlist by default.
- Add advanced mode toggle and warning for custom mint addresses.

## Positive Controls Observed
- [AndroidManifest.xml](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/AndroidManifest.xml:10): `allowBackup=false`
- [AndroidManifest.xml](c:/Users/Buidl/Desktop/SeekerDiversifi/app/src/main/AndroidManifest.xml:4): internet permission only; no broad storage permissions
- [app/build.gradle](c:/Users/Buidl/Desktop/SeekerDiversifi/app/build.gradle:31): release minification enabled
- Mainnet endpoints use HTTPS

## Priority Remediation Plan
1. Implement payload verification and account binding (DIV-SEC-001, DIV-SEC-002).
2. Encrypt auth/session settings and reduce sensitive local persistence (DIV-SEC-003, DIV-SEC-004).
3. Add confirmation-gated progression and retry/fail states (DIV-SEC-005).
4. Add guarded custom-mint UX (DIV-SEC-006).

## Release Gate Recommendation
- **Do not mark wallet-safe production for high-value use until DIV-SEC-001 and DIV-SEC-002 are fixed.**
- Current build is acceptable for controlled low-funds testing.
