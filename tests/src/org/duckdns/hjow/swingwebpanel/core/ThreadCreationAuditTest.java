package org.duckdns.hjow.swingwebpanel.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Source inspection of every place where the product code starts a thread (plan item P21-05): a thread that is not a daemon thread keeps the
 * JVM alive after the application ended and is a root that holds everything it refers to for ever, so every source file that creates a
 * thread directly must make its threads daemon threads and must give them a name that identifies the owner (<code>swingwebpanel-...</code>),
 * which also lets the leak tests count them. Thread pools are checked through their thread factories in the same way. The test reads the
 * sources below <code>src/org/duckdns/hjow/swingwebpanel</code> of the working directory and is skipped if they are not there.
 * <p>
 * 제품 코드가 스레드를 시작하는 모든 곳의 소스 점검(계획 P21-05): 데몬이 아닌 스레드는 응용이 끝난 뒤에도 JVM 을 살려 두고 자기가 참조하는 모든 것을 영원히
 * 붙잡는 뿌리이므로, 스레드를 직접 만드는 모든 소스 파일은 스레드를 데몬으로 만들고 소유자를 알 수 있는 이름(<code>swingwebpanel-...</code>)을 붙여야 하며,
 * 이름은 누수 시험이 스레드를 세는 데에도 쓰인다. 스레드 풀은 스레드 팩터리로 같은 방식으로 점검한다. 시험은 작업 디렉터리의
 * <code>src/org/duckdns/hjow/swingwebpanel</code> 아래 소스를 읽으며 없으면 건너뛴다.
 */
public class ThreadCreationAuditTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ThreadCreationAuditTest() {
    }

    /**
     * Collects the Java sources below a directory.
     * <p>
     * 디렉터리 아래의 자바 소스를 모은다.
     *
     * @param dir the directory / 디렉터리
     * @param out receives the files / 파일을 받는다
     */
    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        // 하위 디렉터리는 재귀로, 자바 소스는 목록에 넣는다.
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".java")) {
                out.add(f);
            }
        }
    }

    /**
     * Reads a source file as UTF-8 text.
     * <p>
     * 소스 파일을 UTF-8 문자열로 읽는다.
     *
     * @param f the file / 파일
     * @return the text / 문자열
     * @throws IOException on failure / 실패 시
     */
    private static String read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 파일을 끝까지 읽는다.
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /**
     * Every source that creates a thread makes it a daemon thread and names it with the <code>swingwebpanel-</code> prefix.
     * <p>
     * 스레드를 만드는 모든 소스는 스레드를 데몬으로 만들고 <code>swingwebpanel-</code> 접두사로 이름을 붙인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEveryThreadIsADaemonWithAnOwnerName() throws Exception {
        File root = new File("src/org/duckdns/hjow/swingwebpanel");
        if (!root.isDirectory()) {
            Check.skip("the sources are not below the working directory: " + root.getAbsolutePath());
        }
        List<File> sources = new ArrayList<File>();
        collect(root, sources);
        Check.isTrue(sources.size() > 500, "the sources were found: " + sources.size());
        List<String> missingDaemon = new ArrayList<String>();
        List<String> missingName = new ArrayList<String>();
        int creators = 0;
        // 소스마다 스레드를 직접 만드는지 보고, 만든다면 데몬 지정과 이름 접두사가 있는지 확인한다.
        for (File f : sources) {
            String text = read(f);
            boolean creates = text.indexOf("new Thread(") >= 0 || text.indexOf("new Thread(\"") >= 0 || text.indexOf("extends Thread") >= 0;
            if (!creates) {
                continue;
            }
            creators++;
            if (text.indexOf("setDaemon(true)") < 0) {
                missingDaemon.add(f.getPath());
            }
            if (text.indexOf("\"swingwebpanel-") < 0) {
                missingName.add(f.getPath());
            }
        }
        Check.isTrue(creators >= 8, "the thread creating sources were found: " + creators);
        Check.isTrue(missingDaemon.isEmpty(), "sources that start threads without setDaemon(true): " + missingDaemon);
        Check.isTrue(missingName.isEmpty(), "sources that start threads without a swingwebpanel- name: " + missingName);
    }
}
