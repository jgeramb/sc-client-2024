package de.teamgruen.sc.sdk.protocol;

import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XMLTcpClientTest {

    private static final String HOST = "localhost";

    @Test
    public void testConnect() throws IOException {
        final Object readyLock = new Object(), connectLock = new Object();

        new Thread(() -> {
            try(ServerSocket serverSocket = new ServerSocket(20_000)) {
                synchronized (readyLock) {
                    readyLock.notify();
                }

                serverSocket.accept();

                synchronized (connectLock) {
                    connectLock.wait();
                }
            } catch (IOException | InterruptedException ignore) {
            }
        }).start();

        synchronized (readyLock) {
            try {
                readyLock.wait();
            } catch (InterruptedException ignore) {
            }
        }

        final XMLTcpClient client = new XMLTcpClient(HOST, 20_000);
        client.connect(packet -> {}, null);

        assertTrue(client.isConnected());

        synchronized (connectLock) {
            connectLock.notify();
        }

        client.disconnect();
    }

    @Test
    public void testConnect_Closed() {
        final XMLTcpClient client = new XMLTcpClient(HOST, 20_001);

        assertThrows(TcpConnectException.class, () -> client.connect(packet -> {}, null));
    }

    @Test
    public void testReadAndWrite() throws IOException {
        final List<XMLProtocolPacket> receivedPackets = new ArrayList<>();
        final Object readyLock = new Object(), connectLock = new Object(), readLock = new Object();

        new Thread(() -> {
            try(ServerSocket serverSocket = new ServerSocket(20_002)) {
                synchronized (readyLock) {
                    readyLock.notify();
                }

                final Socket socket = serverSocket.accept();

                synchronized (connectLock) {
                    connectLock.wait();
                }

                final InputStream inputStream = socket.getInputStream();
                final byte[] buffer = new byte[512];
                int nRead;
                boolean protocolInitiated = false;

                while((nRead = inputStream.read(buffer)) > 0) {
                    StringBuilder builder = new StringBuilder(new String(buffer, 0, nRead, StandardCharsets.UTF_8));

                    if (nRead == buffer.length) {
                        while (inputStream.available() > 0) {
                            nRead = inputStream.read(buffer);

                            builder.append(new String(buffer, 0, nRead, StandardCharsets.UTF_8));
                        }
                    }

                    final String xml = builder.toString();

                    if (xml.equals("<protocol>")) {
                        protocolInitiated = true;

                        socket.getOutputStream().write(("<buffer>" + "x".repeat(512) + "</buffer>").getBytes());
                        socket.getOutputStream().flush();
                    } else if (protocolInitiated && xml.equals("<join gameType=\"test\"/>")) {
                        socket.getOutputStream().write("<protocol><joined roomId=\"test\"/>".getBytes());
                        socket.getOutputStream().flush();
                    }
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

            final XMLTcpClient client = new XMLTcpClient(HOST, 20_002);
            client.connect(packet -> {
                if(packet instanceof JoinedRoomResponse) {
                    receivedPackets.add(packet);

                    client.disconnect();
                }
            }, message -> {});

            synchronized (connectLock) {
                connectLock.notify();
            }

            client.send(new JoinGameRequest("test"));

            synchronized (readLock) {
                readLock.wait();
            }
        } catch (InterruptedException ignore) {
        }

        assertFalse(receivedPackets.isEmpty());
    }

}
