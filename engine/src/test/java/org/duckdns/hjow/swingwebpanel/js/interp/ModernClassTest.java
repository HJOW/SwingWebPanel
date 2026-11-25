package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Execution tests for class fields, private elements and static blocks (plan item P28-03-b).
 * <p>
 * 클래스 필드, private 요소, static 블록 실행 시험(계획 P28-03-b).
 */
public class ModernClassTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ModernClassTest() {
    }

    /**
     * Checks public and computed instance fields and their initialization order.
     * <p>
     * public·계산 인스턴스 필드와 초기화 순서를 검사한다.
     */
    public void testInstanceFields() {
        Scripts.checkInline(
                "var log = '', keys = 0;"
                + "class Base { x = (log += 'b', 10); constructor(){ log += 'c'; } }"
                + "class Derived extends Base { [++keys] = (log += 'd', 20);"
                + " constructor(){ log += 'a'; super(); log += 'e'; } }"
                + "var d = new Derived();"
                + "t('order', log, 'abcde'); t('field 1', d.x, 10); t('computed key once', keys, 1);"
                + "t('computed value', d[1], 20); var d2 = new Derived();"
                + "t('computed per class', keys, 1); t('same field key', d2[1], 20);",
                6);
    }

    /**
     * Checks private fields, methods and accessor brands.
     * <p>
     * private 필드, 메서드, 접근자의 brand 를 검사한다.
     */
    public void testPrivateElements() {
        Check.equal("1", Scripts.eval("class Minimal { #x = 1; read(){ return this.#x; } } new Minimal().read()"),
                "minimal private field");
        String definition = "class Counter { #value = 1; #get(){ return this.#value; }"
                + " get #through(){ return this.#get(); } set #through(v){ this.#value = v; }"
                + " read(){ return this.#through; } write(v){ this.#through = v; }"
                + " branded(o){ return #value in o; } method(){ return this.#get; } } ";
        Check.equal("1", Scripts.eval(definition + "new Counter().read()"), "private field and getter read");
        Check.equal("4", Scripts.eval(definition + "var a=new Counter(); a.write(4); a.read()"), "private setter write");
        Scripts.checkInline(
                definition
                + "var a = new Counter(), b = new Counter();"
                + "t('initial', a.read(), 1); a.write(4); t('setter', a.read(), 4); t('separate', b.read(), 1);"
                + "t('brand true', a.branded(a), true); t('brand false', a.branded(bad = {}), false);"
                + "var stolen = a.method(); terr('method brand', function(){ stolen.call({}); }, 'TypeError');"
                + "terr('public receiver brand', function(){ Counter.prototype.read.call({}); }, 'TypeError');"
                + "var holder = {}; holder.m = Counter.prototype.method;"
                + "terr('extracted brand', function(){ holder.m.call({}).call({}); }, 'TypeError');",
                8);
    }

    /**
     * Checks static fields, static private elements, static blocks and super access.
     * <p>
     * static 필드·private 요소·static 블록과 super 접근을 검사한다.
     */
    public void testStaticElements() {
        Scripts.checkInline(
                "var order = '';"
                + "class Base { static seed = 4; }"
                + "class Values extends Base { static first = (order += '1');"
                + " static #secret = (order += '2', super.seed + 1);"
                + " static { order += '3'; this.third = super.seed + this.#secret; }"
                + " static read(){ return this.#secret; } static setSecret(v){ this.#secret = v; } }"
                + "t('order', order, '123'); t('first', Values.first, '1');"
                + "t('secret', Values.read(), 5); t('block', Values.third, 9);"
                + "Values.setSecret(7); t('static write', Values.read(), 7);"
                + "t('no private property', Object.keys(Values).indexOf('#secret'), -1);"
                + "terr('static brand', function(){ Values.read.call({}); }, 'TypeError');",
                7);
    }

    /**
     * Checks field initializers observe each earlier field only after it was defined.
     * <p>
     * 필드 초기화 식이 앞 필드가 정의된 뒤에만 보이는지 검사한다.
     */
    public void testFieldDefinitionTiming() {
        Scripts.checkInline(
                "class Timing { a = 1; b = this.a + 1; #p = this.b + 1;"
                + " static s = 2; static t = this.s + 1; read(){ return this.#p; } }"
                + "var x = new Timing();"
                + "t('public earlier', x.b, 2); t('private earlier', x.read(), 3);"
                + "t('static earlier', Timing.t, 3); t('not prototype', Timing.prototype.a, undefined);",
                4);
    }
}

