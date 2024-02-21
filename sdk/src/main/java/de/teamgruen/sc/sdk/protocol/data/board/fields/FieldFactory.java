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
        Passenger field = new Passenger();
        field.setDirection(direction);
        field.setPassenger(passengers);

        return field;
    }

    public static Finish finish() {
        return new Finish();
    }

}
