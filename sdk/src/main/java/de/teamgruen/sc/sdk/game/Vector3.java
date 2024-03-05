/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.game;

import lombok.*;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Vector3 {

    private int q, r, s;

    public Vector3 add(@NonNull Vector3 delta) {
        this.q += delta.q;
        this.r += delta.r;
        this.s += delta.s;

        return this;
    }

    public Vector3 subtract(@NonNull Vector3 delta) {
        final Vector3 vector = this.copy();
        vector.q -= delta.q;
        vector.r -= delta.r;
        vector.s -= delta.s;

        return vector;
    }

    public Vector3 multiply(int times) {
        this.q *= times;
        this.r *= times;
        this.s *= times;

        return this;
    }

    public Vector3 invert() {
        this.q = -this.q;
        this.r = -this.r;
        this.s = -this.s;

        return this;
    }

    public Vector3 copy() {
        return new Vector3(this.q, this.r, this.s);
    }

}
