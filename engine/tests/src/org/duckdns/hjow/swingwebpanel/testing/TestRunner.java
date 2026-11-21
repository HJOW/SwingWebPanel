package org.duckdns.hjow.swingwebpanel.testing;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minimal reflection based test runner written with standard Java only (no JUnit).
 * A <em>suite</em> is a public class with a public no-argument constructor. Every public instance method whose name
 * starts with <code>test</code> is one case; optional <code>setUp()</code>/<code>tearDown()</code> methods run around it.
 * Failure is decided explicitly ({@link Check}, exceptions), not by Java <code>assert</code>. Each case has a timeout
 * so that an infinite loop in a case cannot hang the run. The process exit code is 0 only if no case failed.
 * <p>
 * Usage: <code>java org.duckdns.hjow.swingwebpanel.testing.TestRunner [--scan classesDir] [--filter text] [SuiteClassName ...]</code>.
 * With <code>--scan</code> every class named <code>*Test</code> or <code>*Tests</code> under the directory is a suite; the option may be
 * repeated to scan several directories (for example the test classes of the engine and of the browser).
 * <p>
 * 표준 Java 만으로 만든 리플렉션 기반 최소 시험 실행기(JUnit 미사용). <em>스위트</em>는 public 기본 생성자가 있는 public 클래스이며,
 * 이름이 <code>test</code> 로 시작하는 public 인스턴스 메서드 하나가 사례 하나이다. 선택적인 <code>setUp()</code>/<code>tearDown()</code>
 * 이 사례 전후에 실행된다. 실패는 Java <code>assert</code> 가 아니라 명시적으로({@link Check}, 예외) 판정한다.
 * 사례마다 제한 시간이 있어 무한 반복이 전체 실행을 멈추지 못한다. 실패한 사례가 하나도 없을 때만 종료 코드가 0 이다.
 * <p>
 * 사용법: <code>java org.duckdns.hjow.swingwebpanel.testing.TestRunner [--scan 클래스디렉터리] [--filter 문자열] [스위트클래스이름 ...]</code>.
 * <code>--scan</code> 을 쓰면 해당 디렉터리 아래에서 이름이 <code>*Test</code> 또는 <code>*Tests</code> 인 클래스를 모두 스위트로 보며, 이 옵션을 여러 번
 * 써서 디렉터리 여러 개(예: 엔진 시험 클래스와 브라우저 시험 클래스)를 검색할 수 있다.
 */
public final class TestRunner {

    /**
     * Default per-case timeout in milliseconds; override with the system property <code>swp.test.timeout</code>.
     * <p>
     * 사례별 기본 제한 시간(밀리초). 시스템 속성 <code>swp.test.timeout</code> 으로 바꿀 수 있다.
     */
    public static final long DEFAULT_TIMEOUT_MS = 60000L;

    /**
     * Exit code: all cases passed (skips allowed).
     * <p>
     * 종료 코드: 모든 사례 통과(건너뜀 허용).
     */
    public static final int EXIT_OK = 0;

    /**
     * Exit code: at least one case failed or timed out.
     * <p>
     * 종료 코드: 실패 또는 시간 초과 사례가 하나 이상 있음.
     */
    public static final int EXIT_FAILED = 1;

    /**
     * Exit code: usage error or no suite found.
     * <p>
     * 종료 코드: 사용법 오류이거나 스위트를 찾지 못함.
     */
    public static final int EXIT_USAGE = 2;

    /**
     * Private constructor: use {@link #main(String[])} or {@link #run(String[], PrintStream)}.
     * <p>
     * 비공개 생성자: {@link #main(String[])} 또는 {@link #run(String[], PrintStream)} 을 사용한다.
     */
    private TestRunner() {
    }

    /**
     * Program entry point; exits the JVM with the run's exit code.
     * <p>
     * 프로그램 진입점. 실행 결과의 종료 코드로 JVM 을 종료한다.
     *
     * @param args command line arguments / 명령행 인수
     */
    public static void main(String[] args) {
        int code = run(args, System.out);
        System.out.flush();
        System.exit(code);
    }

