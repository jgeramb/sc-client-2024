/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.logging;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static de.teamgruen.sc.sdk.logging.AnsiColor.*;

@RequiredArgsConstructor
public class Logger {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final String
            OPEN_BRACKET = BLACK + "[" + RESET,
            CLOSE_BRACKET = BLACK + "]" + RESET,
            COLON = BLACK + ":" + RESET;
    
    private final OutputStream out;
    @Setter
    private boolean debug;

    public void info(String message) {
        this.log(Level.INFO, message);
    }

    public void warn(String message) {
        this.log(Level.WARN, message);
    }

    public void error(String message) {
        this.log(Level.ERROR, message);
    }

    public void debug(String message) {
        if(this.debug)
            this.log(Level.DEBUG, message);
    }

    public void log(@NonNull Level logLevel, String message) {
        final String time = DATE_FORMAT.format(new Date());
        final String levelName = logLevel.name();
        final String level = logLevel.getColor() + levelName + RESET + " ".repeat(5 - levelName.length());
        final String threadName = CYAN + Thread.currentThread().getName() + RESET;

        this.print(OPEN_BRACKET + time + " " + level + CLOSE_BRACKET + " " + threadName + COLON + " " + message);
    }

    private void print(String message) {
        try {
            this.out.write(message.getBytes());
            this.out.write('\n');
            this.out.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Could not write to output stream", ex);
        }
    }

}
