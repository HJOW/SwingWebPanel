package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Highlight;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the script interfaces of the CSS Custom Highlight API Level 1 (plan item P10-08-h, https://www.w3.org/TR/css-highlight-api-1/ WD 14 December
 * 2021): the <code>Highlight</code> constructor and its setlike members, <code>priority</code> and <code>type</code>, the maplike
 * <code>CSS.highlights</code>, the TypeErrors of wrong arguments, the selector <code>::highlight(name)</code>, and the repaint requests (section 5.1) when the
 * registry, a registered highlight or the boundary of a registered range changes.
 * <p>
 * CSS Custom Highlight API Level 1 의 스크립트 인터페이스 시험(계획 P10-08-h): <code>Highlight</code> 생성자와 setlike 멤버, <code>priority</code> 와 <code>type</code>,
 * maplike <code>CSS.highlights</code>, 틀린 인수의 TypeError, 선택자 <code>::highlight(이름)</code>, 등록부나 등록된 하이라이트나 등록된 범위의 경계가 바뀔 때의 다시 그리기
 * 요청(5.1 절).
 */
public class HighlightApiTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HighlightApiTest() {
    }

    /**
     * <code>Highlight</code> is a setlike of ranges with a priority 0 and the type <code>highlight</code> by default.
     * <p>
     * <code>Highlight</code> 는 범위의 setlike 이며 기본 우선순위 0, 종류 <code>highlight</code> 이다.
     */
    public void testHighlightSetlike() {
        PageFixture f = new PageFixture().load("<p id=p>One two three</p>");
        Check.equal("function|function|object", f.scripts.evaluate("[typeof Highlight, typeof HighlightRegistry, typeof CSS.highlights].join('|')", "t"), "the interfaces");
        Check.equal("2|0|highlight|true|true", f.scripts.evaluate("var t = document.getElementById('p').firstChild; var r1 = new Range(); r1.setStart(t, 0); r1.setEnd(t, 3); "
                + "var r2 = new StaticRange({startContainer: t, startOffset: 4, endContainer: t, endOffset: 7}); var h = new Highlight(r1, r2, r1); "
                + "[h.size, h.priority, h.type, h.has(r1), h.has(r2)].join('|')", "t"), "the constructor takes ranges and static ranges, once each");
        Check.equal("true|true|1|false|0", f.scripts.evaluate("[h.add(r1) === h, h.delete(r2), h.size, h.delete(r2), (h.clear(), h.size)].join('|')", "t"),
                "add returns the highlight, delete, clear");
        Check.equal("3|true,true,true|true", f.scripts.evaluate("var a = new Range(), b = new Range(), c = new Range(); var g = new Highlight(a, b, c); var seen = []; "
                + "g.forEach(function (v, k, s) { seen.push(v === k && s === g); }); [g.size, seen.join(','), [...g].length === 3 && [...g][1] === b].join('|')", "t"),
                "forEach and the iterator in insertion order");
        Check.equal("true|true", f.scripts.evaluate("var e = g.entries().next().value; [e[0] === a && e[1] === a, g.keys().next().value === a].join('|')", "t"), "entries and keys");
        Check.equal("5|spelling-error|spelling-error|-3", f.scripts.evaluate("g.priority = 5; var p = g.priority; g.type = 'spelling-error'; var t1 = g.type; g.type = 'nonsense'; "
                + "var t2 = g.type; g.priority = -3; [p, t1, t2, g.priority].join('|')", "t"), "priority and type (an unknown type is ignored)");
        Check.equal("TypeError|TypeError|false|TypeError", f.scripts.evaluate("var out = []; try { new Highlight(1) } catch (e) { out.push(e.name) } "
                + "try { g.add({}) } catch (e) { out.push(e.name) } out.push(g.has({})); try { Highlight() } catch (e) { out.push(e.name) } out.join('|')", "t"),
                "wrong arguments and a call without new");
    }

    /**
     * <code>CSS.highlights</code> is one maplike registry in registration order; <code>set</code> checks that the value is a highlight.
     * <p>
     * <code>CSS.highlights</code> 는 등록 순서를 지키는 maplike 등록부 하나이다. <code>set</code> 은 값이 하이라이트인지 확인한다.
     */
    public void testRegistryMaplike() {
        PageFixture f = new PageFixture().load("<p id=p>One two three</p>");
        Check.equal("true|true|true|2|true|undefined", f.scripts.evaluate("var h1 = new Highlight(), h2 = new Highlight(); var reg = CSS.highlights; "
                + "[reg === CSS.highlights, reg instanceof HighlightRegistry, reg.set('a', h1) === reg, (reg.set('b', h2), reg.size), reg.get('a') === h1, "
                + "String(reg.get('zz'))].join('|')", "t"), "the registry");
        Check.equal("a,b|a,b|true", f.scripts.evaluate("var names = []; reg.forEach(function (v, k, m) { names.push(k); }); "
                + "[names.join(','), [...reg.keys()].join(','), [...reg][1][1] === h2].join('|')", "t"), "forEach, keys and the iterator of [name, highlight]");
        Check.equal("a,b|true|1|false|0", f.scripts.evaluate("reg.set('a', h2); var order = [...reg.keys()].join(','); [order, reg.delete('a'), reg.size, reg.has('a'), (reg.clear(), reg.size)].join('|')",
                "t"), "setting an existing name keeps its place, delete and clear");
        Check.equal("TypeError", f.scripts.evaluate("try { reg.set('x', {}) } catch (e) { e.name }", "t"), "the value must be a Highlight");
        Check.isTrue(f.document.getHighlights().getSize() == 0, "the registry of the document is the one that scripts change");
        f.scripts.evaluate("reg.set('k', new Highlight())", "t");
        Highlight h = f.document.getHighlights().get("k");
        Check.notNull(h, "the Java side sees the highlight that a script registered");
    }

    /**
     * The selector <code>::highlight(name)</code> takes one identifier.
     * <p>
     * 선택자 <code>::highlight(이름)</code> 은 식별자 하나를 받는다.
     */
    public void testSelector() {
        PageFixture f = new PageFixture().load("<p>x</p>");
        Check.equal("true|true|false|false|false", f.scripts.evaluate("[CSS.supports('selector(::highlight(found))'), CSS.supports('selector(p::highlight(Found))'), "
                + "CSS.supports('selector(::highlight())'), CSS.supports('selector(::highlight(a b))'), CSS.supports('selector(::highlight(found) span)')].join('|')", "t"),
                "one identifier, last in the selector");
    }

    /**
     * Changing the registry or a registered highlight asks for a new painting; moving the boundary of a registered range asks for one at the end of the task;
     * a task that changes nothing does not.
     * <p>
     * 등록부나 등록된 하이라이트를 바꾸면 새 그리기를 요청하고, 등록된 범위의 경계를 옮기면 작업이 끝날 때 요청하며, 아무것도 바꾸지 않는 작업은 요청하지 않는다.
     */
    public void testRepaintRequests() {
        PageFixture f = new PageFixture().load("<p id=p>One two three</p>");
        f.scripts.evaluate("var t = document.getElementById('p').firstChild; var r = new Range(); r.setStart(t, 0); r.setEnd(t, 3); var h = new Highlight(r);", "t");
        int before = f.env.canvasChanges;
        f.scripts.evaluate("CSS.highlights.set('x', h)", "t");
        Check.isTrue(f.env.canvasChanges > before, "registering paints again");
        int after = f.env.canvasChanges;
        f.scripts.evaluate("var nothing = 1", "t");
        Check.equal(after, f.env.canvasChanges, "a task that changes nothing does not paint again");
        f.scripts.evaluate("r.setEnd(t, 7)", "t");
        Check.isTrue(f.env.canvasChanges > after, "moving the end of a registered range paints again");
        int moved = f.env.canvasChanges;
        f.scripts.evaluate("h.priority = 2", "t");
        Check.isTrue(f.env.canvasChanges > moved, "a new priority paints again");
        int prio = f.env.canvasChanges;
        f.scripts.evaluate("var unregistered = new Highlight(); unregistered.add(new Range())", "t");
        Check.equal(prio, f.env.canvasChanges, "a highlight that is not registered does not paint");
    }
}
