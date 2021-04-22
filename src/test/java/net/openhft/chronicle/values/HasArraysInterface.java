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

public interface HasArraysInterface {
    @Array(length = 4)
    void setFlagAt(int idx, boolean flag);

    boolean getFlagAt(int idx);

    @Array(length = 4)
    void setByteAt(int idx, byte b);

    byte getByteAt(int idx);

    @Array(length = 4)
    void setShortAt(int idx, short s);

    short getShortAt(int idx);

    @Array(length = 4)
    void setCharAt(int idx, char ch);

    char getCharAt(int idx);

    @Array(length = 4)
    void setIntAt(int idx, int i);

    int getIntAt(int idx);

    @Array(length = 4)
    void setFloatAt(int idx, float f);

    float getFloatAt(int idx);

    @Array(length = 4)
    void setLongAt(int idx, long l);

    long getLongAt(int idx);

    @Array(length = 4)
    void setDoubleAt(int idx, double d);

    double getDoubleAt(int idx);

    @Array(length = 4)
    void setStringAt(int idx, @MaxUtf8Length(8) String s);

    String getStringAt(int idx);
}
