package de.teamgruen.sc.sdk.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LevelTest {

    @Test
    public void testGetColor() {
        assertEquals(AnsiColor.GREEN, Level.INFO.getColor());
        assertEquals(AnsiColor.YELLOW, Level.WARN.getColor());
        assertEquals(AnsiColor.RED, Level.ERROR.getColor());
        assertEquals(AnsiColor.BLUE, Level.DEBUG.getColor());
    }

}
