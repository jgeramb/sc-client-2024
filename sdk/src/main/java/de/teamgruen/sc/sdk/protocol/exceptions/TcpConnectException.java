/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.exceptions;

import java.io.IOException;

public class TcpConnectException extends IOException {

    public TcpConnectException(Throwable cause) {
        super(cause);
    }

}
