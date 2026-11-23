package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the interface names of the objects of a page (WebIDL "interface object" of <code>Navigator</code>, <code>Location</code>, <code>History</code>,
 * <code>Screen</code>, <code>Performance</code>, <code>DOMStringMap</code> and <code>MutationRecord</code>; HTML Standard section "Web application APIs", fetched
 * 2026-11-17; see <code>docs/progress/webapi2.md</code>): the global functions exist, the objects are instances of them and tell their name to
 * <code>Object.prototype.toString</code>, the interface objects cannot be called or constructed, and what the objects did before still works.
 * <p>
 * 페이지의 객체의 인터페이스 이름을 시험한다(<code>Navigator</code>, <code>Location</code>, <code>History</code>, <code>Screen</code>, <code>Performance</code>,
 * <code>DOMStringMap</code>, <code>MutationRecord</code> 의 WebIDL "인터페이스 객체", HTML Standard "Web application APIs" 절, 2026-11-17 조회,
 * <code>docs/progress/webapi2.md</code> 참조): 전역 함수가 있고, 객체가 그 인스턴스이며 <code>Object.prototype.toString</code> 에 자기 이름을 알리고, 인터페이스 객체를
 * 부르거나 만들 수 없으며, 객체가 전에 하던 일은 그대로 된다.
 */
public class InterfaceNamesTest {

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public InterfaceNamesTest() {
    }

    /**
     * Loads a page that has a dataset and a mutation observer with a record.
     * <p>
     * dataset 과 기록을 가진 변경 관찰자가 있는 페이지를 읽는다.
     *
     * @return the fixture / 시험 틀
     */
    private static PageFixture load() {
        PageFixture f = new PageFixture().load("<div id=a data-x-y='1'></div><script>window.observer = new MutationObserver(function () {}); "
                + "observer.observe(document.getElementById('a'), { attributes: true }); document.getElementById('a').setAttribute('title', 't'); "
                + "window.record = observer.takeRecords()[0];</" + "script>");
        f.env.scheduler.advance(10);
        return f;
    }

    /**
     * The interface objects are global functions whose prototype names the interface.
     * <p>
     * 인터페이스 객체는 프로토타입이 인터페이스 이름을 가진 전역 함수이다.
     */
    public void testInterfaceObjects() {
        PageFixture f = load();
        String names = "['Navigator', 'Location', 'History', 'Screen', 'Performance', 'DOMStringMap', 'MutationRecord']";
        Check.equal("true", f.scripts.evaluate("String(" + names + ".every(function (n) { var c = window[n]; return typeof c === 'function' && c.name === n && "
                + "c.prototype.constructor === c && Object.prototype.toString.call(c.prototype) === '[object ' + n + ']'; }))", "t"),
                "functions with a prototype that points back and names the interface");
        Check.equal("true", f.scripts.evaluate("String(" + names + ".every(function (n) { return /illegal constructor/i.test(msg(function () { new window[n](); })) && "
                + "/illegal constructor/i.test(msg(function () { window[n](); })); function msg(f) { try { f(); return ''; } catch (e) { return e.name + ': ' + e.message; } } }))", "t")
                , "constructing or calling is an Illegal constructor TypeError");
        Check.equal("TypeError: Illegal constructor", f.scripts.evaluate("try { new Navigator(); } catch (e) { e.name + ': ' + e.message }", "t"), "the message");
    }

    /**
     * The objects of the page are instances of their interfaces and tell their names.
     * <p>
     * 페이지의 객체는 자기 인터페이스의 인스턴스이고 이름을 알린다.
     */
    public void testInstances() {
        PageFixture f = load();
        Check.equal("true|true|true|true|true|true|true", f.scripts.evaluate("[navigator instanceof Navigator, location instanceof Location, history instanceof History, "
                + "screen instanceof Screen, performance instanceof Performance, document.getElementById('a').dataset instanceof DOMStringMap, "
                + "record instanceof MutationRecord].join('|')", "t"), "instanceof");
        Check.equal("[object Navigator]|[object Location]|[object History]|[object Screen]|[object Performance]|[object DOMStringMap]|[object MutationRecord]",
                f.scripts.evaluate("[navigator, location, history, screen, performance, document.getElementById('a').dataset, record]"
                        + ".map(function (o) { return Object.prototype.toString.call(o); }).join('|')", "t"), "Object.prototype.toString");
        Check.equal("true|true", f.scripts.evaluate("[Object.getPrototypeOf(navigator) === Navigator.prototype, Navigator.prototype.isPrototypeOf(navigator)].join('|')", "t"),
                "the prototype of the object is the prototype of the interface");
        Check.equal("false|false", f.scripts.evaluate("[navigator instanceof History, history instanceof Navigator].join('|')", "t"), "and of no other interface");
    }

    /**
     * What the objects could do before the interfaces still works.
     * <p>
     * 인터페이스가 생기기 전에 객체가 하던 일은 그대로 된다.
     */
    public void testMembersStillWork() {
        PageFixture f = load();
        Check.equal("true|true|true|true|1|attributes|title", f.scripts.evaluate("[typeof navigator.userAgent === 'string' && navigator.onLine === true, "
                + "typeof location.href === 'string' && typeof location.assign === 'function', typeof history.length === 'number' && typeof history.pushState === 'function', "
                + "typeof screen.width === 'number', document.getElementById('a').dataset.xY, record.type, record.attributeName].join('|')", "t"), "the members");
    }
}
