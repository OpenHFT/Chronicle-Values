/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

import net.openhft.chronicle.core.Maths;

import java.lang.reflect.Method;

import static net.openhft.chronicle.values.Primitives.widthInBits;

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
