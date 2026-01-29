package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, String> {
}