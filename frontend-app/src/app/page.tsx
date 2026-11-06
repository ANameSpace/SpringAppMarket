import { Suspense } from "react";
import HomeContent from "./home-content";

function HomeFallback() {
  return (
    <main className="flex flex-1 items-center justify-center px-6 py-20">
      <div className="w-full max-w-6xl">
        <div className="mx-auto h-6 w-48 animate-pulse rounded-full bg-border" />
        <div className="mx-auto mt-6 h-12 w-full max-w-2xl animate-pulse rounded-2xl bg-border" />
        <div className="mx-auto mt-4 h-4 w-full max-w-xl animate-pulse rounded-full bg-border/70" />
        <div className="mx-auto mt-10 grid max-w-3xl gap-4 sm:grid-cols-3">
          <div className="h-28 animate-pulse rounded-3xl bg-border/60" />
          <div className="h-28 animate-pulse rounded-3xl bg-border/60" />
          <div className="h-28 animate-pulse rounded-3xl bg-border/60" />
        </div>
      </div>
    </main>
  );
}

export default function Home() {
  return (
    <Suspense fallback={<HomeFallback />}>
      <HomeContent />
    </Suspense>
  );
}
