/*
 *      Copyright (C) 2015, 2016-2020 chronicle.software
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
