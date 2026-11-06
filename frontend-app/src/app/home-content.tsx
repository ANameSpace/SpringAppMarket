import type { ReactNode } from "react";
import { getSession } from "@/lib/session";

const loginUrl =
  process.env.OAUTH_LOGIN_URL ?? "/oauth2/authorization/gateway-client-oidc";
const docsUrl = process.env.API_DOCS_URL ?? "/swagger-ui.html";
const grafanaUrl = process.env.GRAFANA_URL ?? "/grafana/";
const adminUrl = process.env.ADMIN_PANEL_URL ?? "/admin";

const services = [
  { name: "gateway-server", note: "единственная публичная точка входа" },
  { name: "user-service", note: "OAuth 2.1 и профили" },
  { name: "app-service", note: "каталог приложений" },
  { name: "order-service", note: "заказы" },
  { name: "review-service", note: "отзывы" },
  { name: "grafana", note: "метрики, только ADMIN" },
];

const features = [
  {
    title: "Один публичный порт",
    body: "Снаружи доступен только gateway. Остальные сервисы общаются внутри Docker-сети и не публикуют порты.",
    icon: (
      <path d="M12 3 4.5 7v10L12 21l7.5-4V7L12 3Z M4.5 7 12 11l7.5-4 M12 11v10" />
    ),
  },
  {
    title: "OAuth 2.1 и роли",
    body: "Gateway — OAuth2-клиент. Роль ADMIN открывает Swagger и Grafana, обычный USER видит только публичные разделы.",
    icon: (
      <>
        <rect x="5" y="10" width="14" height="10" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3" />
        <path d="M12 14v2" />
      </>
    ),
  },
  {
    title: "Наблюдаемость из коробки",
    body: "Prometheus собирает метрики сервисов, Grafana строит дашборды. Доступ к ним также идёт через gateway.",
    icon: <path d="M4 19.5V13M9.3 19.5V8.5M14.7 19.5v-4M20 19.5V5" />,
  },
];

