/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
