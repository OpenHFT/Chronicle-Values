package net.openhft.chronicle.values.issue9;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.values.MaxUtf8Length;
import net.openhft.chronicle.values.NotNull;
import net.openhft.chronicle.values.Values;
import net.openhft.chronicle.values.ValuesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class HeapVsNativeTest extends ValuesTestCommon {
    public static final String SYMBOL = "symbol";

    @Test
    public void heap() {
        Entity entity = Values.newHeapInstance(Entity.class);
        check(entity);
    }

    @Test
    public void nativeRef() {
        Entity entity = Values.newNativeReference(Entity.class);
        byte[] bytes = new byte[7];
        BytesStore bs = BytesStore.wrap(bytes);
        Byteable byteable = (Byteable) entity;
        byteable.bytesStore(bs, 0, bytes.length);
        check(entity);
    }

    private void check(Entity entity) {
        entity.setSymbol(SYMBOL);
        assertTrue(SYMBOL.contentEquals(entity.getSymbol()));
        assertNotEquals(SYMBOL, entity.getSymbol());
    }

    public interface Entity {
        CharSequence getSymbol();

        void setSymbol(@NotNull @MaxUtf8Length(6) CharSequence symbol);
    }
}
