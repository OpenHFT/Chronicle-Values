package net.openhft.chronicle.values;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
class MethodKey {
    final String regex;
    final int arguments;

    MethodKey(String regex, int arguments) {
        this.regex = regex;
        this.arguments = arguments;
    }
}
