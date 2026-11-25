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
     * Checks repetition order and disjunction priority inside lookbehind.
     * <p>
     * 뒤쪽 탐색에서 반복 평가 순서와 대안의 우선순위를 확인한다.
     */
    public void testReverseLookbehindPriority() {
        Check.equal("a,bc,0,1,1,3", Scripts.eval("var m=/(?<=([ab]+)([bc]+))$/d.exec('abc');"
                + "[m[1],m[2],m.indices[1],m.indices[2]].join()"), "rightmost capture is greedy first");
        Check.equal("a,ba,a,ab,2,4", Scripts.eval("var m=/(?<=a([ab]+))c/d.exec('baabc');"
                + "[/(?<=(a|ba))c/.exec('bac')[1],/(?<=(ba|a))c/.exec('bac')[1],"
                + "/(?<=(a+?))b/.exec('aaab')[1],m[1],m.indices[1]].join()"),
                "alternatives retain source priority and repetitions can backtrack toward the start position");
    }

    /**
     * Checks capture and reference semantics in nested assertions and supplementary text.
     * <p>
     * 중첩 단언과 보충 문자에서 캡처와 역참조 동작을 확인한다.
     */
    public void testReverseLookbehindCapturesAndReferences() {
        Check.equal("a,false,a,4,2,4,2,0,2", Scripts.eval(
                "var r=/(?<=\\1(.))x/diu.exec('\\u{10400}\\u{10428}x');"
                + "var s=/(?<=(.))x/du.exec('\\u{1D11E}x');"
                + "[/(?<=\\1(a))b/.exec('aab')[1],/(?<=\\1(a))b/.test('ab'),"
                + "/(?<=(a)\\1)b/.exec('ab')[1],r.index,r.indices[1],s.index,s.indices[1]].join()"),
                "backward references use captured code points and report UTF-16 positions");
        Check.equal("b,1,2,true,,,a,false", Scripts.eval("var m=/(?<=a(?=(b)))b/d.exec('ab');"
                + "[m[1],m.indices[1],/(?<=(?<=a)b)c/.test('abc'),"
                + "/(?<!(a))b/d.exec('b')[1],/(?:(?<=(a))xz|x)/d.exec('ax')[1],"
                + "/(?<n>a)(?<=a)\\k<n>/d.exec('aa').groups.n,/^(.)\\1/u.test('\\uD800\\uD800\\uDC00')].join()"),
                "inner direction is local, captured slots roll back, and forward references respect scalar boundaries");
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
        Check.equal("true,false,true", Scripts.eval(
                "[/\\p{L}/.test('p{L}'),/\\p{L}/.test('a'),/[\\P]/.test('P')].join()"),
                "legacy identity escapes outside Unicode mode");
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
     * Checks simple Unicode folding, legacy canonicalization and Unicode group names.
     * <p>
     * 유니코드 단순 접기, 레거시 정규형, 유니코드 그룹 이름을 확인한다.
     */
    public void testUnicode18RegExpCaseFolding() {
        Check.equal("true,true,true,true,false,false", Scripts.eval("var low='\\u0277', adlam='\\u{1E922}';"
                + " [/^\\uA7DD$/iu.test(low),/^[\\uA7DD]$/iu.test(low),/^\\uA7DD$/i.test(low),"
                + " /^[\\u{1E900}]$/iu.test(adlam),/^\\u{1E900}$/iu.test('A'),/^\\u{1E900}$/i.test(adlam)].join()"),
                "full scalar folding and the legacy code unit boundary");
        Check.equal("true,false,true,false,false", Scripts.eval(
                "[/^[a-z]+$/iu.test('\\u017F\\u212A'),/^[a-z]+$/i.test('\\u017F\\u212A'),"
                + " /^\\u00DF$/iu.test('\\u1E9E'),/^\\u00DF$/iu.test('SS'),/^\\u0130$/iu.test('i')].join()"),
                "simple folds exclude full and Turkic expansions and legacy non-ASCII to ASCII folds");
        Check.equal("x:xx", Scripts.eval("var m=/(?<\\u{18E00}>x)\\k<\\u{18E00}>/u.exec('xx');"
                + "m.groups['\\u{18E00}']+':'+m[0]"), "newly assigned supplementary group name and back reference");
        Check.equal("true", Scripts.eval("/(?<\\uD823\\uDE00>x)\\k<\\u{18E00}>/.test('xx')"),
                "group name escapes use UnicodeMode even without a pattern flag");
    }

    /**
     * Checks full case mappings and contextual final sigma with updated Unicode properties.
     * <p>
     * 갱신한 유니코드 속성의 전체 대소문자 매핑과 문맥에 따른 어말 시그마를 확인한다.
     */
    public void testUnicode18StringCasing() {
        Check.equal("true,true,true,true,true,true", Scripts.eval(
                "['\\u0277\\u00DF'.toUpperCase()==='\\uA7DDSS','\\uA7DD\\u0130'.toLowerCase()==='\\u0277i\\u0307',"
                + " '\\uA7DD\\u03A3'.toLowerCase()==='\\u0277\\u03C2',"
                + " 'A\\u03A3\\u05C8\\uA7DD'.toLowerCase()==='a\\u03C3\\u05C8\\u0277',"
                + " '\\u{1E900}'.toLocaleLowerCase()==='\\u{1E922}','\\uD800'.toUpperCase()==='\\uD800'].join()"),
                "multi-code-point mapping, sigma context, supplementary casing, and unpaired surrogates");
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
