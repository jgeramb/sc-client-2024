package de.teamgruen.sc.player;

import de.teamgruen.sc.player.handlers.RandomGameHandler;
import de.teamgruen.sc.player.handlers.SimpleGameHandler;
import de.teamgruen.sc.sdk.SoftwareChallengeClient;
import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpCloseException;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;

public class SoftwareChallengePlayer {

    private static final Logger LOGGER = new Logger();

    public static void main(String[] args) {
        if(args.length < 3) {
            LOGGER.error("Usage: [-d; --debug] <host> <port> <play-style: random | simple>");
            return;
        }

        final String host = args[args.length - 3];
        final int port;

        try {
            port = Integer.parseInt(args[args.length - 2]);
        } catch (NumberFormatException e) {
            LOGGER.error("Port must be a number");
            return;
        }

        LOGGER.setDebug(args[0].equals("-d") || args[0].equals("--debug"));

        final String playStyle = args[args.length - 1];
        final GameHandler gameHandler = switch (playStyle) {
            case "random" -> new RandomGameHandler(LOGGER);
            case "simple" -> new SimpleGameHandler(LOGGER);
            default -> {
                LOGGER.error("Unknown play-style: " + playStyle);
                yield null;
            }
        };

        if(gameHandler == null)
            return;

        final SoftwareChallengeClient client = new SoftwareChallengeClient(host, port, gameHandler);

        try {
            client.start();

            // join any game
            client.joinGame();
        } catch (TcpConnectException ex) {
            LOGGER.error("Could not connect to server: " + ex.getMessage());
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                client.stop();
            } catch (TcpCloseException ex) {
                LOGGER.error("Error while disconnecting from server: " + ex.getMessage());
            }
        }));
    }

}
