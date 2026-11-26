package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

    /** Paths of the script bindings inspected by this test. / 이 시험이 검사하는 스크립트 연결 경로. */
    private static final String[] BINDINGS = { "org/duckdns/hjow/swingwebpanel/js/host/", "org/duckdns/hjow/swingwebpanel/js/dom/",
        "org/duckdns/hjow/swingwebpanel/js/worker/" };

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public HostIndependenceTest() {
    }

    /**
     * Finds the JAR or directory that supplied the product class.
     * <p>
     * 제품 클래스를 제공한 JAR 또는 디렉터리를 찾는다.
     *
     * @return class source / 클래스 원천
     * @throws IOException if its location cannot be read / 위치를 읽을 수 없으면
     */
    private File classSource() throws IOException {
        try {
            return new File(PageScripts.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new IOException("cannot find the browser class source", e);
        }
    }

    /**
     * Reads one class and closes its stream.
     * <p>
     * 클래스 하나를 읽고 스트림을 닫는다.
     *
     * @param in class input / 클래스 입력
     * @return the bytes / 바이트
     * @throws IOException on read failure / 읽기 실패 시
     */
    private byte[] read(InputStream in) throws IOException {
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
                scanClass(f.getName(), new FileInputStream(f), report, count);
            }
        }
    }

    /**
     * Scans script binding classes inside the packaged product. / 패키징된 제품의 스크립트 연결 클래스를 검사한다.
     * @param file product JAR / 제품 JAR
     * @param report findings / 발견 사항
     * @param count running class count / 검사한 클래스 수
     * @throws IOException on reading failure / 읽기 실패 시
     */
    private void scanJar(File file, StringBuilder report, int[] count) throws IOException {
        JarFile jar = new JarFile(file);
        try {
            Enumeration<JarEntry> entries = jar.entries();
            // 패키징된 JAR에서 대상 패키지의 클래스만 검사한다.
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }
                // 동일한 경계 목록을 디렉터리 검사와 공유한다.
                for (String binding : BINDINGS) {
                    if (name.startsWith(binding)) {
                        scanClass(name, jar.getInputStream(entry), report, count);
                        break;
                    }
                }
            }
        } finally {
            jar.close();
        }
    }

    /**
     * Looks for forbidden references in a single class. / 클래스 하나의 금지 참조를 찾는다.
     * @param name class path / 클래스 경로
     * @param in class byte stream / 클래스 바이트 스트림
     * @param report findings / 발견 사항
     * @param count running class count / 검사한 클래스 수
     * @throws IOException on reading failure / 읽기 실패 시
     */
    private void scanClass(String name, InputStream in, StringBuilder report, int[] count) throws IOException {
        String text = new String(read(in), "ISO-8859-1");
        count[0]++;
        // 같은 목록으로 모든 클래스의 바이트를 확인한다.
        for (String forbidden : FORBIDDEN) {
            if (text.indexOf(forbidden) >= 0) {
                report.append("\n  ").append(name).append(" references ").append(forbidden);
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
        File source = classSource();
        StringBuilder report = new StringBuilder();
        int[] count = new int[1];
        if (source.isDirectory()) {
            // 개별 클래스 파일로 실행하는 경우에도 같은 패키지를 검사한다.
            for (String binding : BINDINGS) {
                scan(new File(source, binding), report, count);
            }
        } else {
            scanJar(source, report, count);
        }
        Check.isTrue(count[0] > 100, "scanned a plausible number of class files (" + count[0] + ")");
        Check.equal("", report.toString(), "forbidden references in the browser's script bindings");
    }
}
