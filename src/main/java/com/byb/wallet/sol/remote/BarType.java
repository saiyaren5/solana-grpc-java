package com.byb.wallet.sol.remote;

import javax.sound.midi.SysexMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public enum BarType {
    MIN1("yyyyMMddHHmm", 1, 1),
    MIN5("yyyyMMddHH", 1, 5),
    MIN15("yyyyMMddHH", 1, 15),
    MIN30("yyyyMMddHH", 1, 30),
    /** 1小时 */
    H1("yyyyMMddHH", 2, 1),
    /** 2小时 */
    H2("yyyyMMdd", 2, 2),
    /** 4小时 */
    H4("yyyyMMdd", 2, 4),
    /** 6小时 */
    H6("yyyyMMdd", 2, 6),
    /** 12小时 */
    H12("yyyyMMdd", 2, 12),
    /** 1天 */
    D1("yyyyMMdd", 3, 1);

    String format;
    int type;
    int step;

    DateTimeFormatter formatter;

    BarType(String format, int type, int step){
        formatter = DateTimeFormatter.ofPattern(format);
        this.type = type;
        this.step = step;
    }

    public long getId(long timeMillis) {
        LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.of("GMT+08:00"));

        if (step <= 1)
            return Long.parseLong(ldt.format(formatter));

        int st = 0;
        if (this.type == 1)
            st = (int) Math.floor(ldt.getMinute() / this.step) * this.step;
        else if (this.type == 2)
            st = (int) Math.floor(ldt.getHour() / this.step) * this.step;

        return Long.parseLong(ldt.format(formatter)) * 100 + st;
    }

    public static void main(String[] args) {
        System.out.println(BarType.MIN5.getId(System.currentTimeMillis()));
        System.out.println(BarType.MIN1.getId(System.currentTimeMillis()));
        System.out.println(BarType.MIN15.getId(System.currentTimeMillis()));
    }

}
