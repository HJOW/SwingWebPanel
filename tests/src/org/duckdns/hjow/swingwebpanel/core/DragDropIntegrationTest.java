package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.await;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitContains;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.awaitSel;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.drain;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.log;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.midX;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.midY;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.mouse;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.pageWith;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.rect;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.stored;
import static org.duckdns.hjow.swingwebpanel.core.EditingFixture.tap;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.EditingFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of HTML drag and drop with the real component and real AWT mouse events (plan item P20-06): a press on a draggable element or on
 * the selected text, moves of the pointer and the release over another place fire the events of the specification in their order
 * (<code>dragstart</code>, <code>drag</code>, <code>dragenter</code>, <code>dragover</code>, <code>dragleave</code>, <code>drop</code>,
 * <code>dragend</code>) with a <code>DataTransfer</code> in the mode of each event; a cancelled <code>dragenter</code> and <code>dragover</code> accept
 * a place and the <code>dropEffect</code> and <code>effectAllowed</code> choose the operation; a text field and an editing host take dragged text
 * without a script (and moving a selection inside an editing host deletes it from its old place); Escape cancels; a link and an image give their
 * address; a drag suppresses <code>mouseup</code>. The drag is simulated inside the component, so no real drag of the operating system was
 * tried here.
 * <p>
 * 실제 컴포넌트와 실제 AWT 마우스 이벤트로 하는 HTML 끌어다 놓기 시험(계획 P20-06): 끌 수 있는 요소나 선택한 텍스트 위의 누름, 포인터 이동, 다른 곳에서의 뗌이 규격의
 * 이벤트(<code>dragstart</code>, <code>drag</code>, <code>dragenter</code>, <code>dragover</code>, <code>dragleave</code>, <code>drop</code>, <code>dragend</code>)를 순서대로
 * 일으키고 이벤트마다 그 모드의 <code>DataTransfer</code> 가 간다. 취소한 <code>dragenter</code> 와 <code>dragover</code> 는 그곳을 받아들이며 <code>dropEffect</code> 와
 * <code>effectAllowed</code> 가 동작을 고른다. 텍스트 필드와 편집 호스트는 스크립트 없이 끌어온 텍스트를 받고(편집 호스트 안에서 선택을 옮기면 원래 자리에서 지워진다),
 * Esc 는 취소하며, 링크와 이미지는 주소를 주고, 끌기는 <code>mouseup</code> 을 막는다. 끌기는 컴포넌트 안에서 흉내 내므로 운영체제의 실제 끌기는 여기서 시험하지 않았다.
 */
public class DragDropIntegrationTest {

    /**
     * The body of the test page: a draggable source, a place that accepts drops by script, one that does not, a source that cancels
     * <code>dragstart</code>, a link and an image-like draggable element, the editing host, a paragraph of selectable text and a text field.
     * <p>
     * 시험 페이지의 본문: 끌 수 있는 원본, 스크립트로 놓기를 받는 곳, 받지 않는 곳, <code>dragstart</code> 를 취소하는 원본, 링크, 편집 호스트, 선택할 수 있는 텍스트 문단,
     * 텍스트 필드.
     */
    private static final String BODY = "<div id=src draggable=true style='width:120px;height:30px;background:#a00'>drag me</div>"
            + "<div id=zone style='width:300px;height:30px;margin-top:10px;background:#0a0'>zone</div>"
            + "<div id=other style='width:300px;height:30px;margin-top:10px'>other</div>"
            + "<div id=nope draggable=true style='width:120px;height:30px;margin-top:10px;background:#00a'>nope</div>"
            + "<a id=lnk href='http://links.test/x' style='display:block;width:120px;height:24px;margin-top:10px'>a link</a>"
            + "<div id=ed contenteditable=true style='margin-top:10px'>one two three</div>"
            + "<p id=sel style='margin:10px 0 0 0'>selectable text</p>"
            + "<input id=inp value=abc style='display:block;width:200px;margin-top:10px'>"
            + "<div id=ro style='margin-top:10px'>read only</div><div id=box></div>";

