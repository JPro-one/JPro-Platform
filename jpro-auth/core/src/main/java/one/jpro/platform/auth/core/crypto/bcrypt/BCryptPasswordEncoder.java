// Copyright 2002-2011 the original author or authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package one.jpro.platform.auth.core.crypto.bcrypt;

import one.jpro.platform.auth.core.crypto.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of PasswordEncoder that uses the BCrypt strong hashing function. Clients
 * can optionally supply a "version" ($2a, $2b, $2y) and a "strength" (a.k.a. log rounds
 * in BCrypt) and a SecureRandom instance. The larger the strength parameter the more work
 * will have to be done (exponentially) to hash the passwords. The default value is 10.
 *
 * @author Dave Syer
 * @author Besmir Beqiri
 * @see <a href="https://github.com/spring-projects/spring-security/blob/main/crypto/src/main/java/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.java">Spring Security BCryptPasswordEncoder implementation</a>
 */
public class BCryptPasswordEncoder implements PasswordEncoder {

    private static final Logger logger = LoggerFactory.getLogger(BCryptPasswordEncoder.class);

    private final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2([ayb])?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");

    private final int strength;

    private final BCrypt.Version version;

    private final SecureRandom random;

    /**
     * Default constructor which uses a default strength.
     */
    public BCryptPasswordEncoder() {
        this(-1);
    }

    /**
     * Constructor with strength. It sets the log rounds for hashing.
     *
     * @param strength the log rounds to use, between 4 and 31
     */
    public BCryptPasswordEncoder(int strength) {
        this(strength, null);
    }

    /**
     * Constructor with a specific version of BCrypt.
     *
     * @param version the version of bcrypt, can be 2a, 2b, 2y
     */
    public BCryptPasswordEncoder(BCrypt.Version version) {
        this(version, null);
    }

    /**
     * Constructor with a specific version of BCrypt and a SecureRandom instance.
     *
     * @param version the version of bcrypt, can be 2a, 2b, 2y
     * @param random  the secure random instance to use
     */
    public BCryptPasswordEncoder(BCrypt.Version version, SecureRandom random) {
        this(version, -1, random);
    }

    /**
     * Constructor with strength and a SecureRandom instance.
     *
     * @param strength the log rounds to use, between 4 and 31
     * @param random   the secure random instance to use
     */
    public BCryptPasswordEncoder(int strength, SecureRandom random) {
        this(BCrypt.Version.$2A, strength, random);
    }

    /**
     * Constructor with a specific version of BCrypt and strength.
     *
     * @param version  the version of bcrypt, can be 2a, 2b, 2y
     * @param strength the log rounds to use, between 4 and 31
     */
    public BCryptPasswordEncoder(BCrypt.Version version, int strength) {
        this(version, strength, null);
    }

    /**
     * Constructor with a specific version of BCrypt, strength, and a SecureRandom instance.
     *
     * @param version  the version of bcrypt, can be 2a, 2b, 2y
     * @param strength the log rounds to use, between 4 and 31
     * @param random   the secure random instance to use
     */
    public BCryptPasswordEncoder(BCrypt.Version version, int strength, SecureRandom random) {
        if (strength != -1 && (strength < BCrypt.MIN_LOG_ROUNDS || strength > BCrypt.MAX_LOG_ROUNDS)) {
            throw new IllegalArgumentException("Bad strength");
        }
        this.version = version;
        this.strength = (strength == -1) ? 10 : strength;
        this.random = random;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        String salt = getSalt();
        return BCrypt.hashpw(rawPassword.toString(), salt);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            logger.warn("Empty encoded password");
            return false;
        }
        if (!BCRYPT_PATTERN.matcher(encodedPassword).matches()) {
            logger.warn("Encoded password does not look like BCrypt");
            return false;
        }
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            logger.warn("Empty encoded password");
            return false;
        }
        Matcher matcher = BCRYPT_PATTERN.matcher(encodedPassword);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Encoded password does not look like BCrypt: " + encodedPassword);
        }
        int strength = Integer.parseInt(matcher.group(2));
        return strength < this.strength;
    }

    /**
     * Generates a salt for use with this {@link BCryptPasswordEncoder}.
     *
     * @return a BCrypt salt
     */
    private String getSalt() {
        if (random != null) {
            return BCrypt.gensalt(version.getValue(), strength, random);
        }
        return BCrypt.gensalt(version.getValue(), strength);
    }
}
