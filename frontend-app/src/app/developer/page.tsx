import { Suspense } from "react";
import { getSession } from "@/lib/session";
import DeveloperCabinet from "./developer-cabinet";

const loginUrl =
  process.env.OAUTH_LOGIN_URL ?? "/oauth2/authorization/gateway-client-oidc";

function CabinetFallback() {
  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <div className="h-4 w-40 animate-pulse rounded-full bg-border" />
      <div className="mt-4 h-9 w-72 animate-pulse rounded-xl bg-border" />
      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <div className="h-48 animate-pulse rounded-3xl bg-border/60" />
        <div className="h-48 animate-pulse rounded-3xl bg-border/60" />
        <div className="h-48 animate-pulse rounded-3xl bg-border/60" />
      </div>
    </main>
  );
}

export default function DeveloperPage() {
  return (
    <Suspense fallback={<CabinetFallback />}>
      <DeveloperRoute />
    </Suspense>
  );
}

async function DeveloperRoute() {
  const user = await getSession();

  if (!user) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-border bg-card p-8 text-center shadow-sm">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Кабинет разработчика
          </p>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight text-foreground">
            Требуется вход
          </h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Чтобы управлять приложениями, войдите через gateway. Идентичность и
            роли передаёт user-service, у фронтенда собственной авторизации нет.
          </p>
          <div className="mt-6 flex justify-center gap-2">
            <a
              href={loginUrl}
              className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              Войти
            </a>
            <a
              href="/catalog"
              className="inline-flex h-10 items-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              В каталог
            </a>
          </div>
        </section>
      </main>
    );
  }

  return <DeveloperCabinet username={user.username} roles={user.roles} />;
}
