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

-- Ask PostgREST to pick up the new objects right away.
notify pgrst, 'reload schema';
