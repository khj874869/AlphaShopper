package com.webjpa.shopping.dto;

public record AiRecommendationSettingsResponse(
        boolean clickSignalEnabled,
        int clickSignalWindowDays,
        int clickSignalBoostPerClick,
        int clickSignalMaxClickBoost,
        boolean ctrSignalEnabled,
        long ctrSignalMinImpressions,
        double ctrSignalHighThreshold,
        int ctrSignalHighBoost,
        double ctrSignalMidThreshold,
        int ctrSignalMidBoost,
        double ctrSignalLowThreshold,
        int ctrSignalLowPenalty,
        String ctrSignalLowAction,
        boolean experimentEnabled,
        int ctrTreatmentPercent
) {
}
