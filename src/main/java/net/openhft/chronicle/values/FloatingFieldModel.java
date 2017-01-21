/*
 *      Copyright (C) 2015, 2016  higherfrequencytrading.com
 *      Copyright (C) 2016 Roman Leventov
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
import sun.misc.Unsafe;

import static java.lang.String.format;
import static net.openhft.chronicle.values.Primitives.boxed;

class FloatingFieldModel extends PrimitiveFieldModel {

    private final MemberGenerator nativeGenerator = new MemberGenerator(FloatingFieldModel.this) {

        @Override
        void generateArrayElementFields(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder) {
            // no fields
        }

        private int verifiedByteOffset(ValueBuilder valueBuilder) {
            int bitOffset = valueBuilder.model.fieldBitOffset(FloatingFieldModel.this);
            assert bitOffset % 8 == 0;
            return bitOffset / 8;
        }

        private String scaledIndex() {
            if (type == float.class) {
                return "(4L * index)";
            } else if (type == double.class) {
                return "(8L * index)";
            } else {
                throw new AssertionError();
            }
        }

        private void gen(
                ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder,
                String ret, String method, String arguments) {
            methodBuilder.addStatement("$Nbs.$N$N(offset + $L$N)",
                    ret, method, capTypeName(), verifiedByteOffset(valueBuilder), arguments);
        }

        private void genArrayElement(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder, String ret, String method, String arguments) {
            methodBuilder.addStatement("$Nbs.$N$N(offset + $L + $N$N)",
                    ret, method, capTypeName(), arrayFieldModel.verifiedByteOffset(valueBuilder),
                    scaledIndex(), arguments);
        }

        @Override
        void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder, "return ", "read", "");
        }

        @Override
        void generateArrayElementGet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder, "return ", "read", "");
        }

        @Override
        void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder, "return ", "readVolatile", "");
        }

        @Override
        void generateArrayElementGetVolatile(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "return ", "readVolatile", "");
        }

        @Override
        void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder, "", "write", ", " + varName());
        }

        @Override
        void generateArrayElementSet(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "", "write", ", " + varName());
        }

        @Override
        void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder, "", "writeVolatile", ", " + varName());
        }

        @Override
        void generateArrayElementSetVolatile(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "", "writeVolatile", ", " + varName());
        }

        @Override
        void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder, "", "writeOrdered", ", " + varName());
        }

        @Override
        void generateArrayElementSetOrdered(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "", "writeOrdered", ", " + varName());
        }

        @Override
        void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addStatement("return bs.addAndGet$NNotAtomic(offset + $L, addition)",
                    capTypeName(), verifiedByteOffset(valueBuilder));
        }

        @Override
        void generateArrayElementAdd(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            methodBuilder.addStatement(
                    "return bs.writeAndGet$NNotAtomic(offset + $L + $N, addition)",
                    capTypeName(), arrayFieldModel.verifiedByteOffset(valueBuilder), scaledIndex());
        }

        @Override
        void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder, "return ", "addAndGet", ", addition");
        }

        @Override
        void generateArrayElementAddAtomic(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "return ", "addAndGet", ", addition");
        }

        @Override
        void generateCompareAndSwap(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder,
                    "return ", "compareAndSwap", format(", %s, %s", oldName(), newName()));
        }

        @Override
        void generateArrayElementCompareAndSwap(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            arrayFieldModel.checkBounds(methodBuilder);
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "return ", "compareAndSwap", format(", %s, %s", oldName(), newName()));
        }

        @Override
        void generateCopyFrom(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder,
                    "", "write", format(", from.%s()", getOrGetVolatile().getName()));
        }

        @Override
        void generateArrayElementCopyFrom(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "", "write", format(", from.%s(index)",
                            arrayFieldModel.getOrGetVolatile().getName()));
        }

        @Override
        void generateReadMarshallable(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            gen(valueBuilder, methodBuilder,
                    "", "write", format(", bytes.read%s()", capTypeName()));
        }

        @Override
        void generateArrayElementReadMarshallable(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            genArrayElement(arrayFieldModel, valueBuilder, methodBuilder,
                    "", "write", format(", bytes.read%s()", capTypeName()));
        }

        @Override
        void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            methodBuilder.addCode("if ($N($N()) != $N(other.$N())) return false;\n",
                    toBits(), getOrGetVolatile().getName(), toBits(), getOrGetVolatile().getName());
        }

        @Override
        void generateArrayElementEquals(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            String get = arrayFieldModel.getOrGetVolatile().getName();
            methodBuilder.addCode("if ($N($N(index)) != $N(other.$N(index))) return false;\n",
                    toBits(), get, toBits(), get);
        }

        @Override
        String generateHashCode(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
            return String.format("%s.hashCode(%s())",
                    boxed(type).getName(), getOrGetVolatile().getName());
        }

        @Override
        String generateArrayElementHashCode(
                ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                MethodSpec.Builder methodBuilder) {
            return String.format("%s.hashCode(%s(index))",
                    boxed(type).getName(), arrayFieldModel.getOrGetVolatile().getName());
        }
    };

    @Override
    MemberGenerator nativeGenerator() {
        return nativeGenerator;
    }

    @Override
    MemberGenerator createHeapGenerator() {
        return new NumberHeapMemberGenerator(this) {

            @Override
            public void generateAddAtomic(
                    ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.beginControlFlow("while (true)");
                methodBuilder.addStatement(
                        "$T $N = " + wrap(valueBuilder, methodBuilder, "$N.$N(this, $N)"),
                        type, oldName(),
                        valueBuilder.unsafe(), getVolatile(), fieldOffset(valueBuilder));
                methodBuilder.addStatement("$T $N = $N + addition", type, newName(), oldName());
                methodBuilder.beginControlFlow(
                        format("if ($N.$N(this, $N, %s, %s))",
                                unwrap(methodBuilder, "$N"), unwrap(methodBuilder, "$N")),
                        valueBuilder.unsafe(), compareAndSwap(), fieldOffset(valueBuilder),
                        oldName(), newName());
                methodBuilder.addStatement("return $N", newName());
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
            }

            @Override
            public void generateArrayElementAddAtomic(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                arrayFieldModel.checkBounds(methodBuilder);
                methodBuilder.beginControlFlow("while (true)");
                methodBuilder.addStatement(
                        "$T $N = " + wrap(valueBuilder, methodBuilder,
                                "$N.$N($N, (long) $T.$N + (index * (long) $T.$N))"),
                        type, oldName(),
                        valueBuilder.unsafe(), getVolatile(), field, Unsafe.class, arrayBase(),
                        Unsafe.class, arrayScale());
                methodBuilder.addStatement("$T $N = $N + addition", type, newName(), oldName());
                methodBuilder.beginControlFlow(
                        format("if ($N.$N($N, (long) $T.$N + (index * (long) $T.$N), %s, %s))",
                                unwrap(methodBuilder, "$N"), unwrap(methodBuilder, "$N")),
                        valueBuilder.unsafe(), compareAndSwap(), field, Unsafe.class, arrayBase(),
                        Unsafe.class, arrayScale(), oldName(), newName());
                methodBuilder.addStatement("return $N", newName());
                methodBuilder.endControlFlow();
                methodBuilder.endControlFlow();
            }

            @Override
            void generateEquals(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
                methodBuilder.addCode(
                        format("if ($N(%s) != $N(other.$N())) return false;\n",
                                wrap(valueBuilder, methodBuilder, "$N")),
                        toBits(), field, toBits(), getOrGetVolatile().getName());
            }

            @Override
            void generateArrayElementEquals(
                    ArrayFieldModel arrayFieldModel, ValueBuilder valueBuilder,
                    MethodSpec.Builder methodBuilder) {
                methodBuilder.addCode(
                        format("if ($N(%s) != $N(other.$N(index))) return false;\n",
                                wrap(valueBuilder, methodBuilder, "$N[index]")),
                        toBits(), field, toBits(), arrayFieldModel.getOrGetVolatile().getName());
            }
        };
    }

    private String toBits() {
        if (type == float.class) {
            return "java.lang.Float.floatToIntBits";
        } else {
            return "java.lang.Double.doubleToLongBits";
        }
    }
}
