package sourcecheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for forbidden APIs and for syntax or APIs newer than Java 6 in Java sources. Forbidden items come from the project
 * rules: <code>com.sun</code>/<code>sun</code> packages, JavaScript engine bridges, external processes and browsers, native code
 * (<code>native</code> methods, <code>System.loadLibrary</code>), Swing's built-in HTML renderer. The Java 6 syntax/API list is only a
 * fast lint: compiling with JDK 6 is the real proof. A line containing the text <code>sourcecheck:allow</code> is exempt.
 * <p>
 * Java 소스의 금지 API 와 Java 6 이후 구문/API 를 검사한다. 금지 항목은 프로젝트 규칙에서 온다: <code>com.sun</code>/<code>sun</code> 패키지,
 * JavaScript 엔진 연결, 외부 프로세스/브라우저, 네이티브 코드(<code>native</code> 메서드, <code>System.loadLibrary</code>), Swing 내장 HTML 렌더러.
 * Java 6 구문/API 목록은 빠른 점검일 뿐이며 JDK 6 으로 컴파일하는 것이 실제 증명이다. <code>sourcecheck:allow</code> 가 들어 있는 줄은 제외한다.
 */
final class ForbiddenChecks {

    /**
     * One rule: an id, a regular expression over blanked code, and a message.
     * <p>
     * 규칙 하나: 식별자, 공백 처리된 코드에 적용하는 정규식, 메시지.
     */
    private static final class Rule {
        /**
         * Rule id.
         * <p>
         * 규칙 식별자.
         */
        final String id;

        /**
         * Compiled pattern.
         * <p>
         * 컴파일된 패턴.
         */
        final Pattern pattern;

        /**
         * Message shown for a match.
         * <p>
         * 일치 시 보여 줄 메시지.
         */
        final String message;

        /**
         * Creates a rule.
         * <p>
         * 규칙을 만든다.
         *
         * @param id      rule id / 규칙 식별자
         * @param regex   regular expression / 정규식
         * @param message message / 메시지
         */
        Rule(String id, String regex, String message) {
            this.id = id;
            this.pattern = Pattern.compile(regex);
            this.message = message;
        }
    }

    /**
     * All regular-expression rules.
     * <p>
     * 정규식 규칙 전체.
     */
    private static final List<Rule> RULES = new ArrayList<Rule>();

    /**
     * Matches a use of the Java Scripting API.
     * <p>
     * Java Scripting API 사용에 일치한다.
     */
    private static final Pattern SCRIPT_API = Pattern.compile("\\bjavax\\s*\\.\\s*script\\b");

    /**
     * Matches display related packages that no part of the engine module may use (AWT, Swing and what depends on them).
     * <p>
     * 엔진 모듈의 어느 부분도 쓸 수 없는 화면 관련 패키지(AWT, Swing과 그에 의존하는 것)에 일치한다.
     */
    private static final Pattern ENGINE_ALWAYS = Pattern.compile("\\bjava\\s*\\.\\s*(awt|applet)\\b|\\bjavax\\s*\\.\\s*(swing|imageio|sound|print|accessibility)\\b");

    /**
     * Matches the network, remote and file facilities that the engine sources may not use.
     * <p>
     * 엔진 소스가 쓸 수 없는 네트워크, 원격, 파일 기능에 일치한다.
     */
    private static final Pattern ENGINE_SOURCE_ONLY = Pattern.compile("\\bjava\\s*\\.\\s*(net|rmi|sql|nio)\\b|\\bjavax\\s*\\.\\s*(net|sql|rmi|naming)\\b"
            + "|\\bjava\\s*\\.\\s*io\\s*\\.\\s*(File|FileInputStream|FileOutputStream|FileReader|FileWriter|RandomAccessFile)\\b");

    /**
     * Matches a package or class of this project below the main package; group 1 is the first name after it and group 2 the second.
     * <p>
     * 주 패키지 아래의 이 프로젝트의 패키지나 클래스에 일치한다. 그룹 1은 그 뒤의 첫 이름, 그룹 2는 두 번째 이름이다.
     */
    private static final Pattern PROJECT_PACKAGE = Pattern.compile("\\borg\\s*\\.\\s*duckdns\\s*\\.\\s*hjow\\s*\\.\\s*swingwebpanel\\s*\\.\\s*(\\w+)(?:\\s*\\.\\s*(\\w+))?");

