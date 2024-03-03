package de.teamgruen.sc.player;

import de.teamgruen.sc.player.handlers.AdvancedGameHandler;
import de.teamgruen.sc.player.handlers.SimpleGameHandler;
import de.teamgruen.sc.sdk.SoftwareChallengeClient;
import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import jargs.gnu.CmdLineParser;

import java.io.IOException;

public class SoftwareChallengePlayer {

    private static final Logger LOGGER = new Logger(System.out);

    public static void main(String[] args) {
        final CmdLineParser parser = new CmdLineParser();
        final CmdLineParser.Option debugOption = parser.addBooleanOption('d', "debug");
        final CmdLineParser.Option hostOption = parser.addStringOption('h', "host");
        final CmdLineParser.Option portOption = parser.addIntegerOption('p', "port");
        final CmdLineParser.Option reservationOption = parser.addStringOption('r', "reservation");
        final CmdLineParser.Option roomOption = parser.addStringOption('R', "room");
        final CmdLineParser.Option playStyleOption = parser.addStringOption('s', "play-style");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException ex) {
            LOGGER.error(ex.getMessage());
            System.exit(2);
        }

        LOGGER.setDebug((Boolean) parser.getOptionValue(debugOption, false));

        final String playStyle = (String) parser.getOptionValue(playStyleOption, "simple");
        final GameHandler gameHandler = switch (playStyle) {
            case "simple" -> {
                LOGGER.info("Play-Style: " + AnsiColor.PURPLE + "Simple" + AnsiColor.RESET);
                yield new SimpleGameHandler(LOGGER);
            }
            case "advanced" -> {
                LOGGER.info("Play-Style: " + AnsiColor.PURPLE + "Advanced" + AnsiColor.RESET);
                yield new AdvancedGameHandler(LOGGER);
            }
            default -> {
                LOGGER.error("Unknown play-style: " + playStyle);
                yield null;
            }
        };

        if(gameHandler == null)
            return;

        final String host = (String) parser.getOptionValue(hostOption, "localhost");
        final int port = (Integer) parser.getOptionValue(portOption, 13050);
        final SoftwareChallengeClient client = new SoftwareChallengeClient(host, port, gameHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                client.stop();
            } catch (IOException ignore) {
            }
        }));

        try {
            client.start();

            final String reservation = (String) parser.getOptionValue(reservationOption, null);
            final String room = (String) parser.getOptionValue(roomOption, null);

            if (reservation != null)
                client.joinPreparedRoom(reservation);
            else if(room != null)
                client.joinRoom(room);
            else
                client.joinAnyGame();
        } catch (TcpConnectException ex) {
            LOGGER.error("Could not connect to server: " + ex.getMessage());
            System.exit(1);
        }
    }

}
