# Privacy Policy — DRAFT (not published, pending review)

> Internal draft based on code inspection at Checkpoint 6B.2. Before publishing, check it against the
> Play Data Safety form and App Store privacy "nutrition label" definitions, and re-check it after
> any SDK change.

## Summary
RickAndMorty (TODO: final store name) is a fan-made catalogue app. It has no accounts and asks for no personal information.

## Data stored on your device
- **Favorites**: the characters you mark as favorite (public character data only: id, name, status,
  species, image URL, and similar fields).
- **Offline cache**: characters, locations and episodes downloaded from the public API, so the app
  can be used offline.
- **Image cache**: character images cached by the image loader.

This data stays on the device. It is not sent to the developer or to anyone else.

## Network requests
The app reads public content from `https://rickandmortyapi.com` (data and images). As with any
internet request, that third-party service receives your IP address and standard request headers.
The app does not send identifiers, favorites or other personal data in these requests. The API is
run by a third party under its own terms and privacy practices.

## Analytics, advertising and tracking
The app contains no analytics, crash-reporting, advertising or tracking SDKs, and it does not track
you across apps or websites.

## Permissions
- Android: `INTERNET` and `ACCESS_NETWORK_STATE`, used only to load content and to show offline status.
- iOS: no runtime permissions are requested.

## Backup
- Android: system backup is enabled (`allowBackup=true`), so app data, including favorites and the
  cache, may be included in your Google account backup under Android's backup rules.
- iOS: the database is stored in Application Support, which is included in iCloud/device backups by default.

## Deleting your data
Remove favorites in the app, or clear the app's storage, or uninstall the app. This deletes all
local data. The developer holds no copy of it.

## Children
The app does not knowingly collect personal information from anyone, including children.

## Contact
TODO: developer contact email before publishing.

## Changes
TODO: effective date / versioning.
