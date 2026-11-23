package org.duckdns.hjow.swingwebpanel.testing.selftest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Guards the layout of the jars of the product (plan items P26-01 and P26-06): the browser jar <code>swingwebpanel-browser.jar</code> has the browser module and
 * none of the engine, the engine jar <code>swingwebpanel-js.jar</code> has the engine only, and the single jar <code>swingwebpanel.jar</code> (what existing users put
 * on their class path) is exactly the union of the two. Class loaders that see nothing but those jars run a page whose script logs a message, to prove that the browser
 * jar works together with the engine jar and that the single jar works alone. The paths come from the system properties <code>swp.jar</code>, <code>swp.browser.jar</code>
 * and <code>swp.engine.jar</code> (defaults in the modules' <code>target</code> directories); without the jars the cases are skipped with a reason.
 * <p>
 * 제품 JAR 의 구성을 지킨다(계획 항목 P26-01, P26-06): 브라우저 JAR <code>swingwebpanel-browser.jar</code> 에는 브라우저 모듈만 있고 엔진은 없으며, 엔진 JAR
 * <code>swingwebpanel-js.jar</code> 에는 엔진만 있고, 단일 JAR <code>swingwebpanel.jar</code>(기존 사용자가 클래스패스에 두는 것)은 정확히 둘의 합집합이다. 그 JAR 들만 보이는
 * 클래스 로더가 스크립트가 메시지를 로그로 쓰는 페이지를 실행해, 브라우저 JAR 가 엔진 JAR 와 함께 동작하고 단일 JAR 가 혼자 동작함을 증명한다. 경로는 시스템 속성
 * <code>swp.jar</code>, <code>swp.browser.jar</code>, <code>swp.engine.jar</code>(기본값은 각 모듈의 <code>target</code> 아래)에서 얻으며 JAR 가 없으면 이유와 함께 건너뛴다.
 */
public class BrowserJarsTest {

    /**
     * The package prefix of the product.
     * <p>
     * 제품의 패키지 접두사.
     */
    private static final String PREFIX = "org/duckdns/hjow/swingwebpanel/";

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public BrowserJarsTest() {
    }

    /**
     * Finds a jar or skips the case.
     * <p>
     * JAR 를 찾거나 사례를 건너뛴다.
     *
     * @param property the system property / 시스템 속성
     * @param fallback the default path / 기본 경로
     * @return the jar file / JAR 파일
     */
    private static File jar(String property, String fallback) {
        File jar = new File(System.getProperty(property, fallback));
        if (!jar.isFile()) {
            Check.skip("JAR not built: " + jar.getPath());
        }
        return jar;
    }

    /**
     * Lists the names of the files (not the directories and not the manifest) of a jar.
     * <p>
     * JAR 의 파일(디렉터리와 매니페스트 제외) 이름을 나열한다.
     *
     * @param file the jar / JAR
     * @return the names / 이름
     * @throws IOException if the jar cannot be read / JAR 를 읽을 수 없을 때
     */
    private static Set<String> names(File file) throws IOException {
        Set<String> out = new HashSet<String>();
        JarFile jf = new JarFile(file);
        try {
            Enumeration<JarEntry> entries = jf.entries();
            // 항목 중 디렉터리와 매니페스트를 뺀 파일 이름을 모은다.
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (!e.isDirectory() && !e.getName().equals("META-INF/MANIFEST.MF")) {
                    out.add(e.getName());
                }
            }
        } finally {
            jf.close();
        }
        return out;
    }

    /**
     * Tells whether a name belongs to the engine module (a class below <code>js</code> that is not one of the browser sub packages <code>host</code>, <code>dom</code>,
     * <code>worker</code>, or the Java Scripting API registration).
     * <p>
     * 이름이 엔진 모듈에 속하는지 알려 준다(<code>js</code> 아래 클래스 중 브라우저 하위 패키지 <code>host</code>, <code>dom</code>, <code>worker</code> 가 아닌 것, 또는 Java Scripting API 등록).
     *
     * @param name the entry name / 항목 이름
     * @return true for an engine entry / 엔진 항목이면 true
     */
    private static boolean isEngineEntry(String name) {
        if (name.startsWith("META-INF/services/")) {
            return true;
        }
        return name.startsWith(PREFIX + "js/") && !name.startsWith(PREFIX + "js/host/") && !name.startsWith(PREFIX + "js/dom/") && !name.startsWith(PREFIX + "js/worker/");
    }

    /**
     * The browser jar has the browser classes and not one engine class; the engine jar has the engine and not one browser class; the two are disjoint.
     * <p>
     * 브라우저 JAR 에는 브라우저 클래스만 있고 엔진 클래스는 하나도 없으며, 엔진 JAR 에는 엔진만 있고 브라우저 클래스는 하나도 없고, 둘은 겹치지 않는다.
     *
     * @throws IOException if a jar cannot be read / JAR 를 읽을 수 없을 때
     */
    public void testBrowserJarAndEngineJarAreDisjointHalves() throws IOException {
        Set<String> browser = names(jar("swp.browser.jar", "target/swingwebpanel-browser.jar"));
        Set<String> engine = names(jar("swp.engine.jar", "../engine/target/swingwebpanel-js.jar"));
        // 브라우저 JAR 의 항목 중 엔진에 속하는 것이 있는지 본다.
        for (String name : browser) {
            Check.isFalse(isEngineEntry(name), "an engine entry in the browser jar: " + name);
            Check.isFalse(name.startsWith(PREFIX + "testing") || name.startsWith(PREFIX + "examples"), "a test or example in the browser jar: " + name);
        }
        // 엔진 JAR 의 항목 중 엔진에 속하지 않는 것이 있는지 본다.
        for (String name : engine) {
            Check.isTrue(isEngineEntry(name), "a browser entry in the engine jar: " + name);
        }
        Check.isTrue(browser.contains(PREFIX + "SwingWebPanel.class"), "the component is in the browser jar");
        Check.isTrue(browser.contains(PREFIX + "js/host/PageScripts.class") || browser.contains(PREFIX + "js/dom/DomBridge.class"), "the script bindings of the browser are in the browser jar");
        Check.isTrue(engine.contains(PREFIX + "js/embed/JsEngine.class"), "the embedding API is in the engine jar");
        Set<String> common = new HashSet<String>(browser);
        common.retainAll(engine);
        Check.isTrue(common.isEmpty(), "the two jars share entries: " + common);
    }

    /**
     * The single jar is exactly the union of the browser jar and the engine jar.
     * <p>
     * 단일 JAR 는 정확히 브라우저 JAR 와 엔진 JAR 의 합집합이다.
     *
     * @throws IOException if a jar cannot be read / JAR 를 읽을 수 없을 때
     */
    public void testSingleJarIsTheUnionOfBoth() throws IOException {
        Set<String> all = names(jar("swp.jar", "target/swingwebpanel.jar"));
        Set<String> union = new HashSet<String>(names(jar("swp.browser.jar", "target/swingwebpanel-browser.jar")));
        union.addAll(names(jar("swp.engine.jar", "../engine/target/swingwebpanel-js.jar")));
        Set<String> missing = new HashSet<String>(union);
        missing.removeAll(all);
        Set<String> extra = new HashSet<String>(all);
        extra.removeAll(union);
        Check.isTrue(missing.isEmpty(), "entries of the two jars that the single jar lacks: " + missing);
        Check.isTrue(extra.isEmpty(), "entries of the single jar that neither jar has: " + extra);
        Check.isTrue(all.contains("META-INF/services/javax.script.ScriptEngineFactory"), "the single jar registers the Java Scripting API provider too");
    }

    /**
     * The browser jar together with the engine jar runs a page whose script writes to the console, and so does the single jar alone.
     * <p>
     * 브라우저 JAR 와 엔진 JAR 는 함께, 단일 JAR 는 혼자서, 스크립트가 콘솔에 쓰는 페이지를 실행한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageScriptsRunFromTheJars() throws Exception {
        File browser = jar("swp.browser.jar", "target/swingwebpanel-browser.jar");
        File engine = jar("swp.engine.jar", "../engine/target/swingwebpanel-js.jar");
        File single = jar("swp.jar", "target/swingwebpanel.jar");
        Check.equal("from the script", runPage(new URL[] { browser.toURI().toURL(), engine.toURI().toURL() }), "browser jar + engine jar");
        Check.equal("from the script", runPage(new URL[] { single.toURI().toURL() }), "the single jar alone");
    }

    /**
     * Loads the component through a class loader that sees only the given jars, shows a page whose script logs a message, and returns the message that the console
     * listener received.
     * <p>
     * 주어진 JAR 만 보이는 클래스 로더로 컴포넌트를 불러오고, 스크립트가 메시지를 로그로 쓰는 페이지를 보여 준 뒤, 콘솔 리스너가 받은 메시지를 반환한다.
     *
     * @param jars the jars / JAR
     * @return the message / 메시지
     * @throws Exception on failure / 실패 시
     */
    private static String runPage(URL[] jars) throws Exception {
        URLClassLoader loader = new URLClassLoader(jars, null);
        Class<?> panelClass = loader.loadClass("org.duckdns.hjow.swingwebpanel.SwingWebPanel");
        Class<?> listenerClass = loader.loadClass("org.duckdns.hjow.swingwebpanel.ConsoleListener");
        Check.same(loader, panelClass.getClassLoader(), "the component comes from the jars under test");
        final List<String> messages = new ArrayList<String>();
        Object listener = Proxy.newProxyInstance(loader, new Class<?>[] { listenerClass }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("consoleMessage")) {
                    Object event = args[0];
                    String text = (String) event.getClass().getMethod("getMessage").invoke(event);
                    synchronized (messages) {
                        messages.add(text);
                    }
                }
                return null;
            }
        });
        Object panel = panelClass.newInstance();
        try {
            panelClass.getMethod("addConsoleListener", listenerClass).invoke(panel, listener);
            panelClass.getMethod("setContent", String.class, String.class).invoke(panel, "<html><body><p>x</p><script>console.log('from the script');</script></body></html>", "text/html");
            long end = System.currentTimeMillis() + 20000L;
            // 메시지가 도착하거나 시간이 다 될 때까지 기다린다.
            while (System.currentTimeMillis() < end) {
                synchronized (messages) {
                    if (!messages.isEmpty()) {
                        break;
                    }
                }
                Thread.sleep(25L);
            }
        } finally {
            try {
                panelClass.getMethod("dispose").invoke(panel);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // 원인 예외가 보이도록 풀어서 던진다(리플렉션 호출은 원인을 가린다).
                throw new RuntimeException("dispose failed: " + e.getCause(), e.getCause());
            }
        }
        synchronized (messages) {
            return messages.isEmpty() ? null : messages.get(0);
        }
    }
}
