package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the modal dialog and the top layer as scripts see them (plan item P10-08-h, CSS Positioned Layout 4 section 3 and the dialog rules of the WHATWG
 * HTML rendering section): <code>showModal()</code> puts the dialog in the top layer so it matches <code>:modal</code> and computes
 * <code>position: fixed</code>, <code>show()</code> does not, <code>close()</code> takes it out again, and <code>getComputedStyle(dialog, '::backdrop')</code>
 * gives the UA backdrop.
 * <p>
 * 스크립트가 보는 모달 대화상자와 최상위 계층의 시험(계획 P10-08-h, CSS Positioned Layout 4 3 절과 WHATWG HTML 렌더링 절의 dialog 규칙): <code>showModal()</code> 은 대화상자를
 * 최상위 계층에 넣어 <code>:modal</code> 과 일치하고 <code>position: fixed</code> 로 계산되게 하며, <code>show()</code> 는 그렇지 않고, <code>close()</code> 는 다시 뺀다.
 * <code>getComputedStyle(dialog, '::backdrop')</code> 은 UA 의 배경막을 준다.
 */
public class ModalDialogTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ModalDialogTest() {
    }

    /**
     * <code>:modal</code> and the computed styles of a modal and a non-modal dialog.
     * <p>
     * 모달과 비모달 대화상자의 <code>:modal</code> 과 계산 스타일.
     */
    public void testModalState() {
        PageFixture f = CssomSheetsTest.page("<dialog id=d>hi</dialog><dialog id=e>there</dialog>");
        Check.equal("false|absolute", f.scripts.evaluate("var d = document.getElementById('d'); d.show(); [d.matches(':modal'), getComputedStyle(d).position].join('|')", "t"),
                "show() opens a non-modal dialog");
        Check.equal("true|fixed|rgba(0, 0, 0, 0.1)|fixed", f.scripts.evaluate("var e = document.getElementById('e'); e.showModal(); var b = getComputedStyle(e, '::backdrop'); "
                + "[e.matches(':modal'), getComputedStyle(e).position, b.backgroundColor, b.position].join('|')", "t"), "showModal() makes it :modal and fixed with a backdrop");
        Check.equal("true", f.scripts.evaluate("String(document.querySelector(':modal') === e)", "t"), "querySelector(':modal')");
        Check.equal("false|none", f.scripts.evaluate("e.close(); [e.matches(':modal'), getComputedStyle(e).display].join('|')", "t"), "close() takes it out of the top layer");
    }
}
