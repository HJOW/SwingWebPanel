package org.duckdns.hjow.swingwebpanel.js;

/**
 * A module and all the modules it depends on, after they have been fetched and parsed but before anything ran. A host loads the graph
 * first, so that a failure while fetching or parsing (the "graph could not be fetched" case of the HTML Standard, which fires
 * <code>error</code> at the script element) can be told apart from an error that the modules throw while they run (which is only
 * reported). Link and evaluation happen in {@link #evaluate()}; a graph is evaluated once, and modules that several graphs share are
 * evaluated once for the whole realm.
 * <p>
 * 모듈과 그 모듈이 의존하는 모든 모듈을, 가져와 파싱한 뒤 아무것도 실행하기 전의 상태로 나타낸다. 호스트가 그래프를 먼저 불러오면, 가져오기나
 * 파싱 중의 실패(HTML 표준의 "그래프를 가져올 수 없음" 경우이며 script 요소에 <code>error</code> 를 보낸다)를 모듈이 실행되며 던지는 오류(보고만
 * 한다)와 구별할 수 있다. 링크와 평가는 {@link #evaluate()} 에서 하며, 그래프는 한 번 평가되고 여러 그래프가 공유하는 모듈은 realm 전체에서 한 번만
 * 평가된다.
 */
public interface ModuleGraph {

    /**
     * Links and evaluates the graph and returns the namespace object of its entry module.
     * <p>
     * 그래프를 링크하고 평가한 뒤 진입 모듈의 네임스페이스 객체를 반환한다.
     *
     * @return the namespace object / 네임스페이스 객체
     */
    JsObject evaluate();
}
