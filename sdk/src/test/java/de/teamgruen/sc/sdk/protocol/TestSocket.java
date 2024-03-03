package de.teamgruen.sc.sdk.protocol;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class TestSocket extends Socket {

    private final boolean failConnect, failDisconnect, failWrite, failRead;
    private final LinkedList<String> xmlQueue;
    private final Consumer<String> writeListener;
    private final Object dataLock = new Object();

    private boolean isConnected = false;
    private byte[] currentData = new byte[0];
    private int currentDataOffset = 0;
    private String writeBuffer = "";

    @Override
    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        if(this.failConnect)
            throw new IOException("Test exception");

        this.isConnected = true;
    }

    @Override
    public synchronized void close() throws IOException {
        if(this.failDisconnect)
            throw new IOException("Test exception");

        this.isConnected = false;
    }

    @Override
    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws RuntimeException {
                if(failWrite)
                    throw new RuntimeException("Test exception");

                writeBuffer += (char) b;
            }

            @Override
            public void flush() {
                if(!writeBuffer.isBlank())
                    writeListener.accept(writeBuffer);

                writeBuffer = "";

                synchronized (dataLock) {
                    dataLock.notify();
                }
            }
        };
    }

    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                if(failRead)
                    throw new IOException("Test exception");

                if(currentData == null) {
                    synchronized (dataLock) {
                        try {
                            dataLock.wait(250);
                        } catch (InterruptedException ignore) {
                        }
                    }

                    if(xmlQueue.isEmpty())
                        return -1;
                }

                if(currentData == null || currentDataOffset == currentData.length) {
                    final String data = xmlQueue.poll();

                    currentData = data == null ? null : data.getBytes(StandardCharsets.UTF_8);
                    currentDataOffset = 0;

                    return this.read();
                }

                return currentData[currentDataOffset++] & 0xFF;
            }

            @Override
            public int available() {
                if(currentData == null || currentData.length == 0)
                    return 0;

                return currentData.length - currentDataOffset;
            }
        };
    }

}
