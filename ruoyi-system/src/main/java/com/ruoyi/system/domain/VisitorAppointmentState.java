package com.ruoyi.system.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 访客预约状态机 —— 状态定义与合法转移的唯一权威（唯一 seam）
 *
 * <p>设计词汇：本类即预约状态机 module 的 interface：所有状态判断/转移收敛于此，
 * Service 各方法不再各自手写状态合法性判断。</p>
 *
 * 状态：
 *   PENDING    待审批
 *   APPROVED   已通过
 *   REJECTED   已拒绝
 *   CANCELLED  已取消
 *   VISITING   访客中
 *   COMPLETED  已完成
 */
public final class VisitorAppointmentState {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String VISITING = "VISITING";
    public static final String COMPLETED = "COMPLETED";

    /** 状态转移事件 */
    public enum Event {
        /** 审批通过（PENDING → APPROVED） */
        APPROVE,
        /** 审批拒绝（PENDING → REJECTED） */
        REJECT,
        /** 取消预约（PENDING/APPROVED → CANCELLED） */
        CANCEL,
        /** 到访确认（PENDING/APPROVED → VISITING） */
        CHECKIN,
        /** 完成离开（VISITING/APPROVED → COMPLETED） */
        COMPLETE
    }

    /** 每个事件允许的来源状态 */
    private static final Map<Event, Set<String>> ALLOWED;

    static {
        Map<Event, Set<String>> m = new EnumMap<>(Event.class);
        m.put(Event.APPROVE, setOf(PENDING));
        m.put(Event.REJECT, setOf(PENDING));
        m.put(Event.CANCEL, setOf(PENDING, APPROVED));
        m.put(Event.CHECKIN, setOf(PENDING, APPROVED));
        m.put(Event.COMPLETE, setOf(VISITING, APPROVED));
        ALLOWED = m;
    }

    private VisitorAppointmentState() {
    }

    /**
     * 判断当前状态是否允许该事件
     */
    public static boolean can(String current, Event event) {
        Set<String> allowed = ALLOWED.get(event);
        return allowed != null && current != null && allowed.contains(current);
    }

    /**
     * 执行状态转移
     *
     * @param current 当前状态
     * @param event   事件
     * @return 目标状态
     * @throws IllegalStateException 当前状态不允许该事件时
     */
    public static String transition(String current, Event event) {
        if (!can(current, event)) {
            throw new IllegalStateException("预约状态不允许执行该操作: 当前状态=" + current + ", 事件=" + event);
        }
        switch (event) {
            case APPROVE:
                return APPROVED;
            case REJECT:
                return REJECTED;
            case CANCEL:
                return CANCELLED;
            case CHECKIN:
                return VISITING;
            case COMPLETE:
                return COMPLETED;
            default:
                throw new IllegalStateException("不支持的事件: " + event);
        }
    }

    private static Set<String> setOf(String... states) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, states);
        return Collections.unmodifiableSet(set);
    }
}
