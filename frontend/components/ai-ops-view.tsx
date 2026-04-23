"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getAiInteractionLogs,
  getAiRecommendationSettings,
  getProductDiscoveryClickLogs,
  getProductDiscoveryFunnelSummary,
  reviewAiInteraction
} from "@/lib/api";
import { buildAuthPath } from "@/lib/auth";
import { formatDateTime } from "@/lib/format";
import { useSessionStore } from "@/store/session-store";
import type {
  AiInteractionLogResponse,
  ProductDiscoveryClickLogResponse,
  ProductDiscoveryFunnelSummaryResponse
} from "@/lib/types";

const scoreOptions = [5, 4, 3, 2, 1];
const sourceFilters = ["ALL", "ELASTICSEARCH", "DATABASE"] as const;
const llmFilters = ["ALL", "LLM", "FALLBACK"] as const;
const reviewFilters = ["ALL", "REVIEWED", "UNREVIEWED", "LOW_SCORE"] as const;
const clickSurfaceFilters = ["ALL", "SEARCH", "AI_RECOMMENDATION"] as const;
const bucketFilters = ["ALL", "DEFAULT", "CONTROL", "CTR_RANKING"] as const;
const rangeFilters = ["7D", "30D", "ALL"] as const;

type SourceFilter = (typeof sourceFilters)[number];
type LlmFilter = (typeof llmFilters)[number];
type ReviewFilter = (typeof reviewFilters)[number];
type ClickSurfaceFilter = (typeof clickSurfaceFilters)[number];
type BucketFilter = (typeof bucketFilters)[number];
type RangeFilter = (typeof rangeFilters)[number];

