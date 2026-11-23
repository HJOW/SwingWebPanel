package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies that the script bindings of the browser (the packages <code>js.host</code>, <code>js.dom</code> and <code>js.worker</code>) do not depend on another
 * JavaScript engine or on forbidden facilities (plan items P00-09 and P26-05). Since the split of the engine module (P26-01) the engine's own classes
 * are scanned by <code>EngineIndependenceTest</code> of the engine tests; this test scans the classes that stayed in the browser module with the same list of forbidden
 * names. Class files store the names they refer to as plain text in their constant pool, so a byte search finds every use. In particular nothing in the browser may
 * name <code>javax/script</code>: only the Java Scripting API provider of the engine itself does.
 * <p>
 * 브라우저의 스크립트 연결(패키지 <code>js.host</code>, <code>js.dom</code>, <code>js.worker</code>)이 다른 JavaScript 엔진이나 금지된 기능에 의존하지 않음을 확인한다(계획
 * P00-09, P26-05). 엔진 모듈을 분리한(P26-01) 뒤 엔진 자신의 클래스는 엔진 시험의 <code>EngineIndependenceTest</code> 가 검사하고, 이 시험은 브라우저 모듈에 남은 클래스를
 * 같은 금지 이름 목록으로 검사한다. 클래스 파일은 참조하는 이름을 상수 풀에 일반 텍스트로 저장하므로 바이트 검색으로 모든 사용을 찾을 수 있다. 특히 브라우저의 어떤 것도
 * <code>javax/script</code> 를 쓰면 안 된다. 쓰는 것은 엔진 자신의 Java Scripting API 제공자뿐이다.
 */
public class HostIndependenceTest {

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
    public HostIndependenceTest() {
    }

    /**
     * Finds the directory that holds the compiled classes of the product (null if they run from a jar).
     * <p>
     * 제품의 컴파일된 클래스가 있는 디렉터리를 찾는다(jar 에서 실행 중이면 null).
     *
     * @return the class root directory or null / 클래스 루트 디렉터리 또는 null
     */
    private File classRoot() {
        try {
            File f = new File(PageScripts.class.getProtectionDomain().getCodeSource().getLocation().toURI());
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
        // 하위 디렉터리는 재귀로, 클래스 파일은 바이트를 검색한다.
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                scan(f, report, count);
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
     * No class of the browser's script bindings refers to a forbidden facility.
     * <p>
     * 브라우저의 스크립트 연결의 어떤 클래스도 금지된 기능을 참조하지 않는다.
     *
     * @throws IOException on read failure / 읽기 실패 시
     */
    public void testNoForbiddenReferencesInTheBrowserScriptBindings() throws IOException {
        File root = classRoot();
        if (root == null) {
            Check.skip("the classes are not in a directory (running from a jar), nothing to scan");
        }
        StringBuilder report = new StringBuilder();
        int[] count = new int[1];
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js/host"), report, count);
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js/dom"), report, count);
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js/worker"), report, count);
        Check.isTrue(count[0] > 100, "scanned a plausible number of class files (" + count[0] + ")");
        Check.equal("", report.toString(), "forbidden references in the browser's script bindings");
    }
}
