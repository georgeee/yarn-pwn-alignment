package ru.georgeee.bachelor.yarn.dict.wikt;

import lombok.Getter;

public enum WiktLang {
    RU("ru"), EN("en");
    @Getter
    private final String code;

    WiktLang(String code) {
        this.code = code;
    }

    public static WiktLang getByCode(String s) {
        for (WiktLang lang : values()) {
            if (lang.code.equals(s)) {
                return lang;
            }
        }
        return null;
    }
}
