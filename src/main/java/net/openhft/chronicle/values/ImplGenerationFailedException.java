/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.values;

/**
 * Thrown when the Chronicle Values library is unable to generate the heap or native
 * implementation for a particular interface.
 * <p>
 * Typical reasons include using an interface that does not obey the
 * <em>value interface</em> specification or the generated code failing to compile.
 * If a valid value interface triggers this exception, it is likely an internal
 * bug in Chronicle Values.
 */
public final class ImplGenerationFailedException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    /**
     * Creates a new instance wrapping the underlying cause of the generation failure.
     *
     * @param cause the compilation or runtime problem that prevented implementation generation
     */
    public ImplGenerationFailedException(Throwable cause) {
        super(cause);
    }
}
