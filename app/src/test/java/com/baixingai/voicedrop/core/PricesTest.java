package com.baixingai.voicedrop.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 价目表的纯逻辑：什么时候该去拉、远端 JSON 怎么收、脏数据怎么挡。
 *
 * <p>立场：展示价晚一天无所谓（扣费真源在服务端，402 带权威的 need_suanli），但
 * 绝不能因为拿不到价钱就显示 0——所以每条失败路径都得退回旧值/兜底。
 */
public final class PricesTest {

    @Test
    public void fallbackMatchesServerPrice() {
        // 和服务端 usage.js 的 BOOK_SUANLI 对齐；改价时这里跟着改一次。
        assertEquals(160, Prices.FALLBACK_BOOK, 0.0001);
        assertEquals(40, Prices.FALLBACK_REVISE, 0.0001);
    }

    @Test
    public void neverFetchedIsAlwaysStale() {
        assertTrue(Prices.isStale(0, 0));
        assertTrue(Prices.isStale(0, 999_999_999L));
        assertTrue(Prices.isStale(-1, 1000));
    }

    @Test
    public void freshWithinOneDayThenStale() {
        long fetched = 1_000_000L;
        assertFalse(Prices.isStale(fetched, fetched));
        assertFalse(Prices.isStale(fetched, fetched + Prices.TTL_MS));
        assertTrue(Prices.isStale(fetched, fetched + Prices.TTL_MS + 1));
    }

    @Test
    public void parsesServerPrices() {
        String json = "{\"book\":160,\"book_revise\":40,\"image\":1.8}";
        assertEquals(160, Prices.parseBook(json), 0.0001);
        assertEquals(40, Prices.parseRevise(json), 0.0001);
    }

    @Test
    public void rejectsGarbageSoCallerKeepsTheOldValue() {
        assertNull(Prices.parseBook(null));
        assertNull(Prices.parseBook(""));
        assertNull(Prices.parseBook("not json"));
        assertNull(Prices.parseBook("{}"));
        assertNull(Prices.parseBook("{\"book\":0}"));
        assertNull(Prices.parseBook("{\"book\":-5}"));
    }

    @Test
    public void saneGuardsStoredGarbage() {
        assertEquals(160, Prices.sane(0, Prices.FALLBACK_BOOK), 0.0001);
        assertEquals(160, Prices.sane(-1, Prices.FALLBACK_BOOK), 0.0001);
        assertEquals(160, Prices.sane(Double.NaN, Prices.FALLBACK_BOOK), 0.0001);
        assertEquals(123, Prices.sane(123, Prices.FALLBACK_BOOK), 0.0001);
    }
}
