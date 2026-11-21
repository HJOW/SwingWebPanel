package org.duckdns.hjow.swingwebpanel.js;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies that the JavaScript engine does not depend on any other engine or on forbidden facilities (plan items P00-09 and
 * P12-09). The compiled classes of the engine are scanned for references to Rhino, Nashorn, GraalJS, javax.script,
 * <code>com.sun</code>/<code>sun</code> internals, process execution and native loading. Class files store referenced names as
 * plain text in their constant pool, so a byte search finds every use.
 * <p>
 * JavaScript 엔진이 다른 엔진이나 금지된 기능에 의존하지 않음을 확인한다(계획 P00-09, P12-09). 엔진의 컴파일된 클래스에서
 * Rhino, Nashorn, GraalJS, javax.script, <code>com.sun</code>/<code>sun</code> 내부 클래스, 프로세스 실행, 네이티브 로딩에 대한 참조를
 * 검색한다. 클래스 파일은 참조하는 이름을 상수 풀에 일반 텍스트로 저장하므로 바이트 검색으로 모든 사용을 찾을 수 있다.
 */
public class EngineIndependenceTest {

    /**
     * Forbidden name fragments in the constant pool.
     * <p>
     * 상수 풀에 있으면 안 되는 이름 조각.
     */
    private static final String[] FORBIDDEN = { "javax/script", "org/mozilla", "sun/org/mozilla", "jdk/nashorn", "com/oracle/truffle",
        "com/oracle/graal", "com/sun/", "Lsun/", "java/lang/ProcessBuilder", "java/lang/Process", "loadLibrary", "javafx/", "java/awt/Desktop" };

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public EngineIndependenceTest() {
    }

    /**
     * Finds the directory holding the compiled engine classes.
     * <p>
     * 컴파일된 엔진 클래스가 있는 디렉터리를 찾는다.
     *
     * @return the class root directory or null if the engine runs from a jar / 클래스 루트 디렉터리. jar 에서 실행 중이면 null
     */
    private File classRoot() {
        try {
            File f = new File(JsRealm.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isDirectory() ? f : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Reads a whole file.
     * <p>
     * 파일 전체를 읽는다.
     *
     * @param f the file / 파일
     * @return the bytes / 바이트
     * @throws IOException on read failure / 읽기 실패 시
     */
    private byte[] read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Collects forbidden references in all class files below a directory.
     * <p>
     * 디렉터리 아래 모든 클래스 파일에서 금지된 참조를 모은다.
     *
     * @param dir    the directory / 디렉터리
     * @param report receives the findings / 발견 내용을 받는다
     * @param count  counts the scanned files in element 0 / 0번 원소에 검사한 파일 수를 센다
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void scan(File dir, StringBuilder report, int[] count) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // 하위 디렉터리는 재귀로, 클래스 파일은 바이트를 검색한다. 다른 작업자의 독립 라이브러리 패키지는 제외한다.
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                String n = f.getName();
                boolean libs = n.equals("regexp") || n.equals("num") || n.equals("date") || n.equals("text");
                if (!libs) {
                    scan(f, report, count);
                }
            } else if (f.getName().endsWith(".class")) {
                String text = new String(read(f), "ISO-8859-1");
                count[0]++;
                for (int k = 0; k < FORBIDDEN.length; k++) { // 금지 이름을 하나씩 찾아 본다.
                    if (text.indexOf(FORBIDDEN[k]) >= 0) {
                        report.append("\n  ").append(f.getName()).append(" references ").append(FORBIDDEN[k]);
                    }
                }
            }
        }
    }

    /**
     * No engine class refers to a forbidden facility.
     * <p>
     * 어떤 엔진 클래스도 금지된 기능을 참조하지 않는다.
     */
    public void testNoForbiddenReferences() throws IOException {
        File root = classRoot();
        if (root == null) {
            Check.skip("the engine classes are not in a directory (running from a jar), nothing to scan");
        }
        StringBuilder report = new StringBuilder();
        int[] count = new int[1];
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js"), report, count);
        Check.isTrue(count[0] > 20, "scanned a plausible number of class files (" + count[0] + ")");
        Check.equal("", report.toString(), "forbidden references in the engine classes");
    }

    /**
     * The scan itself works: a class that really refers to a forbidden name is detected.
     * <p>
     * 검사 자체가 동작한다: 금지된 이름을 실제로 참조하는 클래스를 찾아낸다.
     */
    public void testScanDetectsReferences() throws IOException {
        File root;
        try {
            root = new File(EngineIndependenceTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            root = null;
        }
        if (root == null || !root.isDirectory()) {
            Check.skip("the test classes are not in a directory (running from a jar), nothing to scan");
        }
        File self = new File(root, "org/duckdns/hjow/swingwebpanel/js/EngineIndependenceTest.class");
        String text = new String(read(self), "ISO-8859-1");
        Check.isTrue(text.indexOf("javax/script") >= 0, "this test class names javax/script in its constant pool, so the byte search must find it");
    }
}
