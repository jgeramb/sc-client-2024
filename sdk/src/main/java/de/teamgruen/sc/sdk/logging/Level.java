/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.logging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Level {

    INFO(AnsiColor.GREEN),
    WARN(AnsiColor.YELLOW),
    ERROR(AnsiColor.RED),
    DEBUG(AnsiColor.BLUE);

    private final AnsiColor color;

}
