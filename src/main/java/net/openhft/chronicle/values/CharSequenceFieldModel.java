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

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

public class CharSequenceFieldModel extends ScalarFieldModel {
    final FieldNullability nullability = new FieldNullability(this);
    MaxUtf8Length maxUtf8Length;

    @Override
    public void addTypeInfo(Method m, MethodTemplate template) {
        super.addTypeInfo(m, template);
        nullability.addTemplate(m, template);
        Parameter annotatedParameter = template.annotatedParameter.apply(m);
        if (annotatedParameter == null)
            return;
        MaxUtf8Length paramMaxUtf8Length = annotatedParameter.getAnnotation(MaxUtf8Length.class);
        if (paramMaxUtf8Length != null) {
            if (maxUtf8Length != null) {
                throw new IllegalStateException(
                        "@MaxUtf8Length should be specified only once for " + name + " field. " +
                                "Specified " + maxUtf8Length + " and " + paramMaxUtf8Length);
            }
            if (paramMaxUtf8Length.value() <= 0)
                throw new IllegalStateException(
                        paramMaxUtf8Length + " max size should be positive");
            maxUtf8Length = paramMaxUtf8Length;
        }
    }

    @Override
    int sizeInBits() {
        if (maxUtf8Length == null)
            throw new IllegalStateException("@MaxUtf8Length must be specified for a field " + name);
        int sizeInBytes = BytesUtil.stopBitLength(maxUtf8Length.value()) + maxUtf8Length.value();
        return sizeInBytes * 8;
    }

    @Override
    int offsetAlignmentInBytes() {
        if (offsetAlignment == Align.DEFAULT) {
            throw new IllegalStateException("Default offset alignment doesn't make sense for " +
                    "CharSequence field " + name);
        }
        return Math.max(offsetAlignment, 1);
    }

    private void verifyBitOffsetAligned(int bitOffset) {
        assert bitOffset % 8 == 0 : "char sequence field " + name + " should be byte-aligned";
    }

    private void lazyAddStringBuilderField(TypeSpec.Builder typeBuilder) {
        String fieldName = builderName();
        if (typeBuilder.fieldSpecs.stream().noneMatch(f -> f.name.equals(fieldName)))
            typeBuilder.addField(StringBuilder.class, fieldName, PRIVATE, FINAL);
    }

    @NotNull
    private String builderName() {
        return name + "Builder";
    }

    private final ValueMemberGenerator nativeGenerator = new ValueMemberGenerator() {
        @Override
        public void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(CharSequenceFieldModel.this);
            verifyBitOffsetAligned(bitOffset);
            int byteOffset = bitOffset / 8;
            lazyAddStringBuilderField(valueBuilder.typeBuilder);
            methodBuilder.addStatement(format("bs.readUtf8Limited(%d, %s, %d)",
                            byteOffset, builderName(), maxUtf8Length.value()));
            if (type == StringBuilder.class) {
                methodBuilder.addStatement("return bs." + builderName());
            } else {
                if (type != String.class) {
                    throw new IllegalStateException("Only StringBuilder and String char sequence " +
                            "classes are supported, " + name + " field type is " + type);
                }
                methodBuilder.addStatement(format("return bs.%s.toString()", builderName()));
            }
        }

        @Override
        public void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(CharSequenceFieldModel.this);
            verifyBitOffsetAligned(bitOffset);
            int byteOffset = bitOffset / 8;
            methodBuilder.addStatement(
                    format("bs.writeUtf8Limited(%d, %s, %d)",
                            byteOffset, name, maxUtf8Length.value()));
        }
    };

    @Override
    ValueMemberGenerator nativeGenerator() {
        return nativeGenerator;
    }
}
