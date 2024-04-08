package de.hilling.maven.release;

import org.junit.Test;

import de.hilling.maven.release.utils.Guard;

public class GuardTest {

    public static final String TEST_NAME = "test";

    @Test
    public void notNullOk() {
        Guard.notNull(TEST_NAME, new Object());
    }

    @Test(expected = Guard.GuardException.class)
    public void notNullNotOk() {
        Guard.notNull(TEST_NAME, null);
    }

    @Test
    public void notBlankOk() {
        Guard.notBlank(TEST_NAME, "h");
    }

    @Test(expected = Guard.GuardException.class)
    public void notBlankNotOkBlank() {
        Guard.notBlank(TEST_NAME, "");
    }

    @Test(expected = Guard.GuardException.class)
    public void notBlankNotOkNull() {
        Guard.notBlank(TEST_NAME, null);
    }


}