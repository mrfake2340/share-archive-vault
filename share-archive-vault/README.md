# 🔐 Share Archive Vault

একটি Android অ্যাপ যা WhatsApp চ্যাট এক্সপোর্ট করা ZIP ফাইল থেকে ফটো ও ভিডিও বের করে সহজে শেয়ার করতে দেয়।  
*An Android app that extracts photos & videos from WhatsApp exported ZIP archives and lets you share them instantly.*

---

## 📋 ফিচার সমূহ / Features

| ফিচার | বিবরণ |
|-------|--------|
| 🔗 Share Sheet Integration | WhatsApp থেকে সরাসরি ZIP শেয়ার করুন এই অ্যাপে |
| 📁 ZIP Extraction | ফটো (.jpg .png .webp) ও ভিডিও (.mp4 .mkv .3gp) আলাদা ট্যাবে দেখা যায় |
| 🔒 Duplicate Prevention | SHA-256 হ্যাশ দিয়ে ডুপ্লিকেট ফাইল সম্পূর্ণ বাদ দেওয়া হয় |
| ✅ Multi-Select | সব সিলেক্ট বা একটি একটি করে সিলেক্ট করুন |
| 📤 Native Share | সিলেক্ট করা ফাইল সরাসরি Telegram বা যেকোনো অ্যাপে শেয়ার |
| 🛡️ Privacy First | অ্যাপ বন্ধ হলে সব ক্যাশ ফাইল স্বয়ংক্রিয়ভাবে মুছে যায় |

---

## 🚀 Setup Instructions (Android Studio)

### Step 1: Prerequisites
- **Android Studio** Hedgehog (2023.1.1) বা তার উপরে ডাউনলোড করুন।
  👉 https://developer.android.com/studio
- **JDK 17** ইন্সটল থাকতে হবে।
- **Android SDK API 34** ইন্সটল করুন (SDK Manager → SDK Platforms)।

### Step 2: প্রজেক্ট ওপেন করুন
1. এই ফোল্ডার (`share-archive-vault/`) ডাউনলোড করুন।
2. Android Studio ওপেন করুন → **File → Open** → `share-archive-vault` ফোল্ডার সিলেক্ট করুন।
3. Gradle sync এর জন্য অপেক্ষা করুন (প্রথমবার ইন্টারনেট লাগবে)।

### Step 3: Gradle Wrapper JAR
Gradle wrapper JAR ফাইলটি generate করতে হবে (এটি Git এ রাখা হয় না):
```bash
# প্রজেক্ট ফোল্ডারে ঢুকে:
gradle wrapper --gradle-version 8.6
```
অথবা Android Studio → **File → Invalidate Caches / Restart** করলেও হবে।

### Step 4: Build ও Install করুন
```bash
# Debug APK build:
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

অথবা Android Studio থেকে: **Run → Run 'app'** (emulator বা real device এ)।

---

## 📱 কীভাবে ব্যবহার করবেন / How to Use

### WhatsApp থেকে:
1. WhatsApp → যেকোনো চ্যাট ওপেন করুন
2. উপরে ৩ ডট মেনু → **More → Export Chat → Include Media**
3. Share Sheet এ **Share Archive Vault** সিলেক্ট করুন
4. অ্যাপটি ZIP এক্সট্র্যাক্ট করে Photos ও Videos আলাদা ট্যাবে দেখাবে
5. যা শেয়ার করতে চান সিলেক্ট করুন → **Share Selected** বাটনে ক্লিক করুন
6. Telegram বা অন্য যেকোনো অ্যাপ বেছে নিন

### File Manager থেকে:
যেকোনো `.zip` ফাইলে ট্যাপ করুন → Open With → **Share Archive Vault**

---

## 🏗️ প্রজেক্ট স্ট্রাকচার / Project Structure

```
app/src/main/
├── AndroidManifest.xml          # Intent filters for Share Sheet
├── java/com/sharearchivevault/
│   ├── MainActivity.kt          # Entry point, intent handler, lifecycle
│   ├── model/
│   │   └── MediaItem.kt         # Data model with hash + selection state
│   ├── ui/
│   │   ├── MainViewModel.kt     # ViewModel with StateFlow
│   │   ├── MediaAdapter.kt      # RecyclerView adapter (dark grid + checkboxes)
│   │   ├── MediaFragment.kt     # Reusable Photos/Videos tab fragment
│   │   └── VaultPagerAdapter.kt # ViewPager2 tab adapter
│   └── util/
│       ├── ZipExtractor.kt      # ZIP parsing + file categorisation
│       ├── HashUtil.kt          # SHA-256 duplicate detection
│       ├── CacheManager.kt      # Temp dir management + wipe
│       └── CacheWipeWorker.kt   # WorkManager background cache wiper
└── res/
    ├── layout/
    │   ├── activity_main.xml    # Dark-themed main layout
    │   ├── fragment_media.xml   # Grid + SelectAll bar
    │   └── item_media.xml       # Thumbnail + checkbox + video badge
    ├── values/
    │   ├── colors.xml           # Dark palette (#0F0F0F bg, #00BFA5 accent)
    │   ├── strings.xml          # All user-facing strings
    │   ├── themes.xml           # Material dark theme
    │   └── dimens.xml
    └── xml/
        └── file_provider_paths.xml  # FileProvider cache path config
```

---

## 🔒 Privacy Architecture

```
WhatsApp ZIP
    │
    ▼  (content:// URI — no file copy, stream-only)
ZipExtractor.extract()
    │
    ├─► getCacheDir()/vault_extract/  ← ONLY location used
    │        (internal app cache, not accessible to other apps)
    │
    ├─► SHA-256 hash each file → skip duplicates
    │
    └─► Display in-memory references only

onDestroy() fired
    │
    ├─► CacheManager.clearAll()  (immediate, same thread)
    └─► CacheWipeWorker          (WorkManager — runs even if process killed)
```

**গ্যারান্টি:** কোনো ফাইলই ডিভাইসের `/sdcard/` বা পার্মানেন্ট স্টোরেজে যায় না।

---

## 🛠️ Tech Stack

| Component | Library |
|-----------|---------|
| Language | Kotlin 1.9 |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 34 (Android 14) |
| UI | Material Components 1.11, ViewBinding |
| Async | Kotlin Coroutines + StateFlow |
| Images | Glide 4.16 |
| Background | WorkManager 2.9 |
| Build | Gradle 8.6 + AGP 8.2 |

---

## 🐛 Troubleshooting

**"Gradle wrapper jar not found"**  
→ Run: `gradle wrapper` in the project root, or let Android Studio regenerate it.

**"App doesn't appear in WhatsApp share sheet"**  
→ Make sure the APK is installed with `assembleDebug`, not just run in an emulator snapshot.

**"Files not showing after ZIP import"**  
→ Check that the ZIP actually contains `.jpg/.png/.mp4` etc. Encrypted WhatsApp backups won't work.

---

## 📄 License
MIT — Free to use, modify, and distribute.
