export type AppStatus = "DRAFT" | "PUBLISHED" | "STUB";

export type DownloadState =
  | "NOT_PURCHASED"
  | "PURCHASED_NOT_DOWNLOADED"
  | "UP_TO_DATE"
  | "UPDATE_AVAILABLE"
  | "OWN_APP_NOT_DOWNLOADED"
  | "OWN_APP_UP_TO_DATE"
  | "OWN_APP_UPDATE_AVAILABLE"
  | "NOT_AVAILABLE_FOR_ADMIN"
  | "NOT_AVAILABLE";

export type Category = {
  id: string;
  slug: string;
  name: string;
  svgIcon: string | null;
  createdAt: string;
};

export type AppVersion = {
  id: string;
  versionName: string;
  changelog: string | null;
  createdAt: string;
};

export type AppItem = {
  id: string;
  slug: string;
  name: string;
  description: string | null;
  developerId: number;
  price: number | null;
  categories: Category[];
  tags: string[];
  logoUrl: string | null;
  galleryUrls: string[];
  latestVersion: AppVersion | null;
  cachedRating: number;
  cachedRatingCount: number;
  downloadCount: number;
  status: AppStatus;
  downloadState: DownloadState | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type GalleryImage = {
  id: string;
  url: string;
  sortOrder: number;
};

export type UploadTicket = {
  uploadUrl: string;
  fileKey: string;
  expiresAt: string;
};

export type DownloadTicket = {
  downloadUrl: string;
  fileName: string;
  expiresAt: string;
};

export type AppSort =
  | "rating_desc"
  | "downloads_desc"
  | "name_asc"
  | "name_desc"
  | "price_asc"
  | "price_desc";

export type CreateAppPayload = {
  name: string;
  slug: string;
  description?: string | null;
  price: number;
  categoryIds: string[];
};

export type UpdateAppPayload = {
  name?: string;
  slug?: string;
  description?: string | null;
  price?: number;
  categoryIds?: string[];
};

export type VersionUploadPayload = {
  versionName: string;
  changelog?: string | null;
  replaceForVersionId?: string | null;
};

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function readError(response: Response): Promise<string> {
  if (response.redirected && response.url.includes("oauth2")) {
    return "Сессия истекла. Войдите заново.";
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    try {
      const body = (await response.json()) as { message?: string; error?: string };
      if (body.message) {
        return body.message;
      }
      if (body.error) {
        return body.error;
      }
    } catch {
      // Fall through to the generic status message.
    }
  }

  if (response.status === 401) {
    return "Требуется вход.";
  }
  if (response.status === 403) {
    return "Недостаточно прав.";
  }
  if (response.status === 404) {
    return "Не найдено.";
  }
  if (response.status === 409) {
    return "Конфликт: такое значение уже используется.";
  }
  return `Запрос не выполнен (${response.status}).`;
}

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (init?.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(path, {
    cache: "no-store",
    credentials: "same-origin",
    ...init,
    headers,
  });

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export function listApps(params: {
  page: number;
  size: number;
  sort?: AppSort;
  q?: string;
  categoryIds?: string[];
  status?: AppStatus;
}): Promise<PageResponse<AppItem>> {
  const search = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
  });
  if (params.sort) {
    search.set("sort", params.sort);
  }
  if (params.q?.trim()) {
    search.set("q", params.q.trim());
  }
  if (params.status) {
    search.set("status", params.status);
  }
  for (const categoryId of params.categoryIds ?? []) {
    search.append("categoryIds", categoryId);
  }
  return api<PageResponse<AppItem>>(`/api/v1/apps?${search.toString()}`);
}

export function getApp(slugOrId: string): Promise<AppItem> {
  return api<AppItem>(`/api/v1/apps/${encodeURIComponent(slugOrId)}`);
}

export function listCategories(): Promise<Category[]> {
  return api<Category[]>("/api/v1/categories");
}

