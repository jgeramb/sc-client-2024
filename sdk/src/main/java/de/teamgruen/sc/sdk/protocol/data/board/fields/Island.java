/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.board.fields;

import lombok.Data;

@Data
public class Island implements Field {

    @Override
    public boolean isObstacle() {
        return true;
    }

}
