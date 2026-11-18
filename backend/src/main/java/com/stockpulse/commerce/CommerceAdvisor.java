package com.stockpulse.commerce;

/**
 * Contract used by both synchronous API callers and asynchronous signal handlers
 * to obtain pricing and replenishment recommendations.
 */
public interface CommerceAdvisor {

    CommerceRecommendation recommend(CommerceContext context);
}