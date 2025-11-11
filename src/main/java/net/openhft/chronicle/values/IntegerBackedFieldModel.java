/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import java.lang.reflect.Method;

/**
 * Base class for field models that store their logical value as an
 * integer. Types such as {@code java.util.Date} or enums encode their
 * value into an {@link IntegerFieldModel} so that the layout and
 * alignment rules of primitive integers can be reused.
 */
class IntegerBackedFieldModel extends PrimitiveFieldModel {

    /**
     * The integer representation used to perform the actual storage. Many
     * methods, for example {@link #sizeInBits()}, simply delegate to this
     * model.
     */
    final IntegerFieldModel backend = new IntegerFieldModel(this);

    @Override
    void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        backend.addVolatileInfo(template);
    }

    @Override
    int sizeInBits() {
        return backend.sizeInBits();
    }

    @Override
    int offsetAlignmentInBytes() {
        return backend.offsetAlignmentInBytes();
    }

    @Override
    int dontCrossAlignmentInBytes() {
        return backend.dontCrossAlignmentInBytes();
    }

    @Override
    void checkState() {
        super.checkState();
        backend.checkState();
    }
}
