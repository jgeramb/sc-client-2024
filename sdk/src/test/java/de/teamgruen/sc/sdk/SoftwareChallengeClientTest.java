package de.teamgruen.sc.sdk;

import de.teamgruen.sc.sdk.game.GameHandler;
import de.teamgruen.sc.sdk.protocol.TestSocket;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.XMLTcpClient;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinPreparedRoomRequest;
import de.teamgruen.sc.sdk.protocol.requests.JoinRoomRequest;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SoftwareChallengeClientTest {

    @Test
    public void testStart_NoGameHandler() {
        assertThrows(
                IllegalArgumentException.class,
                new SoftwareChallengeClient("", 0, null)::start
        );
    }

    @Test
    public void testStart_Closed() {
        assertThrows(
                TcpConnectException.class,
                new SoftwareChallengeClient("localhost", 13050, new GameHandler() {})::start
        );
    }

    @Test
    public void testStart_AlreadyStarted() throws IOException {
        final SoftwareChallengeClient client = new SoftwareChallengeClient("localhost", 13050, new GameHandler() {});
        client.getClient().setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(),
                xml -> {}
        ));
        client.start();

        assertThrows(IllegalStateException.class, client::start);
    }

    @Test
    public void testStart() throws IOException, InterruptedException {
        final Object lock = new Object();
        final AtomicBoolean roomJoined = new AtomicBoolean(false);

        final SoftwareChallengeClient client = new SoftwareChallengeClient("localhost", 13050, new GameHandler() {
            @Override
            public void onRoomJoin(String roomId) {
                assertEquals("test", roomId);

                roomJoined.set(true);

                synchronized (lock) {
                    lock.notify();
                }
            }
        });
        client.getClient().setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(List.of("<protocol>", "<joined roomId=\"test\"/>")),
                xml -> {}
        ));
        client.start();

        synchronized (lock) {
            lock.wait(250);
        }

        assertTrue(roomJoined.get());
    }

    @Test
    public void testStop_NotStarted() {
        assertThrows(
                IllegalStateException.class,
                new SoftwareChallengeClient("", 0, null)::stop
        );
    }

    @Test
    public void testStop_Error() throws IOException {
        final SoftwareChallengeClient client = new SoftwareChallengeClient("localhost", 13050, new GameHandler() {
            @Override
            public void onError(String message) {
            }
        });
        client.getClient().setSocket(new TestSocket(
                false,
                true,
                false,
                false,
                new LinkedList<>(),
                xml -> {}
        ));
        client.start();

        assertThrows(IOException.class, client::stop);
    }

    @Test
    public void testStop() throws IOException {
        final SoftwareChallengeClient client = new SoftwareChallengeClient("localhost", 13050, new GameHandler() {
            @Override
            public void onError(String message) {
            }
        });
        client.getClient().setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(),
                xml -> {}
        ));
        client.start();
        client.stop();
    }

    @Test
    public void testSendPacket() {
        final AtomicBoolean sent = new AtomicBoolean(false);
        final SoftwareChallengeClient client = new SoftwareChallengeClient("", 0, null);
        client.setClient(new XMLTcpClient("", 0) {
            @Override
            public void send(XMLProtocolPacket... packets) {
                assertTrue(packets.length > 0);
                assertInstanceOf(RoomPacket.class, packets[0]);

                sent.set(true);
            }
        });
        client.sendPacket(new RoomPacket());

        assertTrue(sent.get());
    }

    @Test
    public void testJoinAnyGame() {
        final AtomicBoolean sent = new AtomicBoolean(false);
        final SoftwareChallengeClient client = new SoftwareChallengeClient("", 0, null);
        client.setClient(new XMLTcpClient("", 0) {
            @Override
            public void send(XMLProtocolPacket... packets) {
                assertTrue(packets.length > 0);
                assertInstanceOf(JoinGameRequest.class, packets[0]);

                final JoinGameRequest request = (JoinGameRequest) packets[0];
                assertNull(request.gameType());

                sent.set(true);
            }
        });
        client.joinAnyGame();

        assertTrue(sent.get());
    }

    @Test
    public void testJoinGame() {
        final AtomicBoolean sent = new AtomicBoolean(false);
        final SoftwareChallengeClient client = new SoftwareChallengeClient("", 0, null);
        client.setClient(new XMLTcpClient("", 0) {
            @Override
            public void send(XMLProtocolPacket... packets) {
                assertTrue(packets.length > 0);
                assertInstanceOf(JoinGameRequest.class, packets[0]);

                final JoinGameRequest request = (JoinGameRequest) packets[0];
                assertEquals("test", request.gameType());

                sent.set(true);
            }
        });
        client.joinGame("test");

        assertTrue(sent.get());
    }

    @Test
    public void testJoinRoom() {
        final AtomicBoolean sent = new AtomicBoolean(false);
        final SoftwareChallengeClient client = new SoftwareChallengeClient("", 0, null);
        client.setClient(new XMLTcpClient("", 0) {
            @Override
            public void send(XMLProtocolPacket... packets) {
                assertTrue(packets.length > 0);
                assertInstanceOf(JoinRoomRequest.class, packets[0]);

                final JoinRoomRequest request = (JoinRoomRequest) packets[0];
                assertEquals("test", request.roomId());

                sent.set(true);
            }
        });
        client.joinRoom("test");

        assertTrue(sent.get());
    }

    @Test
    public void testJoinPreparedRoom() {
        final AtomicBoolean sent = new AtomicBoolean(false);
        final SoftwareChallengeClient client = new SoftwareChallengeClient("", 0, null);
        client.setClient(new XMLTcpClient("", 0) {
            @Override
            public void send(XMLProtocolPacket... packets) {
                assertTrue(packets.length > 0);
                assertInstanceOf(JoinPreparedRoomRequest.class, packets[0]);

                final JoinPreparedRoomRequest request = (JoinPreparedRoomRequest) packets[0];
                assertEquals("test", request.reservationCode());

                sent.set(true);
            }
        });
        client.joinPreparedRoom("test");

        assertTrue(sent.get());
    }

}
