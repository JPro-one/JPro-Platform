package one.jpro.auth.test;

import one.jpro.auth.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HttpStatus tests.
 *
 * @author Besmir Beqiri
 */
public class HttpStatusTest {

    @Test
    public void fetchingHttpStatusByCodeShouldGetTheRightOne() {
        for (HttpStatus httpStatus : HttpStatus.values()) {
            assertEquals(HttpStatus.fromCode(httpStatus.getCode()), httpStatus);
        }
    }

    @Test
    public void fetchingHttpStatusByCodeOutsideOfRangeShouldNotThrowErrors() {
        assertEquals(HttpStatus.fromCode(-1), HttpStatus.UNKNOWN);
        assertEquals(HttpStatus.fromCode(542345), HttpStatus.UNKNOWN);
    }

    @Test
    public void httpStatusProvidesFormattedImplementationOfToStringMethod() {
        for (HttpStatus httpStatus : HttpStatus.values()) {
            assertEquals(httpStatus.getCode() + " " + httpStatus.getMessage(), httpStatus.toString());
        }
    }
}
