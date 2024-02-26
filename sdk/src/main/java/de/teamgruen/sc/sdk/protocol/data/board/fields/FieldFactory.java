package de.teamgruen.sc.sdk.protocol.data.board.fields;

import de.teamgruen.sc.sdk.protocol.data.Direction;

public class FieldFactory {

    public static Water water() {
        return new Water();
    }

    public static Island island() {
        return new Island();
    }

    public static Passenger passenger(Direction direction, int passengers) {
        return new Passenger(direction, passengers);
    }

    public static Finish finish() {
        return new Finish();
    }

}
