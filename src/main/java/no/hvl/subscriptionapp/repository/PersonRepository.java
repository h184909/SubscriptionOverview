package no.hvl.subscriptionapp.repository;

import no.hvl.subscriptionapp.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, String> {
    Optional<Person> findByEmailVerifyTokenHash(String emailVerifyTokenHash);
}