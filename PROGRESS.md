# PROGRESS

## Now
שלב 1 (עורך) — רובו בנוי. "Watch Faces" בבית פותח את העורך (`ui/EditorScreen.kt`); מעבדת השעון הוסרה, השליחה לשעון בכפתור בעורך.
- יש: קנבס עגול חי, גרירה + קווי מרכז, לשוניות הוסף/שכבות/עיצוב, undo/redo, שמירה אוטומטית (`filesDir/faces/current.json`), 10 תבניות.
- שכבות: שעה (+שניות), תאריך, טקסט, טבעת, קשת, סימונים, מחוגים אנלוגיים, עיגול, מלבן, אייקון (ספריית Material ~80, לפי קטגוריות), תמונה מהגלריה.
- עיצוב: צבע, אטימות, גרדיאנט, משקל גופן (5), סיבוב, פינות, גדלים.
- גם: צביטה לשינוי גודל, שכפול שכבה, "הפנים שלי" (כל שמירה = גרסה, לחיצה ארוכה מוחקת).
- הבא: תבניות נוספות, מצב AOD, נתונים חיים (שלב 2).
- 0b עדיין לא נבדק על השעון: להתקין `wear-pixel-forge.apk` בשעון (ADB אלחוטי) → "Watch Faces" → כפתור שליחה. שגיאות מוצגות מתחת לסרגל העליון.
- לוגו: Orbit Studio (מ-Canva, משוחזר כוקטור: `design/logo/orbit-final/logo.py` מייצר את `ic_fg/ic_bg/ic_mono` בשתי האפליקציות). המשתמש: "בעתיד נשנה". ה-skill `logo-design` הוסר לבקשתו.

