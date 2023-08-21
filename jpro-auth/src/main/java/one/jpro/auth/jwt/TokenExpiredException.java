package one.jpro.auth.jwt;

import one.jpro.auth.authentication.AuthenticationException;

import java.time.Instant;

/**
 * Token expired exception.
 *
 * @author Besmir Beqiri
 */
public class TokenExpiredException extends AuthenticationException {

    private final Instant expiredAt;

    public TokenExpiredException(String message, Instant expiredAt) {
        super(message);
        this.expiredAt = expiredAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
