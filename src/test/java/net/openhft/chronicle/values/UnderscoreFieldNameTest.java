package net.openhft.chronicle.values;

import org.junit.Test;

public class UnderscoreFieldNameTest {

    @Test
    public void testUnderscoreFieldName() {
        Values.heapClassFor(UnderscoreFieldNameInterface.class);
        Values.nativeClassFor(UnderscoreFieldNameInterface.class);
    }
}
