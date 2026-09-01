package com.baixingai.voicedrop.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.baixingai.voicedrop.core.Prices;
import com.baixingai.voicedrop.net.Api;
import com.baixingai.voicedrop.net.HttpClient;

/**
 * 价目表的本地缓存：一天从服务端拉一次，其余时候读 prefs。
 *
 * <p>{@link #book()} 是同步的纯本地读取，页面拿它立刻渲染；{@link #refreshIfNeeded}
 * 会打网络，必须在 io 线程调用。拉不到就沿用旧值/兜底——展示价缺席不该影响任何流程，
 * 扣费真源永远在服务端（402 带权威的 need_suanli）。
 */
public final class PriceStore {
    public static final String PREFS = "voicedrop.prices";
    private static final String KEY_BOOK = "book";
    private static final String KEY_REVISE = "book_revise";
    private static final String KEY_FETCHED_AT = "fetched_at";

    private final Context context;
    private final HttpClient http;

    public PriceStore(Context context, HttpClient http) {
        this.context = context.getApplicationContext();
        this.http = http;
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 当前展示价（写一本书）。同步、永不失败。 */
    public double book() {
        return Prices.sane(prefs().getFloat(KEY_BOOK, 0f), Prices.FALLBACK_BOOK);
    }

    /** 当前展示价（修一轮）。 */
    public double revise() {
        return Prices.sane(prefs().getFloat(KEY_REVISE, 0f), Prices.FALLBACK_REVISE);
    }

    /**
     * 过期了才真的发请求。返回拉完之后的写书价（没拉也返回现价）。
     * 会阻塞——调用方负责放在 io 线程上。
     */
    public double refreshIfNeeded(long now) {
        SharedPreferences prefs = prefs();
        if (!Prices.isStale(prefs.getLong(KEY_FETCHED_AT, 0L), now)) return book();
        try {
            HttpClient.Response response = http.get(Api.agentBase() + "/usage/prices", "");
            if (response == null || !response.ok()) return book();
            String body = response.text();
            Double remoteBook = Prices.parseBook(body);
            if (remoteBook == null) return book();
            Double remoteRevise = Prices.parseRevise(body);
            SharedPreferences.Editor editor = prefs.edit()
                    .putFloat(KEY_BOOK, remoteBook.floatValue())
                    .putLong(KEY_FETCHED_AT, now);
            if (remoteRevise != null) editor.putFloat(KEY_REVISE, remoteRevise.floatValue());
            editor.apply();
            return remoteBook;
        } catch (Exception ignored) {
            return book();
        }
    }

    public double refreshIfNeeded() {
        return refreshIfNeeded(System.currentTimeMillis());
    }
}
