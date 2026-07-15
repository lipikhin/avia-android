# avia-android

Нативный Android-клиент (Kotlin + Jetpack Compose) для avia (aviatechnik.ca).

## Source of truth

Контракт API и поведение экранов зафиксированы в основном Laravel-репозитории (`Famousleonid/avia`):

- `docs/mobile-native-android-brief.md` — бриф этого клиента (стек, экраны, анти-паттерны)
- `docs/mobile-native-tz.md` — полное ТЗ mobile-клиента
- `routes/api.php`, `app/Http/Controllers/Api/Android/AndroidApiController.php` — серверный контракт

API: `https://aviatechnik.ca/api/android/*` (Bearer-токен). Debug-сборка смотрит на локальный OSPanel (`http://avia/`) — работает при запуске на устройстве/эмуляторе с доступом к хосту.

## Сборка

Требуется JDK 17 и Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug   # APK: app/build/outputs/apk/debug/
```

## Статус

- [x] Скелет: Hilt, Retrofit/OkHttp (+Bearer interceptor), EncryptedSharedPreferences, Navigation Compose
- [x] Splash (app-config) → Login (remember me) → Home (bootstrap, server-driven menu)
- [ ] Workorders list/detail + media
- [ ] Tasks, Processes
- [ ] Components + TDR
- [ ] Materials, Paint, Machining
- [ ] Drafts, Profile
