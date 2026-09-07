# Pulse VPN 0.7.0

## New working capabilities

1. MTU presets: provider, 1280, 1400, 1500.
2. TUN stack selection: provider, system, gVisor, mixed.
3. Optional direct routing for private/LAN addresses.
4. Optional UDP/443 rejection to disable QUIC.
5. Android metered VPN flag (Android 10+).
6. Configurable connection timeout.
7. DNS IPv4/IPv6 answer strategy.
8. DNS cache switch.
9. Independent DNS caches.
10. Quad9 DoH resolver.
11. AdGuard DoH resolver.
12. TCP probe timeout.
13. TCP probe concurrency limit.
14. Optional retry after failed TCP probes.
15. Configurable slow-server threshold.
16. Persistent ping history switch.
17. Confirmed history clearing.
18. Server sorting by name, delay, or favorites.
19. Per-profile server favorites.
20. Favorites filter.
21. Protocol filter when multiple protocols exist.
22. Hidden server addresses and ports.
23. Screenshot/recent-app protection (Android FLAG_SECURE).
24. Optional connection-button haptic feedback.
25. Keep screen awake while this app is visible and VPN is connected.
26. Default collapsed subscription card.
27. Optional server previews on Home.
28. Speed updates in VPN notification.
29. Wi-Fi-only subscription refresh.
30. Minimum interval for auto-refresh on app opening.
31. Searchable advanced settings grouped by purpose.
32. Confirmed reset of advanced preferences without deleting subscriptions.
33. Bulk app selection, clear selection, and inversion.
34. Explicit apply-and-reconnect for pending connection settings.
35. Persistent release signing with pinned certificate verification in CI.

These are implemented capabilities, not claims of 35 independently validated real-device network scenarios.

## UI / behavior improvements

- Compact provider card with a two-line description preview.
- Full description remains selectable in a dialog.
- Expiry and quota status are visible before detailed statistics.
- Refresh/ping actions precede expanded details.
- More space for action labels at large font sizes.
- Persistent connection error with retry and server-selection actions.
- Explicit VPN permission-denial explanation.
- DNS and routing choices moved to scrollable selection dialogs.
- Adaptive connection-control sizing for smaller screens and large fonts.
- Growing screen headers rather than a fixed-height clipping box.
- Full server name in scrollable details.
- Wrapping detail values.
- Favorites action reuses the route-row icon area.
- Search reset and a useful no-results state for advanced settings.
- Pending-settings indication in settings and Home.
- Home explicitly distinguishes the running configuration from pending choices.
- Settings remain grouped instead of placing all advanced toggles on the first screen.
- Advanced choices show a plain-language explanation and current value.
- Confirmation before clearing history or resetting preferences.
- Screenshot review covers a small display and 150% system font.

## Fixes (verified causes; not inflated to 50)

- Random debug release certificate replaced with one persistent private key.
- Release packaging refuses missing signing configuration.
- CI verifies the pinned signer for every ABI.
- PR builds cannot publish release APKs using a random/missing key.
- Empty per-app include list no longer falls back to all apps.
- UI blocks connecting with an empty include list and explains how to resolve it.
- Routing/DNS edits no longer abruptly disconnect an active tunnel.
- Per-app changes now mark the connection as needing reapplication.
- Duplicate start requests are guarded.
- User cancellation cancels the connection timeout job.
- Profile switching/deletion cancels a pending connection attempt.
- Successful import stops an old-profile connection before showing the new profile.
- Startup failures remain visible rather than disappearing with a temporary banner.
- Fast failures can terminate connection waiting without waiting for a missed Starting event.
- Core error text no longer exposes raw provider configuration details.
- Probe/switch errors during a running VPN are not mislabeled as startup failures.
- Never-measured servers are not labeled as failed in details.
- Refresh loaders say updating rather than checking.
- Removing a profile also removes its favorite and selected-server preferences.
- Missing direct outbound is supplied for direct mode / LAN bypass.
- Invalid persisted advanced values fall back to validated defaults.
- New invalid advanced values are rejected without mutating preferences.
- Filter reset also resets the protocol restriction.
- Screen privacy settings propagate to skipped/reused composables.
- Turning off history persistence clears stored measurements.
- Ping results no longer change the provider's displayed server order.
- Last delays are restored when switching profiles, not only when reloading.
- Wi-Fi refresh checks the active network, not an unrelated connected network.
- Updated runtime settings are also saved for subsequent background/boot connections.
- Large system fonts use a wider connection control so the action does not split mid-word.
- System navigation/status icon contrast follows the app palette after recomposition.

## Verification and limits

Unit tests cover subscription parsing, runtime overlays, every advanced setting value and invalid-value fallback.
Android instrumentation validates libbox config acceptance for routing/DNS choices and advanced network options,
plus import, description, themes, favorites, searchable settings, error recovery UI, long names and small-screen / large-font rendering.
Acceptance of a config is not a live VPN connectivity test. No personal subscription, credentials, HEX API, or paid operation is involved.

The private key is NOT committed or included in APK artifacts. It is stored in GitHub Actions secrets
PULSE_KEYSTORE_BASE64 / PULSE_KEYSTORE_PASSWORD, with a protected local copy outside the repository.
The signer SHA-256 is 8afbb9a7c07690d2ebd5e791edec2ac786d2a698e9c520a112b72cd7eb47ca36.
Keep the same key and increment versionCode for future updates.
Old 0.6.x CI debug keys cannot be reconstructed from APKs. Moving from those builds requires a one-time reinstall;
save subscription links before uninstalling. Subsequent releases using this signer can update in place.
