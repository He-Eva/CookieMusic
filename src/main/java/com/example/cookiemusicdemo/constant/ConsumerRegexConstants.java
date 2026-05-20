package com.example.cookiemusicdemo.constant;

/**
 * 用户注册字段正则与开关。
 * <p>
 * 测试：{@link #STRICT_REGISTRY_ENABLED} = false（默认，仅校验非空等基础逻辑）。<br>
 * 答辩：改为 true，启用用户名/密码/手机/邮箱正则（需与前端 validate.ts 中 USE_STRICT_SIGNUP 一致）。
 */
public final class ConsumerRegexConstants {

    private ConsumerRegexConstants() {
    }

    /** 与前端 USE_STRICT_SIGNUP 保持一致 */
    public static final boolean STRICT_REGISTRY_ENABLED = true;

    /** 用户名：4-20 位字母、数字、下划线 */
    public static final String USERNAME = "^[a-zA-Z0-9_]{4,20}$";

    /** 密码：8-32 位，至少一个字母和一个数字 */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*._-]{8,32}$";

    /** 大陆手机号 11 位 */
    public static final String PHONE = "^1[3-9]\\d{9}$";

    /** 简易邮箱 */
    public static final String EMAIL = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
}
