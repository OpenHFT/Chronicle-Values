/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;

/**
 * User: peter.lawrey
 * Date: 06/10/13
 * Time: 16:59
 */
public interface MinimalInterface extends BytesMarshallable, Copyable<MinimalInterface>, Byteable {
    void flag(boolean flag);

    boolean flag();

    void byte$(byte b);

    byte byte$();

    void short$(short s);

    short short$();

    void char$(char ch);

    char char$();

    void int$(int i);

    int int$();

    void float$(float f);

    float float$();

    void long$(long l);

    long long$();

    void double$(double d);

    double double$();
}
