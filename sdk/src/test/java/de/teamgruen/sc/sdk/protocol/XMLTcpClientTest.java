/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol;

import de.teamgruen.sc.sdk.protocol.data.Move;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.requests.JoinGameRequest;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XMLTcpClientTest {

    private XMLTcpClient client;

    @BeforeEach
    public void setUp() {
        this.client = new XMLTcpClient("localhost", 13050);
    }

    @Test
    public void testConnect() throws IOException {
        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(),
                xml -> {}
        ));
        this.client.connect(packet -> {}, null);
        this.client.disconnect();
    }

    @Test
    public void testConnect_Closed() {
        this.client.setSocket(new TestSocket(
                true,
                false,
                false,
                true,
                new LinkedList<>(),
                xml -> {}
        ));

        assertThrows(TcpConnectException.class, () -> this.client.connect(packet -> {}, null));
    }

    @Test
    public void testDisconnect_Error() throws TcpConnectException {
        this.client.setSocket(new TestSocket(
                false,
                true,
                false,
                false,
                new LinkedList<>(),
                xml -> {}
        ));
        this.client.connect(packet -> {}, null);

        assertThrows(IOException.class, () -> this.client.disconnect());
    }

    @Test
    public void testWrite_IOException() throws IOException, InterruptedException {
        this.client.setSocket(new TestSocket(
                false,
                false,
                true,
                false,
                new LinkedList<>(),
                xml -> {}
        ));

        assertError("Failed to write to OutputStream: Test exception");
    }

    @Test
    public void testWrite_SerializationException() throws IOException, InterruptedException {
        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(),
                xml -> {}
        ));
        this.client.send(new MovePacket("test", new Move(null)));

        assertError("Failed to serialize XMLProtocolPacket: com.fasterxml.jackson.databind.JsonMappingException: java.lang.NullPointerException: Cannot invoke \"java.util.List.iterator()\" because the return value of \"de.teamgruen.sc.sdk.protocol.data.Move.actions()\" is null (through reference chain: de.teamgruen.sc.sdk.protocol.room.MovePacket[\"data\"])");
    }

    @Test
    public void testWrite() throws IOException, InterruptedException {
        final Object closeLock = new Object();
        final AtomicBoolean packetSent = new AtomicBoolean(false),
                protocolInitiated = new AtomicBoolean(false);

        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(),
                xml -> {
                    if(!protocolInitiated.get()) {
                        assertEquals("<protocol>", xml);

                        protocolInitiated.set(true);
                    } else {
                        assertEquals("<join gameType=\"test\"/>", xml);

                        packetSent.set(true);

                        synchronized (closeLock) {
                            closeLock.notify();
                        }
                    }
                }
        ));
        this.client.connect(packet -> {}, null);
        this.client.send(new JoinGameRequest("test"));

        synchronized (closeLock) {
            closeLock.wait(1_000);
        }

        this.client.disconnect();

        assertTrue(packetSent.get());
    }

    @Test
    public void testRead_ProtocolNotInitiated() throws IOException {
        final Object closeLock = new Object();
        final AtomicBoolean packetReceived = new AtomicBoolean(false);

        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(List.of("<joined roomId=\"test\"/>")),
                xml -> {}
        ));
        this.client.connect(packet -> {
            if (packet instanceof JoinedRoomResponse) {
                packetReceived.set(true);

                synchronized (closeLock) {
                    closeLock.notify();
                }
            }
        }, null);

        synchronized (closeLock) {
            try {
                closeLock.wait(250);
            } catch (InterruptedException ignore) {
            }
        }

        this.client.disconnect();

        assertFalse(packetReceived.get());
    }

    @Test
    public void testRead_Error() throws IOException, InterruptedException {
        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                true,
                new LinkedList<>(),
                xml -> {}
        ));

        assertError("Failed to read from InputStream: Test exception");
    }

    @Test
    public void testRead_MultiUseBuffer() throws IOException, InterruptedException {
        final Object closeLock = new Object();
        final AtomicBoolean packetReceived = new AtomicBoolean(false);

        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(List.of("<protocol>", "<packet>" + "x".repeat(512) + "</packet>", "<joined roomId=\"test\"/>")),
                xml -> {}
        ));
        this.client.connect(packet -> {
            if (packet instanceof JoinedRoomResponse response) {
                assertEquals("test", response.getRoomId());

                packetReceived.set(true);

                synchronized (closeLock) {
                    closeLock.notify();
                }
            }
        }, null);

        synchronized (closeLock) {
            closeLock.wait(1_000);
        }

        this.client.disconnect();

        assertTrue(packetReceived.get());
    }

    @Test
    public void testRead_DeserializationException() throws IOException, InterruptedException {
        this.client.setSocket(new TestSocket(
                false,
                false,
                false,
                false,
                new LinkedList<>(List.of("<protocol>", "<invalid/>")),
                xml -> {}
        ));

        assertError("Failed to deserialize XML: java.lang.IllegalArgumentException: Unknown packet type: invalid");
    }

    private void assertError(String expectedMessage) throws IOException, InterruptedException {
        final Object closeLock = new Object();
        final AtomicBoolean errorReceived = new AtomicBoolean(false);

        this.client.connect(packet -> {}, error -> {
            assertEquals(expectedMessage, error);

            errorReceived.set(true);

            synchronized (closeLock) {
                closeLock.notify();
            }
        });

        synchronized (closeLock) {
            closeLock.wait(1_000);
        }

        this.client.disconnect();

        assertTrue(errorReceived.get());
    }

}
