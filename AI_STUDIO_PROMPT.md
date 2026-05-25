أنت تعمل على مشروع Android قائم، وليس مشروعًا جديدًا.

المطلوب:
- افتح الكود الحالي كما هو.
- لا تعيد بناء التطبيق من الصفر.
- لا تغيّر التصميم العام إلا عند الضرورة.
- حافظ على Kotlin + Jetpack Compose + Room.
- ركّز على إصلاح الأخطاء وربط النواقص تدريجيًا.

وصف التطبيق:
تطبيق المجمع هو Prototype تشغيلي محلي لسوق أدوية/صيدليات. العميل يطلب دواء، الصيدلية تقدم عرضًا، العميل يقبل العرض، يتم تجميد ضمان مالي محلي، ثم تجهيز وتسليم الطلب، مع دعم النزاعات، الوصفات، الإشعارات، واعتماد الصيدليات.

الملفات الأساسية:
- app/src/main/java/com/example/ui/screens/AlMajmaAppUi.kt
  واجهات Compose الرئيسية.
- app/src/main/java/com/example/ui/PlatformViewModel.kt
  حالة الواجهة والأحداث.
- app/src/main/java/com/example/data/repository/PlatformRepository.kt
  منطق التشغيل المحلي.
- app/src/main/java/com/example/data/database/Entities.kt
  كيانات Room.
- app/src/main/java/com/example/data/database/DAOs.kt
  DAO queries.
- app/src/main/java/com/example/data/database/AppDatabase.kt
  قاعدة Room والـ migrations.
- app/src/main/java/com/example/data/database/Money.kt
  تحويل المال إلى minor units.

قواعد صارمة:
1. لا تستخدم Double كأساس محاسبي نهائي. استخدم minor units Long عند إضافة أي مبلغ مالي جديد.
2. لا ترجع fallbackToDestructiveMigration؛ هذا يمسح بيانات المستخدم.
3. لا تضف backend وهمي داخل الواجهة. إذا احتجت API فاعمل طبقة Repository/Network واضحة.
4. لا تخلط منطق المال داخل Composable.
5. لا تجعل أزرار الواجهة شكلية؛ كل زر يجب أن يمر عبر ViewModel ثم Repository.
6. لا تفتح الطلبات للسائق قبل أن تصبح جاهزة أو مجمدة الضمان حسب الحالة.
7. أي تغيير في Room يحتاج Migration صريحة.

أولويات التطوير القادمة:
- إضافة Backend API حقيقي.
- Auth وصلاحيات.
- رفع صور الوصفات والتراخيص.
- FCM push notifications.
- Admin Web Panel.
- GPS/خرائط ونطاق خدمة الصيدليات.
- Payment/Escrow حقيقي بدل الضمان المحلي.

ابدأ أولًا بتشغيل build/check واكتشاف أخطاء compile، ثم أصلحها بأقل تغييرات ممكنة.
