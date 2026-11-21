package org.duckdns.hjow.swingwebpanel.js.interp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helper for the interpreter tests: creates realms whose console output is captured, evaluates inline scripts and script
 * resources that report failures through a tiny assertion prelude (<code>t(name, got, want)</code>,
 * <code>terr(name, fn, errorName)</code>, <code>drain(iterator)</code>), and turns reported failures into test failures.
 * <p>
 * 인터프리터 시험의 도우미: 콘솔 출력을 모으는 realm 을 만들고, 인라인 스크립트와 작은 단언 서문(<code>t(name, got, want)</code>,
 * <code>terr(name, fn, errorName)</code>, <code>drain(iterator)</code>)으로 실패를 보고하는 스크립트 리소스를 실행하며, 보고된 실패를
 * 시험 실패로 바꾼다.
 */
public final class Scripts {

    /**
     * The assertion prelude evaluated before every checked script: <code>t</code> compares with strict equality (NaN equals
     * NaN) and prints a line starting with <code>FAIL</code> on mismatch; <code>terr</code> expects an exception with the given
     * <code>name</code>; <code>drain</code> collects an iterator into a comma separated string.
     * <p>
     * 검사하는 모든 스크립트 앞에서 실행하는 단언 서문: <code>t</code> 는 엄격 동등(NaN 은 NaN 과 같음)으로 비교해 불일치하면
     * <code>FAIL</code> 로 시작하는 줄을 출력하고, <code>terr</code> 는 주어진 <code>name</code> 의 예외를 기대하며,
     * <code>drain</code> 은 이터레이터를 쉼표로 구분한 문자열로 모은다.
     */
    private static final String PRELUDE =
            "var __count = 0;\n"
            + "function t(name, got, want) { __count++; if (got !== want && !(got !== got && want !== want)) {"
            + " print('FAIL ' + name + ': got ' + String(got) + ' want ' + String(want)); } }\n"
            + "function terr(name, fn, want) { __count++; try { fn(); print('FAIL ' + name + ': no exception'); }"
            + " catch (e) { var n = e && e.name; if (n !== want) { print('FAIL ' + name + ': got ' + n + ' (' + (e && e.message)"
            + " + ') want ' + want); } } }\n"
            + "function drain(g) { var o = [], r; while (!(r = g.next()).done) { o.push(r.value); } return o.join(','); }\n";

    /**
     * A console hook that records every printed line.
     * <p>
     * 출력한 모든 줄을 기록하는 콘솔 연결.
     */
    public static final class Capture implements ConsoleHook {
        /**
         * The recorded lines (level is not included).
         * <p>
         * 기록한 줄(수준은 포함하지 않음).
         */
        public final List<String> lines = new ArrayList<String>();

        /**
         * The recorded lines with their level in front, e.g. <code>warn: text</code>.
         * <p>
         * 수준을 앞에 붙인 기록(예: <code>warn: text</code>).
         */
        public final List<String> leveled = new ArrayList<String>();

        /**
         * Creates the capture.
         * <p>
         * 기록기를 만든다.
         */
        public Capture() {
        }

        /**
         * Records a message.
         * <p>
         * 메시지를 기록한다.
         *
         * @param level the level / 수준
         * @param text  the text / 문자열
         */
        public void message(String level, String text) {
            lines.add(text);
            leveled.add(level + ": " + text);
        }
    }

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private Scripts() {
    }

    /**
     * Creates a ready-to-use realm that reports console output to the given capture.
     * <p>
     * 콘솔 출력을 주어진 기록기로 보내는, 바로 쓸 수 있는 realm 을 만든다.
     *
     * @param capture the capture / 기록기
     * @return the realm / realm
     */
    public static JsRealm realm(Capture capture) {
        JsRealm r = JsRealm.create();
        r.setConsole(capture);
        return r;
    }

    /**
     * Evaluates a script in a fresh realm (then runs the pending jobs) and returns the completion value converted to a string.
     * <p>
     * 새 realm 에서 스크립트를 실행하고(대기 작업도 실행) 완료 값을 문자열로 바꿔 반환한다.
     *
     * @param source the source / 소스
     * @return the result text / 결과 문자열
     */
    public static String eval(String source) {
        Capture c = new Capture();
        JsRealm r = realm(c);
        JsValue v = r.evaluateScript(source, "test.js");
        r.runJobs();
        return Ops.toString(v);
    }

