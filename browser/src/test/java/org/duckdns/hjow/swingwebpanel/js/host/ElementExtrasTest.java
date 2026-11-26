package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the element members that need the page environment (plan item P15-03): inline styles, computed styles, dataset, layout hooks,
 * template contents, canvas contexts and the other helpers of {@link ElementExtras}.
 * <p>
 * 페이지 환경이 필요한 요소 멤버의 시험(계획 P15-03): 인라인 스타일, 계산 스타일, dataset, 레이아웃 훅, 템플릿 내용, 캔버스 컨텍스트, 그 밖의
 * {@link ElementExtras} 도우미.
 */
public class ElementExtrasTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ElementExtrasTest() {
    }

    /** Checks the rendered text fragment and its single child-list record. / 렌더링 텍스트 조각과 단일 자식 목록 기록을 확인한다. */
    public void testInnerTextSetter() {
        PageFixture f = new PageFixture().load("<div id=d><span>old</span></div>");
        Check.equal("1|1|6|br|<x>|br|br|z|br|http://www.w3.org/1999/xhtml|<x>z",
                f.scripts.evaluate("var d = document.getElementById('d'); var o = new MutationObserver(function () {});"
                        + "o.observe(d, {childList: true}); d.innerText = '\\r\\n<x>\\r\\r\\nz\\n'; var r = o.takeRecords(); var n = d.childNodes;"
                        + "[r.length,r[0].removedNodes.length,r[0].addedNodes.length,n[0].localName,n[1].data,n[2].localName,n[3].localName,n[4].data,"
                        + "n[5].localName,n[5].namespaceURI,d.textContent].join('|')", "t"), "text, line breaks, and one replacement");
        Check.equal("0|undefined", f.scripts.evaluate("d.innerText = null; var count = d.childNodes.length;"
                + "d.innerText = undefined; [count,d.firstChild.data].join('|')", "t"), "legacy null conversion and ordinary undefined conversion");
    }

    /**
     * P15-03: the inline style reads and writes the style attribute and understands camel-case, dashed and custom property names.
     * <p>
     * P15-03: 인라인 스타일은 style 속성을 읽고 쓰며 카멜 케이스, 대시, 사용자 정의 프로퍼티 이름을 이해한다.
     */
    public void testInlineStyle() {
        PageFixture f = new PageFixture().load("<div id=d style='color: red; margin: 1px 2px'></div>");
        Check.equal("red|1px 2px|5", f.scripts.evaluate("var s = document.getElementById('d').style; [s.color, s.margin, s.length].join('|')", "t"),
                "reading existing declarations");
        Check.equal("blue", f.scripts.evaluate("var s2 = document.getElementById('d').style; s2.color = 'blue'; s2.getPropertyValue('color')", "t"),
                "assignment through a camel-case name");
        Check.equal(true, f.document.getElementById("d").getAttribute("style").indexOf("color: blue") >= 0, "the attribute was updated");
        Check.equal("green", f.scripts.evaluate("var s3 = document.getElementById('d').style; s3.backgroundColor = 'green'; s3['background-color']", "t"),
                "camel-case and dashed names address the same property");
        Check.equal("10px", f.scripts.evaluate("var s4 = document.getElementById('d').style; s4.setProperty('--gap', '10px'); "
                + "s4.getPropertyValue('--gap')", "t"), "custom property");
        Check.equal("important", f.scripts.evaluate("var s5 = document.getElementById('d').style; s5.setProperty('width', '5px', 'important'); "
                + "s5.getPropertyPriority('width')", "t"), "priority");
        Check.equal("red", f.scripts.evaluate("var s6 = document.getElementById('d').style; s6.color = 'red'; s6.removeProperty('color'); "
                + "s6.color === '' ? 'red' : 'still'", "t"), "removeProperty");
        Check.equal("true", f.scripts.evaluate("var s7 = document.getElementById('d').style; s7.cssText = 'top: 0'; String(s7.length === 1 && s7.top === '0px' "
                + "|| s7.top === '0')", "t"), "cssText replaces the declarations");
        Check.equal("true", f.scripts.evaluate("var e = document.getElementById('d'); String(e.style === e.style)", "t"), "the style object is stable");
        Check.equal("5", f.scripts.evaluate("var s8 = document.getElementById('d').style; s8.notAProperty = 5; String(s8.notAProperty)", "t"),
                "other names are ordinary properties");
        Check.equal("[object CSSStyleDeclaration]", f.scripts.evaluate("Object.prototype.toString.call(document.body.style)", "t"), "tag");
    }

    /**
     * P15-03: dataset maps data attributes.
     * <p>
     * P15-03: dataset 은 data 속성에 대응한다.
     */
    public void testDataset() {
        PageFixture f = new PageFixture().load("<div id=d data-foo-bar='1' data-x='2'></div>");
        Check.equal("1,2,undefined", f.scripts.evaluate("var d = document.getElementById('d').dataset; [d.fooBar, d.x, String(d.missing)].join(',')", "t"),
                "reading");
        Check.equal("5,5", f.scripts.evaluate("var el = document.getElementById('d'); el.dataset.helloWorld = 5; el.getAttribute('data-hello-world') "
                + "+ ',' + el.dataset.helloWorld", "t"), "writing converts the name and the value");
        Check.equal("fooBar,x,helloWorld", f.scripts.evaluate("Object.keys(document.getElementById('d').dataset).join(',')", "t"), "keys");
        Check.equal("true", f.scripts.evaluate("var el2 = document.getElementById('d'); delete el2.dataset.x; String(!el2.hasAttribute('data-x'))", "t"),
                "delete removes the attribute");
        Check.equal("SyntaxError", f.scripts.evaluate("try { document.getElementById('d').dataset['a-b'] = 1 } catch (e) { e.name }", "t"),
                "names with a dash and a lower-case letter are rejected");
    }

    /**
     * P15-03: getComputedStyle reads from the style resolver of the page (here the real CSS engine).
     * <p>
     * P15-03: getComputedStyle 은 페이지의 스타일 리졸버(여기서는 실제 CSS 엔진)에서 읽는다.
     */
    public void testComputedStyle() {
        PageFixture f = new PageFixture().load("<style>#d { color: red; display: block } p { margin-top: 3px }</style><div id=d><p id=p>t</p></div>");
        CssEngine engine = new CssEngine(f.document, new StyleSheetLoader() {
            /**
             * No external style sheets in this test.
             * <p>
             * 이 시험에는 외부 스타일시트가 없다.
             *
             * @param url the URL / URL
             * @return never / 반환하지 않음
             * @throws java.io.IOException always / 항상
             */
            public LoadedStyleSheet load(String url) throws java.io.IOException {
                throw new java.io.IOException("none");
            }
        }, new MediaEnvironment());
        engine.loadStyleSheets();
        f.env.resolver = engine.getResolver();
        Check.equal("rgb(255, 0, 0)|block|3px", f.scripts.evaluate("var cs = getComputedStyle(document.getElementById('d')); "
                + "[cs.color, cs.getPropertyValue('display'), getComputedStyle(document.getElementById('p')).marginTop].join('|')", "t"),
                "computed values from the style sheet");
        Check.equal("NoModificationAllowedError|true", f.scripts.evaluate("var c2 = getComputedStyle(document.body); var ok; try { c2.color = 'blue'; ok = 'no error' } "
                + "catch (e) { ok = e.name } ok + '|' + String(c2.color !== 'blue')", "t"), "computed styles are read-only: assigning throws a NoModificationAllowedError");
        Check.equal("NoModificationAllowedError|NoModificationAllowedError|NoModificationAllowedError", f.scripts.evaluate("function errName(fn) { try { fn(); return 'none'; } "
                + "catch (e) { return e.name; } } [errName(function () { c2.setProperty('color', 'blue') }), errName(function () { c2.removeProperty('color') }), "
                + "errName(function () { c2.cssText = 'color: blue' })].join('|')", "t"), "and so do setProperty, removeProperty and cssText");
        Check.equal("TypeError", f.scripts.evaluate("try { getComputedStyle({}) } catch (e) { e.name }", "t"), "argument check");
        Check.equal("rgb(0, 0, 255)", f.scripts.evaluate("var el = document.getElementById('d'); el.style.color = 'blue'; "
                + "getComputedStyle(el).color", "t"), "inline style changes show up in the computed style");
    }

    /**
     * P15-03: layout hooks, template contents, canvas contexts and image state.
     * <p>
     * P15-03: 레이아웃 훅, 템플릿 내용, 캔버스 컨텍스트, 이미지 상태.
     */
    public void testLayoutHooksAndSpecialElements() {
        PageFixture f = new PageFixture().load("<div id=d></div><template id=t><p id=inner>x</p></template><canvas id=c></canvas><img id=i><img id=j src=x.png>"
                + "<a id=a href=x>link text</a>");
        Check.equal("0,0,0,0,0", f.scripts.evaluate("var r = document.getElementById('d').getBoundingClientRect(); [r.x, r.y, r.width, r.height, "
                + "document.getElementById('d').offsetWidth].join(',')", "t"), "layout hooks answer zero without layout");
        Check.equal("true,true", f.scripts.evaluate("var c = document.getElementById('t').content; String(c instanceof DocumentFragment) + ',' + "
                + "String(c.querySelector('#inner') !== null && document.getElementById('inner') === null)", "t"), "template content is inert");
        Check.equal("object,null,null,null", f.scripts.evaluate("var cv = document.getElementById('c'); [typeof cv.getContext('2d'), "
                + "String(cv.getContext('webgl')), String(cv.getContext('webgl2')), String(cv.getContext('webgpu'))].join(',')", "t"),
                "the 2D context exists; WebGL and WebGPU contexts are null (details in the canvas tests)");
        Check.equal("true,false,0", f.scripts.evaluate("[document.getElementById('i').complete, document.getElementById('j').complete, "
                + "document.getElementById('i').naturalWidth].join(',')", "t"), "image state");
        Check.equal("link text", f.scripts.evaluate("document.getElementById('a').text", "t"), "anchor text");
        Check.equal("true", f.scripts.evaluate("var n = 0; var b = document.getElementById('d'); b.addEventListener('click', function () { n++; }); "
                + "b.click(); String(n === 1)", "t"), "click() dispatches a click event");
        Check.equal("hello", f.scripts.evaluate("var p = document.getElementById('d'); p.innerText = 'hello'; p.innerText", "t"), "innerText");
    }
}
