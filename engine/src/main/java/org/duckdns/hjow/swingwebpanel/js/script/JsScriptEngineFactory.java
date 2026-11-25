package org.duckdns.hjow.swingwebpanel.js.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * The factory of the Java Scripting API (JSR 223) engine targeting ECMAScript 2026. It is registered in
 * <code>META-INF/services/javax.script.ScriptEngineFactory</code> of the engine jar, so that
 * <pre>
 * ScriptEngine engine = new ScriptEngineManager().getEngineByName("swingwebpanel-js");
 * </pre>
 * finds it. <b>Names.</b> The Java runtime itself may already register an engine under the common names: JRE 6 and 7 have Rhino, JRE 8 to 14
 * have Nashorn (<code>js</code>, <code>JavaScript</code>, <code>javascript</code>, <code>ECMAScript</code>, <code>ecmascript</code>, the extension <code>js</code>, the MIME types
 * <code>application/javascript</code> and so on), and which factory <code>getEngineByName("javascript")</code> returns then depends on the order in which the
 * class path is searched. To make sure that nobody gets this engine by accident (or another engine by accident), this factory registers only
 * its <b>unique</b> identifiers: the names <code>swingwebpanel-js</code> and <code>swingwebpanel-javascript</code>, the extension <code>swjs</code>, the MIME type
 * <code>application/x-swingwebpanel-javascript</code>. A program that wants this engine under the common names (for example on a Java runtime that has
 * no engine of its own, such as Java 15 and later) starts the JVM with <code>-Dswingwebpanel.js.genericNames=true</code>; the common names, the
 * extension <code>js</code> and the common MIME types are then registered as well, and which engine <code>getEngineByName("javascript")</code> returns is up to the
 * search order of the Java runtime. This engine never delegates to Rhino, Nashorn or any other engine.
 * <p>
 * ECMAScript 2026을 대상으로 하는 Java Scripting API(JSR 223) 엔진의 팩터리. 엔진 JAR 의
 * <code>META-INF/services/javax.script.ScriptEngineFactory</code> 에 등록되어 있어 위 코드의 <code>getEngineByName("swingwebpanel-js")</code> 가 찾는다.
 * <b>이름.</b> Java 런타임이 이미 흔한 이름으로 엔진을 등록했을 수 있다. JRE 6, 7 은 Rhino, JRE 8~14 는 Nashorn 이 있고(<code>js</code>, <code>JavaScript</code>,
 * <code>javascript</code>, <code>ECMAScript</code>, <code>ecmascript</code>, 확장자 <code>js</code>, MIME <code>application/javascript</code> 등), 그때
 * <code>getEngineByName("javascript")</code> 가 어느 팩터리를 돌려주는지는 클래스패스를 찾는 순서에 달려 있다. 아무도 이 엔진을(또는 다른 엔진을) 우연히 얻지
 * 않도록 이 팩터리는 <b>고유한</b> 식별자만 등록한다: 이름 <code>swingwebpanel-js</code>, <code>swingwebpanel-javascript</code>, 확장자 <code>swjs</code>, MIME
 * <code>application/x-swingwebpanel-javascript</code>. 이 엔진을 흔한 이름으로 쓰려는 프로그램(예: 자체 엔진이 없는 Java 15 이상)은 JVM 을
 * <code>-Dswingwebpanel.js.genericNames=true</code> 로 시작한다. 그러면 흔한 이름, 확장자 <code>js</code>, 흔한 MIME 도 함께 등록되고
 * <code>getEngineByName("javascript")</code> 가 어느 엔진을 돌려주는지는 Java 런타임의 검색 순서에 달려 있다. 이 엔진은 Rhino, Nashorn 등 다른 엔진에 위임하지
 * 않는다.
 */
public final class JsScriptEngineFactory implements ScriptEngineFactory {

    /**
     * The name of the system property that adds the common names, extension and MIME types.
     * <p>
     * 흔한 이름, 확장자, MIME 을 추가하는 시스템 속성의 이름.
     */
    public static final String GENERIC_NAMES_PROPERTY = "swingwebpanel.js.genericNames";

    /**
     * The name of the engine.
     * <p>
     * 엔진의 이름.
     */
    public static final String ENGINE_NAME = "SwingWebPanel JavaScript Engine";

