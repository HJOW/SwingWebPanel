package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Execution tests for the ES2016-2021 expression semantics added by plan item P28-03-a.
 * <p>
 * 계획 P28-03-a가 추가한 ES2016~2021 식 실행 의미 시험.
 */
public class ModernExpressionTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModernExpressionTest() {
    }

    /**
     * Checks exponentiation and compound exponentiation on Number values.
     * <p>
     * Number 값의 거듭제곱과 복합 거듭제곱을 검사한다.
     */
    public void testExponentiation() {
        Scripts.checkInline(
                "t('right', 2 ** 3 ** 2, 512);"
                + "t('negative right', 2 ** -2, .25);"
                + "var x = 2; var n = 3; var calls = 0; var target = { get x(){ calls++; return x; },"
                + " set x(v){ x=v; calls++; }}; target.x **= n;"
                + "t('value', x, 8); t('reference twice', calls, 2);"
                + "t('negative zero', (-1) ** 3, -1);",
                5);
    }

    /**
     * Checks nullish and logical-assignment short-circuiting with one reference read.
     * <p>
     * nullish와 논리 대입이 참조를 한 번만 읽고 단락되는지 검사한다.
     */
    public void testLogicalAndAssignment() {
        Scripts.checkInline(
                "var hits = 0, stores = 0, value = 0;"
                + "var o = { get x(){ hits++; return value; }, set x(v){ stores++; value=v; } };"
                + "var fallback = function(){ hits += 10; return 7; };"
                + "t('nullish value', null ?? 3, 3); t('nullish falsy', 0 ?? 4, 0);"
                + "var a = o.x &&= fallback(); t('and short value', a, 0); t('and short reads', hits, 1);"
                + "t('and short stores', stores, 0); value = 2;"
                + "var b = o.x ||= fallback(); t('or short value', b, 2); t('or short reads', hits, 2);"
                + "t('or short stores 2', stores, 0); value = null;"
                + "var c = o.x ??= fallback(); t('nullish assign value', c, 7);"
                + "t('nullish assign reads', hits, 13); t('nullish assign stores', stores, 1); t('stored', value, 7);",
                12);
    }

    /**
     * Checks optional member and call chains, including skipped arguments and normal tails.
     * <p>
     * 선택적 멤버와 호출 체인의 단축, 인수 생략, 일반 꼬리를 검사한다.
     */
    public void testOptionalChains() {
        Scripts.checkInline(
                "var n = null, calls = 0;"
                + "t('member', n?.x, undefined); t('deep', n?.x.y(calls++), undefined);"
                + "t('computed', n?.[calls++].x, undefined); t('call args', n?.(calls++), undefined);"
                + "t('skipped', calls, 0);"
                + "var o = { x: { y: function(a){ calls++; return this === o.x ? a : -1; } } };"
                + "t('tail call this', o?.x.y(4), 4); t('called', calls, 1);"
                + "var u; t('optional member call', u?.f(1), undefined);"
                + "t('parenthesis ends', (o?.x).y(5), 5);",
                9);
    }

    /**
     * Checks object spread CopyDataProperties ordering, symbols and setter bypass.
     * <p>
     * 객체 spread 의 CopyDataProperties 순서, 심볼, setter 우회를 검사한다.
     */
    public void testObjectSpread() {
        Scripts.checkInline(
                "var order = '';"
                + "var first = {}; Object.defineProperty(first, 'a', { enumerable:true, get:function(){ order += 'a'; return 1; } });"
                + "var second = {}; Object.defineProperty(second, 'b', { enumerable:true, get:function(){ order += 'b'; return 2; } });"
                + "var hidden = 0; Object.defineProperty(Object.prototype, 'x', { set:function(){ hidden++; }, configurable:true });"
                + "var source = {x:3}; var symbol = Symbol('s'); first[symbol] = 4;"
                + "var result = {...first, ...second, ...source};"
                + "t('order', order, 'ab'); t('x own', Object.prototype.hasOwnProperty.call(result, 'x'), true);"
                + "t('x value', result.x, 3); t('setter skipped', hidden, 0);"
                + "t('symbols', Object.getOwnPropertySymbols(result).length, 1);"
                + "t('symbol value', result[symbol], 4); delete Object.prototype.x;",
                6);
    }

    /**
     * Checks object-rest binding and assignment with excluded symbol and getter order.
     * <p>
     * 제외 심볼과 getter 순서를 포함한 객체 나머지 바인딩·대입을 검사한다.
     */
    public void testObjectRest() {
        Scripts.checkInline(
                "var s = Symbol('s'), q = Symbol('q'), order = '';"
                + "var source = {a:1, b:2}; source[s] = 3; source[q] = 4;"
                + "Object.defineProperty(source, 'c', { enumerable:true, get:function(){ order += 'c'; return 5; } });"
                + "var {a, [s]: sv, ...rest} = source;"
                + "t('values', a + ':' + sv, '1:3'); t('rest b', rest.b, 2);"
                + "t('rest c', rest.c, 5); t('rest q', rest[q], 4);"
                + "t('excluded a', Object.prototype.hasOwnProperty.call(rest, 'a'), false);"
                + "t('excluded s', Object.prototype.hasOwnProperty.call(rest, s), false);"
                + "t('order', order, 'c'); var x; ({...x});",
                7);
    }

    /**
     * Verifies that a spread object expression ignores null and primitive values correctly.
     * <p>
     * spread 객체 식이 null 과 원시값을 규격대로 처리하는지 검사한다.
     */
    public void testSpreadEdgeCases() {
        Scripts.checkInline(
                "var text = {...'ab'}; t('string 0', text[0], 'a'); t('string 1', text[1], 'b');"
                + "var n = {...null, ...undefined, ...1}; t('keys', Object.keys(n).join(','), '');"
                + "var args = []; function f(){ for (var i=0;i<arguments.length;i++) args.push(arguments[i]); }"
                + "f(...[1,2], ...new Set([3])); t('args', args.join(','), '1,2,3');",
                4);
        Check.contains(Scripts.thrown("var {a, b = missing()} = null;"), "TypeError",
                "null object pattern fails before evaluating defaults");
    }
}

