# تطبيق المرحلة 8 من Termux

هذه المرحلة تغيّر package من `com.example` إلى `com.almajma.app`، لذلك يجب حذف مجلدات `com/example` القديمة قبل نسخ الملفات الجديدة فوق المستودع.

```bash
cd /storage/emulated/0/Download

rm -rf almajma-phase8
mkdir almajma-phase8
unzip -o almajma-phase8-identity-cleanup-source.zip -d almajma-phase8
find almajma-phase8 -name settings.gradle.kts

# قبل النسخ: احذف حزم com.example القديمة من المستودع الحالي
rm -rf /storage/emulated/0/Download/almajma-android/app/src/main/java/com/example
rm -rf /storage/emulated/0/Download/almajma-android/app/src/test/java/com/example
rm -rf /storage/emulated/0/Download/almajma-android/app/src/androidTest/java/com/example

# إذا ظهر settings.gradle.kts مباشرة تحت almajma-phase8
cp -a /storage/emulated/0/Download/almajma-phase8/. /storage/emulated/0/Download/almajma-android/

cd /storage/emulated/0/Download/almajma-android
git status
git add -A
git commit -m "Clean app identity and package naming"
git pull --rebase origin main
git push origin main
```

بعد الرفع، احذف التطبيق القديم من الهاتف قبل تثبيت APK الجديد لأن اسم الحزمة تغيّر فعليًا.
