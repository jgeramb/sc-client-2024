package de.teamgruen.sc.sdk.protocol.exceptions;

import java.io.IOException;

public class TcpCloseException extends IOException {

    public TcpCloseException(Throwable cause) {
        super(cause);
    }

}
