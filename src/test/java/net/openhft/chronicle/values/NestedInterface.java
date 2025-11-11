/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/*
 * Created by peter.lawrey on 03/03/2015.
 */
/**
 * Extends {@link JavaBeanInterface} with two nested beans.
 * The nested beans, {@code nestedA} and {@code nestedB}, are also
 * {@link JavaBeanInterface} instances enabling hierarchical structures
 * in tests.
 */
public interface NestedInterface extends JavaBeanInterface {
    JavaBeanInterface getNestedA();

    void setNestedA(JavaBeanInterface nestedA);

    JavaBeanInterface getNestedB();

    void setNestedB(JavaBeanInterface nestedB);
}
