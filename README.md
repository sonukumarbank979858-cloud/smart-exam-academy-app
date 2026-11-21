# Smart Exam Academy

प्रतियोगी परीक्षाओं (SSC, Railway, Banking, BPSC/State PSC, UPSC, CTET, Defence, Police, LNMU BA) की टेस्ट-सीरीज़ का Android ऐप + वेब एडमिन पैनल, Supabase पर।

- **APK**: हर push पर GitHub Actions से बनता है → **Releases** में `SmartExamAcademy-v1.0.<run>.apk`
- **एडमिन पैनल**: GitHub Pages → `https://sonukumarbank979858-cloud.github.io/smart-exam-academy-app/`

## ऐप (Kotlin + Jetpack Compose)
- लॉग इन: ईमेल OTP — ईमेल का “Sign in” लिंक (`smartexam://login-callback`) या 6 अंकों का कोड; पहली बार प्रोफ़ाइल फ़ॉर्म
- होम: “नमस्ते <नाम>”, परीक्षा खोज, बैनर कैरोसेल, “अपनी परीक्षा चुनें”, मेरी परीक्षाएँ
- परीक्षा → टेस्ट पैक (FREE/PAID) → टेस्ट सूची → निर्देश (English/हिंदी) → टेस्ट (टाइमर, पैलेट, रिव्यू मार्क, ऑटो-सबमिट)
- रिज़ल्ट, विषय/कठिनाई-वार विश्लेषण, उत्तर समीक्षा (फ़िल्टर, व्याख्या), लीडरबोर्ड, दोबारा प्रयास, प्रयास इतिहास
- ऑफ़लाइन टेस्ट (डाउनलोड करें, बाद में सिंक), प्रोफ़ाइल, थीम सिस्टम/दिन/रात

स्कोर सर्वर पर `submit_attempt` RPC से जाँचा जाता है।

## Supabase
- `supabase/schema.sql` — टेबल, RLS, व्यू (`test_stats`, `pack_stats`, `exam_stats`), RPC (`submit_attempt`, `test_leaderboard`, `admin_stats`, `admin_users`, `is_admin`). दोबारा चलाना सुरक्षित है।
- `supabase/seed.sql` — 9 श्रेणियाँ, 3 परीक्षाएँ, 1 सैंपल टेस्ट (10 द्विभाषी प्रश्न)।

URL / key: `app/build.gradle.kts` में डिफ़ॉल्ट हैं; GitHub secrets `SUPABASE_URL`, `SUPABASE_ANON_KEY` (या `gradle -P…`) से बदले जा सकते हैं। एडमिन पैनल `admin/config.js` पढ़ता है (workflow secrets से दोबारा बनाता है)।

## लोकल बिल्ड
```
gradle :app:assembleRelease          # KEYSTORE_PATH/KEYSTORE_PASSWORD न हों तो debug key से साइन
```

## प्रश्न इम्पोर्ट (एडमिन → प्रश्न → CSV/JSON)
कॉलम: `q_en,q_hi,a,b,c,d,a_hi,b_hi,c_hi,d_hi,correct,explanation_en,explanation_hi,difficulty,section` — `correct` = A/B/C/D या 1-4, `difficulty` = easy/medium/hard, `section` नाम (न हो तो बन जाता है)।
