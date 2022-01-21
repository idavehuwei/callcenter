package org.cti.cc.po;

/**
 * Create by caoliang on 2020/9/20
 * <p>
 * 坐席状态
 */
public enum AgentState {
    /**
     *
     */
    LOGOUT,

    /**
     *
     */
    LOGIN,

    /**
     * 重连
     */
    RECONNECT,

    /**
     * 空闲
     */
    READY,

    /**
     * 忙碌
     */
    NOT_READY,

    /**
     *
     */
    BUSY_OTHER,

    /**
     * 话后
     */
    AFTER,

    /**
     * sip 错误
     */
    SIP_ERROR,

    /**
     * 请求外呼
     */
    OUT_CALL,

    /**
     * 外呼振铃中
     */
    OUT_CALLER_RING,

    /**
     * 外呼被叫振铃
     */
    OUT_CALLED_RING,

    /**
     * 转接振铃
     */
    TRANSFER_CALL_RING,

    /**
     * 内呼
     */
    INNER_CALL,

    /**
     * 呼入来电振铃
     */
    IN_CALL_RING,

    /**
     * 转接来电
     */
    TRANSFER_CALL,

    /**
     * 咨询来电
     */
    CONSULT_CALL,

    /**
     * 通话中
     */
    TALKING,

    /**
     * 静音中
     */
    AUDIO_READY_MUTE,

    /**
     * 班长监控中
     */
    MONITOR,

    /**
     * 强插中
     */
    INSERT,

    /**
     * 保持中
     */
    HOLD;


}
