"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { AppAvatar } from "@/components/app-avatar";
import {
  becomeDeveloper,
  createApp,
  formatDate,
  formatPrice,
  listApps,
  listCategories,
  type AppItem,
  type Category,
} from "@/lib/app-api";

const statusStyles: Record<string, string> = {
  DRAFT:
    "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300",
  PUBLISHED:
    "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
};

const statusLabels: Record<string, string> = {
  DRAFT: "Черновик",
  PUBLISHED: "Опубликовано",
  STUB: "Удалено",
};

type CreateForm = {
  name: string;
  slug: string;
  description: string;
  price: string;
  categoryIds: string[];
};

const emptyForm: CreateForm = {
  name: "",
  slug: "",
  description: "",
  price: "0",
  categoryIds: [],
};

export default function DeveloperCabinet({
  username,
  roles,
}: {
  username: string;
  roles: string[];
}) {
  const router = useRouter();
  const isAdmin = roles.includes("ADMIN");
  const isDeveloper = roles.includes("DEVELOPER");
  const canDevelop = isAdmin || isDeveloper;

  const [apps, setApps] = useState<AppItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(canDevelop);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<CreateForm>(emptyForm);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [becoming, setBecoming] = useState(false);
  const [becomeError, setBecomeError] = useState<string | null>(null);
  const [becomeDone, setBecomeDone] = useState(false);

  useEffect(() => {
    if (!canDevelop) {
      return;
    }

    let active = true;

    Promise.all([
      listApps({ page: 0, size: 50, status: "DRAFT" }),
      listApps({ page: 0, size: 50, status: "PUBLISHED" }),
    ])
      .then(([drafts, published]) => {
        if (!active) {
          return;
        }
        const merged = new Map<string, AppItem>();
        for (const app of [...drafts.content, ...published.content]) {
          merged.set(app.id, app);
        }
        setApps(
          [...merged.values()].sort(
            (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
          ),
        );
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Не удалось загрузить приложения.",
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
  }, [canDevelop, refreshKey]);

  useEffect(() => {
    if (!canDevelop) {
      return;
    }
    let active = true;
    listCategories()
      .then((result) => {
        if (active) {
          setCategories(result);
        }
      })
      .catch(() => {
        // Categories are optional for app creation.
      });
    return () => {
      active = false;
    };
  }, [canDevelop]);

  async function requestDeveloperRole() {
    setBecoming(true);
    setBecomeError(null);
    try {
      await becomeDeveloper();
      setBecomeDone(true);
    } catch (requestError) {
      setBecomeError(
        requestError instanceof Error
          ? requestError.message
          : "Не удалось получить роль разработчика.",
      );
    } finally {
      setBecoming(false);
    }
  }

  function toggleCategory(categoryId: string) {
    setForm((current) => {
      if (current.categoryIds.includes(categoryId)) {
        return {
          ...current,
          categoryIds: current.categoryIds.filter((id) => id !== categoryId),
        };
      }
      if (current.categoryIds.length >= 3) {
        return current;
      }
      return { ...current, categoryIds: [...current.categoryIds, categoryId] };
    });
  }

  async function submitCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreateError(null);

    const price = Number(form.price);
    if (!Number.isFinite(price) || price < 0) {
      setCreateError("Цена должна быть неотрицательным числом.");
      return;
    }

    setCreating(true);
    try {
      const created = await createApp({
        name: form.name.trim(),
        slug: form.slug.trim(),
        description: form.description.trim() || null,
        price,
        categoryIds: form.categoryIds,
      });
      router.push(`/developer/apps/${created.id}`);
    } catch (requestError) {
      setCreateError(
        requestError instanceof Error ? requestError.message : "Не удалось создать приложение.",
      );
    } finally {
      setCreating(false);
    }
  }

  if (!canDevelop) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-12 sm:px-6">
        <section className="rounded-3xl border border-border bg-card p-8 shadow-sm">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Кабинет разработчика
          </p>
          <h1 className="mt-3 text-3xl font-semibold tracking-tight text-foreground">
            Станьте разработчиком
          </h1>
          <p className="mt-3 text-sm leading-relaxed text-muted">
            Вы вошли как <span className="font-medium text-foreground">{username}</span>.
            Роль DEVELOPER открывает создание приложений, загрузку версий, логотипа
            и галереи. Роль выдаётся через user-service и попадает в токен при
            следующем входе.
          </p>

          {becomeDone ? (
            <div className="mt-6 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-5">
              <p className="text-sm font-semibold text-emerald-700 dark:text-emerald-300">
                Роль DEVELOPER выдана
              </p>
              <p className="mt-1 text-sm text-muted">
                Выйдите и войдите снова, чтобы обновить токен и открыть кабинет.
              </p>
              <form action="/logout" method="post" className="mt-4">
                <button
                  type="submit"
                  className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
                >
                  Выйти и войти заново
                </button>
              </form>
            </div>
          ) : (
            <>
              <div className="mt-6 grid gap-2 sm:grid-cols-2">
                {[
                  "Создание приложений и версий",
                  "Загрузка логотипа и галереи",
                  "Публикация в каталоге",
                  "Управление ценами и категориями",
                ].map((item) => (
                  <div
                    key={item}
                    className="flex items-center gap-2 rounded-2xl border border-border bg-background/70 px-4 py-3 text-sm text-foreground"
                  >
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                    {item}
                  </div>
                ))}
              </div>

              {becomeError && (
                <p className="mt-4 text-sm font-medium text-rose-600 dark:text-rose-400">
                  {becomeError}
                </p>
              )}

              <div className="mt-6 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={requestDeveloperRole}
                  disabled={becoming}
                  className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85 disabled:opacity-50"
                >
                  {becoming ? "Выдаём роль…" : "Стать разработчиком"}
                </button>
                <Link
                  href="/catalog"
                  className="inline-flex h-10 items-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
                >
                  Сначала посмотреть каталог
                </Link>
              </div>
            </>
          )}
        </section>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Кабинет разработчика
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
            {isAdmin ? "Все приложения" : "Мои приложения"}
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted">
            {isAdmin
              ? "Администратор видит и может редактировать все приложения."
              : `Вы вошли как ${username}. Черновики видны только вам, опубликованные — всем в каталоге.`}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            href="/catalog"
            className="inline-flex h-10 items-center rounded-full border border-border bg-card px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
          >
            Каталог
          </Link>
          <button
            type="button"
            onClick={() => {
              setCreateOpen((value) => !value);
              setCreateError(null);
            }}
            className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
          >
            {createOpen ? "Свернуть форму" : "Создать приложение"}
          </button>
        </div>
      </div>

      {createOpen && (
        <section className="mt-6 rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-6">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-foreground">Новое приложение</h2>
            <p className="text-xs text-muted">
              Slug: только латиница и цифры, 2–64 символа. После первой публикации
              его нельзя изменить.
            </p>
          </div>

          <form onSubmit={submitCreate} className="mt-5 grid gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block text-xs font-medium text-muted">
                Название
                <input
                  required
                  maxLength={200}
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                  className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none focus:border-emerald-500"
                />
              </label>
              <label className="block text-xs font-medium text-muted">
                Slug
                <input
                  required
                  pattern="[a-zA-Z0-9]{2,64}"
                  maxLength={64}
                  value={form.slug}
                  onChange={(event) => setForm({ ...form, slug: event.target.value })}
                  className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 font-mono text-sm text-foreground outline-none focus:border-emerald-500"
                />
              </label>
            </div>

            <label className="block text-xs font-medium text-muted">
              Описание
              <textarea
                rows={4}
                maxLength={5000}
                value={form.description}
                onChange={(event) => setForm({ ...form, description: event.target.value })}
                className="mt-1.5 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-emerald-500"
              />
            </label>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block text-xs font-medium text-muted">
                Цена, ₽
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.price}
                  onChange={(event) => setForm({ ...form, price: event.target.value })}
                  className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none focus:border-emerald-500"
                />
              </label>
              <div className="text-xs font-medium text-muted">
                Категории (до 3)
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {categories.map((category) => {
                    const selected = form.categoryIds.includes(category.id);
                    const disabled = !selected && form.categoryIds.length >= 3;
                    return (
                      <button
                        key={category.id}
                        type="button"
                        onClick={() => toggleCategory(category.id)}
                        disabled={disabled}
                        className={
                          selected
                            ? "rounded-full border border-emerald-500/40 bg-emerald-500/10 px-2.5 py-1 text-[11px] font-semibold text-emerald-700 dark:text-emerald-300"
                            : "rounded-full border border-border bg-background px-2.5 py-1 text-[11px] font-semibold text-muted transition-colors hover:border-emerald-500/40 disabled:cursor-not-allowed disabled:opacity-40"
                        }
                      >
                        {category.name}
                      </button>
                    );
                  })}
                  {categories.length === 0 && (
                    <span className="text-[11px] text-muted">Категории не загружены.</span>
                  )}
                </div>
              </div>
            </div>

            {createError && (
              <p className="text-sm font-medium text-rose-600 dark:text-rose-400">{createError}</p>
            )}

            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setCreateOpen(false)}
                className="inline-flex h-10 items-center rounded-full border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
              >
                Отмена
              </button>
              <button
                type="submit"
                disabled={creating}
                className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85 disabled:opacity-50"
              >
                {creating ? "Создаём…" : "Создать"}
              </button>
            </div>
          </form>
        </section>
      )}

      {error && (
        <section className="mt-6 rounded-3xl border border-rose-500/30 bg-card px-6 py-10 text-center">
          <p className="text-sm font-semibold text-rose-600 dark:text-rose-400">{error}</p>
          <button
            type="button"
            onClick={() => {
              setLoading(true);
              setError(null);
              setRefreshKey((value) => value + 1);
            }}
            className="mt-4 inline-flex h-9 items-center rounded-full border border-border bg-background px-4 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
          >
            Повторить
          </button>
        </section>
      )}

      {!error && loading && apps.length === 0 && (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="h-44 animate-pulse rounded-3xl bg-border/50" />
          ))}
        </div>
      )}

      {!error && !loading && apps.length === 0 && (
        <section className="mt-6 rounded-3xl border border-dashed border-border bg-card/60 px-6 py-16 text-center">
          <h2 className="text-lg font-semibold text-foreground">Пока нет приложений</h2>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Создайте первое приложение, загрузите версию и опубликуйте его в каталоге.
          </p>
          {!createOpen && (
            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              className="mt-5 inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              Создать приложение
            </button>
          )}
        </section>
      )}

      {apps.length > 0 && (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {apps.map((app) => (
            <article
              key={app.id}
              className="flex h-full flex-col rounded-3xl border border-border bg-card p-5 shadow-sm"
            >
              <div className="flex items-start gap-3">
                <AppAvatar name={app.name} logoUrl={app.logoUrl} className="h-12 w-12" />
                <div className="min-w-0 flex-1">
                  <h2 className="truncate text-base font-semibold text-foreground">{app.name}</h2>
                  <p className="mt-0.5 truncate font-mono text-[11px] text-muted">@{app.slug}</p>
                </div>
                <span
                  className={`shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider ${
                    statusStyles[app.status] ?? "border-border bg-background text-muted"
                  }`}
                >
                  {statusLabels[app.status] ?? app.status}
                </span>
              </div>

              <dl className="mt-4 grid grid-cols-2 gap-2 text-[11px]">
                <div className="rounded-xl border border-border bg-background/60 px-3 py-2">
                  <dt className="text-muted">Цена</dt>
                  <dd className="mt-0.5 font-semibold text-foreground">{formatPrice(app.price)}</dd>
                </div>
                <div className="rounded-xl border border-border bg-background/60 px-3 py-2">
                  <dt className="text-muted">Версия</dt>
                  <dd className="mt-0.5 font-mono font-semibold text-foreground">
                    {app.latestVersion ? `v${app.latestVersion.versionName}` : "—"}
                  </dd>
                </div>
                <div className="rounded-xl border border-border bg-background/60 px-3 py-2">
                  <dt className="text-muted">Загрузки</dt>
                  <dd className="mt-0.5 font-semibold text-foreground">
                    {app.downloadCount.toLocaleString("ru-RU")}
                  </dd>
                </div>
                <div className="rounded-xl border border-border bg-background/60 px-3 py-2">
                  <dt className="text-muted">Обновлено</dt>
                  <dd className="mt-0.5 font-semibold text-foreground">
                    {formatDate(app.updatedAt)}
                  </dd>
                </div>
              </dl>

              <div className="mt-auto flex flex-wrap items-center gap-2 border-t border-border pt-4">
                <Link
                  href={`/developer/apps/${app.id}`}
                  className="inline-flex h-9 items-center rounded-full bg-foreground px-4 text-xs font-semibold text-background transition-opacity hover:opacity-85"
                >
                  Редактировать
                </Link>
                {app.status === "PUBLISHED" && (
                  <Link
                    href={`/apps/${encodeURIComponent(app.slug || app.id)}`}
                    className="inline-flex h-9 items-center rounded-full border border-border bg-background px-4 text-xs font-semibold text-foreground transition-colors hover:border-emerald-500/50"
                  >
                    В каталоге
                  </Link>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </main>
  );
}
