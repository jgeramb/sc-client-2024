package de.teamgruen.sc.sdk.logging;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@RequiredArgsConstructor
public class Logger {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final String
            OPEN_BRACKET = AnsiColor.BLACK + "[" + AnsiColor.RESET,
            CLOSE_BRACKET = AnsiColor.BLACK + "]" + AnsiColor.RESET,
            COLON = AnsiColor.BLACK + ":" + AnsiColor.RESET;
    
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

    public void log(Level logLevel, String message) {
        final String time = DATE_FORMAT.format(new Date());
        final String level = logLevel.getColor() + logLevel.name() + AnsiColor.RESET;
        final String threadName = AnsiColor.CYAN + Thread.currentThread().getName() + AnsiColor.RESET;

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
