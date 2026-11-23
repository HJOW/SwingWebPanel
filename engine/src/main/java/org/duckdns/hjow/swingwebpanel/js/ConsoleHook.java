package org.duckdns.hjow.swingwebpanel.js;

/**
 * The host hook that receives output of the script's <code>console</code> object and the test-only <code>print</code> function.
 * The host decides where the text goes (a log, a developer console panel, standard output).
 * <p>
 * 스크립트의 <code>console</code> 객체와 시험용 <code>print</code> 함수의 출력을 받는 호스트 연결 지점. 텍스트를 어디로 보낼지(로그,
 * 개발자 콘솔 창, 표준 출력)는 호스트가 정한다.
 */
public interface ConsoleHook {

    /**
     * Receives one console message.
     * <p>
     * 콘솔 메시지 하나를 받는다.
     *
     * @param level one of "log", "info", "warn", "error", "debug" / "log", "info", "warn", "error", "debug" 중 하나
     * @param text  the message text (arguments already converted and joined by spaces) / 메시지 텍스트(인수를 변환해 공백으로 이은 것)
     */
    void message(String level, String text);
}
