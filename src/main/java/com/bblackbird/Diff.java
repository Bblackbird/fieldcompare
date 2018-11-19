package com.bblackbird;

import java.lang.reflect.Type;
import java.util.Objects;

public class Diff {

    public final String fieldName;
    public final Type type;
    public final Object left;
    public final Object right;

    public Diff(String fieldName, Type type, Object oldValue, Object newValue) {
        this.fieldName = fieldName;
        this.type = type;
        this.left = oldValue;
        this.right = newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Diff diff = (Diff) o;
        return Objects.equals(fieldName, diff.fieldName) &&
                Objects.equals(type, diff.type) &&
                Objects.equals(left, diff.left) &&
                Objects.equals(right, diff.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, type, left, right);
    }

    @Override
    public String toString() {
        return "Diff{" +
                "fieldName='" + fieldName + '\'' +
                ", type=" + type +
                ", left=" + left +
                ", right=" + right +
                '}';
    }
}
