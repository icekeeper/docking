package ru.ifmo.docking.util;

public class Triple<T1, T2, T3> {
    public final T1 first;
    public final T2 second;
    public final T3 third;

    public Triple(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <T1, T2, T3> Triple<T1, T2, T3> of(T1 first, T2 second, T3 third) {
        return new Triple<>(first, second, third);
    }

    @Override
    public String toString() {
        return "(" + first +
                ", " + second +
                ", " + third +
                ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple triple = (Triple) o;

        return !(first != null ? !first.equals(triple.first) : triple.first != null) &&
                !(second != null ? !second.equals(triple.second) : triple.second != null) &&
                !(third != null ? !third.equals(triple.third) : triple.third != null);

    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        result = 31 * result + (third != null ? third.hashCode() : 0);
        return result;
    }
}
