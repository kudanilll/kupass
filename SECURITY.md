# Security policy

## Supported versions

Security fixes go into the latest release on [GitHub Releases](https://github.com/kudanilll/kupass/releases). Before reporting, please check that the problem still happens on the latest release or on `master`.

## Reporting a vulnerability

Report vulnerabilities privately through GitHub: open the repository's [Security tab](https://github.com/kudanilll/kupass/security) and choose "Report a vulnerability", or go straight to the [advisory form](https://github.com/kudanilll/kupass/security/advisories/new). Please don't open a public issue, discussion, or pull request about a vulnerability.

A useful report includes:

- the Kupass version (Settings > About > Kupass) and where you got the app
- the Android version and device
- the steps to reproduce, and what an attacker can do with the problem

Don't send real passwords, vault contents, backup files, or CSV exports. Made-up test data is enough.

The maintainer replies on the advisory, fixes the problem, and publishes the advisory once a fixed release is out. Say in your report if you would like to be credited.

## Scope

Kupass keeps everything on the device, so these are the reports that matter most:

- reading vault data without the device's Keystore key, or weaknesses in the field encryption or the `.kupass` backup format
- opening an existing account or exporting the vault without passing the fingerprint, face, or screen-lock prompt
- vault data leaking into logs, the clipboard, Android backups, other apps, or network requests
- the site icon lookup sending anything other than a public domain name

## Known limitations

These are documented design decisions. Reports that only restate them will be closed.

- The vault key is not bound to user authentication in the Android Keystore, because binding it can lock people out after they change their fingerprints or face unlock. The authentication prompt protects the app's screens, not the key, so someone in control of an unlocked, compromised device can use the key.
- Browsing the vault list, searching, creating entries, importing, and deleting an entry after a confirmation don't ask for authentication. Opening an existing account and exporting do.
- To show site icons, Kupass sends saved domains to the Favget icon service. Favget and the CDN that serves the images can see which domains you have saved. When a site blocks Favget, the Favget server may ask Google's favicon service, so Google can see that domain as well (the request comes from the server, not from your phone).
- To open quickly, Kupass keeps the decrypted vault in memory while the app runs, and an encrypted copy of it in app storage that is never backed up. That copy is sealed with a key wrapped by the vault's Keystore key, so it is no easier to read than the database. Site icons are cached the same way, so the cache doesn't reveal which sites you have saved.
- The vault key is never rotated.
- The 45-second clipboard timer does not survive Android killing the app process.
