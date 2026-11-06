import { headers } from "next/headers";

export type SessionUser = {
  username: string;
  userId: string | null;
  roles: string[];
};

/**
 * Reads the identity that the gateway established for this request.
 *
 * The frontend does not authenticate users and has no session of its own.
 * The gateway is the OAuth2 client and forwards the authenticated user,
 * numeric user id and roles as request headers. Everything else stays public.
 */
export async function getSession(): Promise<SessionUser | null> {
  const requestHeaders = await headers();
  const username = requestHeaders.get("x-authenticated-user");
  if (!username) {
    return null;
  }

  const roles = requestHeaders.get("x-authenticated-roles");
  return {
    username,
    userId: requestHeaders.get("x-authenticated-user-id"),
    roles: roles
      ? roles.split(",").map((role) => role.trim()).filter(Boolean)
      : [],
  };
}
