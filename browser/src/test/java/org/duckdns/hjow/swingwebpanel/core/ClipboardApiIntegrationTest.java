package org.duckdns.hjow.swingwebpanel.core;

import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests asynchronous clipboard access with real input dispatch and a replacement clipboard.
 * <p>
 * 실제 입력 전달과 대체 클립보드로 비동기 클립보드 접근을 검증한다.
 */
public final class ClipboardApiIntegrationTest {
    /** Creates the suite. / 시험 묶음을 만든다. */
    public ClipboardApiIntegrationTest() {
    }

    /**
     * Allows writes in trusted input and reads during a paste, ending both grants with the input task.
     * <p>
     * 신뢰 입력의 쓰기와 붙여넣기 중 읽기를 허용하고 입력 작업과 함께 두 권한을 끝낸다.
     *
     * @throws Exception on setup or verification failure / 구성 또는 검증 실패 시
     */
    public void testTrustedInputAndPlatformClipboard() throws Exception {
        Panel panel = EditingFixture.create(300, 200);
        try {
            panel.clipboard = new StringSelection("붙여넣기");
            panel.show("<!doctype html><button style='width:100px;height:46px'>Copy</button><script>"
                    + "const n=navigator.clipboard, button=document.querySelector('button');"
                    + "function put(k,v){sessionStorage.setItem(k,String(v));}"
                    + "put('ready',isSecureContext && n instanceof Clipboard && n instanceof EventTarget);"
                    + "function result(k,p){p.then(x=>put(k,x),e=>put(k,e.name));}"
                    + "result('initial',n.writeText('denied'));"
                    + "button.addEventListener('click',e=>{"
                    + "if(!e.isTrusted){result('syntheticClick',n.writeText('denied'));return;}"
                    + "Promise.resolve().then(()=>result('written',n.writeText('복사')));"
                    + "result('clickRead',n.readText());setTimeout(()=>result('late',n.writeText('denied')),0);});"
                    + "document.addEventListener('keydown',e=>{if(e.code==='KeyV'){"
                    + "e.preventDefault();result('read-'+(++reads),n.readText());}}); let reads=0;"
                    + "button.click();document.dispatchEvent(new KeyboardEvent('keydown',{code:'KeyV',ctrlKey:true}));"
                    + "</script>");
            EditingFixture.await(panel, "ready", "true");
            EditingFixture.await(panel, "initial", "NotAllowedError");
            EditingFixture.await(panel, "syntheticClick", "NotAllowedError");
            EditingFixture.await(panel, "read-1", "NotAllowedError");
            Check.equal(0, panel.texts.size(), "no write reached the host without trusted input");

            FrameTestSupport.awaitProgress(panel, 100);
            FrameTestSupport.click(panel, 30, 30);
            EditingFixture.await(panel, "written", "undefined");
            EditingFixture.await(panel, "clickRead", "NotAllowedError");
            EditingFixture.await(panel, "late", "NotAllowedError");
            Check.equal(1, panel.texts.size(), "the verified input wrote only one value");
            Check.equal("복사", panel.texts.get(0), "plain text passed through the component clipboard");
            Check.isNull(panel.markups.get(0), "no HTML is included when writing plain text");

            EditingFixture.tap(panel, KeyEvent.VK_V, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            EditingFixture.await(panel, "read-2", "붙여넣기");
            panel.clipboard = null;
            EditingFixture.tap(panel, KeyEvent.VK_V, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            EditingFixture.await(panel, "read-3", "");
            panel.clipboardBroken = true;
            EditingFixture.tap(panel, KeyEvent.VK_V, KeyEvent.CHAR_UNDEFINED, InputEvent.CTRL_DOWN_MASK);
            EditingFixture.await(panel, "read-4", "NotAllowedError");
        } finally {
            EditingFixture.close(panel);
        }
    }

    /**
     * Hides the interface in an opaque, nonsecure document.
     * <p>
     * 보안 컨텍스트가 아닌 불투명 문서에서는 인터페이스를 숨긴다.
     *
     * @throws Exception on setup or verification failure / 구성 또는 검증 실패 시
     */
    public void testClipboardIsSecureContextOnly() throws Exception {
        SwingWebPanel panel = FrameTestSupport.newPanel(240, 160);
        Inbox inbox = new Inbox();
        try {
            panel.addWebMessageListener(inbox);
            panel.setContent("<!doctype html><script>opener.postMessage([isSecureContext, typeof navigator.clipboard, typeof Clipboard].join(','),'*');</script>",
                    "text/html");
            Check.equal("false,undefined,undefined", inbox.next().getData(), "the clipboard interface is hidden");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Allows a host-packaged application to copy text in response to an actual click.
     * <p>
     * 호스트에 패키징된 애플리케이션이 실제 클릭을 받았을 때 텍스트를 복사하게 한다.
     *
     * @throws Exception on setup or verification failure / 구성 또는 검증 실패 시
     */
    public void testClasspathClipboard() throws Exception {
        Panel panel = EditingFixture.create(280, 180);
        try {
            panel.setPage("classpath://org/duckdns/hjow/swingwebpanel/webapi/clipboard.html");
            EditingFixture.await(panel, "clipboard-secure", "true");
            FrameTestSupport.awaitProgress(panel, 100);
            FrameTestSupport.click(panel, 30, 30);
            EditingFixture.await(panel, "classpath-copy", "copied");
            Check.equal("로컬 문서", panel.texts.get(0), "trusted classpath input writes to the host clipboard service");
        } finally {
            EditingFixture.close(panel);
        }
    }

    /**
     * Denies a cross-origin frame even after the user interacts with the frame itself.
     * <p>
     * 사용자가 프레임을 직접 조작해도 교차 오리진 프레임의 요청은 거부한다.
     *
     * @throws Exception on setup or verification failure / 구성 또는 검증 실패 시
     */
    public void testCrossOriginFrameCannotWrite() throws Exception {
        TestHttpServer child = new TestHttpServer();
        Panel panel = EditingFixture.create(300, 220);
        try {
            child.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><body style='margin:0'><button style='width:100px;height:46px'>Copy</button>"
                    + "<script>sessionStorage.setItem('child-ready',String(isSecureContext));"
                    + "document.querySelector('button').onclick=e=>navigator.clipboard.writeText('child')"
                    + ".then(()=>sessionStorage.setItem('child-result','allowed'),reason=>sessionStorage.setItem('child-result',e.isTrusted+':'+reason.name));</script>"));
            child.start();
            panel.show("<!doctype html><body style='margin:0'><iframe style='border:0' src='" + child.url("/child") + "'></iframe>");
            EditingFixture.await(panel, "child-ready", "true");
            FrameTestSupport.awaitProgress(panel, 100);
            FrameTestSupport.click(panel, 25, 20);
            EditingFixture.await(panel, "child-result", "true:NotAllowedError");
            Check.equal(0, panel.texts.size(), "the cross-origin frame did not access the host clipboard");
        } finally {
            EditingFixture.close(panel);
            child.stop();
        }
    }
}
