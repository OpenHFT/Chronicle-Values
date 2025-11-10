//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values;

public interface HasArraysInterface {
    /**
     * Stores {@code flag} in the four-element flag array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setFlagAt(int idx, boolean flag);

    /**
     * Reads the flag at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    boolean getFlagAt(int idx);

    /**
     * Stores {@code b} in the four-element byte array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setByteAt(int idx, byte b);

    /**
     * Reads a byte at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    byte getByteAt(int idx);

    /**
     * Stores {@code s} in the four-element short array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setShortAt(int idx, short s);

    /**
     * Reads the short value at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    short getShortAt(int idx);

    /**
     * Stores {@code ch} in the four-element character array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setCharAt(int idx, char ch);

    /**
     * Reads the character at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    char getCharAt(int idx);

    /**
     * Stores {@code i} in the four-element integer array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setIntAt(int idx, int i);

    /**
     * Reads the integer at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    int getIntAt(int idx);

    /**
     * Stores {@code f} in the four-element float array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setFloatAt(int idx, float f);

    /**
     * Reads the float at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    float getFloatAt(int idx);

    /**
     * Stores {@code l} in the four-element long array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setLongAt(int idx, long l);

    /**
     * Reads the long value at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    long getLongAt(int idx);

    /**
     * Stores {@code d} in the four-element double array.
     *
     * @param idx index in the range {@code 0..3}
     */
    @Array(length = 4)
    void setDoubleAt(int idx, double d);

    /**
     * Reads the double value at {@code idx} from the four-element array.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    double getDoubleAt(int idx);

    /**
     * Stores {@code s} in the four-element string array. Each element is at most
     * eight UTF-8 bytes.
     *
     * @param idx index in the range {@code 0..3}
     * @param s string limited to eight UTF-8 bytes
     */
    @Array(length = 4)
    void setStringAt(int idx, @MaxUtf8Length(8) String s);

    /**
     * Reads the string at {@code idx}. Each stored string is at most eight UTF-8
     * bytes.
     *
     * @param idx index in the range {@code 0..3}
     * @return stored value
     */
    String getStringAt(int idx);
}
