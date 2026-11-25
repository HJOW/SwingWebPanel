package org.duckdns.hjow.swingwebpanel.js.script;

import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of how the Java Scripting API finds the engine of this project (plan items P26-04 and P26-05): the unique name, the service file, the
 * metadata of the factory, and above all that this engine and the engines of the Java runtime (Rhino of JRE 6 and 7, Nashorn of JRE 8 to 14) are never
 * mixed up. The same suite runs on every Java runtime that is available; the result is only meaningful as a proof when the engine that answers is
 * checked to be an instance of the class of this project, which these tests do.
 * <p>
 * Java Scripting API 가 이 프로젝트의 엔진을 찾는 방법의 시험(계획 항목 P26-04, P26-05): 고유한 이름, 서비스 파일, 팩터리의 메타데이터, 그리고 무엇보다 이 엔진과 Java
 * 런타임의 엔진(JRE 6, 7 의 Rhino, JRE 8~14 의 Nashorn)이 뒤섞이지 않는 것. 같은 스위트를 쓸 수 있는 모든 Java 런타임에서 실행한다. 응답한 엔진이 이 프로젝트의 클래스의
 * 인스턴스임을 확인해야 증명이 되며, 이 시험이 그것을 확인한다.
 */
public class ScriptEngineDiscoveryTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ScriptEngineDiscoveryTest() {
    }

    /**
     * The unique name finds an engine of this project, whatever else the Java runtime offers.
     * <p>
     * 고유한 이름은 Java 런타임이 무엇을 제공하든 이 프로젝트의 엔진을 찾는다.
     */
    public void testUniqueNameFindsThisEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("swingwebpanel-js");
        Check.notNull(engine, "an engine is registered under swingwebpanel-js");
        Check.isTrue(engine instanceof JsScriptEngine, "it is an instance of the class of this project, not Rhino or Nashorn: " + engine.getClass().getName());
        Check.isTrue(engine.getFactory() instanceof JsScriptEngineFactory, "its factory is ours: " + engine.getFactory().getClass().getName());
        Check.isTrue(manager.getEngineByName("swingwebpanel-javascript") instanceof JsScriptEngine, "the second unique name");
        Check.isTrue(manager.getEngineByExtension("swjs") instanceof JsScriptEngine, "the unique extension");
        Check.isTrue(manager.getEngineByMimeType("application/x-swingwebpanel-javascript") instanceof JsScriptEngine, "the unique MIME type");
    }

    /**
     * Exactly one factory of this project is registered, and a language feature that Rhino (ECMAScript 5) and Nashorn (JRE 8, ECMAScript 5.1) do not have
     * proves who evaluates.
     * <p>
     * 이 프로젝트의 팩터리는 정확히 하나 등록되어 있고, Rhino(ECMAScript 5)와 Nashorn(JRE 8, ECMAScript 5.1)에 없는 언어 기능이 평가하는 쪽을 증명한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOnlyOurFactoryAndOurEvaluator() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        int ours = 0;
        // 등록된 팩터리 중 이 프로젝트의 것을 센다.
        for (int i = 0; i < factories.size(); i++) {
            if (factories.get(i) instanceof JsScriptEngineFactory) {
                ours++;
            }
        }
        Check.equal(1, ours, "one factory of this project among " + factories.size());
        ScriptEngine engine = manager.getEngineByName("swingwebpanel-js");
        Check.equal(Double.valueOf(42), engine.eval("(x => x * 2)(21)"), "an arrow function and the result type");
        Check.equal("a-b", engine.eval("`${'a'}-${'b'}`"), "a template literal: ECMAScript 2015 that Rhino and Nashorn of JRE 8 cannot parse");
        Check.equal(Boolean.TRUE, engine.eval("typeof Promise === 'function' && typeof Symbol === 'function' && typeof Map === 'function'"), "ES2015 built-ins");
    }

    /**
     * By default the common names, the extension <code>js</code> and the common MIME types are <b>not</b> registered, so the engine can never be
     * chosen instead of the engine of the Java runtime; with the system property they are.
     * <p>
     * 기본적으로 흔한 이름, 확장자 <code>js</code>, 흔한 MIME 은 등록되지 <b>않으므로</b> Java 런타임의 엔진 대신 이 엔진이 선택되는 일은 없다. 시스템 속성이 있으면 등록된다.
     */
    public void testCommonNamesAreOptIn() {
        String saved = System.getProperty(JsScriptEngineFactory.GENERIC_NAMES_PROPERTY);
        try {
            System.clearProperty(JsScriptEngineFactory.GENERIC_NAMES_PROPERTY);
            JsScriptEngineFactory f = new JsScriptEngineFactory();
            Check.equal("[swingwebpanel-js, swingwebpanel-javascript]", f.getNames().toString(), "unique names only");
            Check.equal("[swjs]", f.getExtensions().toString(), "unique extension only");
            Check.equal("[application/x-swingwebpanel-javascript]", f.getMimeTypes().toString(), "unique MIME type only");
            Check.isFalse(f.getNames().contains("javascript") || f.getNames().contains("js") || f.getNames().contains("ECMAScript"), "no common name");
            ScriptEngine other = new ScriptEngineManager().getEngineByName("javascript");
            Check.isFalse(other instanceof JsScriptEngine, "getEngineByName(javascript) never answers with this engine by default");
            Check.isFalse(new ScriptEngineManager().getEngineByExtension("js") instanceof JsScriptEngine, "nor getEngineByExtension(js)");
            Check.isFalse(new ScriptEngineManager().getEngineByMimeType("application/javascript") instanceof JsScriptEngine, "nor getEngineByMimeType");

            System.setProperty(JsScriptEngineFactory.GENERIC_NAMES_PROPERTY, "true");
            Check.isTrue(JsScriptEngineFactory.isGenericNamesEnabled(), "the property is read");
            Check.isTrue(f.getNames().contains("javascript") && f.getNames().contains("js") && f.getNames().contains("swingwebpanel-js"), "common names added: " + f.getNames());
            Check.equal("swingwebpanel-js", f.getNames().get(0), "the unique name stays first");
            Check.isTrue(f.getExtensions().contains("js") && f.getExtensions().contains("swjs"), "extensions: " + f.getExtensions());
            Check.isTrue(f.getMimeTypes().contains("application/javascript"), "MIME types: " + f.getMimeTypes());
        } finally {
            if (saved == null) {
                System.clearProperty(JsScriptEngineFactory.GENERIC_NAMES_PROPERTY);
            } else {
                System.setProperty(JsScriptEngineFactory.GENERIC_NAMES_PROPERTY, saved);
            }
        }
    }

    /**
     * The parameters, the helper statements and the lists of the factory follow the Java Scripting API.
     * <p>
     * 팩터리의 매개변수, 보조 문장, 목록이 Java Scripting API 를 따른다.
     */
    public void testFactoryMetadata() {
        JsScriptEngineFactory f = new JsScriptEngineFactory();
        Check.equal("SwingWebPanel JavaScript Engine", f.getEngineName(), "engine name");
        Check.equal(f.getEngineName(), f.getParameter(ScriptEngine.ENGINE), "javax.script.engine");
        Check.equal(f.getEngineVersion(), f.getParameter(ScriptEngine.ENGINE_VERSION), "javax.script.engine_version");
        Check.equal("swingwebpanel-js", f.getParameter(ScriptEngine.NAME), "javax.script.name");
        Check.equal("ECMAScript", f.getParameter(ScriptEngine.LANGUAGE), "javax.script.language");
        Check.equal("17", f.getLanguageVersion(), "the ECMAScript edition");
        Check.equal("17", f.getParameter(ScriptEngine.LANGUAGE_VERSION), "javax.script.language_version");
        Check.equal("MULTITHREADED", f.getParameter("THREADING"), "the threading model");
        Check.isNull(f.getParameter("no.such.key"), "an unknown key");
        Check.equal("obj.run(1, \"a\")", f.getMethodCallSyntax("obj", "run", "1", "\"a\""), "method call syntax");
        Check.equal("obj.run()", f.getMethodCallSyntax("obj", "run"), "a call without arguments");
        Check.equal("print(\"a\\\"b\\\\c\\nd\")", f.getOutputStatement("a\"b\\c\nd"), "the output statement quotes the text");
        Check.equal("a();\nb();\n", f.getProgram("a()", "b()"), "program");
        Check.isTrue(f.getScriptEngine() instanceof JsScriptEngine && f.getScriptEngine() != f.getScriptEngine(), "every call creates a new engine");
    }
}
