package org.duckdns.hjow.swingwebpanel.testing.selftest;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Guards the contents of the product JAR (plan item P02-05): it may contain only product classes and resources, never test classes,
 * test resources or classes of the development tools. The JAR path comes from the system property <code>swp.jar</code> (default
 * <code>build/swingwebpanel.jar</code>); when no JAR exists the case is skipped with a reason.
 * <p>
 * 제품 JAR 의 내용을 지킨다(계획 P02-05): 제품 클래스와 리소스만 있어야 하고 시험 클래스, 시험 자료, 개발 도구 클래스는 없어야 한다.
 * JAR 경로는 시스템 속성 <code>swp.jar</code>(기본 <code>build/swingwebpanel.jar</code>)에서 얻으며, JAR 가 없으면 이유와 함께 건너뛴다.
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
        File jar = new File(System.getProperty("swp.jar", "build/swingwebpanel.jar"));
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
        } finally {
            jf.close();
        }
    }
}