    /**
     * Runs the suites described by the arguments and prints a report.
     * <p>
     * 인수가 가리키는 스위트를 실행하고 보고서를 출력한다.
     *
     * @param args command line arguments / 명령행 인수
     * @param out  report stream / 보고서 스트림
     * @return exit code ({@link #EXIT_OK}, {@link #EXIT_FAILED} or {@link #EXIT_USAGE}) / 종료 코드
     */
    public static int run(String[] args, PrintStream out) {
        List<String> suites = new ArrayList<String>();
        String filter = null;
        List<File> scanDirs = new ArrayList<File>();
        // 인수를 앞에서부터 해석한다. --scan 은 여러 번 쓸 수 있다.
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--scan".equals(a) && i + 1 < args.length) {
                scanDirs.add(new File(args[++i]));
            } else if ("--filter".equals(a) && i + 1 < args.length) {
                filter = args[++i];
            } else if (a.startsWith("--")) {
                out.println("unknown option: " + a);
                return EXIT_USAGE;
            } else {
                suites.add(a);
            }
        }
        // 지정한 디렉터리마다 스위트를 찾는다.
        for (int i = 0; i < scanDirs.size(); i++) {
            File scanDir = scanDirs.get(i);
            scan(scanDir, scanDir, suites);
        }
        if (suites.isEmpty()) {
            out.println("no test suites found");
            return EXIT_USAGE;
        }
        Collections.sort(suites);

        long timeout = DEFAULT_TIMEOUT_MS;
        String prop = System.getProperty("swp.test.timeout");
        if (prop != null) {
            try {
                timeout = Long.parseLong(prop.trim());
            } catch (NumberFormatException e) {
                out.println("invalid swp.test.timeout: " + prop);
                return EXIT_USAGE;
            }
        }

        int passed = 0;
        int failed = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<String>();
        List<String> skips = new ArrayList<String>();
        long begin = System.currentTimeMillis();

        // 스위트마다 클래스를 불러와 test 로 시작하는 메서드를 모두 실행한다.
        for (int s = 0; s < suites.size(); s++) {
            String suiteName = suites.get(s);
            Class<?> suiteClass;
            try {
                suiteClass = Class.forName(suiteName);
            } catch (Throwable t) {
                out.println("FAIL " + suiteName + " : cannot load suite: " + t);
                failed++;
                failures.add(suiteName + " : cannot load suite: " + t);
                continue;
            }
            List<Method> methods = findTestMethods(suiteClass);
            // 스위트의 사례(test 로 시작하는 메서드)를 하나씩 실행한다.
            for (int m = 0; m < methods.size(); m++) {
                Method method = methods.get(m);
                String id = suiteName + "." + method.getName();
                // --filter 가 있으면 이름에 문자열이 들어 있는 사례만 실행한다.
                if (filter != null && id.indexOf(filter) < 0) {
                    continue;
                }
                long t0 = System.currentTimeMillis();
                Outcome o = runOne(suiteClass, method, timeout);
                long ms = System.currentTimeMillis() - t0;
                if (o.status == Outcome.PASS) {
                    passed++;
                    out.println("PASS " + id + " (" + ms + " ms)");
                } else if (o.status == Outcome.SKIP) {
                    skipped++;
                    out.println("SKIP " + id + " : " + o.message);
                    skips.add(id + " : " + o.message);
                } else {
                    failed++;
                    String label = o.status == Outcome.TIMEOUT ? "TIMEOUT" : "FAIL";
                    out.println(label + " " + id + " (" + ms + " ms) : " + o.message);
                    if (o.trace != null) {
                        out.println(o.trace);
                    }
                    failures.add(id + " : " + o.message);
                }
            }
        }

        out.println();
        out.println("=== " + passed + " passed, " + failed + " failed, " + skipped + " skipped ("
                + (System.currentTimeMillis() - begin) + " ms) ===");
        // 요약에 실패와 건너뜀 목록을 한 번 더 보여 준다.
        for (int i = 0; i < failures.size(); i++) {
            out.println("  FAILED: " + failures.get(i));
        }
        // 건너뛴 사례 목록도 이유와 함께 요약에 보여 준다.
        for (int i = 0; i < skips.size(); i++) {
            out.println("  SKIPPED: " + skips.get(i));
        }
        return failed > 0 ? EXIT_FAILED : EXIT_OK;
    }

    /**
     * Recursively finds suite class names (<code>*Test.class</code>, <code>*Tests.class</code>) under a directory.
     * <p>
     * 디렉터리 아래에서 스위트 클래스 이름(<code>*Test.class</code>, <code>*Tests.class</code>)을 재귀적으로 찾는다.
     *
     * @param root   scan root used to compute class names / 클래스 이름 계산의 기준 디렉터리
     * @param dir    current directory / 현재 디렉터리
     * @param result list receiving suite class names / 스위트 클래스 이름을 받을 목록
     */
    private static void scan(File root, File dir, List<String> result) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // 하위 디렉터리는 재귀로, 클래스 파일은 이름 규칙으로 걸러 낸다.
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                scan(root, f, result);
            } else {
                String name = f.getName();
                if ((name.endsWith("Test.class") || name.endsWith("Tests.class")) && name.indexOf('$') < 0) {
                    String rel = f.getPath().substring(root.getPath().length() + 1);
                    String cls = rel.substring(0, rel.length() - ".class".length()).replace(File.separatorChar, '.');
                    result.add(cls);
                }
            }
        }
    }

    /**
     * Lists the public instance methods named <code>test*</code> of a suite in alphabetical order.
     * <p>
     * 스위트의 <code>test*</code> public 인스턴스 메서드를 이름순으로 나열한다.
     *
     * @param c suite class / 스위트 클래스
     * @return test methods / 시험 메서드 목록
     */
    private static List<Method> findTestMethods(Class<?> c) {
        List<Method> list = new ArrayList<Method>();
        Method[] all = c.getMethods();
        // 이름 규칙에 맞고 인수가 없는 인스턴스 메서드만 사례로 인정한다.
        for (int i = 0; i < all.length; i++) {
            Method m = all[i];
            if (m.getName().startsWith("test") && m.getParameterTypes().length == 0 && !Modifier.isStatic(m.getModifiers())) {
                list.add(m);
            }
        }
        Method[] arr = list.toArray(new Method[list.size()]);
        Arrays.sort(arr, new java.util.Comparator<Method>() {
            /**
             * Orders methods by name.
             * <p>
             * 메서드를 이름순으로 정렬한다.
             *
             * @param a first method / 첫 번째 메서드
             * @param b second method / 두 번째 메서드
             * @return comparison result / 비교 결과
             */
            public int compare(Method a, Method b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return Arrays.asList(arr);
    }

    /**
     * Result of one case.
     * <p>
     * 사례 하나의 결과.
     */
    private static final class Outcome {
        /**
         * Status: passed.
         * <p>
         * 상태: 통과.
         */
        static final int PASS = 0;
        /**
         * Status: failed.
         * <p>
         * 상태: 실패.
         */
        static final int FAIL = 1;
        /**
         * Status: skipped with reason.
         * <p>
         * 상태: 이유와 함께 건너뜀.
         */
        static final int SKIP = 2;
        /**
         * Status: timed out.
         * <p>
         * 상태: 제한 시간 초과.
         */
        static final int TIMEOUT = 3;

        /**
         * Current status (written by the case thread).
         * <p>
         * 현재 상태(사례 스레드가 기록).
         */
        volatile int status = FAIL;
        /**
         * Message for failures and skips.
         * <p>
         * 실패/건너뜀 메시지.
         */
        volatile String message = "did not finish";
        /**
         * Trimmed stack trace for failures.
         * <p>
         * 실패 시 줄인 스택 추적.
         */
        volatile String trace;
    }

    /**
     * Runs one case in its own thread with a timeout.
     * <p>
     * 사례 하나를 별도 스레드에서 제한 시간을 두고 실행한다.
     *
     * @param suiteClass suite class / 스위트 클래스
     * @param method     case method / 사례 메서드
     * @param timeoutMs  timeout in milliseconds / 제한 시간(밀리초)
     * @return the outcome / 결과
     */
    private static Outcome runOne(final Class<?> suiteClass, final Method method, long timeoutMs) {
        final Outcome o = new Outcome();
        Thread t = new Thread(new Runnable() {
            /**
             * Instantiates the suite and invokes setUp, the case and tearDown.
             * <p>
             * 스위트를 만들고 setUp, 사례, tearDown 을 호출한다.
             */
            public void run() {
                Object instance = null;
                try {
                    instance = suiteClass.newInstance();
                    invokeOptional(instance, "setUp");
                    method.invoke(instance);
                    o.status = Outcome.PASS;
                    o.message = "";
                } catch (InvocationTargetException e) {
                    record(o, e.getCause());
                } catch (Throwable e) {
                    record(o, e);
                } finally {
                    try {
                        if (instance != null) {
                            invokeOptional(instance, "tearDown");
                        }
                    } catch (Throwable e) {
                        // tearDown 의 실패는 통과한 사례도 실패로 바꾼다.
                        if (o.status == Outcome.PASS) {
                            record(o, e instanceof InvocationTargetException ? e.getCause() : e);
                        }
                    }
                }
            }
        }, "test-" + method.getName());
        t.setDaemon(true);
        t.start();
        try {
            t.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) {
            t.interrupt();
            o.status = Outcome.TIMEOUT;
            o.message = "timed out after " + timeoutMs + " ms";
            o.trace = null;
        }
        return o;
    }

    /**
     * Stores a thrown exception into an outcome as failure or skip.
     * <p>
     * 던져진 예외를 실패 또는 건너뜀으로 결과에 기록한다.
     *
     * @param o outcome to fill / 채울 결과
     * @param t thrown exception / 던져진 예외
     */
    private static void record(Outcome o, Throwable t) {
        if (t instanceof SkipException) {
            o.status = Outcome.SKIP;
            o.message = String.valueOf(t.getMessage());
            return;
        }
        o.status = Outcome.FAIL;
        o.message = String.valueOf(t);
        StackTraceElement[] st = t.getStackTrace();
        StringBuilder sb = new StringBuilder();
        // 스택은 앞쪽 12 줄만 보여 준다.
        for (int i = 0; i < st.length && i < 12; i++) {
            sb.append("    at ").append(st[i]).append('\n');
        }
        o.trace = sb.toString().trim();
    }

    /**
     * Calls a public no-argument method if the object has one.
     * <p>
     * 객체에 public 인수 없는 메서드가 있으면 호출한다.
     *
     * @param target object / 대상 객체
     * @param name   method name / 메서드 이름
     * @throws Throwable the method's own failure / 메서드가 던진 실패
     */
    private static void invokeOptional(Object target, String name) throws Throwable {
        Method m;
        try {
            m = target.getClass().getMethod(name);
        } catch (NoSuchMethodException e) {
            return;
        }
        try {
            m.invoke(target);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
