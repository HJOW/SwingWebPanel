package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies abortable listeners on a loaded <code>SwingWebPanel</code> page.
 * <p>
 * <code>SwingWebPanel</code>에서 로드한 페이지의 중단 가능 리스너를 검증한다.
 */
public final class AbortListenerIntegrationTest {
    /** Creates the suite. / 시험 묶음을 만든다. */
    public AbortListenerIntegrationTest() {
    }

    /**
     * Removes listeners before the abort event and skips them in a running dispatch.
     * <p>
     * abort 이벤트 전에 리스너를 제거하고 진행 중인 전달에서 건너뛰는지 확인한다.
     *
     * @throws Exception if the page cannot run / 페이지를 실행하지 못하면
     */
    public void testAbortDuringDispatch() throws Exception {
        String script = "const el=document.body, c=new AbortController(), log=[];"
                + "const fire=()=>el.dispatchEvent(new Event('probe',{bubbles:true}));"
                + "const record=n=>()=>log.push(n);"
                + "el.addEventListener('probe',record('pre-aborted'),{signal:AbortSignal.abort()});"
                + "el.addEventListener('probe',()=>{log.push('first');c.abort();},{once:true});"
                + "el.addEventListener('probe',record('removed'),{signal:c.signal});"
                + "addEventListener('probe',record('window removed'),{signal:c.signal});"
                + "c.signal.addEventListener('abort',()=>{log.push('abort');fire();});"
                + "el.addEventListener('probe',record('kept'));"
                + "addEventListener('probe',record('window'));fire();"
                + "opener.postMessage(log.join(','),'*');";
        Check.equal("first,abort,kept,window,kept,window", run(script), "abort steps remove only the linked entries");
    }

    /**
     * Checks signal conversion, duplicate registration and attaching a new signal after removal.
     * <p>
     * 신호 변환, 중복 등록 및 삭제 뒤 새 신호 연결을 검사한다.
     *
     * @throws Exception if the page cannot run / 페이지를 실행하지 못하면
     */
    public void testRegistrationOptionsAndIdentity() throws Exception {
        String script = "const el=document.body, a=new AbortController(), b=new AbortController(), log=[];"
                + "const fire=()=>el.dispatchEvent(new Event('event')); const cb=()=>log.push('listener');"
                + "el.addEventListener({toString(){log.push('type');return 'event';}}, cb, {"
                + " get capture(){log.push('capture');}, get once(){log.push('once');return true;},"
                + " get passive(){log.push('passive');}, get signal(){log.push('signal');return a.signal;} });"
                + "el.addEventListener('event',cb,{signal:b.signal});b.abort();"
                + "a.signal.dispatchEvent(new Event('abort'));fire();fire();"
                + "el.addEventListener('event',cb);a.abort();fire();"
                + "el.removeEventListener('event',cb);fire();"
                + "const c=new AbortController();el.addEventListener('event',cb,{signal:c.signal});"
                + "el.removeEventListener('event',cb);el.addEventListener('event',cb);c.abort();fire();"
                + "try { addEventListener('event',null,{signal:{aborted:false}}); }catch(e){log.push(e.name); }"
                + "try { el.addEventListener('event',cb,{signal:null}); }catch(e){log.push(e.name); }"
                + "opener.postMessage(log.join(','),'*');";
        Check.equal("type,capture,once,passive,signal,listener,listener,listener,TypeError,TypeError", run(script),
                "option order, interface type and per-registration abort");
    }

    /**
     * Runs a page that reports its observations to the Java host.
     * <p>
     * 관찰한 결과를 Java 호스트에 알리는 페이지를 실행한다.
     *
     * @param script page script / 페이지 스크립트
     * @return first report / 첫 통지
     * @throws Exception if loading or delivery fails / 로딩이나 전달 실패 시
     */
    private static Object run(String script) throws Exception {
        SwingWebPanel panel = FrameTestSupport.newPanel(320, 200);
        Inbox results = new Inbox();
        try {
            panel.addWebMessageListener(results);
            panel.setContent("<!doctype html><body><script>" + script + "</script>", "text/html");
            return results.next().getData();
        } finally {
            panel.dispose();
        }
    }
}
