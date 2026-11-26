package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The boundary between the browser application and the component (plan item P31-01): the sources of the application import only the
 * public types in the root package of SwingWebPanel, never its internal packages (<code>core</code>, <code>js</code>,
 * <code>layout</code>, <code>net</code> and so on), so the application proves that a host can be written against the public API alone.
 * <p>
 * 브라우저 응용 프로그램과 컴포넌트 사이의 경계(계획 P31-01): 응용 프로그램의 소스는 SwingWebPanel 루트 패키지의 공개 타입만 import 하고 내부 패키지
 * (<code>core</code>, <code>js</code>, <code>layout</code>, <code>net</code> 등)는 import 하지 않는다. 그래서 이 응용 프로그램은 공개 API 만으로
 * 호스트를 만들 수 있음을 보인다.
 */
public class AppBoundaryTest {

    /** The package of the component. / 컴포넌트의 패키지. */
    private static final String PRODUCT = "org.duckdns.hjow.swingwebpanel.";

    /** The last name of the package of the application. / 응용 프로그램 패키지의 마지막 이름. */
    private static final String APP_NAME = "desktopapp";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AppBoundaryTest() {
    }

    /**
     * Collects the Java files below a directory.
     * <p>
     * 디렉터리 아래의 Java 파일을 모은다.
     *
     * @param dir the directory / 디렉터리
     * @param out the files found so far / 지금까지 찾은 파일
     */
    private static void collect(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        // 하위 디렉터리는 내려가고 Java 파일은 모은다.
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                collect(children[i], out);
            } else if (children[i].getName().endsWith(".java")) {
                out.add(children[i]);
            }
        }
    }

    /**
     * Reads the lines of a source file as UTF-8.
     * <p>
     * 소스 파일의 줄을 UTF-8 로 읽는다.
     *
     * @param file the file / 파일
     * @return the lines / 줄 목록
     * @throws IOException if the file cannot be read / 파일을 읽을 수 없으면
     */
    private static List<String> lines(File file) throws IOException {
        List<String> out = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            String line;
            // 파일 끝까지 한 줄씩 읽는다.
            while ((line = in.readLine()) != null) {
                out.add(line);
            }
        } finally {
            in.close();
        }
        return out;
    }

    /**
     * No source of the application imports or names a type of an internal package of the component.
     * <p>
     * 응용 프로그램의 어떤 소스도 컴포넌트 내부 패키지의 타입을 import 하거나 이름으로 쓰지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSourcesUseOnlyThePublicApi() throws Exception {
        File root = new File("src/main/java");
        Check.isTrue(root.isDirectory(), "the test runs in the module directory: " + root.getAbsolutePath());
        List<File> files = new ArrayList<File>();
        collect(root, files);
        Check.isTrue(files.size() > 0, "the application has sources");
        List<String> bad = new ArrayList<String>();
        // 파일마다 컴포넌트 패키지 이름이 나오는 곳을 본다.
        for (int i = 0; i < files.size(); i++) {
            List<String> text = lines(files.get(i));
            // 줄마다 이름 뒤의 조각이 내부 패키지인지 확인한다.
            for (int n = 0; n < text.size(); n++) {
                String line = text.get(n);
                int at = line.indexOf(PRODUCT);
                // 한 줄에 여러 번 나올 수 있으므로 끝까지 찾는다.
                while (at >= 0) {
                    String rest = line.substring(at + PRODUCT.length());
                    // 응용 프로그램 자신의 패키지("...desktopapp" 뒤가 이름의 일부가 아닌 경우)는 내부 패키지가 아니다.
                    boolean app = rest.startsWith(APP_NAME)
                            && (rest.length() == APP_NAME.length() || !Character.isJavaIdentifierPart(rest.charAt(APP_NAME.length())));
                    // 루트 패키지의 타입은 대문자로 시작한다. 소문자로 시작하면 하위(내부) 패키지이다.
                    if (!app && rest.length() > 0 && Character.isLowerCase(rest.charAt(0))) {
                        bad.add(files.get(i).getPath() + ":" + (n + 1) + ": " + line.trim());
                    }
                    at = line.indexOf(PRODUCT, at + PRODUCT.length());
                }
            }
        }
        Check.equal("[]", bad.toString(), "references to internal packages of the component");
    }
}