    /**
     * Evaluates a script in a fresh realm and returns everything it printed, one line per output call joined with
     * <code>\n</code>.
     * <p>
     * 새 realm 에서 스크립트를 실행하고 출력한 모든 내용을 출력 호출마다 한 줄로 <code>\n</code> 으로 이어 반환한다.
     *
     * @param source the source / 소스
     * @return the printed text / 출력 문자열
     */
    public static String output(String source) {
        Capture c = new Capture();
        JsRealm r = realm(c);
        r.evaluateScript(source, "test.js");
        r.runJobs();
        StringBuilder sb = new StringBuilder();
        // 기록한 줄을 개행 문자로 잇는다.
        for (int i = 0; i < c.lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(c.lines.get(i));
        }
        return sb.toString();
    }

    /**
     * Evaluates a script that must throw a script exception and returns its description (for example
     * <code>TypeError: x is not a function</code>).
     * <p>
     * 스크립트 예외를 던져야 하는 스크립트를 실행하고 그 설명(예: <code>TypeError: x is not a function</code>)을 반환한다.
     *
     * @param source the source / 소스
     * @return the exception description / 예외 설명
     */
    public static String thrown(String source) {
        Capture c = new Capture();
        JsRealm r = realm(c);
        try {
            r.evaluateScript(source, "test.js");
        } catch (JsException e) {
            return e.getMessage();
        }
        Check.fail("script did not throw: " + source);
        return null;
    }

    /**
     * Reads a text resource of this package (under <code>/org/duckdns/hjow/swingwebpanel/js/interp/</code>) as UTF-8.
     * <p>
     * 이 패키지의 텍스트 리소스(<code>/org/duckdns/hjow/swingwebpanel/js/interp/</code> 아래)를 UTF-8 로 읽는다.
     *
     * @param name the resource file name / 리소스 파일 이름
     * @return the text / 텍스트
     */
    public static String resource(String name) {
        InputStream in = Scripts.class.getResourceAsStream("/org/duckdns/hjow/swingwebpanel/js/interp/" + name);
        if (in == null) {
            Check.fail("missing test resource " + name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 리소스를 끝까지 읽는다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + name + ": " + e);
        } finally {
            try {
                in.close();
            } catch (IOException ignore) {
                // 닫기 실패는 시험 결과에 영향이 없다.
            }
        }
    }

    /**
     * Runs a checked script in the given realm and fails the test with every reported failure. The script must have executed at
     * least <code>minChecks</code> assertions.
     * <p>
     * 검사하는 스크립트를 주어진 realm 에서 실행하고 보고된 모든 실패로 시험을 실패시킨다. 스크립트는 단언을 최소
     * <code>minChecks</code> 번 실행해야 한다.
     *
     * @param realm     the realm (the prelude is evaluated in it first) / realm(서문을 먼저 실행한다)
     * @param capture   the capture connected to the realm / realm 에 연결된 기록기
     * @param source    the script source / 스크립트 소스
     * @param name      the script name for messages / 메시지용 스크립트 이름
     * @param minChecks the minimum number of assertions expected / 기대하는 최소 단언 수
     */
    public static void check(JsRealm realm, Capture capture, String source, String name, int minChecks) {
        realm.evaluateScript(PRELUDE, "prelude.js");
        realm.evaluateScript(source, name);
        realm.runJobs();
        StringBuilder failures = new StringBuilder();
        // FAIL 로 시작하는 줄만 모아 한꺼번에 보고한다.
        for (int i = 0; i < capture.lines.size(); i++) {
            String line = capture.lines.get(i);
            if (line.startsWith("FAIL ")) {
                failures.append('\n').append(name).append(": ").append(line);
            }
        }
        if (failures.length() > 0) {
            Check.fail("script assertions failed:" + failures);
        }
        double count = Ops.toNumber(realm.getGlobalObject().get("__count"));
        Check.isTrue(count >= minChecks, name + " ran only " + (long) count + " assertions, expected at least " + minChecks);
    }

    /**
     * Runs a script resource in a fresh realm with the assertion prelude (see {@link #check}).
     * <p>
     * 스크립트 리소스를 단언 서문과 함께 새 realm 에서 실행한다({@link #check} 참조).
     *
     * @param resourceName the resource file name / 리소스 파일 이름
     * @param minChecks    the minimum number of assertions expected / 기대하는 최소 단언 수
     */
    public static void checkResource(String resourceName, int minChecks) {
        Capture c = new Capture();
        JsRealm r = realm(c);
        check(r, c, resource(resourceName), resourceName, minChecks);
    }

    /**
     * Runs an inline script in a fresh realm with the assertion prelude (see {@link #check}).
     * <p>
     * 인라인 스크립트를 단언 서문과 함께 새 realm 에서 실행한다({@link #check} 참조).
     *
     * @param source    the script / 스크립트
     * @param minChecks the minimum number of assertions expected / 기대하는 최소 단언 수
     */
    public static void checkInline(String source, int minChecks) {
        Capture c = new Capture();
        JsRealm r = realm(c);
        check(r, c, source, "inline.js", minChecks);
    }
}
