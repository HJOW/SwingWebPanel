package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the periods of <code>font-display</code> (CSS Fonts Level 4 section 4.8): <code>auto</code>, <code>block</code> and <code>swap</code> have an infinite swap period
 * (a font is never too late), <code>fallback</code> has a short block period of {@link WebFontSet#SHORT_BLOCK_MS} and a short swap period of
 * {@link WebFontSet#FALLBACK_SWAP_MS}, <code>optional</code> has the short block period and no swap period. A font whose arrival is not known (0, or no times) is not late.
 * <p>
 * <code>font-display</code> 의 기간 시험(CSS Fonts Level 4 4.8 절): <code>auto</code>, <code>block</code>, <code>swap</code> 은 swap 기간이 무한하여 글꼴이 너무 늦는 일이 없고,
 * <code>fallback</code> 은 {@link WebFontSet#SHORT_BLOCK_MS} 의 짧은 block 기간과 {@link WebFontSet#FALLBACK_SWAP_MS} 의 짧은 swap 기간을, <code>optional</code> 은 짧은 block 기간과
 * swap 기간 없음을 가진다. 도착 시각을 모르는(0 이거나 시각 없음) 글꼴은 늦지 않다.
 */
public class FontDisplayLateTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FontDisplayLateTest() {
    }

    /**
     * Returns the times of a request that took some milliseconds.
     * <p>
     * 몇 밀리초 걸린 요청의 시각을 반환한다.
     *
     * @param ms the duration / 걸린 시간
     * @return {start, arrival} / {시작, 도착}
     */
    private static long[] took(long ms) {
        return new long[] { 1000, 1000 + ms };
    }

    /**
     * An infinite swap period never makes a font late.
     * <p>
     * 무한한 swap 기간은 글꼴을 늦게 만들지 않는다.
     */
    public void testInfiniteSwapPeriods() {
        String[] values = { "auto", "block", "swap", "bogus" };
        // 값마다 아주 오래 걸린 글꼴도 늦지 않다.
        for (int i = 0; i < values.length; i++) {
            Check.isFalse(WebFontSet.late(values[i], took(600000)), values[i] + " has an infinite swap period");
        }
    }

    /**
     * <code>optional</code>: only the short block period.
     * <p>
     * <code>optional</code>: 짧은 block 기간만 있다.
     */
    public void testOptional() {
        Check.isFalse(WebFontSet.late("optional", took(0)), "at once");
        Check.isFalse(WebFontSet.late("optional", took(WebFontSet.SHORT_BLOCK_MS)), "exactly at the end of the block period");
        Check.isTrue(WebFontSet.late("optional", took(WebFontSet.SHORT_BLOCK_MS + 1)), "after the block period");
    }

    /**
     * <code>fallback</code>: the short block period and the short swap period.
     * <p>
     * <code>fallback</code>: 짧은 block 기간과 짧은 swap 기간이 있다.
     */
    public void testFallback() {
        Check.isFalse(WebFontSet.late("fallback", took(500)), "within the swap period");
        Check.isFalse(WebFontSet.late("fallback", took(WebFontSet.SHORT_BLOCK_MS + WebFontSet.FALLBACK_SWAP_MS)), "exactly at its end");
        Check.isTrue(WebFontSet.late("fallback", took(WebFontSet.SHORT_BLOCK_MS + WebFontSet.FALLBACK_SWAP_MS + 1)), "after the swap period");
    }

    /**
     * Unknown times are never late.
     * <p>
     * 모르는 시각은 늦지 않다.
     */
    public void testUnknownTimes() {
        Check.isFalse(WebFontSet.late("optional", null), "no times");
        Check.isFalse(WebFontSet.late("optional", new long[] { 1000, 0 }), "not arrived yet");
        Check.isFalse(WebFontSet.late("optional", new long[] { 1000 }), "a short array");
    }
}
