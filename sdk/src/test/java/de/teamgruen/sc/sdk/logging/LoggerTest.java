/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.logging;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerTest {

    @Test
    public void testInfo() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        new Logger(out).info("Test");

        final String actualOutput = out.toString().strip();

        assertTrue(actualOutput.contains("INFO") && actualOutput.endsWith("Test"));
    }

    @Test
    public void testWarn() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        new Logger(out).warn("Test");

        final String actualOutput = out.toString().strip();

        assertTrue(actualOutput.contains("WARN") && actualOutput.endsWith("Test"));
    }

    @Test
    public void testError() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        new Logger(out).error("Test");

        final String actualOutput = out.toString().strip();

        assertTrue(actualOutput.contains("ERROR") && actualOutput.endsWith("Test"));
    }

    @Test
    public void testDebug_NoDebugEnabled() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        new Logger(out).debug("Test");

        final String actualOutput = out.toString().strip();

        assertFalse(actualOutput.endsWith("Test"));
    }

    @Test
    public void testDebug() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Logger logger = new Logger(out);
        logger.setDebug(true);
        logger.debug("Test");

        final String actualOutput = out.toString().strip();

        assertTrue(actualOutput.contains("DEBUG") && actualOutput.endsWith("Test"));
    }

    @Test
    public void testLog_Replace() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        new Logger(out).log(Level.INFO, "Test", true);

        final String actualOutput = out.toString();

        assertTrue(actualOutput.startsWith("\r") && actualOutput.endsWith("Test"));
    }

    @Test
    public void testClosedOutputStream() {
        final OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException();
            }
        };
        final Logger logger = new Logger(out);

        assertThrows(RuntimeException.class, () -> logger.info("Test"));
    }

    @Test
    public void testLog_NoDebugEnabled() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertFalse(new Logger(out).log(Level.DEBUG, "Test", false));
    }

}
