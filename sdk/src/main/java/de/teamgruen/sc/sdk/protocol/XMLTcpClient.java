/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol;

import de.teamgruen.sc.sdk.protocol.exceptions.DeserializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.room.MovePacket;
import de.teamgruen.sc.sdk.protocol.serialization.PacketSerializationUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class XMLTcpClient {

    private static long lastGarbageCollection = 0;

    @Setter
    private Socket socket = new Socket();
    private final Queue<XMLProtocolPacket> requestQueue = new LinkedBlockingQueue<>();
    private final Object requestLock = new Object();
    private Thread readThread, writeThread;

    private final String host;
    private final int port;

    /**
     * Connects to the server and starts the read and write threads.
     *
     * @param packetListener the listener for incoming packets
     * @param errorListener the listener for errors
     * @throws TcpConnectException if the connection fails
     */
    public void connect(@NonNull Consumer<XMLProtocolPacket> packetListener,
                        Consumer<String> errorListener) throws TcpConnectException {
        try {
            this.socket.setTcpNoDelay(true);
            this.socket.setKeepAlive(true);
            this.socket.connect(new InetSocketAddress(this.host, this.port));
        } catch (IOException ex) {
            throw new TcpConnectException(ex);
        }

        // warm up CPU for faster deserialization
        PacketSerializationUtil.deserializeXML("joined", "<joined roomId=\"warmUp\" />");

        // read packets
        (this.readThread = new Thread(() -> {
            if(!this.isConnected())
                return;

            try(InputStream in = this.socket.getInputStream()) {
                boolean protocolInitiated = false;

                final byte[] buffer = new byte[512];
                final StringBuilder builder = new StringBuilder();
                int nRead;

                while((nRead = in.read(buffer)) != -1) {
                    builder.append(new String(buffer, 0, nRead, StandardCharsets.UTF_8));

                    if(!protocolInitiated) {
                        final int protocolIndex = builder.toString().indexOf("<protocol>");

                        if (protocolIndex != -1) {
                            builder.delete(protocolIndex, protocolIndex + 10);
                            protocolInitiated = true;
                        } else
                            continue;
                    }

                    String tagName;

                    while((tagName = PacketSerializationUtil.parseXMLTagName(builder.toString())) != null) {
                        final Pattern pattern = Pattern.compile("<" + tagName + "(.+)?/>");
                        int endTagIndex;

                        while(true) {
                            endTagIndex = builder.toString().indexOf("</" + tagName + ">");

                            if(endTagIndex != -1) {
                                endTagIndex += tagName.length() + 3;
                                break;
                            }

                            final Matcher matcher = pattern.matcher(builder.toString());

                            if (matcher.find()) {
                                endTagIndex = matcher.end();
                                break;
                            }

                            if ((nRead = in.read(buffer)) == -1)
                                return;

                            builder.append(new String(buffer, 0, nRead, StandardCharsets.UTF_8));
                        }

                        try {
                            packetListener.accept(PacketSerializationUtil.deserializeXML(tagName, builder.substring(0, endTagIndex)));
                        } catch (DeserializationException ex) {
                            if(errorListener != null)
                                errorListener.accept("Failed to deserialize XML: " + ex.getMessage());
                        }

                        builder.delete(0, endTagIndex);
                    }
                }
            } catch (IOException ex) {
                if(errorListener == null || ex.getMessage() == null)
                    return;

                if(ex.getMessage().contains("closed") || ex.getMessage().contains("reset"))
                    return;

                errorListener.accept("Failed to read from InputStream: " + ex.getMessage());
            }
        }, "ReadThread")).start();

        // write packets
        (this.writeThread = new Thread(() -> {
            if(!this.isConnected())
                return;

            try(PrintWriter out = new PrintWriter(this.socket.getOutputStream())) {
                out.print("<protocol>");
                out.flush();

                while(this.isConnected()) {
                    synchronized (this.requestLock) {
                        if(this.requestQueue.isEmpty())
                            this.requestLock.wait();
                    }

                    final XMLProtocolPacket packet = this.requestQueue.poll();

                    if(packet != null) {
                        try {
                            String xml = PacketSerializationUtil.serialize(packet);

                            out.print(xml);
                            out.flush();
                        } catch (SerializationException ex) {
                            if(errorListener != null)
                                errorListener.accept("Failed to serialize XMLProtocolPacket: " + ex.getMessage());
                        }

                        if(packet instanceof MovePacket && System.currentTimeMillis() - lastGarbageCollection > 1_000L) {
                            // collect garbage to reduce probability of lags
                            System.gc();

                            lastGarbageCollection = System.currentTimeMillis();
                        }
                    }
                }
            } catch (InterruptedException | IOException | RuntimeException ex) {
                if(errorListener == null || ex.getMessage() == null)
                    return;

                if(ex.getMessage().contains("closed") || ex.getMessage().contains("reset"))
                    return;

                errorListener.accept("Failed to write to OutputStream: " + ex.getMessage());
            }
        }, "WriteThread")).start();
    }

    /**
     * Disconnects from the server and stops the read and write threads.
     */
    public void disconnect() throws IOException {
        if(!this.isConnected()) return;

        try {
            this.socket.close();
        } finally {
            if(this.readThread != null) {
                this.readThread.interrupt();
                this.readThread = null;
            }

            if(this.writeThread != null) {
                this.writeThread.interrupt();
                this.writeThread = null;
            }
        }
    }

    public boolean isConnected() {
        return this.socket.isConnected();
    }

    public void send(XMLProtocolPacket... packets) {
        this.requestQueue.addAll(List.of(packets));

        synchronized (this.requestLock) {
            this.requestLock.notify();
        }
    }

}
