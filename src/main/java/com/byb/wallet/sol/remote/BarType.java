package com.byb.wallet.sol.remote;

public enum BarType {
    MIN1("3", 0),
    MIN5("2", 5),
    MIN15("1", 15);

    String format;
    int type;

    BarType(String format, int type){
        this.format = format;
        this.type = type;
    }

}
