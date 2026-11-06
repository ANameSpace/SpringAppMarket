"use client";

import { useEffect, useMemo, useState } from "react";

type AdminUser = {
  id: number;
  username: string;
  email: string;
  displayName: string;
  avatarColor: string;
  roles: string[];
  status: string;
  createdAt: string;
};

type PageResponse = {
  content: AdminUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

const PAGE_SIZE = 20;

const roleOptions = [
  { value: "", label: "Все роли" },
  { value: "USER", label: "USER" },
  { value: "DEVELOPER", label: "DEVELOPER" },
  { value: "ADMIN", label: "ADMIN" },
];

const statusOptions = [
  { value: "", label: "Все статусы" },
  { value: "ACTIVE", label: "ACTIVE" },
  { value: "BLOCKED", label: "BLOCKED" },
];

function avatarLetter(user: AdminUser) {
  return user.username.slice(0, 1).toUpperCase();
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString("ru-RU");
}

export default function AdminPanel({ currentUser }: { currentUser: string }) {
  const [page, setPage] = useState(0);
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [data, setData] = useState<PageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const query = useMemo(() => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(PAGE_SIZE),
    });
    if (role) {
      params.set("role", role);
    }
    if (status) {
      params.set("status", status);
    }
    return params.toString();
  }, [page, role, status]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    fetch(`/api/v1/users?${query}`, {
      credentials: "same-origin",
      cache: "no-store",
      signal: controller.signal,
    })
      .then(async (response) => {
        if (response.status === 403) {
          throw new Error("Недостаточно прав: нужна роль ADMIN.");
        }
        if (!response.ok) {
          throw new Error(`Не удалось загрузить пользователей (${response.status}).`);
        }
        return (await response.json()) as PageResponse;
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
        setError(requestError instanceof Error ? requestError.message : "Неизвестная ошибка.");
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
  }, [query, refreshKey]);

  function changeRole(value: string) {
    setLoading(true);
    setError(null);
    setRole(value);
    setPage(0);
  }

  function changeStatus(value: string) {
    setLoading(true);
    setError(null);
    setStatus(value);
    setPage(0);
  }

  function goToPage(value: number) {
    setLoading(true);
    setError(null);
    setPage(value);
  }

  const users = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-emerald-600 dark:text-emerald-400">
            Администрирование
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-foreground">
            Пользователи
          </h1>
          <p className="mt-2 text-sm text-muted">
            Вы вошли как <span className="font-medium text-foreground">{currentUser}</span>.
            Данные загружаются из user-service через gateway.
          </p>
        </div>
        <a
          href="/auth-server/settings"
          className="inline-flex h-10 items-center rounded-full border border-border bg-card px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
        >
          Мои настройки
        </a>
      </div>

      <section className="mt-8 overflow-hidden rounded-3xl border border-border bg-card shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-4 py-4 sm:px-6">
          <div className="flex flex-wrap items-center gap-2">
            <label className="flex items-center gap-2 text-xs font-medium text-muted">
              Роль
              <select
                value={role}
                onChange={(event) => changeRole(event.target.value)}
                className="h-9 rounded-xl border border-border bg-background px-3 text-sm font-medium text-foreground outline-none focus:border-emerald-500"
              >
                {roleOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex items-center gap-2 text-xs font-medium text-muted">
              Статус
              <select
                value={status}
                onChange={(event) => changeStatus(event.target.value)}
                className="h-9 rounded-xl border border-border bg-background px-3 text-sm font-medium text-foreground outline-none focus:border-emerald-500"
              >
                {statusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <p className="text-xs text-muted">
            {loading ? "Загрузка…" : `Найдено: ${totalElements}`}
          </p>
        </div>

        {error ? (
          <div className="px-4 py-10 text-center sm:px-6">
            <p className="text-sm font-medium text-rose-600 dark:text-rose-400">{error}</p>
            <button
              type="button"
              onClick={() => {
                setLoading(true);
                setError(null);
                setRefreshKey((value) => value + 1);
              }}
              className="mt-4 inline-flex h-9 items-center rounded-full border border-border bg-background px-4 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              Обновить
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-border text-[10px] uppercase tracking-[0.14em] text-muted">
                  <th className="px-4 py-3 font-semibold sm:px-6">Пользователь</th>
                  <th className="px-4 py-3 font-semibold">Email</th>
                  <th className="px-4 py-3 font-semibold">Роли</th>
                  <th className="px-4 py-3 font-semibold">Статус</th>
                  <th className="px-4 py-3 font-semibold sm:px-6">Создан</th>
                </tr>
              </thead>
              <tbody>
                {!loading && users.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-6 py-12 text-center text-sm text-muted">
                      Пользователи не найдены.
                    </td>
                  </tr>
                )}

                {users.map((user) => (
                  <tr
                    key={user.id}
                    className="border-b border-border/70 last:border-0 hover:bg-muted/5"
                  >
                    <td className="px-4 py-3 sm:px-6">
                      <div className="flex items-center gap-3">
                        <span
                          className="grid h-9 w-9 shrink-0 place-items-center rounded-full text-xs font-bold uppercase text-white"
                          style={{ backgroundColor: user.avatarColor }}
                          aria-hidden="true"
                        >
                          {avatarLetter(user)}
                        </span>
                        <div className="min-w-0">
                          <p className="truncate font-medium text-foreground">
                            {user.displayName || user.username}
                          </p>
                          <p className="truncate font-mono text-[11px] text-muted">
                            @{user.username}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="max-w-56 truncate px-4 py-3 text-muted">{user.email}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {user.roles.map((userRole) => (
                          <span
                            key={userRole}
                            className="rounded-full border border-border bg-background px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-muted"
                          >
                            {userRole}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={
                          user.status === "ACTIVE"
                            ? "inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-emerald-700 dark:text-emerald-300"
                            : "inline-flex items-center gap-1.5 rounded-full border border-rose-500/30 bg-rose-500/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-rose-700 dark:text-rose-300"
                        }
                      >
                        <span className="h-1.5 w-1.5 rounded-full bg-current" />
                        {user.status}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-xs text-muted sm:px-6">
                      {formatDate(user.createdAt)}
                    </td>
                  </tr>
                ))}

                {loading && users.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-6 py-12 text-center text-sm text-muted">
                      Загружаем пользователей…
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-4 sm:px-6">
          <p className="text-xs text-muted">
            Страница {totalPages === 0 ? 0 : page + 1} из {totalPages}
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
      </section>
    </main>
  );
}
