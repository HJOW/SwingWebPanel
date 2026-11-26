package org.duckdns.hjow.swingwebpanel.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Exercises local documents, file subresources and the local-to-network boundary with the real component.
 * <p>
 * 실제 컴포넌트에서 로컬 문서, 파일 하위 리소스와 로컬·네트워크 간 경계를 확인한다.
 */
public final class LocalFileIntegrationTest {
    /** Creates the test suite. / 시험 묶음을 만든다. */
    public LocalFileIntegrationTest() {
    }

    /**
     * Opens a document with a Unicode name, resolves files from it, and follows a local link.
     * <p>
     * 유니코드 이름의 문서를 열고 상대 파일을 읽은 뒤 로컬 링크를 따라간다.
     *
     * @throws Exception on loading or verification failure / 로딩이나 검증이 실패하면
     */
    public void testLocalDocumentResourcesAndNavigation() throws Exception {
        File root = tempDirectory();
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            write(root, "code.js", "window.fileScript='script';");
            write(root, "data file.txt", "local data");
            write(root, "next.html", "<!doctype html><p id='marker'>local frame</p>"
                    + "<script>if(top===window)opener.postMessage('linked','*');</script>");
            String page = "<!doctype html><body><a href='next.html' style='display:block;width:100px;height:40px'>Next</a>"
                    + "<script src='code.js'></script><script>(async()=>{"
                    + "let xhr=new Promise((yes,no)=>{let x=new XMLHttpRequest();x.open('GET','data%20file.txt');"
                    + "x.onload=()=>yes(x.responseText);x.onerror=no;x.send();});"
                    + "let frame=new Promise(yes=>{let f=document.createElement('iframe');f.src='next.html';"
                    + "f.onload=()=>{let d=f.contentDocument,m=d&&d.getElementById('marker');if(m)yes(m.textContent);};"
                    + "document.body.appendChild(f);});"
                    + "let r=await fetch('data%20file.txt',{mode:'same-origin'});"
                    + "opener.postMessage([fileScript,await r.text(),await xhr,await frame,location.protocol].join('|'),'*');"
                    + "})().catch(e=>opener.postMessage('error:'+e.message,'*'));</script>";
            File entry = write(root, "한글 index.html", page);
            panel.addWebMessageListener(inbox);
            panel.setPage(entry.toURI().toASCIIString());
            Check.equal("script|local data|local data|local frame|file:", inbox.next().getData(), "local script, Fetch, XHR and frame");
            awaitPage(panel, UrlUtil.normalize(entry.toURI().toASCIIString()));
            Check.equal(page, panel.getContent(), "the source is the decoded file");
            FrameTestSupport.click(panel, 24, 20);
            Check.equal("linked", inbox.next().getData(), "the link navigated to the local document");
            awaitPage(panel, UrlUtil.normalize(new File(root, "next.html").toURI().toString()));
        } finally {
            panel.dispose();
            delete(root);
        }
    }

    /**
     * An HTTP document cannot gain local access by selecting a local base or embedding a local frame.
     * <p>
     * HTTP 문서는 로컬 기준 URL을 두거나 로컬 프레임을 넣어 파일 접근 권한을 얻을 수 없다.
     *
     * @throws Exception if loading or verification fails / 로딩이나 검증이 실패하면
     */
    public void testRemoteDocumentCannotUseLocalFileBase() throws Exception {
        File root = tempDirectory();
        TestHttpServer site = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(300, 180);
        Inbox inbox = new Inbox();
        try {
            write(root, "secret.js", "opener.postMessage('read local script','*');");
            File secret = write(root, "secret.html", "<script>parent.postMessage('read local document','*')</script>");
            String target = secret.toURI().toASCIIString();
            String source = "<!doctype html><head><base href='" + root.toURI().toASCIIString() + "'></head><body>"
                    + "<script src='secret.js' onerror='window.localScriptBlocked=true'></script><script>"
                    + "onmessage=e=>opener.postMessage('original','*');"
                    + "let phase='fetch';let timer=setTimeout(()=>opener.postMessage('timeout:'+phase,'*'),4000);"
                    + "(async()=>{let blocked=await fetch('secret.js').then(()=>false,()=>true);"
                    + "phase='xhr';let xhr=await new Promise(yes=>{let x=new XMLHttpRequest();x.open('GET','secret.js');"
                    + "x.onload=()=>yes(false);x.onerror=()=>yes(x.status===0);x.send();});"
                    + "let script=window.localScriptBlocked===true;"
                    + "phase='frame';let frame=await new Promise(yes=>{let f=document.createElement('iframe');f.src='secret.html';"
                    + "f.onload=()=>yes(f.contentDocument===null);document.body.appendChild(f);});"
                    + "phase='location';location.assign('secret.html');clearTimeout(timer);opener.postMessage([blocked,xhr,script,frame].join('|'),'*');"
                    + "})().catch(e=>opener.postMessage('error:'+e.name+':'+e.message,'*'));</script>";
            site.routeFixed("/remote", TestHttpServer.Response.html(source));
            site.routeFixed("/redirect", TestHttpServer.Response.redirect(302, target));
            site.start();
            panel.addWebMessageListener(inbox);
            panel.setPage(site.url("/remote"));
            Check.equal("true|true|true|true", inbox.next().getData(), "Fetch, XHR, script and frame stay isolated");
            awaitPage(panel, site.url("/remote"));
            panel.postMessage("still here", "*");
            Check.equal("original", inbox.next().getData(), "location assignment did not replace the network page");
            panel.setPage(site.url("/redirect"));
            awaitPage(panel, site.url("/redirect"));
            Check.isTrue(panel.getStatusText().startsWith("Error"), "the HTTP redirect to a local file was refused");
        } finally {
            panel.dispose();
            site.stop();
            delete(root);
        }
    }

    /**
     * Waits for this page's final URL and load completion.
     * <p>
     * 페이지의 최종 URL과 로드 완료를 기다린다.
     *
     * @param panel component under test / 시험 컴포넌트
     * @param url expected URL / 예상 URL
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    private static void awaitPage(SwingWebPanel panel, String url) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        // 이전 문서가 남아 있거나 대상의 작업이 진행 중이면 기다린다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "settled URL");
        Check.equal(100, panel.getProgress(), "settled progress");
    }

    /** Makes a unique fixture directory. / 고유한 시험 파일 디렉터리를 만든다. */
    private static File tempDirectory() throws IOException {
        File root = File.createTempFile("swp-file-", "");
        if (!root.delete() || !root.mkdir()) {
            throw new IOException("could not create the fixture directory");
        }
        return root;
    }

    /** Writes a UTF-8 fixture and returns its path. / UTF-8 시험 파일을 쓰고 경로를 반환한다. */
    private static File write(File root, String name, String text) throws IOException {
        File file = new File(root, name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
        return file;
    }

    /** Deletes the files created in a fixture directory. / 시험 디렉터리에 만든 파일을 지운다. */
    private static void delete(File root) {
        File[] files = root.listFiles();
        if (files != null) {
            // 다른 경로를 따라가지 않고 이번 시험에서 만든 파일만 지운다.
            for (File file : files) {
                file.delete();
            }
        }
        root.delete();
    }
}
