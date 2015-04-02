/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.values;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Created by peter.lawrey on 03/03/2015.
 */
public interface JavaBeanInterface {
    public byte getS8();

    public void setS8(byte s8);

    public char getU16();

    public void setU16(char u16);

    public short getS16();

    public void setS16(short s16);

    public int getS32();

    public void setS32(int s32);

    public long getS64();

    public void setS64(long s64);

    public float getF32();

    public void setF32(float f32);

    public double getF64();

    public void setF64(double f64);

    public BuySell getBuySell();

    public void setBuySell(BuySell buySell);

    public String getText();

    public void setText(String text);

    public LocalDate getDate();

    public void setDate(LocalDate date);

    public LocalTime getTime();

    public void setTime(LocalTime time);

    public LocalDateTime getDateTime();

    public void setDateTime(LocalDateTime dateTime);
}