    /**
     * The script of the test page: it records the drag events by target (consecutive repeats once), and gives the places their behavior.
     * <p>
     * 시험 페이지의 스크립트: 끌기 이벤트를 대상별로 기록하고(연속된 반복은 한 번) 곳마다 동작을 준다.
     */
    private static final String SCRIPT = "var dlog = []; var overs = 0; var seen = {};"
            + " function dnd(e) { var t = e.target; var id = t.id || t.nodeName; var s = e.type + '@' + id;"
            + "   if (!dlog.length || dlog[dlog.length - 1] !== s) { dlog.push(s); put('dnd', dlog.join(' ')); }"
            + "   if (e.type === 'dragover') { overs++; put('overs', String(overs)); } }"
            + " var dts = ['dragstart', 'drag', 'dragenter', 'dragover', 'dragleave', 'drop', 'dragend'];"
            + " for (var q = 0; q < dts.length; q++) { document.addEventListener(dts[q], dnd, true); }"
            + " document.addEventListener('mouseup', function () { put('mouseup', 'yes'); });"
            + " var zone = document.getElementById('zone'); var src = document.getElementById('src');"
            + " zone.addEventListener('dragenter', function (e) { e.preventDefault(); });"
            + " zone.addEventListener('dragover', function (e) { e.preventDefault(); e.dataTransfer.dropEffect = 'copy';"
            + "   put('over', e.dataTransfer.getData('text/plain') + '/' + e.dataTransfer.types.join() + '/' + e.dataTransfer.effectAllowed); });"
            + " zone.addEventListener('drop', function (e) { e.preventDefault();"
            + "   put('dropped', e.dataTransfer.getData('text/plain') + '|' + e.dataTransfer.types.join() + '|' + e.dataTransfer.dropEffect + '|' + e.dataTransfer.effectAllowed); });"
            + " src.addEventListener('dragstart', function (e) { e.dataTransfer.setData('text/plain', 'payload'); e.dataTransfer.effectAllowed = 'copyMove'; window.__dt = e.dataTransfer; });"
            + " src.addEventListener('dragend', function (e) { put('ended', e.dataTransfer.dropEffect); put('late', window.__dt.types.length + ':' + window.__dt.getData('text/plain')); });"
            + " document.getElementById('nope').addEventListener('dragstart', function (e) { e.preventDefault(); });"
            + " document.addEventListener('dragstart', function (e) { put('uri', e.dataTransfer.getData('text/uri-list') + '#' + e.dataTransfer.types.join()); });"
            + " document.getElementById('inp').addEventListener('input', function (e) { put('inp', document.getElementById('inp').value + ':' + e.inputType); });";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DragDropIntegrationTest() {
    }

    /**
     * Opens the test page.
     * <p>
     * 시험 페이지를 연다.
     *
     * @param panel the component / 컴포넌트
     * @return the rectangles / 사각형
     * @throws Exception on failure / 실패 시
     */
    private static Rects page(Panel panel) throws Exception {
        return open(panel, pageWith(BODY, SCRIPT));
    }

    /**
     * Returns the width of one character of the text of the editing host (<code>one two three</code>: 13 characters, monospace).
     * <p>
     * 편집 호스트의 텍스트(<code>one two three</code>: 13 글자, 고정폭)의 글자 하나의 너비를 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @return the width / 너비
     */
    private static double adv13(Panel panel) {
        return Double.parseDouble(stored(panel, "textw")) / 13;
    }

    /**
     * Presses the main button at a point and waits until the document queue has handled the press.
     * <p>
     * 한 점에서 주 버튼을 누르고 문서 큐가 누름을 처리할 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param x     component x / 컴포넌트 x
     * @param y     component y / 컴포넌트 y
     * @throws Exception on failure / 실패 시
     */
    private static void press(Panel panel, double x, double y) throws Exception {
        mouse(panel, MouseEvent.MOUSE_PRESSED, x, y, 1, InputEvent.BUTTON1_DOWN_MASK);
        drain(panel);
    }

