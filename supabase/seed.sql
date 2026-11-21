-- Smart Exam Academy — sample content so the app is not empty.
-- Run AFTER schema.sql. Idempotent (fixed ids + on conflict do nothing).

insert into public.categories (id, name, name_hi, sort) values
  ('10000000-0000-0000-0000-000000000001', 'SSC',                 'एसएससी',               1),
  ('10000000-0000-0000-0000-000000000002', 'Railway',             'रेलवे',                 2),
  ('10000000-0000-0000-0000-000000000003', 'Banking',             'बैंकिंग',                3),
  ('10000000-0000-0000-0000-000000000004', 'BPSC / State PSC',    'बीपीएससी / राज्य पीएससी', 4),
  ('10000000-0000-0000-0000-000000000005', 'UPSC',                'यूपीएससी',               5),
  ('10000000-0000-0000-0000-000000000006', 'Teaching / CTET',     'शिक्षक / सीटीईटी',        6),
  ('10000000-0000-0000-0000-000000000007', 'Defence',             'रक्षा',                  7),
  ('10000000-0000-0000-0000-000000000008', 'Police',              'पुलिस',                  8),
  ('10000000-0000-0000-0000-000000000009', 'LNMU BA University',  'एलएनएमयू बीए',           9)
on conflict (id) do nothing;

insert into public.exams (id, category_id, name, name_hi, description, is_active, sort) values
  ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001',
   'SSC CGL', 'एसएससी सीजीएल', 'Combined Graduate Level — Central Level', true, 1),
  ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002',
   'RRB NTPC', 'आरआरबी एनटीपीसी', 'Non-Technical Popular Categories', true, 1),
  ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004',
   'BPSC TRE 4.0', 'बीपीएससी शिक्षक भर्ती 4.0', 'Bihar Public Service Commission — Teacher Recruitment', true, 1)
on conflict (id) do nothing;

insert into public.packs (id, exam_id, title, title_hi, is_free, price, sort) values
  ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
   'GK & Reasoning (Chapter Wise)', 'सामान्य ज्ञान और रीज़निंग (अध्याय अनुसार)', true, 0, 1),
  ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001',
   'SSC CGL Full Mock (CBT 1 + CBT 2)', 'एसएससी सीजीएल फुल मॉक (CBT 1 + CBT 2)', false, 199, 2)
on conflict (id) do nothing;

insert into public.tests (id, pack_id, title, duration_min, total_marks, marks_correct, marks_wrong,
                          language, is_free, is_published, sort, instructions) values
  ('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
   'Set-01: GK + Reasoning Mix', 10, 20, 2, 0.5, 'both', true, true, 1,
   'Each correct answer carries +2 marks; each wrong answer -0.5. Calculators are not allowed. The test submits itself when the timer reaches zero.')
on conflict (id) do nothing;

