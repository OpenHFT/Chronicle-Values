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
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
class MethodTemplates {
    final Function<Method, Class> fieldType;
    final BiConsumer<Method, FieldModel> templateExtractor;
    final BiConsumer<FieldModel, JavaCodeModel> javaCodeGenerator;
    final BiConsumer<FieldModel, ByteCodeModel> byteCodeGenerator;

    MethodTemplates(Function<Method, Class> fieldType,
                    BiConsumer<Method, FieldModel> templateExtractor,
                    BiConsumer<FieldModel, JavaCodeModel> javaCodeGenerator,
                    BiConsumer<FieldModel, ByteCodeModel> byteCodeGenerator) {
        this.fieldType = fieldType;
        this.templateExtractor = templateExtractor;
        this.javaCodeGenerator = javaCodeGenerator;
        this.byteCodeGenerator = byteCodeGenerator;
    }
}