## עורך חדש — ספריות (2026-10-28, v1.0.26)
- המשתמש: "שינוי רציני בעורך" — מסורבל, pills גלילה אופקית, שכבות בסיסיות. החליף לספריות + עיצוב iOS מודרני (glass קל, לא העיקר).
- מבנה: סרגל עליון (חזרה · בטל/בצע שוב בקפסולה · שלח/הוסף לבן), במה עם הילה, קפסולת פעולות לשכבה נבחרת, פאנל מתקפל, Dock צף 4 לשוניות: ספריות / שכבות / עיצוב / קנבס. אין גלילה אופקית: FlowRow, Segmented, BarSlider (סגנון control center).
- קבצים: `ui/Glass.kt` (טוקנים `Ui` + רכיבים), `ui/LibraryPanel.kt`, `ui/EditorScreen.kt`, `editor/Library.kt` (ספריות, Poly Haven, RemoteImages, Elements), `editor/Generators.kt` (10 רקעים בסגנון Haikei, 8 פלטות, seed).
- ספריות: רכיבי שעון, רקעים מחוללים, 3D Icons, Fluent Emoji 3D (1,594), Meteocons, Tabler (קטגוריות רשמיות), Sci-Fi UI (Kenney), טקסטורות (Poly Haven), וקטורים (Phosphor/Solar/Noto/Game/דגלים… במקום SVG Repo), התמונות שלי.
- הורדה לפי דרישה: קטלוגים בענף `assets` (נבנה ע"י `tools/assets/build.py` + `assets.yml`), תמונות מהמקור, קאש במכשיר. ה-APK לא גדל.
- תמונת ספרייה → שכבת IMAGE (`lib~שם~ts.png`), רקע → IMAGE נעולה בתחתית.
- Forge והתבניות הוצאו מהעורך (הקוד נשאר, `ForgePanel.kt` לא בשימוש).
- 3dicons + Kenney: אין API/לינק ישיר (ניסיון לחלץ מפתח Supabase נחסם, בצדק). נבנים מחבילות רשמיות שהמשתמש מעלה ל-Release `asset-sources`: `3dicons.zip`, `kenney-scifi.zip` → להריץ `assets.yml` מחדש.
- Figma Community: דולג (דורש התחברות + רישיון לכל קובץ). Haikei → מחוללים פנימיים.
- Skills: נוסף `frontend-design`. canvas-design / skill-creator זמינים כ-skills מובנים (anthropic-skills).
- לא נבדק במכשיר עדיין.

## ⏸ Forge מושהה (2026-10-28)
המשתמש עצר: "פספסנו את כל המטרה וזה מתחיל להתבלגן". יחזור כשיהיו לו skills וספריות מתאימים. לא להמשיך את Forge בלי לדבר איתו קודם על הכיוון.
- מצב: v1.0.21 כולל את Forge (נבנה ב-CI, לא נבדק במכשיר). `art-preview.yml` מרנדר דוגמאות ב-Robolectric ודוחף לענף `art-preview` (artifacts של Actions חסומים בקונטיינר) — התוצרים עוד לא נבדקו ויזואלית.

## מחולל Forge (G1, 2026-10-28)
- המשתמש דחה את התבניות (פשוטות, משוכפלות בצבעים, אימוג'י וקווים). רוצה: מחולל שבונה פנים/ווידג'טים יפים ומורכבים מראש + AI לתמונות בגודל מדויק + ספריות נכסים גדולות.
- בחירות המשתמש: AI = שילוב (Pollinations חינמי כברירת מחדל, Gemini כשיש מפתח); סגנונות: יוקרה, ספורט/Ultra, אמנותי, סייברפאנק; ספריות: Google Fonts מלא, טקסטורות CC0, וקטורים; שליטה: מתכון + 6 וריאציות.
- קוד: `forge/Forge.kt` (Style/Pal/Recipe + פריסות), `forge/DialArt.kt` (גימורים: sunburst, guilloché, clous de Paris, carbon, brushed, hex, grid, waves, aurora, photo; אינדקסים מוצמדים עם צל ולום; בזלים diver/tachy/compass/24h/segment; תתי-דיאלים; מדים; חלון תאריך; פאנלים לווידג'ט), `forge/HandArt.kt` (7 סגנונות מחוגים כתמונה), `forge/Sources.kt` (GoogleFonts, AiImages + מונה יומי, Textures/Poly Haven), `ui/ForgePanel.kt` (לשונית "מחולל" ראשונה בעורך).
- מודל: `LayerKind.DATA` (ערך חי), `GAUGE` (קשת/מחט לפי `DataSource`), HANDS עם `text`=סגנון → 3 slots. `FaceXml.slots()` מחליף את `imageLayers`. facecheck עובר (כולל Styled).
- האמנות נאפית ל-PNG אחד (900px) כשכבת IMAGE נעולה; החלקים החיים נשארים שכבות.
- פונט Google רק באמנות האפויה; השעה החיה בשעון עדיין SYNC_TO_DEVICE (G2: BitmapFont).
- רשת הקונטיינר: Poly Haven/Openverse/Pollinations חסומים לבדיקה; Google Fonts CSS עובד. Robolectric (NATIVE graphics) ב-scratch לרינדור תצוגות — Maven Central זורק 429 לפעמים.

## נכסים ותבניות (2026-10-28)
- Iconify (`editor/Iconify.kt`): חיפוש/עיון ב-200K+ אייקונים, 13 ספריות נבחרות (אימוג'י 3D, Noto, מזג אוויר, דגלים, Phosphor…). מזהה שכבה `prefix:name`, SVG נשמר ב-`filesDir/icons`, רינדור AndroidSVG. חד-צבעי (currentColor) נצבע בצבע השכבה; צבעוני נשאר.
- Iconify חסום ברשת הקונטיינר; npm פתוח → `npm pack @iconify-json/<set>` לבדיקת שמות אייקונים.
- תבניות (`editor/Presets.kt`): 10 ערכות (`Themes`) × 14 פריסות פני שעון (+10 מקוריות = 150), × 10 פריסות ווידג'ט (100). רקע גרדיאנט (`background2`).
- facecheck מאמת את כל 151 הפנים מול ה-XSD — עובר.

## Widget Studio (W1)
- אותו עורך (`EditorScreen(Studio.WIDGET)`), קנבס מלבן מעוגל: 2×2 / 4×2 / 4×4 / 4×1 (`FaceDoc.w/h/round/corner`). ציור משותף: `editor/Render.kt` (`drawDoc`).
- ווידג'ט היברידי (`widget/StudioWidget.kt`, Glance 1.2.0): כל העיצוב נאפה לתמונה; TIME/DATE = TextClock חי מעל (`res/layout/wclock_0..4` לפי משקל). אין מחוגים בווידג'טים.
- כל ווידג'ט במסך שומר עיצוב משלו (`widgets/placed/<id>.json`); ווידג'ט חדש לוקח את העיצוב הפתוח. כפתורים: הוספה למסך הבית, עדכון כל הווידג'טים.
- הבא: מסך הגדרת ווידג'ט (בחירת עיצוב לכל ווידג'ט, reconfigurable), נתונים חיים (סוללה, מזג אוויר), Motion/אפקטים.

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
2026-10-28: לוגו, עורך פני שעון, נכסים. אחר כך: Iconify, 150 תבניות פנים, Widget Studio (W1 בנוי).
