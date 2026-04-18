/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.annotation.Annotation;

/**
 * Immutable implementation of {@link Range} used by the generator at run time.
 * Each instance stores inclusive lowest and highest values. The constants cover
 * the full span of the primitive integer types.
 */
@SuppressWarnings("ClassExplicitlyAnnotation")
final class RangeImpl implements Range {
    /** Range covering every {@code byte} value. */
    static final Range DEFAULT_BYTE_RANGE = new RangeImpl(Byte.MIN_VALUE, Byte.MAX_VALUE);
    /** Range covering every {@code char} value. */
    static final Range DEFAULT_CHAR_RANGE = new RangeImpl(Character.MIN_VALUE, Character.MAX_VALUE);
    /** Range covering every {@code short} value. */
    static final Range DEFAULT_SHORT_RANGE = new RangeImpl(Short.MIN_VALUE, Short.MAX_VALUE);
    /** Range covering every {@code int} value. */
    static final Range DEFAULT_INT_RANGE = new RangeImpl(Integer.MIN_VALUE, Integer.MAX_VALUE);
    /** Range covering every {@code long} value. */
    static final Range DEFAULT_LONG_RANGE = new RangeImpl(Long.MIN_VALUE, Long.MAX_VALUE);

    private final long min, max;

    /**
     * Builds a range with the given inclusive bounds.
     *
     * @param min smallest permitted value
     * @param max largest permitted value
     */
    RangeImpl(long min, long max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Inclusive lower bound of the range.
     */
    @Override
    public long min() {
        return min;
    }

    /**
     * Inclusive upper bound of the range.
     */
    @Override
    public long max() {
        return max;
    }

    /**
     * Required by the {@link java.lang.annotation.Annotation} contract.
     */
    @Override
    public Class<? extends Annotation> annotationType() {
        return Range.class;
    }

    /**
     * RangeImpl instances are not intended for hashed collections.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    /**
     * Two ranges are equal when their bounds are both equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Range))
            return false;
        Range other = (Range) obj;
        return other.min() == min && other.max() == max;
    }
}
