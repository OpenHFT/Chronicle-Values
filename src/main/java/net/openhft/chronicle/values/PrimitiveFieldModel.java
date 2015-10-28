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

import net.openhft.chronicle.core.Maths;

import java.lang.reflect.Method;

import static net.openhft.chronicle.values.Primitives.widthInBits;

abstract class PrimitiveFieldModel extends ScalarFieldModel {

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
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
        return offsetAlignment != Align.DEFAULT ? offsetAlignment : 1;
    }

    @Override
    int dontCrossAlignmentInBytes() {
        if (dontCrossAlignment == Align.DEFAULT)
            return Maths.nextPower2(sizeInBits(), 8) / 8;
        return dontCrossAlignment;
    }
}
