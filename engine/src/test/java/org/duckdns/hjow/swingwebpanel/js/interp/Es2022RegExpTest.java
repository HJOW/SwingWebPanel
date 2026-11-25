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
     * Checks Unicode 18 identifiers and the same code points in regular expression property escapes.
     * <p>
     * 유니코드 18 식별자와 정규식 속성 이스케이프에서 같은 코드 포인트를 확인한다.
     */
    public void testUnicode18Identifiers() {
        String armenian = String.valueOf((char) 0x0558);
        String jurchen = new String(Character.toChars(0x18E00));
        String combining = String.valueOf((char) 0x05C8);
        Check.equal("42", Scripts.eval("var " + armenian + combining + "=40; var " + jurchen
                + "=2; " + armenian + "\\u05C8+\\u{18E00}"), "literal and escaped identifier segments");
        Check.equal("true,true,false,true,true", Scripts.eval("var a='\\u0558', b='\\u05C8', c='\\u{18E00}';"
                + " [/\\p{ID_Start}/u.test(a),/\\p{ID_Continue}/u.test(b),/\\p{ID_Start}/u.test(b),"
                + " /\\p{Script=Jurchen}/u.test(c),/\\p{sc=Jurc}/u.test(c)].join()"), "same identifier classes and supplementary script");
        Check.equal("SyntaxError", Scripts.eval("try{eval('var \\u05C8a=1')}catch(e){e.name}"), "a combining mark cannot start an identifier");
    }

    /**
     * Checks exact aliases, default category and script values, and Script_Extensions overrides.
     * <p>
     * 별칭의 정확한 철자, 범주와 스크립트의 기본값, Script_Extensions 대체를 확인한다.
     */
    public void testUnicode18PropertyAliases() {
        Check.equal("true,true,false,true", Scripts.eval(
                "var a='\\u30FC'; [/\\p{sc=Zyyy}/u.test(a),/\\p{scx=Kana}/u.test(a),"
                + " /\\p{scx=Zyyy}/u.test(a),/\\p{Script_Extensions=Katakana}/u.test('\\u30AB')].join()"),
                "Script_Extensions overrides explicit Common and defaults to Script elsewhere");
        Check.equal("true,true,true,true,true,true", Scripts.eval("var u='\\u0378';"
                + " [/\\p{Cn}/u.test(u),/\\p{gc=Other}/u.test(u),/\\p{sc=Unknown}/u.test(u),"
                + " /\\P{Assigned}/u.test(u),/\\p{Bidi_M}/u.test('('),/\\p{CWKCF}/u.test('A')].join()"),
                "unassigned classification and derived binary properties");
        Check.equal("SyntaxError,SyntaxError,SyntaxError,SyntaxError,SyntaxError,SyntaxError,SyntaxError,SyntaxError",
                Scripts.eval("['script=Greek','Sc=Grek','Uppercase Letter','Greek','WhiteSpace','Hyphen','Emoji_Component','RI']"
                + ".map(function(name){try{new RegExp('\\\\p{'+name+'}','u');return 'accepted';}catch(e){return e.name;}}).join()"),
                "invalid variants, non-ECMAScript binary properties, and excluded emoji properties");
    }

    /**
     * Checks ECMAScript white space independently of Unicode's binary White_Space property.
     * <p>
     * ECMAScript 공백과 유니코드 이진 속성 White_Space를 구분해 확인한다.
     */
    public void testUnicode18WhiteSpace() {
        Check.equal("1,1,false,false,true,false,true,SyntaxError", Scripts.eval("var v='\\u180E', n='\\u0085', b='\\uFEFF';"
                + " var result=[v.trim().length,n.trim().length,/\\s/u.test(v),/\\s/u.test(n),"
                + " /\\p{White_Space}/u.test(n),/\\p{White_Space}/u.test(b),/\\s/u.test(b)];"
                + " try{eval(v+'42')}catch(e){result.push(e.name)} result.join()"), "white space across lexer, trim and RegExp");
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
