package sourcecheck;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Self test of the checker: it writes known-bad and known-good sample files into a temporary directory, runs the checker
 * and verifies that every expected rule fires for the bad samples and that the good sample is clean. This proves that the
 * checker really fails (exit code 1) on violations instead of silently passing.
 * <p>
 * 검사기의 자체 시험: 알려진 불량/양호 샘플 파일을 임시 디렉터리에 쓰고 검사기를 돌려, 불량 샘플에서 기대한 규칙이 모두 발생하고
 * 양호 샘플은 깨끗한지 확인한다. 위반이 있을 때 검사기가 조용히 통과하지 않고 실제로 실패(종료 코드 1)함을 증명한다.
 */
final class SelfTest {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SelfTest() {
    }

    /**
     * Runs the self test.
     * <p>
     * 자체 시험을 실행한다.
     *
     * @param out report stream / 보고 스트림
     * @param err error stream / 오류 스트림
     * @return 0 if the checker behaves as expected, otherwise 1 / 검사기가 기대대로 동작하면 0, 아니면 1
     */
    static int run(PrintStream out, PrintStream err) {
        File dir = null;
        try {
            dir = File.createTempFile("sourcecheck", "");
            dir.delete();
            dir.mkdirs();
            write(dir, "bad/Bad.java", BAD_JAVA);
            write(dir, "good/Good.java", GOOD_JAVA);
            write(dir, "bad/crlf.sh", "echo hi\r\n");
            write(dir, "bad/native.dll", "x");
            SourceCheck.writeAll(new File(dir, "bad/utf8.bat"), "echo 한글\r\n".getBytes("UTF-8"));
            SourceCheck.writeAll(new File(dir, "bad/lf.bat"), "echo hi\n".getBytes("US-ASCII"));
            SourceCheck.writeAll(new File(dir, "bad/broken.md"), new byte[] { 'a', (byte) 0xFF, 'b' });
            SourceCheck.writeAll(new File(dir, "bad/Bom.java"), concat(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF },
                    "/** English text here. 한글 설명. */ class Bom { }\n".getBytes("UTF-8")));
            // 엔진 모듈 경계(P26): 엔진 소스가 AWT, 파일, 브라우저 패키지, javax.script 를 쓰는 불량 샘플과 제공자 패키지의 양호 샘플.
            write(dir, "bad/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/Leak.java", BAD_ENGINE_JAVA);
            write(dir, "bad/engine/src/test/java/org/duckdns/hjow/swingwebpanel/js/LeakTest.java", BAD_ENGINE_TEST_JAVA);
            write(dir, "good/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/script/Provider.java", GOOD_ENGINE_JAVA);

            String badReport = capture(dir, "bad");
            String goodReport = capture(dir, "good");
            Set<String> missing = new HashSet<String>();
            String[] expected = { "bad/Bad.java:", "[javadoc]", "[javadoc-lang]", "[loop-comment]", "[forbidden-engine]", "[forbidden-native]",
                "[forbidden-process]", "[java6-syntax]", "[default-charset]", "bad/crlf.sh:1: [sh-crlf]", "[native-artifact]", "[bat-utf8]",
                "[bat-crlf]", "[utf8]", "[bom]",
                "bad/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/Leak.java:3: [engine-boundary]",
                "bad/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/Leak.java:4: [engine-boundary]",
                "bad/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/Leak.java:5: [engine-boundary]",
                "bad/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/Leak.java:6: [engine-boundary]",
                "bad/engine/src/main/java/org/duckdns/hjow/swingwebpanel/js/Leak.java:7: [forbidden-engine]",
                "bad/engine/src/test/java/org/duckdns/hjow/swingwebpanel/js/LeakTest.java:3: [engine-boundary]",
                "bad/engine/src/test/java/org/duckdns/hjow/swingwebpanel/js/LeakTest.java:4: [engine-boundary]" };
            // 기대한 규칙이 불량 샘플 보고서에 모두 있는지 확인한다.
            for (int i = 0; i < expected.length; i++) {
                if (badReport.indexOf(expected[i]) < 0) {
                    missing.add(expected[i]);
                }
            }
            boolean ok = true;
            if (!missing.isEmpty()) {
                err.println("selftest FAILED: expected violations not reported: " + missing);
                err.println(badReport);
                ok = false;
            }
            if (goodReport.indexOf("0 violation(s)") < 0) {
                err.println("selftest FAILED: the good sample is not clean:");
                err.println(goodReport);
                ok = false;
            }
            int badExit = SourceCheck.run(new String[] { "--root", dir.getPath(), "bad" }, new PrintStream(new ByteArrayOutputStream()), err);
            int goodExit = SourceCheck.run(new String[] { "--root", dir.getPath(), "good" }, new PrintStream(new ByteArrayOutputStream()), err);
            if (badExit != SourceCheck.EXIT_VIOLATIONS || goodExit != SourceCheck.EXIT_CLEAN) {
                err.println("selftest FAILED: exit codes bad=" + badExit + " good=" + goodExit + " (expected 1 and 0)");
                ok = false;
            }
            if (ok) {
                out.println("selftest OK: bad samples fail with exit code 1, good sample passes with exit code 0");
            }
            return ok ? 0 : 1;
        } catch (IOException e) {
            err.println("selftest error: " + e);
            return 1;
        } finally {
            if (dir != null) {
                deleteTree(dir);
            }
        }
    }

    /**
     * Runs the checker on a sub-directory and returns the report text.
     * <p>
     * 하위 디렉터리에 검사기를 돌려 보고 문자열을 돌려준다.
     *
     * @param dir base directory / 기준 디렉터리
     * @param sub sub-directory name / 하위 디렉터리 이름
     * @return report / 보고 문자열
     * @throws IOException on encoding failure / 인코딩 실패 시
     */
    private static String capture(File dir, String sub) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos, true, "UTF-8");
        SourceCheck.run(new String[] { "--root", dir.getPath(), sub }, ps, ps);
        ps.flush();
        return bos.toString("UTF-8");
    }

    /**
     * Writes a UTF-8 text sample.
     * <p>
     * UTF-8 텍스트 샘플을 쓴다.
     *
     * @param dir  base directory / 기준 디렉터리
     * @param name relative file name / 상대 파일 이름
     * @param text content / 내용
     * @throws IOException on write failure / 쓰기 실패 시
     */
    private static void write(File dir, String name, String text) throws IOException {
        SourceCheck.writeAll(new File(dir, name), text.getBytes("UTF-8"));
    }

    /**
     * Concatenates two byte arrays.
     * <p>
     * 바이트 배열 두 개를 이어 붙인다.
     *
     * @param a first / 앞
     * @param b second / 뒤
     * @return joined array / 이어 붙인 배열
     */
    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /**
     * Deletes a directory tree.
     * <p>
     * 디렉터리 트리를 삭제한다.
     *
     * @param f file or directory / 파일 또는 디렉터리
     */
    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            // 하위 항목을 먼저 지운다.
            for (int i = 0; i < children.length; i++) {
                deleteTree(children[i]);
            }
        }
        f.delete();
    }

    /**
     * Sample with many violations.
     * <p>
     * 위반이 많은 샘플.
     */
    private static final String BAD_JAVA =
            "package bad;\n"
            + "import javax.script.ScriptEngine;\n"
            + "/** English only description text. */\n"
            + "public class Bad {\n"
            + "    public int x;\n"
            + "    void m(java.util.List<String> l) throws Exception {\n"
            + "        for (int i = 0; i < 3; i++) { }\n"
            + "        Runnable r = () -> { };\n"
            + "        java.util.List<String> d = new java.util.ArrayList<>();\n"
            + "        new java.io.FileReader(\"x\");\n"
            + "        Runtime.getRuntime().exec(\"ls\");\n"
            + "    }\n"
            + "    native void nat();\n"
            + "}\n";

    /**
     * Sample of an engine source that leaks into the browser, the display, the network, the files and the scripting API.
     * <p>
     * 브라우저, 화면, 네트워크, 파일, 스크립팅 API 로 새는 엔진 소스 샘플.
     */
    private static final String BAD_ENGINE_JAVA =
            "package org.duckdns.hjow.swingwebpanel.js;\n"
            + "/** English only description text. */\n"
            + "import java.awt.Color;\n"
            + "import java.net.URL;\n"
            + "import org.duckdns.hjow.swingwebpanel.core.BrowserSession;\n"
            + "import org.duckdns.hjow.swingwebpanel.js.host.Window;\n"
            + "import javax.script.ScriptEngineManager;\n"
            + "class Leak { }\n";

    /**
     * Sample of an engine test that leaks into the display and the browser (the test runner package and files are allowed).
     * <p>
     * 화면과 브라우저로 새는 엔진 시험 샘플(시험 실행기 패키지와 파일은 허용된다).
     */
    private static final String BAD_ENGINE_TEST_JAVA =
            "package org.duckdns.hjow.swingwebpanel.js;\n"
            + "/** English only description text. */\n"
            + "import javax.swing.JPanel;\n"
            + "import org.duckdns.hjow.swingwebpanel.SwingWebPanel;\n"
            + "import org.duckdns.hjow.swingwebpanel.testing.Check;\n"
            + "import java.io.File;\n"
            + "class LeakTest { }\n";

    /**
     * Sample of an engine provider source that may use the scripting API and the standard data API.
     * <p>
     * 스크립팅 API 와 표준 데이터 API 를 써도 되는 엔진 제공자 소스 샘플.
     */
    private static final String GOOD_ENGINE_JAVA =
            "package org.duckdns.hjow.swingwebpanel.js.script;\n"
            + "import java.io.Reader;\n"
            + "import java.util.Map;\n"
            + "import javax.script.ScriptEngineManager;\n"
            + "import org.duckdns.hjow.swingwebpanel.js.JsRealm;\n"
            + "/**\n * Provider sample.\n * <p>\n * 제공자 샘플.\n */\n"
            + "class Provider { }\n";

    /**
     * Compliant sample.
     * <p>
     * 규칙을 지키는 샘플.
     */
    private static final String GOOD_JAVA =
            "package good;\n"
            + "/**\n * Sample class used by the self test.\n * <p>\n * 자체 시험에서 쓰는 샘플 클래스.\n */\n"
            + "public class Good {\n"
            + "    /**\n     * Counter value.\n     * <p>\n     * 카운터 값.\n     */\n"
            + "    private int counter;\n"
            + "    /**\n     * Adds numbers up to n.\n     * <p>\n     * n 까지의 수를 더한다.\n     *\n     * @param n limit / 상한\n     * @return sum / 합\n     */\n"
            + "    public int sum(int n) {\n"
            + "        int s = 0;\n"
            + "        // 1 부터 n 까지 반복하여 더한다.\n"
            + "        for (int i = 1; i <= n; i++) {\n"
            + "            s += i;\n"
            + "        }\n"
            + "        counter = s;\n"
            + "        return s;\n"
            + "    }\n"
            + "}\n";
}
