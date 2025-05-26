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
 * Second level nested interface used in tests.
 * <p>
 * Holds {@code bid} and {@code ask} values which act as simple price fields.
 * They are written to and read from nested structures so tests can verify
 * floating point values are preserved when the objects are encoded.
 */
public interface NestedB {
    void bid(double bid);

    double bid();

    void ask(double ask);

    double ask();
}
