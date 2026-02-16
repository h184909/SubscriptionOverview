package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.SuggestionDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SuggestionDecisionRepository extends JpaRepository<SuggestionDecision, UUID> {

    List<SuggestionDecision> findByUserEmail(String userEmail);

    Optional<SuggestionDecision> findByUserEmailAndSuggestionKey(String userEmail, String suggestionKey);

    List<SuggestionDecision> findByUserEmailAndSuggestionKeyIn(String userEmail, Set<String> keys);
    void deleteByUserEmail(String userEmail);
}
