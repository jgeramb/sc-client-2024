/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.player;

import de.teamgruen.sc.player.clients.AdminClient;
import de.teamgruen.sc.player.clients.PlayerClient;
import de.teamgruen.sc.player.handlers.AdvancedGameHandler;
import de.teamgruen.sc.player.handlers.SimpleGameHandler;
import de.teamgruen.sc.sdk.SoftwareChallengeClient;
import de.teamgruen.sc.sdk.game.handlers.GameHandler;
import de.teamgruen.sc.sdk.logging.AnsiColor;
import de.teamgruen.sc.sdk.logging.Logger;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import jargs.gnu.CmdLineParser;

public class SoftwareChallengePlayer {

    private static final Logger LOGGER = new Logger(System.out);

    public static void main(String[] args) {
        final CmdLineParser parser = new CmdLineParser();
        final CmdLineParser.Option debugOption = parser.addBooleanOption('d', "debug");
        final CmdLineParser.Option batchModeOption = parser.addBooleanOption('b', "batch-mode");
        final CmdLineParser.Option hostOption = parser.addStringOption('h', "host");
        final CmdLineParser.Option portOption = parser.addIntegerOption('p', "port");

        final CmdLineParser.Option playStyleOption = parser.addStringOption('s', "play-style");
        final CmdLineParser.Option gameTypeOption = parser.addStringOption('g', "game-type");
        final CmdLineParser.Option reservationOption = parser.addStringOption('r', "reservation");
        final CmdLineParser.Option roomOption = parser.addStringOption('R', "room");

        final CmdLineParser.Option testsOption = parser.addIntegerOption('t', "tests");
        final CmdLineParser.Option passwordOption = parser.addStringOption('P', "password");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException ex) {
            LOGGER.error(ex.getMessage());
            return;
        }

        SoftwareChallengeClient.setBatchMode((Boolean) parser.getOptionValue(batchModeOption, false));
        LOGGER.setDebug((Boolean) parser.getOptionValue(debugOption, false));

        final String host = (String) parser.getOptionValue(hostOption, "localhost");
        final int port = (Integer) parser.getOptionValue(portOption, 13050);
        final int tests = (int) parser.getOptionValue(testsOption, 0);
        final String playStyle = (String) parser.getOptionValue(playStyleOption, null);

        try {
            if(tests > 0) {
                final AdminClient adminClient = new AdminClient(LOGGER, host, port, playStyle);
                adminClient.connect();

                final String password = (String) parser.getOptionValue(passwordOption, "admin");

                adminClient.runTests(password, tests);
            } else {
                final GameHandler gameHandler = switch (playStyle == null ? "simple" : playStyle) {
                    case "simple" -> {
                        LOGGER.info("Play-Style: " + AnsiColor.PURPLE + "Simple" + AnsiColor.RESET);
                        yield new SimpleGameHandler(LOGGER);
                    }
                    case "advanced" -> {
                        LOGGER.info("Play-Style: " + AnsiColor.PURPLE + "Advanced" + AnsiColor.RESET);
                        yield new AdvancedGameHandler(LOGGER);
                    }
                    default -> throw new IllegalArgumentException("Illegal play style: " + playStyle);
                };

                final PlayerClient client = new PlayerClient(host, port, gameHandler);
                client.connect();

                final String gameType = (String) parser.getOptionValue(gameTypeOption, null);
                final String reservation = (String) parser.getOptionValue(reservationOption, null);
                final String room = (String) parser.getOptionValue(roomOption, null);

                if (gameType != null)
                    client.joinGame(gameType);
                else if (reservation != null)
                    client.joinPreparedRoom(reservation);
                else if (room != null)
                    client.joinRoom(room);
                else
                    client.joinAnyGame();
            }
        } catch (TcpConnectException ex) {
            LOGGER.error("Could not connect to server: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

}
