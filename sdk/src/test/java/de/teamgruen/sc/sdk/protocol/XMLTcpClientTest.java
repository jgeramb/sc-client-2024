package de.teamgruen.sc.sdk.protocol;

import de.teamgruen.sc.sdk.protocol.data.Move;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XMLTcpClientTest {

    private static final String HOST = "localhost";

    @Test
    public void testConnect() {
        openConnection(
                20_000,
                error -> {},
                Collections.emptyList(),
                socket -> {},
                (client, packet) -> {},
                (socket, xml) -> {}
        );
    }

    @Test
    public void testConnect_Closed() {
        final XMLTcpClient client = new XMLTcpClient(HOST, 20_001);

        assertThrows(TcpConnectException.class, () -> client.connect(packet -> {}, null));
    }

    @Test
    public void testReadAndWrite() {
        final AtomicBoolean packetReceived = new AtomicBoolean(false),
                protocolInitiated = new AtomicBoolean(false);

        openConnection(
                20_002,
                error -> {},
                List.of(new JoinGameRequest("test")),
                socket -> {},
                (client, packet) -> {
                    if (packet instanceof JoinedRoomResponse) {
                        packetReceived.set(true);

                        client.disconnect();
                    }
                },
                (socket, xml) -> {
                    try {
                        if (xml.equals("<protocol>")) {
                            protocolInitiated.set(true);

                            socket.getOutputStream().write(("<buffer>" + "x".repeat(512) + "</buffer>").getBytes());
                            socket.getOutputStream().flush();
                        } else if (protocolInitiated.get() && xml.equals("<join gameType=\"test\"/>")) {
                            socket.getOutputStream().write("<protocol><joined roomId=\"test\"/>".getBytes());
                            socket.getOutputStream().flush();
                        }
                    } catch (IOException ignore) {
                    }
                }
        );

        assertTrue(packetReceived.get());
    }

    @Test
    public void testWrite_SerializationException() {
        final AtomicBoolean errorReceived = new AtomicBoolean(false);

        openConnection(
                20_003,
                error -> errorReceived.set(true),
                List.of(new MovePacket("test", new Move(null))),
                socket -> {},
                (client, packet) -> {},
                (socket, xml) -> {}
        );

        assertTrue(errorReceived.get());
    }

    @Test
    public void testRead_DeserializationException() {
        final AtomicBoolean errorReceived = new AtomicBoolean(false);

        openConnection(
                20_004,
                error -> errorReceived.set(true),
                List.of(new JoinGameRequest(null)),
                socket -> {
                    try {
                        socket.getOutputStream().write("<protocol><invalid/>".getBytes());
                        socket.getOutputStream().flush();
                    } catch (IOException ignore) {
                    }
                },
                (client, packet) -> {},
                (socket, xml) -> {}
        );

        assertTrue(errorReceived.get());
    }

    private static void openConnection(int port,
                                       Consumer<String> errorHandler,
                                       List<XMLProtocolPacket> packetsToSend,
                                       Consumer<Socket> clientConnectHook,
                                       BiConsumer<XMLTcpClient, XMLProtocolPacket> packetHandler,
                                       BiConsumer<Socket, String> serverXmlHandler) {
        final Object readyLock = new Object(), connectLock = new Object(), readLock = new Object();

        new Thread(() -> {
            try(ServerSocket serverSocket = new ServerSocket(port)) {
                synchronized (readyLock) {
                    readyLock.notify();
                }

                final Socket socket = serverSocket.accept();

                synchronized (connectLock) {
                    connectLock.wait();
                }

                clientConnectHook.accept(socket);

                final InputStream inputStream = socket.getInputStream();
                final byte[] buffer = new byte[512];
                int nRead;

                while((nRead = inputStream.read(buffer)) > 0) {
                    StringBuilder builder = new StringBuilder(new String(buffer, 0, nRead, StandardCharsets.UTF_8));

                    if (nRead == buffer.length) {
                        while (inputStream.available() > 0) {
                            nRead = inputStream.read(buffer);

                            builder.append(new String(buffer, 0, nRead, StandardCharsets.UTF_8));
                        }
                    }

                    serverXmlHandler.accept(socket, builder.toString());
                }

                synchronized (readLock) {
                    readLock.notify();
                }
            } catch (IOException | InterruptedException ignore) {
            }
        }).start();

        try {
            synchronized (readyLock) {
                readyLock.wait();
            }

            final XMLTcpClient client = new XMLTcpClient(HOST, port);
            client.connect(packet -> packetHandler.accept(client, packet), (message) -> {
                if(errorHandler != null)
                    errorHandler.accept(message);

                client.disconnect();
            });

            synchronized (connectLock) {
                connectLock.notify();
            }

            if(packetsToSend.isEmpty())
                return;

            client.send(packetsToSend.toArray(new XMLProtocolPacket[0]));

            synchronized (readLock) {
                readLock.wait();
            }
        } catch (TcpConnectException | InterruptedException ignore) {
        }
    }

}
