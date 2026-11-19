package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.ScriptAbortedException;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpCancel;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the RegExp built-in (plan item P14-03): construction, source escaping, exec and the lastIndex protocol, the flag accessors,
 * the symbol methods match, replace, search and split, Annex B compile, resource limits and cancellation.
 * <p>
 * RegExp 내장 객체 시험(계획 P14-03): 생성, source 이스케이프, exec 와 lastIndex 규약, 플래그 접근자, 심볼 메서드 match/replace/search/split,
 * 부속서 B compile, 자원 제한과 취소.
 */
public class RegExpTest {

    /**
     * Environment of the current case.
     * <p>
     * 현재 사례의 환경.
     */
    private Env env;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RegExpTest() {
    }

    /**
     * Creates a fresh realm before each case.
     * <p>
     * 사례마다 새 realm 을 만든다.
     */
    public void setUp() {
        env = new Env();
    }

    /**
     * Creates a regular expression.
     * <p>
     * 정규식을 만든다.
     *
     * @param source pattern / 패턴
     * @param flags  flags / 플래그
     * @return the object / 객체
     */
    private JsObject re(String source, String flags) {
        return env.construct("RegExp", Env.s(source), Env.s(flags));
    }

    /**
     * Runs exec and returns the result object or null.
     * <p>
     * exec 를 실행하여 결과 객체 또는 null 을 돌려준다.
     *
     * @param re the regular expression / 정규식
     * @param s  the input / 입력
     * @return result array or null / 결과 배열 또는 null
     */
    private JsObject exec(JsObject re, String s) {
        JsValue v = env.invoke(re, "exec", Env.s(s));
        return v.isNull() ? null : (JsObject) v;
    }

    /**
     * Calls a symbol method of a regular expression with a string and optional extra argument.
     * <p>
     * 정규식의 심볼 메서드를 문자열과 선택 인수로 호출한다.
     *
     * @param re     the regular expression / 정규식
     * @param symbol the symbol key / 심볼 키
     * @param s      the input / 입력
     * @param extra  extra arguments / 추가 인수
     * @return the result / 결과
     */
    private JsValue sym(JsObject re, Object symbol, String s, JsValue... extra) {
        JsValue[] args = new JsValue[extra.length + 1];
        args[0] = Env.s(s);
        System.arraycopy(extra, 0, args, 1, extra.length);
        return env.invokeSymbol(re, symbol, args);
    }

    /**
     * Construction with pattern and flags, the own lastIndex property and toString.
     * <p>
     * 패턴과 플래그로 만들기, 자체 lastIndex 프로퍼티, toString.
     */
    public void testConstruction() {
        JsObject r = re("a+b", "gi");
        Check.equal("a+b", Env.str(r.get("source")), "source");
        Check.equal("gi", Env.str(r.get("flags")), "flags");
        Check.same(Js.TRUE, r.get("global"), "global");
        Check.same(Js.TRUE, r.get("ignoreCase"), "ignoreCase");
        Check.same(Js.FALSE, r.get("multiline"), "multiline");
        Check.same(Js.FALSE, r.get("unicode"), "unicode");
        Check.same(Js.FALSE, r.get("sticky"), "sticky");
        Check.equal("/a+b/gi", Env.str(env.invoke(r, "toString")), "toString");
        PropertyDescriptor li = r.getOwnProperty("lastIndex");
        Check.equal(0.0, Env.d(li.value), "lastIndex starts at 0");
        Check.isTrue(li.isWritableTrue() && !li.isEnumerableTrue() && !li.isConfigurableTrue(), "lastIndex attributes: writable only");
        Check.equal("(?:)", Env.str(env.construct("RegExp").get("source")), "empty pattern");
        Check.equal("(?:)", Env.str(env.construct("RegExp", Js.UNDEFINED, Env.s("g")).get("source")), "undefined pattern is empty");
        Check.equal("gimy", Env.str(re("a", "ygim").get("flags")), "flags getter order is g i m y");
        Check.same(env.get("RegExp.prototype"), r.getPrototypeOf(), "prototype");
    }

