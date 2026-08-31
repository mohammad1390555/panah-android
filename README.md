# پناه

کلاینت سبک اندروید برای لینک سابسکریپشن پنل **PANAHANNET**.

بدون تونل، بدون پلی‌استور، بدون فایربیس. لینک را می‌چسبانی، لیست کانفیگ می‌آید، کپی یا اشتراک.

APK از ریلیزهای همین ریپو نصب می‌شود. هر پوش روی `main` یک نسخهٔ جدید می‌سازد.

## نصب

1. [Releases](https://github.com/mohammad1390555/panah-android/releases) → آخرین APK
2. روی گوشی اجازهٔ نصب از منبع ناشناس
3. نصب

اندروید ۸ به بالا.

## استفاده

- `+` → نام و لینک سابسکریپشن
- لمس کارت → لیست کانفیگ‌ها
- لمس کانفیگ → کپی
- نگه‌داشتن → ویرایش / حذف / اشتراک
- منو → به‌روزرسانی همه

پروتکل‌ها: VLESS، VMess، Trojan، Shadowsocks، Hysteria2، TUIC، WireGuard.

## ساخت محلی

JDK 17 + Android SDK 35.

```bash
./gradlew assembleRelease -PversionCode=1 -PversionName=1.0
# خروجی: app/build/outputs/apk/release/app-release.apk
```

## ورکفلو

`.github/workflows/release.yml` روی هر push به `main`:

- `versionName = 1.{run_number}`
- APK ریلیز ساین می‌شود
- GitHub Release با تگ `v1.N` و فایل APK
