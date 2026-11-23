package org.duckdns.hjow.swingwebpanel.testing.selftest;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Guards the contents of the product JAR (plan item P02-05): it may contain only product classes and resources, never test classes,
 * test resources or classes of the development tools. Since P26 the product JAR is the single JAR with the engine and the browser component together
 * (the layout of the separate engine and browser jars is checked by <code>BrowserJarsTest</code>). The JAR path comes from the system property
 * <code>swp.jar</code> (default <code>target/swingwebpanel.jar</code>); when no JAR exists the case is skipped with a reason.
 * <p>
 * 제품 JAR 의 내용을 지킨다(계획 P02-05): 제품 클래스와 리소스만 있어야 하고 시험 클래스, 시험 자료, 개발 도구 클래스는 없어야 한다.
 * P26 부터 제품 JAR 는 엔진과 브라우저 컴포넌트를 함께 가진 단일 JAR 이다(분리된 엔진 JAR 와 브라우저 JAR 의 구성은 <code>BrowserJarsTest</code> 가 확인한다).
 * JAR 경로는 시스템 속성 <code>swp.jar</code>(기본 <code>target/swingwebpanel.jar</code>)에서 얻으며, JAR가 없으면 이유와 함께 건너뛴다.
 */
public class JarContentTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public JarContentTest() {
    }

    /**
     * Checks that every entry of the product JAR belongs to the product.
     * <p>
     * 제품 JAR 의 모든 항목이 제품에 속하는지 확인한다.
     *
     * @throws IOException if the JAR cannot be read / JAR 를 읽을 수 없을 때
     */
    public void testProductJarContainsOnlyProductFiles() throws IOException {
        File jar = new File(System.getProperty("swp.jar", "target/swingwebpanel.jar"));
        if (!jar.isFile()) {
            Check.skip("product JAR not built: " + jar.getPath());
        }
        JarFile jf = new JarFile(jar);
        try {
            int classes = 0;
            Enumeration<JarEntry> entries = jf.entries();
            // 항목을 하나씩 보며 제품에 속하지 않는 이름이 있는지 검사한다.
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class")) {
                    classes++;
                }
                Check.isFalse(name.startsWith("org/duckdns/hjow/swingwebpanel/testing"), "test infrastructure in the product JAR: " + name);
                Check.isFalse(name.startsWith("webfetch/") || name.startsWith("sourcecheck/"), "development tool in the product JAR: " + name);
                Check.isFalse(name.startsWith("org/duckdns/hjow/swingwebpanel/examples"), "example in the product JAR: " + name);
                Check.isFalse(name.endsWith("Test.class") || name.endsWith("Tests.class"), "test class in the product JAR: " + name);
                Check.isFalse(name.indexOf("test-keystore") >= 0, "test resource in the product JAR: " + name);
            }
            Check.isTrue(classes > 0, "the product JAR must contain classes");
            Check.isTrue(jf.getEntry("org/duckdns/hjow/swingwebpanel/SwingWebPanel.class") != null || jf.getEntry("org/duckdns/hjow/swingwebpanel/core/Version.class") != null,
                    "the product JAR must contain product classes");
            // 합친 JAR 는 엔진(P26)과 javax.script 제공자 등록도 함께 가진다.
            Check.isTrue(jf.getEntry("org/duckdns/hjow/swingwebpanel/js/embed/JsEngine.class") != null, "the single JAR must contain the engine too");
            Check.isTrue(jf.getEntry("META-INF/services/javax.script.ScriptEngineFactory") != null, "the single JAR must register the Java Scripting API provider");
        } finally {
            jf.close();
        }
    }
}
