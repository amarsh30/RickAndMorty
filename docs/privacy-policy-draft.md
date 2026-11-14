# Privacy Policy — DRAFT (not published, pending review)

> Internal draft. Last checked against the code at Checkpoint 6D.2 (Android) and 6B.2 (iOS).
> Every `[TODO: …]` must be completed by the developer before publishing. Re-check this text
> whenever a dependency, permission, network endpoint or stored data type changes.

**App:** [TODO: final app name]
**Developer:** [TODO: developer or legal entity name]
**Contact:** [TODO: contact email]
**Policy URL:** [TODO: public URL where this policy is hosted]
**Effective date:** [TODO: date]

## Summary

[TODO: app name] is a fan-made catalogue of the Rick and Morty characters, locations and
episodes, using the public Rick and Morty API. It has no user accounts, and it contains no
analytics, advertising or crash-reporting tools. The developer does not run any server for
this app and does not receive data from it.

## Data stored on your device

- **Favorites:** the characters you mark as favorites. This copies the character's public
  information (such as name, status, species and image address) into the app's local database.
- **Offline cache:** characters, locations, episodes and search results downloaded from the
  Rick and Morty API, so the app can show them without a connection.
- **Image cache:** character images, kept in the app's temporary cache.

This data stays on your device. The developer cannot access it.

## Data sent to a third-party service

To show content, the app connects to the **Rick and Morty API** (`rickandmortyapi.com`), a
public service run by a third party, not by the developer. The app sends it:

- **Requests for content:** lists and details of characters, locations and episodes, and the
  character images.
- **Your searches:** when you search, the name and species text you type and any status or
  gender filters you choose are sent to the API as part of the request.
- **Basic connection information:** like any website or app, the service receives your IP
  address and standard technical request details (for example, the network library's version).

The app does not send an account, advertising ID, device ID, your favorites or your location.
The Rick and Morty API has its own terms and privacy practices, which the developer does not
control. [TODO: decide whether to link to the API's own terms or privacy information.]

## Analytics, advertising and tracking

The app does not include analytics, advertising, attribution or crash-reporting tools, and it
does not track you across other apps or websites.

## Permissions (Android)

- `INTERNET`: to load content from the Rick and Morty API.
- `ACCESS_NETWORK_STATE`: to show when you are offline and to retry when the connection returns.

The app requests no runtime permissions such as location, camera, contacts or storage.

## Backup and device transfer (Android)

If Android backup or device-to-device transfer is turned on for your device, the app's local
database may be included. It contains your favorites and the offline cache of public API
data, so your favorites can be restored on a new or reset device. Images and other temporary
files are not included. Backups are handled by Android and your backup provider (for example,
your Google account), not by the developer.

## Backup (iOS)

On iOS, the local database is stored in the app's Application Support folder, which iOS
includes in device and iCloud backups by default.

## Deleting your data

- Remove a favorite in the app at any time.
- Clear the app's storage in your device settings, or uninstall the app, to delete all of its
  local data.
- The developer holds no copy of your data, so there is nothing to request from the developer.
- Data already included in a device backup is managed through your device's backup settings.

## Children

[TODO: developer statement about the intended audience and children.]

## Changes to this policy

[TODO: how changes will be announced, e.g. by updating this page and its effective date.]

## Contact

[TODO: contact email or other contact method.]
