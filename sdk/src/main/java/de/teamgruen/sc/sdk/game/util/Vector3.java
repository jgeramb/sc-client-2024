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
        Vector3 vector = this.copy();
        vector.q *= times;
        vector.s *= times;
        vector.r *= times;

        return vector;
    }

    public Vector3 add(Vector3 delta) {
        Vector3 vector = this.copy();
        vector.q += delta.q;
        vector.s += delta.s;
        vector.r += delta.r;

        return vector;
    }

    public Vector3 subtract(Vector3 delta) {
        Vector3 vector = this.copy();
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

    public byte toColumnIndex() {
        if(this.q == 1 && this.s == -1)
            return 0;
        else if((this.q == 1 && this.s == 0) || (this.q == 0 && this.s == -1))
            return 1;
        else if((this.q == 0 && this.s == 1) || (this.q == -1 && this.s == 0))
            return 2;
        else if(this.q == -1 && this.s == 1)
            return 3;

        throw new IllegalArgumentException("Not a direction vector");
    }

}
