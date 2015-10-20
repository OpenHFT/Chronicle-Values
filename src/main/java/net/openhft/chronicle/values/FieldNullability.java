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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static net.openhft.chronicle.values.Nullability.NULLABLE;

/**
 * This class exists for DRY between {@link EnumFieldModel} and {@link CharSequenceFieldModel}.
 */
final class FieldNullability {

    final FieldModel model;
    /**
     * Default is {@link Nullability#NULLABLE}, but not set, to distinguish "not set explicitly
     * case"
     */
    Nullability nullability;

    FieldNullability(FieldModel model) {
        this.model = model;
    }

    Nullability nullability() {
        return nullability != null ? nullability : NULLABLE;
    }

    public void addTemplate(Method m, MethodTemplate template) {
        Parameter annotatedParameter = template.annotatedParameter.apply(m);
        if (annotatedParameter == null)
            return;
        Nullability explicitNullability = Nullability.explicitNullability(annotatedParameter);
        if (explicitNullability != null) {
            if (nullability != null && nullability != explicitNullability) {
                throw new IllegalStateException("Conflicting explicit nullability in parameters " +
                        "of methods accessing field " + model.name);
            }
            nullability = explicitNullability;
        }
    }
}
