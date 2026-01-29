package no.hvl.subscriptionapp.service;

import no.hvl.subscriptionapp.domain.Person;
import no.hvl.subscriptionapp.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final PersonRepository personRepository;
    private final PasswordService passwordService;

    public AuthService(PersonRepository personRepository, PasswordService passwordService) {
        this.personRepository = personRepository;
        this.passwordService = passwordService;
    }

    public Optional<Person> authenticate(String email, String rawPassword) {
        return personRepository.findById(email)
                .filter(p -> passwordService.verify(rawPassword, p.getSalt(), p.getHash()));
    }
}