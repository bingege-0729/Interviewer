package net.xdclass.enums;

import lombok.Getter;

/**
 * 智能对话意图枚举
 */
@Getter
public enum ChatIntentEnum {

    /**
     * 简历分析意图
     */
    RESUME_ANALYSE("RESUME_ANALYSE", "简历分析"),

    /**
     * AI面试意图
     */
    INTERVIEW("INTERVIEW", "AI面试"),

    /**
     * 权益查询与购买意图
     */
    BENEFIT_QUERY("BENEFIT_QUERY", "权益查询与购买"),

    /**
     * 通用对话意图
     */
    GENERAL_CHAT("GENERAL_CHAT", "通用对话");

    private final String code;
    private final String desc;

    ChatIntentEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static ChatIntentEnum of(String code) {
        for (ChatIntentEnum intent : values()) {
            if (intent.code.equals(code)) {
                return intent;
            }
        }
        return GENERAL_CHAT;
    }
}
