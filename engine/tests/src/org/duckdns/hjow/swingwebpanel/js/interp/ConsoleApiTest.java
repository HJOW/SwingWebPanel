package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the complete <code>console</code> object (WHATWG Console Standard, plan item P20-01): the formatter, <code>assert</code>,
 * <code>count</code>, groups, timers, <code>dir</code>, <code>table</code> and <code>trace</code>.
 * <p>
 * 전체 <code>console</code> 객체의 시험(WHATWG Console Standard, 계획 P20-01): 서식, <code>assert</code>, <code>count</code>, 그룹, 타이머,
 * <code>dir</code>, <code>table</code>, <code>trace</code>.
 */
public class ConsoleApiTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ConsoleApiTest() {
    }

    /**
     * Runs a script in a fresh realm and returns the leveled console lines.
     * <p>
     * 새 realm 에서 스크립트를 실행하고 수준이 붙은 콘솔 줄을 반환한다.
     *
     * @param source the script / 스크립트
     * @return the lines such as <code>log: text</code> / <code>log: text</code> 형태의 줄
     */
    private static String lines(String source) {
        Scripts.Capture c = new Scripts.Capture();
        JsRealm r = Scripts.realm(c);
        r.evaluateScript(source, "t.js");
        return c.leveled.toString();
    }

    /**
     * Every method of the Console Standard exists and is a function.
     * <p>
     * 콘솔 표준의 모든 메서드가 있고 함수이다.
     */
    public void testAllMethodsExist() {
        String names = "log info warn error debug assert clear count countReset dir dirxml group groupCollapsed groupEnd table time timeEnd timeLog trace";
        Check.equal("true", Scripts.eval("var n = '" + names + "'.split(' '); String(n.every(function (k) { return typeof console[k] === 'function'; }))"),
                "all methods");
    }

    /**
     * The formatter: <code>%s %d %i %f %o %O %c %%</code>, a missing argument, and extra arguments.
     * <p>
     * 서식: <code>%s %d %i %f %o %O %c %%</code>, 인수가 모자란 경우, 남는 인수.
     */
    public void testFormatter() {
        Check.equal("[log: a=7 b=3 c=2.5 d=x]", lines("console.log('a=%d b=%i c=%f d=%s', 7, '3.9px', '2.5em', 'x')"), "numbers and strings");
        Check.equal("[log: 100% done 1]", lines("console.log('100%% done', 1)"), "percent sign and an unused argument");
        Check.equal("[log: x NaN]", lines("console.log('x %d', {})"), "an object is NaN for %d");
        Check.equal("[log: { a: 1 } and [ 1, 2 ]]", lines("console.log('%o and %O', {a: 1}, [1, 2])"), "objects without running code");
        Check.equal("[log: styled]", lines("console.log('%cstyled', 'color: red')"), "%c consumes the style argument");
        Check.equal("[log: one %s only]", lines("console.log('%s %s only', 'one')"), "a specifier without argument stays");
        Check.equal("[log: v 1 2]", lines("console.log('v', 1, 2)"), "no specifier: plain join");
        Check.equal("[log: 5%]", lines("console.log('5%')"), "a single argument is not a format");
        Check.equal("[log: a 1 { b: 2 }]", lines("console.log('a %d', 1, {b: 2})"), "an extra object is appended");
    }

    /**
     * <code>console.assert</code> prints at the error level only when the condition is false.
     * <p>
     * <code>console.assert</code> 는 조건이 거짓일 때만 오류 수준으로 출력한다.
     */
    public void testAssert() {
        Check.equal("[]", lines("console.assert(true, 'no'); console.assert(1)"), "a true condition is silent");
        Check.equal("[error: Assertion failed]", lines("console.assert(false)"), "default text");
        Check.equal("[error: Assertion failed: x 3]", lines("console.assert(0, 'x %d', 3)"), "formatted details");
    }

    /**
     * <code>count</code> and <code>countReset</code> keep one counter per label.
     * <p>
     * <code>count</code> 와 <code>countReset</code> 은 라벨마다 카운터 하나를 둔다.
     */
    public void testCount() {
        Check.equal("[log: default: 1, log: default: 2, log: a: 1, log: default: 1]",
                lines("console.count(); console.count(); console.count('a'); console.countReset(); console.count();"), "labels and reset");
        Check.equal("[warn: Count for 'nope' does not exist]", lines("console.countReset('nope')"), "reset of a missing counter");
    }

    /**
     * Groups indent the following lines and <code>groupEnd</code> ends them; an unbalanced <code>groupEnd</code> is ignored.
     * <p>
     * 그룹은 뒤따르는 줄을 들여 쓰고 <code>groupEnd</code> 가 끝낸다. 짝이 맞지 않는 <code>groupEnd</code> 는 무시한다.
     */
    public void testGroups() {
        Check.equal("[log: outer, log:   inner, log:     deep, log: back, log: end]",
                lines("console.group('outer'); console.log('inner'); console.groupCollapsed(); console.log('deep'); console.groupEnd(); "
                        + "console.groupEnd(); console.log('back'); console.groupEnd(); console.log('end')"),
                "indentation by group depth");
        Check.equal("[log:   a\n  b]", lines("console.group(); console.log('a\\nb'); "), "lines of a multi-line message are all indented");
        Check.equal("[log: x]", lines("console.groupEnd(); console.groupEnd(); console.log('x')"), "unbalanced groupEnd");
        Check.equal("[log: g, log:   in, log: out]", lines("console.group('g'); console.log('in'); console.clear(); console.log('out')"),
                "clear resets the groups");
    }

    /**
     * The timers report the elapsed time and warn about missing or duplicate labels.
     * <p>
     * 타이머는 경과 시간을 보고하고 없거나 중복된 라벨은 경고한다.
     */
    public void testTimers() {
        String out = lines("console.time('t'); console.timeLog('t', 'extra', 1); console.timeEnd('t'); console.timeEnd('t'); console.timeLog('zz'); "
                + "console.time('d'); console.time('d');");
        Check.isTrue(out.matches("\\[log: t: [0-9.]+ms extra 1, log: t: [0-9.]+ms, warn: Timer 't' does not exist, warn: Timer 'zz' does not exist, "
                + "warn: Timer 'd' already exists\\]"), "timer output: " + out);
    }

    /**
     * <code>console.dir</code> quotes strings like the compact object form; <code>trace</code> and <code>dirxml</code> print too.
     * <p>
     * <code>console.dir</code> 은 문자열을 간결한 객체 형태처럼 따옴표로 묶는다. <code>trace</code> 와 <code>dirxml</code> 도 출력한다.
     */
    public void testDirAndTrace() {
        Check.equal("[log: { a: 'x' }, log: 'q']", lines("console.dir({a: 'x'}); console.dir('q')"), "dir");
        Check.equal("[log: 1 2]", lines("console.dirxml(1, 2)"), "dirxml");
        String t = lines("console.trace('here %d', 4)");
        Check.contains(t, "Trace: here 4", "trace text");
    }

    /**
     * <code>console.table</code> draws the index, the property columns and the <code>Values</code> column.
     * <p>
     * <code>console.table</code> 은 색인, 프로퍼티 열, <code>Values</code> 열을 그린다.
     */
    public void testTable() {
        String expected = "[log: +---------+---+---+\n| (index) | a | b |\n+---------+---+---+\n| 0       | 1 | x |\n| 1       | 2 |   |\n+---------+---+---+]";
        Check.equal(expected, lines("console.table([{a: 1, b: 'x'}, {a: 2}])"), "array of objects");
        String filtered = lines("console.table([{a: 1, b: 2}], ['b'])");
        Check.contains(filtered, "| (index) | b |", "only the listed columns");
        Check.isFalse(filtered.contains("| a |"), "the other column is left out");
        String primitives = lines("console.table(['p', 7])");
        Check.contains(primitives, "| (index) | Values |", "primitive rows use Values");
        Check.contains(primitives, "| 1       | 7      |", "a value row");
        Check.equal("[log: 5]", lines("console.table(5)"), "a primitive is logged");
    }
}
