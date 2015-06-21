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
    byte getS8();

    void setS8(byte s8);

    char getU16();

    void setU16(char u16);

    short getS16();

    void setS16(short s16);

    int getS32();

    void setS32(int s32);

    long getS64();

    void setS64(long s64);

    float getF32();

    void setF32(float f32);

    double getF64();

    void setF64(double f64);

    BuySell getBuySell();

    void setBuySell(BuySell buySell);

    String getText();

    void setText(String text);

    LocalDate getDate();

    void setDate(LocalDate date);

    LocalTime getTime();

    void setTime(LocalTime time);

    LocalDateTime getDateTime();

    void setDateTime(LocalDateTime dateTime);
}
