package org.duckdns.hjow.swingwebpanel.js;

/**
 * A script that has been parsed but not run yet. Create it with {@link JsRealm#parseScript(String, String)} to find syntax errors early or
 * to run the same text many times without parsing it again, and run it with {@link JsRealm#evaluateScript(ParsedScript)}. The object is
 * immutable and may be run any number of times, in the realm that created it or in another realm, but never by two threads at the
 * same moment unless the realm calls are serialized by the caller.
 * <p>
 * 파싱은 끝났지만 아직 실행하지 않은 스크립트. 구문 오류를 일찍 찾거나 같은 텍스트를 다시 파싱하지 않고 여러 번 실행하려면
 * {@link JsRealm#parseScript(String, String)} 로 만들고 {@link JsRealm#evaluateScript(ParsedScript)} 로 실행한다. 객체는 불변이며, 만든
 * realm 이나 다른 realm 에서 몇 번이든 실행할 수 있다. 다만 호출자가 realm 호출을 직렬화하지 않으면 두 스레드가 동시에 실행해서는 안 된다.
 */
public interface ParsedScript {

    /**
     * Returns the name of the script (file name or URL) that was given when it was parsed.
     * <p>
     * 파싱할 때 준 스크립트 이름(파일 이름이나 URL)을 반환한다.
     *
     * @return the name, never null / 이름. null 이 아니다
     */
    String getSourceName();
}
