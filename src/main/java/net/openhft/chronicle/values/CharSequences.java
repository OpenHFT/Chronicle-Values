/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * Small collection of helpers for hashing and comparing {@link CharSequence}
 * objects.
 *
 * <p>The methods deal explicitly with {@code null} values. The
 * {@link #hashCode(CharSequence)} helper returns {@code 0} for a {@code null}
 * reference and otherwise produces the same result as
 * {@link String#hashCode()}. The {@link #equals(CharSequence, CharSequence)}
 * method mirrors {@link java.util.Objects#equals(Object, Object)} by
 * considering two {@code null} references equal and carrying out a
 * character-by-character comparison otherwise.
 */
public enum CharSequences {
    ; // none

    /**
     * Computes the hash code of a character sequence.
     *
     * @param cs the sequence to hash, may be {@code null}
     * @return zero for {@code null}; otherwise the hash of its characters
     */
    public static int hashCode(CharSequence cs) {
        if (cs == null)
            return 0;
        int h = 0;
        for (int i = 0; i < cs.length(); i++) {
            h = 31 * h + cs.charAt(i);
        }
        return h;
    }

    /**
     * Character-wise comparison matching
     * {@link java.util.Objects#equals(Object, Object)}.
     *
     * @param left  first sequence, may be {@code null}
     * @param right second sequence, may be {@code null}
     * @return {@code true} if both are {@code null} or contain identical characters
     */
    public static boolean equals(CharSequence left, CharSequence right) {
        if (left == null)
            return right == null;
        if (left.length() != right.length())
            return false;
        for (int i = 0; i < left.length(); i++) {
            if (left.charAt(i) != right.charAt(i))
                return false;
        }
        return true;
    }
}