    /**
     * RegExp called as a function and copying of regular expressions (IsRegExp, same constructor shortcut).
     * <p>
     * 함수로 호출한 RegExp 와 정규식 복사(IsRegExp, 같은 생성자 단축 경로).
     */
    public void testCallAndCopy() {
        JsObject r = re("x", "g");
        JsValue same = Ops.call(env.get("RegExp"), Js.UNDEFINED, new JsValue[] { r });
        Check.same(r, same, "RegExp(re) returns re itself when the constructor matches and flags are undefined");
        JsObject copy = env.construct("RegExp", r);
        Check.isTrue(copy != r, "new RegExp(re) creates a new object");
        Check.equal("x", Env.str(copy.get("source")), "copy source");
        Check.equal("g", Env.str(copy.get("flags")), "copy flags");
        JsObject other = env.construct("RegExp", r, Env.s("i"));
        Check.equal("i", Env.str(other.get("flags")), "new flags replace the old ones");
        JsValue notSame = Ops.call(env.get("RegExp"), Js.UNDEFINED, new JsValue[] { r, Env.s("m") });
        Check.isTrue(notSame != r, "RegExp(re, flags) creates a new object");
        JsObject like = env.realm.newObject();
        like.createDataProperty(WellKnownSymbols.MATCH, Js.TRUE);
        like.createDataProperty("source", Env.s("q+"));
        like.createDataProperty("flags", Env.s("g"));
        JsObject fromLike = env.construct("RegExp", like);
        Check.equal("q+", Env.str(fromLike.get("source")), "IsRegExp objects supply source");
        Check.equal("g", Env.str(fromLike.get("flags")), "IsRegExp objects supply flags");
        JsObject plain = env.construct("RegExp", Env.n(123));
        Check.equal("123", Env.str(plain.get("source")), "the pattern is converted with ToString");
    }

