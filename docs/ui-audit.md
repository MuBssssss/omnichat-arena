# OmniChat Arena UI/accessibility audit

## Scope

This audit covers the native Jetpack Compose Chat, Arena, and Keys surfaces for a 360dp-wide,
720x1280-class Android phone such as the Samsung Galaxy J7 Nxt. It is a design/implementation
review, not a substitute for a full TalkBack or instrumented accessibility run on every release.

## 1. Layout and density

- Keep the main screens vertically scrollable where content can exceed a 720p viewport.
- Use full-width controls and modest 8–12dp spacing; avoid fixed pixel widths.
- Keep chat bubbles below the full width so sender distinction remains visible on narrow screens.
- Align the provider/status row and composer vertically to avoid baseline drift.

## 2. Touch targets and focus

- Material buttons, menu items, and text fields retain Compose Material minimum touch targets.
- Keep the provider selector and Send action visible without requiring horizontal scrolling.
- Do not make a color-only affordance the sole way to select a provider or retry an error.

## 3. Chat scrolling

- The chat list uses a remembered `LazyListState` and follows new user/streaming content so the
  active answer remains visible.
- Scrolling must remain user-controllable; the list should not reset history when switching tabs.
- Streaming updates should not create duplicate persistent messages.

## 4. IME and keyboard behavior

- The composer uses a single-line Send action for the common phone flow.
- `ImeAction.Send` and `KeyboardActions` submit the trimmed prompt without requiring the user to
  dismiss the keyboard first.
- Empty prompts remain disabled and do not start a provider request.

## 5. TalkBack semantics and status

- The streaming/ready indicator is a polite live region so generation state is announced without
  interrupting the user's current speech.
- Sender labels remain in the message text and are not conveyed only through color or alignment.
- Error cards use the error color role plus explicit warning text.
- Any future icon-only action needs a content description and a testable semantic label.

## 6. Error and loading states

- Missing keys/session values surface an in-app, non-crashing warning.
- Network/provider failures are rendered as error messages rather than silent spinner states.
- A provider auth-wall must request re-login and must not expose response bodies or cookies.

## 7. Secret-entry safety

- Session and API-key fields use password transformation, password keyboard type, and disabled
  autocorrect to reduce personal-dictionary and keyboard telemetry leakage.
- Saved values are cleared from the Compose field after the save action.
- The UI must never display, log, partially mask, or screenshot a stored value.
- The app must explain that unofficial browser-session imports carry account/terms risk.

## 8. Minimal interaction and verification

- The common loop is provider select, prompt, Send/IME Send, and observe streaming output.
- No WebView is used for chat; browser launch is limited to explicit login/session setup.
- The final acceptance pass belongs to Gemini: Gradle tests/build plus a non-sensitive
  `mobile-mcp` run on the target device. TalkBack should be spot-checked on the physical phone
  before release.

## Review status

Arena implemented the small UI slice directly after the AI Studio branch/commit could not be
verified through GitHub. Gemini must rerun the Android build and device smoke check on the Arena
branch before these changes are treated as release-verified.
