package com.byb.wallet.sol.remote;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.math.BigDecimal;

public class Bar {
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BarType getBarType() {
        return barType;
    }

    public void setBarType(BarType barType) {
        this.barType = barType;
    }

    private BigDecimal close;
    private BarType barType;

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();

        return super.toString();
    }
}
