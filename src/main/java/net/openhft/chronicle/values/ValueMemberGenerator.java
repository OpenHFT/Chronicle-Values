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

interface ValueMemberGenerator {

    default void generateFields(ValueBuilder valueBuilder) {
        // do nothing by default
    }

    default void generateGet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateGetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateGetUsing(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateSet(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateSetVolatile(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateSetOrdered(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateAdd(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateAddAtomic(ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }

    default void generateCompareAndSwap(
            ValueBuilder valueBuilder, MethodSpec.Builder methodBuilder) {
        throw new UnsupportedOperationException();
    }
}
