"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { AppAvatar } from "@/components/app-avatar";
import {
  commitGalleryUpload,
  commitLogoUpload,
  commitVersionUpload,
  deleteGallery,
  deleteOldVersions,
  deleteVersion,
  formatDate,
  getApp,
  initGalleryUpload,
  initLogoUpload,
  initVersionUpload,
  listCategories,
  listGallery,
  listVersions,
  publishApp,
  unpublishApp,
  updateApp,
  uploadToStorage,
  type AppItem,
  type AppVersion,
  type Category,
  type GalleryImage,
} from "@/lib/app-api";

const MAX_VERSION_BYTES = 500 * 1024 * 1024;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

const statusStyles: Record<string, string> = {
  DRAFT:
    "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300",
  PUBLISHED:
    "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300",
  STUB: "border-border bg-background text-muted",
};

const statusLabels: Record<string, string> = {
  DRAFT: "Черновик",
  PUBLISHED: "Опубликовано",
  STUB: "Удалено",
};

type MetadataForm = {
  name: string;
  slug: string;
  description: string;
  price: string;
  categoryIds: string[];
};

function metadataFrom(app: AppItem): MetadataForm {
  return {
    name: app.name,
    slug: app.slug,
    description: app.description ?? "",
    price: app.price === null ? "0" : String(app.price),
    categoryIds: app.categories.map((category) => category.id),
  };
}

function humanFileSize(bytes: number): string {
  if (bytes >= 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} ГБ`;
  }
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`;
  }
  return `${Math.max(1, Math.round(bytes / 1024))} КБ`;
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
}

