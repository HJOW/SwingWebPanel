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
     * Collects forbidden references in all class files below a directory. The package <code>js/script</code> is the Java Scripting API provider of the engine itself and
     * is the only one that may name <code>javax/script</code>.
     * <p>
     * 디렉터리 아래 모든 클래스 파일에서 금지된 참조를 모은다. 패키지 <code>js/script</code> 는 엔진 자체의 Java Scripting API 제공자이며 <code>javax/script</code> 를 쓸 수 있는 유일한 곳이다.
     *
     * @param dir    the directory / 디렉터리
     * @param report receives the findings / 발견 내용을 받는다
     * @param count  counts the scanned files in element 0 / 0번 원소에 검사한 파일 수를 센다
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void scan(File dir, StringBuilder report, int[] count) throws IOException {
        scan(dir, report, count, false);
    }

    /**
     * Collects forbidden references below a directory; inside the scripting provider package the name <code>javax/script</code> is allowed.
     * <p>
     * 디렉터리 아래의 금지된 참조를 모은다. 스크립팅 제공자 패키지 안에서는 <code>javax/script</code> 가 허용된다.
     *
     * @param dir       the directory / 디렉터리
     * @param report    receives the findings / 발견 내용을 받는다
     * @param count     counts the scanned files in element 0 / 0번 원소에 검사한 파일 수를 센다
     * @param scripting true inside the scripting provider package / 스크립팅 제공자 패키지 안이면 true
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void scan(File dir, StringBuilder report, int[] count, boolean scripting) throws IOException {
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
                    scan(f, report, count, scripting || (n.equals("script") && dir.getName().equals("js")));
                }
            } else if (f.getName().endsWith(".class")) {
                String text = new String(read(f), "ISO-8859-1");
                count[0]++;
                for (int k = 0; k < FORBIDDEN.length; k++) { // 금지 이름을 하나씩 찾아 본다.
                    if (scripting && FORBIDDEN[k].equals("javax/script")) {
                        continue;
                    }
                    if (text.indexOf(FORBIDDEN[k]) >= 0) {
                        report.append("\n  ").append(f.getName()).append(" references ").append(FORBIDDEN[k]);
                    }
                }
            }
        }
    }

    /**
     * Name fragments in the constant pool that tie a class to the display, the network, the files or the browser module: no class of the engine module may
     * contain one (plan item P26-01). The sub-packages of <code>js</code> that belong to the browser module (<code>host</code>, <code>dom</code>, <code>worker</code>) are not part of
     * the engine and are not scanned.
     * <p>
     * 클래스를 화면, 네트워크, 파일, 브라우저 모듈에 묶는 상수 풀의 이름 조각. 엔진 모듈의 어떤 클래스도 이것을 가질 수 없다(계획 항목 P26-01). 브라우저 모듈에 속하는
     * <code>js</code> 의 하위 패키지(<code>host</code>, <code>dom</code>, <code>worker</code>)는 엔진이 아니므로 검사하지 않는다.
     */
    private static final String[] BOUNDARY = { "java/awt/", "javax/swing/", "java/applet/", "javax/imageio/", "javax/sound/", "java/net/", "javax/net/",
        "java/io/File", "java/nio/", "java/sql/", "java/rmi/", "org/duckdns/hjow/swingwebpanel/core/", "org/duckdns/hjow/swingwebpanel/dom/",
        "org/duckdns/hjow/swingwebpanel/css/", "org/duckdns/hjow/swingwebpanel/html/", "org/duckdns/hjow/swingwebpanel/layout/",
        "org/duckdns/hjow/swingwebpanel/render/", "org/duckdns/hjow/swingwebpanel/net/", "org/duckdns/hjow/swingwebpanel/storage/",
        "org/duckdns/hjow/swingwebpanel/media/", "org/duckdns/hjow/swingwebpanel/content/", "org/duckdns/hjow/swingwebpanel/download/",
        "org/duckdns/hjow/swingwebpanel/js/host/", "org/duckdns/hjow/swingwebpanel/js/dom/", "org/duckdns/hjow/swingwebpanel/js/worker/",
        "org/duckdns/hjow/swingwebpanel/SwingWebPanel", "org/duckdns/hjow/swingwebpanel/Console", "org/duckdns/hjow/swingwebpanel/testing/" };

    /**
     * The sub-packages of <code>js</code> that make up the engine module (the classes directly in <code>js</code> belong to it too).
     * <p>
     * 엔진 모듈을 이루는 <code>js</code> 의 하위 패키지(<code>js</code> 바로 아래의 클래스도 엔진에 속한다).
     */
    private static final String[] ENGINE_DIRS = { "ast", "parser", "interp", "builtins", "date", "num", "regexp", "text", "embed", "script" };

    /**
     * Collects the references to the display, the network, the files and the browser module in the class files of the engine module.
     * <p>
     * 엔진 모듈의 클래스 파일에서 화면, 네트워크, 파일, 브라우저 모듈에 대한 참조를 모은다.
     *
     * @param dir    the directory / 디렉터리
     * @param report receives the findings / 발견 내용을 받는다
     * @param count  counts the scanned files in element 0 / 0번 원소에 검사한 파일 수를 센다
     * @throws IOException on read failure / 읽기 실패 시
     */
    private void scanBoundary(File dir, StringBuilder report, int[] count) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // 하위 디렉터리는 재귀로, 클래스 파일은 상수 풀 바이트를 검색한다.
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                scanBoundary(f, report, count);
            } else if (f.getName().endsWith(".class")) {
                String text = new String(read(f), "ISO-8859-1");
                count[0]++;
                for (int k = 0; k < BOUNDARY.length; k++) { // 경계 이름을 하나씩 찾아 본다.
                    if (text.indexOf(BOUNDARY[k]) >= 0) {
                        report.append("\n  ").append(f.getName()).append(" references ").append(BOUNDARY[k]);
                    }
                }
            }
        }
    }

    /**
     * The engine module does not depend on the display, the network, files or the browser module: no class refers to AWT or Swing, to a network or a file
     * class, or to a class of the browser module. (The engine is also compiled with an empty class path by the build, and these tests run without the browser
     * classes and with <code>java.awt.headless=true</code>.)
     * <p>
     * 엔진 모듈은 화면, 네트워크, 파일, 브라우저 모듈에 의존하지 않는다. 어떤 클래스도 AWT, Swing, 네트워크나 파일 클래스, 브라우저 모듈의 클래스를 참조하지 않는다.
     * (빌드도 엔진을 빈 클래스패스로 컴파일하며, 이 시험은 브라우저 클래스 없이 <code>java.awt.headless=true</code> 로 실행된다.)
     *
     * @throws IOException on read failure / 읽기 실패 시
     */
    public void testEngineModuleDoesNotTouchDisplayNetworkFilesOrBrowser() throws IOException {
        File root = classRoot();
        if (root == null) {
            Check.skip("the engine classes are not in a directory (running from a jar), nothing to scan");
        }
        File js = new File(root, "org/duckdns/hjow/swingwebpanel/js");
        StringBuilder report = new StringBuilder();
        int[] count = new int[1];
        File[] direct = js.listFiles();
        // js 바로 아래의 클래스 파일은 엔진에 속하므로 검사한다.
        for (int i = 0; direct != null && i < direct.length; i++) {
            if (direct[i].isFile() && direct[i].getName().endsWith(".class")) {
                String text = new String(read(direct[i]), "ISO-8859-1");
                count[0]++;
                for (int k = 0; k < BOUNDARY.length; k++) { // 경계 이름을 하나씩 찾아 본다.
                    if (text.indexOf(BOUNDARY[k]) >= 0) {
                        report.append("\n  ").append(direct[i].getName()).append(" references ").append(BOUNDARY[k]);
                    }
                }
            }
        }
        // 엔진 하위 패키지 디렉터리를 재귀로 검사한다.
        for (int i = 0; i < ENGINE_DIRS.length; i++) {
            scanBoundary(new File(js, ENGINE_DIRS[i]), report, count);
        }
        Check.isTrue(count[0] > 150, "scanned a plausible number of class files (" + count[0] + ")");
        Check.equal("", report.toString(), "references to the display, the network, files or the browser in the engine classes");
    }

    /**
     * The scripting provider package really names <code>javax/script</code> (so that the scan of the other packages is meaningful), and no package of the engine
     * outside it does.
     * <p>
     * 스크립팅 제공자 패키지는 실제로 <code>javax/script</code> 를 쓰며(그래서 다른 패키지의 검사가 의미가 있다) 그 밖의 엔진 패키지는 쓰지 않는다.
     *
     * @throws IOException on read failure / 읽기 실패 시
     */
    public void testOnlyTheProviderPackageUsesTheScriptingApi() throws IOException {
        File root = classRoot();
        if (root == null) {
            Check.skip("the engine classes are not in a directory (running from a jar), nothing to scan");
        }
        File script = new File(root, "org/duckdns/hjow/swingwebpanel/js/script");
        File[] provider = script.listFiles();
        Check.notNull(provider, "the provider package exists in the class files");
        int naming = 0;
        // 제공자 패키지의 클래스 중 javax/script 를 쓰는 것을 센다.
        for (int i = 0; i < provider.length; i++) {
            if (provider[i].getName().endsWith(".class") && new String(read(provider[i]), "ISO-8859-1").indexOf("javax/script") >= 0) {
                naming++;
            }
        }
        Check.isTrue(naming >= 3, "the provider classes name javax/script (" + naming + ")");
        StringBuilder report = new StringBuilder();
        int[] count = new int[1];
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js/embed"), report, count);
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js/interp"), report, count);
        scan(new File(root, "org/duckdns/hjow/swingwebpanel/js/builtins"), report, count);
        Check.equal("", report.toString(), "javax/script outside the provider package");
    }

    /**
     * No engine class refers to a forbidden facility.
     * <p>
     * 어떤 엔진 클래스도 금지된 기능을 참조하지 않는다.
     *
     * @throws IOException on read failure / 읽기 실패 시
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
