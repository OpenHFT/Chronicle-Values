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