export default function AppEditor({
  appId,
  userId,
  isAdmin,
  username,
}: {
  appId: string;
  userId: string | null;
  isAdmin: boolean;
  username: string;
}) {
  const [app, setApp] = useState<AppItem | null>(null);
  const [versions, setVersions] = useState<AppVersion[]>([]);
  const [gallery, setGallery] = useState<GalleryImage[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [metadata, setMetadata] = useState<MetadataForm | null>(null);
  const initializedFor = useRef<string | null>(null);

  const [savingMeta, setSavingMeta] = useState(false);
  const [metaError, setMetaError] = useState<string | null>(null);
  const [metaSaved, setMetaSaved] = useState(false);

  const [logoUploading, setLogoUploading] = useState(false);
  const [logoError, setLogoError] = useState<string | null>(null);
  const logoInputRef = useRef<HTMLInputElement>(null);

  const [galleryUploading, setGalleryUploading] = useState(false);
  const [galleryError, setGalleryError] = useState<string | null>(null);
  const [deletingImageId, setDeletingImageId] = useState<string | null>(null);
  const galleryInputRef = useRef<HTMLInputElement>(null);

  const [versionName, setVersionName] = useState("");
  const [changelog, setChangelog] = useState("");
  const [replaceForVersionId, setReplaceForVersionId] = useState("");
  const [versionFile, setVersionFile] = useState<File | null>(null);
  const [versionUploading, setVersionUploading] = useState(false);
  const [versionError, setVersionError] = useState<string | null>(null);
  const versionInputRef = useRef<HTMLInputElement>(null);

  const [busyVersionId, setBusyVersionId] = useState<string | null>(null);
  const [deletingOld, setDeletingOld] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [publishError, setPublishError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    getApp(appId)
      .then((result) => {
        if (!active) {
          return;
        }
        setApp(result);
        return Promise.all([listVersions(result.id), listGallery(result.id)]);
      })
      .then((result) => {
        if (!active || !result) {
          return;
        }
        setVersions(result[0]);
        setGallery(result[1]);
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(errorMessage(requestError, "Не удалось загрузить приложение."));
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
  }, [appId, refreshKey]);

  useEffect(() => {
    let active = true;
    listCategories()
      .then((result) => {
        if (active) {
          setCategories(result);
        }
      })
      .catch(() => {
        // Categories are optional for editing.
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (app && initializedFor.current !== app.id) {
      initializedFor.current = app.id;
      setMetadata(metadataFrom(app));
    }
  }, [app]);

  async function refreshApp() {
    const result = await getApp(appId);
    setApp(result);
    return result;
  }

  function toggleCategory(categoryId: string) {
    setMetadata((current) => {
      if (!current) {
        return current;
      }
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
    setMetaSaved(false);
  }

  async function submitMetadata(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!metadata || !app) {
      return;
    }

    const price = Number(metadata.price);
    if (!Number.isFinite(price) || price < 0) {
      setMetaError("Цена должна быть неотрицательным числом.");
      return;
    }

    setSavingMeta(true);
    setMetaError(null);
    setMetaSaved(false);
    try {
      const updated = await updateApp(app.id, {
        name: metadata.name.trim(),
        description: metadata.description.trim() || null,
        price,
        categoryIds: metadata.categoryIds,
        // The backend rejects slug changes after the first publication.
        ...(app.publishedAt === null ? { slug: metadata.slug.trim() } : {}),
      });
      setApp(updated);
      setMetadata(metadataFrom(updated));
      setMetaSaved(true);
    } catch (requestError) {
      setMetaError(errorMessage(requestError, "Не удалось сохранить изменения."));
    } finally {
      setSavingMeta(false);
    }
  }

  async function handleLogoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !app) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      setLogoError("Логотип должен быть изображением PNG, JPEG или WebP.");
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setLogoError(`Логотип не больше ${humanFileSize(MAX_IMAGE_BYTES)}.`);
      return;
    }

    setLogoUploading(true);
    setLogoError(null);
    setNotice(null);
    try {
      const ticket = await initLogoUpload(app.id);
      await uploadToStorage(ticket, file);
      const updated = await commitLogoUpload(app.id, ticket.fileKey);
      setApp(updated);
      setNotice("Логотип обновлён.");
    } catch (requestError) {
      setLogoError(errorMessage(requestError, "Не удалось загрузить логотип."));
    } finally {
      setLogoUploading(false);
    }
  }

  async function handleGalleryChange(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    if (files.length === 0 || !app) {
      return;
    }

    const image = files.find((file) => file.type.startsWith("image/"));
    if (!image) {
      setGalleryError("Скриншот должен быть изображением PNG, JPEG или WebP.");
      return;
    }
    if (image.size > MAX_IMAGE_BYTES) {
      setGalleryError(`Скриншот не больше ${humanFileSize(MAX_IMAGE_BYTES)}.`);
      return;
    }
    if (gallery.length >= 10) {
      setGalleryError("В галерее уже 10 изображений. Удалите лишнее.");
      return;
    }

    setGalleryUploading(true);
    setGalleryError(null);
    setNotice(null);
    try {
      const ticket = await initGalleryUpload(app.id);
      await uploadToStorage(ticket, image);
      const sortOrder = Math.min(gallery.length, 9);
      await commitGalleryUpload(app.id, ticket.fileKey, sortOrder);
      const [updatedGallery, updatedApp] = await Promise.all([
        listGallery(app.id),
        refreshApp(),
      ]);
      setGallery(updatedGallery);
      setApp(updatedApp);
      setNotice("Изображение добавлено в галерею.");
    } catch (requestError) {
      setGalleryError(errorMessage(requestError, "Не удалось загрузить изображение."));
    } finally {
      setGalleryUploading(false);
    }
  }

  async function removeGalleryImage(imageId: string) {
    if (!app) {
      return;
    }
    setDeletingImageId(imageId);
    setGalleryError(null);
    try {
      await deleteGallery(app.id, imageId);
      setGallery((current) => current.filter((image) => image.id !== imageId));
      await refreshApp();
    } catch (requestError) {
      setGalleryError(errorMessage(requestError, "Не удалось удалить изображение."));
    } finally {
      setDeletingImageId(null);
    }
  }

  async function submitVersion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!app) {
      return;
    }
    const file = versionFile;
    if (!file) {
      setVersionError("Выберите файл версии.");
      return;
    }
    if (file.size > MAX_VERSION_BYTES) {
      setVersionError(`Файл версии не больше ${humanFileSize(MAX_VERSION_BYTES)}.`);
      return;
    }
    if (!versionName.trim()) {
      setVersionError("Укажите версию в формате SemVer, например 1.0.0.");
      return;
    }

    setVersionUploading(true);
    setVersionError(null);
    setNotice(null);
    try {
      const payload = {
        versionName: versionName.trim(),
        changelog: changelog.trim() || null,
        replaceForVersionId: replaceForVersionId || null,
      };
      const ticket = await initVersionUpload(app.id, payload);
      await uploadToStorage(ticket, file);
      await commitVersionUpload(app.id, { ...payload, fileKey: ticket.fileKey });
      const [updatedVersions, updatedApp] = await Promise.all([
        listVersions(app.id),
        refreshApp(),
      ]);
      setVersions(updatedVersions);
      setApp(updatedApp);
      setVersionName("");
      setChangelog("");
      setReplaceForVersionId("");
      setVersionFile(null);
      if (versionInputRef.current) {
        versionInputRef.current.value = "";
      }
      setNotice("Версия загружена.");
    } catch (requestError) {
      setVersionError(errorMessage(requestError, "Не удалось загрузить версию."));
    } finally {
      setVersionUploading(false);
    }
  }

  async function removeVersion(version: AppVersion) {
    if (!app) {
      return;
    }
    if (!window.confirm(`Удалить версию ${version.versionName}? Файл будет удалён.`)) {
      return;
    }
    setBusyVersionId(version.id);
    setVersionError(null);
    setNotice(null);
    try {
      await deleteVersion(app.id, version.id);
      const [updatedVersions, updatedApp] = await Promise.all([
        listVersions(app.id),
        refreshApp(),
      ]);
      setVersions(updatedVersions);
      setApp(updatedApp);
      setNotice(`Версия ${version.versionName} удалена.`);
    } catch (requestError) {
      setVersionError(errorMessage(requestError, "Не удалось удалить версию."));
    } finally {
      setBusyVersionId(null);
    }
  }

  async function removeOldVersions() {
    if (!app) {
      return;
    }
    if (!window.confirm("Удалить все версии, кроме последней?")) {
      return;
    }
    setDeletingOld(true);
    setVersionError(null);
    setNotice(null);
    try {
      const result = await deleteOldVersions(app.id);
      const [updatedVersions, updatedApp] = await Promise.all([
        listVersions(app.id),
        refreshApp(),
      ]);
      setVersions(updatedVersions);
      setApp(updatedApp);
      setNotice(`Удалено старых версий: ${result.deleted}.`);
    } catch (requestError) {
      setVersionError(errorMessage(requestError, "Не удалось удалить старые версии."));
    } finally {
      setDeletingOld(false);
    }
  }

  async function togglePublication() {
    if (!app) {
      return;
    }
    setPublishing(true);
    setPublishError(null);
    setNotice(null);
    try {
      const updated =
        app.status === "PUBLISHED" ? await unpublishApp(app.id) : await publishApp(app.id);
      setApp(updated);
      setMetadata(metadataFrom(updated));
      setNotice(
        updated.status === "PUBLISHED"
          ? "Приложение опубликовано в каталоге."
          : "Приложение снято с публикации.",
      );
    } catch (requestError) {
      setPublishError(errorMessage(requestError, "Не удалось изменить статус."));
    } finally {
      setPublishing(false);
    }
  }

  if (loading) {
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

  if (error || !app || !metadata) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-rose-500/30 bg-card p-8 text-center">
          <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-rose-600 dark:text-rose-400">
            Редактор приложения
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
              href="/developer"
              className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
            >
              В кабинет
            </Link>
          </div>
        </section>
      </main>
    );
  }

  const isOwner = userId !== null && Number(userId) === app.developerId;
  const allowed = isAdmin || isOwner;

  if (!allowed) {
    return (
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-16 sm:px-6">
        <section className="rounded-3xl border border-border bg-card p-8 text-center shadow-sm">
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">Нет доступа</h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-muted">
            Приложение принадлежит другому разработчику. Вы вошли как {username}.
          </p>
          <Link
            href="/developer"
            className="mt-6 inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85"
          >
            К моим приложениям
          </Link>
        </section>
      </main>
    );
  }

  const slugFrozen = app.publishedAt !== null;
  const isPublished = app.status === "PUBLISHED";
  const latestVersionId = app.latestVersion?.id ?? null;

  return (
    <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-10 sm:px-6 lg:px-8">
      <nav aria-label="Хлебные крошки" className="flex items-center gap-2 text-xs text-muted">
        <Link href="/developer" className="transition-colors hover:text-foreground">
          Кабинет разработчика
        </Link>
        <span aria-hidden="true">/</span>
        <span className="truncate text-foreground">{app.name}</span>
      </nav>

      <div className="mt-5 flex flex-wrap items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-3">
          <AppAvatar name={app.name} logoUrl={app.logoUrl} className="h-14 w-14" />
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="truncate text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">
                {app.name}
              </h1>
              <span
                className={`rounded-full border px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider ${
                  statusStyles[app.status] ?? "border-border bg-background text-muted"
                }`}
              >
                {statusLabels[app.status] ?? app.status}
              </span>
            </div>
            <p className="mt-1 font-mono text-xs text-muted">@{app.slug}</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {app.status === "PUBLISHED" && (
            <Link
              href={`/apps/${encodeURIComponent(app.slug || app.id)}`}
              className="inline-flex h-10 items-center rounded-full border border-border bg-card px-5 text-sm font-semibold text-foreground transition-colors hover:border-emerald-500/50"
            >
              Открыть в каталоге
            </Link>
          )}
          <button
            type="button"
            onClick={togglePublication}
            disabled={publishing || (!isPublished && versions.length === 0)}
            title={
              !isPublished && versions.length === 0
                ? "Сначала загрузите хотя бы одну версию"
                : undefined
            }
            className={
              isPublished
                ? "inline-flex h-10 items-center rounded-full border border-amber-500/40 bg-amber-500/10 px-5 text-sm font-semibold text-amber-700 transition-colors hover:bg-amber-500/15 disabled:cursor-not-allowed disabled:opacity-50 dark:text-amber-300"
                : "inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-40"
            }
          >
            {publishing
              ? "Применяем…"
              : isPublished
                ? "Снять с публикации"
                : "Опубликовать"}
          </button>
        </div>
      </div>

      {publishError && (
        <p className="mt-4 rounded-2xl border border-rose-500/30 bg-rose-500/5 px-4 py-3 text-sm font-medium text-rose-600 dark:text-rose-400">
          {publishError}
        </p>
      )}
      {notice && !publishError && (
        <p className="mt-4 rounded-2xl border border-emerald-500/30 bg-emerald-500/5 px-4 py-3 text-sm font-medium text-emerald-700 dark:text-emerald-300">
          {notice}
        </p>
      )}

      <div className="mt-6 grid gap-4 lg:grid-cols-[1.4fr_1fr]">
        <div className="space-y-4">
          <section className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-lg font-semibold text-foreground">Описание и цена</h2>
              {metaSaved && (
                <span className="text-xs font-medium text-emerald-600 dark:text-emerald-400">
                  Сохранено
                </span>
              )}
            </div>

            <form onSubmit={submitMetadata} className="mt-5 grid gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-xs font-medium text-muted">
                  Название
                  <input
                    required
                    maxLength={200}
                    value={metadata.name}
                    onChange={(event) => {
                      setMetadata({ ...metadata, name: event.target.value });
                      setMetaSaved(false);
                    }}
                    className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none focus:border-emerald-500"
                  />
                </label>
                <label className="block text-xs font-medium text-muted">
                  Slug
                  <input
                    required
                    readOnly={slugFrozen}
                    pattern="[a-zA-Z0-9]{2,64}"
                    maxLength={64}
                    value={metadata.slug}
                    onChange={(event) => {
                      setMetadata({ ...metadata, slug: event.target.value });
                      setMetaSaved(false);
                    }}
                    className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 font-mono text-sm text-foreground outline-none read-only:cursor-not-allowed read-only:opacity-60 focus:border-emerald-500"
                  />
                  <span className="mt-1 block text-[10px] font-normal leading-relaxed text-muted">
                    {slugFrozen
                      ? "Slug заморожен после первой публикации."
                      : "Латиница и цифры, 2–64 символа. После публикации изменить нельзя."}
                  </span>
                </label>
              </div>

              <label className="block text-xs font-medium text-muted">
                Описание
                <textarea
                  rows={5}
                  maxLength={5000}
                  value={metadata.description}
                  onChange={(event) => {
                    setMetadata({ ...metadata, description: event.target.value });
                    setMetaSaved(false);
                  }}
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
                    value={metadata.price}
                    onChange={(event) => {
                      setMetadata({ ...metadata, price: event.target.value });
                      setMetaSaved(false);
                    }}
                    className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none focus:border-emerald-500"
                  />
                </label>
                <div className="text-xs font-medium text-muted">
                  Категории (до 3)
                  <div className="mt-1.5 flex flex-wrap gap-1.5">
                    {categories.map((category) => {
                      const selected = metadata.categoryIds.includes(category.id);
                      const disabled = !selected && metadata.categoryIds.length >= 3;
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

              {metaError && (
                <p className="text-sm font-medium text-rose-600 dark:text-rose-400">{metaError}</p>
              )}

              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={savingMeta}
                  className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85 disabled:opacity-50"
                >
                  {savingMeta ? "Сохраняем…" : "Сохранить"}
                </button>
              </div>
            </form>
          </section>

          <section className="rounded-3xl border border-border bg-card p-5 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-lg font-semibold text-foreground">Версии</h2>
              {versions.length > 1 && (
                <button
                  type="button"
                  onClick={removeOldVersions}
                  disabled={deletingOld}
                  className="inline-flex h-8 items-center rounded-full border border-border bg-background px-3 text-xs font-semibold text-foreground transition-colors hover:border-rose-500/40 hover:text-rose-600 disabled:opacity-50"
                >
                  {deletingOld ? "Удаляем…" : "Удалить старые"}
                </button>
              )}
            </div>

            {versions.length === 0 ? (
              <p className="mt-4 rounded-2xl border border-dashed border-border bg-background/60 px-4 py-6 text-center text-sm text-muted">
                Версий пока нет. Публикация станет доступна после загрузки первой версии.
              </p>
            ) : (
              <ul className="mt-4 space-y-2">
                {versions.map((version) => {
                  const isLatest = version.id === latestVersionId;
                  return (
                    <li
                      key={version.id}
                      className="rounded-2xl border border-border bg-background/60 px-4 py-3"
                    >
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-mono text-sm font-semibold text-foreground">
                          v{version.versionName}
                        </span>
                        {isLatest && (
                          <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-emerald-700 dark:text-emerald-300">
                            Последняя
                          </span>
                        )}
                        <span className="text-[11px] text-muted">{formatDate(version.createdAt)}</span>
                        <button
                          type="button"
                          onClick={() => removeVersion(version)}
                          disabled={busyVersionId === version.id}
                          className="ml-auto inline-flex h-7 items-center rounded-full border border-border bg-card px-3 text-[11px] font-semibold text-rose-600 transition-colors hover:border-rose-500/40 disabled:opacity-50 dark:text-rose-400"
                        >
                          {busyVersionId === version.id ? "Удаляем…" : "Удалить"}
                        </button>
                      </div>
                      {version.changelog && (
                        <p className="mt-2 whitespace-pre-line text-xs leading-relaxed text-muted">
                          {version.changelog}
                        </p>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}

            <form
              onSubmit={submitVersion}
              className="mt-5 rounded-2xl border border-border bg-background/40 p-4"
            >
              <h3 className="text-sm font-semibold text-foreground">Загрузить версию</h3>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <label className="block text-xs font-medium text-muted">
                  Версия (SemVer)
                  <input
                    required
                    value={versionName}
                    onChange={(event) => setVersionName(event.target.value)}
                    placeholder="1.0.0"
                    pattern="\d+\.\d+\.\d+([+\-].*)?"
                    className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 font-mono text-sm text-foreground outline-none focus:border-emerald-500"
                  />
                </label>
                <label className="block text-xs font-medium text-muted">
                  Заменить версию (необязательно)
                  <select
                    value={replaceForVersionId}
                    onChange={(event) => setReplaceForVersionId(event.target.value)}
                    className="mt-1.5 h-10 w-full rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none focus:border-emerald-500"
                  >
                    <option value="">Не заменять</option>
                    {versions.map((version) => (
                      <option key={version.id} value={version.id}>
                        v{version.versionName}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label className="mt-3 block text-xs font-medium text-muted">
                Список изменений
                <textarea
                  rows={2}
                  maxLength={10000}
                  value={changelog}
                  onChange={(event) => setChangelog(event.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-emerald-500"
                />
              </label>

              <div className="mt-3 flex flex-wrap items-center gap-3">
                <input
                  ref={versionInputRef}
                  type="file"
                  onChange={(event) => setVersionFile(event.target.files?.[0] ?? null)}
                  className="block w-full max-w-sm text-xs text-muted file:mr-3 file:cursor-pointer file:rounded-full file:border-0 file:bg-foreground file:px-4 file:py-2 file:text-xs file:font-semibold file:text-background hover:file:opacity-85"
                />
                <span className="text-[11px] text-muted">
                  До {humanFileSize(MAX_VERSION_BYTES)}: zip, tar, gz, 7z, rar, jar, apk, exe,
                  msi, dmg.
                </span>
              </div>

              {versionError && (
                <p className="mt-3 text-sm font-medium text-rose-600 dark:text-rose-400">
                  {versionError}
                </p>
              )}

              <div className="mt-4 flex justify-end">
                <button
                  type="submit"
                  disabled={versionUploading || !versionFile}
                  className="inline-flex h-10 items-center rounded-full bg-foreground px-5 text-sm font-semibold text-background transition-opacity hover:opacity-85 disabled:opacity-40"
                >
                  {versionUploading ? "Загружаем…" : "Загрузить версию"}
                </button>
              </div>
            </form>
          </section>
        </div>

        <div className="space-y-4">
          <section className="rounded-3xl border border-border bg-card p-5 shadow-sm">
            <h2 className="text-lg font-semibold text-foreground">Логотип</h2>
            <div className="mt-4 flex items-center gap-4">
              <AppAvatar
                name={app.name}
                logoUrl={app.logoUrl}
                className="h-20 w-20"
                rounded="rounded-2xl"
                textClassName="text-2xl"
              />
              <div className="min-w-0 flex-1">
                <input
                  ref={logoInputRef}
                  type="file"
                  accept="image/png,image/jpeg,image/webp"
                  onChange={handleLogoChange}
                  className="block w-full text-xs text-muted file:mr-3 file:cursor-pointer file:rounded-full file:border-0 file:bg-foreground file:px-4 file:py-2 file:text-xs file:font-semibold file:text-background hover:file:opacity-85"
                />
                <p className="mt-2 text-[11px] leading-relaxed text-muted">
                  PNG, JPEG или WebP до {humanFileSize(MAX_IMAGE_BYTES)}. Квадратное
                  изображение выглядит лучше.
                </p>
                {logoUploading && (
                  <p className="mt-2 text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
                    Загружаем логотип…
                  </p>
                )}
                {logoError && (
                  <p className="mt-2 text-[11px] font-medium text-rose-600 dark:text-rose-400">
                    {logoError}
                  </p>
                )}
              </div>
            </div>
          </section>

          <section className="rounded-3xl border border-border bg-card p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-lg font-semibold text-foreground">Галерея</h2>
              <span className="text-[11px] text-muted">{gallery.length} / 10</span>
            </div>

            {gallery.length === 0 ? (
              <p className="mt-3 rounded-2xl border border-dashed border-border bg-background/60 px-4 py-5 text-center text-xs text-muted">
                Скриншотов пока нет.
              </p>
            ) : (
              <div className="mt-3 grid grid-cols-2 gap-2">
                {gallery.map((image) => (
                  <div
                    key={image.id}
                    className="group relative overflow-hidden rounded-xl border border-border bg-background"
                  >
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img
                      src={image.url}
                      alt=""
                      loading="lazy"
                      className="aspect-video w-full object-cover"
                    />
                    <button
                      type="button"
                      onClick={() => removeGalleryImage(image.id)}
                      disabled={deletingImageId === image.id}
                      aria-label="Удалить изображение"
                      className="absolute right-1.5 top-1.5 grid h-7 w-7 place-items-center rounded-full bg-black/60 text-white transition-colors hover:bg-rose-600 disabled:opacity-50"
                    >
                      <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        className="h-3.5 w-3.5"
                        aria-hidden="true"
                      >
                        <path d="M6 6l12 12M18 6 6 18" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className="mt-3">
              <input
                ref={galleryInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                onChange={handleGalleryChange}
                disabled={gallery.length >= 10 || galleryUploading}
                className="block w-full text-xs text-muted file:mr-3 file:cursor-pointer file:rounded-full file:border-0 file:bg-foreground file:px-4 file:py-2 file:text-xs file:font-semibold file:text-background hover:file:opacity-85 disabled:opacity-40"
              />
              {galleryUploading && (
                <p className="mt-2 text-[11px] font-medium text-emerald-600 dark:text-emerald-400">
                  Загружаем изображение…
                </p>
              )}
              {galleryError && (
                <p className="mt-2 text-[11px] font-medium text-rose-600 dark:text-rose-400">
                  {galleryError}
                </p>
              )}
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}
