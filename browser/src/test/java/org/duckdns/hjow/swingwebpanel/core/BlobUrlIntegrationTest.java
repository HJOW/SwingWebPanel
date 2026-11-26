package org.duckdns.hjow.swingwebpanel.core;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Exercises ordinary request paths for object URLs created within a SwingWebPanel page.
 * <p>
 * SwingWebPanel 페이지에서 생성한 객체 URL의 일반 요청 경로를 검증한다.
 */
public final class BlobUrlIntegrationTest {
    /** Creates the suite. / 시험 묶음을 만든다. */
    public BlobUrlIntegrationTest() {
    }

    /**
     * Fetch and XHR share the creator's opaque origin and keep bytes they already requested.
     * <p>
     * Fetch와 XHR가 생성자의 불투명 오리진을 공유하고 이미 요청한 바이트를 보존한다.
     *
     * @throws Exception on loading or verification failure / 로딩 또는 검증 실패 시
     */
    public void testWebRequestsAndRevocation() throws Exception {
        Check.equal("200|blob bytes|206|bytes|true|TypeError", fromPage("(async()=>{"
                + "const url=URL.createObjectURL(new Blob(['blob bytes'],{type:'text/plain'}));"
                + "const xhr=new Promise((resolve,reject)=>{let x=new XMLHttpRequest();x.open('GET',url);"
                + "x.onload=()=>resolve(x.status+'|'+x.responseText);x.onerror=()=>reject(new Error('xhr'));x.send();});"
                + "const f=fetch(url+'#part',{mode:'same-origin',headers:{Range:'bytes=5-'}});"
                + "URL.revokeObjectURL(url);const r=await f;const text=await r.text();let failure;"
                + "try{await fetch(url);}catch(e){failure=e.name;}"
                + "return [await xhr,r.status,text,r.bodyUsed,failure].join('|');})()"), "ordinary resource requests resolve before revocation");
    }

    /**
     * Loads a classic script, stylesheet, SVG image, WAV metadata and worker from registered objects.
     * <p>
     * 등록된 객체에서 일반 스크립트, 스타일시트, SVG 이미지, WAV 메타데이터와 워커를 읽는다.
     *
     * @throws Exception on loading or verification failure / 로딩 또는 검증 실패 시
     */
    public void testBlobSubresourcesAndWorker() throws Exception {
        byte[] wav = AudioKit.wav(1, 1, 8000, 16, new byte[1600], -1);
        String script = "(async()=>{const make=(data,type)=>URL.createObjectURL(new Blob([data],{type}));"
                + "function load(e,event){return new Promise((yes,no)=>{e.addEventListener(event,()=>yes(e),{once:true});"
                + "e.onerror=()=>no(new Error(e.tagName));document.body.appendChild(e);});}"
                + "const js=document.createElement('script');js.src=make('window.blobScriptRan=42','text/javascript');"
                + "const css=document.createElement('link');css.rel='stylesheet';css.href=make('body{color:#123456}','text/css');"
                + "const img=document.createElement('img');img.crossOrigin='anonymous';"
                + "img.src=make('<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"9\" height=\"8\"/>','image/svg+xml');"
                + "const audio=document.createElement('audio');audio.preload='metadata';"
                + "audio.src=make(new Uint8Array(" + Arrays.toString(wav) + "),'audio/wav');"
                + "const worker=new Worker(make(\"fetch(URL.createObjectURL(new Blob(['worker'])))"
                + ".then(r=>r.text()).then(s=>postMessage(s));\",'text/javascript'));"
                + "const workerText=new Promise((yes,no)=>{worker.onmessage=e=>yes(e.data);worker.onerror=()=>no(new Error('worker'));});"
                + "const cssReady=new Promise(yes=>{document.body.appendChild(css);const timer=setInterval(()=>{"
                + "if(css.sheet){clearInterval(timer);yes();}},10);});"
                + "await Promise.all([cssReady,load(js,'load'),load(img,'load'),load(audio,'loadedmetadata')]);"
                + "return [window.blobScriptRan,css.sheet.cssRules.length,img.naturalWidth,audio.duration>0,await workerText].join('|');})()";
        Check.equal("42|1|9|true|worker", fromPage(script), "object URLs load all tested subresource types in an opaque document");
    }

    /**
     * A frame at a different origin cannot read an object URL received in a message.
     * <p>
     * 다른 오리진의 프레임은 메시지로 받은 객체 URL을 읽을 수 없다.
     *
     * @throws Exception if a test server or the page fails / 시험 서버나 페이지가 실패하면
     */
    public void testOtherOriginCannotFetchBlob() throws Exception {
        TestHttpServer site = new TestHttpServer();
        TestHttpServer frame = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            String child = "<!doctype html><script>onmessage=async function(e){const url=e.data;const xhr=new Promise(resolve=>{"
                    + "const x=new XMLHttpRequest();x.open('GET',url);x.onload=()=>resolve('read');x.onerror=()=>resolve('xhr:'+x.status);x.send();});"
                    + "let result;try{await fetch(url);result='read';}catch(e){result=e.name;}parent.postMessage(result+'|'+await xhr,'*');};"
                    + "parent.postMessage('ready','*');</script>";
            frame.routeFixed("/child", TestHttpServer.Response.html(child));
            frame.start();
            String parent = "<!doctype html><script>const url=URL.createObjectURL(new Blob(['private']));"
                    + "onmessage=e=>{if(e.data==='ready')e.source.postMessage(url,e.origin);else opener.postMessage(e.data,'*');};</script>"
                    + "<iframe src='" + frame.url("/child") + "'></iframe>";
            site.routeFixed("/parent", TestHttpServer.Response.html(parent));
            site.start();
            panel.addWebMessageListener(inbox);
            panel.setPage(site.url("/parent"));
            Check.equal("TypeError|xhr:0", inbox.next().getData(), "Fetch and XHR refuse the other origin");
        } finally {
            panel.dispose();
            site.stop();
            frame.stop();
        }
    }

    /**
     * Runs an opaque page and returns the expression's asynchronous result.
     * <p>
     * 불투명 페이지를 실행하고 식의 비동기 결과를 반환한다.
     *
     * @param expression expression evaluating to a promise / 프라미스로 평가할 식
     * @return reported result / 통지한 결과
     * @throws Exception failure to load the page or deliver a result / 페이지 로딩 또는 결과 전달 실패
     */
    private static Object fromPage(String expression) throws Exception {
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            panel.addWebMessageListener(inbox);
            panel.setContent("<!doctype html><body><script>Promise.resolve(" + expression
                    + ").then(s=>opener.postMessage(s,'*'),e=>opener.postMessage('error:'+e.message,'*'));</script>", "text/html");
            return inbox.next().getData();
        } finally {
            panel.dispose();
        }
    }
}