    /**
     * The version of the engine.
     * <p>
     * 엔진의 버전.
     */
    public static final String ENGINE_VERSION = "0.1.0";

    /**
     * The unique short name of the engine, the first of its names.
     * <p>
     * 엔진의 고유한 짧은 이름. 이름 중 첫 번째이다.
     */
    public static final String UNIQUE_NAME = "swingwebpanel-js";

    /**
     * The name of the language.
     * <p>
     * 언어의 이름.
     */
    public static final String LANGUAGE_NAME = "ECMAScript";

    /**
     * The targeted language edition: ECMA-262, 17th edition (ECMAScript 2026).
     * <p>
     * 대상 언어 판본: ECMA-262 제17판(ECMAScript 2026).
     */
    public static final String LANGUAGE_VERSION = "17";

    /**
     * The unique names of the engine.
     * <p>
     * 엔진의 고유한 이름들.
     */
    private static final String[] UNIQUE_NAMES = { UNIQUE_NAME, "swingwebpanel-javascript" };

    /**
     * The common names that are registered only with {@link #GENERIC_NAMES_PROPERTY}.
     * <p>
     * {@link #GENERIC_NAMES_PROPERTY} 가 있을 때만 등록하는 흔한 이름들.
     */
    private static final String[] GENERIC_NAMES = { "js", "JavaScript", "javascript", "ECMAScript", "ecmascript" };

    /**
     * Creates the factory (the Java Scripting API finds it through the service file; programs may also create it directly).
     * <p>
     * 팩터리를 만든다(Java Scripting API 는 서비스 파일로 찾으며, 프로그램이 직접 만들어도 된다).
     */
    public JsScriptEngineFactory() {
    }

    /**
     * Tells whether the common names are registered, by the system property {@link #GENERIC_NAMES_PROPERTY}, read at the time of the call.
     * <p>
     * 시스템 속성 {@link #GENERIC_NAMES_PROPERTY} 로 흔한 이름을 등록하는지 알려 준다. 호출하는 시점에 읽는다.
     *
     * @return true if the common names are registered / 흔한 이름을 등록하면 true
     */
    public static boolean isGenericNamesEnabled() {
        return "true".equalsIgnoreCase(System.getProperty(GENERIC_NAMES_PROPERTY));
    }

