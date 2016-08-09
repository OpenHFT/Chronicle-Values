package net.openhft.chronicle.values.issue10;

import net.openhft.chronicle.values.Values;
import org.junit.Test;

public class ChronicleValueTypeTest {

    @Test
    public void testChronicleValueDate() {
        Values.heapClassFor(ChronicleValueDate.class);
        Values.nativeClassFor(ChronicleValueDate.class);
    }
}
