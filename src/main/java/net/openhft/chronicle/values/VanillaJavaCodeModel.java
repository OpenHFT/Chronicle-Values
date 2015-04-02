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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public class VanillaJavaCodeModel implements JavaCodeModel {
    private final String name;
    private final Class tClass;
    private final Map<String, FieldModel> fields = new LinkedHashMap<>();

    public VanillaJavaCodeModel(String name, Class tClass) {
        this.name = name;
        this.tClass = tClass;
    }

    public static <T> JavaCodeModel forName(String name, Class<T> tClass) {
        return new VanillaJavaCodeModel(name, tClass);
    }

    @Override
    public void addField(String key, FieldModel value) {
        fields.put(key, value);
    }

    @Override
    public String generateJavaCode() {
        throw new UnsupportedOperationException();
    }
}
