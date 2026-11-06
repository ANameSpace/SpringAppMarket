import { Suspense } from "react";
import { getSession } from "@/lib/session";
import AppDetailContent from "./app-detail-content";

const loginUrl =
  process.env.OAUTH_LOGIN_URL ?? "/oauth2/authorization/gateway-client-oidc";

function DetailFallback() {
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

export default function AppDetailPage({
  params,
}: {
  params: Promise<{ slugOrId: string }>;
}) {
  return (
    <Suspense fallback={<DetailFallback />}>
      <AppDetailRoute params={params} />
    </Suspense>
  );
}

async function AppDetailRoute({
  params,
}: {
  params: Promise<{ slugOrId: string }>;
}) {
  const [{ slugOrId }, user] = await Promise.all([params, getSession()]);

  return (
    <AppDetailContent
      slugOrId={slugOrId}
      authenticated={Boolean(user)}
      isAdmin={user?.roles.includes("ADMIN") ?? false}
      userId={user?.userId ?? null}
      loginUrl={loginUrl}
    />
  );
}
