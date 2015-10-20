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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import static net.openhft.chronicle.values.Primitives.isPrimitiveIntegerType;
import static net.openhft.chronicle.values.Utils.roundUp;

public class ArrayFieldModel extends FieldModel {

    ScalarFieldModel elemModel;
    Array array;

    @Override
    public void addLayoutInfo(Method m, MethodTemplate template) {
        super.addLayoutInfo(m, template);
        Array array = m.getAnnotation(Array.class);
        if (array != null) {
            if (this.array != null) {
                throw new IllegalStateException("@Array should be specified only once for " + name +
                        " field. Specified " + this.array + " and " + array);
            }
            if (array.length() <= 1)
                throw new IllegalStateException(array + ": length should be > 1, field " + name);
            this.array = array;
            elemModel.setOffsetAlignmentExplicitly(array.elementOffsetAlignment());
            elemModel.dontCrossAlignment = array.elementDontCrossAlignment();
        }
    }

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        if (elemModel == null)
            elemModel = createScalarModel(type);
        elemModel.addTypeInfo(m, template);
    }

    ScalarFieldModel createScalarModel(Class type) {
        return createScalarFieldModel(type, name);
    }

    @NotNull
    static ScalarFieldModel createScalarFieldModel(Class type, String fieldName) {
        if (isPrimitiveIntegerType(type))
            return new IntegerFieldModel();
        if (type == float.class || type == double.class)
            return new PrimitiveFieldModel();
        if (type == boolean.class)
            return new BooleanFieldModel();
        if (Enum.class.isAssignableFrom(type))
            return new EnumFieldModel();
        if (CharSequence.class.isAssignableFrom(type))
            return new CharSequenceFieldModel();
        if (type.isInterface())
            return new ValueFieldModel();
        throw new IllegalStateException(fieldName + " field type " + type + " is not supported: " +
                "not a primitive, enum, CharSequence or another value interface");
    }

    @Override
    int sizeInBits() {
        int elemSizeInBits = elemModel.sizeInBits();
        int elemAlignmentInBits = elemModel.offsetAlignmentInBits();
        if (elemSizeInBits <= elemAlignmentInBits) {
            // A power of 2, for fast index computation
            int elemsInOneAlignment = 1 << Maths.intLog2(elemAlignmentInBits / elemSizeInBits);
            return (array.length() / elemsInOneAlignment) * elemAlignmentInBits +
                    (array.length() % elemsInOneAlignment) * elemSizeInBits;
        } else {
            int roundedElemSizeInBits = roundUp(elemSizeInBits, elemAlignmentInBits);
            return roundedElemSizeInBits * (array.length() - 1) + elemSizeInBits;
        }
    }

    /**
     * 1, 2, 4 bits - unchanged, 3 bits - aligned to 4, otherwise aligned to a byte boundary
     */
    private int elemSizeInBits() {
        int elemSizeInBits = elemModel.sizeInBits();
        if (elemSizeInBits > 8)
            return (elemSizeInBits + 7) & ~7;
        return Maths.nextPower2(elemSizeInBits, 1);
    }

    @Override
    int offsetAlignmentInBytes() {
        int elementAlignment = Math.max(elemModel.offsetAlignmentInBytes(),
                elemModel.dontCrossAlignmentInBytes());
        if (offsetAlignment == Align.DEFAULT) {
            return elementAlignment;
        }
        if (offsetAlignment == 0 && elementAlignment == 1) {
            // Special case, to avoid ISE below, because offset alignment of 1 of the element model
            // could be implicit (element is CharSequence or another value)
            return 1;
        }
        if (offsetAlignment < elementAlignment ||
                (elementAlignment > 0 && offsetAlignment % elementAlignment != 0)) {
            throw new IllegalStateException("Alignment of the array field " + name +
                    " " + offsetAlignment + " must be a multiple of it's element alignment " +
                    elementAlignment +
                    " (offset alignment is " + elemModel.offsetAlignmentInBytes() +
                    ", dontCross alignment is " + elemModel.dontCrossAlignmentInBytes());
        }
        return offsetAlignment;
    }
}
