package org.duckdns.hjow.swingwebpanel.js.script;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.duckdns.hjow.swingwebpanel.js.ParsedScript;

/**
 * A script that a {@link JsScriptEngine} parsed in advance ({@link javax.script.Compilable#compile(String)}). Parsing found the syntax errors; every
 * <code>eval</code> runs the parsed script without parsing it again, in the scope of the context that is given to <code>eval</code>. Like the engine
 * that created it, it may be used from any thread.
 * <p>
 * {@link JsScriptEngine} 이 미리 파싱한 스크립트({@link javax.script.Compilable#compile(String)}). 파싱할 때 구문 오류를 찾았으며, <code>eval</code> 은 다시 파싱하지 않고
 * 파싱된 스크립트를 <code>eval</code> 에 준 컨텍스트의 스코프에서 실행한다. 만든 엔진처럼 어느 스레드에서나 쓸 수 있다.
 */
public final class JsCompiledScript extends CompiledScript {

    /**
     * The script engine that created this object.
     * <p>
     * 이 객체를 만든 스크립트 엔진.
     */
    private final JsScriptEngine engine;

    /**
     * The parsed script.
     * <p>
     * 파싱된 스크립트.
     */
    private final ParsedScript parsed;

    /**
     * Creates the compiled script.
     * <p>
     * 컴파일된 스크립트를 만든다.
     *
     * @param engine the script engine / 스크립트 엔진
     * @param parsed the parsed script / 파싱된 스크립트
     */
    JsCompiledScript(JsScriptEngine engine, ParsedScript parsed) {
        this.engine = engine;
        this.parsed = parsed;
    }

    /**
     * Runs the script in a context.
     * <p>
     * 스크립트를 컨텍스트에서 실행한다.
     *
     * @param context the context / 컨텍스트
     * @return the result of the script; null if it has no value / 스크립트의 결과. 값이 없으면 null
     * @throws ScriptException for an error of the script / 스크립트 오류일 때
     */
    public Object eval(ScriptContext context) throws ScriptException {
        if (context == null) {
            throw new NullPointerException("context");
        }
        return engine.runParsed(context, parsed);
    }

    /**
     * Returns the script engine that created this object.
     * <p>
     * 이 객체를 만든 스크립트 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    public ScriptEngine getEngine() {
        return engine;
    }
}
