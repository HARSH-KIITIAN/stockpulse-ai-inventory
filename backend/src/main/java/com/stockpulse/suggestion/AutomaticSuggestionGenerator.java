package com.stockpulse.suggestion;

import java.util.Set;
import java.util.UUID;

/** Application boundary used by committed inventory signal handling. */
public interface AutomaticSuggestionGenerator {

    void createAutomaticSuggestions(UUID productId, Set<TriggerReason> triggerReasons);
}