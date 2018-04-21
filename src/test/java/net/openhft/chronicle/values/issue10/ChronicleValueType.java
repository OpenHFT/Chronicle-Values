package net.openhft.chronicle.values.issue10;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.values.Copyable;

public interface ChronicleValueType<C extends ChronicleValueType<C>>
        extends Byteable, BytesMarshallable, Copyable<C> {

    int getValue();

    void setValue(int value);
}
