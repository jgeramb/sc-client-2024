package de.teamgruen.sc.sdk.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoggerTest {

    @Test
    public void testInfo() {
        new Logger().info("Test");
    }

    @Test
    public void testWarn() {
        new Logger().warn("Test");
    }

    @Test
    public void testError() {
        new Logger().error("Test");
    }

    @Test
    public void testDebug() {
        final Logger logger = new Logger();
        logger.debug("Test");
        logger.setDebug(true);
        logger.debug("Debug Test");
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

}
