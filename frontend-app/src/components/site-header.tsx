import { getSession } from "@/lib/session";
import { HeaderShell } from "./header-shell";

/**
 * Reads the identity forwarded by the gateway and renders the shared header.
 * The component is intentionally rendered inside a Suspense boundary in the
 * root layout: reading request headers is a request-time operation.
 */
export async function SiteHeader() {
  const user = await getSession();

  const loginUrl =
    process.env.OAUTH_LOGIN_URL ?? "/oauth2/authorization/gateway-client-oidc";
  const docsUrl = process.env.API_DOCS_URL ?? "/swagger-ui.html";
  const grafanaUrl = process.env.GRAFANA_URL ?? "/grafana/";
  const settingsUrl = process.env.USER_SETTINGS_URL ?? "/auth-server/settings";
  const adminUrl = process.env.ADMIN_PANEL_URL ?? "/admin";

  return (
    <HeaderShell
      user={user}
      loginUrl={loginUrl}
      docsUrl={docsUrl}
      grafanaUrl={grafanaUrl}
      settingsUrl={settingsUrl}
      adminUrl={adminUrl}
    />
  );
}
