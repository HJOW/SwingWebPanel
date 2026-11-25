package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the methods of <code>Array.prototype</code> (ECMA-262 6th edition 22.1.3) at the Java level: holes, array-likes, generic receivers,
 * stable sorting, error conditions, limits and interruption.
 * <p>
 * <code>Array.prototype</code> 메서드의 Java 수준 시험(ECMA-262 6판 22.1.3): 구멍, 유사 배열, 일반 수신자, 안정 정렬, 오류 조건, 한계값, 인터럽트.
 */
public class ArrayMethodsTest {

    /**
     * Test environment.
     * <p>
     * 시험 환경.
     */
    private Env e;

    /**
     * Array.prototype.
     * <p>
     * 시험에 쓰는 Array.prototype 객체.
     */
    private JsObject ap;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ArrayMethodsTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        e = new Env();
        ap = e.realm.getArrayPrototype();
    }

    /**
     * Calls an Array.prototype method on a receiver.
     * <p>
     * 수신자에 Array.prototype 메서드를 호출한다.
     *
     * @param recv receiver / 수신자
     * @param name method / 메서드
     * @param args arguments / 인수
     * @return the result / 결과
     */
    private JsValue m(Object recv, String name, Object... args) {
        return e.callOn(ap, name, recv, args);
    }

    /**
     * A callback that returns true for numbers greater than the limit.
     * <p>
     * 한계보다 큰 숫자에 true 를 돌려주는 콜백.
     *
     * @param limit the limit / 한계
     * @return the function / 함수
     */
    private JsValue greaterThan(final double limit) {
        return e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(Ops.toNumber(args[0]) > limit);
            }
        });
    }

    /**
     * push, pop, shift and unshift on arrays and on array-likes.
     * <p>
     * 배열과 유사 배열에서 push, pop, shift, unshift.
     */
    public void testPushPopShiftUnshift() {
        JsArray a = e.array(1, 2);
        Check.equal(4, Env.i(m(a, "push", 3, 4)), "push returns the new length");
        Check.equal(Arrays.asList("1", "2", "3", "4"), Env.list(a), "after push");
        Check.equal(4, Env.i(m(a, "pop")), "pop returns the last element");
        Check.equal(Arrays.asList("1", "2", "3"), Env.list(a), "after pop");
        Check.equal(1, Env.i(m(a, "shift")), "shift returns the first element");
        Check.equal(Arrays.asList("2", "3"), Env.list(a), "after shift");
        Check.equal(4, Env.i(m(a, "unshift", 0, 1)), "unshift returns the new length");
        Check.equal(Arrays.asList("0", "1", "2", "3"), Env.list(a), "after unshift");
        JsArray empty = e.array();
        Check.isTrue(m(empty, "pop").isUndefined(), "pop of an empty array");
        Check.isTrue(m(empty, "shift").isUndefined(), "shift of an empty array");
        JsObject like = e.arrayLike("x", "y");
        Check.equal(3, Env.i(m(like, "push", "z")), "push on an array-like");
        Check.equal(3, Env.i(like.get("length")), "length of the array-like");
        Check.equal("z", Env.str(like.get("2")), "element of the array-like");
        JsObject noLength = e.realm.newObject();
        Check.equal(1, Env.i(m(noLength, "push", "v")), "push on an object without length");
        Check.equal(1, Env.i(noLength.get("length")), "length was created");
    }

    /**
     * shift and unshift move holes as holes.
     * <p>
     * shift 와 unshift 는 구멍을 구멍인 채로 옮긴다.
     */
    public void testShiftAndUnshiftKeepHoles() {
        JsArray a = e.array("a", Env.HOLE, "c");
        m(a, "shift");
        Check.equal(Arrays.asList("<hole>", "c"), Env.list(a), "shift keeps the hole");
        m(a, "unshift", "x");
        Check.equal(Arrays.asList("x", "<hole>", "c"), Env.list(a), "unshift keeps the hole");
    }

    /**
     * slice and concat, including negative indices, holes, spreadable objects and the spreadable flag.
     * <p>
     * 음수 인덱스, 구멍, 펼칠 수 있는 객체와 펼침 플래그를 포함한 slice 와 concat.
     */
    public void testSliceAndConcat() {
        JsArray a = e.array(1, 2, 3, 4, 5);
        Check.equal(Arrays.asList("2", "3"), Env.list(m(a, "slice", 1, 3)), "slice(1,3)");
        Check.equal(Arrays.asList("4", "5"), Env.list(m(a, "slice", -2)), "slice(-2)");
        Check.equal(Arrays.asList("1", "2", "3", "4"), Env.list(m(a, "slice", 0, -1)), "slice(0,-1)");
        Check.equal(0, Env.list(m(a, "slice", 3, 1)).size(), "empty slice");
        Check.equal(Arrays.asList("a", "<hole>", "c"), Env.list(m(e.array("a", Env.HOLE, "c"), "slice")), "slice keeps holes");
        Check.equal(Arrays.asList("1", "2", "3", "4", "5"), Env.list(m(a, "concat")), "concat() copies");
        Check.equal(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8"), Env.list(m(a, "concat", e.array(6, 7), 8)), "arrays are spread, scalars appended");
        Check.equal(Arrays.asList("1", "<hole>", "9"), Env.list(m(e.array(1, Env.HOLE), "concat", 9)), "concat keeps holes");
        JsArray nested = e.array(1);
        JsArray result = (JsArray) m(nested, "concat", e.array(e.array(2)));
        Check.equal(2, result.getLength(), "only one level is spread");
        JsObject like = e.arrayLike("x", "y");
        Check.equal(1, Env.list(m(e.array(), "concat", like)).size(), "an array-like is not spread by default");
        like.createDataProperty(WellKnownSymbols.IS_CONCAT_SPREADABLE, Js.TRUE);
        Check.equal(Arrays.asList("x", "y"), Env.list(m(e.array(), "concat", like)), "Symbol.isConcatSpreadable makes it spread");
        JsArray noSpread = e.array(7, 8);
        noSpread.createDataProperty(WellKnownSymbols.IS_CONCAT_SPREADABLE, Js.FALSE);
        Check.equal(1, Env.list(m(e.array(), "concat", noSpread)).size(), "an array can opt out of spreading");
        Check.equal(1, Env.list(m(e.arrayLike(1, 2), "concat")).size(), "a non-array receiver is a single element");
    }

    /**
     * splice in all its forms.
     * <p>
     * 여러 형태의 splice.
     */
    public void testSplice() {
        JsArray a = e.array(1, 2, 3, 4, 5);
        Check.equal(Arrays.asList("2", "3"), Env.list(m(a, "splice", 1, 2)), "removed elements");
        Check.equal(Arrays.asList("1", "4", "5"), Env.list(a), "after removal");
        m(a, "splice", 1, 0, "x", "y");
        Check.equal(Arrays.asList("1", "x", "y", "4", "5"), Env.list(a), "insertion");
        m(a, "splice", 1, 2, "z");
        Check.equal(Arrays.asList("1", "z", "4", "5"), Env.list(a), "replace with fewer");
        m(a, "splice", 1, 1, "p", "q", "r");
        Check.equal(Arrays.asList("1", "p", "q", "r", "4", "5"), Env.list(a), "replace with more");
        Check.equal(Arrays.asList("4", "5"), Env.list(m(a, "splice", -2)), "one argument removes to the end");
        Check.equal(0, Env.list(m(a, "splice")).size(), "no argument removes nothing");
        Check.equal(4, Env.list(a).size(), "length after no-op");
        Check.equal(0, Env.list(m(a, "splice", 0, -5)).size(), "negative delete count");
        JsArray holes = e.array("a", Env.HOLE, "c", "d");
        m(holes, "splice", 0, 1);
        Check.equal(Arrays.asList("<hole>", "c", "d"), Env.list(holes), "holes move as holes");
        Check.equal(2, Env.i(((JsObject) e.callOn(ap, "splice", e.array(1, 2, 3), 0, 2)).get("length")), "returns an array of the removed elements");
    }

    /**
     * reverse including the four hole combinations.
     * <p>
     * 구멍 조합 네 가지를 포함한 reverse.
     */
    public void testReverse() {
        Check.equal(Arrays.asList("3", "2", "1"), Env.list(m(e.array(1, 2, 3), "reverse")), "odd length");
        Check.equal(Arrays.asList("4", "3", "2", "1"), Env.list(m(e.array(1, 2, 3, 4), "reverse")), "even length");
        Check.equal(Arrays.asList("c", "<hole>", "a"), Env.list(m(e.array("a", Env.HOLE, "c"), "reverse")), "hole in the middle");
        Check.equal(Arrays.asList("<hole>", "b", "a"), Env.list(m(e.array("a", "b", Env.HOLE), "reverse")), "hole at the end moves to the front");
        Check.equal(Arrays.asList("c", "b", "<hole>"), Env.list(m(e.array(Env.HOLE, "b", "c"), "reverse")), "hole at the start moves to the end");
        JsObject like = e.arrayLike("x", "y", "z");
        Check.same(like, m(like, "reverse"), "reverse returns the receiver");
        Check.equal("z", Env.str(like.get("0")), "array-like reversed");
    }

    /**
     * indexOf, lastIndexOf and includes with positions, strict equality, NaN, holes and negative zero.
     * <p>
     * 위치, 엄격 동등, NaN, 구멍, 음의 0 을 포함한 indexOf, lastIndexOf, includes.
     */
    public void testIndexOfFamily() {
        JsArray a = e.array(1, 2, 3, 2, 1);
        Check.equal(1, Env.i(m(a, "indexOf", 2)), "indexOf");
        Check.equal(3, Env.i(m(a, "indexOf", 2, 2)), "indexOf from index");
        Check.equal(3, Env.i(m(a, "indexOf", 2, -2)), "indexOf from negative index");
        Check.equal(-1, Env.i(m(a, "indexOf", "2")), "strict equality");
        Check.equal(-1, Env.i(m(a, "indexOf", 9)), "not found");
        Check.equal(-1, Env.i(m(a, "indexOf", 1, 99)), "from beyond the end");
        Check.equal(3, Env.i(m(a, "lastIndexOf", 2)), "lastIndexOf");
        Check.equal(1, Env.i(m(a, "lastIndexOf", 2, 2)), "lastIndexOf from index");
        Check.equal(3, Env.i(m(a, "lastIndexOf", 2, -2)), "lastIndexOf from negative index");
        Check.equal(-1, Env.i(m(a, "lastIndexOf", 3, -4)), "lastIndexOf before the start");
        Check.equal(0, Env.i(m(a, "lastIndexOf", 1, 0)), "lastIndexOf at 0");
        Check.equal(-1, Env.i(m(e.array(Double.NaN), "indexOf", Double.NaN)), "indexOf never finds NaN");
        Check.isTrue(Ops.toBoolean(m(e.array(Double.NaN), "includes", Double.NaN)), "includes finds NaN");
        Check.equal(0, Env.i(m(e.array(-0.0), "indexOf", 0)), "-0 equals 0");
        Check.equal(-1, Env.i(m(e.array(Env.HOLE, 1), "indexOf", (Object) null)), "indexOf skips holes");
        Check.isTrue(Ops.toBoolean(m(e.array(Env.HOLE, 1), "includes", (Object) null)), "includes reads holes as undefined");
        Check.isTrue(Ops.toBoolean(m(a, "includes", 3, -3)), "includes from negative index");
        Check.isFalse(Ops.toBoolean(m(a, "includes", 3, 3)), "includes from index after the match");
        Check.equal(2, Env.i(m(e.arrayLike("a", "b", "c"), "indexOf", "c")), "array-like");
    }

    /**
     * join, toString and toLocaleString, including null and undefined elements and cycles.
     * <p>
     * null 과 undefined 요소와 순환을 포함한 join, toString, toLocaleString.
     */
    public void testJoinAndToString() {
        JsArray a = e.array(1, Js.NULL, null, "x", e.array(2, 3));
        Check.equal("1,,,x,2,3", Env.str(m(a, "join")), "default separator");
        Check.equal("1-x", Env.str(m(e.array(1, "x"), "join", "-")), "custom separator");
        Check.equal("1,x", Env.str(m(e.array(1, "x"), "join", (Object) null)), "undefined separator is a comma");
        Check.equal("", Env.str(m(e.array(), "join")), "empty array");
        Check.equal("1,2", Env.str(m(e.array(1, 2), "toString")), "toString calls join");
        Check.equal("a,,c", Env.str(m(e.array("a", Env.HOLE, "c"), "join")), "holes are empty");
        Check.equal("x,y", Env.str(m(e.arrayLike("x", "y"), "join")), "array-like");
        JsArray cyclic = e.array(1, 2);
        cyclic.createDataPropertyOrThrow("2", cyclic);
        Check.equal("1,2,", Env.str(m(cyclic, "join")), "an array that contains itself yields an empty string for the inner reference");
        Check.equal("1,2,", Env.str(m(cyclic, "toString")), "toString of a cyclic array");
        JsObject withJoin = e.realm.newObject();
        withJoin.createDataProperty("join", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str("custom");
            }
        }));
        Check.equal("custom", Env.str(m(withJoin, "toString")), "toString uses the join of the receiver");
        JsObject noJoin = e.realm.newObject();
        Check.equal("[object Object]", Env.str(m(noJoin, "toString")), "falls back to the Object.prototype.toString behavior");
        JsObject withLocale = e.realm.newObject();
        withLocale.createDataProperty("toLocaleString", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str("L");
            }
        }));
        Check.equal("L,,L", Env.str(m(e.array(withLocale, Js.NULL, withLocale), "toLocaleString")), "toLocaleString calls the element methods");
    }

    /**
     * fill and copyWithin.
     * <p>
     * fill 과 copyWithin 시험.
     */
    public void testFillAndCopyWithin() {
        Check.equal(Arrays.asList("0", "0", "0"), Env.list(m(e.array(1, 2, 3), "fill", 0)), "fill all");
        Check.equal(Arrays.asList("1", "9", "9"), Env.list(m(e.array(1, 2, 3), "fill", 9, 1)), "fill from index");
        Check.equal(Arrays.asList("1", "9", "3"), Env.list(m(e.array(1, 2, 3), "fill", 9, 1, 2)), "fill range");
        Check.equal(Arrays.asList("1", "2", "9"), Env.list(m(e.array(1, 2, 3), "fill", 9, -1)), "fill negative start");
        Check.equal(Arrays.asList("4", "5", "3", "4", "5"), Env.list(m(e.array(1, 2, 3, 4, 5), "copyWithin", 0, 3)), "copyWithin(0,3)");
        Check.equal(Arrays.asList("1", "2", "3", "1", "2"), Env.list(m(e.array(1, 2, 3, 4, 5), "copyWithin", 3, 0)), "overlapping copy to the right");
        Check.equal(Arrays.asList("1", "2", "3", "4", "5"), Env.list(m(e.array(1, 2, 3, 4, 5), "copyWithin", 1, 1)), "no-op");
        Check.equal(Arrays.asList("4", "2", "3", "4", "5"), Env.list(m(e.array(1, 2, 3, 4, 5), "copyWithin", -5, -2, -1)), "negative indices");
        Check.equal(Arrays.asList("<hole>", "<hole>", "3"), Env.list(m(e.array(Env.HOLE, 2, 3), "copyWithin", 1, 0, 1)), "copying a hole deletes the target");
        Check.equal(Arrays.asList("<hole>", "<hole>", "<hole>"), Env.list(m(e.array(1, Env.HOLE, Env.HOLE), "copyWithin", 0, 1)), "holes are copied as holes");
    }

    /**
     * Callback methods call the callback only for present elements, with (value, index, array) and thisArg.
     * <p>
     * 콜백 메서드는 있는 요소에만 (값, 인덱스, 배열) 과 thisArg 로 콜백을 호출한다.
     */
    public void testCallbackMethods() {
        final List<String> log = new ArrayList<String>();
        final JsObject thisArg = e.realm.newObject();
        final JsArray a = e.array(10, Env.HOLE, 30);
        JsValue cb = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                log.add(Env.describe(args[0]) + "@" + Env.describe(args[1]) + (args[2] == a ? "+" : "-") + (thisValue == thisArg ? "T" : "o"));
                return args[0];
            }
        });
        Check.isTrue(m(a, "forEach", cb, thisArg).isUndefined(), "forEach returns undefined");
        Check.equal(Arrays.asList("10@0+T", "30@2+T"), log, "holes are skipped, arguments and thisArg");
        Check.equal(Arrays.asList("10", "<hole>", "30"), Env.list(m(a, "map", cb, thisArg)), "map keeps holes");
        Check.equal(Arrays.asList("10", "30"), Env.list(m(a, "filter", cb, thisArg)), "filter skips holes");
        Check.equal(Arrays.asList("2", "3"), Env.list(m(e.array(1, 2, 3), "filter", greaterThan(1))), "filter");
        Check.isTrue(Ops.toBoolean(m(e.array(2, 3), "every", greaterThan(1))), "every");
        Check.isFalse(Ops.toBoolean(m(e.array(1, 3), "every", greaterThan(1))), "every false");
        Check.isTrue(Ops.toBoolean(m(e.array(), "every", greaterThan(1))), "every of an empty array");
        Check.isTrue(Ops.toBoolean(m(e.array(1, 3), "some", greaterThan(1))), "some");
        Check.isFalse(Ops.toBoolean(m(e.array(), "some", greaterThan(1))), "some of an empty array");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(a, "forEach", "not a function");
            }
        }), "callback must be callable");
        final JsArray growing = e.array(1, 2);
        final int[] calls = new int[1];
        m(growing, "forEach", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls[0]++;
                growing.setStrict("2", Js.num(99));
                return Js.UNDEFINED;
            }
        }));
        Check.equal(2, calls[0], "elements added during the iteration are not visited");
    }

    /**
     * find, findIndex visit holes as undefined.
     * <p>
     * find, findIndex 는 구멍을 undefined 로 방문한다.
     */
    public void testFind() {
        Check.equal(3, Env.i(m(e.array(1, 2, 3, 4), "find", greaterThan(2))), "find");
        Check.equal(2, Env.i(m(e.array(1, 2, 3, 4), "findIndex", greaterThan(2))), "findIndex");
        Check.isTrue(m(e.array(1), "find", greaterThan(5)).isUndefined(), "find without match");
        Check.equal(-1, Env.i(m(e.array(1), "findIndex", greaterThan(5))), "findIndex without match");
        final List<String> seen = new ArrayList<String>();
        m(e.array(1, Env.HOLE), "find", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                seen.add(Env.describe(args[0]));
                return Js.FALSE;
            }
        }));
        Check.equal(Arrays.asList("1", "undefined"), seen, "holes are visited");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(e.array(1), "find", Js.UNDEFINED);
            }
        }), "predicate must be callable");
    }

    /**
     * Visits indices from the end, observes holes, and stops at the first match.
     * <p>
     * 끝에서부터 인덱스를 순회해 빈 칸도 읽고 처음 일치한 요소에서 멈춘다.
     */
    public void testFindLastAndIndex() {
        String source = "var log=[];var receiver={0:'a',2:'c',length:3};var self={};"
                + "var index=Array.prototype.findLastIndex.call(receiver,function(x,i,o){"
                + " log.push([i,String(x),this===self,o===receiver].join(':'));return x===undefined;},self);"
                + "[index,log.join('/'),[2,3,4].findLast(x=>x%2===0),[].findLastIndex(()=>true)].join('|');";
        Check.equal("1|2:c:true:true/1:undefined:true:true|4|-1",
                Env.str(JsRealm.create().evaluateScript(source, "array-find-last.js")), "descending read, callback and failure value");
    }

    /**
     * Copies densely, ignores species, and reads only the indices needed by each operation.
     * <p>
     * 밀집 배열로 복사하고 species를 조회하지 않으며 연산에 필요한 인덱스만 읽는다.
     */
    public void testArrayCopyMethods() {
        String source = "var proto=Array.prototype;"
                + "var a=[3,,1,undefined];Object.defineProperty(a,'constructor',{get(){throw 'species';}});"
                + "var reversed=a.toReversed();var sorted=a.toSorted((x,y)=>x-y);"
                + "var copied=a.toSpliced();var inserted=a.toSpliced(1,undefined,2);"
                + "var log=[];var target={0:'x',2:'z',length:3};"
                + "var proxy=new Proxy(target,{has(){throw 'has';},get(t,k){log.push(k);return t[k];}});"
                + "var rev=proto.toReversed.call(proxy);var reverseOrder=log.join(',');log.length=0;"
                + "var modified=proto.with.call(proxy,-2,'y');var replaceOrder=log.join(',');log.length=0;"
                + "var spliced=proto.toSpliced.call(proxy,1,1,'y');var spliceOrder=log.join(',');log.length=0;"
                + "var ordered=proto.toSorted.call(proxy);var sortOrder=log.join(',');"
                + "[reversed.join('/'),sorted.join('/'),Object.keys(reversed).length,Object.keys(sorted).length,"
                + " Object.keys(copied).length,inserted.join('/'),a.toSpliced(1).join('/'),"
                + " rev.join('/'),reverseOrder,modified.join('/'),replaceOrder,spliced.join('/'),spliceOrder,"
                + " ordered.join('/'),sortOrder].join('|');";
        Check.equal("/1//3|1/3//|4|4|4|3/2//1/|3|z//x|length,2,1,0|x/y/z|length,0,2|x/y/z|length,0,2|x/z/|length,0,1,2",
                Env.str(JsRealm.create().evaluateScript(source, "array-copy.js")), "dense outputs and observable access order");
    }

    /**
     * Checks boundary errors before indexed reads.
     * <p>
     * 인덱스 접근 전의 범위 오류를 확인한다.
     */
    public void testCopyMethodErrors() {
        String source = "var proto=Array.prototype,log=[];"
                + "var source=new Proxy({length:1},{get(t,k){if(k==='length')return t.length;throw 'index';}});"
                + "try{proto.with.call(source,Infinity,0);}catch(e){log.push(e.name);}"
                + "try{proto.toSorted.call(source,0);}catch(e){log.push(e.name);}"
                + "try{proto.toReversed.call({length:4294967296});}catch(e){log.push(e.name);}"
                + "try{proto.toSpliced.call({length:Number.MAX_SAFE_INTEGER},0,0,1);}catch(e){log.push(e.name);}"
                + "log.join('|');";
        Check.equal("RangeError|TypeError|RangeError|TypeError",
                Env.str(JsRealm.create().evaluateScript(source, "array-copy-errors.js")), "range and type");
    }

    /**
     * reduce and reduceRight with and without an initial value, holes and errors.
     * <p>
     * 초기값 유무, 구멍, 오류를 포함한 reduce 와 reduceRight.
     */
    public void testReduce() {
        JsValue concat = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(Env.describe(args[0]) + Env.describe(args[1]));
            }
        });
        Check.equal("abc", Env.str(m(e.array("a", "b", "c"), "reduce", concat)), "reduce without initial value");
        Check.equal("xabc", Env.str(m(e.array("a", "b", "c"), "reduce", concat, "x")), "reduce with initial value");
        Check.equal("cba", Env.str(m(e.array("a", "b", "c"), "reduceRight", concat)), "reduceRight");
        Check.equal("xcba", Env.str(m(e.array("a", "b", "c"), "reduceRight", concat, "x")), "reduceRight with initial value");
        Check.equal("ac", Env.str(m(e.array("a", Env.HOLE, "c"), "reduce", concat)), "reduce skips holes");
        Check.equal("b", Env.str(m(e.array("b"), "reduce", concat)), "a single element is returned without a call");
        Check.equal("x", Env.str(m(e.array(), "reduce", concat, "x")), "empty array with initial value");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(e.array(), "reduce", e.fn(new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue t, JsValue[] a) {
                        return Js.UNDEFINED;
                    }
                }));
            }
        }), "empty array without initial value");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(e.array(Env.HOLE, Env.HOLE), "reduceRight", e.fn(new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue t, JsValue[] a) {
                        return Js.UNDEFINED;
                    }
                }));
            }
        }), "only holes without initial value");
    }

    /**
     * sort: default string order, numeric comparison functions, undefined and holes, stability.
     * <p>
     * sort: 기본 문자열 순서, 숫자 비교 함수, undefined 와 구멍, 안정성.
     */
    public void testSort() {
        Check.equal(Arrays.asList("1", "10", "2", "9"), Env.list(m(e.array(10, 9, 2, 1), "sort")), "default order is by string");
        JsValue numeric = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(Ops.toNumber(args[0]) - Ops.toNumber(args[1]));
            }
        });
        Check.equal(Arrays.asList("1", "2", "9", "10"), Env.list(m(e.array(10, 9, 2, 1), "sort", numeric)), "numeric comparison");
        Check.equal(Arrays.asList("a", "b", "undefined", "<hole>"), Env.list(m(e.array("b", null, "a", Env.HOLE), "sort")), "undefined last, holes after");
        JsArray strings = e.array("b", "a", "가", "나", "B", "A");
        Check.equal(Arrays.asList("A", "B", "a", "b", "가", "나"), Env.list(m(strings, "sort")), "code unit order");
        JsArray pairs = e.array();
        // 키가 같은 쌍의 입력 순서를 기억하는 객체를 만든다.
        String[] keys = { "b", "a", "b", "a", "b", "a", "c", "a" };
        for (int i = 0; i < keys.length; i++) {
            JsObject o = e.realm.newObject();
            o.createDataProperty("k", Js.str(keys[i]));
            o.createDataProperty("i", Js.num(i));
            m(pairs, "push", o);
        }
        JsValue byKey = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(Ops.toString(((JsObject) args[0]).get("k")).compareTo(Ops.toString(((JsObject) args[1]).get("k"))));
            }
        });
        m(pairs, "sort", byKey);
        StringBuilder order = new StringBuilder();
        // 정렬 뒤 각 요소의 키와 원래 위치를 읽는다.
        for (int i = 0; i < keys.length; i++) {
            JsObject o = (JsObject) pairs.get(String.valueOf(i));
            order.append(Env.str(o.get("k"))).append(Env.str(o.get("i")));
        }
        Check.equal("a1a3a5a7b0b2b4c6", order.toString(), "equal keys keep their input order (stable)");
        Check.same(strings, m(strings, "sort"), "sort returns the receiver");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(e.array(2, 1), "sort", "not a function");
            }
        }), "comparison function must be a function or undefined");
        Check.equal(Arrays.asList("1", "2"), Env.list(m(e.array(2, 1), "sort", Js.UNDEFINED)), "undefined comparison function");
        JsObject like = e.arrayLike("b", "a");
        m(like, "sort");
        Check.equal("a", Env.str(like.get("0")), "array-like is sorted in place");
    }

    /**
     * An exception from the comparison function propagates and leaves the array unchanged; a NaN result counts as equal.
     * <p>
     * 비교 함수의 예외는 전파되며 배열은 바뀌지 않는다. NaN 결과는 같음으로 본다.
     */
    public void testSortComparatorFailureAndNaN() {
        final JsArray a = e.array(3, 1, 2);
        final JsValue failing = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                throw JsException.typeError("compare failed");
            }
        });
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(a, "sort", failing);
            }
        }), "the exception propagates");
        Check.equal(Arrays.asList("3", "1", "2"), Env.list(a), "the array is untouched");
        JsValue nan = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(Double.NaN);
            }
        });
        Check.equal(Arrays.asList("3", "1", "2"), Env.list(m(e.array(3, 1, 2), "sort", nan)), "NaN results keep the order");
        JsValue inconsistent = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(-1);
            }
        });
        Check.equal(3, Env.list(m(e.array(3, 1, 2), "sort", inconsistent)).size(), "an inconsistent comparison function still terminates");
    }

    /**
     * Sorting a larger array gives a correct order.
     * <p>
     * 큰 배열도 올바른 순서로 정렬한다.
     */
    public void testSortLargeArray() {
        final int n = 5000;
        JsArray a = e.array();
        // 의사 난수 열을 만들어 넣는다.
        long seed = 12345;
        for (int i = 0; i < n; i++) {
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            m(a, "push", (double) (seed % 1000));
        }
        JsValue numeric = e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.num(Ops.toNumber(args[0]) - Ops.toNumber(args[1]));
            }
        });
        m(a, "sort", numeric);
        // 이웃한 요소가 오름차순인지 확인한다.
        for (int i = 1; i < n; i++) {
            Check.isTrue(Ops.toNumber(a.get(String.valueOf(i - 1))) <= Ops.toNumber(a.get(String.valueOf(i))), "ascending at " + i);
        }
    }

    /**
     * entries, keys, values and the array iterator.
     * <p>
     * entries, keys, values 와 배열 이터레이터.
     */
    public void testIterators() {
        JsArray a = e.array("x", "y");
        Check.equal(Arrays.asList("0", "1"), Env.list(e.realm.newArray(IteratorHelper.toList(e.realm, m(a, "keys")))), "keys");
        Check.equal(Arrays.asList("x", "y"), Env.list(e.realm.newArray(IteratorHelper.toList(e.realm, m(a, "values")))), "values");
        List<JsValue> entries = IteratorHelper.toList(e.realm, m(a, "entries"));
        Check.equal(2, entries.size(), "entries count");
        Check.equal(Arrays.asList("1", "y"), Env.list(entries.get(1)), "an entry is [index, value]");
        Check.same(ap.get("values"), ap.get(WellKnownSymbols.ITERATOR), "[Symbol.iterator] is values");
        Check.same(ap.get("values"), e.realm.getIntrinsic("%ArrayProto_values%"), "intrinsic");
        JsObject it = (JsObject) m(a, "values");
        Check.same(e.realm.getIntrinsic("%ArrayIteratorPrototype%"), it.getPrototypeOf(), "prototype of the iterator");
        Check.same(e.realm.getIteratorPrototype(), e.realm.getIntrinsic("%ArrayIteratorPrototype%").getPrototypeOf(), "inherits %IteratorPrototype%");
        Check.equal("Array Iterator", Env.str(e.realm.getIntrinsic("%ArrayIteratorPrototype%").get(WellKnownSymbols.TO_STRING_TAG)), "@@toStringTag");
        JsObject r1 = (JsObject) e.call(it, "next");
        Check.equal("x", Env.str(r1.get("value")), "first value");
        e.call(it, "next");
        JsObject end = (JsObject) e.call(it, "next");
        Check.isTrue(Ops.toBoolean(end.get("done")), "finished");
        a.setStrict("2", Js.str("z"));
        Check.isTrue(Ops.toBoolean(((JsObject) e.call(it, "next")).get("done")), "a finished iterator stays finished");
        JsObject live = (JsObject) m(a, "values");
        e.call(live, "next");
        e.call(live, "next");
        Check.equal("z", Env.str(((JsObject) e.call(live, "next")).get("value")), "a running iterator sees elements added later");
        Check.equal(2, IteratorHelper.toList(e.realm, m(e.arrayLike("p", "q"), "values")).size(), "iterator over an array-like");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                Ops.call(e.realm.getIntrinsic("%ArrayIteratorPrototype%").get("next"), e.realm.newObject(), Js.NO_ARGS);
            }
        }), "next on a foreign object");
    }

    /**
     * Array.prototype[@@unscopables] has the names in the ES2026 table.
     * <p>
     * Array.prototype[@@unscopables]는 ES2026 표의 이름을 가진다.
     */
    public void testUnscopables() {
        JsObject u = (JsObject) ap.get(WellKnownSymbols.UNSCOPABLES);
        Check.isNull(u.getPrototypeOf(), "null prototype");
        List<String> names = Arrays.asList("at", "copyWithin", "entries", "fill", "find", "findIndex", "findLast", "findLastIndex",
                "flat", "flatMap", "includes", "keys", "toReversed", "toSorted", "toSpliced", "values");
        Check.equal(names, u.ownKeys(), "names and definition order");
        // 목록의 모든 프로퍼티 값이 true인지 검사한다.
        for (String name : names) {
            Check.equal(Js.TRUE, u.get(name), name + " is unscopable");
        }
        Check.equal("outer", Env.str(JsRealm.create().evaluateScript("var toSorted='outer';with([]){toSorted;}", "unscopables.js")),
                "with resolves an outer binding");
    }

    /**
     * Array.prototype methods work on strings, numbers and other values through ToObject; null and undefined are rejected.
     * <p>
     * Array.prototype 메서드는 ToObject 를 거쳐 문자열, 숫자 등에서도 동작하며 null 과 undefined 는 거부한다.
     */
    public void testGenericReceivers() {
        Check.equal("a-b-c", Env.str(m("abc", "join", "-").toString().length() == 0 ? Js.str("") : Js.str(Env.str(m("abc", "join", "-")).replace("a-b-c", "a-b-c"))), "join of a string receiver");
        Check.equal(Arrays.asList("b", "c"), Env.list(m("abc", "slice", 1)), "slice of a string receiver");
        Check.equal(3, Env.list(m("abc", "map", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return args[0];
            }
        }))).size(), "map of a string receiver");
        String[] names = { "push", "pop", "shift", "unshift", "slice", "splice", "concat", "join", "reverse", "sort", "indexOf", "forEach", "map",
            "filter", "reduce", "find", "fill", "keys", "values", "entries", "includes", "toString" };
        // 모든 메서드는 undefined 와 null 수신자를 TypeError 로 거부한다.
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    m(Js.UNDEFINED, name);
                }
            }), name + " on undefined");
            Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
                public void run() {
                    m(Js.NULL, name);
                }
            }), name + " on null");
        }
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m("abc", "push", "d");
            }
        }), "push on a string fails because the string indices and length are read-only");
    }

    /**
     * Limits: growing beyond 2^53-1 is a TypeError and long loops can be interrupted.
     * <p>
     * 한계: 2^53-1 을 넘겨 늘리면 TypeError 이고 오래 걸리는 반복은 인터럽트할 수 있다.
     */
    public void testLimitsAndInterruption() {
        final JsObject big = e.arrayLike();
        big.createDataProperty("length", Js.num(9007199254740991.0));
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(big, "push", 1);
            }
        }), "push beyond 2^53-1");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(big, "unshift", 1);
            }
        }), "unshift beyond 2^53-1");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                m(big, "splice", 0, 0, 1);
            }
        }), "splice beyond 2^53-1");
        Check.equal(ErrorKind.TYPE_ERROR, Env.errorOf(new Block() {
            public void run() {
                big.createDataProperty(WellKnownSymbols.IS_CONCAT_SPREADABLE, Js.TRUE);
                m(e.array(1), "concat", big);
            }
        }), "concat beyond 2^53-1");
        final JsObject huge = e.arrayLike();
        huge.createDataProperty("length", Js.num(1e15));
        Thread.currentThread().interrupt();
        try {
            m(huge, "indexOf", 1);
            Check.fail("a loop over 10^15 indices must be interrupted");
        } catch (ScriptAbortedException ex) {
            Check.equal(ScriptAbortedException.Reason.CANCELLED, ex.getReason(), "reason");
        } finally {
            Thread.interrupted();
        }
        JsObject bigArray = Ops.construct(e.global("Array"), new JsValue[] { Js.num(4294967295.0) });
        Check.equal(4294967295L, Env.i(bigArray.get("length")), "an array can have the maximum length without allocating it");
        Check.equal(ErrorKind.RANGE_ERROR, Env.errorOf(new Block() {
            public void run() {
                Ops.call(e.global("Array").get("of"), Js.UNDEFINED, new JsValue[0]);
                Ops.construct(e.global("Array"), new JsValue[] { Js.num(4294967296.0) });
            }
        }), "a longer array");
    }

    /**
     * Sparse arrays with a large length and few elements work for operations that do not need to visit every index.
     * <p>
     * 길이는 크지만 요소가 적은 희소 배열은 모든 인덱스를 방문할 필요가 없는 연산에서 동작한다.
     */
    public void testSparseArray() {
        JsArray sparse = e.array();
        sparse.defineOwnProperty("length", org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor.valueOnly(Js.num(1000000)));
        sparse.createDataPropertyOrThrow("999999", Js.str("last"));
        Check.equal(999999, Env.i(m(sparse, "indexOf", "last")), "indexOf in a sparse array (1,000,000 steps)");
        Check.equal("last", Env.str(m(sparse, "pop")), "pop");
        Check.equal(999999, Env.i(sparse.get("length")), "length after pop");
    }

    /**
     * toString and sort on array-likes with accessors behave generically (getters are called).
     * <p>
     * 접근자가 있는 유사 배열에서도 toString 과 sort 는 일반적으로 동작한다(getter 를 호출한다).
     */
    public void testAccessorsAreHonored() {
        final int[] gets = new int[1];
        JsObject o = e.arrayLike("b", "a");
        o.defineAccessor("1", e.fn(new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                gets[0]++;
                return Js.str("a");
            }
        }), null, true, true);
        Check.equal("b,a", Env.str(m(o, "join")), "join calls the getter");
        Check.isTrue(gets[0] >= 1, "getter was called");
    }
}
