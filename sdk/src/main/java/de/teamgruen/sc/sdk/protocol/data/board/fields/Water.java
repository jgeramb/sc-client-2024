package de.teamgruen.sc.sdk.protocol.data.board.fields;

import lombok.Data;

@Data
public class Water implements Field {

    @Override
    public boolean isPassable() {
        return true;
    }

}
