"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AppAvatar } from "@/components/app-avatar";
import {
  formatDate,
  formatPrice,
  getApp,
  requestDownload,
  startDownload,
  type AppItem,
  type DownloadState,
} from "@/lib/app-api";

function downloadLabel(state: DownloadState | null, authenticated: boolean): string {
  if (!authenticated) {
    return "Войти, чтобы скачать";
  }
  switch (state) {
    case "PURCHASED_NOT_DOWNLOADED":
    case "UP_TO_DATE":
      return "Скачать";
    case "UPDATE_AVAILABLE":
      return "Скачать обновление";
    case "OWN_APP_NOT_DOWNLOADED":
      return "Скачать (моё приложение)";
    case "OWN_APP_UP_TO_DATE":
      return "Скачать (моё приложение)";
    case "OWN_APP_UPDATE_AVAILABLE":
      return "Скачать обновление";
    case "NOT_PURCHASED":
      return "Не куплено";
    case "NOT_AVAILABLE_FOR_ADMIN":
      return "Недоступно администратору";
    case "NOT_AVAILABLE":
      return "Недоступно";
    default:
      return "Скачать";
  }
}

function canDownload(state: DownloadState | null, authenticated: boolean): boolean {
  if (!authenticated || state === null) {
    return false;
  }
  return (
    state !== "NOT_PURCHASED" &&
    state !== "NOT_AVAILABLE_FOR_ADMIN" &&
    state !== "NOT_AVAILABLE"
  );
}

function downloadHint(state: DownloadState | null, authenticated: boolean): string | null {
  if (!authenticated) {
    return "Скачивание доступно после входа через gateway.";
  }
  switch (state) {
    case "UP_TO_DATE":
    case "OWN_APP_UP_TO_DATE":
      return "У вас уже последняя версия.";
    case "UPDATE_AVAILABLE":
    case "OWN_APP_UPDATE_AVAILABLE":
      return "Доступна более новая версия.";
    case "NOT_PURCHASED":
      return "Приложение ещё не приобретено.";
    case "NOT_AVAILABLE_FOR_ADMIN":
      return "Администраторы не могут скачивать приложения.";
    case "NOT_AVAILABLE":
      return "Скачивание недоступно.";
    default:
      return null;
  }
}

