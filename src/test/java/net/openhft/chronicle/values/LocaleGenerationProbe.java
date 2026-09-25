/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;

import java.util.Locale;

/** Launched with the locale set before any Values model or generated class exists. */
public final class LocaleGenerationProbe {
    public static void main(String[] args) {
        if (!args[0].equals(Locale.getDefault().getLanguage()))
            throw new AssertionError("Requested startup locale is not active");
        System.out.println("startup-locale=" + Locale.getDefault());
        exercise(Values.newHeapInstance(PrimitiveValue.class));
        System.out.println("heap=PASS");
        PrimitiveValue value = Values.newNativeReference(PrimitiveValue.class);
        Byteable byteable = (Byteable) value;
        BytesStore<?, ?> store = BytesStore.nativeStoreWithFixedCapacity(byteable.maxSize());
        try {
            byteable.bytesStore(store, 0, byteable.maxSize());
            exercise(value);
            System.out.println("native=PASS");
        } finally {
            store.releaseLast();
        }
    }

    private static void exercise(PrimitiveValue value) {
        value.setId(-27);
        value.setPrice(1.5f);
        if (value.getId() != -27 || value.getPrice() != 1.5f)
            throw new AssertionError("Generated primitive access changed its value");
    }

    public interface PrimitiveValue {
        int getId();
        void setId(int id);
        float getPrice();
        void setPrice(float price);
    }
}
