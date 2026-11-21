package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecord;
import org.duckdns.hjow.swingwebpanel.js.ConsoleRecordHook;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the structured console output of the engine (plan item P24-03): a {@link ConsoleRecordHook} gets the method, the level, the
 * texts of the arguments, the group depth and the place of the call for every console method, while a plain
 * {@link org.duckdns.hjow.swingwebpanel.js.ConsoleHook} keeps getting the level and the text it always got.
 * <p>
 * 엔진의 구조화된 콘솔 출력 시험(계획 P24-03): {@link ConsoleRecordHook} 은 모든 콘솔 메서드의 메서드 이름, 수준, 인수 문자열, 그룹 단계, 호출 위치를
 * 받고, 일반 {@link org.duckdns.hjow.swingwebpanel.js.ConsoleHook} 은 지금까지와 같은 수준과 문자열을 계속 받는다.
 */
public class ConsoleRecordTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ConsoleRecordTest() {
    }

    /**
     * A hook that keeps the records, and what the plain method got.
     * <p>
     * 기록과 일반 메서드가 받은 것을 보관하는 연결.
     */
    private static final class Records implements ConsoleRecordHook {

        /** The records. / 기록. */
        final List<ConsoleRecord> records = new ArrayList<ConsoleRecord>();

        /** The calls of the plain method. / 일반 메서드의 호출. */
        final List<String> plain = new ArrayList<String>();

        /**
         * Keeps a record.
         * <p>
         * 기록을 보관한다.
         *
         * @param record the record / 기록
         */
        public void record(ConsoleRecord record) {
            records.add(record);
        }

        /**
         * Keeps a call of the plain method.
         * <p>
         * 일반 메서드의 호출을 보관한다.
         *
         * @param level the level / 수준
         * @param text  the text / 문자열
         */
        public void message(String level, String text) {
            plain.add(level + ": " + text);
        }
    }

    /**
     * Runs a script in a fresh realm whose console is a record hook.
     * <p>
     * 콘솔이 기록 연결인 새 realm 에서 스크립트를 실행한다.
     *
     * @param source the script / 스크립트
     * @return what the hook got / 연결이 받은 것
     */
    private static Records run(String source) {
        Records hook = new Records();
        JsRealm r = Scripts.realm(new Scripts.Capture());
        r.setConsole(hook);
        r.evaluateScript(source, "t.js");
        return hook;
    }

    /**
     * A hook of this type gets each message exactly once, as a record (never through the plain method), with the method name, the level, the
     * formatted text, the unformatted argument texts and the place of the call.
     * <p>
     * 이 타입의 연결은 각 메시지를 정확히 한 번, 기록으로만(일반 메서드로는 절대 아님) 받는다. 메서드 이름, 수준, 서식을 적용한 문자열, 서식을 적용하기 전의
     * 인수 문자열, 호출 위치가 함께 온다.
     */
    public void testRecordCarriesMethodArgumentsAndPlace() {
        Records h = run("console.log('a %d', 1, {b: 2});\nconsole.warn('w');\nfunction f() {\n  console.error('e');\n}\nf();");
        Check.equal(0, h.plain.size(), "a record hook never gets the plain call");
        Check.equal(3, h.records.size(), "three messages");
        ConsoleRecord a = h.records.get(0);
        Check.equal(ConsoleRecord.Kind.CALL, a.getKind(), "kind of a console call");
        Check.equal("log", a.getMethod(), "method");
        Check.equal("log", a.getLevel(), "level");
        Check.equal("a 1 { b: 2 }", a.getText(), "formatted text");
        Check.equal("[a %d, 1, { b: 2 }]", a.getArguments().toString(), "argument texts");
        Check.equal("t.js", a.getSourceName(), "script name");
        Check.equal(1, a.getLine(), "line");
        Check.equal(1, a.getColumn(), "column");
        Check.equal("warn", h.records.get(1).getLevel(), "warn level");
        ConsoleRecord e = h.records.get(2);
        Check.equal("error", e.getLevel(), "error level");
        Check.equal(4, e.getLine(), "line inside a function");
        Check.equal(3, e.getColumn(), "column inside a function");
        Check.equal("t.js", e.getSourceName(), "function from the same script");
    }

    /**
     * Every method of the Console Standard reports its own method name and the level the standard gives it.
     * <p>
     * 콘솔 표준의 모든 메서드가 자기 메서드 이름과 표준이 정한 수준을 보고한다.
     */
    public void testMethodsAndLevels() {
        Records h = run("console.info('i'); console.debug('d'); console.dir({x: 1}); console.dirxml('x'); console.assert(false, 'm');\n"
                + "console.count('c'); console.countReset('none'); console.table([1]); console.trace('t'); console.time('t'); console.time('t');\n"
                + "console.timeLog('nope'); console.group('g'); console.groupEnd();");
        String expected = "info/info, debug/debug, dir/log, dirxml/log, assert/error, count/log, countReset/warn, table/log, trace/log, time/warn, "
                + "timeLog/warn, group/log";
        StringBuilder sb = new StringBuilder();
        // 기록마다 "메서드/수준" 을 모아 기대와 비교한다.
        for (int i = 0; i < h.records.size(); i++) {
            sb.append(i > 0 ? ", " : "").append(h.records.get(i).getMethod()).append('/').append(h.records.get(i).getLevel());
        }
        Check.equal(expected, sb.toString(), "methods and levels");
    }

    /**
     * A message inside <code>console.group</code> is indented and says how deep it is.
     * <p>
     * <code>console.group</code> 안의 메시지는 들여 써지고 깊이를 알려 준다.
     */
    public void testGroupDepth() {
        Records h = run("console.group('g'); console.group(); console.log('x'); console.groupEnd(); console.log('y'); console.groupEnd(); console.log('z');");
        Check.equal(4, h.records.size(), "group label, x, y, z");
        Check.equal("    x", h.records.get(1).getText(), "two levels of indentation");
        Check.equal(2, h.records.get(1).getGroupDepth(), "depth two");
        Check.equal("  y", h.records.get(2).getText(), "one level");
        Check.equal(1, h.records.get(2).getGroupDepth(), "depth one");
        Check.equal(0, h.records.get(3).getGroupDepth(), "depth zero");
    }

    /**
     * <code>eval</code> code is named <code>eval</code>, the test-only <code>print</code> function is a record of method
     * <code>print</code>, and a module is named by its key.
     * <p>
     * <code>eval</code> 코드는 <code>eval</code> 이라 부르고, 시험용 <code>print</code> 함수는 메서드 <code>print</code> 의 기록이며, 모듈은 키로
     * 이름을 붙인다.
     */
    public void testEvalPrintAndModuleNames() {
        Records h = run("eval('console.log(1)'); print('p');");
        Check.equal("eval", h.records.get(0).getSourceName(), "eval code name");
        Check.equal("print", h.records.get(1).getMethod(), "print is a record");
        Check.equal("t.js", h.records.get(1).getSourceName(), "print has the place of its caller");
    }

    /**
     * A plain hook gets what it always got, and a dialog record reaches it with the function name in the place of the level.
     * <p>
     * 일반 연결은 지금까지 받던 것을 받고, 대화 상자 기록은 수준 자리에 함수 이름이 담겨 전달된다.
     */
    public void testPlainHookGetsLevelAndText() {
        final List<String> got = new ArrayList<String>();
        org.duckdns.hjow.swingwebpanel.js.ConsoleHook plain = new org.duckdns.hjow.swingwebpanel.js.ConsoleHook() {
            /**
             * Keeps the call.
             * <p>
             * 호출을 보관한다.
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                got.add(level + ": " + text);
            }
        };
        ConsoleRecord.deliver(plain, ConsoleRecord.call("log", "log", "x 1", java.util.Collections.singletonList("x 1"), "s", 1, 1, 0));
        ConsoleRecord.deliver(plain, ConsoleRecord.dialog("alert", "hi", "s", 1, 1));
        ConsoleRecord.deliver(plain, ConsoleRecord.report(ConsoleRecord.Kind.UNCAUGHT_EXCEPTION, "Uncaught boom", "s", 2, 3));
        Check.equal("[log: x 1, alert: hi, error: Uncaught boom]", got.toString(), "plain hook");
    }

    /**
     * A record cannot be changed, and the factories reject a wrong kind.
     * <p>
     * 기록은 바꿀 수 없고, 팩터리는 잘못된 종류를 거부한다.
     */
    public void testRecordIsImmutable() {
        final ConsoleRecord r = ConsoleRecord.dialog("confirm", "ok?", null, -3, -1);
        Check.equal("", r.getSourceName(), "null name is empty");
        Check.equal(0, r.getLine(), "negative line is unknown");
        Check.equal("info", r.getLevel(), "dialogs are info");
        Check.equal("confirm", r.getLegacyLevel(), "legacy level is the function name");
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            /**
             * Tries to change the arguments.
             * <p>
             * 인수를 바꿔 본다.
             *
             * @throws Exception always expected / 항상 기대함
             */
            public void run() throws Exception {
                r.getArguments().add("x");
            }
        }, "arguments are read-only");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Asks for a report of a call kind.
             * <p>
             * 호출 종류의 보고를 요청한다.
             *
             * @throws Exception always expected / 항상 기대함
             */
            public void run() throws Exception {
                ConsoleRecord.report(ConsoleRecord.Kind.CALL, "x", "", 0, 0);
            }
        }, "a report cannot be a console call");
    }
}
