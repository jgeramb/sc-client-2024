/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.logging;

import de.teamgruen.sc.sdk.SoftwareChallengeClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnsiColorTest {

    @Test
    public void testToString() {
        SoftwareChallengeClient.setBatchMode(false);

        assertEquals("\033[0m", AnsiColor.RESET.toString());
        assertEquals("\033[30m", AnsiColor.BLACK.toString());
        assertEquals("\033[31m", AnsiColor.RED.toString());
        assertEquals("\033[32m", AnsiColor.GREEN.toString());
        assertEquals("\033[33m", AnsiColor.YELLOW.toString());
        assertEquals("\033[34m", AnsiColor.BLUE.toString());
        assertEquals("\033[35m", AnsiColor.PURPLE.toString());
        assertEquals("\033[36m", AnsiColor.CYAN.toString());
        assertEquals("\033[37m", AnsiColor.WHITE.toString());
    }

}