function Icon({ children }: { children: ReactNode }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-5 w-5"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}

function ServiceStack() {
  return (
    <div className="relative overflow-hidden rounded-3xl border border-border bg-card p-6 shadow-2xl shadow-emerald-900/10">
      <div className="absolute -right-16 -top-16 h-40 w-40 rounded-full bg-emerald-400/20 blur-3xl" />
      <div className="absolute -bottom-20 -left-10 h-44 w-44 rounded-full bg-cyan-400/20 blur-3xl" />

      <div className="relative">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted">
            Топология стенда
          </span>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            online
          </span>
        </div>

        <div className="mt-5 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3">
          <p className="text-xs font-semibold text-emerald-700 dark:text-emerald-300">
            gateway-server :8080
          </p>
          <p className="mt-0.5 text-[11px] text-muted">OAuth2 client · frontend · API proxy</p>
        </div>

        <div className="mx-auto h-5 w-px bg-border" />

        <div className="grid grid-cols-2 gap-2">
          {services.slice(1).map((service) => (
            <div
              key={service.name}
              className="rounded-xl border border-border bg-background/70 px-3 py-2.5"
            >
              <p className="truncate font-mono text-[11px] font-medium text-foreground">
                {service.name}
              </p>
              <p className="mt-0.5 truncate text-[10px] text-muted">{service.note}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function AnonymousHome() {
  return (
    <>
      <section className="relative overflow-hidden">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-80 bg-gradient-to-b from-emerald-500/10 to-transparent" />
        <div className="relative mx-auto grid w-full max-w-6xl gap-12 px-4 py-16 sm:px-6 sm:py-24 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:gap-16 lg:px-8">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
              Spring Boot · OAuth 2.1 · Next.js
            </span>
            <h1 className="mt-6 text-4xl font-semibold tracking-tight text-foreground sm:text-5xl lg:text-6xl">
              Маркетплейс,
              <br />
              собранный из
              <span className="bg-gradient-to-r from-emerald-500 to-cyan-500 bg-clip-text text-transparent">
                {" "}
                микросервисов
              </span>
            </h1>
            <p className="mt-5 max-w-xl text-base leading-relaxed text-muted">
              Демонстрационный стенд с единым gateway, собственной авторизацией,
              каталогом, заказами и отзывами. Войдите, чтобы увидеть закрытые
              разделы и персональный профиль.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-3">
              {/* A full navigation starts the gateway OAuth2 flow. */}
              <a
                href={loginUrl}
                className="inline-flex h-11 items-center gap-2 rounded-full bg-foreground px-6 text-sm font-semibold text-background transition-opacity hover:opacity-85"
              >
                Войти в аккаунт
                <Icon>
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </Icon>
              </a>
              <a
                href="#architecture"
                className="inline-flex h-11 items-center rounded-full border border-border bg-card px-6 text-sm font-semibold text-foreground transition-colors hover:bg-muted/10"
              >
                Как это устроено
              </a>
            </div>

            <dl className="mt-10 grid max-w-lg grid-cols-3 gap-4">
              {[
                { value: "1", label: "публичный порт" },
                { value: "6", label: "сервисов" },
                { value: "2", label: "роли доступа" },
              ].map((stat) => (
                <div key={stat.label}>
                  <dt className="text-2xl font-semibold tracking-tight text-foreground">
                    {stat.value}
                  </dt>
                  <dd className="mt-1 text-xs text-muted">{stat.label}</dd>
                </div>
              ))}
            </dl>
          </div>

          <ServiceStack />
        </div>
      </section>

      <section id="architecture" className="mx-auto w-full max-w-6xl px-4 pb-20 sm:px-6 lg:px-8">
        <div className="grid gap-4 md:grid-cols-3">
          {features.map((feature) => (
            <article
              key={feature.title}
              className="rounded-3xl border border-border bg-card p-6 transition-colors hover:border-emerald-500/40"
            >
              <span className="grid h-10 w-10 place-items-center rounded-2xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                <Icon>{feature.icon}</Icon>
              </span>
              <h2 className="mt-4 text-base font-semibold text-foreground">
                {feature.title}
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-muted">{feature.body}</p>
            </article>
          ))}
        </div>

        <div className="mt-6 flex flex-col gap-4 rounded-3xl border border-border bg-card/60 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-foreground">Каталог приложений уже открыт</p>
            <p className="mt-1 text-xs text-muted">
              Смотрите опубликованные приложения без входа. Разработчикам доступен
              кабинет с загрузкой версий, логотипа и публикацией.
            </p>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2">
            <a
              href="/catalog"
              className="inline-flex h-10 items-center justify-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              Открыть каталог
            </a>
            <a
              href="/developer"
              className="inline-flex h-10 items-center justify-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              Кабинет разработчика
            </a>
          </div>
        </div>
      </section>
    </>
  );
}

function AuthenticatedHome({
  username,
  roles,
  isAdmin,
}: {
  username: string;
  roles: string[];
  isAdmin: boolean;
}) {
  return (
    <section className="mx-auto w-full max-w-6xl px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
        Личный кабинет
      </p>
      <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
            Привет, {username}
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted">
            Вы вошли через единый gateway. Профиль и роль переданы фронтенду
            доверенными заголовками, собственной auth-логики у него нет.
          </p>
        </div>
        <span className="inline-flex items-center gap-2 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
          Сессия активна
        </span>
      </div>

      <div className="mt-8 grid gap-4 lg:grid-cols-3">
        <article className="rounded-3xl border border-border bg-card p-6 lg:col-span-1">
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
            Ваш профиль
          </p>
          <p className="mt-3 truncate text-xl font-semibold text-foreground">{username}</p>
          <div className="mt-4 flex flex-wrap gap-2">
            {roles.length > 0 ? (
              roles.map((role) => (
                <span
                  key={role}
                  className="rounded-full border border-border bg-background px-3 py-1 text-[11px] font-semibold uppercase tracking-wider text-muted"
                >
                  {role}
                </span>
              ))
            ) : (
              <span className="text-xs text-muted">Без ролей</span>
            )}
          </div>
          <dl className="mt-6 space-y-2 border-t border-border pt-4 text-xs">
            <div className="flex items-center justify-between gap-3">
              <dt className="text-muted">Провайдер</dt>
              <dd className="font-medium text-foreground">user-service</dd>
            </div>
            <div className="flex items-center justify-between gap-3">
              <dt className="text-muted">Точка входа</dt>
              <dd className="font-medium text-foreground">gateway-server</dd>
            </div>
            <div className="flex items-center justify-between gap-3">
              <dt className="text-muted">Протокол</dt>
              <dd className="font-medium text-foreground">OpenID Connect</dd>
            </div>
          </dl>
        </article>

        {isAdmin ? (
          <article className="rounded-3xl border border-emerald-500/30 bg-gradient-to-br from-emerald-500/10 to-cyan-500/5 p-6 lg:col-span-1">
            <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-emerald-700 dark:text-emerald-300">
              Администратор
            </p>
            <h2 className="mt-3 text-lg font-semibold text-foreground">
              Инструменты стенда
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-muted">
              Эти разделы доступны только роли ADMIN. Ссылки также продублированы
              в меню профиля в шапке.
            </p>
            <div className="mt-5 grid gap-2">
              <a
                href={adminUrl}
                className="flex items-center justify-between rounded-2xl border border-border bg-background/80 px-4 py-3 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
              >
                Админ-панель
                <Icon>
                  <path d="M7 17 17 7M8 7h9v9" />
                </Icon>
              </a>
              <a
                href={docsUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-center justify-between rounded-2xl border border-border bg-background/80 px-4 py-3 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
              >
                Swagger UI
                <Icon>
                  <path d="M7 17 17 7M8 7h9v9" />
                </Icon>
              </a>
              <a
                href={grafanaUrl}
                target="_blank"
                rel="noreferrer"
                className="flex items-center justify-between rounded-2xl border border-border bg-background/80 px-4 py-3 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
              >
                Grafana
                <Icon>
                  <path d="M7 17 17 7M8 7h9v9" />
                </Icon>
              </a>
            </div>
          </article>
        ) : (
          <article className="rounded-3xl border border-border bg-card p-6 lg:col-span-1">
            <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
              Доступные разделы
            </p>
            <h2 className="mt-3 text-lg font-semibold text-foreground">
              Каталог приложений
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-muted">
              Опубликованные приложения доступны сразу. Разработчики могут
              создать своё приложение, загрузить версии и опубликовать его.
            </p>
            <div className="mt-5 grid gap-2">
              <a
                href="/catalog"
                className="flex items-center justify-between rounded-2xl border border-border bg-background/80 px-4 py-3 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
              >
                Каталог
                <Icon>
                  <path d="M7 17 17 7M8 7h9v9" />
                </Icon>
              </a>
              <a
                href="/developer"
                className="flex items-center justify-between rounded-2xl border border-border bg-background/80 px-4 py-3 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
              >
                Кабинет разработчика
                <Icon>
                  <path d="M7 17 17 7M8 7h9v9" />
                </Icon>
              </a>
            </div>
          </article>
        )}

        <article className="rounded-3xl border border-border bg-card p-6 lg:col-span-1">
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
            Стек под капотом
          </p>
          <ul className="mt-4 space-y-3">
            {services.map((service) => (
              <li key={service.name} className="flex items-start gap-3">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
                <div className="min-w-0">
                  <p className="truncate font-mono text-xs font-medium text-foreground">
                    {service.name}
                  </p>
                  <p className="truncate text-[11px] text-muted">{service.note}</p>
                </div>
              </li>
            ))}
          </ul>
        </article>
      </div>
    </section>
  );
}

export default async function HomeContent() {
  const user = await getSession();
  const isAdmin = user?.roles.includes("ADMIN") ?? false;

  if (!user) {
    return (
      <main className="flex-1">
        <AnonymousHome />
      </main>
    );
  }

  return (
    <main className="flex-1">
      <AuthenticatedHome username={user.username} roles={user.roles} isAdmin={isAdmin} />
    </main>
  );
}
