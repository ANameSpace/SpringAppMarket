import Link from "next/link";
import { Suspense } from "react";
import { getSession } from "@/lib/session";
import AdminPanel from "./admin-panel";

const loginUrl =
  process.env.OAUTH_LOGIN_URL ?? "/oauth2/authorization/gateway-client-oidc";

function AdminFallback() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <div className="h-4 w-40 animate-pulse rounded-full bg-border" />
      <div className="mt-4 h-9 w-64 animate-pulse rounded-xl bg-border" />
      <div className="mt-8 h-64 animate-pulse rounded-3xl bg-border/60" />
    </main>
  );
}

export default function AdminPage() {
  return (
    <Suspense fallback={<AdminFallback />}>
      <AdminContent />
    </Suspense>
  );
}

async function AdminContent() {
  const user = await getSession();

  if (!user) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-border bg-card p-8 text-center shadow-sm">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Администрирование
          </p>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground">
            Требуется вход
          </h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Админ-панель доступна только администраторам. Войдите под учётной
            записью с ролью ADMIN.
          </p>
          <a
            href={loginUrl}
            className="mt-6 inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
          >
            Войти
          </a>
        </section>
      </main>
    );
  }

  if (!user.roles.includes("ADMIN")) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-rose-500/30 bg-card p-8 text-center shadow-sm">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-rose-600 dark:text-rose-400">
            Доступ запрещён
          </p>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground">
            Нужна роль ADMIN
          </h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Вы вошли как <span className="font-medium text-foreground">{user.username}</span>.
            Этот раздел доступен только администраторам.
          </p>
          <Link
            href="/"
            className="mt-6 inline-flex h-10 items-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
          >
            На главную
          </Link>
        </section>
      </main>
    );
  }

  return <AdminPanel currentUser={user.username} />;
}
