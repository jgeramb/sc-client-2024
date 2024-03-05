/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.logging;

public enum AnsiColor {

    RESET,
    BLACK,
    RED,
    GREEN,
    YELLOW,
    BLUE,
    PURPLE,
    CYAN,
    WHITE;

    private final String code;

    AnsiColor() {
        final int ordinal = ordinal();
        this.code = "\033[" + (ordinal == 0 ? "0" : (29 + ordinal)) + "m";
    }

    @Override
    public String toString() {
        return this.code;
    }

}
