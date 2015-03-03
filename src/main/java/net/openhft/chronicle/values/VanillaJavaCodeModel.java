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
