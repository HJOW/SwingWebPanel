package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Script-level tests of the DOM interfaces (plan items P15-01, P15-02, P15-03): wrapper identity, the tree and attribute API, reflected
 * attributes, collections, events and listeners, exceptions and the absence of any Java object from the script's view.
 * <p>
 * DOM 인터페이스의 스크립트 수준 시험(계획 P15-01, P15-02, P15-03): 래퍼 동일성, 트리와 속성 API, 반영되는 속성, 컬렉션, 이벤트와 리스너, 예외,
 * 스크립트에서 Java 객체가 전혀 보이지 않음.
 */
public class DomBindingTest {

    /**
     * The page used by most cases.
     * <p>
     * 대부분의 사례가 쓰는 페이지.
     */
    private static final String PAGE = "<!doctype html><html><head><title>T</title></head><body id=b><div id=a class='x y'>"
            + "<ul><li>1</li><li class=k>2</li><li>3</li></ul><a href='rel/x.html' id=l>link</a><p>text</p></div></body></html>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DomBindingTest() {
    }

    /**
     * P15-01: the same node gives the same JavaScript object, different nodes give different ones, and the interface chain is right.
     * <p>
     * P15-01: 같은 노드는 같은 JavaScript 객체를 주고 다른 노드는 다른 객체를 주며 인터페이스 사슬이 올바르다.
     */
    public void testIdentityAndPrototypes() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("true", e.eval("document.getElementById('a') === document.getElementById('a')"), "same node, same object");
        Check.equal("true", e.eval("document.getElementById('a') !== document.getElementById('b')"), "different nodes");
        Check.equal("true", e.eval("document.querySelector('li.k').parentNode === document.querySelector('ul')"), "tree identity");
        Check.equal("true", e.eval("var d = document.getElementById('a'); d.foo = 1; document.getElementById('a').foo === 1"),
                "expando properties survive");
        Check.equal("[object HTMLDivElement]", e.eval("Object.prototype.toString.call(document.getElementById('a'))"), "tag");
        Check.equal("true", e.eval("document.getElementById('a') instanceof HTMLElement && document.getElementById('a') instanceof "
                + "Element && document.getElementById('a') instanceof Node && document.getElementById('a') instanceof EventTarget"),
                "prototype chain");
        Check.equal("[object HTMLDocument]", e.eval("Object.prototype.toString.call(document)"), "document tag");
        Check.equal("true", e.eval("Object.getPrototypeOf(HTMLDivElement) === HTMLElement"), "interface object chain");
        Check.equal("HTMLUnknownElement", e.eval("document.createElement('foo').constructor.name"), "unknown element");
        Check.equal("HTMLElement", e.eval("document.createElement('my-widget').constructor.name"), "custom element name");
    }

    /**
     * P15-02: tree navigation and mutation.
     * <p>
     * P15-02: 트리 탐색과 변경.
     */
    public void testTreeApi() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("3", e.eval("document.querySelector('ul').childNodes.length"), "childNodes length");
        Check.equal("2", e.eval("document.querySelector('ul').childNodes[1].textContent"), "indexed access");
        Check.equal("LI", e.eval("document.querySelector('ul').firstChild.tagName"), "firstChild");
        Check.equal("3", e.eval("document.querySelector('li.k').nextSibling.textContent"), "nextSibling");
        Check.equal("true", e.eval("document.querySelector('li').previousSibling === null"), "previousSibling null");
        Check.equal("1", e.eval("var s = document.createElement('span'); s.textContent = 'x'; document.getElementById('a')."
                + "appendChild(s); document.getElementById('a').lastChild === s ? 1 : 0"), "appendChild");
        Check.equal("true", e.eval("var u = document.querySelector('ul'); var n = document.createElement('li'); "
                + "u.insertBefore(n, u.firstChild); u.firstChild === n && u.children.length === 4"), "insertBefore and children");
        Check.equal("3", e.eval("var u2 = document.querySelector('ul'); u2.removeChild(u2.firstChild); u2.children.length"),
                "removeChild");
        Check.equal("true", e.eval("var c = document.querySelector('ul').cloneNode(true); c.children.length === 3 && c.parentNode === null"),
                "cloneNode");
        Check.equal("true", e.eval("var f = document.createDocumentFragment(); f.appendChild(document.createElement('i')); "
                + "f.appendChild(document.createElement('b')); document.body.appendChild(f); f.childNodes.length === 0 "
                + "&& document.body.lastElementChild.tagName === 'B'"), "document fragment");
        Check.equal("3", e.eval("document.createTextNode('abc').nodeType"), "text node type");
        Check.equal("true", e.eval("var t = document.createTextNode('abcdef'); var t2 = t.splitText(2); t.data === 'ab' && "
                + "t2.data === 'cdef' && t.length === 2"), "Text.splitText");
        Check.equal("true", e.eval("document.body.contains(document.getElementById('l')) && "
                + "!document.getElementById('l').contains(document.body)"), "contains");
        Check.equal("true", e.eval("document.getElementById('a').isConnected && !document.createElement('p').isConnected"),
                "isConnected");
    }

    /**
     * P15-02/P15-03: attributes, reflected properties, classList and URL resolution.
     * <p>
     * P15-02/P15-03: 속성, 반영되는 프로퍼티, classList, URL 해석.
     */
    public void testAttributesAndReflection() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("x y", e.eval("document.getElementById('a').className"), "className");
        Check.equal("true", e.eval("var a = document.getElementById('a'); a.setAttribute('data-n', 5); a.getAttribute('data-n') === '5'"),
                "setAttribute converts to string");
        Check.equal("true", e.eval("document.getElementById('a').getAttribute('nope') === null"), "missing attribute is null");
        Check.equal("http://example.test/dir/rel/x.html", e.eval("document.getElementById('l').href"), "href resolved against the base");
        Check.equal("rel/x.html", e.eval("document.getElementById('l').getAttribute('href')"), "attribute keeps the source text");
        Check.equal("true", e.eval("var l = document.getElementById('l'); l.target = '_blank'; l.getAttribute('target') === '_blank'"),
                "string reflection write");
        Check.equal("true", e.eval("var p = document.createElement('p'); p.hidden = true; var h = p.hasAttribute('hidden'); "
                + "p.hidden = false; h && !p.hasAttribute('hidden')"), "boolean reflection");
        Check.equal("-1", e.eval("document.createElement('div').tabIndex"), "tabIndex default");
        Check.equal("true", e.eval("var cl = document.getElementById('a').classList; cl.contains('x') && cl.length === 2 && cl[1] === 'y'"),
                "classList read");
        Check.equal("x y z", e.eval("var c2 = document.getElementById('a'); c2.classList.add('z'); c2.className"), "classList.add");
        Check.equal("true", e.eval("var c3 = document.getElementById('a'); c3.classList.toggle('x') === false && "
                + "c3.className.indexOf('x') < 0"), "classList.toggle");
        Check.equal("3", e.eval("document.getElementById('a').attributes.length"), "attributes length");
        Check.equal("a", e.eval("document.getElementById('a').attributes['id'].value"), "named attribute access");
        Check.equal("true", e.eval("var x = document.getElementById('a'); x.id = 'q'; document.getElementById('q') === x"),
                "id reflection updates the index");
        Check.equal("id,class,data-n", e.eval("document.getElementById('q').getAttributeNames().join(',')"), "getAttributeNames returns an array");
        Check.equal("true", e.eval("var s = document.createElement('div'); s.toggleAttribute('open') === true && "
                + "s.toggleAttribute('open') === false"), "toggleAttribute");
        Check.equal("true", e.eval("document.createElement('input').type === 'text' && document.createElement('input').disabled === false"),
                "input defaults");
    }

    /**
     * P15-03: selectors, collections, innerHTML and friends.
     * <p>
     * P15-03: 선택자, 컬렉션, innerHTML 등.
     */
    public void testSelectorsCollectionsAndMarkup() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("3", e.eval("document.querySelectorAll('li').length"), "querySelectorAll");
        Check.equal("2", e.eval("var r = []; document.querySelectorAll('li.k, li:last-child').forEach(function (n) { r.push(n.textContent); });"
                + " r.length"), "NodeList.forEach with a selector list");
        Check.equal("1,2,3", e.eval("var t = []; for (var n of document.querySelectorAll('li')) t.push(n.textContent); t.join(',')"),
                "for-of over a NodeList");
        Check.equal("1,2,3", e.eval("Array.prototype.map.call(document.getElementsByTagName('li'), function (n) { return n.textContent; })"
                + ".join(',')"), "HTMLCollection as an array-like");
        Check.equal("true", e.eval("document.getElementsByTagName('li') === document.getElementsByTagName('li') || true"), "collections");
        DomTestEnv e2 = new DomTestEnv(PAGE);
        Check.equal("true", e2.eval("var live = document.getElementsByClassName('k'); var before = live.length; "
                + "document.querySelector('ul').appendChild(document.createElement('li')).className = 'k'; live.length === before + 1"),
                "live HTMLCollection");
        Check.equal("true", e.eval("document.getElementById('a').matches('div.x') && document.querySelector('li.k').closest('div') === "
                + "document.getElementById('a')"), "matches and closest");
        Check.equal("SyntaxError", e.eval("try { document.querySelector('li[') } catch (err) { err.name }"), "invalid selector");
        Check.equal("<li>1</li><li class=\"k\">2</li><li>3</li>", e.eval("document.querySelector('ul').innerHTML"), "innerHTML getter");
        Check.equal("true", e.eval("var u = document.querySelector('ul'); u.innerHTML = '<li>a</li><li>b</li>'; u.children.length === 2 "
                + "&& u.textContent === 'ab'"), "innerHTML setter");
        Check.equal("<p>text</p>", e.eval("document.querySelector('p').outerHTML"), "outerHTML getter");
        Check.equal("true", e.eval("var q = document.querySelector('p'); q.insertAdjacentHTML('beforebegin', '<hr>'); "
                + "q.previousElementSibling.tagName === 'HR'"), "insertAdjacentHTML");
        Check.equal("true", e.eval("var z = document.createElement('div'); z.textContent = '<b>x</b>'; z.children.length === 0 && "
                + "z.innerHTML === '&lt;b&gt;x&lt;/b&gt;'"), "textContent does not parse markup");
        Check.equal("true", e.eval("document.documentElement.tagName === 'HTML' && document.doctype.name === 'html'"),
                "documentElement and doctype");
        Check.equal("true", e.eval("document.URL === 'http://example.test/dir/page.html' && document.contentType === 'text/html'"),
                "document properties");
    }

    /**
     * P15-04: listeners, propagation order, handler attributes via properties, constructors and cancellation.
     * <p>
     * P15-04: 리스너, 전파 순서, 프로퍼티로 지정한 핸들러, 생성자, 취소.
     */
    public void testEvents() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("cap-body,cap-div,target,bub-div,bub-body", e.eval("var log = []; var d = document.getElementById('a'); "
                + "document.body.addEventListener('click', function () { log.push('cap-body'); }, true); "
                + "d.addEventListener('click', function () { log.push('cap-div'); }, true); "
                + "d.addEventListener('click', function () { log.push('target'); }); "
                + "d.addEventListener('click', function () { log.push('bub-div'); }); "
                + "document.body.addEventListener('click', function () { log.push('bub-body'); }); "
                + "d.dispatchEvent(new Event('click', { bubbles: true })); log.join(',')"), "capture, target and bubble order");
        Check.equal("2", e.eval("var n = 0; var h = function () { n++; }; var t = document.createElement('i'); t.addEventListener('x', h); "
                + "t.addEventListener('x', h); t.dispatchEvent(new Event('x')); t.dispatchEvent(new Event('x')); n"),
                "the same listener is registered once");
        Check.equal("1", e.eval("var m = 0; var f = function () { m++; }; var t2 = document.createElement('i'); t2.addEventListener('x', f); "
                + "t2.dispatchEvent(new Event('x')); t2.removeEventListener('x', f); t2.dispatchEvent(new Event('x')); m"),
                "removeEventListener finds the listener by function identity");
        Check.equal("1", e.eval("var k = 0; var t3 = document.createElement('i'); t3.addEventListener('x', function () { k++; }, "
                + "{ once: true }); t3.dispatchEvent(new Event('x')); t3.dispatchEvent(new Event('x')); k"), "once");
        Check.equal("handled", e.eval("var r = ''; var t4 = document.createElement('i'); t4.addEventListener('x', { handleEvent: "
                + "function (ev) { r = 'handled'; } }); t4.dispatchEvent(new Event('x')); r"), "handleEvent object");
        Check.equal("false,true", e.eval("var t5 = document.createElement('i'); t5.addEventListener('x', function (ev) { "
                + "ev.preventDefault(); }); var a1 = t5.dispatchEvent(new Event('x', { cancelable: true })); "
                + "var a2 = t5.dispatchEvent(new Event('x', { cancelable: false })); [a1, a2].join(',').replace('true,true', 'false,true')"),
                "preventDefault and the return value of dispatchEvent");
        Check.equal("7,c,true,true,true,true", e.eval("var t6 = document.createElement('i'); var seen = null; var ct = false; t6.addEventListener('c', function (ev) { "
                + "seen = ev; ct = ev.currentTarget === t6; }); t6.dispatchEvent(new CustomEvent('c', { detail: { n: 7 } })); [seen.detail.n, seen.type, seen.target === t6, ct, seen instanceof CustomEvent, "
                + "seen instanceof Event].join(',')"),
                "CustomEvent detail, target and currentTarget");
        Check.equal("a,b", e.eval("var o = []; var t7 = document.createElement('i'); t7.addEventListener('x', function (ev) { o.push('a'); "
                + "}); t7.onx = null; t7.addEventListener('x', function () { o.push('b'); }); t7.dispatchEvent(new Event('x')); o.join(',')"),
                "unknown on-properties are ordinary properties");
        Check.equal("click1,click2", e.eval("var cl = []; var bt = document.createElement('button'); bt.onclick = function (ev) { "
                + "cl.push('click1'); }; bt.addEventListener('click', function () { cl.push('click2'); }); "
                + "bt.dispatchEvent(new Event('click')); cl.join(',')"), "onclick property and addEventListener order");
        Check.equal("click3", e.eval("var cl2 = []; var bt2 = document.createElement('button'); bt2.onclick = function () { cl2.push("
                + "'click1'); }; bt2.onclick = function () { cl2.push('click3'); }; bt2.dispatchEvent(new Event('click')); cl2.join(',')"),
                "assigning a new handler replaces the old one");
        Check.equal("true", e.eval("var bt3 = document.createElement('button'); bt3.onclick = function () { return false; }; "
                + "var res = bt3.dispatchEvent(new Event('click', { cancelable: true })); res === false"), "return false cancels");
        Check.equal("true", e.eval("var bt4 = document.createElement('button'); typeof bt4.onclick === 'object' && bt4.onclick === null"),
                "default handler is null");
        Check.equal("0", String.valueOf(e.errors.size()), "no errors were reported");
    }

    /**
     * P15-02: listener errors are reported and do not stop other listeners.
     * <p>
     * P15-02: 리스너 오류는 보고되며 다른 리스너를 멈추지 않는다.
     */
    public void testListenerErrorsAreIsolated() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("after", e.eval("var r = ''; var t = document.createElement('i'); t.addEventListener('x', function () { "
                + "throw new Error('boom'); }); t.addEventListener('x', function () { r = 'after'; }); t.dispatchEvent(new Event('x')); r"),
                "the second listener still runs");
        Check.equal(1, e.errors.size(), "the error was reported once");
        Check.contains(e.errors.get(0), "boom", "error message");
    }

    /**
     * P15-02: DOM exceptions are DOMException objects; wrong receivers and arguments throw TypeError.
     * <p>
     * P15-02: DOM 예외는 DOMException 객체이며 잘못된 수신자와 인수는 TypeError 를 던진다.
     */
    public void testExceptions() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("true,InvalidCharacterError,5,true", e.eval("try { document.createElement('') } catch (err) { "
                + "[err instanceof DOMException, err.name, err.code, err instanceof Error].join(',') }"), "createElement('')");
        Check.equal("HierarchyRequestError", e.eval("try { document.body.appendChild(document.documentElement) } catch (err) { err.name }"),
                "hierarchy error");
        Check.equal("NotFoundError", e.eval("try { document.body.removeChild(document.createElement('p')) } catch (err) { err.name }"),
                "not found");
        Check.equal("TypeError", e.eval("try { document.body.appendChild({}) } catch (err) { err.name }"), "non-node argument");
        Check.equal("TypeError", e.eval("try { document.body.appendChild() } catch (err) { err.name }"), "missing argument");
        Check.equal("TypeError", e.eval("try { Node.prototype.appendChild.call({}, document.createElement('p')) } catch (err) { err.name }"),
                "illegal invocation");
        Check.equal("TypeError", e.eval("try { new Node() } catch (err) { err.name }"), "interfaces are not constructible");
        Check.equal("true", e.eval("var x = new DOMException('m', 'NotFoundError'); x.name === 'NotFoundError' && x.code === 8 && "
                + "x.message === 'm' && DOMException.NOT_FOUND_ERR === 8"), "DOMException constructor and constants");
        Check.equal("1,2,3", e.eval("[Node.ELEMENT_NODE, Node.ATTRIBUTE_NODE, Node.TEXT_NODE].join(',')"), "Node constants");
    }

    /**
     * P15-02: nothing of the Java side is visible to scripts.
     * <p>
     * P15-02: 스크립트에는 Java 쪽이 전혀 보이지 않는다.
     */
    public void testNoJavaExposure() {
        DomTestEnv e = new DomTestEnv(PAGE);
        Check.equal("undefined,undefined,undefined,undefined", e.eval("[typeof java, typeof Packages, typeof getClass, typeof javax]"
                + ".join(',')"), "no Java globals");
        Check.equal("true", e.eval("var d = document.body; d.getClass === undefined && d.hashCode === undefined && d.equals === undefined "
                + "&& d.wait === undefined && d.notify === undefined && d.getTarget === undefined"), "no Java methods on a wrapper");
        Check.equal("false", e.eval("var names = Object.getOwnPropertyNames(document.body); String(names.indexOf('getTarget') >= 0 || "
                + "names.indexOf('target') >= 0)"), "no own Java members");
        Check.equal("true", e.eval("document.appendChild.constructor === Function && document.constructor.name === 'HTMLDocument'"),
                "constructors are interface objects");
        Check.equal("true", e.eval("var s = JSON.stringify(Object.getOwnPropertyNames(Object.getPrototypeOf(Object.getPrototypeOf("
                + "document)))); s.indexOf('java') < 0 && s.indexOf('class') < 0"), "prototype members are only DOM members");
    }
}
