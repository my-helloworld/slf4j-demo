package com.chpengzh.slf4j;

import java.util.function.Supplier;

public class LogEvent {

    private Level level;

    private Supplier<String> msgSupplier;

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Supplier<String> getMsgSupplier() {
        return msgSupplier;
    }

    public void setMsgSupplier(Supplier<String> msgSupplier) {
        this.msgSupplier = msgSupplier;
    }

    @Override
    public String toString() {
        return msgSupplier.get();
    }

}
