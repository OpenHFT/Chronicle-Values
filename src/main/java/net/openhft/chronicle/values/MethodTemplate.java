//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software
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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Descriptor for a method-matching pattern used to recognise accessor
 * methods. Each instance specifies the regular expression and parameter
 * count for a method name. Once a match is found the template supplies
 * the field type, the annotated parameter and a hook to update the
 * {@link FieldModel}.
 */
class MethodTemplate {
    /** regular expression describing the accessor name */
    final String regex;
    /** required number of parameters */
    final int parameters;
    /** whether the method acts on a scalar or array field */
    final Type type;
    /** function to derive the field type from the method */
    final Function<Method, Class<?>> fieldType;
    /** parameter that carries field annotations, if any */
    final Function<Method, Parameter> annotatedParameter;
    /** consumer that applies the method to the field model */
    final BiConsumer<FieldModel, Method> addMethodToModel;

    MethodTemplate(String regex, int parameters, Type type, Function<Method, Class<?>> fieldType,
                   Function<Method, Parameter> annotatedParameter,
                   BiConsumer<FieldModel, Method> addMethodToModel) {
        this.regex = regex;
        this.parameters = parameters;
        this.type = type;
        this.fieldType = fieldType;
        this.annotatedParameter = annotatedParameter;
        this.addMethodToModel = addMethodToModel;
    }

    enum Type {
        /** Accesses a single field */
        SCALAR,
        /** Accesses an element of an array field */
        ARRAY
    }
}