export function createApp(payload: CreateAppPayload): Promise<AppItem> {
  return api<AppItem>("/api/v1/apps", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateApp(id: string, payload: UpdateAppPayload): Promise<AppItem> {
  return api<AppItem>(`/api/v1/apps/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function publishApp(id: string): Promise<AppItem> {
  return api<AppItem>(`/api/v1/apps/${id}/publish`, { method: "POST" });
}

export function unpublishApp(id: string): Promise<AppItem> {
  return api<AppItem>(`/api/v1/apps/${id}/unpublish`, { method: "POST" });
}

export function listVersions(appId: string): Promise<AppVersion[]> {
  return api<AppVersion[]>(`/api/v1/apps/${appId}/versions`);
}

export function initVersionUpload(
  appId: string,
  payload: VersionUploadPayload,
): Promise<UploadTicket> {
  return api<UploadTicket>(`/api/v1/apps/${appId}/versions/upload-init`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function commitVersionUpload(
  appId: string,
  payload: VersionUploadPayload & { fileKey: string },
): Promise<AppVersion> {
  return api<AppVersion>(`/api/v1/apps/${appId}/versions/upload-commit`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateVersion(
  appId: string,
  versionId: string,
  payload: { versionName?: string; changelog?: string },
): Promise<AppVersion> {
  return api<AppVersion>(`/api/v1/apps/${appId}/versions/${versionId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deleteVersion(appId: string, versionId: string): Promise<void> {
  return api<void>(`/api/v1/apps/${appId}/versions/${versionId}`, {
    method: "DELETE",
  });
}

export function deleteOldVersions(appId: string): Promise<{ deleted: number }> {
  return api<{ deleted: number }>(`/api/v1/apps/${appId}/versions/old`, {
    method: "DELETE",
  });
}

export function initLogoUpload(appId: string): Promise<UploadTicket> {
  return api<UploadTicket>(`/api/v1/apps/${appId}/logo/upload-init`, {
    method: "POST",
  });
}

export function commitLogoUpload(appId: string, fileKey: string): Promise<AppItem> {
  return api<AppItem>(`/api/v1/apps/${appId}/logo/upload-commit`, {
    method: "POST",
    body: JSON.stringify({ fileKey }),
  });
}

export function initGalleryUpload(appId: string): Promise<UploadTicket> {
  return api<UploadTicket>(`/api/v1/apps/${appId}/gallery/upload-init`, {
    method: "POST",
  });
}

export function commitGalleryUpload(
  appId: string,
  fileKey: string,
  sortOrder: number,
): Promise<GalleryImage> {
  return api<GalleryImage>(`/api/v1/apps/${appId}/gallery/upload-commit`, {
    method: "POST",
    body: JSON.stringify({ fileKey, sortOrder }),
  });
}

export function listGallery(appId: string): Promise<GalleryImage[]> {
  return api<GalleryImage[]>(`/api/v1/apps/${appId}/gallery`);
}

export function deleteGallery(appId: string, imageId: string): Promise<void> {
  return api<void>(`/api/v1/apps/${appId}/gallery/${imageId}`, {
    method: "DELETE",
  });
}

export function requestDownload(slugOrId: string): Promise<DownloadTicket> {
  return api<DownloadTicket>(`/api/v1/apps/${encodeURIComponent(slugOrId)}/download`);
}

export function becomeDeveloper(): Promise<unknown> {
  return api<unknown>("/api/v1/users/me/become-developer", { method: "POST" });
}

/**
 * Uploads a file directly to object storage using the presigned URL issued by
 * app-service. The browser never sends the file through the gateway.
 */
export async function uploadToStorage(ticket: UploadTicket, file: File): Promise<void> {
  const response = await fetch(ticket.uploadUrl, {
    method: "PUT",
    headers: {
      "Content-Type": file.type || "application/octet-stream",
    },
    body: file,
  });
  if (!response.ok) {
    const details = response.status === 403
      ? "хранилище отклонило запрос (проверьте CORS и права ключа)"
      : `код ${response.status}`;
    throw new Error(`Не удалось загрузить файл: ${details}.`);
  }
}

export function startDownload(url: string): void {
  const link = document.createElement("a");
  link.href = url;
  link.rel = "noreferrer";
  document.body.appendChild(link);
  link.click();
  link.remove();
}

export function formatPrice(price: number | null): string {
  if (price === null) {
    return "—";
  }
  if (price === 0) {
    return "Бесплатно";
  }
  return `${price.toLocaleString("ru-RU", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })} ₽`;
}

export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString("ru-RU");
}
