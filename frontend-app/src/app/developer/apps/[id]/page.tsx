import { Suspense } from "react";
import Link from "next/link";
import { getSession } from "@/lib/session";
import AppEditor from "./app-editor";

const loginUrl =
  process.env.OAUTH_LOGIN_URL ?? "/oauth2/authorization/gateway-client-oidc";

function EditorFallback() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <div className="h-4 w-40 animate-pulse rounded-full bg-border" />
      <div className="mt-4 h-9 w-72 animate-pulse rounded-xl bg-border" />
      <div className="mt-8 grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <div className="h-80 animate-pulse rounded-3xl bg-border/60" />
        <div className="h-80 animate-pulse rounded-3xl bg-border/60" />
      </div>
    </main>
  );
}

export default function DeveloperAppPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <Suspense fallback={<EditorFallback />}>
      <DeveloperAppRoute params={params} />
    </Suspense>
  );
}

async function DeveloperAppRoute({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const [{ id }, user] = await Promise.all([params, getSession()]);

  if (!user) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-border bg-card p-8 text-center shadow-sm">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Редактор приложения
          </p>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground">
            Требуется вход
          </h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Управление приложением доступно только его разработчику. Войдите
            через gateway, чтобы продолжить.
          </p>
          <div className="mt-6 flex justify-center gap-2">
            <a
              href={loginUrl}
              className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              Войти
            </a>
            <Link
              href="/catalog"
              className="inline-flex h-10 items-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              В каталог
            </Link>
          </div>
        </section>
      </main>
    );
  }

  const isAdmin = user.roles.includes("ADMIN");
  const isDeveloper = user.roles.includes("DEVELOPER");

  if (!isAdmin && !isDeveloper) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-border bg-card p-8 text-center shadow-sm">
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">
            Нет доступа
          </h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Для управления приложениями нужна роль DEVELOPER. Её можно получить
            в кабинете разработчика.
          </p>
          <Link
            href="/developer"
            className="mt-6 inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
          >
            В кабинет разработчика
          </Link>
        </section>
      </main>
    );
  }

  return (
    <AppEditor
      appId={id}
      userId={user.userId}
      isAdmin={isAdmin}
      username={user.username}
    />
  );
}
