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
