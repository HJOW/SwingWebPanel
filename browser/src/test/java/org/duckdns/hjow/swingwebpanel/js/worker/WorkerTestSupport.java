package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.Iterator;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Helpers of the worker tests (plan item P20-09-b): a real {@link SwingWebPanel} that loads pages from a real local HTTP server, the page
 * that reports results through the <code>sessionStorage</code> of the component, waiting for values and pages, and the counting of the threads
 * of the workers and of the web requests.
 * <p>
 * worker 시험(계획 P20-09-b)의 도구: 실제 로컬 HTTP 서버에서 페이지를 읽는 실제 {@link SwingWebPanel}, 컴포넌트의 <code>sessionStorage</code> 로 결과를
 * 보고하는 페이지, 값과 페이지 기다리기, worker 와 웹 요청의 스레드 세기.
 */
public final class WorkerTestSupport {

    /**
     * Private constructor: static helpers.
     * <p>
     * 비공개 생성자: 정적 도우미.
     */
    private WorkerTestSupport() {
    }

    /**
     * Creates the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static SwingWebPanel newPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(300, 180);
            }
        });
        return result[0];
    }

    /**
     * Reads a value that a script stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 값을 읽는다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    public static String value(SwingWebPanel panel, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until a key has a value and returns it.
     * <p>
     * 키에 값이 생길 때까지 기다리고 그 값을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static String await(SwingWebPanel panel, String key) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (value(panel, key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.notNull(value(panel, key), "a value for " + key);
        return value(panel, key);
    }

    /**
     * Waits until the component confirmed a page.
     * <p>
     * 컴포넌트가 페이지를 확정할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param url   the URL / URL
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static void awaitPage(SwingWebPanel panel, String url) throws Exception {
        long end = System.currentTimeMillis() + 60000;
        // 페이지가 확정될 때까지 짧게 쉬며 확인한다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
    }

    /**
     * Returns the origin text of a server.
     * <p>
     * 서버의 오리진 문자열을 반환한다.
     *
     * @param server the server / 서버
     * @return <code>http://localhost:port</code> / <code>http://localhost:port</code>
     */
    public static String origin(TestHttpServer server) {
        String u = server.url("/x");
        return u.substring(0, u.length() - 2);
    }

    /**
     * Wraps a script in a page that has the <code>put(key, value)</code> function (it stores a value for the test).
     * <p>
     * 스크립트를 <code>put(key, value)</code> 함수가 있는 페이지로 감싼다(시험을 위해 값을 저장한다).
     *
     * @param script the script text / 스크립트 텍스트
     * @return the page markup / 페이지 마크업
     */
    public static String page(String script) {
        return "<!doctype html><meta charset=utf-8><title>t</title><script>function put(k, v) { sessionStorage.setItem(k, String(v)); }" + script + "</"
                + "script>";
    }

    /**
     * Creates the response of a JavaScript file.
     * <p>
     * JavaScript 파일의 응답을 만든다.
     *
     * @param code the script text / 스크립트 텍스트
     * @return the response / 응답
     */
    public static TestHttpServer.Response js(String code) {
        return TestHttpServer.Response.ok("application/javascript", code);
    }

    /**
     * Serves a document that can grant isolation to its workers.
     * <p>
     * 워커에 격리 권한을 부여할 수 있는 문서를 제공한다.
     *
     * @param code the inline page script / 인라인 페이지 스크립트
     * @return the response / 응답
     */
    static TestHttpServer.Response isolatedPage(String code) {
        return TestHttpServer.Response.html(page(code)).header("Cross-Origin-Opener-Policy", "same-origin")
                .header("Cross-Origin-Embedder-Policy", "require-corp");
    }

    /**
     * Serves a worker entry that opts in to embedder isolation.
     * <p>
     * 삽입 격리를 요청하는 워커 진입 파일을 제공한다.
     *
     * @param code the worker script / 워커 스크립트
     * @return the response / 응답
     */
    static TestHttpServer.Response isolatedScript(String code) {
        return js(code).header("Cross-Origin-Embedder-Policy", "require-corp");
    }

    /**
     * Counts the live threads whose name starts with a prefix.
     * <p>
     * 이름이 접두사로 시작하는 살아 있는 스레드를 센다.
     *
     * @param prefix the name prefix / 이름 접두사
     * @return the number of threads / 스레드 수
     */
    public static int threads(String prefix) {
        int n = 0;
        Iterator<Map.Entry<Thread, StackTraceElement[]>> it = Thread.getAllStackTraces().entrySet().iterator();
        // 모든 스레드 중 이름이 접두사로 시작하는 것을 센다.
        while (it.hasNext()) {
            Thread t = it.next().getKey();
            if (t.isAlive() && t.getName().startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Counts the live worker threads (<code>swingwebpanel-worker-N</code>).
     * <p>
     * 살아 있는 worker 스레드(<code>swingwebpanel-worker-N</code>)를 센다.
     *
     * @return the number of threads / 스레드 수
     */
    public static int workerThreads() {
        return threads("swingwebpanel-worker-");
    }

    /**
     * Waits until the number of live threads with a prefix is at most a number.
     * <p>
     * 이름 접두사가 있는 살아 있는 스레드 수가 일정 수 이하가 될 때까지 기다린다.
     *
     * @param prefix the name prefix / 이름 접두사
     * @param max    the largest accepted number / 허용하는 최대 수
     * @param ms     the longest wait in milliseconds / 최대 대기 시간(밀리초)
     * @return the number at the end / 마지막의 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static int awaitThreads(String prefix, int max, long ms) throws Exception {
        long end = System.currentTimeMillis() + ms;
        // 스레드 수가 줄어들 때까지 짧게 쉬며 확인한다.
        while (threads(prefix) > max && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return threads(prefix);
    }
}