    /**
     * Invalid patterns and flags raise SyntaxError.
     * <p>
     * 잘못된 패턴과 플래그는 SyntaxError 를 일으킨다.
     */
    public void testSyntaxErrors() {
        JsException e = Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Compiles an invalid pattern.
             * <p>
             * 잘못된 패턴을 컴파일한다.
             */
            public void run() {
                re("(", "");
            }
        }, "unterminated group");
        Check.contains(e.getErrorMessage(), "Invalid regular expression", "message names the problem");
        Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Uses an unknown flag.
             * <p>
             * 알 수 없는 플래그를 쓴다.
             */
            public void run() {
                re("a", "x");
            }
        }, "unknown flag");
        Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Repeats a flag.
             * <p>
             * 플래그를 반복한다.
             */
            public void run() {
                re("a", "gg");
            }
        }, "repeated flag");
        Env.expect(ErrorKind.SYNTAX_ERROR, new Block() {
            /**
             * Uses a quantifier without operand.
             * <p>
             * 대상이 없는 수량자를 쓴다.
             */
            public void run() {
                re("*a", "");
            }
        }, "nothing to repeat");
    }

    /**
     * The source property escapes slashes and line terminators the way a regular expression literal needs.
     * <p>
     * source 프로퍼티는 정규식 리터럴이 필요로 하는 대로 슬래시와 줄 끝 문자를 이스케이프한다.
     */
    public void testSourceEscaping() {
        Check.equal("a\\/b", Env.str(re("a/b", "").get("source")), "slash is escaped");
        Check.equal("a\\/b", Env.str(re("a\\/b", "").get("source")), "an already escaped slash stays");
        Check.equal("[/]", Env.str(re("[/]", "").get("source")), "slash inside a class stays");
        Check.equal("a\\nb", Env.str(re("a\nb", "").get("source")), "a line feed is written as \\n");
        Check.equal("\\u2028", Env.str(re("" + (char) 0x2028, "").get("source")), "line separator is escaped");
        Check.equal("/a\\/b/", Env.str(env.invoke(re("a/b", ""), "toString")), "toString uses the escaped source");
    }

    /**
     * exec returns captures, index and input; unmatched groups are undefined.
     * <p>
     * exec 는 캡처, index, input 을 돌려주고 일치하지 않은 그룹은 undefined 이다.
     */
    public void testExecResult() {
        JsObject r = re("(a)|(b)", "");
        JsObject m = exec(r, "xxb");
        Check.notNull(m, "match");
        Check.equal(Arrays.asList("b", "undefined", "b"), Env.list(m), "captures, undefined for the unmatched group");
        Check.equal(2.0, Env.d(m.get("index")), "index");
        Check.equal("xxb", Env.str(m.get("input")), "input");
        Check.equal(3.0, Env.d(m.get("length")), "length");
        Check.isNull(exec(re("z", ""), "abc"), "no match gives null");
        Check.equal(Arrays.asList(""), Env.list(exec(re("", ""), "abc")), "empty pattern matches the empty string");
        Check.equal("abc", Env.str(exec(re("a.c", "s"), "a\nc") == null ? Env.s("") : Env.s("abc")), "dotAll (s) lets . match a line feed");
        Check.notNull(exec(re("a.c", "s"), "a\nc"), "dotAll match");
        Check.isNull(exec(re("a.c", ""), "a\nc"), "without s the dot does not match a line feed");
    }

    /**
     * The lastIndex protocol: global advances it, a failure resets it, non-global ignores it, sticky anchors at it.
     * <p>
     * lastIndex 규약: 전역은 전진시키고 실패하면 되돌리며, 비전역은 무시하고, 고정은 그 위치에 묶는다.
     */
    public void testLastIndexProtocol() {
        JsObject g = re("a", "g");
        Check.equal(0.0, Env.d(exec(g, "aXa").get("index")), "first match");
        Check.equal(1.0, Env.d(g.get("lastIndex")), "lastIndex after the first match");
        Check.equal(2.0, Env.d(exec(g, "aXa").get("index")), "second match");
        Check.equal(3.0, Env.d(g.get("lastIndex")), "lastIndex after the second match");
        Check.isNull(exec(g, "aXa"), "third attempt fails");
        Check.equal(0.0, Env.d(g.get("lastIndex")), "a failure resets lastIndex");
        JsObject plain = re("a", "");
        g.setStrict("lastIndex", Env.n(5));
        plain.setStrict("lastIndex", Env.n(2));
        Check.equal(0.0, Env.d(exec(plain, "aXa").get("index")), "non-global ignores lastIndex");
        Check.equal(2.0, Env.d(plain.get("lastIndex")), "non-global leaves lastIndex alone on success");
        JsObject y = re("a", "y");
        Check.isNull(exec(y, "ba"), "sticky fails when the match is not at lastIndex");
        y.setStrict("lastIndex", Env.n(1));
        Check.notNull(exec(y, "ba"), "sticky matches at lastIndex");
        Check.equal(2.0, Env.d(y.get("lastIndex")), "sticky advances lastIndex");
        g.setStrict("lastIndex", Env.n(99));
        Check.isNull(exec(g, "aaa"), "lastIndex beyond the length fails");
        Check.equal(0.0, Env.d(g.get("lastIndex")), "and resets");
        JsObject frozen = re("a", "g");
        frozen.defineOwnProperty("lastIndex", PropertyDescriptor.data(Env.n(0), false, false, false));
        final JsObject target = frozen;
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Runs a successful global match on a regular expression whose lastIndex is read-only.
             * <p>
             * lastIndex 가 읽기 전용인 정규식에서 전역 일치를 실행한다.
             */
            public void run() {
                exec(target, "a");
            }
        }, "writing a read-only lastIndex");
        JsObject u = re("(?:)", "gu");
        u.setStrict("lastIndex", Env.n(0));
        exec(u, "😀");
        Check.equal(0.0, Env.d(u.get("lastIndex")), "an empty match does not advance by itself");
    }

    /**
     * The flag accessors on RegExp.prototype and the generic flags getter.
     * <p>
     * RegExp.prototype 의 플래그 접근자와 범용 flags getter.
     */
    public void testFlagAccessors() {
        final JsObject proto = env.obj("RegExp.prototype");
        Check.same(Js.UNDEFINED, proto.get("global"), "RegExp.prototype.global is undefined");
        Check.same(Js.UNDEFINED, proto.get("sticky"), "RegExp.prototype.sticky is undefined");
        Check.equal("(?:)", Env.str(proto.get("source")), "RegExp.prototype.source");
        Check.equal("", Env.str(proto.get("flags")), "flags of the prototype is empty");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Reads the global accessor of a plain object.
             * <p>
             * 일반 객체에서 global 접근자를 읽는다.
             */
            public void run() {
                JsObject plain = env.realm.newObject();
                Ops.call(proto.getOwnProperty("global").get, plain, Js.NO_ARGS);
            }
        }, "global getter on a plain object");
        JsObject fake = env.realm.newObject();
        fake.createDataProperty("global", Js.TRUE);
        fake.createDataProperty("sticky", Js.TRUE);
        fake.createDataProperty("unicode", Js.TRUE);
        Check.equal("guy", Env.str(((NativeFunction) proto.getOwnProperty("flags").get).call(fake, Js.NO_ARGS)), "flags getter is generic and ordered");
        Check.equal("gmsuy", Env.str(re("a", "ysmug").get("flags")), "all flags in order");
        Check.isTrue(proto.getOwnProperty("global").isAccessor(), "global is an accessor property");
        Check.isFalse(proto.getOwnProperty("global").isEnumerableTrue(), "not enumerable");
        Check.isTrue(proto.getOwnProperty("global").isConfigurableTrue(), "configurable");
    }

    /**
     * test() and a user supplied exec method.
     * <p>
     * test() 와 사용자가 지정한 exec 메서드.
     */
    public void testTestAndCustomExec() {
        Check.same(Js.TRUE, env.invoke(re("b", ""), "test", Env.s("abc")), "test true");
        Check.same(Js.FALSE, env.invoke(re("z", ""), "test", Env.s("abc")), "test false");
        JsObject g = re("a", "g");
        env.invoke(g, "test", Env.s("aa"));
        Check.equal(1.0, Env.d(g.get("lastIndex")), "test advances lastIndex of a global regular expression");
        JsObject custom = re("zzz", "");
        final int[] calls = new int[1];
        env.realm.method(custom, "exec", 1, new NativeFunction.Call() {
            /**
             * A replacement exec that always reports a match.
             * <p>
             * 항상 일치를 알리는 대체 exec.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return an object / 객체
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                calls[0]++;
                return r.newObject();
            }
        });
        Check.same(Js.TRUE, env.invoke(custom, "test", Env.s("abc")), "test uses the exec property");
        Check.equal(1, calls[0], "the custom exec was called");
        JsObject bad = re("a", "");
        env.realm.method(bad, "exec", 1, new NativeFunction.Call() {
            /**
             * A broken exec returning a number.
             * <p>
             * 숫자를 반환하는 잘못된 exec.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return a number / 숫자
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Env.n(1);
            }
        });
        final JsObject badTarget = bad;
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls test with an exec that returns a primitive.
             * <p>
             * 원시 값을 반환하는 exec 로 test 를 호출한다.
             */
            public void run() {
                env.invoke(badTarget, "test", Env.s("a"));
            }
        }, "exec must return an object or null");
    }

    /**
     * Symbol.match: non-global gives the exec result, global gives all matches and handles empty matches.
     * <p>
     * Symbol.match: 비전역은 exec 결과를, 전역은 모든 일치를 주며 빈 일치를 처리한다.
     */
    public void testSymbolMatch() {
        Check.equal(Arrays.asList("b", "b"), Env.list(sym(re("(b)", ""), WellKnownSymbols.MATCH, "abc")), "non-global result with capture");
        Check.equal(Arrays.asList("a", "a", "a"), Env.list(sym(re("a", "g"), WellKnownSymbols.MATCH, "aXaYa")), "global matches");
        Check.same(Js.NULL, sym(re("z", "g"), WellKnownSymbols.MATCH, "abc"), "no match gives null");
        Check.equal(Arrays.asList("", "", "", ""), Env.list(sym(re("", "g"), WellKnownSymbols.MATCH, "abc")), "empty matches advance one position");
        JsObject g = re("a", "g");
        g.setStrict("lastIndex", Env.n(2));
        Check.equal(Arrays.asList("a", "a", "a"), Env.list(sym(g, WellKnownSymbols.MATCH, "aaa")), "global match starts from 0, not from lastIndex");
        Check.equal(4, Env.list(sym(re("(?:)", "g"), WellKnownSymbols.MATCH, "😀x")).size(), "without u the empty matches split the surrogate pair");
        Check.equal(3, Env.list(sym(re("(?:)", "gu"), WellKnownSymbols.MATCH, "😀x")).size(), "with u the pair is one step");
        Check.equal(2, Env.list(sym(re("(?:)", "gu"), WellKnownSymbols.MATCH, "😀")).size(), "empty matches before and after the pair");
    }

    /**
     * Symbol.replace: templates, functions, global and sticky behavior.
     * <p>
     * Symbol.replace: 틀, 함수, 전역과 고정 동작.
     */
    public void testSymbolReplace() {
        Check.equal("a[b]c", Env.str(sym(re("b", ""), WellKnownSymbols.REPLACE, "abc", Env.s("[$&]"))), "$&");
        Check.equal("a$c", Env.str(sym(re("b", ""), WellKnownSymbols.REPLACE, "abc", Env.s("$$"))), "$$");
        Check.equal("aacc", Env.str(sym(re("b", ""), WellKnownSymbols.REPLACE, "abc", Env.s("$`$'"))), "$` and $'");
        Check.equal("a-b-c", Env.str(sym(re("(\\w)(\\w)", ""), WellKnownSymbols.REPLACE, "ab c", Env.s("$1-$2"))).replace(" ", "-"), "numbered groups");
        Check.equal("[ab]x", Env.str(sym(re("(a)(b)", ""), WellKnownSymbols.REPLACE, "abx", Env.s("[$1$2]"))), "adjacent references");
        Check.equal("$3", Env.str(sym(re("a", ""), WellKnownSymbols.REPLACE, "a", Env.s("$3"))), "a reference to a missing group stays");
        Check.equal("a0", Env.str(sym(re("(a)", ""), WellKnownSymbols.REPLACE, "a", Env.s("$10"))), "$10 falls back to $1 followed by 0 when only one group exists");
        Check.equal("-a-b-c-", Env.str(sym(re("", "g"), WellKnownSymbols.REPLACE, "abc", Env.s("-"))), "global replace of the empty match");
        Check.equal("xbx", Env.str(sym(re("a", "g"), WellKnownSymbols.REPLACE, "aba", Env.s("x"))), "global");
        Check.equal("xba", Env.str(sym(re("a", ""), WellKnownSymbols.REPLACE, "aba", Env.s("x"))), "non-global replaces the first match only");
        Check.equal("", Env.str(sym(re("(a)|(b)", ""), WellKnownSymbols.REPLACE, "b", Env.s("$1"))), "an undefined group expands to the empty string");
        NativeFunction fn = new NativeFunction(env.realm, "f", 0, new NativeFunction.Call() {
            /**
             * Replacement function: reports its arguments.
             * <p>
             * 대체 함수: 받은 인수를 보고한다.
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return text describing the arguments / 인수를 설명하는 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                StringBuilder sb = new StringBuilder("<");
                // 인수를 하나씩 문자열로 이어 붙인다.
                for (int i = 0; i < args.length; i++) {
                    sb.append(i == 0 ? "" : "|").append(args[i].isUndefined() ? "undef" : Ops.toString(args[i]));
                }
                return Js.str(sb.append(">").toString());
            }
        });
        Check.equal("x<b|b|1|xbz>z", Env.str(sym(re("(b)", ""), WellKnownSymbols.REPLACE, "xbz", fn)), "function receives match, groups, offset and string");
        Check.equal("<a|undef|0|a>", Env.str(sym(re("a(x)?", ""), WellKnownSymbols.REPLACE, "a", fn)), "an undefined group is passed as undefined");
        JsObject y = re("a", "y");
        Check.equal("xa", Env.str(sym(y, WellKnownSymbols.REPLACE, "aa", Env.s("x"))), "sticky replaces at lastIndex 0 only");
        Check.equal("5", Env.str(sym(re("a", ""), WellKnownSymbols.REPLACE, "a", Env.n(5))), "the replacement value is converted with ToString");
    }

    /**
     * Symbol.search restores lastIndex.
     * <p>
     * Symbol.search 는 lastIndex 를 복원한다.
     */
    public void testSymbolSearch() {
        JsObject g = re("b", "g");
        g.setStrict("lastIndex", Env.n(2));
        Check.equal(1.0, Env.d(sym(g, WellKnownSymbols.SEARCH, "abc")), "found at 1 although lastIndex was 2");
        Check.equal(2.0, Env.d(g.get("lastIndex")), "lastIndex is restored");
        Check.equal(-1.0, Env.d(sym(re("z", ""), WellKnownSymbols.SEARCH, "abc")), "no match gives -1");
    }

    /**
     * Symbol.split: separators, captures, limits, empty input and the unicode flag.
     * <p>
     * Symbol.split: 구분자, 캡처, 제한, 빈 입력, 유니코드 플래그.
     */
    public void testSymbolSplit() {
        Check.equal(Arrays.asList("a", "b", "c"), Env.list(sym(re(",", ""), WellKnownSymbols.SPLIT, "a,b,c")), "simple split");
        Check.equal(Arrays.asList("a", "b"), Env.list(sym(re(",", ""), WellKnownSymbols.SPLIT, "a,b,c", Env.n(2))), "limit");
        Check.equal(Arrays.asList("a", ",", "b"), Env.list(sym(re("(,)", ""), WellKnownSymbols.SPLIT, "a,b")), "captures are included");
        Check.equal(Arrays.asList("a", "b", "c"), Env.list(sym(re("", ""), WellKnownSymbols.SPLIT, "abc")), "empty pattern splits into characters");
        Check.equal(Arrays.asList(""), Env.list(sym(re("x", ""), WellKnownSymbols.SPLIT, "")), "empty input gives [\"\"]");
        Check.equal(0, Env.list(sym(re("a", ""), WellKnownSymbols.SPLIT, "abc", Env.n(0))).size(), "limit 0 gives an empty array");
        Check.equal(Arrays.asList("", "b"), Env.list(sym(re("a", ""), WellKnownSymbols.SPLIT, "ab")), "separator at the start gives an empty first piece");
        Check.equal(Arrays.asList("a", "", "b"), Env.list(sym(re(",", ""), WellKnownSymbols.SPLIT, "a,,b")), "adjacent separators give an empty piece");
        Check.equal(Arrays.asList("a", "undefined", "b"), Env.list(sym(re("(x)?,", ""), WellKnownSymbols.SPLIT, "a,b")), "an undefined capture is included as undefined");
        Check.equal(2, Env.list(sym(re("", "u"), WellKnownSymbols.SPLIT, "😀😁")).size(), "with u a surrogate pair is one piece");
        Check.equal(4, Env.list(sym(re("", ""), WellKnownSymbols.SPLIT, "😀😁")).size(), "without u pairs are split");
        Check.equal(Arrays.asList("a", "b"), Env.list(sym(re(",", "g"), WellKnownSymbols.SPLIT, "a,b")), "the g flag does not matter");
        Check.equal(Arrays.asList("a", "b"), Env.list(sym(re(",", "y"), WellKnownSymbols.SPLIT, "a,b")), "an existing y flag is fine");
    }

    /**
     * Annex B compile re-initializes the regular expression in place.
     * <p>
     * 부속서 B compile 은 정규식을 그 자리에서 다시 초기화한다.
     */
    public void testCompile() {
        JsObject r = re("a", "g");
        r.setStrict("lastIndex", Env.n(3));
        JsValue ret = env.invoke(r, "compile", Env.s("b"), Env.s("i"));
        Check.same(r, ret, "compile returns the object");
        Check.equal("b", Env.str(r.get("source")), "new source");
        Check.equal("i", Env.str(r.get("flags")), "new flags");
        Check.equal(0.0, Env.d(r.get("lastIndex")), "lastIndex is reset");
        env.invoke(r, "compile", re("z", "m"));
        Check.equal("z", Env.str(r.get("source")), "compile from a RegExp");
        Check.equal("m", Env.str(r.get("flags")), "compile from a RegExp takes its flags");
        final JsObject target = r;
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Passes flags together with a RegExp pattern.
             * <p>
             * RegExp 패턴과 함께 플래그를 넘긴다.
             */
            public void run() {
                env.invoke(target, "compile", re("q", ""), Env.s("g"));
            }
        }, "flags with a RegExp pattern");
    }

    /**
     * Receivers that are not regular expressions are rejected where the specification requires it.
     * <p>
     * 규격이 요구하는 곳에서는 정규식이 아닌 수신 객체를 거부한다.
     */
    public void testReceiverChecks() {
        final JsObject proto = env.obj("RegExp.prototype");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls exec on the prototype object.
             * <p>
             * 프로토타입 객체에서 exec 를 호출한다.
             */
            public void run() {
                env.invoke(proto, "exec", Env.s("a"));
            }
        }, "exec on RegExp.prototype");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls toString on a primitive.
             * <p>
             * 원시 값에서 toString 을 호출한다.
             */
            public void run() {
                env.call("RegExp.prototype.toString", Env.n(1));
            }
        }, "toString on a number");
        Env.expect(ErrorKind.TYPE_ERROR, new Block() {
            /**
             * Calls test on undefined.
             * <p>
             * undefined 에서 test 를 호출한다.
             */
            public void run() {
                env.call("RegExp.prototype.test", Js.UNDEFINED, Env.s("a"));
            }
        }, "test on undefined");
    }

    /**
     * The step limit makes catastrophic matches fail with a RangeError; the cancellation hook aborts the script.
     * <p>
     * 단계 제한은 파국적인 일치를 RangeError 로 실패시키고, 취소 훅은 스크립트를 중단시킨다.
     */
    public void testResourceLimitsAndCancellation() {
        RegExpBuiltins.setStepLimit(env.realm, 2000);
        final JsObject r = re("(a*)*b", "");
        JsException e = Env.expect(ErrorKind.RANGE_ERROR, new Block() {
            /**
             * Runs a match that needs exponential time.
             * <p>
             * 지수 시간이 필요한 일치를 실행한다.
             */
            public void run() {
                exec(r, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            }
        }, "step limit");
        Check.contains(e.getErrorMessage(), "too complex", "message names the problem");
        RegExpBuiltins.setStepLimit(env.realm, 0);
        final JsObject c = re("(a*)*b", "");
        RegExpBuiltins.setCancelHook(env.realm, new RegExpCancel() {
            /**
             * Always asks to stop.
             * <p>
             * 항상 중단을 요청한다.
             *
             * @return true / true
             */
            public boolean isCancelled() {
                return true;
            }
        });
        Check.throwsException(ScriptAbortedException.class, new Block() {
            /**
             * Runs a long match with the cancellation hook set.
             * <p>
             * 취소 훅을 지정하고 긴 일치를 실행한다.
             */
            public void run() {
                exec(c, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            }
        }, "cancelled match");
        RegExpBuiltins.setCancelHook(env.realm, null);
    }

    /**
     * RegExp[Symbol.species] returns the receiver and is an accessor.
     * <p>
     * RegExp[Symbol.species] 는 수신 객체를 반환하는 접근자이다.
     */
    public void testSpecies() {
        JsObject ctor = env.obj("RegExp");
        Check.same(ctor, ctor.get(WellKnownSymbols.SPECIES), "species getter returns the constructor");
        Check.isTrue(ctor.getOwnProperty(WellKnownSymbols.SPECIES).isAccessor(), "accessor property");
        List<String> names = Arrays.asList("[Symbol.match]", "[Symbol.replace]", "[Symbol.search]", "[Symbol.split]");
        Object[] keys = { WellKnownSymbols.MATCH, WellKnownSymbols.REPLACE, WellKnownSymbols.SEARCH, WellKnownSymbols.SPLIT };
        double[] lengths = { 1, 2, 1, 2 };
        // 심볼 메서드마다 이름과 length 를 확인한다.
        for (int i = 0; i < keys.length; i++) {
            JsObject f = (JsObject) env.obj("RegExp.prototype").get(keys[i]);
            Check.equal(names.get(i), Env.str(f.get("name")), "name of " + names.get(i));
            Check.equal(lengths[i], Env.d(f.get("length")), "length of " + names.get(i));
        }
    }
}
