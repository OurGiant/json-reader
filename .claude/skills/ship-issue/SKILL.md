---
name: ship-issue
description: The standard workflow for shipping a bug fix or feature to JSON Viewer — file a GitHub issue, branch off main, implement, verify, bump the patch version, and open a PR. Use whenever picking up a bug fix or feature for this repo.
---

# Shipping a change to JSON Viewer

Follow `java-swing-ship-issue` (the generic workflow shared across the Java
Swing project family) with these JSON Viewer specifics:

- **Project path**: `/projects/OHI/json-reader` inside the build container
  (not `/projects/json-reader` — the mount has an `OHI/` subdirectory,
  confirmed 2026-08-08, issue #29); build container name and bind-mount
  gotchas are in this repo's own `.claude/skills/verify/SKILL.md`.
- **Verify**: use this repo's own `.claude/skills/verify/SKILL.md` for
  build/launch mechanics and this project's confirmed environment gotchas.
- **Untrusted-input surfaces**: this app formats/lints/tokenizes arbitrary
  pasted or opened JSON text. A change to `core/JsonProcessor` is a good
  candidate for updating README's Hardening section alongside the code
  (size cap, nesting-depth cap, malformed-input handling), matching this
  project's existing practice — not just implementing it silently. See
  `SPEC.md` for why JSON parsing is hand-rolled rather than Jackson-based;
  revisit that decision explicitly (don't silently drift from it) if a
  change would otherwise pull in a JSON library.
- **Package layout**: `gui/` depends one-way on `core/`/`model/`; neither
  of those may import `javax.swing.*`. If a fix needs new pure logic,
  put it in `core/` (or `model/` for a plain data type) so it stays
  unit-testable — don't grow `gui/MainWindow` back into a god-class.
- No repo-specific branch-naming or extra PR-checklist step beyond the
  generic workflow has been established here yet; follow
  `java-swing-ship-issue` as-is until one is.
