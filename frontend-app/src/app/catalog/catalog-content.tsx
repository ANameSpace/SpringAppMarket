"use client";

import { useEffect, useState } from "react";
import { AppCard } from "@/components/app-card";
import {
  listApps,
  listCategories,
  type AppItem,
  type AppSort,
  type Category,
  type PageResponse,
} from "@/lib/app-api";

const PAGE_SIZE = 12;

const sortOptions: { value: AppSort; label: string }[] = [
  { value: "rating_desc", label: "По рейтингу" },
  { value: "downloads_desc", label: "По загрузкам" },
  { value: "name_asc", label: "По названию" },
  { value: "price_asc", label: "Сначала дешёвые" },
  { value: "price_desc", label: "Сначала дорогие" },
];

function SearchIcon() {
  return (
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
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </svg>
  );
}

export default function CatalogContent() {
  const [searchInput, setSearchInput] = useState("");
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<AppSort>("rating_desc");
  const [categoryId, setCategoryId] = useState("");
  const [page, setPage] = useState(0);

  const [categories, setCategories] = useState<Category[]>([]);
  const [data, setData] = useState<PageResponse<AppItem> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    let active = true;
    listCategories()
      .then((result) => {
        if (active) {
          setCategories(result);
        }
      })
      .catch(() => {
        // The catalog still works without the category filter.
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    listApps({
      page,
      size: PAGE_SIZE,
      sort,
      q: query,
      categoryIds: categoryId ? [categoryId] : [],
    })
      .then((result) => {
        if (active) {
          setData(result);
        }
      })
      .catch((requestError: unknown) => {
        if (!active || (requestError instanceof DOMException && requestError.name === "AbortError")) {
          return;
        }
        setError(
          requestError instanceof Error ? requestError.message : "Не удалось загрузить каталог.",
        );
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [page, sort, query, categoryId, refreshKey]);

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextQuery = searchInput.trim();
    if (nextQuery !== query || page !== 0) {
      setLoading(true);
      setError(null);
    }
    setPage(0);
    setQuery(nextQuery);
  }

  function changeSort(value: string) {
    if (value !== sort || page !== 0) {
      setLoading(true);
      setError(null);
    }
    setSort(value as AppSort);
    setPage(0);
  }

  function selectCategory(value: string) {
    if (value !== categoryId || page !== 0) {
      setLoading(true);
      setError(null);
    }
    setCategoryId(value);
    setPage(0);
  }

  function goToPage(nextPage: number) {
    setLoading(true);
    setError(null);
    setPage(nextPage);
  }

  const apps = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Витрина
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-foreground sm:text-4xl">
            Каталог приложений
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-muted">
            Опубликованные приложения из app-service. Скачивание идёт напрямую из
            объектного хранилища по короткоживущей ссылке.
          </p>
        </div>
        <a
          href="/developer"
          className="inline-flex h-10 items-center rounded-full border border-border bg-card px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
        >
          Кабинет разработчика
        </a>
      </div>

      <section className="mt-8 rounded-3xl border border-border bg-card p-4 shadow-sm sm:p-5">
        <form onSubmit={submitSearch} className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <label className="relative flex-1">
            <span className="sr-only">Поиск по каталогу</span>
            <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted">
              <SearchIcon />
            </span>
            <input
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Название или slug приложения"
              className="h-10 w-full rounded-xl border border-border bg-background pl-10 pr-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted focus:border-emerald-500"
            />
          </label>

          <div className="flex items-center gap-2">
            <button
              type="submit"
              className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              Найти
            </button>
            <label className="flex items-center gap-2 text-xs font-medium text-muted">
              <span className="hidden sm:inline">Сортировка</span>
              <select
                value={sort}
                onChange={(event) => changeSort(event.target.value)}
                className="h-10 rounded-xl border border-border bg-background px-3 text-sm font-medium text-foreground outline-none focus:border-emerald-500"
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </form>

        <div className="mt-4 flex flex-wrap items-center gap-2 border-t border-border pt-4">
          <button
            type="button"
            onClick={() => selectCategory("")}
            className={
              categoryId === ""
                ? "rounded-full border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300"
                : "rounded-full border border-border bg-background px-3 py-1.5 text-xs font-semibold text-muted transition-colors hover:border-emerald-500/40 hover:text-foreground"
            }
          >
            Все категории
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              type="button"
              onClick={() => selectCategory(category.id)}
              className={
                categoryId === category.id
                  ? "rounded-full border border-emerald-500/40 bg-emerald-500/10 px-3 py-1.5 text-xs font-semibold text-emerald-700 dark:text-emerald-300"
                  : "rounded-full border border-border bg-background px-3 py-1.5 text-xs font-semibold text-muted transition-colors hover:border-emerald-500/40 hover:text-foreground"
              }
            >
              {category.name}
            </button>
          ))}
          <span className="ml-auto text-xs text-muted">
            {loading && !data ? "Загрузка…" : `Найдено: ${totalElements}`}
          </span>
        </div>
      </section>

      {error ? (
        <section className="mt-8 rounded-3xl border border-rose-500/30 bg-card px-6 py-12 text-center">
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
      ) : loading && !data ? (
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div
              key={index}
              className="h-56 animate-pulse rounded-3xl border border-border bg-card/60"
            />
          ))}
        </div>
      ) : apps.length === 0 ? (
        <section className="mt-8 rounded-3xl border border-dashed border-border bg-card/60 px-6 py-16 text-center">
          <h2 className="text-lg font-semibold text-foreground">Приложения не найдены</h2>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Попробуйте изменить запрос или сбросить фильтр категории. Новые
            приложения появляются здесь после публикации разработчиком.
          </p>
          {(query || categoryId) && (
            <button
              type="button"
              onClick={() => {
                setLoading(true);
                setError(null);
                setSearchInput("");
                setQuery("");
                setCategoryId("");
                setPage(0);
              }}
              className="mt-5 inline-flex h-9 items-center rounded-full border border-border bg-background px-4 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              Сбросить фильтры
            </button>
          )}
        </section>
      ) : (
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {apps.map((app) => (
            <AppCard key={app.id} app={app} />
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-8 flex flex-wrap items-center justify-between gap-3 rounded-3xl border border-border bg-card px-4 py-4 sm:px-6">
          <p className="text-xs text-muted">
            Страница {page + 1} из {totalPages}
          </p>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => goToPage(Math.max(0, page - 1))}
              disabled={loading || page <= 0}
              className="inline-flex h-9 items-center rounded-full border border-border bg-background px-4 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Назад
            </button>
            <button
              type="button"
              onClick={() => goToPage(page + 1)}
              disabled={loading || page + 1 >= totalPages}
              className="inline-flex h-9 items-center rounded-full border border-border bg-background px-4 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Вперёд
            </button>
          </div>
        </div>
      )}
    </main>
  );
}
