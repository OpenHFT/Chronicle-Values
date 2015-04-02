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

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public class VanillaFieldModel implements FieldModel {
    final String name;
    private int groupId = 0;
    private Method getter, setter;

    public VanillaFieldModel(String name) {
        this.name = name;
    }

    @Override
    public int getGroupId() {
        return groupId;
    }

    @Override
    public int byteAlignment() {
        return 0;
    }

    @Override
    public void addTemplate(Method m, MethodKey key, MethodTemplates value) {
        GroupId groupId = m.getAnnotation(GroupId.class);
        if (groupId != null)
            this.groupId = groupId.value();
    }

    @Override
    public void setGetter(Method method) {
        this.getter = method;
    }

    public Method getSetter() {
        return setter;
    }

    @Override
    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public Method getGetter() {
        return getter;
    }
}
