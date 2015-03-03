package net.openhft.chronicle.values;

import java.lang.reflect.Method;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public interface FieldModel {
    int getGroupId();

    int byteAlignment();

    void addTemplate(Method m, MethodKey key, MethodTemplates value);

    void setGetter(Method method);

    void setSetter(Method method);
}
