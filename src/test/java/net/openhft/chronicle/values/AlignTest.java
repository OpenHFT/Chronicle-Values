package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.NativeBytesStore;
import org.junit.Test;

import static net.openhft.chronicle.values.Values.newNativeReference;

public class AlignTest {
    @Test
    public void testAlign() {
        DemoOrderVOInterface value = newNativeReference(DemoOrderVOInterface.class);
        long size = value.maxSize();
        NativeBytesStore<Void> bs = NativeBytesStore.nativeStore(size);
        value.bytesStore(bs, 0, size);
        value.addAtomicOrderQty(10.0);
        System.out.println(value);
        bs.release();
    }

    interface DemoOrderVOInterface extends Byteable {
        public CharSequence getSymbol();
//    public StringBuilder getUsingSymbol(StringBuilder sb);

        public void setSymbol(@MaxUtf8Length(20) CharSequence symbol);

        public double addAtomicOrderQty(double toAdd);

        public double getOrderQty();

        public void setOrderQty(double orderQty);

    }
}
