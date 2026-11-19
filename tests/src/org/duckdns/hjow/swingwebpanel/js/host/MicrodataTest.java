package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of microdata (W3C Working Group Note "HTML Microdata", 29 October 2013, plan item P20-10): the top-level items of
 * <code>document.getItems()</code>, the properties of an item with <code>itemref</code>, the names, the value of each kind of property
 * element, the reflecting attributes and the interfaces <code>HTMLPropertiesCollection</code> and <code>PropertyNodeList</code>.
 * <p>
 * 마이크로데이터(W3C 작업 그룹 노트 "HTML Microdata", 2013-10-29, 계획 P20-10)의 시험: <code>document.getItems()</code> 의 최상위 아이템, <code>itemref</code> 를 포함한
 * 아이템의 프로퍼티, 이름, 프로퍼티 요소 종류별 값, 반영 속성, 인터페이스 <code>HTMLPropertiesCollection</code>, <code>PropertyNodeList</code>.
 */
public class MicrodataTest {

    /**
     * The test page.
     * <p>
     * 시험 페이지.
     */
    private static final String PAGE = "<div id=p itemscope itemtype=\"http://schema.org/Person http://example.org/x\" itemref=\"r1 r2 nothere\">"
            + "<span id=n itemprop=name>Elizabeth</span><a id=l itemprop=\"url homepage\" href=\"/rel\">x</a>"
            + "<div id=a itemprop=address itemscope itemtype=\"http://schema.org/PostalAddress\"><span id=c itemprop=city>London</span></div>"
            + "<meta id=m itemprop=m content=\"mm\"><img id=i itemprop=photo src=\"/p.png\"><time id=t itemprop=born datetime=\"1926-04-21\">April 21</time>"
            + "<time id=t2 itemprop=born>noon</time><data id=d itemprop=num value=7>seven</data>"
            + "<p id=plain>not a property <b id=b itemprop=bold>bold</b></p></div>"
            + "<p id=r1 itemprop=extra>ref one</p><p id=r2><span id=r3 itemprop=extra>nested ref</span></p>"
            + "<section id=o itemscope itemtype=\"http://x/Other\"></section>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MicrodataTest() {
    }

    /**
     * <code>getItems()</code> lists the top-level items in tree order and filters by types (all the types must be among the item types).
     * <p>
     * <code>getItems()</code> 는 최상위 아이템을 트리 순서로 나열하고 타입으로 거른다(모든 타입이 아이템 타입에 있어야 한다).
     */
    public void testGetItems() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("p,o", f.scripts.evaluate("[].map.call(document.getItems(), function (e) { return e.id; }).join()", "t"), "the nested item is not top level");
        Check.equal("1|1|0|o", f.scripts.evaluate("[document.getItems('http://schema.org/Person').length, "
                + "document.getItems('http://schema.org/Person http://example.org/x').length, document.getItems('http://schema.org/Person http://other').length, "
                + "document.getItems('http://x/Other')[0].id].join('|')", "t"), "the types filter");
        Check.equal("[object NodeList]", f.scripts.evaluate("Object.prototype.toString.call(document.getItems())", "t"), "a NodeList");
        Check.equal("2", f.scripts.evaluate("var g = document.createElementNS('http://www.w3.org/2000/svg', 'g'); g.setAttribute('itemscope', ''); "
                + "document.body.appendChild(g); String(document.getItems().length)", "t"), "an SVG element is never an item");
    }

    /**
     * The properties of an item: tree order, the elements named by <code>itemref</code>, no descent into nested items, and the names.
     * <p>
     * 아이템의 프로퍼티: 트리 순서, <code>itemref</code> 가 가리키는 요소, 중첩 아이템 안으로 들어가지 않음, 이름.
     */
    public void testProperties() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("n,l,a,m,i,t,t2,d,b,r1,r3", f.scripts.evaluate("var p = document.getElementById('p'); [].map.call(p.properties, function (e) { return e.id; }).join()", "t"),
                "the properties in tree order");
        Check.equal("11", f.scripts.evaluate("String(p.properties.length)", "t"), "length");
        Check.equal("name,url,homepage,address,m,photo,born,num,bold,extra", f.scripts.evaluate("p.properties.names.join()", "t"), "the names without duplicates");
        Check.equal("r1", f.scripts.evaluate("p.properties.item(9).id", "t"), "item()");
        Check.equal("true", f.scripts.evaluate("String(p.properties === p.properties)", "t"), "the same collection every time");
        Check.equal("[object HTMLPropertiesCollection]|true|true", f.scripts.evaluate("Object.prototype.toString.call(p.properties) + '|' + (p.properties instanceof HTMLCollection) "
                + "+ '|' + (document.getElementById('plain').properties.length === 0)", "t"), "the interface; an element that is not an item has no properties");
        Check.equal("city|1", f.scripts.evaluate("var a = document.getElementById('a'); a.properties.names.join() + '|' + a.properties.length", "t"),
                "the properties of the nested item");
        f.scripts.evaluate("var s = document.createElement('span'); s.setAttribute('itemprop', 'added'); p.appendChild(s);", "t");
        Check.equal("12", f.scripts.evaluate("String(p.properties.length)", "t"), "the collection follows the tree");
        Check.equal("1", f.scripts.evaluate("document.body.insertAdjacentHTML('beforeend', '<div id=x itemscope itemref=x><span itemprop=k>v</span></div>'); "
                + "String(document.getElementById('x').properties.length)", "t"), "an itemref to the item itself does not loop");
    }

    /**
     * <code>namedItem()</code>, the named properties and <code>getValues()</code>.
     * <p>
     * <code>namedItem()</code>, 이름 있는 프로퍼티, <code>getValues()</code>.
     */
    public void testNamedProperties() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("2|2|[object PropertyNodeList]|true", f.scripts.evaluate("var p = document.getElementById('p'); var born = p.properties.namedItem('born'); "
                + "born.length + '|' + p.properties['born'].length + '|' + Object.prototype.toString.call(born) + '|' + (born instanceof NodeList)", "t"),
                "the elements of a name");
        Check.equal("1926-04-21,noon", f.scripts.evaluate("p.properties.namedItem('born').getValues().join()", "t"), "the values");
        Check.equal("null|undefined", f.scripts.evaluate("String(p.properties.namedItem('nope')) + '|' + typeof p.properties['nope']", "t"), "a name that no property has");
        Check.equal("true", f.scripts.evaluate("String(p.properties.namedItem('born') === p.properties.namedItem('born'))", "t"), "the same list every time");
        Check.equal("n|Elizabeth", f.scripts.evaluate("var nm = p.properties.namedItem('name'); nm[0].id + '|' + nm.getValues()[0]", "t"), "indexing the list");
        Check.equal("true", f.scripts.evaluate("var vs = p.properties.namedItem('address').getValues(); String(vs[0] === document.getElementById('a'))", "t"),
                "the value of a nested item is the element");
    }

    /**
     * The value of each kind of property element and its setter.
     * <p>
     * 프로퍼티 요소 종류별 값과 setter.
     */
    public void testItemValue() {
        PageFixture f = new PageFixture().load(PAGE);
        Check.equal("Elizabeth|http://example.test/rel|true|mm|http://example.test/p.png|1926-04-21|noon|7|null", f.scripts.evaluate("var g = function (id) { return document.getElementById(id); };"
                + "[g('n').itemValue, g('l').itemValue, g('a').itemValue === g('a'), g('m').itemValue, g('i').itemValue, g('t').itemValue, g('t2').itemValue, g('d').itemValue, "
                + "String(g('plain').itemValue)].join('|')", "t"), "the values");
        f.scripts.evaluate("g('n').itemValue = 'X'; g('l').itemValue = '/z'; g('m').itemValue = 'q'; g('t').itemValue = '2000-01-01'; g('d').itemValue = '9';", "t");
        Check.equal("X|/z|q|2000-01-01|9", f.scripts.evaluate("[g('n').textContent, g('l').getAttribute('href'), g('m').getAttribute('content'), g('t').getAttribute('datetime'), "
                + "g('d').getAttribute('value')].join('|')", "t"), "the setters write the content attribute that belongs to the element");
        Check.equal("InvalidAccessError|InvalidAccessError", f.scripts.evaluate("var e = function (fn) { try { fn(); return 'none'; } catch (x) { return x.name; } };"
                + "[e(function () { g('plain').itemValue = 'x'; }), e(function () { g('a').itemValue = 'x'; })].join('|')", "t"),
                "an element without itemprop and an item cannot be set");
    }

    /**
     * The reflecting attributes <code>itemScope</code>, <code>itemType</code>, <code>itemId</code>, <code>itemProp</code> and <code>itemRef</code>.
     * <p>
     * 반영 속성 <code>itemScope</code>, <code>itemType</code>, <code>itemId</code>, <code>itemProp</code>, <code>itemRef</code>.
     */
    public void testReflectingAttributes() {
        PageFixture f = new PageFixture().load(PAGE + "<div id=w itemscope itemid=\"urn:a\"></div>");
        Check.equal("true|false|2|true|3|urn:a|", f.scripts.evaluate("var g = function (id) { return document.getElementById(id); };"
                + "[g('p').itemScope, g('plain').itemScope, g('p').itemType.length, g('p').itemType.contains('http://example.org/x'), g('p').itemRef.length, g('w').itemId, g('p').itemId].join('|')",
                "t"), "reading");
        f.scripts.evaluate("g('plain').itemScope = true; g('n').itemProp = 'a b'; g('w').itemId = 'urn:b'; g('w').itemType.add('t1'); g('w').itemRef = 'r1';", "t");
        Check.equal("true|a b|2|urn:b|t1|r1", f.scripts.evaluate("[g('plain').hasAttribute('itemscope'), g('n').getAttribute('itemprop'), g('n').itemProp.length, "
                + "g('w').getAttribute('itemid'), g('w').getAttribute('itemtype'), g('w').getAttribute('itemref')].join('|')", "t"), "writing");
        f.scripts.evaluate("g('plain').itemScope = false;", "t");
        Check.equal("false", f.scripts.evaluate("String(g('plain').hasAttribute('itemscope'))", "t"), "itemScope = false removes the attribute");
    }
}
