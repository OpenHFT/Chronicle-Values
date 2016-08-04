/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

class MethodTemplate {
    final String regex;
    final int parameters;
    final Type type;
    final Function<Method, Class> fieldType;
    final Function<Method, Parameter> annotatedParameter;
    final BiConsumer<FieldModel, Method> addMethodToModel;

    MethodTemplate(String regex, int parameters, Type type, Function<Method, Class> fieldType,
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
        SCALAR, ARRAY
    }
}
