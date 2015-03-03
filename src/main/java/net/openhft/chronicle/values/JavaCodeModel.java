package net.openhft.chronicle.values;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public interface JavaCodeModel {
    void addField(String key, FieldModel value);

    String generateJavaCode();
}
