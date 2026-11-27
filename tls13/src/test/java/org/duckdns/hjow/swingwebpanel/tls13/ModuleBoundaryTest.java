package org.duckdns.hjow.swingwebpanel.tls13;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.crypto.Bytes;

/**
 * The boundary of the TLS module (plan item P32-02): its compiled classes refer only to the JRE and to the module itself, never to the
 * JavaScript engine, the browser component, AWT or Swing, internal JRE classes, process execution or native loading. Class files store
 * referenced names as plain text in their constant pool, so a byte search finds every use.
 * <p>
 * TLS 모듈의 경계(계획 P32-02): 컴파일된 클래스는 JRE 와 모듈 자신만 참조하고 JavaScript 엔진, 브라우저 컴포넌트, AWT/Swing, JRE 내부 클래스,
 * 프로세스 실행, 네이티브 로딩을 참조하지 않는다. 클래스 파일은 참조하는 이름을 상수 풀에 일반 텍스트로 저장하므로 바이트 검색으로 모든 사용을 찾을 수 있다.
 */
public class ModuleBoundaryTest {

    /** The package directory of the project. / 프로젝트의 패키지 디렉터리. */
    private static final String PROJECT = "org/duckdns/hjow/swingwebpanel/";

    /** The package directory of this module. / 이 모듈의 패키지 디렉터리. */
    private static final String MODULE = PROJECT + "tls13/";

    /** Name fragments that no class of the module may contain. / 모듈의 어떤 클래스에도 있으면 안 되는 이름 조각. */
    private static final String[] FORBIDDEN = { "java/awt/", "javax/swing/", "java/applet/", "com/sun/", "Lsun/", "sun/security/", "sun/misc/",
        "java/lang/ProcessBuilder", "java/lang/Process", "loadLibrary", "javax/script", "java/security/Security" };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ModuleBoundaryTest() {
    }

    /**
     * Finds the directory that holds the compiled classes of the module.
     * <p>
     * 모듈의 컴파일된 클래스가 있는 디렉터리를 찾는다.
     *
     * @return the class root, or null if the module runs from a jar / 클래스 루트. jar 에서 실행 중이면 null
     */
    private static File classRoot() {
        try {
            File f = new File(Bytes.class.getProtectionDomain().getCodeSource().getLocation().toURI());
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
    private static byte[] read(File f) throws IOException {
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
     * Collects the forbidden references of one class file.
     * <p>
     * 클래스 파일 하나의 금지된 참조를 모은다.
     *
     * @param name   the file name for the report / 보고에 쓸 파일 이름
     * @param text   the bytes of the class as ISO-8859-1 text / ISO-8859-1 문자열로 본 클래스 바이트
     * @param report receives the findings / 발견 내용을 받는다
     */
    private static void check(String name, String text, StringBuilder report) {
        // 금지 이름을 하나씩 찾는다.
        for (int k = 0; k < FORBIDDEN.length; k++) {
            if (text.indexOf(FORBIDDEN[k]) >= 0) {
                report.append("\n  ").append(name).append(" references ").append(FORBIDDEN[k]);
            }
        }
        int at = text.indexOf(PROJECT);
        // 프로젝트 패키지 이름이 나오는 곳마다 이 모듈의 것인지 확인한다.
        while (at >= 0) {
            if (!text.startsWith(MODULE, at)) {
                int end = Math.min(text.length(), at + PROJECT.length() + 24);
                report.append("\n  ").append(name).append(" references ").append(text.substring(at, end));
            }
            at = text.indexOf(PROJECT, at + PROJECT.length());
        }
    }

    /**
     * Scans the class files below a directory.
     * <p>
     * 디렉터리 아래의 클래스 파일을 검사한다.
     *
     * @param dir    the directory / 디렉터리
     * @param report receives the findings / 발견 내용을 받는다
     * @param count  counts the scanned files in element 0 / 0번 원소에 검사한 파일 수를 센다
     * @throws IOException on read failure / 읽기 실패 시
     */
    private static void scan(File dir, StringBuilder report, int[] count) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // 하위 디렉터리는 재귀로, 클래스 파일은 바이트를 검색한다.
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                scan(files[i], report, count);
            } else if (files[i].getName().endsWith(".class")) {
                count[0]++;
                check(files[i].getName(), new String(read(files[i]), "ISO-8859-1"), report);
            }
        }
    }

    /**
     * No class of the module refers to the engine, the browser, the display, internal JRE classes, processes or native code.
     * <p>
     * 모듈의 어떤 클래스도 엔진, 브라우저, 화면, JRE 내부 클래스, 프로세스, 네이티브 코드를 참조하지 않는다.
     *
     * @throws IOException on read failure / 읽기 실패 시
     */
    public void testModuleClassesReferOnlyToTheJreAndTheModule() throws IOException {
        File root = classRoot();
        if (root == null) {
            Check.skip("the module classes are not in a directory (running from a jar), nothing to scan");
        }
        StringBuilder report = new StringBuilder();
        int[] count = new int[1];
        scan(root, report, count);
        Check.isTrue(count[0] > 0, "scanned class files (" + count[0] + ")");
        Check.equal("", report.toString(), "forbidden references in the classes of the TLS module");
    }

    /**
     * The scan itself works: a class that names the testing package of the project and a forbidden name is reported.
     * <p>
     * 검사 자체가 동작한다: 프로젝트의 시험 패키지와 금지된 이름을 가진 클래스를 보고한다.
     */
    public void testScanDetectsReferences() {
        StringBuilder report = new StringBuilder();
        check("Sample.class", "x" + PROJECT + "testing/Check;java/awt/Color;" + MODULE + "crypto/Bytes", report);
        String text = report.toString();
        Check.contains(text, "references " + PROJECT + "testing/Check", "a class of another module is reported");
        Check.contains(text, "references java/awt/", "a display class is reported");
        Check.isFalse(text.indexOf("crypto/Bytes") >= 0, "a class of the module itself is not reported");
    }
}
