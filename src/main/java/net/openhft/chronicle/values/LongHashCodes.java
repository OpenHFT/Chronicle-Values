/*
 *      Copyright (C) 2015  higherfrequencytrading.com
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.values;

public final class LongHashCodes {

    private static final long NULL_HASHCODE = Long.MIN_VALUE;

    public static long calcLongHashCode(boolean a) {
        return a ? 1 : 0;
    }

    public static long calcLongHashCode(Boolean a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.booleanValue());
    }

    public static long calcLongHashCode(byte a) {
        return a;
    }

    public static long calcLongHashCode(Byte a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.byteValue());
    }

    public static long calcLongHashCode(char a) {
        return a;
    }

    public static long calcLongHashCode(Character a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.charValue());
    }

    public static long calcLongHashCode(short a) {
        return a;
    }

    public static long calcLongHashCode(Short a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.shortValue());
    }

    public static long calcLongHashCode(int a) {
        return a;
    }

    public static long calcLongHashCode(Integer a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.intValue());
    }

    public static long calcLongHashCode(long a) {
        return a;
    }

    public static long calcLongHashCode(Long a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.longValue());
    }

    public static long calcLongHashCode(float a) {
        return Float.floatToRawIntBits(a);
    }

    public static long calcLongHashCode(Float a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.floatValue());
    }

    public static long calcLongHashCode(double a) {
        return Double.doubleToRawLongBits(a);
    }

    public static long calcLongHashCode(Double a) {
        return a == null ? NULL_HASHCODE : calcLongHashCode(a.doubleValue());
    }

    public static long calcLongHashCode(Object t) {
        return t == null ? NULL_HASHCODE :
                t instanceof CharSequence ? calcLongHashCode((CharSequence) t) : t.hashCode();
    }

    public static long calcLongHashCode(CharSequence s) {
        if (s == null)
            return NULL_HASHCODE;
        long hash = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            hash = 57 * hash + s.charAt(i);
        }
        return hash;
    }

    private LongHashCodes() {}
}
