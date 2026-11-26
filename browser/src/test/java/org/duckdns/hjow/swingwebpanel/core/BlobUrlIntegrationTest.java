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
     * A host navigation can consume a document before the creating page is discarded.
     * <p>
     * 호스트 이동은 생성한 페이지를 폐기하기 전에 대상 문서를 확보할 수 있다.
     *
     * @throws Exception if navigation or message delivery fails / 이동이나 메시지 전달이 실패하면
     */
    public void testHostNavigatesToBlobDocument() throws Exception {
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        String html = "<!doctype html><script>opener.postMessage(location.protocol+'|'+origin,'*');</script>";
        try {
            panel.addWebMessageListener(inbox);
            panel.setContent("<!doctype html><script>const url=URL.createObjectURL(new Blob(["
                    + "'" + html.replace("'", "\\'").replace("</script>", "<\\/script>") + "'],{type:'text/html'}));"
                    + "opener.postMessage(url,'*');</script>", "text/html");
            String url = (String) inbox.next().getData();
            panel.setPage(url + "#document");
            Check.equal("blob:|null", inbox.next().getData(), "opaque origin and address of the displayed document");
            awaitPage(panel, url + "#document");
            Check.equal(html, panel.getContent(), "host navigation retains the original HTML");
            panel.setPage("about:blank");
            awaitPage(panel, "about:blank");
            panel.setPage(url);
            awaitPage(panel, url);
            Check.isTrue(panel.getContent().contains("404"), "closed creator removed the registered document");
        } finally {
            panel.dispose();
        }
    }

    /**
     * The media document renders a captured image after the creator is gone.
     * <p>
     * 미디어 문서는 생성자가 사라진 뒤에도 확보한 이미지를 그린다.
     *
     * @throws Exception if the rendered image cannot be checked / 그려진 이미지를 검증할 수 없으면
     */
    public void testHostNavigatesToBlobImage() throws Exception {
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            panel.addWebMessageListener(inbox);
            panel.setContent("<!doctype html><script>const image='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"40\" height=\"40\">"
                    + "<rect width=\"40\" height=\"40\" fill=\"#123456\"/></svg>';"
                    + "opener.postMessage(URL.createObjectURL(new Blob([image],{type:'image/svg+xml'})),'*');</script>", "text/html");
            String url = (String) inbox.next().getData();
            panel.setPage(url);
            awaitPage(panel, url);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x123456, "standalone object URL image");
        } finally {
            panel.dispose();
        }
    }

    /**
     * A same-origin frame and an activated link display HTML from object URLs.
     * <p>
     * 같은 오리진 프레임과 활성화한 링크는 객체 URL의 HTML을 표시한다.
     *
     * @throws Exception if navigation or message delivery fails / 이동이나 메시지 전달이 실패하면
     */
    public void testLinkAndFrameDisplayBlobDocument() throws Exception {
        TestHttpServer site = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            String parent = "<!doctype html><body><a id='open' style='display:block;width:120px;height:35px'>Open</a>"
                    + "<iframe id='frame'></iframe><script>const make=html=>URL.createObjectURL(new Blob([html],{type:'text/html'}));"
                    + "const f=document.getElementById('frame');f.onload=()=>{if(f.contentDocument.getElementById('payload'))"
                    + "opener.postMessage(f.contentDocument.getElementById('payload').textContent,'*');};"
                    + "f.src=make('<!doctype html><p id=payload>framed</p>');"
                    + "document.getElementById('open').href=make(\"<!doctype html><script>opener.postMessage(origin+'|'+location.href,'*');<\\/script>\");"
                    + "</script>";
            site.routeFixed("/parent", TestHttpServer.Response.html(parent));
            site.start();
            panel.addWebMessageListener(inbox);
            panel.setPage(site.url("/parent"));
            Check.equal("framed", inbox.next().getData(), "the object's tuple origin permits frame DOM access");
            FrameTestSupport.awaitProgress(panel, 100);
            FrameTestSupport.click(panel, 25, 20);
            String result = (String) inbox.next().getData();
            String destination = result.substring(result.indexOf('|') + 1);
            Check.equal(site.url("") + "|", result.substring(0, result.indexOf('|') + 1), "link inherits the creator's tuple origin");
            Check.isTrue(destination.startsWith("blob:"), "link's document address is an object URL");
            awaitPage(panel, destination);
        } finally {
            panel.dispose();
            site.stop();
        }
    }

    /**
     * An opaque document can reach its own blob frame; a sandbox starts with a fresh opaque origin.
     * <p>
     * 불투명 문서는 자체 객체 URL 프레임에 접근할 수 있고 샌드박스는 별도의 불투명 오리진을 사용한다.
     *
     * @throws Exception if the frame does not load / 프레임을 읽지 못하면
     */
    public void testOpaqueBlobFrameOrigin() throws Exception {
        Check.equal("framed|true", fromPage("new Promise(resolve=>{const f=document.createElement('iframe');"
                + "const s=document.createElement('iframe');s.setAttribute('sandbox','allow-scripts');"
                + "const url=URL.createObjectURL(new Blob(['<!doctype html><p id=payload>framed</p>'],{type:'text/html'}));"
                + "f.src=url;s.src=url;f.onload=()=>{const p=f.contentDocument&&f.contentDocument.getElementById('payload');"
                + "if(p){s.onload=()=>resolve(p.textContent+'|'+(s.contentDocument===null));document.body.appendChild(s);}};"
                + "document.body.appendChild(f);})"), "an inherited opaque origin retains access only outside a sandbox");
    }

    /**
     * A remote frame cannot navigate its own child to a blob URL from a different origin.
     * <p>
     * 원격 프레임은 자기 자식 프레임을 다른 오리진의 객체 URL로 이동할 수 없다.
     *
     * @throws Exception if test page loading or message delivery fails / 시험 페이지 로딩이나 메시지 전달이 실패하면
     */
    public void testOtherOriginCannotEmbedBlobDocument() throws Exception {
        TestHttpServer site = new TestHttpServer();
        TestHttpServer frame = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            frame.routeFixed("/frame", TestHttpServer.Response.html("<!doctype html><body><script>"
                    + "onmessage=e=>{const f=document.createElement('iframe');f.src=e.data;f.onload=()=>{"
                    + "parent.postMessage('settled:'+(f.contentDocument===null),'*');};document.body.appendChild(f);};"
                    + "parent.postMessage('ready','*');</script>"));
            frame.start();
            String page = "<!doctype html><script>const source='<scr'+'ipt>top.postMessage(\"ran\",\"*\");<\\/script>';"
                    + "const url=URL.createObjectURL(new Blob([source],{type:'text/html'}));"
                    + "onmessage=e=>{if(e.data==='ready')e.source.postMessage(url,e.origin);else opener.postMessage(e.data,'*');};</script>"
                    + "<iframe src='" + frame.url("/frame") + "'></iframe>";
            site.routeFixed("/site", TestHttpServer.Response.html(page));
            site.start();
            panel.addWebMessageListener(inbox);
            panel.setPage(site.url("/site"));
            Check.equal("settled:true", inbox.next().getData(), "the other origin does not run the blob script or expose frame DOM");
        } finally {
            panel.dispose();
            site.stop();
            frame.stop();
        }
    }

    /**
     * Waits for the requested URL and a settled navigation.
     * <p>
     * 요청한 URL의 이동이 확정될 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param url the destination / 대상 주소
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        // 이전 페이지가 남아 있거나 요청이 진행 중이면 잠시 기다린다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "committed destination");
        Check.equal(100, panel.getProgress(), "completed destination");
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
