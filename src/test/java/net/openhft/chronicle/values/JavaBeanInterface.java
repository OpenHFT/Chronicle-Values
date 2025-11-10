//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

package net.openhft.chronicle.values;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Value interface exercising a variety of primitive, enumerated and
 * Java&nbsp;8 date/time fields. The methods follow the JavaBean pattern so
 * that code generation tests can validate setter and getter handling for each
 * type.
 */
public interface JavaBeanInterface {

    /**
     * Signed eight-bit integer field.
     */
    byte getS8();

    /**
     * Updates the signed eight-bit integer field.
     */
    void setS8(byte s8);

    /**
     * Unsigned sixteen-bit value stored in a {@code char}.
     */
    char getU16();

    /**
     * Updates the unsigned sixteen-bit value.
     */
    void setU16(char u16);

    /**
     * Signed sixteen-bit integer field.
     */
    short getS16();

    /**
     * Updates the signed sixteen-bit integer field.
     */
    void setS16(short s16);

    /**
     * Signed thirty-two-bit integer field.
     */
    int getS32();

    /**
     * Updates the signed thirty-two-bit integer field.
     */
    void setS32(int s32);

    /**
     * Signed sixty-four-bit integer field.
     */
    long getS64();

    /**
     * Updates the signed sixty-four-bit integer field.
     */
    void setS64(long s64);

    /**
     * Thirty-two-bit floating point field.
     */
    float getF32();

    /**
     * Updates the thirty-two-bit floating point field.
     */
    void setF32(float f32);

    /**
     * Sixty-four-bit floating point field.
     */
    double getF64();

    /**
     * Updates the sixty-four-bit floating point field.
     */
    void setF64(double f64);

    /**
     * Enumeration used in tests.
     */
    BuySell getBuySell();

    /**
     * Updates the enumeration field.
     */
    void setBuySell(BuySell buySell);

    /**
     * Arbitrary text field.
     */
    String getText();

    /**
     * Updates the text field.
     */
    void setText(String text);

    /**
     * {@link LocalDate} without a time component.
     */
    LocalDate getDate();

    /**
     * Updates the {@link LocalDate} field.
     */
    void setDate(LocalDate date);

    /**
     * Time of day using {@link LocalTime}.
     */
    LocalTime getTime();

    /**
     * Updates the {@link LocalTime} field.
     */
    void setTime(LocalTime time);

    /**
     * Combined date and time using {@link LocalDateTime}.
     */
    LocalDateTime getDateTime();

    /**
     * Updates the {@link LocalDateTime} field.
     */
    void setDateTime(LocalDateTime dateTime);
}
