package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the state members of the elements in {@link ElementStates} (plan item P20-01): <code>HTMLMapElement.areas</code>, the methods of
 * <code>HTMLDialogElement</code>, <code>HTMLTrackElement.readyState</code> and <code>track</code>, the members of <code>HTMLObjectElement</code> and
 * the default of <code>rowSpan</code>.
 * <p>
 * 요소 상태 멤버({@link ElementStates})의 시험(계획 P20-01): <code>HTMLMapElement.areas</code>, <code>HTMLDialogElement</code> 의 메서드,
 * <code>HTMLTrackElement.readyState</code> 와 <code>track</code>, <code>HTMLObjectElement</code> 의 멤버, <code>rowSpan</code> 의 기본값.
 */
public class ElementStatesTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ElementStatesTest() {
    }

    /**
     * P20-01: <code>map.areas</code> is the live collection of the <code>area</code> descendants and is the same object every time.
     * <p>
     * P20-01: <code>map.areas</code> 는 <code>area</code> 후손의 라이브 컬렉션이며 매번 같은 객체이다.
     */
    public void testMapAreas() {
        PageFixture f = new PageFixture().load("<map id=m name=n><area id=a1 shape=rect coords='0,0,5,5' href='/x'><div><area id=a2 shape=default></div></map>");
        Check.equal("2,a1,a2", f.scripts.evaluate("var m = document.getElementById('m'); m.areas.length + ',' + m.areas[0].id + ',' + m.areas[1].id", "t"),
                "areas lists the descendants in tree order");
        Check.equal("true", f.scripts.evaluate("var m1 = document.getElementById('m'); String(m1.areas === m1.areas)", "t"), "the same object every time");
        Check.equal("3", f.scripts.evaluate("var m2 = document.getElementById('m'); var c = m2.areas; var n = document.createElement('area'); "
                + "m2.appendChild(n); String(c.length)", "t"), "the collection is live");
        Check.equal("[object HTMLCollection]", f.scripts.evaluate("Object.prototype.toString.call(document.getElementById('m').areas)", "t"), "tag");
        Check.equal("[object HTMLAreaElement]", f.scripts.evaluate("Object.prototype.toString.call(document.getElementById('m').areas[0])", "t"),
                "the items are area elements");
    }

    /**
     * P20-01: <code>dialog.show()</code>, <code>showModal()</code> and <code>close()</code> change the <code>open</code> attribute, keep the
     * <code>returnValue</code> and fire the <code>close</code> event as a task.
     * <p>
     * P20-01: <code>dialog.show()</code>, <code>showModal()</code>, <code>close()</code> 는 <code>open</code> 속성을 바꾸고
     * <code>returnValue</code> 를 보관하며 <code>close</code> 이벤트를 작업으로 발생시킨다.
     */
    public void testDialog() {
        PageFixture f = new PageFixture().load("<dialog id=d>hello</dialog>");
        Check.equal("false,", f.scripts.evaluate("var d = document.getElementById('d'); d.open + ',' + d.returnValue", "t"), "initial state");
        Check.equal("true", f.scripts.evaluate("var d1 = document.getElementById('d'); d1.show(); String(d1.open && d1.hasAttribute('open'))", "t"), "show");
        Check.equal("true", f.scripts.evaluate("var d2 = document.getElementById('d'); d2.show(); String(d2.open)", "t"), "show again does nothing");
        Check.equal("InvalidStateError", f.scripts.evaluate("try { document.getElementById('d').showModal() } catch (e) { e.name }", "t"),
                "showModal of an open dialog");
        f.scripts.evaluate("window.closed = 0; var d3 = document.getElementById('d'); d3.addEventListener('close', function (e) { window.closed++; "
                + "window.bubbled = e.bubbles; }); d3.close('done');", "t");
        Check.equal("false,done", f.scripts.evaluate("var d4 = document.getElementById('d'); d4.open + ',' + d4.returnValue", "t"), "close sets the value");
        Check.equal("0", f.scripts.evaluate("String(window.closed)", "t"), "the event is a task, not synchronous");
        f.advance(1);
        Check.equal("1,false", f.scripts.evaluate("window.closed + ',' + window.bubbled", "t"), "the close event does not bubble");
        f.scripts.evaluate("document.getElementById('d').close('again');", "t");
        f.advance(1);
        Check.equal("1,done", f.scripts.evaluate("window.closed + ',' + document.getElementById('d').returnValue", "t"),
                "closing a closed dialog does nothing");
        Check.equal("true", f.scripts.evaluate("var d5 = document.getElementById('d'); d5.showModal(); String(d5.open)", "t"), "showModal of a closed dialog");
        Check.equal("InvalidStateError", f.scripts.evaluate("try { document.createElement('dialog').showModal() } catch (e) { e.name }", "t"),
                "showModal of a dialog that is not in the document");
        Check.equal("kept", f.scripts.evaluate("var d6 = document.getElementById('d'); d6.returnValue = 'kept'; d6.close(); d6.returnValue", "t"),
                "close without an argument keeps the value");
    }

    /**
     * P20-01: <code>track.readyState</code> is <code>NONE</code> until a track is fetched and <code>track.track</code> follows the attributes (the
     * fetch and the cues are tested by <code>TextTrackTest</code>).
     * <p>
     * P20-01: <code>track.readyState</code> 는 트랙을 가져오기 전에는 <code>NONE</code> 이고 <code>track.track</code> 은 속성을 따른다(가져오기와 cue 는
     * <code>TextTrackTest</code> 가 시험한다).
     */
    public void testTrack() {
        PageFixture f = new PageFixture().load("<video><track id=t kind=captions label='En' srclang=en src='/a.vtt'></video>");
        Check.equal("0,0,1,2,3", f.scripts.evaluate("var t = document.getElementById('t'); [t.readyState, HTMLTrackElement.NONE, HTMLTrackElement.LOADING, "
                + "HTMLTrackElement.LOADED, HTMLTrackElement.ERROR].join(',')", "t"), "state and constants");
        Check.equal("captions,En,en,disabled,null,null", f.scripts.evaluate("var tt = document.getElementById('t').track; [tt.kind, tt.label, tt.language, "
                + "tt.mode, String(tt.cues), String(tt.activeCues)].join(',')", "t"), "text track members");
        Check.equal("metadata,subtitles", f.scripts.evaluate("var t2 = document.getElementById('t'); t2.setAttribute('kind', 'bogus'); var k1 = t2.track.kind; "
                + "t2.removeAttribute('kind'); k1 + ',' + t2.track.kind", "t"), "an unknown kind reads as metadata and a missing one as subtitles");
        Check.equal("showing,showing", f.scripts.evaluate("var t3 = document.getElementById('t'); t3.track.mode = 'showing'; t3.track.mode = 'wrong'; "
                + "t3.track.mode + ',' + t3.track.mode", "t"), "mode accepts only the known values");
        Check.equal("true", f.scripts.evaluate("var t4 = document.getElementById('t'); String(t4.track === t4.track)", "t"), "the same object every time");
    }

    /**
     * P20-01: the members of <code>object</code>: form owner, no content document, barred from constraint validation.
     * <p>
     * P20-01: <code>object</code> 의 멤버: 폼 소유자, 내용 문서 없음, 제약 검증 대상 아님.
     */
    public void testObject() {
        PageFixture f = new PageFixture().load("<form id=f1><object id=o1></object></form><form id=f2></form><object id=o2 form=f2></object>"
                + "<object id=o3></object>");
        Check.equal("f1", f.scripts.evaluate("document.getElementById('o1').form.id", "t"), "the ancestor form");
        Check.equal("f2", f.scripts.evaluate("document.getElementById('o2').form.id", "t"), "the form attribute");
        Check.equal("null", f.scripts.evaluate("String(document.getElementById('o3').form)", "t"), "no form");
        Check.equal("null,null,null", f.scripts.evaluate("var o = document.getElementById('o1'); [String(o.contentDocument), String(o.contentWindow), String(o.getSVGDocument())]"
                + ".join(',')", "t"), "no nested document");
        Check.equal("false,,true,true", f.scripts.evaluate("var o4 = document.getElementById('o1'); o4.setCustomValidity('x'); [o4.willValidate, "
                + "o4.validationMessage, o4.checkValidity(), o4.reportValidity()].join(',')", "t"), "barred from constraint validation");
    }

    /**
     * P20-01: the script members <code>insertAdjacentElement</code>, <code>insertAdjacentText</code> and <code>replaceChildren</code>.
     * <p>
     * P20-01: 스크립트 멤버 <code>insertAdjacentElement</code>, <code>insertAdjacentText</code>, <code>replaceChildren</code>.
     */
    public void testInsertAdjacentAndReplaceChildren() {
        PageFixture f = new PageFixture().load("<div id=host><p id=p>old</p></div>");
        Check.equal("true,x|P|S|y", f.scripts.evaluate("var h = document.getElementById('host'); var s = document.createElement('s'); "
                + "var r = h.insertAdjacentElement('beforeend', s); h.insertAdjacentText('afterbegin', 'x'); h.insertAdjacentText('beforeend', 'y'); "
                + "String(r === s) + ',' + Array.prototype.map.call(h.childNodes, function (n) { return n.nodeType === 3 ? n.data : n.tagName; })"
                + ".join('|')", "t"), "positions and the return value");
        Check.equal("null", f.scripts.evaluate("String(document.createElement('u').insertAdjacentElement('afterend', document.createElement('s')))", "t"),
                "no parent gives null");
        Check.equal("SyntaxError", f.scripts.evaluate("try { document.getElementById('host').insertAdjacentText('middle', 'z') } catch (e) { e.name }", "t"),
                "unknown position");
        Check.equal("2|B|z", f.scripts.evaluate("var h2 = document.getElementById('host'); h2.replaceChildren(document.createElement('b'), 'z'); "
                + "h2.childNodes.length + '|' + h2.firstChild.tagName + '|' + h2.lastChild.data", "t"), "replaceChildren");
        Check.equal("0", f.scripts.evaluate("var h3 = document.getElementById('host'); h3.replaceChildren(); String(h3.childNodes.length)", "t"),
                "replaceChildren without arguments empties the element");
        Check.equal("1,HTML", f.scripts.evaluate("document.replaceChildren(document.createElement('html')); "
                + "document.childNodes.length + ',' + document.documentElement.tagName", "t"), "replaceChildren of the document");
    }

    /**
     * P20-01: <code>relList</code> is the live token list of <code>rel</code> (the same object every time, writable as a string) and the new
     * reflecting properties of links and images.
     * <p>
     * P20-01: <code>relList</code> 는 <code>rel</code> 의 라이브 토큰 목록이며(매번 같은 객체, 문자열로 쓸 수 있음) 링크와 이미지의 새 반영 프로퍼티.
     */
    public void testRelListAndLinkReflection() {
        PageFixture f = new PageFixture().load("<a id=a href='/x' rel='noopener nofollow' referrerpolicy=origin></a><img id=i loading=lazy decoding=async>");
        Check.equal("2,noopener,nofollow,true,[object DOMTokenList]", f.scripts.evaluate("var a = document.getElementById('a'); [a.relList.length, a.relList[0], "
                + "a.relList[1], a.relList === a.relList, Object.prototype.toString.call(a.relList)].join(',')", "t"), "reading");
        Check.equal("noopener nofollow external|true", f.scripts.evaluate("a.relList.add('external'); a.rel + '|' + a.relList.contains('external')", "t"), "the list is live");
        Check.equal("x y|2", f.scripts.evaluate("a.relList = 'x y'; a.getAttribute('rel') + '|' + a.relList.length", "t"), "assigning a string sets the attribute");
        Check.equal("origin|lazy|async|", f.scripts.evaluate("var i = document.getElementById('i'); [a.referrerPolicy, i.loading, i.decoding, i.lowsrc].join('|')", "t"), "reflecting properties");
    }

    /**
     * P20-01: <code>document.createAttributeNS</code> makes an attribute node of a qualified name in a namespace.
     * <p>
     * P20-01: <code>document.createAttributeNS</code> 는 네임스페이스의 정규화된 이름으로 속성 노드를 만든다.
     */
    public void testCreateAttributeNs() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("xlink:href,http://www.w3.org/1999/xlink,xlink,href", f.scripts.evaluate("var a = document.createAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href'); "
                + "[a.name, a.namespaceURI, a.prefix, a.localName].join()", "t"), "a qualified name in a namespace");
        Check.equal("NamespaceError", f.scripts.evaluate("try { document.createAttributeNS(null, 'a:b') } catch (e) { e.name }", "t"), "a prefix needs a namespace");
    }

    /**
     * P20-01: <code>rowSpan</code> reads 1 when the attribute is missing or wrong and is limited to 0..65534; <code>colSpan</code> is at least 1.
     * <p>
     * P20-01: <code>rowSpan</code> 은 속성이 없거나 잘못되면 1 을 읽고 0~65534 로 제한한다. <code>colSpan</code> 은 1 이상이다.
     */
    public void testRowSpanDefault() {
        PageFixture f = new PageFixture().load("<table><tr><td id=a></td><td id=b rowspan=0></td><td id=c rowspan=3 colspan=0></td>"
                + "<td id=d rowspan=70000></td><td id=e rowspan=x></td></tr></table>");
        Check.equal("1,0,3,65534,1", f.scripts.evaluate("['a','b','c','d','e'].map(function (i) { return document.getElementById(i).rowSpan; }).join(',')",
                "t"), "rowSpan");
        Check.equal("1,1", f.scripts.evaluate("[document.getElementById('a').colSpan, document.getElementById('c').colSpan].join(',')", "t"), "colSpan");
        Check.equal("7", f.scripts.evaluate("var c = document.getElementById('a'); c.rowSpan = 7; c.getAttribute('rowspan')", "t"), "writing");
    }
}
