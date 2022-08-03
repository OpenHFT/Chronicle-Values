/*
 * Copyright 2016-2021 chronicle.software
 *
 *       https://chronicle.software
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
 * Thrown when the Chronicle Values library is not able to generate heap or native implementation
 * for the given interface, i. e. it is not a <i>value interface</i>. This exception thrown for
 * a valid value interface (according to
 * <a href="https://github.com/OpenHFT/Chronicle-Values#value-interface-specification">the
 * specification</a>) might also indicate a bug in the Chronicle Values library.
 */
public final class ImplGenerationFailedException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public ImplGenerationFailedException(Throwable cause) {
        super(cause);
    }
}
