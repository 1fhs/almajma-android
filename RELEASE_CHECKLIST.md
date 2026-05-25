# AlMajma Release Checklist

هذه المرحلة تنظف هوية التطبيق فقط. التطبيق ما زال Prototype محليًا وليس Production.

## قبل توزيع APK خارجيًا

- تأكد أن `applicationId` هو `com.almajma.app`.
- لا ترفع مفاتيح keystore أو Firebase أو API إلى GitHub.
- نسخة Debug تحمل suffix داخلي: `com.almajma.app.debug` حتى لا تختلط مع نسخة Release.
- لا تعتمد على Room المحلي كحقيقة نهائية للضمان أو النزاعات أو الاعتمادات.
- اربط Backend قبل أي تشغيل تجاري.

## Release signing

لإنتاج نسخة Release موقعة من GitHub Actions لاحقًا، أضف Secrets التالية:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

حالياً Workflow يبني Debug APK فقط لأن التوقيع الرسمي غير مجهز.
