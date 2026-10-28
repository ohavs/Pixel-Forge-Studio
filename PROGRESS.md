# PROGRESS

## Now
שלב 0b — בדיקת היתכנות Watch Face Push (ראה docs/ROADMAP.md). Release אחרון: v1.0.7.

## החלטות
- AI = Gemini (מפתח מ-AI Studio). המשתמש מזין אותו בהגדרות ("מפתח Gemini"); לשמור על המכסה החינמית (מונה יומי).
- RTL כפוי בכל האפליקציה (`StudioTheme` → `LocalLayoutDirection.Rtl`) — המשתמש דורש יישור לימין מלא.
- כיוון עיצובי: כמו בסרטון — דינמי, נקי, מודרני, **הרבה הנפשות**. כלים: `ui/Motion.kt` (`enter(index)` כניסה מדורגת, `pressScale`), AnimatedContent בין מסכים, כרטיסים עם גרדיאנט חי (Canvas). מותר להוסיף ספריות (למשל Haze לטשטוש זכוכית, Lottie) כשצריך.
- CI: שגיאות build מוצגות כ-annotations (`/check-runs/<job>/annotations`, בלי auth) — לוגים לא נגישים מהקונטיינר.

## Secrets (שמות בלבד)
KEYSTORE_BASE64, KEYSTORE_PASSWORD, AI_API_KEY

## Last session
2026-10-28: שלד, עדכון עצמי, RTL, הנפשות בבית ובהגדרות. v1.0.7.
