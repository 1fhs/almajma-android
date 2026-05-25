# Phase 9 — Production UI Hardening

تم تنفيذ تنظيف واجهات وصلاحيات قبل أي Backend:

- إخفاء تبديل الأدوار وأدوات المحاكاة عبر `BuildConfig.ENABLE_DEMO_TOOLS=false`.
- إخفاء دخول الأدمن خلف رمز داخلي `BuildConfig.ADMIN_ACCESS_CODE`.
- منع الصيدلية/التاجر/السائق من الدخول إلى الشاشات التشغيلية قبل اعتماد الإدارة.
- إضافة شاشة انتظار اعتماد واضحة مع زر مراجعة الملف وتسجيل الخروج.
- تثبيت `compileSdk = 36` بدل صيغة minor API غير مستقرة في GitHub Actions.
- رفع الهوية إلى `versionCode=9` و `versionName=0.9.0`.

ملاحظة: هذا ليس أمان Production نهائي؛ الرمز المحلي يجب استبداله لاحقًا بـ Backend Auth وصلاحيات Server-side.
