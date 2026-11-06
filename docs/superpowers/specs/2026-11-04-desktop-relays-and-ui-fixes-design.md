# Desktop relays and cross-platform UI fixes

## Intent

Make the existing Spider UI clearer and make the requested routing, SNI, and LAN sharing actions easy to reach. Add MHR and Soar as usable server types on Desktop only, where their runtimes can be managed by the existing JVM desktop app.

## Goals

- Show only the red circular custom pointer on desktop and hide the native cursor while the custom pointer is active.
- Remove the flower motifs from affected red cards and replace flower-like key marks with circles.
- Add a Home Rule control that opens a glass style picker for Global and IR.
- In IR mode, route Iranian domains and the user's selected Iranian apps directly, while other traffic uses the active VPN profile.
- Add optional SNI spoof settings at the end of VLESS create/edit, with an enable switch and expandable SNI and IP fields; apply these settings to the generated config.
- Add Share LAN beside the map. Tap toggles sharing on `0.0.0.0`; long press shows the active local IP and port.
- Add MHR from Add Server with one deployment ID per line and an auth field.
- Add Soar from Add Server with a pasteable server input, then connect through it.
- MHR and Soar are Desktop-only. Hide or disable their choices on Android.
- Keep shared UI and settings behavior aligned across Android and Desktop where the feature applies.

## Non-goals

- Add iOS or other targets; the current Gradle project defines Android and JVM Desktop only.
- Make SNI spoofing work on Desktop: the current implementation requires Android root and Desktop explicitly rejects it.
- Reimplement MHR or Soar protocols from scratch. Desktop integrations should wrap or package the supplied project implementations, subject to their startup and runtime requirements.

## Constraints and findings

- Compose UI is shared under `shared/src/commonMain`; Android and Desktop platform behavior is supplied by platform source sets.
- `AppSettings` already has routing modes including `Global` and `BypassIran`, app selection and LAN sharing settings. Desktop has a LAN proxy listener path; Android has VPN route/app rules.
- The pointer modifier draws a pointer-shaped glyph and does not hide the operating-system pointer.
- VLESS configuration is built by `ManualConfigSheet` and `ServerConfig`; TLS SNI already exists as a separate field, while SNI spoof profiles are currently independent profiles.
- MHR is a Python local HTTP/SOCKS proxy and requires deployment IDs plus an auth key. Soar is a Go DNS tunnel client requiring a zone, server public key, and resolver configuration. The supplied archives are source archives, not ready-to-run desktop binaries.
- Desktop integration must report missing/unsupported runtime assets as a visible connection error, and must stop child processes when disconnected or the app exits.

## Approaches

1. **Shared UI and configuration, platform-specific engines (recommended).** Keep forms and persisted profile data in shared Kotlin; Desktop adapters launch/manage MHR and Soar runtimes, while Android hides those options. This follows the current architecture and supports existing Android/Desktop targets.
2. **Separate standalone desktop companion processes.** Launch each upstream project independently and connect Spider through its local proxy. This reduces protocol integration but adds installation, lifecycle, and configuration burden for users.
3. **Port both protocols into common Kotlin.** This would provide a uniform runtime but duplicates mature upstream implementations, especially Soar's cryptography and transport, and increases maintenance risk.

The first approach is recommended. Runtime packaging details must be validated against the available Desktop distribution targets before claiming out-of-box support for each OS/CPU pair.

## Proposed behavior

- The custom pointer is a red circle with a subtle pulse. Desktop pointer visibility is suppressed only while the custom pointer overlay is enabled; touch pointers are unaffected.
- Decorative flower silhouettes are removed from the affected red surfaces, and key decorations use a simple circle.
- Home's Rule action opens a translucent menu with Global and IR. Selecting a mode updates the existing routing setting immediately. IR uses the project's existing Iran route rules and per-app selection.
- VLESS SNI spoof is an optional final accordion in create/edit. It stores a boolean, fake SNI, and optional clean IP with the server profile; generation merges the spoof parameters into the existing supported runtime config without changing TLS SNI semantics for normal VLESS profiles.
- Map's Share LAN action toggles the existing proxy-sharing setting. Long press opens an informational view with reachable local address and effective port. Existing authentication setting remains honored.
- MHR profiles store a list of deployment IDs and auth key. Desktop starts the MHR local proxy and feeds its SOCKS endpoint into the existing connection path. Invalid/empty IDs and startup errors are shown before connection is reported as active.
- Soar profiles accept pasted server configuration text and parse it into required zone, server key, and optional resolver/discovery values. Desktop starts the Soar client and uses its local SOCKS endpoint. Android does not offer either profile type.

## Open implementation decisions

- Inspect supplied MHR and Soar sources to identify a stable command/API and license/runtime constraints before packaging. If either cannot be embedded as a child process without user-installed runtimes, expose a clear Desktop prerequisite and do not report it as connected until the local proxy is ready.
- Existing IR app routing uses the app-selection mechanism; the UI should make clear that only selected apps bypass the tunnel.

## Acceptance

- On Desktop, only the red circle is visible as the mouse indicator.
- Flower shapes are absent from the referenced red cards and key marks are circular.
- Home Rule changes between Global and IR and persists through existing settings storage.
- IR routes Iran destinations and selected apps directly.
- VLESS SNI settings survive edit/reopen and are reflected in the generated runtime config when enabled; disabled settings do not alter it.
- Share LAN starts/stops on all interfaces and the long press displays a working local address/port.
- MHR and Soar can be added, persisted, started, stopped, and surfaced as Desktop server entries; Android does not expose them.
- Build checks cover Android and Desktop compile paths. Feature tests cover parsing, config generation, and routing mode mapping.
