# PROGRESS

## Now
שלב 0b — קוד מוכן, Release v1.0.11 (טלפון + `wear-pixel-forge.apk`). ממתין לבדיקה על השעון: התקנת אפליקציית השעון ב-ADB אלחוטי → "מעבדת שעון" בטלפון → "בנה ושלח". אם עובד: לסמן ✅ ולעבור לשלב 1.
אם נכשל: הודעת השגיאה מוצגת במסך המעבדה (וגם בשעון) — לתקן לפיה.

## ארכיטקטורה (0b)
- `:watchface` — חבילות WFF בלי קוד. flavors: `home` (פני ברירת מחדל שמותקנים עם אפליקציית השעון) ו-`custom` (תבנית שהטלפון משכתב). חתימה: מפתח debug של CI (חייב להיות שונה ממפתח האפליקציה).
- `:wear` — אותו applicationId ומפתח כמו הטלפון (חובה ל-Data Layer). `PushListenerService` מקבל `/wf/push` (asset + token), מתקין ב-Watch Face Push, מנסה להפעיל, ומחזיר `/wf/result`.
- טלפון: `watch/FaceBuilder` — מחליף את ה-XML בתבנית (נתיבי res מקוצרים ע"י AGP → מזהה לפי תוכן `<WatchFace`), חותם v2 במפתח Android Keystore, מאמת ב-`validator-push-android` לקבלת token. `WatchLink` — Data Layer. מסך `WatchLabScreen`.
- CI: ה-validator (JVM) רץ ב-classloader נפרד (`build.gradle.kts` שורש) — על ה-buildscript classpath הוא שובר את AGP (tink/protobuf).
- גרסאות validator זמינות: alpha09 / 1.1.0-alpha01 (alpha10 מהתיעוד לא קיים).
- לשלבים הבאים: Google ממליצים על ספריית Pack לבניית APK על המכשיר — לבדוק במקום שכתוב zip (מאפשר שמות חבילה שונים = כמה פני שעון במקביל).

## החלטות
- AI = Gemini (מפתח מ-AI Studio). המשתמש מזין אותו בהגדרות; מונה יומי.
- כיוון RTL בכל האפליקציה, אבל כל טקסט מתיישר לפי השפה שלו (`TextDirection.Content` ב-`StudioTheme`): עברית לימין, אנגלית לשמאל. טקסט באנגלית שצריך להיצמד לשמאל — `fillMaxWidth`.
- כיוון עיצובי: דינמי, נקי, הרבה הנפשות (`ui/Motion.kt`).
- לוגו: "Spark P" (`design/logo/b-symbol.svg`) — P גיאומטרי עם פיקסל שניתז. אייקון adaptive + monochrome בשתי האפליקציות. חלופות ב-`design/logo/`.
- Skills: `logo-design` מותקן ב-`.claude/skills`. `landing-page-design` (elayadesign) — לא רלוונטי לאפליקציה, לא הותקן. prompt-motion.com — חסום ברשת של הסשן.
- CI: לוגים נגישים דרך GitHub MCP (`get_job_logs`).

## Secrets (שמות בלבד)
KEYSTORE_BASE64, KEYSTORE_PASSWORD, AI_API_KEY

## Last session
2026-10-28: יישור לפי שפה, לוגו חדש, שלב 0b (מודול שעון + Watch Face Push + בנייה על הטלפון). v1.0.11.
