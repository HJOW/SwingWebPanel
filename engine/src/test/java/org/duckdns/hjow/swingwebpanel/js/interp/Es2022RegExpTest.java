package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests ES2018-ES2022 regular expression features.
 * <p>
 * ES2018-ES2022 정규식 기능을 검사한다.
 */
public final class Es2022RegExpTest {

    /**
     * Creates the suite.
     * <p>
     * 시험 묶음을 만든다.
     */
    public Es2022RegExpTest() {
    }

    /**
     * Checks named groups in exec, match, replace and indices.
     * <p>
     * exec, match, replace, indices 의 이름 있는 그룹을 검사한다.
     */
    public void testNamedGroupsAndIndices() {
        Check.equal("ab,0,2:1,2", Scripts.eval("var m=/(?<one>a)(?<two>b)/d.exec('ab');m.groups.one+m.groups.two+','+m.indices[0].join()+':'+m.indices.groups.two.join()"), "groups and indices");
        Check.equal("b:ac", Scripts.eval("'abc'.replace(/(?<x>a)(?<y>b)/,'$<y>:$<x>')"), "named replacement");
        Check.equal("x=A", Scripts.eval("'ab'.replace(/(?<x>a)(b)/,function(m,a,b,p,s,g){return 'x='+g.x.toUpperCase()})"), "named callback");
        Check.equal("undefined", Scripts.eval("/(?<x>a)|(?<y>b)/d.exec('b').indices.groups.x"), "unmatched named index");
    }

    /**
     * Checks positive and negative lookbehind including captured values.
     * <p>
     * 캡처 값을 포함한 긍정·부정 뒤쪽 탐색을 검사한다.
     */
    public void testLookbehind() {
        Check.equal("b,aaa", Scripts.eval("var a=/(?<=(a+))b/.exec('aaab');a[0]+','+a[1]"), "positive");
        Check.equal("b", Scripts.eval("/(?<!a)b/.exec('b')[0]"), "negative");
        Check.equal("null,true", Scripts.eval("/(?<!a)b/.exec('ab')+','+/(?<=a{2})b/.test('aab')"), "failure and variable length");
    }

    /**
     * Checks Unicode property escapes for categories, binary properties and scripts.
     * <p>
     * 범주, 이진 속성, script 의 유니코드 속성 escape 를 검사한다.
     */
    public void testUnicodeProperties() {
        try {
            Check.equal("true,true,false", Scripts.eval("/\\p{L}/u.test('한')+','+/\\p{Nd}/u.test('٣')+','+/\\P{L}/u.test('a')"), "categories");
            Check.equal("true", Scripts.eval("/\\p{Script=Greek}/u.test('Ω')"), "script");
        } catch (ExceptionInInitializerError e) {
            Check.fail("UnicodePropertyData initialization: " + e.getCause());
        }
        Check.equal("SyntaxError", Scripts.eval("try{new RegExp('\\\\p{L}')}catch(e){e.name}"), "u required");
        Check.equal("SyntaxError", Scripts.eval("try{new RegExp('\\\\p{Emoji}','u')}catch(e){e.name}"), "Emoji table is intentionally excluded");
    }

    /**
     * Checks String.matchAll and RegExp[Symbol.matchAll] iteration and empty matches.
     * <p>
     * String.matchAll 과 RegExp[Symbol.matchAll] 반복과 빈 일치를 검사한다.
     */
    public void testMatchAll() {
        Check.equal("a:0:b1:1", Scripts.eval("Array.from('ab1'.matchAll(/[a-z](?<n>\\d)?/g)).map(function(m){return m[0]+':'+(m.groups.n===undefined?m.index:m.groups.n)}).join(':')"), "matchAll");
        Check.equal("0,1", Scripts.eval("Array.from('ab'.matchAll(/(?=.)/g)).map(function(m){return m.index}).join()"), "empty");
        Check.equal("TypeError", Scripts.eval("try{Array.from('a'.matchAll(/a/))}catch(e){e.name}"), "global required");
        Check.equal("0", Scripts.eval("var it='a1a'.matchAll(/a./g);it.next().value.index"), "iterator has next");
    }

    /**
     * Checks RegExp flag accessors and canonical flag order including d.
     * <p>
     * d 를 포함한 RegExp 플래그 접근자와 표준 순서를 검사한다.
     */
    public void testIndicesFlagAccessor() {
        Check.equal("dg,true", Scripts.eval("/a/dg.flags+','+/a/dg.hasIndices"), "flags");
    }
}
