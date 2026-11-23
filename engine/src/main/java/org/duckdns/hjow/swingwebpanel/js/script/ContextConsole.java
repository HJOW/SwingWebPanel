package org.duckdns.hjow.swingwebpanel.js.script;

import java.io.IOException;
import java.io.Writer;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;

/**
 * Sends the output of <code>console</code> and <code>print</code> of a script to the writers of the {@link ScriptContext} that is running it, as the
 * Java Scripting API expects: the levels <code>log</code>, <code>info</code> and <code>debug</code> (and <code>print</code>) go to
 * {@link ScriptContext#getWriter()}, <code>warn</code> and <code>error</code> go to {@link ScriptContext#getErrorWriter()}, one line per message. The script
 * engine sets the current context around every operation, under the lock of the engine; when no operation sets one (a call through an interface
 * implementation that the script engine returned), the default context of the script engine is used.
 * <p>
 * 스크립트의 <code>console</code> 과 <code>print</code> 출력을, Java Scripting API 가 기대하는 대로 그것을 실행 중인 {@link ScriptContext} 의 라이터로 보낸다.
 * <code>log</code>, <code>info</code>, <code>debug</code> 수준(과 <code>print</code>)은 {@link ScriptContext#getWriter()} 로, <code>warn</code>, <code>error</code> 는
 * {@link ScriptContext#getErrorWriter()} 로 가며 메시지 하나가 한 줄이다. 스크립트 엔진은 엔진의 잠금 안에서 연산마다 현재 컨텍스트를 지정하며, 어떤 연산도 지정하지
 * 않았으면(스크립트 엔진이 돌려준 인터페이스 구현을 통한 호출) 스크립트 엔진의 기본 컨텍스트를 쓴다.
 */
final class ContextConsole implements ConsoleHook {

    /**
     * The script engine whose default context is the fallback.
     * <p>
     * 기본 컨텍스트가 대체 수단인 스크립트 엔진.
     */
    private final ScriptEngine owner;

    /**
     * The context that is running a script now, or null.
     * <p>
     * 지금 스크립트를 실행 중인 컨텍스트. 없으면 null.
     */
    private volatile ScriptContext current;

    /**
     * Creates the hook.
     * <p>
     * 연결을 만든다.
     *
     * @param owner the script engine / 스크립트 엔진
     */
    ContextConsole(ScriptEngine owner) {
        this.owner = owner;
    }

    /**
     * Sets the context that runs scripts from now on.
     * <p>
     * 지금부터 스크립트를 실행하는 컨텍스트를 지정한다.
     *
     * @param context the context or null / 컨텍스트. 없으면 null
     * @return the context that was current before / 이전의 현재 컨텍스트
     */
    ScriptContext enter(ScriptContext context) {
        ScriptContext before = current;
        current = context;
        return before;
    }

    /**
     * Writes one console message as a line to the writer of the current context.
     * <p>
     * 콘솔 메시지 하나를 현재 컨텍스트의 라이터에 한 줄로 쓴다.
     *
     * @param level one of log, info, warn, error, debug / log, info, warn, error, debug 중 하나
     * @param text  the message / 메시지
     */
    public void message(String level, String text) {
        ScriptContext ctx = current != null ? current : owner.getContext();
        if (ctx == null) {
            return;
        }
        boolean error = "error".equals(level) || "warn".equals(level);
        Writer w = error ? ctx.getErrorWriter() : ctx.getWriter();
        if (w == null) {
            return;
        }
        try {
            w.write(text);
            w.write(System.getProperty("line.separator", "\n"));
            w.flush();
        } catch (IOException e) {
            // 출력 장치의 실패는 스크립트 실행을 멈추지 않는다.
            return;
        }
    }
}
