package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.ExecutionLimits;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.NumberText;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.parser.Parser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the connections between the engine core and the optional library packages: exact number text through the number
 * library, the <code>s</code> regular expression flag, the restricted function properties, and the cancellation API that
 * long running built-ins (regular expression matching) use.
 * <p>
 * 엔진 코어와 선택적 라이브러리 패키지의 연결 시험: 숫자 라이브러리를 통한 정확한 숫자 문자열, 정규식 <code>s</code> 플래그, 제한된
 * 함수 프로퍼티, 오래 걸리는 내장 객체(정규식 일치)가 쓰는 취소 API.
 */
public class CoreIntegrationTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public CoreIntegrationTest() {
    }

    /**
     * Number to string conversion is exact: shortest round-trip digits and the extreme values.
     * <p>
     * 숫자-문자열 변환은 정확하다: 가장 짧은 왕복 자릿수와 극단 값.
     */
    public void testNumberTextIsExact() {
        Check.equal("5e-324", NumberText.toString(Double.MIN_VALUE), "Number.MIN_VALUE");
        Check.equal("1.7976931348623157e+308", NumberText.toString(Double.MAX_VALUE), "Number.MAX_VALUE");
        Check.equal("0.1", NumberText.toString(0.1), "0.1");
        Check.equal("0.30000000000000004", NumberText.toString(0.1 + 0.2), "0.1 + 0.2");
        Check.equal("100", NumberText.toString(100.0), "integer");
        Check.equal("1e+21", NumberText.toString(1e21), "exponent form");
        Check.equal("123456789012345680000", NumberText.toString(123456789012345680000.0), "large integer below 1e21");
        Check.equal("4.35", NumberText.toString(4.35), "4.35");
        Check.equal("ff", NumberText.toString(255.0, 16), "radix 16");
        Check.equal("-101", NumberText.toString(-5.0, 2), "radix 2");
        Check.equal("5e-324", Ops.toString(minValue()), "through Ops.toString");
        Check.equal(Double.MIN_VALUE, NumberText.parse("5e-324"), "parse the smallest double");
        Check.equal(0.1, NumberText.parse("  0.1  "), "parse with spaces");
        Check.equal(255.0, NumberText.parse("0xff"), "parse hex");
        Check.isTrue(Double.isNaN(NumberText.parse("1d")), "Java-only forms are NaN");
        Check.equal(0.0, NumberText.parse("  "), "blank is zero");
    }

    /**
     * Returns the smallest positive double as a script number value.
     * <p>
     * 가장 작은 양의 double 을 스크립트 숫자 값으로 반환한다.
     *
     * @return the value / 값
     */
    private static org.duckdns.hjow.swingwebpanel.js.JsValue minValue() {
        return org.duckdns.hjow.swingwebpanel.js.Js.num(Double.MIN_VALUE);
    }

    /**
     * The lexer accepts the flags g, i, m, s, u and y once each and rejects unknown or repeated flags.
     * <p>
     * 렉서는 플래그 g, i, m, s, u, y 를 한 번씩 받아들이고 알 수 없거나 반복된 플래그는 거부한다.
     */
    public void testRegExpFlagsInLiterals() {
        String[] good = { "/a/g", "/a/i", "/a/m", "/a/s", "/a/u", "/a/y", "/a/gimsuy", "/a/sg" };
        // 허용되는 플래그 조합이 파싱되는지 확인한다.
        for (int i = 0; i < good.length; i++) {
            Check.notNull(Parser.parseScript(good[i] + ";", "t.js"), "parses " + good[i]);
        }
        String[] bad = { "/a/gg", "/a/ss", "/a/x", "/a/gx", "/a/d" };
        // 거부되어야 하는 플래그를 확인한다.
        for (int i = 0; i < bad.length; i++) {
            final String src = bad[i] + ";";
            Check.throwsException(org.duckdns.hjow.swingwebpanel.js.JsException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                public void run() {
                    Parser.parseScript(src, "t.js");
                }
            }, "rejects " + bad[i]);
        }
    }

    /**
     * Function.prototype has the restricted caller and arguments accessors (AddRestrictedFunctionProperties).
     * <p>
     * Function.prototype 은 제한된 caller 와 arguments 접근자를 가진다(AddRestrictedFunctionProperties).
     */
    public void testRestrictedFunctionProperties() {
        Scripts.checkInline("var d = Object.getOwnPropertyDescriptor(Function.prototype, 'caller');"
                + "t('caller accessor', typeof d.get + typeof d.set + d.enumerable + d.configurable, 'functionfunctionfalsetrue');"
                + "t('getter is the ThrowTypeError function', d.get === Object.getOwnPropertyDescriptor(Function.prototype, 'arguments').get, true);"
                + "terr('strict function caller', function () { 'use strict'; return (function () { }).caller; }, 'TypeError');"
                + "terr('arrow arguments property', function () { return (() => 1).arguments; }, 'TypeError');"
                + "t('sloppy function has no own caller', (function () { }).hasOwnProperty('caller'), false);", 5);
    }

    /**
     * The non-throwing and throwing cancellation queries used by long running native code follow cancel() and the time limit.
     * <p>
     * 오래 걸리는 네이티브 코드가 쓰는 비예외/예외 취소 조회가 cancel() 과 시간 제한을 따른다.
     */
    public void testCancellationQueries() throws Exception {
        ExecutionLimits l = new ExecutionLimits();
        Check.isFalse(l.isAbortRequested(), "nothing requested");
        l.poll();
        l.cancel();
        Check.isTrue(l.isAbortRequested(), "cancelled");
        try {
            l.poll();
            Check.fail("poll must throw after cancel");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.CANCELLED, e.getReason(), "cancel reason");
        }
        l.reset();
        Check.isFalse(l.isAbortRequested(), "cleared");
        l.setMaxTimeMillis(30L);
        l.beginTask();
        Thread.sleep(80L);
        Check.isTrue(l.isAbortRequested(), "time limit exceeded");
        try {
            l.poll();
            Check.fail("poll must throw after the time limit");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.TIME_LIMIT, e.getReason(), "time limit reason");
        }
        l.endTask();
    }

    /**
     * When the regular expression library is installed, a long running match stops on cancellation and on the time limit.
     * <p>
     * 정규식 라이브러리가 설치된 경우, 오래 걸리는 일치는 취소와 시간 제한에서 멈춘다.
     */
    public void testRegExpMatchingIsCancellable() {
        JsRealm probe = Scripts.realm(new Scripts.Capture());
        if (!"function".equals(Ops.toString(probe.evaluateScript("typeof RegExp", "p.js")))) {
            Check.skip("the regular expression library is not part of this build");
        }
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.getLimits().setMaxSteps(0);
        r.getLimits().setMaxTimeMillis(300L);
        String catastrophic = "var s = ''; for (var i = 0; i < 40; i++) s += 'a'; s += 'b'; /^(a+)+$/.test(s);";
        long start = System.currentTimeMillis();
        try {
            r.evaluateScript(catastrophic, "t.js");
            Check.fail("a catastrophic backtracking match must be stopped");
        } catch (ScriptAbortedException e) {
            Check.equal(ScriptAbortedException.Reason.CANCELLED, e.getReason(), "the regexp engine reports a cancelled match");
        } catch (org.duckdns.hjow.swingwebpanel.js.JsException e) {
            Check.contains(e.getMessage(), "RangeError", "or the engine's own backtracking limit: " + e.getMessage());
        }
        Check.isTrue(System.currentTimeMillis() - start < 20000L, "stopped in reasonable time");
        r.getLimits().setMaxTimeMillis(0L);
        Check.equal("true", Ops.toString(r.evaluateScript("/^a+$/.test('aaa')", "u.js")), "the realm still matches afterwards");
    }
}
