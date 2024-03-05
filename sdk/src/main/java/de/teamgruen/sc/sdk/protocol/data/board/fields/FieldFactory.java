/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.board.fields;

import de.teamgruen.sc.sdk.protocol.data.Direction;
import lombok.NonNull;

public class FieldFactory {

    public static Water water() {
        return new Water();
    }

    public static Island island() {
        return new Island();
    }

    public static Passenger passenger(@NonNull Direction direction, int passengers) {
        return new Passenger(direction, passengers);
    }

    public static Finish finish() {
        return new Finish();
    }

}
