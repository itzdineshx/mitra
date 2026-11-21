# Privacy — what Mitra does with your data

> **Draft for the first release.** It describes how Mitra is designed (see `docs/DATA_SPEC.md`). Each promise becomes true as its task is built and tested; review this text, with a lawyer if you can, before the first public release (T27). It is not legal advice.

**Short version: your voice, your screens and your history stay on your phone.**

## What Mitra needs and why
- **Microphone:** to hear you. Speech is turned into text on the phone. Audio is not saved.
- **Accessibility access:** to read the current screen and tap, type and scroll for you. Mitra never reads or types passwords, PINs or OTPs, and never controls banking or UPI apps.
- **Notifications and "Show over apps":** to show what Mitra is doing and the Stop button.
- **Internet:** only to download the AI model, to use cloud help if you turn it on (it asks every time), and, in the `direct` build, to send a family approval request if you link a helper.

## What Mitra stores (on the phone only)
- Your tasks: what you said, what Mitra did, and the result. Steps are kept 90 days.
- Settings, the list of apps and websites you allowed, standing rules you set, and details you asked Mitra to remember.
- Not stored: audio, screenshots, passwords, OTPs, card or UPI numbers, or full message texts (only a short preview and a hash for the audit trail).
- The database is encrypted with a key kept in the Android Keystore. It is excluded from cloud backups and phone-to-phone transfers.

## What leaves the phone
- Nothing, by default. No account, no ads, no analytics, no crash reports.
- If you turn on cloud help, the text of one task may be sent to the cloud model you chose, only after you approve that task.
- Family approval (optional, `direct` build): a short approval request goes through Firebase to your linked family member. The `foss` build uses UnifiedPush or the same phone and has no Firebase.

## Your control
- See everything in **History**. Swipe a task to delete it.
- **Setup › Privacy › Delete all Mitra data** removes the history, rules, memory, plugins and settings, and optionally the AI model.
- Turn off "Hey Mitra", cloud help or any permission at any time. Android's own Settings also lets you remove the Accessibility access.

## Safety promises
Sending, submitting and deleting always ask you first. Text on web pages, in messages and in mails is treated as data and never as an instruction. Details: `docs/TRUST_ENGINE_SPEC.md`.

Contact: see `SECURITY.md`.