    static {
        add("forbidden-api", "\\bcom\\s*\\.\\s*sun\\b", "com.sun.* packages are forbidden");
        add("forbidden-api", "(?<![\\w.])sun\\s*\\.\\s*[a-z]", "sun.* packages are forbidden");
        add("forbidden-api", "\\bjdk\\s*\\.\\s*(internal|nashorn)\\b", "JDK internal/Nashorn packages are forbidden");
        add("forbidden-engine", "\\borg\\s*\\.\\s*mozilla\\b", "Rhino (org.mozilla) is forbidden");
        add("forbidden-engine", "\\b(org\\s*\\.\\s*graalvm|com\\s*\\.\\s*oracle\\s*\\.\\s*truffle)\\b", "GraalJS/Truffle is forbidden");
        add("forbidden-engine", "\\bjavafx\\b", "JavaFX (WebView) is forbidden");
        add("forbidden-engine", "\\bnetscape\\s*\\.\\s*javascript\\b", "netscape.javascript is forbidden");
        add("forbidden-engine", "\\bnet\\s*\\.\\s*java\\s*\\.\\s*dev\\s*\\.\\s*jna\\b", "JNA is forbidden");
        add("forbidden-process", "\\bRuntime\\s*\\.\\s*getRuntime\\s*\\(\\s*\\)\\s*\\.\\s*exec\\b", "Runtime.exec is forbidden");
        add("forbidden-process", "\\bProcessBuilder\\b", "ProcessBuilder is forbidden");
        add("forbidden-process", "\\bDesktop\\s*\\.\\s*(getDesktop|isDesktopSupported)\\b", "java.awt.Desktop (external browser) is forbidden");
        add("forbidden-native", "\\bSystem\\s*\\.\\s*(loadLibrary|load|mapLibraryName)\\s*\\(", "System.load/loadLibrary (native code) is forbidden");
        add("forbidden-render", "\\b(JEditorPane|HTMLEditorKit|HTMLDocument)\\b", "Swing built-in HTML renderer is forbidden");
        add("forbidden-render", "\\bjavax\\s*\\.\\s*swing\\s*\\.\\s*text\\s*\\.\\s*html\\b", "javax.swing.text.html is forbidden");

        add("java6-api", "\\bjava\\s*\\.\\s*nio\\s*\\.\\s*file\\b", "java.nio.file (Java 7) is not allowed");
        add("java6-api", "\\bjava\\s*\\.\\s*(util\\s*\\.\\s*(function|stream)|time|lang\\s*\\.\\s*invoke)\\b", "Java 8+ package is not allowed");
        add("java6-api", "\\b(StandardCharsets|Objects|Optional|Base64|CompletableFuture|ThreadLocalRandom|AutoCloseable|ReflectiveOperationException)\\b",
                "Java 7+ class is not allowed");
        add("java6-api", "\\b(Integer|Long|Short|Byte|Character|Boolean)\\s*\\.\\s*compare\\s*\\(", "X.compare (Java 7) is not allowed");
        add("java6-api", "\\b(Integer|Long|Double|Boolean|Short|Byte|Character)\\s*\\.\\s*hashCode\\s*\\(", "static X.hashCode (Java 8) is not allowed");
        add("java6-api", "\\bMath\\s*\\.\\s*(floorMod|floorDiv|addExact|subtractExact|multiplyExact|toIntExact|negateExact|incrementExact|decrementExact)\\b",
                "Math exact/floor methods (Java 8) are not allowed");
        add("java6-api", "\\.\\s*(forEach|stream|removeIf|getOrDefault|putIfAbsent|computeIfAbsent|addSuppressed)\\s*\\(", "Java 7/8 collection or Throwable API is not allowed");
        add("java6-api", "\\bString\\s*\\.\\s*join\\s*\\(", "String.join (Java 8) is not allowed");
        add("java6-api", "\\bCharacter\\s*\\.\\s*isAlphabetic\\b", "Character.isAlphabetic (Java 7) is not allowed");
        add("java6-api", "\\bUNICODE_CHARACTER_CLASS\\b", "Pattern.UNICODE_CHARACTER_CLASS (Java 7) is not allowed");
        add("java6-syntax", "->", "lambda expression (Java 8) is not allowed");
        add("java6-syntax", "::", "method reference (Java 8) is not allowed");
        add("java6-syntax", "<\\s*>", "diamond operator (Java 7) is not allowed");
        add("java6-syntax", "\\btry\\s*\\(", "try-with-resources (Java 7) is not allowed");
        add("java6-syntax", "\\bcatch\\s*\\([^)]*\\|", "multi-catch (Java 7) is not allowed");
        add("java6-syntax", "\\b0[bB][01_]+\\b", "binary literal (Java 7) is not allowed");
        add("java6-syntax", "\\b\\d+_\\d+", "underscore in numeric literal (Java 7) is not allowed");
        add("java6-syntax", "@\\s*(FunctionalInterface|SafeVarargs)\\b", "Java 7/8 annotation is not allowed");
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ForbiddenChecks() {
    }

    /**
     * Registers a rule.
     * <p>
     * 규칙을 등록한다.
     *
     * @param id      rule id / 규칙 식별자
     * @param regex   regular expression / 정규식
     * @param message message / 메시지
     */
    private static void add(String id, String regex, String message) {
        RULES.add(new Rule(id, regex, message));
    }

    /**
     * Runs all rules over one scanned file.
     * <p>
     * 스캔된 파일 하나에 모든 규칙을 적용한다.
     *
     * @param sc   scanned source / 스캔된 소스
     * @param path display path / 표시 경로
     * @param out  list receiving violations / 위반을 받을 목록
     */
    static void check(JavaScanner sc, String path, List<Violation> out) {
        // 규칙마다 코드 텍스트에서 일치하는 곳을 모두 찾는다.
        for (int r = 0; r < RULES.size(); r++) {
            Rule rule = RULES.get(r);
            Matcher m = rule.pattern.matcher(sc.code);
            // 같은 규칙에 일치하는 곳을 모두 보고한다.
            while (m.find()) {
                int line = sc.lineOf(m.start());
                if (!isAllowed(sc, line)) {
                    out.add(new Violation(path, line, rule.id, rule.message));
                }
            }
        }
        // native 한정자는 토큰으로 검사한다.
        for (int i = 0; i < sc.toks.size(); i++) {
            JavaScanner.Tok k = sc.toks.get(i);
            if (k.isIdent("native") && !isAllowed(sc, k.line)) {
                out.add(new Violation(path, k.line, "forbidden-native", "native methods (JNI) are forbidden"));
            }
        }
        checkDefaultCharset(sc, path, out);
        checkScriptingApi(sc, path, out);
        checkEngineBoundary(sc, path, out);
    }

    /**
     * Normalizes a display path to forward slashes with a leading slash, so that a directory can be recognized by text such as
     * <code>/engine/src/main/java/</code> both in the repository and in the self test samples.
     * <p>
     * 표시 경로를 슬래시 구분에 맨 앞 슬래시가 있는 형태로 바꿔 저장소와 자체 시험에서 <code>/engine/src/main/java/</code> 같은 경로를 알아볼 수 있게 한다.
     *
     * @param path display path / 표시 경로
     * @return the normalized path / 정규화된 경로
     */
    private static String normalized(String path) {
        return "/" + path.replace('\\', '/');
    }

    /**
     * Tells whether a source may use the Java Scripting API (<code>javax.script</code>). Only the provider package of the own engine
     * (<code>js/script</code> in the engine's Java source and test trees) and the examples package that shows its use
     * may. Product code (the browser component, the rest of the engine) must never reach a JavaScript engine through
     * <code>ScriptEngineManager</code>, because that could silently run the Rhino or Nashorn engine of the JRE (plan items P00-09, P26-05).
     * <p>
     * 소스가 Java Scripting API(<code>javax.script</code>)를 써도 되는지 알려 준다. 엔진 모듈의 Java 소스와 시험의 <code>js/script</code> 패키지와
     * 그 사용법을 보여 주는 예제 패키지만 쓸 수 있다. 제품 코드(브라우저 컴포넌트와
     * 엔진의 나머지)는 <code>ScriptEngineManager</code> 로 JavaScript 엔진에 닿으면 안 된다. JRE 의 Rhino 나 Nashorn 이 몰래 실행될 수 있기 때문이다
     * (계획 P00-09, P26-05).
     *
     * @param path display path / 표시 경로
     * @return true if javax.script is allowed in the file / 파일에서 javax.script 를 써도 되면 true
     */
    static boolean mayUseScriptingApi(String path) {
        String p = normalized(path);
        boolean providerPackage = (p.indexOf("/engine/src/main/java/") >= 0 || p.indexOf("/engine/src/test/java/") >= 0)
                && p.indexOf("/swingwebpanel/js/script/") >= 0;
        boolean examplePackage = p.indexOf("/examples/src/main/java/") >= 0 && p.indexOf("/swingwebpanel/examples/script/") >= 0;
        return providerPackage || examplePackage;
    }

    /**
     * Reports uses of <code>javax.script</code> outside the places where it is allowed (see {@link #mayUseScriptingApi(String)}).
     * <p>
     * 허용된 곳 밖에서 <code>javax.script</code> 를 쓰는 것을 보고한다({@link #mayUseScriptingApi(String)} 참조).
     *
     * @param sc   scanned source / 스캔된 소스
     * @param path display path / 표시 경로
     * @param out  list receiving violations / 위반을 받을 목록
     */
    private static void checkScriptingApi(JavaScanner sc, String path, List<Violation> out) {
        if (mayUseScriptingApi(path)) {
            return;
        }
        Matcher m = SCRIPT_API.matcher(sc.code);
        // 일치하는 곳을 모두 보고한다.
        while (m.find()) {
            int line = sc.lineOf(m.start());
            if (!isAllowed(sc, line)) {
                out.add(new Violation(path, line, "forbidden-engine",
                        "javax.script (JavaScript engine bridge) is forbidden here; only the engine provider package js/script and the examples/script package may use it"));
            }
        }
    }

    /**
     * Reports references that would tie the engine module (<code>engine/</code>) to the browser: AWT and Swing, the network and the file
     * system, and any package of the browser module. The engine sources may use the JRE's standard API for data (collections, text,
     * math, reflection for the Java Scripting API) but nothing that needs a display, a connection or a file (plan item P26-01). The test
     * sources of the engine follow the same rule except that they may use the shared test runner package <code>testing</code> and files.
     * <p>
     * 엔진 모듈(<code>engine/</code>)을 브라우저에 묶는 참조를 보고한다: AWT와 Swing, 네트워크와 파일 시스템, 브라우저 모듈의 모든 패키지. 엔진 소스는
     * 데이터를 다루는 JRE 표준 API(컬렉션, 텍스트, 수학, Java Scripting API 를 위한 리플렉션)를 쓸 수 있지만 화면, 연결, 파일이 필요한 것은 쓸 수
     * 없다(계획 P26-01). 엔진의 시험 소스도 같은 규칙을 따르되 공용 시험 실행기 패키지 <code>testing</code> 과 파일은 쓸 수 있다.
     *
     * @param sc   scanned source / 스캔된 소스
     * @param path display path / 표시 경로
     * @param out  list receiving violations / 위반을 받을 목록
     */
    private static void checkEngineBoundary(JavaScanner sc, String path, List<Violation> out) {
        String p = normalized(path);
        boolean engineSource = p.indexOf("/engine/src/main/java/") >= 0;
        boolean engineTest = p.indexOf("/engine/src/test/java/") >= 0;
        if (!engineSource && !engineTest) {
            return;
        }
        Matcher m = ENGINE_ALWAYS.matcher(sc.code);
        // AWT/Swing 등 화면 관련 패키지는 엔진 소스와 엔진 시험 모두 쓸 수 없다.
        while (m.find()) {
            int line = sc.lineOf(m.start());
            if (!isAllowed(sc, line)) {
                out.add(new Violation(path, line, "engine-boundary", "the engine module must not use " + m.group().replaceAll("\\s+", "") + " (display facilities)"));
            }
        }
        if (engineSource) {
            m = ENGINE_SOURCE_ONLY.matcher(sc.code);
            // 엔진 소스는 네트워크와 파일 시스템도 쓸 수 없다.
            while (m.find()) {
                int line = sc.lineOf(m.start());
                if (!isAllowed(sc, line)) {
                    out.add(new Violation(path, line, "engine-boundary", "the engine module must not use " + m.group().replaceAll("\\s+", "") + " (network or files)"));
                }
            }
        }
        m = PROJECT_PACKAGE.matcher(sc.code);
        // 프로젝트의 다른 패키지 참조를 찾는다. js 아래의 엔진 패키지(와 시험의 testing)만 허용한다.
        while (m.find()) {
            String first = m.group(1);
            String second = m.group(2);
            boolean allowed;
            if (first.equals("js")) {
                allowed = !("host".equals(second) || "dom".equals(second) || "worker".equals(second));
            } else {
                allowed = engineTest && first.equals("testing");
            }
            int line = sc.lineOf(m.start());
            if (!allowed && !isAllowed(sc, line)) {
                out.add(new Violation(path, line, "engine-boundary", "the engine module must not refer to the browser module package "
                        + (first.equals("js") ? "js." + second : first)));
            }
        }
    }

    /**
     * Tests whether a source line carries the allow marker.
     * <p>
     * 소스 줄에 예외 표시가 있는지 확인한다.
     *
     * @param sc   scanned source / 스캔된 소스
     * @param line 1-based line number / 1 기반 줄 번호
     * @return true if the line is exempt / 예외이면 true
     */
    private static boolean isAllowed(JavaScanner sc, int line) {
        int from = 0;
        // 줄 번호에 해당하는 원본 줄 텍스트를 잘라 낸다.
        for (int l = 1; l < line; l++) {
            int nl = sc.src.indexOf('\n', from);
            if (nl < 0) {
                return false;
            }
            from = nl + 1;
        }
        int to = sc.src.indexOf('\n', from);
        String text = to < 0 ? sc.src.substring(from) : sc.src.substring(from, to);
        return text.indexOf("sourcecheck:allow") >= 0;
    }

    /**
     * Detects reads/writes that depend on the platform default charset.
     * <p>
     * 플랫폼 기본 문자셋에 의존하는 읽기/쓰기를 찾는다.
     *
     * @param sc   scanned source / 스캔된 소스
     * @param path display path / 표시 경로
     * @param out  list receiving violations / 위반을 받을 목록
     */
    private static void checkDefaultCharset(JavaScanner sc, String path, List<Violation> out) {
        // FileReader/FileWriter 는 어떤 생성자든 문자셋을 지정할 수 없으므로 항상 위반이다.
        scanCtor(sc, path, out, "FileReader", Integer.MAX_VALUE);
        scanCtor(sc, path, out, "FileWriter", Integer.MAX_VALUE);
        scanCtor(sc, path, out, "InputStreamReader", 1);
        scanCtor(sc, path, out, "OutputStreamWriter", 1);
        Matcher m = Pattern.compile("\\.\\s*getBytes\\s*\\(\\s*\\)").matcher(sc.code);
        // 인수 없는 getBytes() 는 기본 문자셋을 쓴다.
        while (m.find()) {
            int line = sc.lineOf(m.start());
            if (!isAllowed(sc, line)) {
                out.add(new Violation(path, line, "default-charset", "getBytes() without charset depends on the platform encoding"));
            }
        }
    }

    /**
     * Reports constructor calls of a class that have at most the given number of arguments.
     * <p>
     * 주어진 인수 수 이하로 호출된 클래스 생성자를 보고한다.
     *
     * @param sc      scanned source / 스캔된 소스
     * @param path    display path / 표시 경로
     * @param out     list receiving violations / 위반을 받을 목록
     * @param cls     class name / 클래스 이름
     * @param maxArgs largest argument count that still means "no charset" / 문자셋이 없다는 뜻이 되는 최대 인수 수
     */
    private static void scanCtor(JavaScanner sc, String path, List<Violation> out, String cls, int maxArgs) {
        Matcher m = Pattern.compile("\\bnew\\s+(?:[\\w.]+\\.)?" + cls + "\\s*\\(").matcher(sc.code);
        // 생성자 호출마다 최상위 쉼표 수로 인수 개수를 센다.
        while (m.find()) {
            int p = m.end();
            int depth = 1;
            int commas = 0;
            boolean any = false;
            // 괄호 깊이가 0 이 될 때까지 문자를 읽으며 최상위 쉼표를 센다.
            while (p < sc.code.length() && depth > 0) {
                char c = sc.code.charAt(p);
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                } else if (c == ',' && depth == 1) {
                    commas++;
                } else if (!Character.isWhitespace(c)) {
                    any = true;
                }
                p++;
            }
            int args = any ? commas + 1 : 0;
            if (args <= maxArgs) {
                int line = sc.lineOf(m.start());
                if (!isAllowed(sc, line)) {
                    out.add(new Violation(path, line, "default-charset", "new " + cls + "(...) without an explicit charset depends on the platform encoding"));
                }
            }
        }
    }
}