export function AiOpsView() {
  const member = useSessionStore((state) => state.member);
  const queryClient = useQueryClient();
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("ALL");
  const [llmFilter, setLlmFilter] = useState<LlmFilter>("ALL");
  const [reviewFilter, setReviewFilter] = useState<ReviewFilter>("ALL");
  const [clickSurfaceFilter, setClickSurfaceFilter] = useState<ClickSurfaceFilter>("ALL");
  const [clickSourceFilter, setClickSourceFilter] = useState<SourceFilter>("ALL");
  const [bucketFilter, setBucketFilter] = useState<BucketFilter>("ALL");
  const [rangeFilter, setRangeFilter] = useState<RangeFilter>("7D");
  const dateRange = useMemo(() => buildDateRange(rangeFilter), [rangeFilter]);

  const { data: logs } = useQuery({
    queryKey: ["admin", "ai-interactions", sourceFilter, bucketFilter, llmFilter, reviewFilter],
    queryFn: () =>
      getAiInteractionLogs(50, {
        recommendationSource: sourceFilter === "ALL" ? undefined : sourceFilter,
        recommendationBucket: bucketFilter === "ALL" ? undefined : bucketFilter,
        llmUsed: toLlmUsedFilter(llmFilter),
        reviewStatus: reviewFilter === "ALL" ? undefined : reviewFilter
      }),
    enabled: member?.role === "ADMIN"
  });

  const { data: recommendationSettings } = useQuery({
    queryKey: ["admin", "ai-recommendation-settings"],
    queryFn: getAiRecommendationSettings,
    enabled: member?.role === "ADMIN"
  });

  const reviewMutation = useMutation({
    mutationFn: ({ id, qualityScore }: { id: number; qualityScore: number }) =>
      reviewAiInteraction(id, {
        qualityScore,
        qualityNote: notes[id]?.trim() || null
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin", "ai-interactions"] });
    }
  });

  const visibleLogs = useMemo(() => logs ?? [], [logs]);
  const lowScoreLogs = useMemo(() => visibleLogs.filter((log) => isLowScore(log)), [visibleLogs]);
  const metrics = useMemo(() => summarize(visibleLogs), [visibleLogs]);

  const { data: clicks } = useQuery({
    queryKey: ["admin", "product-clicks", clickSurfaceFilter, clickSourceFilter, bucketFilter, dateRange.from, dateRange.to],
    queryFn: () =>
      getProductDiscoveryClickLogs(100, {
        surface: clickSurfaceFilter === "ALL" ? undefined : clickSurfaceFilter,
        recommendationSource: clickSourceFilter === "ALL" ? undefined : clickSourceFilter,
        recommendationBucket: bucketFilter === "ALL" ? undefined : bucketFilter,
        from: dateRange.from,
        to: dateRange.to
      }),
    enabled: member?.role === "ADMIN"
  });
  const visibleClicks = useMemo(() => clicks ?? [], [clicks]);
  const clickMetrics = useMemo(() => summarizeClicks(visibleClicks), [visibleClicks]);

  const { data: funnelSummary } = useQuery({
    queryKey: ["admin", "discovery-funnel", clickSurfaceFilter, clickSourceFilter, bucketFilter, dateRange.from, dateRange.to],
    queryFn: () =>
      getProductDiscoveryFunnelSummary({
        surface: clickSurfaceFilter === "ALL" ? undefined : clickSurfaceFilter,
        recommendationSource: clickSourceFilter === "ALL" ? undefined : clickSourceFilter,
        recommendationBucket: bucketFilter === "ALL" ? undefined : bucketFilter,
        from: dateRange.from,
        to: dateRange.to
      }),
    enabled: member?.role === "ADMIN"
  });
  const funnelMetrics = useMemo(() => summarizeFunnel(funnelSummary), [funnelSummary]);
  const topClickedProducts = useMemo(() => funnelSummary?.topProducts ?? [], [funnelSummary]);
  const trendPoints = useMemo(() => funnelSummary?.dailyTrend ?? [], [funnelSummary]);
  const maxTrendCtr = useMemo(() => Math.max(0, ...trendPoints.map((point) => point.ctr)), [trendPoints]);
  const bucketComparisons = useMemo(() => summarizeBucketComparisons(funnelSummary), [funnelSummary]);

  if (!member) {
    return (
      <section className="panel auth-callout">
        <div>
          <p className="eyebrow">AI operations</p>
          <h1>Admin login is required to review AI interactions.</h1>
        </div>
        <Link className="button button--dark" href={buildAuthPath({ next: "/admin/ai" })}>
          Open login
        </Link>
      </section>
    );
  }

  if (member.role !== "ADMIN") {
    return (
      <section className="panel auth-callout">
        <div>
          <p className="eyebrow">Admin only</p>
          <h1>Your account cannot access AI operations.</h1>
        </div>
        <Link className="button button--ghostDark" href="/">
          Back home
        </Link>
      </section>
    );
  }

  return (
    <div className="page-stack">
      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">AI quality desk</p>
            <h1>Review chatbot and recommendation traces.</h1>
          </div>
        </div>
        <div className="ops-metric-grid">
          <article>
            <span>Total logs</span>
            <strong>{metrics.total}</strong>
          </article>
          <article>
            <span>LLM used</span>
            <strong>{metrics.llmUsed}</strong>
          </article>
          <article>
            <span>ES sourced</span>
            <strong>{metrics.elasticsearch}</strong>
          </article>
          <article>
            <span>Reviewed</span>
            <strong>{metrics.reviewed}</strong>
          </article>
          <article>
            <span>Average score</span>
            <strong>{metrics.averageScore}</strong>
          </article>
        </div>
        {recommendationSettings ? (
          <div className="guardrail-panel">
            <div>
              <span>Recommendation guardrail</span>
              <strong>{recommendationSettings.ctrSignalLowAction}</strong>
              <small>
                low CTR below {formatPercent(recommendationSettings.ctrSignalLowThreshold)} after{" "}
                {recommendationSettings.ctrSignalMinImpressions} impressions
              </small>
            </div>
            <div>
              <span>CTR scoring</span>
              <strong>{recommendationSettings.ctrSignalEnabled ? "ON" : "OFF"}</strong>
              <small>
                high {formatPercent(recommendationSettings.ctrSignalHighThreshold)} +{recommendationSettings.ctrSignalHighBoost} / mid{" "}
                {formatPercent(recommendationSettings.ctrSignalMidThreshold)} +{recommendationSettings.ctrSignalMidBoost}
              </small>
            </div>
            <div>
              <span>Experiment</span>
              <strong>{recommendationSettings.experimentEnabled ? "ON" : "OFF"}</strong>
              <small>{recommendationSettings.ctrTreatmentPercent}% CTR treatment traffic</small>
            </div>
            <div>
              <span>Click signal</span>
              <strong>{recommendationSettings.clickSignalEnabled ? "ON" : "OFF"}</strong>
              <small>
                {recommendationSettings.clickSignalWindowDays} days / +{recommendationSettings.clickSignalBoostPerClick} per click / max{" "}
                {recommendationSettings.clickSignalMaxClickBoost}
              </small>
            </div>
          </div>
        ) : null}
      </section>

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Discovery funnel</p>
            <h2>Track result impressions, clicks and CTR across search and AI.</h2>
          </div>
          <div className="ai-ops-toolbar">
            <div className="segmented-control" aria-label="Click surface filter">
              {clickSurfaceFilters.map((surface) => (
                <button
                  className={clickSurfaceFilter === surface ? "is-selected" : undefined}
                  key={surface}
                  onClick={() => setClickSurfaceFilter(surface)}
                  type="button"
                >
                  {surface === "ALL" ? "All" : surface === "SEARCH" ? "Search" : "AI"}
                </button>
              ))}
            </div>
            <div className="segmented-control" aria-label="Click source filter">
              {sourceFilters.map((source) => (
                <button
                  className={clickSourceFilter === source ? "is-selected" : undefined}
                  key={source}
                  onClick={() => setClickSourceFilter(source)}
                  type="button"
                >
                  {source === "ALL" ? "All" : source === "ELASTICSEARCH" ? "ES" : "DB"}
                </button>
              ))}
            </div>
            <div className="segmented-control" aria-label="Recommendation bucket filter">
              {bucketFilters.map((bucket) => (
                <button
                  className={bucketFilter === bucket ? "is-selected" : undefined}
                  key={bucket}
                  onClick={() => setBucketFilter(bucket)}
                  type="button"
                >
                  {bucketLabel(bucket)}
                </button>
              ))}
            </div>
            <div className="segmented-control" aria-label="Date range filter">
              {rangeFilters.map((range) => (
                <button
                  className={rangeFilter === range ? "is-selected" : undefined}
                  key={range}
                  onClick={() => setRangeFilter(range)}
                  type="button"
                >
                  {range}
                </button>
              ))}
            </div>
            <button
              className="button button--ghostDark"
              disabled={visibleClicks.length === 0}
              onClick={() => exportClickLogsJson(visibleClicks)}
              type="button"
            >
              JSON
            </button>
            <button
              className="button button--ghostDark"
              disabled={visibleClicks.length === 0}
              onClick={() => exportClickLogsCsv(visibleClicks)}
              type="button"
            >
              CSV
            </button>
          </div>
        </div>
        <div className="ops-metric-grid ops-metric-grid--compact">
          <article>
            <span>Impressions</span>
            <strong>{funnelMetrics.impressions}</strong>
          </article>
          <article>
            <span>Clicks</span>
            <strong>{funnelMetrics.clicks}</strong>
          </article>
          <article>
            <span>CTR</span>
            <strong>{funnelMetrics.ctr}</strong>
          </article>
          <article>
            <span>Search CTR</span>
            <strong>{funnelMetrics.searchCtr}</strong>
          </article>
          <article>
            <span>AI CTR</span>
            <strong>{funnelMetrics.aiCtr}</strong>
          </article>
          <article>
            <span>ES clicks</span>
            <strong>{clickMetrics.elasticsearch}</strong>
          </article>
          <article>
            <span>Avg rank</span>
            <strong>{clickMetrics.averageRank}</strong>
          </article>
        </div>
        <div className="discovery-trend">
          <div className="discovery-trend__head">
            <span>Daily CTR</span>
            <strong>{trendPoints.length ? formatPercent(trendPoints.at(-1)?.ctr ?? 0) : "-"}</strong>
          </div>
          {trendPoints.length ? (
            <div className="discovery-trend__chart">
              {trendPoints.map((point) => (
                <div
                  className="discovery-trend__point"
                  key={point.date}
                  title={`${point.date}: ${point.clicks} clicks / ${point.impressions} impressions`}
                >
                  <div className="discovery-trend__bar" aria-hidden="true">
                    <i style={{ height: `${trendBarHeight(point.ctr, maxTrendCtr)}%` }} />
                  </div>
                  <small>{formatShortDate(point.date)}</small>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">No dated trend is available for this range.</div>
          )}
        </div>
        <div className="experiment-table">
          <div className="experiment-table__head">
            <span>Experiment buckets</span>
            <strong>{bucketComparisons.length} active</strong>
          </div>
          {bucketComparisons.length ? (
            <div className="experiment-table__grid">
              <span>Bucket</span>
              <span>Impressions</span>
              <span>Clicks</span>
              <span>CTR</span>
              <span>Lift</span>
              {bucketComparisons.map((bucket) => (
                <div className="experiment-table__row" key={bucket.key}>
                  <strong>{bucketLabel(bucket.key)}</strong>
                  <span>{bucket.impressions}</span>
                  <span>{bucket.clicks}</span>
                  <span>{formatPercent(bucket.ctr)}</span>
                  <span className={bucket.liftClass}>{bucket.liftLabel}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">No experiment bucket traffic matches this view.</div>
          )}
        </div>
        <div className="discovery-layout">
          <div className="top-products">
            {topClickedProducts.length ? (
              topClickedProducts.map((product) => (
                <article key={product.productId}>
                  <span>#{product.productId}</span>
                  <strong>{product.productName}</strong>
                  <small>
                    {product.clicks} clicks / {product.impressions} impressions / CTR {formatPercent(product.ctr)}
                  </small>
                </article>
              ))
            ) : (
              <div className="empty-state">No product clicks match this view.</div>
            )}
          </div>
          <div className="click-log-list">
            {visibleClicks.length ? (
              visibleClicks.slice(0, 20).map((click) => (
                <article className="click-log-card" key={click.id}>
                  <div>
                    <span>{click.surface === "AI_RECOMMENDATION" ? "AI" : "SEARCH"}</span>
                    <strong>{click.productName}</strong>
                    <small>{click.query}</small>
                  </div>
                  <div>
                    <em>{formatDateTime(click.clickedAt)}</em>
                    <small>
                      rank {click.rankPosition ?? "-"} / {click.recommendationSource ?? "-"}
                      {click.recommendationBucket ? ` / ${bucketLabel(click.recommendationBucket)}` : ""}
                      {click.searchScore ? ` / score ${click.searchScore.toFixed(2)}` : ""}
                    </small>
                  </div>
                </article>
              ))
            ) : (
              <div className="empty-state">No recent clicks match this view.</div>
            )}
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">Recent interactions</p>
            <h2>Score answer quality from one to five.</h2>
          </div>
          <div className="ai-ops-toolbar">
            <div className="segmented-control" aria-label="Recommendation source filter">
              {sourceFilters.map((source) => (
                <button
                  className={sourceFilter === source ? "is-selected" : undefined}
                  key={source}
                  onClick={() => setSourceFilter(source)}
                  type="button"
                >
                  {source === "ALL" ? "All" : source === "ELASTICSEARCH" ? "ES" : "DB"}
                </button>
              ))}
            </div>
            <div className="segmented-control" aria-label="Recommendation bucket filter">
              {bucketFilters.map((bucket) => (
                <button
                  className={bucketFilter === bucket ? "is-selected" : undefined}
                  key={bucket}
                  onClick={() => setBucketFilter(bucket)}
                  type="button"
                >
                  {bucketLabel(bucket)}
                </button>
              ))}
            </div>
            <div className="segmented-control" aria-label="LLM usage filter">
              {llmFilters.map((usage) => (
                <button
                  className={llmFilter === usage ? "is-selected" : undefined}
                  key={usage}
                  onClick={() => setLlmFilter(usage)}
                  type="button"
                >
                  {usage === "ALL" ? "All" : usage === "LLM" ? "LLM" : "Fallback"}
                </button>
              ))}
            </div>
            <div className="segmented-control" aria-label="Review status filter">
              {reviewFilters.map((status) => (
                <button
                  className={reviewFilter === status ? "is-selected" : undefined}
                  key={status}
                  onClick={() => setReviewFilter(status)}
                  type="button"
                >
                  {reviewLabel(status)}
                </button>
              ))}
            </div>
            <button
              className="button button--ghostDark"
              disabled={lowScoreLogs.length === 0}
              onClick={() => exportLowScoreLogs(lowScoreLogs)}
              type="button"
            >
              Export lows
            </button>
          </div>
        </div>
        <div className="ai-log-list">
          {visibleLogs.length ? (
            visibleLogs.map((log) => (
              <article className="ai-log-card" key={log.id}>
                <div className="ai-log-card__top">
                  <div>
                    <span>{log.interactionType}</span>
                    <strong>#{log.id}</strong>
                  </div>
                  <em>{formatDateTime(log.requestedAt)}</em>
                </div>
                <div className="ai-log-card__body">
                  <p>{log.prompt}</p>
                  {log.reply ? <blockquote>{log.reply}</blockquote> : null}
                  <small>
                    member {log.memberId ?? "guest"} / {log.llmUsed ? "LLM" : "fallback"} / {log.recommendationSource} /
                    {bucketLabel(log.recommendationBucket ?? "DEFAULT")} / products {log.recommendedProductIds || "-"}
                  </small>
                </div>
                <div className="ai-review-row">
                  <input
                    aria-label={`Review note for interaction ${log.id}`}
                    placeholder={log.qualityNote ?? "Review note"}
                    value={notes[log.id] ?? ""}
                    onChange={(event) => setNotes((current) => ({ ...current, [log.id]: event.target.value }))}
                  />
                  <div className="score-buttons">
                    {scoreOptions.map((score) => (
                      <button
                        key={score}
                        className={log.qualityScore === score ? "is-selected" : undefined}
                        onClick={() => reviewMutation.mutate({ id: log.id, qualityScore: score })}
                      >
                        {score}
                      </button>
                    ))}
                  </div>
                </div>
              </article>
            ))
          ) : (
            <div className="empty-state">No AI interactions match this view.</div>
          )}
        </div>
      </section>
    </div>
  );
}

function isLowScore(log: AiInteractionLogResponse) {
  return log.qualityScore !== null && log.qualityScore <= 2;
}

function toLlmUsedFilter(llmFilter: LlmFilter) {
  if (llmFilter === "LLM") {
    return true;
  }
  if (llmFilter === "FALLBACK") {
    return false;
  }
  return undefined;
}

function reviewLabel(reviewFilter: ReviewFilter) {
  if (reviewFilter === "LOW_SCORE") {
    return "Low";
  }
  if (reviewFilter === "UNREVIEWED") {
    return "Open";
  }
  if (reviewFilter === "REVIEWED") {
    return "Reviewed";
  }
  return "All";
}

function bucketLabel(bucket: string) {
  if (bucket === "CTR_RANKING") {
    return "CTR";
  }
  if (bucket === "CONTROL") {
    return "Control";
  }
  if (bucket === "DEFAULT") {
    return "Default";
  }
  if (bucket === "UNASSIGNED") {
    return "Unassigned";
  }
  return "All";
}

function exportLowScoreLogs(logs: AiInteractionLogResponse[]) {
  const payload = logs.map((log) => ({
    id: log.id,
    requestedAt: log.requestedAt,
    interactionType: log.interactionType,
    recommendationSource: log.recommendationSource,
    recommendationBucket: log.recommendationBucket,
    llmUsed: log.llmUsed,
    prompt: log.prompt,
    reply: log.reply,
    recommendedProductIds: log.recommendedProductIds,
    qualityScore: log.qualityScore,
    qualityNote: log.qualityNote
  }));
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `ai-low-score-logs-${new Date().toISOString().slice(0, 10)}.json`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function exportClickLogsJson(clicks: ProductDiscoveryClickLogResponse[]) {
  const payload = clicks.map((click) => ({
    id: click.id,
    clickedAt: click.clickedAt,
    memberId: click.memberId,
    surface: click.surface,
    query: click.query,
    productId: click.productId,
    productName: click.productName,
    recommendationSource: click.recommendationSource,
    recommendationBucket: click.recommendationBucket,
    searchScore: click.searchScore,
    rankPosition: click.rankPosition,
    highlights: click.highlights
  }));
  downloadText(
    JSON.stringify(payload, null, 2),
    `product-click-logs-${new Date().toISOString().slice(0, 10)}.json`,
    "application/json"
  );
}

function exportClickLogsCsv(clicks: ProductDiscoveryClickLogResponse[]) {
  const headers = [
    "id",
    "clickedAt",
    "memberId",
    "surface",
    "query",
    "productId",
    "productName",
    "recommendationSource",
    "recommendationBucket",
    "searchScore",
    "rankPosition",
    "highlights"
  ];
  const rows = clicks.map((click) =>
    [
      click.id,
      click.clickedAt,
      click.memberId ?? "",
      click.surface,
      click.query,
      click.productId,
      click.productName,
      click.recommendationSource ?? "",
      click.recommendationBucket ?? "",
      click.searchScore ?? "",
      click.rankPosition ?? "",
      click.highlights ?? ""
    ].map(toCsvCell)
  );
  downloadText(
    [headers.map(toCsvCell), ...rows].map((row) => row.join(",")).join("\n"),
    `product-click-logs-${new Date().toISOString().slice(0, 10)}.csv`,
    "text/csv;charset=utf-8"
  );
}

function downloadText(content: string, filename: string, type: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

function toCsvCell(value: string | number) {
  const normalized = String(value);
  return `"${normalized.replaceAll('"', '""')}"`;
}

function summarize(logs: AiInteractionLogResponse[]) {
  const reviewedLogs = logs.filter((log) => log.qualityScore !== null);
  const average =
    reviewedLogs.length === 0
      ? "-"
      : (
          reviewedLogs.reduce((sum, log) => sum + (log.qualityScore ?? 0), 0) / reviewedLogs.length
        ).toFixed(1);

  return {
    total: logs.length,
    llmUsed: logs.filter((log) => log.llmUsed).length,
    elasticsearch: logs.filter((log) => log.recommendationSource === "ELASTICSEARCH").length,
    reviewed: reviewedLogs.length,
    averageScore: average
  };
}

function summarizeClicks(clicks: ProductDiscoveryClickLogResponse[]) {
  const rankedClicks = clicks.filter((click) => click.rankPosition !== null);
  const averageRank =
    rankedClicks.length === 0
      ? "-"
      : (
          rankedClicks.reduce((sum, click) => sum + (click.rankPosition ?? 0), 0) / rankedClicks.length
        ).toFixed(1);

  return {
    total: clicks.length,
    search: clicks.filter((click) => click.surface === "SEARCH").length,
    ai: clicks.filter((click) => click.surface === "AI_RECOMMENDATION").length,
    elasticsearch: clicks.filter((click) => click.recommendationSource === "ELASTICSEARCH").length,
    averageRank
  };
}

function summarizeFunnel(summary: ProductDiscoveryFunnelSummaryResponse | undefined) {
  const search = summary?.surfaces.find((segment) => segment.key === "SEARCH");
  const ai = summary?.surfaces.find((segment) => segment.key === "AI_RECOMMENDATION");

  return {
    impressions: summary?.impressions ?? 0,
    clicks: summary?.clicks ?? 0,
    ctr: formatPercent(summary?.ctr ?? 0),
    searchCtr: formatPercent(search?.ctr ?? 0),
    aiCtr: formatPercent(ai?.ctr ?? 0)
  };
}

function summarizeBucketComparisons(summary: ProductDiscoveryFunnelSummaryResponse | undefined) {
  const buckets = summary?.buckets.filter((bucket) => bucket.key !== "UNASSIGNED") ?? [];
  const activeBuckets = buckets.filter((bucket) => bucket.impressions > 0 || bucket.clicks > 0);
  const control = buckets.find((bucket) => bucket.key === "CONTROL");
  const controlCtr = control?.impressions ? control.ctr : null;

  return activeBuckets.map((bucket) => {
    const lift = controlCtr !== null && controlCtr > 0 ? (bucket.ctr - controlCtr) / controlCtr : null;
    return {
      ...bucket,
      liftLabel: bucket.key === "CONTROL" ? "baseline" : formatLift(lift),
      liftClass: liftClass(lift)
    };
  });
}

function formatLift(value: number | null) {
  if (value === null) {
    return "-";
  }
  const sign = value > 0 ? "+" : "";
  return `${sign}${(value * 100).toFixed(1)}%`;
}

function liftClass(value: number | null) {
  if (value === null || value === 0) {
    return "is-neutral";
  }
  return value > 0 ? "is-positive" : "is-negative";
}

function formatPercent(value: number) {
  return `${(value * 100).toFixed(1)}%`;
}

function trendBarHeight(value: number, maxValue: number) {
  if (maxValue <= 0) {
    return 4;
  }
  return Math.max(6, Math.round((value / maxValue) * 100));
}

function formatShortDate(value: string) {
  const [, month, day] = value.split("-");
  return month && day ? `${month}/${day}` : value;
}

function buildDateRange(range: RangeFilter): { from?: string; to?: string } {
  if (range === "ALL") {
    return {};
  }

  const days = range === "7D" ? 7 : 30;
  const from = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
  return { from: toLocalDateTime(from) };
}

function toLocalDateTime(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60 * 1000);
  return localDate.toISOString().slice(0, 19);
}
