/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.actions;

import de.teamgruen.sc.sdk.game.GameState;
import lombok.NonNull;

public interface Action {

    void perform(@NonNull GameState gameState);

}