    /**
     * Moves the pointer with the button down.
     * <p>
     * 버튼을 누른 채 포인터를 옮긴다.
     *
     * @param panel the component / 컴포넌트
     * @param x     component x / 컴포넌트 x
     * @param y     component y / 컴포넌트 y
     * @throws Exception on failure / 실패 시
     */
    private static void moveTo(Panel panel, double x, double y) throws Exception {
        mouse(panel, MouseEvent.MOUSE_DRAGGED, x, y, 1, InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Releases the main button at a point.
     * <p>
     * 한 점에서 주 버튼을 뗀다.
     *
     * @param panel the component / 컴포넌트
     * @param x     component x / 컴포넌트 x
     * @param y     component y / 컴포넌트 y
     * @throws Exception on failure / 실패 시
     */
    private static void release(Panel panel, double x, double y) throws Exception {
        mouse(panel, MouseEvent.MOUSE_RELEASED, x, y, 1, 0);
    }

    /**
     * Checks that the fragments occur in the log in this order (other events may lie between them).
     * <p>
     * 조각들이 로그에 이 순서로 나오는지 검사한다(그 사이에 다른 이벤트가 있어도 된다).
     *
     * @param log       the log / 로그
     * @param fragments the events in order / 순서대로의 이벤트
     */
    private static void inOrder(String log, String... fragments) {
        int from = 0;
        // 조각을 차례로 앞에서부터 찾아 간다.
        for (String f : fragments) {
            int at = log.indexOf(f, from);
            Check.isTrue(at >= 0, "'" + f + "' should come in this order in: " + log);
            from = at + f.length();
        }
    }

    /**
     * A drag from a draggable element to a place that accepts it: the events come in the order of the specification, the data transfers
     * have the modes of their events, <code>drop</code> carries the data and the effect, and <code>dragend</code> the operation; no
     * <code>mouseup</code> reaches the page.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragFromAnElementToAZone() throws Exception {
        Panel panel = create(500, 600);
        try {
            Rects rects = page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            double zx = midX(panel, "zone");
            double zy = midY(panel, "zone");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, (sx + zx) / 2, (sy + zy) / 2);
            moveTo(panel, zx, zy);
            awaitContains(panel, "over", "text/plain");
            release(panel, zx, zy);
            awaitContains(panel, "ended", "copy");
            String l = stored(panel, "dnd");
            inOrder(l, "dragstart@src", "drag@src", "dragenter@zone", "dragover@zone", "drop@zone", "dragend@src");
            Check.equal("payload|text/plain|copy|copyMove", stored(panel, "dropped"), "drop: the data is readable, the effect is the operation, the allowed effects are kept");
            Check.equal("/text/plain/copyMove", stored(panel, "over"), "dragover: the types are listed but the data cannot be read (protected mode)");
            Check.equal("0:", stored(panel, "late"), "after its event the data transfer shows nothing any more");
            Check.equal("", stored(panel, "mouseup"), "the drag swallowed the mouseup");
        } finally {
            close(panel);
        }
    }

    /**
     * A place that does not accept (no cancelled <code>dragenter</code> or <code>dragover</code>) gives no drop: <code>dragleave</code> at the
     * target and <code>dragend</code> with the effect <code>none</code>.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNotAcceptedDragEndsWithoutDrop() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "other"), midY(panel, "other"));
            awaitContains(panel, "dnd", "dragenter@other");
            release(panel, midX(panel, "other"), midY(panel, "other"));
            awaitContains(panel, "ended", "none");
            String l = stored(panel, "dnd");
            Check.isTrue(l.indexOf("drop@") < 0, "no drop event: " + l);
            inOrder(l, "dragenter@other", "dragleave@BODY", "dragend@src");
        } finally {
            close(panel);
        }
    }

    /**
     * Moves of the pointer that arrive while <code>dragstart</code> is still being handled are not lost: the first iteration uses the position
     * that began the drag, and the next one the latest position (a slow <code>dragstart</code> listener keeps the document thread busy while the
     * pointer reaches the zone).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMovesDuringASlowDragstartAreNotLost() throws Exception {
        Panel panel = create(500, 600);
        try {
            open(panel, pageWith(BODY, SCRIPT + " src.addEventListener('dragstart', function () { var t = new Date().getTime(); while (new Date().getTime() - t < 400) { } });"));
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            Thread.sleep(150);
            moveTo(panel, (sx + midX(panel, "zone")) / 2, (sy + midY(panel, "zone")) / 2);
            moveTo(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "dnd", "dragenter@zone");
            release(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "ended", "copy");
            inOrder(stored(panel, "dnd"), "dragstart@src", "dragenter@zone", "drop@zone", "dragend@src");
        } finally {
            close(panel);
        }
    }

    /**
     * A press that does not move far enough is a click (no drag events, the <code>mouseup</code> arrives); a <code>dragstart</code> that is
     * cancelled stops the drag (<code>drag</code> is never fired) and the release is a normal one.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBelowTheThresholdAndCancelledDragstart() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 2, sy + 1);
            release(panel, sx + 2, sy + 1);
            drain(panel);
            Check.equal("", stored(panel, "dnd"), "no drag event for a tiny move");
            Check.equal("yes", stored(panel, "mouseup"), "the mouseup arrived");
            double nx = midX(panel, "nope");
            double ny = midY(panel, "nope");
            press(panel, nx, ny);
            moveTo(panel, nx + 20, ny + 20);
            moveTo(panel, nx + 40, ny + 40);
            release(panel, nx + 40, ny + 40);
            drain(panel);
            Check.equal("dragstart@nope", stored(panel, "dnd"), "the cancelled dragstart is the only event");
        } finally {
            close(panel);
        }
    }

    /**
     * The pointer events of the mouse pointer when a drag starts (Pointer Events 4.1.3.3): after <code>pointerdown</code> the start of the drag sends
     * <code>pointercancel</code>, <code>pointerout</code> and <code>pointerleave</code> to the target of the pointer and releases a captured pointer
     * (<code>lostpointercapture</code>); the drag takes the place of the pointer, so the release at the end sends no <code>pointerup</code>, and the
     * mouse events of a drag are not sent either; a <code>dragstart</code> that is cancelled starts no drag, so the pointer continues as usual and the
     * release sends <code>pointerup</code> and no <code>pointercancel</code>.
     * <p>
     * 끌기가 시작될 때 마우스 포인터의 포인터 이벤트(Pointer Events 4.1.3.3): <code>pointerdown</code> 뒤에 끌기가 시작되면 포인터의 대상에
     * <code>pointercancel</code>, <code>pointerout</code>, <code>pointerleave</code> 가 가고 붙잡힌 포인터는 풀린다(<code>lostpointercapture</code>). 끌기가
     * 포인터를 대신하므로 끝에서 뗄 때 <code>pointerup</code> 은 가지 않고 끌기의 마우스 이벤트도 가지 않는다. 취소된 <code>dragstart</code> 는 끌기를 시작하지
     * 않으므로 포인터는 평소처럼 이어지고 뗄 때 <code>pointerup</code> 이 가며 <code>pointercancel</code> 은 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragStartCancelsThePointer() throws Exception {
        String ptr = " var plog = []; var pnames = ['pointerdown', 'pointermove', 'pointerup', 'pointercancel', 'pointerout', 'pointerleave', 'gotpointercapture', 'lostpointercapture'];"
                + " for (var pi = 0; pi < pnames.length; pi++) { document.addEventListener(pnames[pi], function (e) { var id = (e.target.id || e.target.nodeName);"
                + "   if (e.type === 'pointermove' && plog[plog.length - 1] === 'pointermove@' + id) { return; }"
                + "   plog.push(e.type + '@' + id); put('ptr', plog.join(' ')); }, true); }"
                + " document.getElementById('src').addEventListener('pointerdown', function (e) { this.setPointerCapture(e.pointerId); });";
        Panel panel = create(500, 600);
        try {
            open(panel, pageWith(BODY, SCRIPT + ptr));
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            awaitContains(panel, "ptr", "pointerdown@src");
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "dnd", "dragover@zone");
            release(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "ended", "copy");
            drain(panel);
            String l = stored(panel, "ptr");
            inOrder(l, "pointerdown@src", "gotpointercapture@src", "pointercancel@src", "pointerout@src", "pointerleave@src");
            Check.isTrue(l.indexOf("pointerup@") < 0, "no pointerup for a drag: " + l);
            Check.isTrue(l.indexOf("lostpointercapture@") > l.indexOf("pointercancel@src"), "the capture is released by the cancel: " + l);
            Check.equal("", stored(panel, "mouseup"), "and no mouseup");
            // 취소된 dragstart 는 끌기를 시작하지 않으므로 포인터가 평소처럼 이어진다.
            double nx = midX(panel, "nope");
            double ny = midY(panel, "nope");
            press(panel, nx, ny);
            moveTo(panel, nx + 20, ny + 20);
            moveTo(panel, nx + 40, ny + 40);
            release(panel, nx + 40, ny + 40);
            drain(panel);
            String l2 = stored(panel, "ptr");
            String tail = l2.substring(l.length());
            Check.isTrue(tail.indexOf("pointerdown@nope") >= 0 && tail.indexOf("pointerup@") >= 0, "the pointer went on and the release sent pointerup: " + tail);
            Check.isTrue(tail.indexOf("pointercancel@") < 0, "a cancelled dragstart sends no pointercancel: " + tail);
        } finally {
            close(panel);
        }
    }

    /**
     * Escape ends the drag as a failed one: no <code>drop</code>, <code>dragend</code> with the effect <code>none</code>, and the key is not sent to the page.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEscapeCancelsTheDrag() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "dnd", "dragover@zone");
            tap(panel, KeyEvent.VK_ESCAPE, (char) 27, 0);
            awaitContains(panel, "ended", "none");
            release(panel, midX(panel, "zone"), midY(panel, "zone"));
            drain(panel);
            String l = stored(panel, "dnd");
            Check.isTrue(l.indexOf("drop@") < 0, "no drop after Escape: " + l);
            inOrder(l, "dragover@zone", "dragleave@zone", "dragend@src");
            Check.equal("", stored(panel, "dropped"), "the zone got no drop");
        } finally {
            close(panel);
        }
    }

    /**
     * Replacing the page during a drag ends the drag without any event for the new page, and the old page gets no more events: the repeat
     * timer stops and the key input works again.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReplacingThePageEndsTheDragSilently() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "dnd", "dragover@zone");
            panel.setContent("<!doctype html><p>another page</p>", "text/html");
            Thread.sleep(300);
            String before = stored(panel, "dnd") + "|" + stored(panel, "overs");
            Thread.sleep(1200);
            Check.equal(before, stored(panel, "dnd") + "|" + stored(panel, "overs"), "the old page gets no more drag events");
            Check.isFalse(stored(panel, "dnd").indexOf("drop@") >= 0, "and no drop");
            release(panel, midX(panel, "zone"), midY(panel, "zone"));
            Thread.sleep(200);
            Check.equal(before, stored(panel, "dnd") + "|" + stored(panel, "overs"), "the release after the replacement does nothing");
        } finally {
            close(panel);
        }
    }

    /**
     * The pointer standing still: the iterations go on every 350 milliseconds, so <code>dragover</code> repeats without any movement.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragoverRepeatsWhileThePointerStands() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "dnd", "dragover@zone");
            int first = Integer.parseInt(stored(panel, "overs"));
            Thread.sleep(1500);
            int later = Integer.parseInt(stored(panel, "overs"));
            Check.isTrue(later >= first + 2, "dragover repeated while the pointer stood still: " + first + " then " + later);
            release(panel, midX(panel, "zone"), midY(panel, "zone"));
            awaitContains(panel, "ended", "copy");
        } finally {
            close(panel);
        }
    }

    /**
     * A link and an image give their address as <code>text/uri-list</code> (and as <code>text/plain</code>); the data is read in
     * <code>dragstart</code>, which is the one event with a writable data transfer.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLinkGivesItsAddress() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double lx = midX(panel, "lnk");
            double ly = midY(panel, "lnk");
            press(panel, lx, ly);
            moveTo(panel, lx + 10, ly + 10);
            moveTo(panel, lx + 20, ly + 20);
            awaitContains(panel, "uri", "http://links.test/x");
            Check.equal("http://links.test/x#text/uri-list,text/plain", stored(panel, "uri"), "the link gives its address");
            release(panel, lx + 20, ly + 20);
            awaitContains(panel, "dnd", "dragend@lnk");
        } finally {
            close(panel);
        }
    }

    /**
     * Dragged text goes into a text field without a script: it is appended to the value, the field gets the <code>input</code> event with
     * <code>insertFromDrop</code>, and the drop is accepted (the effect is copy).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDropIntoATextField() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "inp"), midY(panel, "inp"));
            awaitContains(panel, "dnd", "dragenter@inp");
            release(panel, midX(panel, "inp"), midY(panel, "inp"));
            awaitContains(panel, "ended", "copy");
            await(panel, "inp", "abcpayload:insertFromDrop");
            inOrder(stored(panel, "dnd"), "dragenter@inp", "dragover@inp", "drop@inp", "dragend@src");
        } finally {
            close(panel);
        }
    }

    /**
     * Selected text of the page dragged into an editing host is inserted where it is dropped (<code>beforeinput</code> and
     * <code>input</code> of the type <code>insertFromDrop</code>) and selected; the paragraph keeps its text (a copy, because the source is
     * not editable).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragSelectedTextIntoAnEditingHost() throws Exception {
        Panel panel = create(500, 600);
        try {
            Rects rects = page(panel);
            // 문단의 첫 낱말을 더블 클릭으로 선택한다.
            double[] p = rect(panel, "sel");
            click(panel, p[0] + 3, p[1] + 10, 2, false);
            drain(panel);
            double[] e = rect(panel, "ed");
            double ex = e[0] + 7 * adv13(panel);
            press(panel, p[0] + 3, p[1] + 10);
            moveTo(panel, p[0] + 20, p[1] + 20);
            moveTo(panel, ex, e[1] + 10);
            awaitContains(panel, "dnd", "dragover@ed");
            release(panel, ex, e[1] + 10);
            awaitContains(panel, "dnd", "dragend@sel");
            await(panel, "text", "one twoselectable three");
            Check.contains(log(panel), "beforeinput:insertFromDrop:null input:insertFromDrop:null", "the editing events of a drop");
            Check.contains(stored(panel, "dnd"), "dragend@sel", "the source is the paragraph");
            awaitSel(panel, "7,17");
        } finally {
            close(panel);
        }
    }

    /**
     * A selection dragged inside an editing host is moved: it is deleted from the old place (<code>deleteByDrag</code>) and inserted at the new one
     * (<code>insertFromDrop</code>), and with Ctrl down it is copied; a drop into the selection itself changes nothing.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMoveTextInsideAnEditingHost() throws Exception {
        Panel panel = create(500, 600);
        try {
            Rects rects = page(panel);
            double[] e = rect(panel, "ed");
            double adv = adv13(panel);
            // "one two three" 에서 "two" 를 더블 클릭으로 고른다.
            click(panel, e[0] + 5 * adv, e[1] + 10, 2, false);
            awaitSel(panel, "4,7");
            press(panel, e[0] + 5 * adv, e[1] + 10);
            moveTo(panel, e[0] + 8 * adv, e[1] + 12);
            moveTo(panel, e[0] + 13 * adv - 1, e[1] + 10);
            awaitContains(panel, "dnd", "dragover@ed");
            release(panel, e[0] + 13 * adv - 1, e[1] + 10);
            awaitContains(panel, "dnd", "dragend@ed");
            await(panel, "text", "one  threetwo");
            drain(panel);
            Check.contains(log(panel), "beforeinput:deleteByDrag:null input:deleteByDrag:null", "the source was deleted as dragged away");
            Check.contains(log(panel), "beforeinput:insertFromDrop:null input:insertFromDrop:null", "and inserted as dropped");
        } finally {
            close(panel);
        }
    }

    /**
     * A drop onto a place that cannot be edited and that does not accept changes nothing (the editing host keeps its text), and the data transfer
     * of <code>dragstart</code> can be changed by the script (the type it added is what the zone sees).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReadOnlyTextTakesNoDrop() throws Exception {
        Panel panel = create(500, 600);
        try {
            page(panel);
            double sx = midX(panel, "src");
            double sy = midY(panel, "src");
            press(panel, sx, sy);
            moveTo(panel, sx + 10, sy + 10);
            moveTo(panel, midX(panel, "ro"), midY(panel, "ro"));
            awaitContains(panel, "dnd", "dragenter@ro");
            release(panel, midX(panel, "ro"), midY(panel, "ro"));
            awaitContains(panel, "ended", "none");
            drain(panel);
            Check.equal("read only", stored(panel, "ro"), "the read-only text was not changed");
            Check.isTrue(stored(panel, "dnd").indexOf("drop@") < 0, "no drop event: " + stored(panel, "dnd"));
        } finally {
            close(panel);
        }
    }
}
