export function AppAvatar({
  name,
  logoUrl,
  className = "h-12 w-12",
  rounded = "rounded-2xl",
  textClassName = "text-base",
}: {
  name: string;
  logoUrl: string | null;
  className?: string;
  rounded?: string;
  textClassName?: string;
}) {
  if (logoUrl) {
    return (
      // Presigned S3 URLs are short-lived and host-specific, so next/image
      // remote patterns would only add configuration churn here.
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={logoUrl}
        alt=""
        className={`${className} ${rounded} shrink-0 border border-border bg-card object-cover`}
      />
    );
  }

  return (
    <span
      aria-hidden="true"
      className={`${className} ${rounded} grid shrink-0 place-items-center bg-gradient-to-br from-emerald-400 via-teal-500 to-cyan-600 font-semibold uppercase text-white shadow-sm shadow-emerald-500/20 ${textClassName}`}
    >
      {name.slice(0, 1)}
    </span>
  );
}
