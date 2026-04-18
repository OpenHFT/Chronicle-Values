/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.core.Maths;

import java.lang.reflect.Method;

import static net.openhft.chronicle.values.Primitives.widthInBits;

/**
 * Provides layout rules for primitive fields within a value interface. The
 * calculations here take the raw primitive width and adjust it for
 * {@code volatile} or ordered writes so that atomic operations are possible.
 * <p>
 * {@link IntegerBackedFieldModel} reuses this logic for domain types encoded as
 * integers. Subclasses may override {@link #sizeInBits()} when the chosen
 * storage type must be wider than the logical primitive, typically because of
 * the concurrency semantics requested by the interface.
 */
abstract class PrimitiveFieldModel extends ScalarFieldModel {

    @Override
    void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        addVolatileInfo(template);
    }

    void addVolatileInfo(MethodTemplate template) {
        String regex = template.regex;
        if (regex.contains("Volatile") || regex.contains("Ordered") ||
                regex.contains("Atomic") || regex.contains("compareAndSwap")) {
            if (!alignmentSpecifiedExplicitly)
                offsetAlignment = Align.DEFAULT;
            // do not set alignmentSpecifiedExplicitly = true intentionally (later explicit
            // specification of alignment shouldn't fail because of this implicit alignment)
        }
    }

    /**
     * Volatile or ordered puts, unlike volatile gets, CASes and atomic adds, cannot extract the
     * field from wider byte/short/int/long word, => must be alone in the word.
     */
    int sizeInBitsConsideringVolatileOrOrderedPuts(int rawSizeInBits) {
        int minBits = setOrdered != null ? 32 : 8;
        return setVolatile != null || setOrdered != null ?
                Maths.nextPower2(rawSizeInBits, minBits) : rawSizeInBits;
    }

    /**
     * Width of this primitive in bits. Volatile or ordered setters may force
     * the value to align to at least a byte or word boundary so the returned
     * size can be greater than the raw primitive width.
     */
    @Override
    int sizeInBits() {
        return sizeInBitsConsideringVolatileOrOrderedPuts(widthInBits(type));
    }

    @Override
    int offsetAlignmentInBytes() {
        if (offsetAlignment == Align.DEFAULT)
            return Maths.nextPower2(sizeInBits(), 8) / 8;
        return offsetAlignment;
    }

    @Override
    int dontCrossAlignmentInBytes() {
        if (dontCrossAlignment == Align.DEFAULT)
            return Maths.nextPower2(sizeInBits(), 8) / 8;
        return dontCrossAlignment;
    }
}
