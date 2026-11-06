import Link from "next/link";
import { AppAvatar } from "./app-avatar";
import { formatPrice, type AppItem } from "@/lib/app-api";

function StarIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="currentColor"
      className="h-3.5 w-3.5 text-amber-500"
      aria-hidden="true"
    >
      <path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9-4.3-4.2 5.9-.9L12 3.5Z" />
    </svg>
  );
}

function DownloadIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-3.5 w-3.5"
      aria-hidden="true"
    >
      <path d="M12 3v12M7 10l5 5 5-5M5 21h14" />
    </svg>
  );
}

export function AppCard({ app }: { app: AppItem }) {
  return (
    <Link
      href={`/apps/${encodeURIComponent(app.slug || app.id)}`}
      className="group flex h-full flex-col rounded-3xl border border-border bg-card p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-emerald-500/40 hover:shadow-lg hover:shadow-emerald-900/5"
    >
      <div className="flex items-start gap-3">
        <AppAvatar name={app.name} logoUrl={app.logoUrl} className="h-14 w-14" />
        <div className="min-w-0 flex-1">
          <h2 className="truncate text-base font-semibold text-foreground group-hover:text-emerald-700 dark:group-hover:text-emerald-400">
            {app.name}
          </h2>
          <p className="mt-0.5 truncate font-mono text-[11px] text-muted">@{app.slug}</p>
          <p className="mt-1.5 inline-flex items-center rounded-full border border-border bg-background px-2 py-0.5 text-[11px] font-semibold text-foreground">
            {formatPrice(app.price)}
          </p>
        </div>
      </div>

      <p className="mt-4 line-clamp-2 min-h-10 text-sm leading-relaxed text-muted">
        {app.description || "Описание пока не добавлено."}
      </p>

      <div className="mt-4 flex flex-wrap gap-1.5">
        {app.categories.slice(0, 3).map((category) => (
          <span
            key={category.id}
            className="rounded-full border border-border bg-background px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-muted"
          >
            {category.name}
          </span>
        ))}
        {app.categories.length === 0 && (
          <span className="text-[11px] text-muted">Без категории</span>
        )}
      </div>

      <div className="mt-auto flex items-center gap-3 border-t border-border pt-3 text-[11px] font-medium text-muted">
        <span className="inline-flex items-center gap-1">
          <StarIcon />
          {app.cachedRatingCount > 0 ? app.cachedRating.toFixed(1) : "—"}
        </span>
        <span className="inline-flex items-center gap-1">
          <DownloadIcon />
          {app.downloadCount.toLocaleString("ru-RU")}
        </span>
        {app.latestVersion && (
          <span className="ml-auto truncate font-mono text-[10px]">
            v{app.latestVersion.versionName}
          </span>
        )}
      </div>
    </Link>
  );
}
