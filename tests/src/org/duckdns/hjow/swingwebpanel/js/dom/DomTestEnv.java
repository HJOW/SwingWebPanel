package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * A small test environment: a standard realm, a {@link DomBridge} with the DOM interfaces, and a parsed HTML document that is visible to
 * scripts as the global <code>document</code>. Console output and script errors are recorded for the assertions.
 * <p>
 * 작은 시험 환경: 표준 realm, DOM 인터페이스가 설치된 {@link DomBridge}, 그리고 스크립트에 전역 <code>document</code> 로 보이는 해석된 HTML 문서.
 * 콘솔 출력과 스크립트 오류는 검증을 위해 기록한다.
 */
public final class DomTestEnv {

    /**
     * The realm.
     * <p>
     * 시험용 realm(JavaScript 실행 영역).
     */
    public final JsRealm realm = JsRealm.create();

    /**
     * The bridge.
     * <p>
     * 브리지.
     */
    public final DomBridge bridge = new DomBridge(realm);

    /**
     * The document.
     * <p>
     * 문서.
     */
    public final Document document;

    /**
     * Console lines as <code>level: text</code>.
     * <p>
     * 콘솔 줄(<code>level: text</code>).
     */
    public final List<String> console = new ArrayList<String>();

    /**
     * Reported script errors.
     * <p>
     * 보고된 스크립트 오류.
     */
    public final List<String> errors = new ArrayList<String>();

    /**
     * Creates the environment with a document parsed from markup.
     * <p>
     * 마크업에서 해석한 문서로 환경을 만든다.
     *
     * @param html the page source / 페이지 원문
     */
    public DomTestEnv(String html) {
        document = Document.createHtmlDocument();
        document.setMarkupSupport(new HtmlMarkupSupport());
        CssSelectorEngine.install(document);
        document.setUrl("http://example.test/dir/page.html");
        new HtmlParser(document, html).run();
        realm.setConsole(new ConsoleHook() {
            /**
             * Records a console line.
             * <p>
             * 콘솔 줄을 기록한다.
             *
             * @param level the level / 수준
             * @param text  the text / 문자열
             */
            public void message(String level, String text) {
                console.add(level + ": " + text);
            }
        });
        bridge.setErrorSink(new DomBridge.ErrorSink() {
            /**
             * Records an error.
             * <p>
             * 오류를 기록한다.
             *
             * @param error the error / 오류
             */
            public void scriptError(RuntimeException error) {
                errors.add(String.valueOf(error.getMessage()));
            }
        });
        DomInterfaces.install(bridge);
        realm.getGlobalObject().defineBuiltin("document", bridge.wrap(document));
    }

    /**
     * Runs a script and returns the completion value converted to a string.
     * <p>
     * 스크립트를 실행하고 완료 값을 문자열로 바꿔 반환한다.
     *
     * @param source the script / 스크립트
     * @return the result as text / 문자열로 바꾼 결과
     */
    public String eval(String source) {
        JsValue v = realm.evaluateScript(source, "test");
        realm.runJobs();
        return Ops.toString(v);
    }

    /**
     * Runs a script that must throw and returns the message of the error.
     * <p>
     * 반드시 던져야 하는 스크립트를 실행하고 오류 메시지를 반환한다.
     *
     * @param source the script / 스크립트
     * @return the message / 메시지
     */
    public String evalError(String source) {
        try {
            realm.evaluateScript(source, "test");
        } catch (JsException e) {
            return e.getMessage();
        }
        return null;
    }
}
