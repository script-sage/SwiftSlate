<div align="center">

<br>

<img src="playstore-icon.png" width="140" alt="SwiftSlate Icon" />

<br>

# SwiftSlate (Script Sage Edition)

### System-wide AI text assistant AND native File Sharing toolkit for Android

Type a trigger like **`?fix`** at the end of any text, in any app, and watch it get replaced — instantly, or broadcast your typed texts via **`/share`** natively across Android.

This is a customized standalone fork of SwiftSlate tracking features like 3-tier prefixing, offline File Sharing, and usage tracking natively over the official UI. 

<br>

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#-getting-started)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#%EF%B8%8F-tech-stack)
[![Gemini](https://img.shields.io/badge/Gemini_AI-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)](#-supported-ai-providers)

<br>

</div>

> [!NOTE]
> SwiftSlate works in most apps — WhatsApp, Gmail, Twitter/X, Messages, Notes, and more. No copy-pasting. No app switching. Just type and go.

<br>

## ✨ Added Features in Script Sage Edition

Along with all upstream capabilities from the original SwiftSlate tracking master, this standalone edition brings:

### 🧩 3-Tier Prefix Configurations
Tired of one global trigger character? You can now separately configure prefix triggers for:
- **AI Commands:** (Default `?`) e.g. `?fix`
- **Text Replacers:** (Default `$`) e.g. `$sig`
- **File & Text Sharing:** (Default `/`) e.g. `/share`

Configure all three prefixes seamlessly inside the standard Settings menu.

### 📤 Native File & Text Sharing Commands
Create a new custom command of type **File Share**. SwiftSlate will catch your typed input, instantly revert it from the input field, and bounce it into Android's native system-wide Share Sheet. You can even configure a custom prepended "prompt" to add context to any shared text!

Example workflow:
`Check out this cool link! /send` → Instantly pops open Android Share menu to send to any app.

<br>

## ⚡ Quick Demo

```
📝  You type       →  "i dont no whats hapening ?fix"
⏳  SwiftSlate     →  ◐ ◓ ◑ ◒  (processing...)
✅  Result         →  "I don't know what's happening."
```

```
📝  You type       →  "Meeting in 10 mins $office"
✅  Result         →  "Meeting in 10 mins at 123 Main St, Floor 4"
```

```
📝  You type       →  "Here is that snippet you wanted /share"
📤  Result         →  (Pops up Android Share Intent containing the snippet)
```

<br>

## 🤖 Supported AI Providers

| Provider | Models | Notes |
|:---------|:-------|:------|
| **Google Gemini** (default) | `gemini-2.5-flash-lite`, `gemini-3-flash-preview` | Free tier available at [aistudio.google.com](https://aistudio.google.com) |
| **Custom (OpenAI-compatible)** | Any model your endpoint supports | Works with Ollama, LM Studio, vLLM, etc |

<br>

## 🚀 Getting Started

**1.** Compile and build the APK using Android Studio, or download our released APK.
**2.** Install the APK on your device (allow installation from unknown sources if prompted)
**3.** Open the **Keys** tab, enter your API key (Free Gemini key applies!).
**4.** On the **Dashboard**, tap **"Enable"** → find **"SwiftSlate Assistant"** in Accessibility Settings → toggle it on.
**5.** Open any app, type your text, and deploy your triggers natively!

<br>

## 🔐 Privacy & Security

| Concern | How SwiftSlate Handles It |
|:--------|:------------------------|
| **Text Monitoring** | Only processes text when a trigger command is detected at the end. All other typing is completely ignored. Password fields are always skipped. |
| **Data Transmission** | Text is sent **only** to the configured AI provider. No other servers are ever contacted. Text replacer and Share commands never leave your device natively. |
| **Key Storage** | API keys are encrypted with AES-256-GCM using the Android Keystore system. |

---

<div align="center">

<br>

### Original Upstream Project
This project is an extended fork of [Musheer360/SwiftSlate](https://github.com/Musheer360/SwiftSlate).
License: MIT License. Original Copyright (c) 2026 Musheer Alam.

<br>

</div>