insert into public.sections (id, test_id, name, sort) values
  ('50000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'General Awareness', 1),
  ('50000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'Reasoning & Maths', 2)
on conflict (id) do nothing;

insert into public.questions (id, test_id, section_id, q_en, q_hi, options_en, options_hi,
                              correct_index, explanation_en, explanation_hi, difficulty, sort) values
('60000000-0000-0000-0000-000000000001','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
 'Who is known as the "Father of the Indian Constitution"?',
 '"भारतीय संविधान के जनक" के रूप में किसे जाना जाता है?',
 '["Mahatma Gandhi","Dr. B. R. Ambedkar","Jawaharlal Nehru","Sardar Vallabhbhai Patel"]',
 '["महात्मा गांधी","डॉ. बी. आर. आंबेडकर","जवाहरलाल नेहरू","सरदार वल्लभभाई पटेल"]',
 1, 'Dr. B. R. Ambedkar chaired the Drafting Committee of the Constituent Assembly (1947).',
 'डॉ. बी. आर. आंबेडकर संविधान सभा की प्रारूप समिति (1947) के अध्यक्ष थे।', 'easy', 1),
('60000000-0000-0000-0000-000000000002','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
 'Which Article of the Indian Constitution abolishes untouchability?',
 'भारतीय संविधान का कौन-सा अनुच्छेद अस्पृश्यता (छुआछूत) का उन्मूलन करता है?',
 '["Article 14","Article 15","Article 17","Article 21"]',
 '["अनुच्छेद 14","अनुच्छेद 15","अनुच्छेद 17","अनुच्छेद 21"]',
 2, 'Article 17 abolishes untouchability and forbids its practice in any form.',
 'अनुच्छेद 17 अस्पृश्यता का अंत करता है और किसी भी रूप में इसके आचरण को निषिद्ध करता है।', 'medium', 2),
('60000000-0000-0000-0000-000000000003','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
 'In which year was the Battle of Plassey fought?',
 'प्लासी का युद्ध किस वर्ष लड़ा गया था?',
 '["1757","1764","1857","1761"]',
 '["1757","1764","1857","1761"]',
 0, 'The Battle of Plassey was fought on 23 June 1757 between Robert Clive and Siraj-ud-Daulah.',
 'प्लासी का युद्ध 23 जून 1757 को रॉबर्ट क्लाइव और सिराजुद्दौला के बीच लड़ा गया था।', 'easy', 3),
('60000000-0000-0000-0000-000000000004','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
 'Which is the longest river flowing within India?',
 'भारत में बहने वाली सबसे लंबी नदी कौन-सी है?',
 '["Godavari","Ganga","Yamuna","Brahmaputra"]',
 '["गोदावरी","गंगा","यमुना","ब्रह्मपुत्र"]',
 1, 'The Ganga (about 2,525 km) is the longest river in India; the Godavari is the longest peninsular river.',
 'गंगा (लगभग 2,525 किमी) भारत की सबसे लंबी नदी है; गोदावरी प्रायद्वीपीय भारत की सबसे लंबी नदी है।', 'easy', 4),
('60000000-0000-0000-0000-000000000005','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
 'Which gas is the most abundant in the Earth''s atmosphere?',
 'पृथ्वी के वायुमंडल में सबसे अधिक मात्रा में कौन-सी गैस पाई जाती है?',
 '["Oxygen","Carbon dioxide","Nitrogen","Argon"]',
 '["ऑक्सीजन","कार्बन डाइऑक्साइड","नाइट्रोजन","आर्गन"]',
 2, 'Nitrogen makes up about 78% of dry air by volume; oxygen is about 21%.',
 'शुष्क वायु में आयतन के अनुसार नाइट्रोजन लगभग 78% है; ऑक्सीजन लगभग 21% है।', 'easy', 5),
('60000000-0000-0000-0000-000000000006','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000001',
 'Deficiency of Vitamin C causes which disease?',
 'विटामिन C की कमी से कौन-सा रोग होता है?',
 '["Rickets","Scurvy","Night blindness","Beriberi"]',
 '["रिकेट्स","स्कर्वी","रतौंधी","बेरी-बेरी"]',
 1, 'Scurvy (bleeding gums, weakness) is caused by lack of Vitamin C. Rickets – Vitamin D; Night blindness – Vitamin A; Beriberi – Vitamin B1.',
 'स्कर्वी (मसूड़ों से खून, कमज़ोरी) विटामिन C की कमी से होता है। रिकेट्स – विटामिन D; रतौंधी – विटामिन A; बेरी-बेरी – विटामिन B1।', 'medium', 6),
('60000000-0000-0000-0000-000000000007','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000002',
 'If 20% of a number is 50, what is the number?',
 'यदि किसी संख्या का 20% 50 है, तो वह संख्या क्या है?',
 '["100","200","250","500"]',
 '["100","200","250","500"]',
 2, 'Number = 50 × 100 / 20 = 250.',
 'संख्या = 50 × 100 / 20 = 250।', 'easy', 7),
('60000000-0000-0000-0000-000000000008','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000002',
 'Find the next number in the series: 2, 6, 12, 20, 30, ?',
 'श्रृंखला में अगली संख्या ज्ञात कीजिए: 2, 6, 12, 20, 30, ?',
 '["40","42","44","36"]',
 '["40","42","44","36"]',
 1, 'Differences are 4, 6, 8, 10, so the next difference is 12: 30 + 12 = 42. (Also n × (n+1): 6 × 7 = 42.)',
 'अंतर 4, 6, 8, 10 हैं, इसलिए अगला अंतर 12 होगा: 30 + 12 = 42। (या n × (n+1): 6 × 7 = 42)', 'medium', 8),
('60000000-0000-0000-0000-000000000009','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000002',
 'A train 150 m long passes a pole in 15 seconds. What is its speed in km/h?',
 '150 मीटर लंबी एक ट्रेन एक खंभे को 15 सेकंड में पार करती है। उसकी चाल किमी/घंटा में कितनी है?',
 '["36 km/h","40 km/h","45 km/h","54 km/h"]',
 '["36 किमी/घंटा","40 किमी/घंटा","45 किमी/घंटा","54 किमी/घंटा"]',
 0, 'Speed = 150/15 = 10 m/s = 10 × 18/5 = 36 km/h.',
 'चाल = 150/15 = 10 मी/से = 10 × 18/5 = 36 किमी/घंटा।', 'hard', 9),
('60000000-0000-0000-0000-000000000010','40000000-0000-0000-0000-000000000001','50000000-0000-0000-0000-000000000002',
 'Choose the odd one out: Apple, Mango, Potato, Banana',
 'असंगत को चुनिए: सेब, आम, आलू, केला',
 '["Apple","Mango","Potato","Banana"]',
 '["सेब","आम","आलू","केला"]',
 2, 'Potato is a vegetable (a stem tuber); the others are fruits.',
 'आलू एक सब्ज़ी (तना कंद) है; बाकी सभी फल हैं।', 'easy', 10)
on conflict (id) do nothing;
