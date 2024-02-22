package de.teamgruen.sc.sdk.game.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Vector3 {

    private int q, s, r;

    public void translate(int q, int s, int r) {
        this.q += q;
        this.s += s;
        this.r += r;
    }

    public void translate(Vector3 delta) {
        this.translate(delta.q, delta.s, delta.r);
    }

    public Vector3 multiply(int times) {
        final Vector3 vector = this.copy();
        vector.q *= times;
        vector.s *= times;
        vector.r *= times;

        return vector;
    }

    public Vector3 add(Vector3 delta) {
        final Vector3 vector = this.copy();
        vector.q += delta.q;
        vector.s += delta.s;
        vector.r += delta.r;

        return vector;
    }

    public Vector3 subtract(Vector3 delta) {
        final Vector3 vector = this.copy();
        vector.q -= delta.q;
        vector.s -= delta.s;
        vector.r -= delta.r;

        return vector;
    }

    public Vector3 copy() {
        return new Vector3(this.q, this.s, this.r);
    }

    public boolean equals(Vector3 vector) {
        return this.q == vector.q && this.s == vector.s && this.r == vector.r;
    }

}
