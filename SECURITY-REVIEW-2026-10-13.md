# Security review — 2026-10-13

Prepared by i729. This review records tested source changes; it is not a certification of the deployed service.

## Change

Replace local default-password and client-side password-hash login with Firebase Authentication and server-assigned jclAdmin/jclMaster claims. Remove seeded administrator accounts, tracked debug signing key and personal metadata. Protect bot routes, add Firestore policies, and require private signing credentials for release builds.

## Validation

Bot authentication tests passed; real Firestore emulator profile/self-promotion/cross-account/admin cases passed. No Android APK was built.

## Dependency audit

Counts refer to package findings across the complete locked dependency tree, including tooling; they are not counts of distinct exploitable production vulnerabilities.

| Package project | Before | After | Remaining critical | Remaining high |
| --- | ---: | ---: | ---: | ---: |
| kiki-laundry-stock-app/wa-bot | 15 | 5 | 0 | 0 |

## Required before rollout

This is a staged authentication migration. Existing users need Firebase Auth accounts, matching UID profile documents and trusted claims before rollout. Reset any legacy default passwords and retire old password-hash fields. Android APK compilation/device testing and production Firebase policy checks remain outstanding. Previously published debug-signed APKs must not be treated as securely signed releases.

## Scope

Source and locally fetched Git branches/tags/available pull-request heads were inspected. Private production settings, actual database policies, deployed binaries, backup stores, and third-party service permissions were not changed. Current-source cleanup does not remove data from Git history, existing forks, cached copies, or previously downloaded assets. Sensitive values are intentionally omitted.
