package de.teamgruen.sc.sdk.protocol.data.board.fields;

import lombok.Data;

@Data
public class Island implements Field {

    @Override
    public boolean isObstacle() {
        return true;
    }

}
