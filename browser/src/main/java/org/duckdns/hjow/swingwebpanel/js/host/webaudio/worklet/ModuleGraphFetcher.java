package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.ModuleRequest;
import org.duckdns.hjow.swingwebpanel.js.ast.Node;
import org.duckdns.hjow.swingwebpanel.js.ast.ProgramInfo;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleScriptSource.Credentials;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleSources;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptSource;
import org.duckdns.hjow.swingwebpanel.js.host.ScriptText;
import org.duckdns.hjow.swingwebpanel.js.parser.Parser;

/**
 * Fetches the whole graph of a module before any of it runs (the HTML Worklets step "fetch a worklet script graph"): the entry module is fetched through the request layer of the
 * component, parsed with the parser of the engine (which needs no realm) to find what it imports, and every import is fetched the same way, until the graph is complete. The
 * work is done on the worklet thread <em>outside</em> the lock of the script realm, so a slow network never keeps the rendering thread from running a processor, and the realm
 * only has to parse and evaluate texts that are already there.
 * <p>
 * 모듈 그래프 전체를 실행에 앞서 가져온다(HTML Worklets 의 "fetch a worklet script graph" 단계): 진입 모듈을 컴포넌트의 요청 계층으로 가져오고, 엔진의 파서(realm 이 필요 없다)로 파싱해 import 하는
 * 것을 찾고, 그래프가 완성될 때까지 import 를 같은 방법으로 가져온다. 이 일은 worklet 스레드에서 스크립트 realm 의 잠금 <em>밖에서</em> 하므로, 느린 네트워크가 렌더링 스레드가 프로세서를
 * 실행하는 것을 막지 않으며, realm 은 이미 있는 텍스트를 파싱하고 평가하기만 하면 된다.
 */
final class ModuleGraphFetcher {

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private ModuleGraphFetcher() {
    }

    /**
     * Fetches the entry module and everything it imports into the maps. Modules whose keys are in <code>known</code> are already in the script realm and are not fetched again.
     * <p>
     * 진입 모듈과 그것이 import 하는 모든 것을 맵으로 가져온다. 키가 <code>known</code> 에 있는 모듈은 이미 스크립트 realm 에 있으므로 다시 가져오지 않는다.
     *
     * @param source      the script source / 스크립트 공급원
     * @param entry       the resolved request for the entry module / 해석된 진입 모듈 요청
     * @param credentials fetch credentials mode / 가져오기 자격 증명 모드
     * @param texts       receives the text of every module that was fetched, by key / 가져온 모든 모듈의 텍스트를 키별로 받는다
     * @param bases       receives the final address of every module that was fetched, by key / 가져온 모든 모듈의 최종 주소를 키별로 받는다
     * @param known       the keys of the modules that the realm has / realm 이 가진 모듈의 키
     * @throws IOException if a module cannot be fetched or the graph is too large / 모듈을 가져올 수 없거나 그래프가 너무 크면
     * @throws JsException  a <code>SyntaxError</code> of a module, or the <code>TypeError</code> of a specifier that cannot be resolved / 모듈의 <code>SyntaxError</code> 또는 해석할
     *                      수 없는 지정자의 <code>TypeError</code>
     */
    static void fetch(ScriptSource source, ModuleRequest entry, Credentials credentials, Map<ModuleRequest, String> texts,
            Map<String, String> bases, Set<ModuleRequest> known)
            throws IOException {
        LinkedList<ModuleRequest> work = new LinkedList<ModuleRequest>();
        work.add(entry);
        // 가져올 모듈이 없어질 때까지 하나씩 가져와 파싱하고 import 하는 모듈을 대기열에 더한다.
        while (!work.isEmpty()) {
            ModuleRequest request = work.removeFirst();
            if (texts.containsKey(request) || known.contains(request)) {
                continue;
            }
            if (texts.size() >= WorkletLimits.MAX_MODULES_PER_LOAD) {
                throw new IOException("The module graph has more than " + WorkletLimits.MAX_MODULES_PER_LOAD + " modules.");
            }
            ScriptText loaded = ModuleSources.fetch(source, request, credentials, false);
            texts.put(request, loaded.getText());
            // JSON은 추가 요청을 만들지 않으며 JavaScript 파서를 거치지 않는다.
            if (request.isJson()) {
                continue;
            }
            String key = request.getSpecifier();
            bases.put(key, loaded.getUrl());
            Node program = Parser.parseModule(loaded.getText(), key);
            ProgramInfo info = (ProgramInfo) program.value;
            // 모듈이 요청한 지정자마다 키를 구해 아직 없는 것은 대기열에 더한다.
            for (ModuleRequest attributeRequest : info.moduleRequests) {
                attributeRequest.validateAttributes();
                String url = ModuleSources.resolve(loaded.getUrl(), attributeRequest.getSpecifier());
                ModuleRequest dependency = new ModuleRequest(url, attributeRequest.getAttributes());
                if (!texts.containsKey(dependency) && !known.contains(dependency)) {
                    work.add(dependency);
                }
            }
        }
    }
}
