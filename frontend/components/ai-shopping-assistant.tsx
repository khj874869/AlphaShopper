"use client";

import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { askShoppingAi, recordProductDiscoveryClick, recordProductDiscoveryImpressions } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import { ProductVisual } from "@/components/product-visual";
import { useSessionStore } from "@/store/session-store";
import type { AiChatResponse } from "@/lib/types";

const quickPrompts = [
  "\uCD9C\uADFC\uB8E9 \uCD94\uCC9C\uD574\uC918",
  "5\uB9CC\uC6D0\uB300 \uB370\uC77C\uB9AC \uC0C1\uD488",
  "\uB370\uB2D8\uACFC \uC5B4\uC6B8\uB9AC\uB294 \uCF54\uB514",
  "\uB370\uC774\uD2B8\uB8E9 \uD3EC\uC778\uD2B8 \uC0C1\uD488"
];

export function AiShoppingAssistant() {
  const member = useSessionStore((state) => state.member);
  const [message, setMessage] = useState("\uCD9C\uADFC\uB8E9\uC5D0 \uC5B4\uC6B8\uB9AC\uB294 \uC0C1\uD488 \uCD94\uCC9C\uD574\uC918");
  const [lastPrompt, setLastPrompt] = useState("");
  const [answer, setAnswer] = useState<AiChatResponse | null>(null);
  const recordedImpressionsRef = useRef(new Set<string>());

  const chatMutation = useMutation({
    mutationFn: askShoppingAi,
    onSuccess: (response) => setAnswer(response)
  });

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = message.trim();
    if (!trimmed) {
      return;
    }
    setLastPrompt(trimmed);
    chatMutation.mutate({ message: trimmed, memberId: member?.id, maxRecommendations: 4 });
  };

  const recordRecommendationClick = (recommendation: AiChatResponse["recommendations"][number], index: number) => {
    if (!answer) {
      return;
    }
    void recordProductDiscoveryClick({
      memberId: member?.id,
      surface: "AI_RECOMMENDATION",
      query: lastPrompt || message,
      productId: recommendation.product.id,
      productName: recommendation.product.name,
      recommendationSource: answer.recommendationSource,
      recommendationBucket: answer.recommendationBucket,
      searchScore: recommendation.searchScore,
      rankPosition: index + 1,
      highlights: recommendation.highlights
    }).catch(() => undefined);
  };

  useEffect(() => {
    if (!answer?.recommendations.length) {
      return;
    }

    const prompt = lastPrompt || message;
    const productIds = answer.recommendations.map((recommendation) => recommendation.product.id).join(",");
    const impressionKey = `ai:${prompt}:${answer.recommendationSource}:${answer.recommendationBucket}:${productIds}`;
    if (recordedImpressionsRef.current.has(impressionKey)) {
      return;
    }
    recordedImpressionsRef.current.add(impressionKey);

    void recordProductDiscoveryImpressions({
      impressions: answer.recommendations.map((recommendation, index) => ({
        memberId: member?.id,
        surface: "AI_RECOMMENDATION",
        query: prompt,
        productId: recommendation.product.id,
        productName: recommendation.product.name,
        recommendationSource: answer.recommendationSource,
        recommendationBucket: answer.recommendationBucket,
        searchScore: recommendation.searchScore,
        rankPosition: index + 1,
        highlights: recommendation.highlights,
        impressionKey
      }))
    }).catch(() => undefined);
  }, [answer, lastPrompt, member?.id, message]);

  return (
    <section className="panel ai-assistant">
      <div className="ai-assistant__copy">
        <p className="eyebrow">Internal shopping AI</p>
        <h2>Ask for a look, budget, or shopping mood.</h2>
      </div>

      <form className="ai-assistant__composer" onSubmit={submit}>
        <input
          aria-label="Shopping AI message"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          placeholder="\uC608: 7\uB9CC\uC6D0 \uC774\uD558 \uCD9C\uADFC\uB8E9 \uCD94\uCC9C\uD574\uC918"
        />
        <button className="button button--dark" disabled={chatMutation.isPending} type="submit">
          {chatMutation.isPending ? "Thinking" : "Ask AI"}
        </button>
      </form>

      <div className="chip-row">
        {quickPrompts.map((prompt) => (
          <button key={prompt} className="chip chip--light" onClick={() => setMessage(prompt)} type="button">
            {prompt}
          </button>
        ))}
      </div>

      {chatMutation.isError ? <p className="form-error">{chatMutation.error.message}</p> : null}

      {answer ? (
        <div className="ai-assistant__result">
          <div className="ai-assistant__reply">
            <span>
              {answer.llmUsed ? "LLM" : "Catalog fallback"} / {answer.recommendationSource} /{" "}
              {bucketLabel(answer.recommendationBucket)}
            </span>
            <p>{answer.reply}</p>
            {member ? <small>Personalized for {member.name}</small> : null}
          </div>
          <div className="ai-recommendation-grid">
            {answer.recommendations.map((recommendation, index) => (
              <article className="ai-product" key={recommendation.product.id}>
                <ProductVisual
                  brand={recommendation.product.brand}
                  className="ai-product__visual"
                  imageUrl={recommendation.product.imageUrl}
                />
                <div className="ai-product__body">
                  <p>{recommendation.product.brand}</p>
                  <h3>{recommendation.product.name}</h3>
                  <strong>{formatCurrency(recommendation.product.price)}</strong>
                  {recommendation.searchScore ? (
                    <small className="search-score">ES score {recommendation.searchScore.toFixed(2)}</small>
                  ) : null}
                  <small>{recommendation.reason}</small>
                  {recommendation.highlights.length ? (
                    <div className="search-highlights">
                      {recommendation.highlights.slice(0, 2).map((highlight) => (
                        <p key={highlight}>{renderHighlight(highlight)}</p>
                      ))}
                    </div>
                  ) : null}
                </div>
                <Link
                  className="button button--ghostDark"
                  href={`/products/${recommendation.product.id}`}
                  onClick={() => recordRecommendationClick(recommendation, index)}
                >
                  View
                </Link>
              </article>
            ))}
          </div>
        </div>
      ) : null}
    </section>
  );
}

function renderHighlight(value: string) {
  return value.split(/(\[\[.*?\]\])/g).map((part, index) => {
    if (part.startsWith("[[") && part.endsWith("]]")) {
      return <mark key={`${part}-${index}`}>{part.slice(2, -2)}</mark>;
    }
    return <span key={`${part}-${index}`}>{part}</span>;
  });
}

function bucketLabel(bucket: AiChatResponse["recommendationBucket"]) {
  if (bucket === "CTR_RANKING") {
    return "CTR";
  }
  if (bucket === "CONTROL") {
    return "Control";
  }
  return "Default";
}
