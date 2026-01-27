package org.project.infrastructure.security;

import java.util.Objects;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import org.springframework.stereotype.Component;

@Component
public class PasswordEncoder {

    private final Argon2 argon2;

    public PasswordEncoder() {
        this.argon2 = Argon2Factory.create();
    }

    public String encode(String password) {
        Objects.requireNonNull(password);
        return argon2.hash(2, 65_536, 4, password.toCharArray());
    }

    public boolean verify(String password, String hashed) {
        Objects.requireNonNull(password);
        Objects.requireNonNull(hashed);
        return argon2.verify(hashed, password.toCharArray());
    }
}
