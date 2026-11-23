package org.duckdns.hjow.swingwebpanel.js.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Guards the engine jar (plan items P26-01 and P26-06): it contains the classes of the engine module and the registration of the Java Scripting
 * API provider, no class of the browser module and nothing of the tests or tools, and it works completely alone: a class loader that sees nothing but this
 * jar and the Java runtime can create an engine, run a script, and find the engine through <code>ScriptEngineManager</code>. The jar path comes from the
 * system property <code>swp.engine.jar</code> (default <code>target/swingwebpanel-js.jar</code>); without a jar the cases are skipped with a reason.
 * <p>
 * 엔진 JAR 를 지킨다(계획 항목 P26-01, P26-06): 엔진 모듈의 클래스와 Java Scripting API 제공자 등록이 들어 있고, 브라우저 모듈의 클래스나 시험, 도구의 것은 없으며,
 * 완전히 혼자 동작한다. 이 JAR 와 Java 런타임만 보이는 클래스 로더가 엔진을 만들고, 스크립트를 실행하고, <code>ScriptEngineManager</code> 로 엔진을 찾을 수 있다. JAR 경로는
 * 시스템 속성 <code>swp.engine.jar</code>(기본 <code>target/swingwebpanel-js.jar</code>)에서 얻으며 JAR가 없으면 이유와 함께 건너뛴다.
 */
public class EngineJarTest {

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
    public EngineJarTest() {
    }

    /**
     * Finds the engine jar or skips the case.
     * <p>
     * 엔진 JAR 를 찾거나 사례를 건너뛴다.
     *
     * @return the jar file / JAR 파일
     */
    private static File engineJar() {
        File jar = new File(System.getProperty("swp.engine.jar", "target/swingwebpanel-js.jar"));
        if (!jar.isFile()) {
            Check.skip("engine JAR not built: " + jar.getPath());
        }
        return jar;
    }

    /**
     * Every entry of the engine jar belongs to the engine module; the registration file names the factory.
     * <p>
     * 엔진 JAR 의 모든 항목이 엔진 모듈에 속하며, 등록 파일이 팩터리를 가리킨다.
     *
     * @throws IOException if the jar cannot be read / JAR 를 읽을 수 없을 때
     */
    public void testEngineJarHoldsOnlyTheEngineModule() throws IOException {
        JarFile jf = new JarFile(engineJar());
        try {
            int classes = 0;
            Enumeration<JarEntry> entries = jf.entries();
            // 항목을 하나씩 보며 엔진 모듈에 속하지 않는 이름이 있는지 검사한다.
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                if (name.endsWith(".class")) {
                    classes++;
                }
                if (name.startsWith(PREFIX)) {
                    Check.isTrue(name.startsWith(PREFIX + "js/"), "a class of the browser module in the engine jar: " + name);
                    Check.isFalse(name.startsWith(PREFIX + "js/host/") || name.startsWith(PREFIX + "js/dom/") || name.startsWith(PREFIX + "js/worker/"),
                            "a class of the browser module in the engine jar: " + name);
                    Check.isFalse(name.endsWith("Test.class") || name.endsWith("Tests.class"), "a test class in the engine jar: " + name);
                }
                Check.isFalse(name.startsWith(PREFIX + "testing") || name.startsWith(PREFIX + "examples"), "test or example in the engine jar: " + name);
                Check.isFalse(name.startsWith("webfetch/") || name.startsWith("sourcecheck/") || name.startsWith("uicheck/"), "development tool in the engine jar: " + name);
            }
            Check.isTrue(classes > 500, "the engine jar must contain the engine classes: " + classes);
            JarEntry service = jf.getJarEntry("META-INF/services/javax.script.ScriptEngineFactory");
            Check.notNull(service, "the provider registration is in the engine jar");
            InputStream in = jf.getInputStream(service);
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                Check.equal("org.duckdns.hjow.swingwebpanel.js.script.JsScriptEngineFactory", r.readLine().trim(), "the registered factory");
            } finally {
                in.close();
            }
            Check.notNull(jf.getEntry(PREFIX + "js/embed/JsEngine.class"), "the embedding API");
            Check.notNull(jf.getEntry(PREFIX + "js/JsRealm.class"), "the realm");
        } finally {
            jf.close();
        }
    }

    /**
     * A class loader that sees only the engine jar (and the bootstrap classes of the Java runtime) can run scripts and find the engine through the
     * Java Scripting API, and cannot see any class of the browser module.
     * <p>
     * 엔진 JAR(와 Java 런타임의 부트스트랩 클래스)만 보이는 클래스 로더가 스크립트를 실행하고 Java Scripting API 로 엔진을 찾을 수 있으며 브라우저 모듈의 클래스는 하나도 볼 수
     * 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEngineJarWorksAloneInAnIsolatedClassLoader() throws Exception {
        File jar = engineJar();
        URLClassLoader loader = new URLClassLoader(new URL[] { jar.toURI().toURL() }, null);
        Class<?> engineClass = loader.loadClass("org.duckdns.hjow.swingwebpanel.js.embed.JsEngine");
        Check.same(loader, engineClass.getClassLoader(), "the class comes from the isolated loader");
        Object engine = engineClass.newInstance();
        Method eval = engineClass.getMethod("eval", String.class);
        Check.equal(Double.valueOf(42), eval.invoke(engine, "(x => x * 2)(21)"), "a script runs in the isolated engine");
        Check.equal("a-b", eval.invoke(engine, "`${'a'}-${'b'}`"), "ES2015 template string");
        Check.throwsException(ClassNotFoundException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new URLClassLoader(new URL[] { engineJar().toURI().toURL() }, null).loadClass("org.duckdns.hjow.swingwebpanel.SwingWebPanel");
            }
        }, "the browser component is not in the engine jar");
        Check.throwsException(ClassNotFoundException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                new URLClassLoader(new URL[] { engineJar().toURI().toURL() }, null).loadClass("org.duckdns.hjow.swingwebpanel.js.host.PageScripts");
            }
        }, "the browser host classes are not in the engine jar");
        ScriptEngineManager manager = new ScriptEngineManager(loader);
        ScriptEngine found = manager.getEngineByName("swingwebpanel-js");
        Check.notNull(found, "the engine is found through the registration inside the jar");
        Check.same(loader, found.getClass().getClassLoader(), "and it is the engine of the isolated jar");
        Check.equal(Double.valueOf(3), found.eval("1 + 2"), "it evaluates");
    }
}