export default function AppDetailContent({
  slugOrId,
  authenticated,
  isAdmin,
  userId,
  loginUrl,
}: {
  slugOrId: string;
  authenticated: boolean;
  isAdmin: boolean;
  userId: string | null;
  loginUrl: string;
}) {
  const [app, setApp] = useState<AppItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let active = true;

    getApp(slugOrId)
      .then((result) => {
        if (active) {
          setApp(result);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Не удалось загрузить приложение.",
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [slugOrId, refreshKey]);

  async function download() {
    if (!app) {
      return;
    }
    setDownloading(true);
    setDownloadError(null);
    try {
      const ticket = await requestDownload(app.slug || app.id);
      startDownload(ticket.downloadUrl);
      getApp(slugOrId)
        .then(setApp)
        .catch(() => {
          // The download itself already started; an updated state is optional.
        });
    } catch (requestError) {
      setDownloadError(
        requestError instanceof Error ? requestError.message : "Не удалось начать скачивание.",
      );
    } finally {
      setDownloading(false);
    }
  }

  if (loading) {
    return (
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
        <div className="h-4 w-40 animate-pulse rounded-full bg-border" />
        <div className="mt-6 flex gap-5">
          <div className="h-24 w-24 animate-pulse rounded-3xl bg-border/70" />
          <div className="flex-1 space-y-3">
            <div className="h-9 w-2/3 animate-pulse rounded-xl bg-border" />
            <div className="h-4 w-1/2 animate-pulse rounded-full bg-border/70" />
          </div>
        </div>
        <div className="mt-8 grid gap-4 lg:grid-cols-[1.6fr_1fr]">
          <div className="h-64 animate-pulse rounded-3xl bg-border/50" />
          <div className="h-64 animate-pulse rounded-3xl bg-border/50" />
        </div>
      </main>
    );
  }

  if (error || !app) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-rose-500/30 bg-card p-8 text-center">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-rose-600 dark:text-rose-400">
            Приложение
          </p>
          <h1 className="mt-3 text-2xl font-semibold text-foreground">Не удалось открыть</h1>
          <p className="mx-auto mt-2 max-w-md text-sm text-muted">
            {error ?? "Приложение не найдено."}
          </p>
          <div className="mt-6 flex justify-center gap-2">
            <button
              type="button"
              onClick={() => {
                setLoading(true);
                setError(null);
                setRefreshKey((value) => value + 1);
              }}
              className="inline-flex h-10 items-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              Повторить
            </button>
            <Link
              href="/catalog"
              className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              В каталог
            </Link>
          </div>
        </section>
      </main>
    );
  }

  const manageHref = `/developer/apps/${app.id}`;
  const canManage = authenticated && (isAdmin || (userId !== null && Number(userId) === app.developerId));
  const downloadable = canDownload(app.downloadState, authenticated);
  const hint = downloadHint(app.downloadState, authenticated);

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <nav aria-label="Хлебные крошки" className="flex items-center gap-2 text-xs text-muted">
        <Link href="/catalog" className="transition-colors hover:text-foreground">
          Каталог
        </Link>
        <span aria-hidden="true">/</span>
        <span className="truncate text-foreground">{app.name}</span>
      </nav>

      <section className="mt-6 overflow-hidden rounded-3xl border border-border bg-card shadow-sm">
        <div className="relative border-b border-border bg-gradient-to-br from-emerald-500/10 via-transparent to-cyan-500/10 px-5 py-8 sm:px-8">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
            <AppAvatar
              name={app.name}
              logoUrl={app.logoUrl}
              className="h-24 w-24"
              rounded="rounded-3xl"
              textClassName="text-3xl"
            />

            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
                  {app.name}
                </h1>
                {app.status === "DRAFT" && (
                  <span className="rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-amber-700 dark:text-amber-300">
                    Черновик
                  </span>
                )}
              </div>
              <p className="mt-1 font-mono text-xs text-muted">@{app.slug}</p>

              <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted">
                <span className="inline-flex items-center gap-1.5">
                  <svg
                    viewBox="0 0 24 24"
                    fill="currentColor"
                    className="h-3.5 w-3.5 text-amber-500"
                    aria-hidden="true"
                  >
                    <path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9-4.3-4.2 5.9-.9L12 3.5Z" />
                  </svg>
                  {app.cachedRatingCount > 0
                    ? `${app.cachedRating.toFixed(1)} · ${app.cachedRatingCount} оценок`
                    : "Нет оценок"}
                </span>
                <span>{app.downloadCount.toLocaleString("ru-RU")} загрузок</span>
                <span>Разработчик #{app.developerId}</span>
                <span>Обновлено {formatDate(app.updatedAt)}</span>
              </div>

              <div className="mt-4 flex flex-wrap gap-1.5">
                {app.categories.map((category) => (
                  <span
                    key={category.id}
                    className="rounded-full border border-border bg-background px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-muted"
                  >
                    {category.name}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="grid gap-6 p-5 sm:p-8 lg:grid-cols-[1.6fr_1fr]">
          <div className="min-w-0">
            <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-muted">
              Описание
            </h2>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-foreground">
              {app.description || "Разработчик пока не добавил описание."}
            </p>

            {app.galleryUrls.length > 0 && (
              <section className="mt-8">
                <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-muted">
                  Галерея
                </h2>
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  {app.galleryUrls.map((url, index) => (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      key={`${url}-${index}`}
                      src={url}
                      alt={`Скриншот ${index + 1}`}
                      loading="lazy"
                      className="aspect-video w-full rounded-2xl border border-border bg-background object-cover"
                    />
                  ))}
                </div>
              </section>
            )}

            <section className="mt-8">
              <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-muted">
                Последняя версия
              </h2>
              {app.latestVersion ? (
                <div className="mt-3 rounded-2xl border border-border bg-background/60 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="font-mono text-sm font-semibold text-foreground">
                      v{app.latestVersion.versionName}
                    </p>
                    <p className="text-xs text-muted">{formatDate(app.latestVersion.createdAt)}</p>
                  </div>
                  <p className="mt-2 whitespace-pre-line text-sm leading-relaxed text-muted">
                    {app.latestVersion.changelog || "Список изменений не указан."}
                  </p>
                </div>
              ) : (
                <p className="mt-3 text-sm text-muted">У приложения пока нет версий.</p>
              )}
            </section>
          </div>

          <aside className="space-y-3">
            <div className="rounded-3xl border border-border bg-background/60 p-5">
              <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted">
                Цена
              </p>
              <p className="mt-2 text-2xl font-semibold tracking-tight text-foreground">
                {formatPrice(app.price)}
              </p>

              {authenticated ? (
                <button
                  type="button"
                  onClick={download}
                  disabled={!downloadable || downloading}
                  className="mt-4 inline-flex h-11 w-full items-center justify-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {downloading ? "Готовим ссылку…" : downloadLabel(app.downloadState, authenticated)}
                </button>
              ) : (
                <a
                  href={loginUrl}
                  className="mt-4 inline-flex h-11 w-full items-center justify-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
                >
                  Войти, чтобы скачать
                </a>
              )}

              {hint && <p className="mt-3 text-xs leading-relaxed text-muted">{hint}</p>}
              {downloadError && (
                <p className="mt-3 text-xs leading-relaxed text-rose-600 dark:text-rose-400">
                  {downloadError}
                </p>
              )}

              <p className="mt-4 border-t border-border pt-3 text-[11px] leading-relaxed text-muted">
                Ссылка на файл формируется на 10 минут и ведёт напрямую в S3,
                минуя gateway.
              </p>
            </div>

            {canManage && (
              <Link
                href={manageHref}
                className="flex items-center justify-between rounded-3xl border border-emerald-500/30 bg-emerald-500/10 px-5 py-4 text-sm font-semibold text-emerald-700 transition-colors hover:bg-emerald-500/15 dark:text-emerald-300"
              >
                Управлять приложением
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="h-4 w-4"
                  aria-hidden="true"
                >
                  <path d="M7 17 17 7M8 7h9v9" />
                </svg>
              </Link>
            )}
          </aside>
        </div>
      </section>
    </main>
  );
}
