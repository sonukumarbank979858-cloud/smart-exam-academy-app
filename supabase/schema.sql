-- Smart Exam Academy — Supabase schema
-- Idempotent: safe to run again in the SQL editor (Dashboard → SQL Editor → paste → Run).

create extension if not exists pgcrypto;

-- ───────────────────────── Tables ─────────────────────────

create table if not exists public.admins (
  email text primary key
);

create table if not exists public.categories (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  name_hi text,
  icon_url text,
  sort int not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.exams (
  id uuid primary key default gen_random_uuid(),
  category_id uuid references public.categories(id) on delete set null,
  name text not null,
  name_hi text,
  description text,
  logo_url text,
  is_active boolean not null default true,
  sort int not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.packs (
  id uuid primary key default gen_random_uuid(),
  exam_id uuid not null references public.exams(id) on delete cascade,
  title text not null,
  title_hi text,
  is_free boolean not null default true,
  price int not null default 0,
  sort int not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.tests (
  id uuid primary key default gen_random_uuid(),
  pack_id uuid not null references public.packs(id) on delete cascade,
  title text not null,
  duration_min int not null default 15,
  total_marks numeric not null default 0,
  marks_correct numeric not null default 1,
  marks_wrong numeric not null default 0.25,
  language text not null default 'both' check (language in ('en','hi','both')),
  is_free boolean not null default true,
  is_published boolean not null default false,
  sort int not null default 0,
  instructions text,
  created_at timestamptz not null default now()
);

create table if not exists public.sections (
  id uuid primary key default gen_random_uuid(),
  test_id uuid not null references public.tests(id) on delete cascade,
  name text not null,
  sort int not null default 0
);

create table if not exists public.questions (
  id uuid primary key default gen_random_uuid(),
  test_id uuid not null references public.tests(id) on delete cascade,
  section_id uuid references public.sections(id) on delete set null,
  q_en text,
  q_hi text,
  options_en jsonb not null default '[]'::jsonb,
  options_hi jsonb not null default '[]'::jsonb,
  correct_index int not null default 0 check (correct_index between 0 and 9),
  explanation_en text,
  explanation_hi text,
  difficulty text not null default 'medium' check (difficulty in ('easy','medium','hard')),
  sort int not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.banners (
  id uuid primary key default gen_random_uuid(),
  image_url text not null,
  link text,
  sort int not null default 0,
  active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  phone text,
  state text,
  district text,
  category text,
  created_at timestamptz not null default now()
);

create table if not exists public.attempts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  test_id uuid not null references public.tests(id) on delete cascade,
  answers jsonb not null default '{}'::jsonb,
  score numeric not null default 0,
  correct int not null default 0,
  wrong int not null default 0,
  skipped int not null default 0,
  time_taken_sec int not null default 0,
  created_at timestamptz not null default now()
);

create index if not exists exams_category_idx on public.exams(category_id);
create index if not exists packs_exam_idx on public.packs(exam_id);
create index if not exists tests_pack_idx on public.tests(pack_id);
create index if not exists sections_test_idx on public.sections(test_id);
create index if not exists questions_test_idx on public.questions(test_id);
create index if not exists attempts_user_idx on public.attempts(user_id, created_at desc);
create index if not exists attempts_test_idx on public.attempts(test_id);

-- ───────────────────────── Seed admins ─────────────────────────
insert into public.admins(email) values
  ('sonukumarbank979858@gmail.com'),
  ('classessonu65@gmail.com')
on conflict do nothing;

-- ───────────────────────── Helpers ─────────────────────────

create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public
as $$
  select exists (
    select 1 from public.admins a
    where lower(a.email) = lower(coalesce(auth.jwt() ->> 'email', ''))
  );
$$;
grant execute on function public.is_admin() to anon, authenticated;

-- ───────────────────────── Row Level Security ─────────────────────────

alter table public.admins     enable row level security;
alter table public.categories enable row level security;
alter table public.exams      enable row level security;
alter table public.packs      enable row level security;
alter table public.tests      enable row level security;
alter table public.sections   enable row level security;
alter table public.questions  enable row level security;
alter table public.banners    enable row level security;
alter table public.profiles   enable row level security;
alter table public.attempts   enable row level security;

-- admins: an admin can see/manage the list; anyone can check only their own row
drop policy if exists admins_select on public.admins;
create policy admins_select on public.admins for select to authenticated
  using (lower(email) = lower(coalesce(auth.jwt() ->> 'email','')) or public.is_admin());
drop policy if exists admins_write on public.admins;
create policy admins_write on public.admins for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- categories: public read
drop policy if exists categories_read on public.categories;
create policy categories_read on public.categories for select to anon, authenticated using (true);
drop policy if exists categories_admin on public.categories;
create policy categories_admin on public.categories for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- exams: active ones are public; admins see all
drop policy if exists exams_read on public.exams;
create policy exams_read on public.exams for select to anon, authenticated
  using (is_active or public.is_admin());
drop policy if exists exams_admin on public.exams;
create policy exams_admin on public.exams for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- packs: visible if the exam is active
drop policy if exists packs_read on public.packs;
create policy packs_read on public.packs for select to anon, authenticated
  using (public.is_admin() or exists (select 1 from public.exams e where e.id = exam_id and e.is_active));
drop policy if exists packs_admin on public.packs;
create policy packs_admin on public.packs for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- tests: published only
drop policy if exists tests_read on public.tests;
create policy tests_read on public.tests for select to anon, authenticated
  using (is_published or public.is_admin());
drop policy if exists tests_admin on public.tests;
create policy tests_admin on public.tests for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- sections / questions: visible when their test is published
drop policy if exists sections_read on public.sections;
create policy sections_read on public.sections for select to anon, authenticated
  using (public.is_admin() or exists (select 1 from public.tests t where t.id = test_id and t.is_published));
drop policy if exists sections_admin on public.sections;
create policy sections_admin on public.sections for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists questions_read on public.questions;
create policy questions_read on public.questions for select to anon, authenticated
  using (public.is_admin() or exists (select 1 from public.tests t where t.id = test_id and t.is_published));
drop policy if exists questions_admin on public.questions;
create policy questions_admin on public.questions for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- banners: active only
drop policy if exists banners_read on public.banners;
create policy banners_read on public.banners for select to anon, authenticated
  using (active or public.is_admin());
drop policy if exists banners_admin on public.banners;
create policy banners_admin on public.banners for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- profiles: own row (admins may read all for the users list)
drop policy if exists profiles_own_select on public.profiles;
create policy profiles_own_select on public.profiles for select to authenticated
  using (id = auth.uid() or public.is_admin());
drop policy if exists profiles_own_insert on public.profiles;
create policy profiles_own_insert on public.profiles for insert to authenticated
  with check (id = auth.uid());
drop policy if exists profiles_own_update on public.profiles;
create policy profiles_own_update on public.profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());
drop policy if exists profiles_own_delete on public.profiles;
create policy profiles_own_delete on public.profiles for delete to authenticated
  using (id = auth.uid());

-- attempts: own rows (admins may read all for counts)
drop policy if exists attempts_own_select on public.attempts;
create policy attempts_own_select on public.attempts for select to authenticated
  using (user_id = auth.uid() or public.is_admin());
drop policy if exists attempts_own_insert on public.attempts;
create policy attempts_own_insert on public.attempts for insert to authenticated
  with check (user_id = auth.uid());
drop policy if exists attempts_own_delete on public.attempts;
create policy attempts_own_delete on public.attempts for delete to authenticated
  using (user_id = auth.uid());

grant usage on schema public to anon, authenticated;
grant select on all tables in schema public to anon, authenticated;
grant insert, update, delete on all tables in schema public to authenticated;

-- ───────────────────────── Count views (RLS-aware) ─────────────────────────

create or replace view public.test_stats with (security_invoker = true) as
  select t.id as test_id,
         t.pack_id,
         (select count(*) from public.questions q where q.test_id = t.id)::int as question_count,
         (select count(*) from public.sections s where s.test_id = t.id)::int as section_count
  from public.tests t;

create or replace view public.pack_stats with (security_invoker = true) as
  select p.id as pack_id,
         p.exam_id,
         (select count(*) from public.tests t where t.pack_id = p.id)::int as test_count
  from public.packs p;

create or replace view public.exam_stats with (security_invoker = true) as
  select e.id as exam_id,
         (select count(*) from public.packs p where p.exam_id = e.id)::int as pack_count,
         (select count(*) from public.tests t join public.packs p on p.id = t.pack_id where p.exam_id = e.id)::int as test_count
  from public.exams e;

grant select on public.test_stats, public.pack_stats, public.exam_stats to anon, authenticated;

-- ───────────────────────── RPCs ─────────────────────────

-- Grades an attempt on the server (the score cannot be faked by the client) and stores it.
-- p_answers: {"<question_id>": <chosen_index>, ...}  (unanswered questions omitted)
create or replace function public.submit_attempt(p_test_id uuid, p_answers jsonb, p_time_sec int)
returns public.attempts
language plpgsql security definer set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_test public.tests;
  v_correct int := 0; v_wrong int := 0; v_skipped int := 0;
  r record;
  v_row public.attempts;
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  select * into v_test from public.tests where id = p_test_id and (is_published or public.is_admin());
  if not found then raise exception 'test not found'; end if;
  for r in select id, correct_index from public.questions where test_id = p_test_id loop
    if p_answers ? r.id::text and jsonb_typeof(p_answers -> r.id::text) = 'number' then
      if (p_answers ->> r.id::text)::int = r.correct_index then v_correct := v_correct + 1;
      else v_wrong := v_wrong + 1; end if;
    else
      v_skipped := v_skipped + 1;
    end if;
  end loop;
  insert into public.attempts(user_id, test_id, answers, score, correct, wrong, skipped, time_taken_sec)
  values (v_uid, p_test_id, coalesce(p_answers,'{}'::jsonb),
          round(v_correct * v_test.marks_correct - v_wrong * v_test.marks_wrong, 2),
          v_correct, v_wrong, v_skipped, greatest(coalesce(p_time_sec,0),0))
  returning * into v_row;
  return v_row;
end $$;
grant execute on function public.submit_attempt(uuid, jsonb, int) to authenticated;

-- Top scores for a test (first attempt per user), names only.
create or replace function public.test_leaderboard(p_test_id uuid, p_limit int default 20)
returns table(rank bigint, full_name text, score numeric, time_taken_sec int, is_me boolean)
language sql stable security definer set search_path = public
as $$
  with firsts as (
    select distinct on (a.user_id) a.user_id, a.score, a.time_taken_sec
    from public.attempts a where a.test_id = p_test_id
    order by a.user_id, a.created_at asc
  )
  select row_number() over (order by f.score desc, f.time_taken_sec asc) as rank,
         coalesce(nullif(p.full_name,''), 'Student') as full_name,
         f.score, f.time_taken_sec, f.user_id = auth.uid() as is_me
  from firsts f left join public.profiles p on p.id = f.user_id
  order by f.score desc, f.time_taken_sec asc
  limit greatest(1, least(coalesce(p_limit,20), 100));
$$;
grant execute on function public.test_leaderboard(uuid, int) to authenticated;

-- Admin dashboard numbers.
create or replace function public.admin_stats()
returns json
language plpgsql stable security definer set search_path = public
as $$
begin
  if not public.is_admin() then raise exception 'admins only'; end if;
  return json_build_object(
    'users', (select count(*) from auth.users),
    'profiles', (select count(*) from public.profiles),
    'attempts', (select count(*) from public.attempts),
    'tests', (select count(*) from public.tests),
    'questions', (select count(*) from public.questions)
  );
end $$;
grant execute on function public.admin_stats() to authenticated;

-- Admin users list with attempt counts.
create or replace function public.admin_users()
returns table(id uuid, email text, full_name text, phone text, state text, district text,
              category text, created_at timestamptz, last_sign_in_at timestamptz, attempts bigint)
language plpgsql stable security definer set search_path = public
as $$
begin
  if not public.is_admin() then raise exception 'admins only'; end if;
  return query
    select u.id, u.email::text, p.full_name, p.phone, p.state, p.district, p.category,
           u.created_at, u.last_sign_in_at,
           (select count(*) from public.attempts a where a.user_id = u.id)
    from auth.users u left join public.profiles p on p.id = u.id
    order by u.created_at desc;
end $$;
grant execute on function public.admin_users() to authenticated;

-- ═════════════════════════ v2: notifications, students, AI context ═════════════════════════
-- Idempotent. Applied after the base schema.

-- Profiles: extra sign-up fields
alter table public.profiles add column if not exists email text;
alter table public.profiles add column if not exists exam_interest text;

-- New auth users get a profile from their sign-up metadata (never blocks a sign-up)
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  begin
    insert into public.profiles(id, email, full_name, phone, state, exam_interest)
    values (new.id, new.email,
            nullif(new.raw_user_meta_data ->> 'full_name', ''),
            nullif(new.raw_user_meta_data ->> 'phone', ''),
            nullif(new.raw_user_meta_data ->> 'state', ''),
            nullif(new.raw_user_meta_data ->> 'exam_interest', ''))
    on conflict (id) do nothing;
  exception when others then null;
  end;
  return new;
end $$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- ───────── Notifications ─────────
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade,   -- null = broadcast
  audience text not null default 'all' check (audience in ('all','user','admins')),
  title text not null,
  body text,
  type text not null default 'info',
  data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists notifications_created_idx on public.notifications(created_at desc);
create index if not exists notifications_user_idx on public.notifications(user_id, created_at desc);

create table if not exists public.notification_reads (
  notification_id uuid not null references public.notifications(id) on delete cascade,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  read_at timestamptz not null default now(),
  primary key (notification_id, user_id)
);

alter table public.notifications enable row level security;
alter table public.notification_reads enable row level security;

drop policy if exists notifications_read on public.notifications;
create policy notifications_read on public.notifications for select to authenticated
  using (audience = 'all' or user_id = auth.uid() or (audience = 'admins' and public.is_admin()));
drop policy if exists notifications_admin on public.notifications;
create policy notifications_admin on public.notifications for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists notification_reads_own on public.notification_reads;
create policy notification_reads_own on public.notification_reads for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

grant select, insert, update, delete on public.notifications, public.notification_reads to authenticated;

-- Max marks of a test (total_marks, else questions × marks_correct)
create or replace function public.test_max_marks(p_test_id uuid)
returns numeric language sql stable security definer set search_path = public
as $$
  select case when t.total_marks > 0 then t.total_marks
              else (select count(*) from public.questions q where q.test_id = t.id) * t.marks_correct end
  from public.tests t where t.id = p_test_id;
$$;
grant execute on function public.test_max_marks(uuid) to authenticated;

-- Trigger: test published → broadcast
create or replace function public.notify_test_published()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
  if new.is_published and (tg_op = 'INSERT' or not coalesce(old.is_published, false)) then
    insert into public.notifications(user_id, audience, title, body, type, data)
    values (null, 'all', 'नया टेस्ट: ' || new.title,
            'नया टेस्ट उपलब्ध है — ' || new.duration_min || ' मिनट। अभी अभ्यास शुरू करें!',
            'new_test', jsonb_build_object('test_id', new.id));
  end if;
  return new;
end $$;
drop trigger if exists tests_publish_notify on public.tests;
create trigger tests_publish_notify after insert or update of is_published on public.tests
  for each row execute function public.notify_test_published();

-- Trigger: attempt submitted → personal + admins
create or replace function public.notify_attempt()
returns trigger language plpgsql security definer set search_path = public
as $$
declare
  v_title text; v_max numeric; v_name text; v_email text;
begin
  begin
    select title into v_title from public.tests where id = new.test_id;
    v_max := public.test_max_marks(new.test_id);
    select coalesce(nullif(p.full_name, ''), split_part(u.email, '@', 1)), u.email
      into v_name, v_email
      from auth.users u left join public.profiles p on p.id = u.id where u.id = new.user_id;
    insert into public.notifications(user_id, audience, title, body, type, data)
    values (new.user_id, 'user', 'परिणाम: ' || coalesce(v_title, 'टेस्ट'),
            'आपने ' || coalesce(v_title, 'टेस्ट') || ' में ' || trim(to_char(new.score, 'FM999990.##')) || '/' ||
              trim(to_char(coalesce(v_max, 0), 'FM999990.##')) || ' अंक पाए',
            'result', jsonb_build_object('test_id', new.test_id, 'attempt_id', new.id));
    insert into public.notifications(user_id, audience, title, body, type, data)
    values (null, 'admins', 'छात्र ने टेस्ट दिया: ' || coalesce(v_name, 'Student'),
            coalesce(v_name, 'Student') || ' (' || coalesce(v_email, '') || ') ने ' || coalesce(v_title, 'टेस्ट') || ' दिया — स्कोर ' ||
              trim(to_char(new.score, 'FM999990.##')) || '/' || trim(to_char(coalesce(v_max, 0), 'FM999990.##')) ||
              ' (सही ' || new.correct || ', ग़लत ' || new.wrong || ')',
            'submission', jsonb_build_object('test_id', new.test_id, 'attempt_id', new.id, 'user_id', new.user_id));
  exception when others then null;
  end;
  return new;
end $$;
drop trigger if exists attempts_notify on public.attempts;
create trigger attempts_notify after insert on public.attempts
  for each row execute function public.notify_attempt();

-- My notifications with read state
create or replace function public.my_notifications(p_limit int default 50)
returns table(id uuid, title text, body text, type text, data jsonb, created_at timestamptz, is_read boolean)
language sql stable security definer set search_path = public
as $$
  select n.id, n.title, n.body, n.type, n.data, n.created_at,
         exists (select 1 from public.notification_reads r where r.notification_id = n.id and r.user_id = auth.uid())
  from public.notifications n
  where auth.uid() is not null
    and (n.audience = 'all' or n.user_id = auth.uid() or (n.audience = 'admins' and public.is_admin()))
    and n.created_at >= coalesce((select u.created_at from auth.users u where u.id = auth.uid()), now()) - interval '30 days'
  order by n.created_at desc
  limit greatest(1, least(coalesce(p_limit, 50), 200));
$$;
grant execute on function public.my_notifications(int) to authenticated;

create or replace function public.mark_notifications_read(p_ids uuid[] default null)
returns int language plpgsql security definer set search_path = public
as $$
declare v_n int;
begin
  if auth.uid() is null then raise exception 'not signed in'; end if;
  insert into public.notification_reads(notification_id, user_id)
  select n.id, auth.uid() from public.notifications n
  where (p_ids is null or n.id = any(p_ids))
    and (n.audience = 'all' or n.user_id = auth.uid() or (n.audience = 'admins' and public.is_admin()))
  on conflict do nothing;
  get diagnostics v_n = row_count;
  return v_n;
end $$;
grant execute on function public.mark_notifications_read(uuid[]) to authenticated;

-- ───────── Test store (all published tests with pack + exam) ─────────
create or replace view public.store_tests with (security_invoker = true) as
  select t.id, t.title, t.duration_min, t.total_marks, t.marks_correct, t.is_free, t.language, t.created_at, t.sort,
         p.id as pack_id, p.title as pack_title, p.is_free as pack_free,
         e.id as exam_id, e.name as exam_name, e.name_hi as exam_name_hi, e.logo_url as exam_logo,
         c.id as category_id, c.name as category_name, c.name_hi as category_name_hi,
         (select count(*) from public.questions q where q.test_id = t.id)::int as question_count
  from public.tests t
  join public.packs p on p.id = t.pack_id
  join public.exams e on e.id = p.exam_id
  left join public.categories c on c.id = e.category_id
  where t.is_published and e.is_active;
grant select on public.store_tests to anon, authenticated;

-- ───────── Admin: students ─────────
drop function if exists public.admin_students();
create or replace function public.admin_students()
returns table(id uuid, email text, full_name text, phone text, state text, exam_interest text,
              created_at timestamptz, last_active timestamptz, tests_taken bigint, avg_score numeric, avg_pct numeric)
language plpgsql stable security definer set search_path = public
as $$
begin
  if not public.is_admin() then raise exception 'admins only'; end if;
  return query
    select u.id, u.email::text, p.full_name, p.phone, p.state, p.exam_interest, u.created_at,
           greatest(u.last_sign_in_at, (select max(a.created_at) from public.attempts a where a.user_id = u.id)),
           (select count(*) from public.attempts a where a.user_id = u.id),
           (select round(avg(a.score), 2) from public.attempts a where a.user_id = u.id),
           (select round(avg(case when public.test_max_marks(a.test_id) > 0
                                  then a.score * 100 / public.test_max_marks(a.test_id) end), 1)
              from public.attempts a where a.user_id = u.id)
    from auth.users u left join public.profiles p on p.id = u.id
    order by 8 desc nulls last;
end $$;
grant execute on function public.admin_students() to authenticated;

drop function if exists public.admin_student_attempts(uuid);
create or replace function public.admin_student_attempts(p_user uuid)
returns table(id uuid, test_id uuid, test_title text, score numeric, max_marks numeric, correct int, wrong int,
              skipped int, time_taken_sec int, created_at timestamptz)
language plpgsql stable security definer set search_path = public
as $$
begin
  if not public.is_admin() then raise exception 'admins only'; end if;
  return query
    select a.id, a.test_id, t.title, a.score, public.test_max_marks(a.test_id), a.correct, a.wrong, a.skipped,
           a.time_taken_sec, a.created_at
    from public.attempts a left join public.tests t on t.id = a.test_id
    where a.user_id = p_user order by a.created_at desc;
end $$;
grant execute on function public.admin_student_attempts(uuid) to authenticated;

drop function if exists public.admin_recent_submissions(int);
create or replace function public.admin_recent_submissions(p_limit int default 50)
returns table(id uuid, user_id uuid, student text, email text, test_title text, score numeric, max_marks numeric,
              correct int, wrong int, time_taken_sec int, created_at timestamptz)
language plpgsql stable security definer set search_path = public
as $$
begin
  if not public.is_admin() then raise exception 'admins only'; end if;
  return query
    select a.id, a.user_id, coalesce(nullif(p.full_name, ''), split_part(u.email, '@', 1)), u.email::text, t.title,
           a.score, public.test_max_marks(a.test_id), a.correct, a.wrong, a.time_taken_sec, a.created_at
    from public.attempts a
    left join auth.users u on u.id = a.user_id
    left join public.profiles p on p.id = a.user_id
    left join public.tests t on t.id = a.test_id
    order by a.created_at desc limit greatest(1, least(coalesce(p_limit, 50), 500));
end $$;
grant execute on function public.admin_recent_submissions(int) to authenticated;

-- ───────── AI coach context (called by the ai-coach edge function with the user's JWT) ─────────
create or replace function public.ai_context()
returns json language plpgsql stable security definer set search_path = public
as $$
declare v_uid uuid := auth.uid(); v_admin boolean := public.is_admin(); v json;
begin
  if v_uid is null then raise exception 'not signed in'; end if;
  select json_build_object(
    'is_admin', v_admin,
    'me', (select json_build_object('name', p.full_name, 'email', u.email, 'state', p.state, 'exam_interest', p.exam_interest)
             from auth.users u left join public.profiles p on p.id = u.id where u.id = v_uid),
    'my_attempts', coalesce((select json_agg(x) from (
        select t.title as test, a.score, public.test_max_marks(a.test_id) as max, a.correct, a.wrong, a.skipped,
               a.time_taken_sec as time_sec, to_char(a.created_at at time zone 'Asia/Kolkata', 'YYYY-MM-DD HH24:MI') as at
        from public.attempts a left join public.tests t on t.id = a.test_id
        where a.user_id = v_uid order by a.created_at desc limit 30) x), '[]'::json),
    'available_tests', coalesce((select json_agg(x) from (
        select title, exam_name as exam, duration_min, question_count from public.store_tests order by created_at desc limit 40) x), '[]'::json),
    'students', case when v_admin then coalesce((select json_agg(x) from (
        select coalesce(nullif(p.full_name, ''), split_part(u.email, '@', 1)) as name, u.email, p.state, p.exam_interest,
               (select count(*) from public.attempts a where a.user_id = u.id) as tests_taken,
               (select json_agg(y) from (
                  select t.title as test, a.score, public.test_max_marks(a.test_id) as max, a.correct, a.wrong,
                         to_char(a.created_at at time zone 'Asia/Kolkata', 'YYYY-MM-DD HH24:MI') as at
                  from public.attempts a left join public.tests t on t.id = a.test_id
                  where a.user_id = u.id order by a.created_at desc limit 15) y) as attempts
        from auth.users u left join public.profiles p on p.id = u.id
        order by u.created_at desc limit 200) x), '[]'::json) else null end
  ) into v;
  return v;
end $$;
grant execute on function public.ai_context() to authenticated;

notify pgrst, 'reload schema';
