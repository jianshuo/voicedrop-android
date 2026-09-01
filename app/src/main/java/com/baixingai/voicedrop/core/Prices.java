package com.baixingai.voicedrop.core;

import org.json.JSONObject;

/**
 * 价目表的纯逻辑：兜底价、过期判断、远端 JSON 的收货校验。不碰磁盘也不碰网络，
 * 那两件事在 data/PriceStore 里。
 *
 * <p>价钱真源在服务端（agent/src/usage.js 的 BOOK_SUANLI），扣费也只发生在那边
 * （402 会带权威的 need_suanli）。这里管的只是展示价——一天拉一次就够，拿不到就
 * 用 FALLBACK，绝不因为拉不到价钱而显示 0 或挡住流程。
 */
public final class Prices {
    /** 和服务端 BOOK_SUANLI 对齐；改价时顺手改一次，忘了也只是显示的价钱旧一天。 */
    public static final double FALLBACK_BOOK = 160;
    public static final double FALLBACK_REVISE = 40;
    public static final long TTL_MS = 24L * 60 * 60 * 1000;

    private Prices() {}

    /** 没拉过、或拉过但超过一天 → 该去拉了。 */
    public static boolean isStale(long fetchedAt, long now) {
        if (fetchedAt <= 0) return true;
        return now - fetchedAt > TTL_MS;
    }

    /**
     * 远端 /agent/usage/prices 的 JSON → 写书价。脏数据（解析失败、缺字段、0、负数、
     * NaN）一律返回 null，调用方沿用旧值。
     */
    public static Double parseBook(String json) {
        return parseField(json, "book");
    }

    public static Double parseRevise(String json) {
        return parseField(json, "book_revise");
    }

    private static Double parseField(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        try {
            double value = new JSONObject(json).optDouble(key, Double.NaN);
            if (!Double.isFinite(value) || value <= 0) return null;
            return value;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 存下来的价钱可能是脏的（旧版本写的 0、被改过的 prefs）——一样按没有处理。 */
    public static double sane(double cached, double fallback) {
        return Double.isFinite(cached) && cached > 0 ? cached : fallback;
    }
}
