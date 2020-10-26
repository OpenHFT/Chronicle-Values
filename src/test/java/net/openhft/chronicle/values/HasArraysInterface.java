/*
 *     Copyright (C) 2015-2020 chronicle.software
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
