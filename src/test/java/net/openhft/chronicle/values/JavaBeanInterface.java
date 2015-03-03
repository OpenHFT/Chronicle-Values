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
