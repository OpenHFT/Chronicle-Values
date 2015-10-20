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

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

enum Nullability {
    NULLABLE, NOT_NULL;

    static Nullability explicitNullability(Parameter p) {
        boolean hasNotNullAnnotation = hasNotNullAnnotation(p);
        boolean hasNullableAnnotation = hasNullableAnnotation(p);
        if (hasNotNullAnnotation && hasNullableAnnotation) {
            throw new IllegalStateException("Param " + p +
                    " has both @Nullable and @NotNull annotations");
        }
        if (hasNotNullAnnotation)
            return NOT_NULL;
        if (hasNullableAnnotation)
            return NULLABLE;
        return null;
    }

    static boolean hasNullableAnnotation(Parameter p) {
        for (Annotation a : p.getAnnotations()) {
            if (a.annotationType().getSimpleName().equalsIgnoreCase("Nullable"))
                return true;
        }
        return false;
    }

    static boolean hasNotNullAnnotation(Parameter p) {
        for (Annotation a : p.getAnnotations()) {
            String annotationName = a.annotationType().getSimpleName();
            if (annotationName.equalsIgnoreCase("NotNull") ||
                    annotationName.equalsIgnoreCase("Nonnull"))
                return true;
        }
        return false;
    }
}
