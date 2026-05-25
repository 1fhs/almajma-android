# Project Structure

```text
.
├── AI_STUDIO_IMPORT.md
├── AI_STUDIO_PROMPT.md
├── PROJECT_STRUCTURE.md
├── README.md
├── .env.example
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
└── app
    ├── build.gradle.kts
    └── src
        ├── main
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/MainActivity.kt
        │   ├── java/com/example/data/database
        │   │   ├── AppDatabase.kt
        │   │   ├── DAOs.kt
        │   │   ├── Entities.kt
        │   │   └── Money.kt
        │   ├── java/com/example/data/repository/PlatformRepository.kt
        │   ├── java/com/example/ui/PlatformViewModel.kt
        │   └── java/com/example/ui/screens/AlMajmaAppUi.kt
        └── test/java/com/example/MoneyPrecisionTest.kt
```

## فتح المشروع

- Android Studio: افتح مجلد المشروع الذي يحتوي `settings.gradle.kts`.
- Google AI Studio: ارفع ZIP أو اسحب الحزمة إذا كانت واجهة Build تدعم الاستيراد.
- GitHub: ارفع الملفات كما هي إلى Repository، ثم اربطها من AI Studio إذا ظهرت لك خاصية GitHub.
