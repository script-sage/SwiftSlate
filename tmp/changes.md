## Thanks for this PR! The Text Replacer concept is a great addition — offline snippets are one of the most requested features (#25). Solid thinking on the separate prefix to keep AI and offline commands distinct.

That said, there are a few things that need addressing before this can be merged:

---

### 🔴 Blockers

**1. Default prefix changed from `?` to `!`**
`DEFAULT_PREFIX` in `CommandManager.kt` was changed from `"?"` to `"!"`. This is a breaking change for every existing user — `?fix`, `?improve`, `?formal` etc. are what people know from the README, the Play Store description, and muscle memory. New installs would get `!` as the default, which is inconsistent with all documentation. Please revert this to `"?"`.

**2. `handleTextReplacer` and `handleFileShare` don't guard with `isProcessing`**
Both methods launch coroutines without setting `isProcessing = true` first. If a user types fast or the accessibility service fires multiple events, these could execute concurrently. The AI path sets `isProcessing` before launching — the replacer/file paths should too, and reset it in a `finally` block.

---

### 🟡 Should Fix

**3. No error handling in `handleTextReplacer`**
If `replaceText` throws (which it can — stale node, app killed, etc.), the coroutine crashes silently. Wrap in try-catch like the existing `handleUndo` does, with a toast on failure.

**4. Hardcoded strings**
The codebase recently had all UI strings extracted to `strings.xml` for localization (#17). This PR adds new hardcoded strings: `"Replacement Text (e.g., Let's connect!)"`, `"File Path or URI"`, `"Browse"`, `"File could not be found or read."`, `"Could not share file"`, `"[AI Action]"`, `"[Content Replacer]"`, `"[File Share]"`, etc. These should go in `strings.xml`.

**5. No runtime file validation for File Share**
`checkFileExists` validates at creation time, but the file could be moved/deleted between creation and trigger. A quick check before `startActivity` with a user-friendly toast would prevent a confusing crash.

---

### 💭 Suggestions

**6. Consider deferring File Share to a separate PR**
Text Replacer is a clean, focused feature that stands on its own. File Share is a different interaction model (opens share sheet, needs URI permissions, file picker) and adds significant complexity. Shipping Text Replacer first and File Share as a follow-up would make both easier to review and test.

**7. The `precedingTextRaw` approach is good**
Preserving the untrimmed preceding text for replacer commands (so `"hello /email"` becomes `"hello myemail@example.com"` with the space) is the right behavior for a text expander. Nice detail.

---

Looking forward to the updated version! The core idea here is solid.