# PROGRESS

## Now
שלב 1 (עורך) — רובו בנוי. "Watch Faces" בבית פותח את העורך (`ui/EditorScreen.kt`); מעבדת השעון הוסרה, השליחה לשעון בכפתור בעורך.
- יש: קנבס עגול חי, גרירה + קווי מרכז, לשוניות הוסף/שכבות/עיצוב, undo/redo, שמירה אוטומטית (`filesDir/faces/current.json`), 10 תבניות.
- שכבות: שעה (+שניות), תאריך, טקסט, טבעת, קשת, סימונים, מחוגים אנלוגיים, עיגול, מלבן, אייקון (ספריית Material ~80, לפי קטגוריות), תמונה מהגלריה.
- עיצוב: צבע, אטימות, גרדיאנט, משקל גופן (5), סיבוב, פינות, גדלים.
- הבא בשלב 1: גלריית פנים שמורות + היסטוריית גרסאות, צביטה לשינוי גודל, שכפול שכבה.
- 0b עדיין לא נבדק על השעון: להתקין `wear-pixel-forge.apk` בשעון (ADB אלחוטי) → "Watch Faces" → כפתור שליחה. שגיאות מוצגות מתחת לסרגל העליון.
- לוגו: Orbit Studio (מ-Canva, משוחזר כוקטור: `design/logo/orbit-final/logo.py` מייצר את `ic_fg/ic_bg/ic_mono` בשתי האפליקציות). המשתמש: "בעתיד נשנה". ה-skill `logo-design` הוסר לבקשתו.

## עורך — ארכיטקטורה
- `editor/FaceModel.kt`: `Layer`/`FaceDoc` (JSON) + `FaceXml` (מייצר WFF v1) + `Presets`. פיקסלים של 450.
- תמונות/אייקונים: בתבנית `custom` יש 24 משבצות `slot_N.png` (PNG 4x4 עם chunk `pfslot`, crunch כבוי). `FaceBuilder` מחליף אותן לפי תוכן ב-PNG שנוצרו בטלפון (`editor/Assets.kt`).
- בדיקת XML בלי טלפון: `tools/facecheck` (JVM + XSD הרשמי של WFF). Google Maven חסום בקונטיינר — את ה-UI רק CI מקמפל.

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
- לוגו ישן (יוחלף): "Spark P" (`design/logo/b-symbol.svg`) — P גיאומטרי עם פיקסל שניתז. אייקון adaptive + monochrome בשתי האפליקציות. חלופות ב-`design/logo/`.
- Skills ב-`.claude/skills` (`logo-design` הוסר): `algorithmic-art`, `theme-factory` (Anthropic), `material-3` (hamen), `text-to-lottie` (diffusionstudio — לאנימציות UI בטלפון בלבד; WFF לא מריץ Lottie), `wear-widgets`, `wear-compose-m3`, `glance-canonical-layouts`, `glance-appwidget-updates-and-memory` (רשמיים של Google, android/skills). ל-WFF אין skill פופולרי — רק פרויקטים קהילתיים קטנים. `landing-page-design` (elayadesign) — לא רלוונטי לאפליקציה, לא הותקן. prompt-motion.com — חסום ברשת של הסשן.
- CI: לוגים נגישים דרך GitHub MCP (`get_job_logs`).

## Secrets (שמות בלבד)
KEYSTORE_BASE64, KEYSTORE_PASSWORD, AI_API_KEY

## Last session
2026-10-28: לוגו Orbit Studio, עורך פני שעון (שלב 1 ברובו), נכסים: אייקונים + תמונות.
