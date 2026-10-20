# UniRemote 📱📺

A modern, universal TV remote control application for Android built with **Jetpack Compose**, **Kotlin Coroutines**, and **Material 3**. 

UniRemote seamlessly combines **Infrared (IR Blaster)** hardware control and **Wi-Fi Smart TV (Google Android TV Remote v2)** network protocols in a single tactile interface.

---

## ✨ Features

- **Dual-Mode Control:**
  - 📡 **Infrared (IR) Mode:** Directly control older & non-smart TVs using the phone's hardware IR blaster.
  - 📶 **Wi-Fi Smart TV Mode:** Control Android TVs, Toshiba Smart TVs, Roku, etc., over local Wi-Fi without needing line-of-sight.
- **Smart TV Auto-Discovery:**
  - Fast subnet auto-scan using SSDP/mDNS and DIAL (` Eureka / 8008`) protocol to detect Smart TVs automatically.
- **Google Android TV Remote v2 Protocol:**
  - Built-in TLS mutual authentication on port `6467` (Polo Pairing Protocol) and port `6466` (Remote Key Injection).
  - Fast, responsive key commands with persistent TLS socket connection.
  - Wake-on-LAN (WoL) support to turn on sleeping TVs.
- **Multi-Brand IR Code Library:**
  - **Toshiba:** Dual profile support (Smart / CT-8560 and Regza / CT-90325) with bit-reversed NEC encoding.
  - **Samsung:** Official NEC `0xE0E0` standard codes.
  - **LG:** Official NEC `0x20DF` standard codes.
  - **Sony:** SIRC 12-bit (40 kHz carrier frequency).
  - **Mi / Xiaomi:** Standard `0x807F` NEC codes.
  - **TCL / Thomson:** Standard `0x00F0` NEC codes.
  - **Panasonic:** Kaseikyo / Panasonic 36.7 kHz codes.
  - **Roku:** External Control Protocol (ECP) over HTTP.
- **Tactile UI Experience:**
  - Skeuomorphic remote layout with haptic vibration feedback.
  - Animated top IR LED indicator.
  - Full D-Pad navigation, Volume/Channel rockers, Keypad (0-9), TV/AV input toggle, Home, Back, and Mute.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** MVVM (Model-View-ViewModel) + StateFlow
- **Cryptography & Security:** BouncyCastle for X.509 Certificate Generation & TLS Key Management
- **Networking:** Kotlin Coroutines + Sockets + Android TV Remote v2 Protobuf Framing
- **Target SDK:** Android 15 (API 35)
- **Minimum SDK:** Android 7.0 (API 24)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 17+
- Android SDK Platform 35

### Build and Run
```bash
# Clone the repository
git clone https://github.com/Livuniq/UniRemote.git
cd UniRemote

# Build Debug APK
./gradlew assembleDebug

# Build Play Store App Bundle (.aab)
./gradlew bundleRelease
```

---

## 📄 License & Disclaimer

This project is open-source.

*Disclaimer: UniRemote is an independent application and is not affiliated with, endorsed by, or sponsored by Toshiba, Samsung, LG, Sony, Roku, or any other television brand. All product names, logos, and brands are property of their respective owners.*