    /**
     * Returns the name of the engine.
     * <p>
     * 엔진의 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getEngineName() {
        return ENGINE_NAME;
    }

    /**
     * Returns the version of the engine.
     * <p>
     * 엔진의 버전을 반환한다.
     *
     * @return the version / 버전
     */
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    /**
     * Returns the file extensions of scripts: <code>swjs</code>, and <code>js</code> with {@link #GENERIC_NAMES_PROPERTY}.
     * <p>
     * 스크립트 파일의 확장자를 반환한다: <code>swjs</code>, {@link #GENERIC_NAMES_PROPERTY} 가 있으면 <code>js</code> 도.
     *
     * @return the extensions (not modifiable) / 확장자(수정할 수 없음)
     */
    public List<String> getExtensions() {
        List<String> list = new ArrayList<String>();
        list.add("swjs");
        if (isGenericNamesEnabled()) {
            list.add("js");
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns the MIME types of scripts: <code>application/x-swingwebpanel-javascript</code>, and the common JavaScript types with
     * {@link #GENERIC_NAMES_PROPERTY}.
     * <p>
     * 스크립트의 MIME 타입을 반환한다: <code>application/x-swingwebpanel-javascript</code>, {@link #GENERIC_NAMES_PROPERTY} 가 있으면 흔한 JavaScript 타입도.
     *
     * @return the MIME types (not modifiable) / MIME 타입(수정할 수 없음)
     */
    public List<String> getMimeTypes() {
        List<String> list = new ArrayList<String>();
        list.add("application/x-swingwebpanel-javascript");
        if (isGenericNamesEnabled()) {
            list.addAll(Arrays.asList("application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript"));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns the names of the engine: {@link #UNIQUE_NAME} first, then the other unique name, and the common names with
     * {@link #GENERIC_NAMES_PROPERTY}.
     * <p>
     * 엔진의 이름을 반환한다: {@link #UNIQUE_NAME} 이 첫째, 그 다음 다른 고유한 이름, {@link #GENERIC_NAMES_PROPERTY} 가 있으면 흔한 이름도.
     *
     * @return the names (not modifiable) / 이름(수정할 수 없음)
     */
    public List<String> getNames() {
        List<String> list = new ArrayList<String>(Arrays.asList(UNIQUE_NAMES));
        if (isGenericNamesEnabled()) {
            list.addAll(Arrays.asList(GENERIC_NAMES));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns the name of the language.
     * <p>
     * 언어의 이름을 반환한다.
     *
     * @return the name / 이름
     */
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    /**
     * Returns the version of the language.
     * <p>
     * 언어의 버전을 반환한다.
     *
     * @return the version / 버전
     */
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    /**
     * Returns a parameter of the engine: <code>javax.script.engine</code>, <code>javax.script.engine_version</code>, <code>javax.script.name</code> (the unique
     * name), <code>javax.script.language</code>, <code>javax.script.language_version</code> and <code>THREADING</code>, which is <code>MULTITHREADED</code>: an engine
     * may be used from several threads, its operations are serialized, and their effects are visible to the other threads.
     * <p>
     * 엔진의 매개변수를 반환한다: <code>javax.script.engine</code>, <code>javax.script.engine_version</code>, <code>javax.script.name</code>(고유한 이름),
     * <code>javax.script.language</code>, <code>javax.script.language_version</code>, 그리고 <code>MULTITHREADED</code> 인 <code>THREADING</code>. 엔진은 여러 스레드에서 쓸 수
     * 있고 연산은 직렬화되며 그 효과는 다른 스레드에 보인다.
     *
     * @param key the key / 키
     * @return the value, null for an unknown key / 값. 모르는 키이면 null
     * @throws NullPointerException if the key is null / 키가 null 이면
     */
    public Object getParameter(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        }
        if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        }
        if (key.equals(ScriptEngine.NAME)) {
            return UNIQUE_NAME;
        }
        if (key.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        }
        if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        }
        if (key.equals("THREADING")) {
            return "MULTITHREADED";
        }
        return null;
    }

    /**
     * Returns the text of a method call: <code>obj.method(arg1, arg2)</code>.
     * <p>
     * 메서드 호출 문장을 반환한다: <code>obj.method(arg1, arg2)</code>.
     *
     * @param obj  the object / 객체
     * @param m    the method / 메서드
     * @param args the arguments / 인수
     * @return the text / 문자열
     */
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(obj).append('.').append(m).append('(');
        // 인수를 쉼표와 공백으로 구분해 붙인다.
        for (int i = 0; args != null && i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args[i]);
        }
        return sb.append(')').toString();
    }

    /**
     * Returns a statement that prints a text: <code>print("text")</code> with the text written as a string literal.
     * <p>
     * 문자열을 출력하는 문장을 반환한다: 문자열을 리터럴로 쓴 <code>print("text")</code>.
     *
     * @param toDisplay the text / 문자열
     * @return the statement / 문장
     */
    public String getOutputStatement(String toDisplay) {
        StringBuilder sb = new StringBuilder("print(\"");
        // 문자열 리터럴 안에서 특별한 문자를 이스케이프한다.
        for (int i = 0; i < toDisplay.length(); i++) {
            char c = toDisplay.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == (char) 0x2028) {
                sb.append("\\u2028");
            } else if (c == (char) 0x2029) {
                sb.append("\\u2029");
            } else {
                sb.append(c);
            }
        }
        return sb.append("\")").toString();
    }

    /**
     * Returns a program that consists of the statements: each statement followed by a semicolon and a line break.
     * <p>
     * 문장들로 이루어진 프로그램을 반환한다: 문장마다 세미콜론과 줄바꿈이 뒤따른다.
     *
     * @param statements the statements / 문장들
     * @return the program / 프로그램
     */
    public String getProgram(String... statements) {
        StringBuilder sb = new StringBuilder();
        // 문장마다 세미콜론과 줄바꿈을 붙인다.
        for (int i = 0; statements != null && i < statements.length; i++) {
            sb.append(statements[i]).append(";\n");
        }
        return sb.toString();
    }

    /**
     * Creates a new script engine with its own, independent state.
     * <p>
     * 자신만의 독립된 상태를 가진 새 스크립트 엔진을 만든다.
     *
     * @return the new engine / 새 엔진
     */
    public ScriptEngine getScriptEngine() {
        return new JsScriptEngine(this);
    }
}
