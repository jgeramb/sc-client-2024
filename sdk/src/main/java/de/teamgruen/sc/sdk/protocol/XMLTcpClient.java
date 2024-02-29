package de.teamgruen.sc.sdk.protocol;

import de.teamgruen.sc.sdk.protocol.exceptions.DeserializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.TcpConnectException;
import de.teamgruen.sc.sdk.protocol.serialization.PacketSerializationUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class XMLTcpClient {

    private Socket socket;
    private final Queue<XMLProtocolPacket> requestQueue = new LinkedBlockingQueue<>();
    private final Object requestLock = new Object();
    private Thread readThread, writeThread;

    private final String host;
    private final int port;

    public void connect(@NonNull Consumer<XMLProtocolPacket> responseListener,
                        Consumer<String> errorListener) throws TcpConnectException {
        try {
            this.socket = new Socket(this.host, this.port);
            this.socket.setTcpNoDelay(true);
            this.socket.setKeepAlive(true);
        } catch (IOException ex) {
            throw new TcpConnectException(ex);
        }

        // read packets
        (this.readThread = new Thread(() -> {
            if(!this.isConnected())
                return;

            try(InputStream in = this.socket.getInputStream()) {
                boolean protocolInitiated = false;

                final byte[] buffer = new byte[512];
                int nRead;

                while((nRead = in.read(buffer)) > 0) {
                    StringBuilder builder = new StringBuilder(new String(buffer, 0, nRead, StandardCharsets.UTF_8));
                    long readStart = System.currentTimeMillis();

                    if(nRead == buffer.length) {
                        final String tagName = Objects.requireNonNull(PacketSerializationUtil.parseXMLTagName(builder.toString()));

                        while(System.currentTimeMillis() - readStart <= 2_500
                                && (in.available() > 0 || !builder.toString().contains("</" + tagName + ">"))) {
                            nRead = in.read(buffer);

                            builder.append(new String(buffer, 0, nRead, StandardCharsets.UTF_8));
                        }
                    }

                    String xml = builder.toString().strip();

                    if(xml.startsWith("<protocol>")) {
                        xml = xml.replace("<protocol>", "").stripLeading();
                        protocolInitiated = true;
                    }

                    if(xml.isEmpty() || !protocolInitiated)
                        continue;

                    try {
                        PacketSerializationUtil.deserialize(xml).forEach(responseListener);
                    } catch (DeserializationException ex) {
                        if(errorListener != null)
                            errorListener.accept("Failed to deserialize XML: " + ex.getMessage());
                    }
                }
            } catch (IOException ex) {
                if(errorListener != null && ex.getMessage() != null && !ex.getMessage().contains("closed") && !ex.getMessage().contains("reset"))
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
                    if(this.requestQueue.isEmpty()) {
                        synchronized (this.requestLock) {
                            this.requestLock.wait();
                        }
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
                    }
                }
            } catch (InterruptedException | IOException ex) {
                if(errorListener != null && ex.getMessage() != null && !ex.getMessage().contains("closed") && !ex.getMessage().contains("reset"))
                    errorListener.accept("Failed to write to OutputStream: " + ex.getMessage());
            }
        }, "WriteThread")).start();
    }

    public void disconnect() {
        if(!this.isConnected()) return;

        try {
            this.socket.close();
        } catch (IOException ignore) {
        } finally {
            this.socket = null;

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
        return socket != null && socket.isConnected();
    }

    public void send(XMLProtocolPacket... packets) {
        this.requestQueue.addAll(List.of(packets));

        synchronized (this.requestLock) {
            this.requestLock.notify();
        }
    }

}
