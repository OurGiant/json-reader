---
name: verify
description: How to build, launch, and drive JSON Viewer to verify a Swing UI change actually works on this dev setup. Use before trusting the generic verify-java-swing skill's screenshot step; read that skill too for the underlying techniques (modal-dialog deadlock, synthetic MouseEvent dispatch, process safety).
---

# Verifying JSON Viewer

This is the project-specific companion to the generic `verify-java-swing`
skill (techniques) and `java-swing-project-setup` (build/structure standard
this project follows). Read those first — this file is what to actually
type for *this* project.

## Build here, run there

Maven only exists in the Docker container, not on the host:

```bash
docker exec festive_bardeen bash -c "cd /projects/OHI/json-reader && mvn -q package -DskipTests"
```

If `festive_bardeen` doesn't respond, find the current container:
`docker ps -a --format '{{.Names}} {{.Status}} {{.Image}}'` and
`docker start <name>` if stopped — the name can drift across sessions.

`/projects` is bind-mounted from the host's `~/projects`, which has an
`OHI/` subdirectory this repo lives under — confirmed 2026-08-08 (issue
#29) after `/projects/json-reader` (this file's path until then) turned
up "No such file or directory"; don't assume the mount lands directly on
the repo without checking `docker exec festive_bardeen ls /projects/` if
this ever 404s again. The jar lands at `target/json-viewer-all.jar`,
visible on the host. The container is
headless (no `DISPLAY`) — run the jar on the **host**, not inside the
container, or it dies at `JFrame` construction with `HeadlessException`.

```bash
java -jar target/json-viewer-all.jar
```

Main class: `com.ourgiant.utilities.Main`.

## Confirmed: bind-mount staleness hits both `pom.xml` edits and new files under `src/main/resources`

Not just a theoretical risk from the generic skill — hit this directly while
shipping issue #5: after editing `pom.xml`'s `<version>` and adding
`src/main/resources/version.properties`, the container's bind-mounted view
of both was stale (`docker exec festive_bardeen ls src/main/resources`
showed an empty directory; `grep '<version>' pom.xml` in the container
showed the old value). Confirm with a `grep`/`ls` inside the container
before trusting a build result whenever you've just added a new file or
edited `pom.xml` specifically — force a sync with:

```bash
docker cp src/main/resources festive_bardeen:/projects/OHI/json-reader/src/main/
docker cp pom.xml festive_bardeen:/projects/OHI/json-reader/pom.xml
```

## Stale compiled classes can mask a real compile error

Running `mvn test` without `clean` after changing a method signature (e.g.
adding a checked exception) can silently reuse stale `target/classes`/
`target/test-classes` from before the signature change, reporting tests as
passing when a real compile error exists. If a change touches a method
signature, run `mvn clean test` — not just `mvn test` — before trusting a
green result.

## Screenshots: Robot works here, but check for a locked session first

`java.awt.Robot.createScreenCapture(...)` returns genuine, correct pixel
content on this host's display (`:1`) — confirmed by both sampling average
RGB values and visually inspecting captured PNGs of the running app. But
this is a **shared, real desktop session** that can be screen-locked
(KDE's `kscreenlocker_greet`) independently of anything the app is doing.
If a capture comes back looking like a lock-screen wallpaper/clock instead
of the app, check for a lock screen before assuming the app or the capture
technique is broken:

```bash
DISPLAY=:1 xwininfo -root -tree | grep -i kscreenlocker
```

If the session is locked, fall back to non-visual verification (reflection
into `StyledDocument` character attributes, `getText()`, log output) per
`verify-java-swing` — don't attempt to unlock someone else's session.

## Inserting very large text into the `JTextPane` is slow, independent of app logic

Setting several megabytes of text into the app's `JTextPane` via
`setText(...)` can take much longer than a typical test timeout — this is
`StyledDocument`'s own scaling characteristic, not a bug in
`core/JsonProcessor`'s size-cap logic. Don't chase this as if it were an app
hang; verify size-cap behavior directly against `JsonProcessor` instead
(already covered by `JsonProcessorTest`), and reserve live-UI verification
for input sizes a real user would actually paste.

## First-run state: the update-check dedup preference

`AppPreferences` (added for the About/update-check feature, issue #20)
persists the last-notified update version via `java.util.prefs`, backed by
a platform store outside the repo (`~/.java/.userPrefs/...` on Linux) —
not a file under `~/.json-viewer/`. `mvn test` doesn't exercise it (no
`AppPreferencesTest`, mirroring doc-scrubber's minimal version of the same
class), so no surefire system-property redirect is needed yet. If a test
starts exercising it, redirect via a system property rather than letting
`mvn test` read/write the real developer's prefs node — see
`java-swing-project-setup` §7.

Logging still only writes to `~/.json-viewer/logs/` (see `logback.xml`).
