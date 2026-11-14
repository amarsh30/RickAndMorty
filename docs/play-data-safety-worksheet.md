# Google Play Data Safety — Answer Worksheet (Android) — DRAFT

> Internal preparation only. **Not submitted.** Based on code at HEAD `d13fbe1` + Checkpoint 6D.2.
> Answers marked **REVIEW** need a developer decision against the current Play Console
> definitions before submission. This worksheet covers Android only. The iOS privacy manifest
> (`iosApp/iosApp/PrivacyInfo.xcprivacy`) is a separate declaration with its own rules and must
> be kept consistent with the decisions made here.

## Definitions used (summary; confirm against Play Console Help before submitting)

- **Collected:** data transmitted off the user's device by the app (to the developer or a third
  party). Data processed only on the device is not "collected".
- **Shared:** user data transferred to a third party. Play lists exemptions, including transfers
  to a service provider acting on the developer's behalf and transfers the user initiates
  themselves (for example, the user chooses to send data to a third party). Whether any of them
  applies here is a judgement for the developer.
- **Ephemeral processing:** data used only in memory to serve the request and not retained.
  Only the receiving party can guarantee this.
- **Optional:** the user can choose whether the data is provided and still use the app.

## Facts from the code (evidence)

| Fact | Evidence |
|---|---|
| Only network host: `https://rickandmortyapi.com/api/` (third party, not developer-operated) | `core/network/.../ApiConfig.kt` (`BASE_URL`); images come from URLs returned by the API, loaded by Coil (`coil-network-ktor3`) |
| Search sends `name`, `status`, `species`, `gender` as query parameters | `core/data/.../remote/datasource/CharacterRemoteDataSourceImpl.kt` lines 33–36 |
| Other requests carry only `page` or resource ids | `CharacterRemoteDataSourceImpl.kt:19`, `LocationRemoteDataSource.kt:23`, `EpisodeRemoteDataSource.kt:23` |
| No custom headers, no identifiers, no logging plugin | `core/network/.../HttpClientFactory.kt` |
| No analytics, crash reporting, ads, attribution, auth or developer backend SDKs | `gradle/libs.versions.toml` (Compose, Ktor/OkHttp, Room/SQLite, Koin, Coil, kotlinx, navigation only) |
| No accounts, no advertising ID, no device ID read | No such APIs in the codebase |
| Permissions: `INTERNET`, `ACCESS_NETWORK_STATE` only | `androidApp/src/main/AndroidManifest.xml` and merged release manifest |
| Local data: one Room database (favorites + public API cache); Coil image cache in `cacheDir` | `core/database/.../DatabaseBuilder.android.kt`; on-device listing at 6D.2 |
| Backup: database file + `-wal` only (cloud backup and device transfer) | `androidApp/src/main/res/xml/data_extraction_rules.xml`, `backup_rules.xml` |
| Requests use HTTPS for the API base URL | `ApiConfig.BASE_URL`. Image URLs come from the API's data: not enforced by the app (see open items) |

## Section 1 — Data collection and security

| Question | Proposed answer | Status |
|---|---|---|
| Does your app collect or share any of the required user data types? | Depends on the search-terms decision below. If search terms are declared → **Yes**; if judged exempt / not user data → **No** | **REVIEW** (developer) |
| Is all user data encrypted in transit? | **Yes, likely**: the API base URL is HTTPS. Not proven for every image URL (the app does not enforce HTTPS). | **REVIEW**: verify image URLs, or add enforcement before answering Yes |
| Do you provide a way for users to request that their data is deleted? | No developer-held data exists. Local data is deleted by removing favorites, clearing app storage, or uninstalling. Choose the Play option that matches "no data collected by developer" or describe local deletion. | **REVIEW** (wording depends on Section 1 answer) |
| Independent security review | **No** (none performed) | Final unless a review is done |
| Committed to Play Families Policy | Depends on target audience | **REVIEW** (developer) |

## Section 2 — Data types

| Play data type | Collected? | Shared? | Evidence / reasoning | Status |
|---|---|---|---|---|
| **App activity → In-app search history** | **Candidate: Yes** | **Candidate: Yes** (to `rickandmortyapi.com`, third party) | User-typed name/species text and chosen status/gender filters leave the device in search requests. The developer receives nothing; the third party receives it to answer the request. Whether this counts as "collection/sharing", or falls under an exemption (user-initiated, ephemeral processing), is a policy judgement. How the API operator logs or retains requests is unknown. | **REVIEW (main decision).** Conservative option: declare *collected + shared*, purpose **App functionality**, **optional** (search is optional), **not** processed ephemerally (cannot be guaranteed for a third party). |
| Device or other IDs | No | No | No device, advertising or installation ID is read or sent | Final (re-check if SDKs change) |
| IP address (part of every request) | Not declared as a data type by itself in current Play categories | — | Received by the API as part of any network request; the app does not read or send it deliberately | **REVIEW** against current Play guidance on network metadata |
| Location (approximate / precise) | No | No | No location permission or API; IP-based location by the third party is not done by the app | Final |
| Personal info (name, email, user IDs, address, phone, etc.) | No | No | No accounts or forms collecting personal data | Final |
| Financial info | No | No | — | Final |
| Health and fitness | No | No | — | Final |
| Messages | No | No | — | Final |
| Photos and videos | No | No | Images are downloaded public API content, not user media | Final |
| Audio | No | No | — | Final |
| Files and docs | No | No | — | Final |
| Calendar / Contacts | No | No | — | Final |
| App activity → App interactions, other user-generated content, installed apps, other actions | No | No | Favorites stay on the device (not transmitted); no interaction tracking | Final |
| Web browsing | No | No | — | Final |
| App info and performance → Crash logs, Diagnostics, Other | No | No | No crash/diagnostics SDK; nothing transmitted | Final |

Notes:
- **Favorites are not "collected":** they are stored only on the device. Android backup
  (Google account) is a system feature that Play does not count as the app collecting data.
  Re-check this against current guidance.
- If any SDK, analytics, crash reporting or developer backend is added later, this worksheet
  must be redone.

## Open items for the developer

1. **Search terms:** declare as collected and shared (conservative), or document why an exemption applies.
2. **Encryption in transit:** confirm that all image URLs the API returns are HTTPS, or decide
   whether to enforce HTTPS in the app (e.g. a network security config), before answering "Yes".
3. **Deletion wording:** pick the Play option consistent with "no developer-held data".
4. **Target audience / Families policy.**
5. **Privacy policy URL** (required in Play Console) and the identity and contact details in
   `docs/privacy-policy-draft.md`.
6. **Consistency with iOS:** if search terms are declared on Android, review `PrivacyInfo.xcprivacy`
   (`NSPrivacyCollectedDataTypes`, currently empty) on a Mac. Do not copy answers blindly; the
   two platforms use different definitions.
