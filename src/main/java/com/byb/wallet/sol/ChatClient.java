package com.byb.wallet.sol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class ChatClient implements Serializable {
    private static final long serialVersionUID = 1L;

//    @JsonProperty("name")
    private String name;

//    @JsonProperty("message")
    private String message;

    // 必须有一个无参构造函数
    public ChatClient() {
    }

    @Override
    public String toString() {
        return this.name+"==>"+this.message;
    }

    // 带有参数的构造函数
    public ChatClient(String name, String message) {
        this.name = name;
        this.message = message;
    }

    // Getter 和 Setter 方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}