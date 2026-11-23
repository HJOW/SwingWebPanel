package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpCancel;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpCompiler;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpFlags;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpLimitException;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpProgram;
import org.duckdns.hjow.swingwebpanel.js.regexp.RegExpSyntaxException;

/**
 * The built-in <code>RegExp</code> constructor and <code>RegExp.prototype</code> (ECMA-262 6th edition 21.2) on top of the regular expression
 * engine of the package js.regexp: construction and Annex B <code>compile</code>, <code>exec</code> with the <code>lastIndex</code> protocol of the
 * global and sticky flags, <code>test</code>, <code>toString</code>, the flag accessors, <code>[Symbol.match]</code>, <code>[Symbol.replace]</code>,
 * <code>[Symbol.search]</code>, <code>[Symbol.split]</code> and <code>RegExp[Symbol.species]</code>.
 * <p>
 * Resource limits: a match that exceeds the step or backtrack limit of the engine becomes a <code>RangeError</code> with a message that names
 * the limit. If a cancellation hook (see {@link #setCancelHook(JsRealm, RegExpCancel)}) reports cancellation, the match is aborted with a
 * {@link ScriptAbortedException} (reason CANCELLED) so the host can stop the script. The step limit can be changed per realm with
 * {@link #setStepLimit(JsRealm, long)}.
 * <p>
 * Extensions beyond ES2015: the flag <code>s</code> (dotAll) with the accessor <code>dotAll</code>; named groups in patterns are accepted by the
 * engine but the result array has no <code>groups</code> property. Not implemented: the legacy static properties <code>RegExp.$1</code> and
 * <code>RegExp.lastMatch</code>.
 * <p>
 * js.regexp 패키지의 정규식 엔진 위에 만든 내장 <code>RegExp</code> 생성자와 <code>RegExp.prototype</code>(ECMA-262 6판 21.2): 생성과 부속서 B 의
 * <code>compile</code>, g/y 플래그의 <code>lastIndex</code> 규약을 따르는 <code>exec</code>, <code>test</code>, <code>toString</code>,
 * 플래그 접근자, <code>[Symbol.match]</code>, <code>[Symbol.replace]</code>, <code>[Symbol.search]</code>, <code>[Symbol.split]</code>,
 * <code>RegExp[Symbol.species]</code>.
 * <p>
 * 자원 제한: 엔진의 단계 수나 역추적 제한을 넘는 일치는 제한 이름을 알리는 <code>RangeError</code> 가 된다. 취소 훅({@link #setCancelHook(JsRealm, RegExpCancel)})이
 * 취소를 알리면 일치를 {@link ScriptAbortedException}(이유 CANCELLED)으로 중단하여 호스트가 스크립트를 멈출 수 있게 한다. 단계 제한은
 * {@link #setStepLimit(JsRealm, long)} 으로 realm 마다 바꿀 수 있다.
 * <p>
 * ES2015 를 넘는 확장: 플래그 <code>s</code>(dotAll)와 접근자 <code>dotAll</code>. 패턴의 이름 있는 그룹은 엔진이 받아들이지만 결과 배열에
 * <code>groups</code> 프로퍼티는 없다. 미구현: 구형 정적 프로퍼티 <code>RegExp.$1</code>, <code>RegExp.lastMatch</code>.
 */
public final class RegExpBuiltins {

    /**
     * Cancellation hooks of realms.
     * <p>
     * realm 별 취소 훅.
     */
    private static final Map<JsRealm, RegExpCancel> CANCEL_HOOKS = Collections.synchronizedMap(new WeakHashMap<JsRealm, RegExpCancel>());

    /**
     * Step limits of realms.
     * <p>
     * realm 별 단계 제한.
     */
    private static final Map<JsRealm, Long> STEP_LIMITS = Collections.synchronizedMap(new WeakHashMap<JsRealm, Long>());

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RegExpBuiltins() {
    }

    /**
     * Installs the cancellation hook polled by regular expression matches of a realm (the host usually forwards the cancellation flag of the
     * script execution limits); null removes it.
     * <p>
     * realm 의 정규식 일치가 주기적으로 확인하는 취소 훅을 설치한다(호스트는 보통 스크립트 실행 제한의 취소 플래그를 전달한다). null 이면 제거한다.
     *
     * @param realm the realm / realm
     * @param hook  the hook or null / 훅 또는 null
     */
    public static void setCancelHook(JsRealm realm, RegExpCancel hook) {
        if (hook == null) {
            CANCEL_HOOKS.remove(realm);
        } else {
            CANCEL_HOOKS.put(realm, hook);
        }
    }

    /**
     * Sets the matching step limit used by regular expressions created later in a realm; null-like values (zero or less) restore the default.
     * <p>
     * 이후 realm 에서 만드는 정규식이 쓰는 일치 단계 제한을 지정한다. 0 이하이면 기본값으로 되돌린다.
     *
     * @param realm the realm / realm
     * @param limit maximum matching steps per match / 일치 한 번당 최대 단계 수
     */
    public static void setStepLimit(JsRealm realm, long limit) {
        if (limit <= 0) {
            STEP_LIMITS.remove(realm);
        } else {
            STEP_LIMITS.put(realm, Long.valueOf(limit));
        }
    }

    /**
     * Installs <code>RegExp</code> into a realm.
     * <p>
     * realm 에 <code>RegExp</code> 를 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(final JsRealm realm) {
        JsObject proto = realm.newObject();
        NativeFunction ctor = Support.makeConstructor(realm, "RegExp", 2, proto, new NativeFunction.Call() {
            /**
             * RegExp(pattern, flags) called as a function.
             * <p>
             * 함수로 호출한 RegExp(pattern, flags).
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the regular expression / 정규식
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return create(r, args, null);
            }
        }, new NativeFunction.Construct() {
            /**
             * new RegExp(pattern, flags).
             * <p>
             * new RegExp(pattern, flags).
             *
             * @param r         realm / realm
             * @param args      arguments / 인수
             * @param newTarget new.target / new.target
             * @return the regular expression / 정규식
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return (JsObject) create(r, args, newTarget);
            }
        });
        Support.defineSpecies(realm, ctor);
        installAccessors(realm, proto);
        installMethods(realm, proto);
        installSymbolMethods(realm, proto);
    }

    /**
     * IsRegExp (ECMA-262 7.2.8).
     * <p>
     * IsRegExp (ECMA-262 7.2.8).
     *
     * @param a the value / 값
     * @return true if the value is regular-expression-like / 정규식 같으면 true
     */
    static boolean isRegExp(JsValue a) {
        if (!(a instanceof JsObject)) {
            return false;
        }
        JsValue m = ((JsObject) a).get(WellKnownSymbols.MATCH);
        if (!m.isUndefined()) {
            return Ops.toBoolean(m);
        }
        return a instanceof RegExpObject;
    }

    /**
     * The RegExp constructor algorithm (ECMA-262 21.2.3.1).
     * <p>
     * RegExp 생성자 알고리즘(ECMA-262 21.2.3.1).
     *
     * @param r         realm / realm
     * @param args      arguments / 인수
     * @param newTarget new.target, or null when called as a function / new.target. 함수로 호출했으면 null
     * @return the regular expression / 정규식
     */
    private static JsValue create(JsRealm r, JsValue[] args, JsObject newTarget) {
        JsValue pattern = Js.arg(args, 0);
        JsValue flags = Js.arg(args, 1);
        boolean patternIsRegExp = isRegExp(pattern);
        JsObject nt = newTarget;
        if (nt == null) {
            nt = r.getIntrinsic("%RegExp%");
            if (patternIsRegExp && flags.isUndefined()) {
                JsValue pc = ((JsObject) pattern).get("constructor");
                if (Ops.sameValue(nt, pc)) {
                    return pattern;
                }
            }
        }
        JsValue p;
        JsValue f;
        if (pattern instanceof RegExpObject && ((RegExpObject) pattern).isInitialized()) {
            RegExpObject src = (RegExpObject) pattern;
            p = Js.str(src.getSource());
            f = flags.isUndefined() ? Js.str(src.getOriginalFlags()) : flags;
        } else if (patternIsRegExp) {
            p = ((JsObject) pattern).get("source");
            f = flags.isUndefined() ? ((JsObject) pattern).get("flags") : flags;
        } else {
            p = pattern;
            f = flags;
        }
        JsObject proto = Ops.getPrototypeFromConstructor(r, nt, r.getIntrinsic("%RegExpPrototype%"));
        RegExpObject obj = new RegExpObject(proto);
        initialize(r, obj, p, f);
        return obj;
    }

    /**
     * RegExpInitialize (ECMA-262 21.2.3.2.2): converts and compiles pattern and flags and resets lastIndex.
     * <p>
     * RegExpInitialize (ECMA-262 21.2.3.2.2): 패턴과 플래그를 변환·컴파일하고 lastIndex 를 되돌린다.
     *
     * @param r       realm / realm
     * @param obj     the object to initialize / 초기화할 객체
     * @param pattern pattern value, undefined means empty / 패턴 값. undefined 는 빈 문자열
     * @param flags   flags value, undefined means none / 플래그 값. undefined 는 없음
     */
    private static void initialize(JsRealm r, RegExpObject obj, JsValue pattern, JsValue flags) {
        String p = pattern.isUndefined() ? "" : Ops.toString(pattern);
        String f = flags.isUndefined() ? "" : Ops.toString(flags);
        RegExpProgram program;
        try {
            program = RegExpCompiler.compile(p, f);
        } catch (RegExpSyntaxException e) {
            throw JsException.syntaxError(e.getMessage());
        }
        Long limit = STEP_LIMITS.get(r);
        if (limit != null) {
            program.setStepLimit(limit.longValue());
        }
        obj.initialize(program, p, f);
        obj.setStrict("lastIndex", Js.num(0));
    }

    /**
     * Requires an initialized RegExp object as receiver.
     * <p>
     * 초기화된 RegExp 객체가 수신 객체여야 함을 확인한다.
     *
     * @param v      the this value / this 값
     * @param method name for the error message / 오류 메시지용 이름
     * @return the regular expression / 정규식
     */
    private static RegExpObject thisRegExp(JsValue v, String method) {
        if (!(v instanceof RegExpObject) || !((RegExpObject) v).isInitialized()) {
            throw JsException.typeError("Method RegExp.prototype." + method + " called on incompatible receiver");
        }
        return (RegExpObject) v;
    }

    /**
     * Returns the cancellation hook of a realm.
     * <p>
     * realm 의 취소 훅을 반환한다.
     *
     * @param r realm / realm
     * @return the hook or null / 훅 또는 null
     */
    private static RegExpCancel cancelOf(JsRealm r) {
        return CANCEL_HOOKS.get(r);
    }

    /**
     * Sets lastIndex with throwing semantics (Set(R, "lastIndex", v, true)).
     * <p>
     * 예외를 던지는 방식으로 lastIndex 를 지정한다(Set(R, "lastIndex", v, true)).
     *
     * @param rx the object / 객체
     * @param v  the new value / 새 값
     */
    private static void setLastIndex(JsObject rx, double v) {
        rx.setStrict("lastIndex", Js.num(v));
    }

    /**
     * RegExpBuiltinExec (ECMA-262 21.2.5.2.2).
     * <p>
     * RegExpBuiltinExec (ECMA-262 21.2.5.2.2).
     *
     * @param r the realm / realm
     * @param x the regular expression / 정규식
     * @param s the input string / 입력 문자열
     * @return the result array or null / 결과 배열 또는 null
     */
    static JsValue builtinExec(JsRealm r, RegExpObject x, String s) {
        double li = Ops.toLength(x.get("lastIndex"));
        int bits = x.getProgram().getFlags();
        boolean global = (bits & RegExpFlags.GLOBAL) != 0;
        boolean sticky = (bits & RegExpFlags.STICKY) != 0;
        if (!global && !sticky) {
            li = 0;
        }
        if (li > s.length()) {
            setLastIndex(x, 0);
            return Js.NULL;
        }
        int start = (int) li;
        int[] m;
        try {
            m = sticky ? x.getProgram().matchAt(s, start, cancelOf(r)) : x.getProgram().exec(s, start, cancelOf(r));
        } catch (RegExpLimitException e) {
            throw limitError(e);
        }
        if (m == null) {
            setLastIndex(x, 0);
            return Js.NULL;
        }
        if (global || sticky) {
            setLastIndex(x, m[1]);
        }
        JsArray a = r.newArray();
        int groups = m.length / 2;
        // 캡처 그룹마다 일치한 부분 문자열(일치하지 않은 그룹은 undefined)을 배열에 넣는다.
        for (int i = 0; i < groups; i++) {
            int b = m[2 * i];
            int e = m[2 * i + 1];
            a.add(b < 0 || e < 0 ? Js.UNDEFINED : Js.str(s.substring(b, e)));
        }
        a.createDataProperty("index", Js.num(m[0]));
        a.createDataProperty("input", Js.str(s));
        return a;
    }

    /**
     * Converts an engine resource-limit exception into the JavaScript-visible error.
     * <p>
     * 엔진의 자원 제한 예외를 JavaScript 에서 보이는 오류로 바꾼다.
     *
     * @param e the engine exception / 엔진 예외
     * @return RangeError for limits, ScriptAbortedException for cancellation / 제한이면 RangeError, 취소이면 ScriptAbortedException
     */
    private static RuntimeException limitError(RegExpLimitException e) {
        if (e.isCancelled()) {
            return new ScriptAbortedException(ScriptAbortedException.Reason.CANCELLED, "regular expression match cancelled");
        }
        return JsException.rangeError("Regular expression too complex: " + e.getMessage());
    }

    /**
     * RegExpExec (ECMA-262 21.2.5.2.1): calls a user supplied <code>exec</code> method when present.
     * <p>
     * RegExpExec (ECMA-262 21.2.5.2.1): 사용자가 지정한 <code>exec</code> 메서드가 있으면 호출한다.
     *
     * @param r  the realm / realm
     * @param rx the regular expression-like object / 정규식 같은 객체
     * @param s  the input string / 입력 문자열
     * @return the result object or null / 결과 객체 또는 null
     */
    private static JsValue regExpExec(JsRealm r, JsObject rx, String s) {
        JsValue exec = rx.get("exec");
        if (exec.isCallable()) {
            JsValue result = Ops.call(exec, rx, new JsValue[] { Js.str(s) });
            if (!(result instanceof JsObject) && !result.isNull()) {
                throw JsException.typeError("object or null expected from exec");
            }
            return result;
        }
        if (!(rx instanceof RegExpObject) || !((RegExpObject) rx).isInitialized()) {
            throw JsException.typeError("RegExp.prototype.exec called on incompatible receiver");
        }
        return builtinExec(r, (RegExpObject) rx, s);
    }

    /**
     * AdvanceStringIndex (ECMA-262 21.2.5.2.3): next index, stepping over a surrogate pair in unicode mode.
     * <p>
     * AdvanceStringIndex (ECMA-262 21.2.5.2.3): 다음 위치. 유니코드 모드에서는 서로게이트 쌍을 건너뛴다.
     *
     * @param s       the string / 문자열
     * @param index   the index / 위치
     * @param unicode unicode mode / 유니코드 모드
     * @return the next index / 다음 위치
     */
    private static double advance(String s, double index, boolean unicode) {
        if (!unicode || index + 1 >= s.length()) {
            return index + 1;
        }
        int i = (int) index;
        if (Character.isHighSurrogate(s.charAt(i)) && Character.isLowSurrogate(s.charAt(i + 1))) {
            return index + 2;
        }
        return index + 1;
    }

    /**
     * Escapes a pattern for the <code>source</code> property (EscapeRegExpPattern): empty becomes "(?:)", a forward slash outside a class and
     * line terminators are escaped so that <code>/source/</code> is a valid literal for the same pattern.
     * <p>
     * <code>source</code> 프로퍼티용으로 패턴을 이스케이프한다(EscapeRegExpPattern): 빈 패턴은 "(?:)" 가 되고, 클래스 밖의 슬래시와 줄 끝 문자를
     * 이스케이프하여 <code>/source/</code> 가 같은 패턴의 올바른 리터럴이 되게 한다.
     *
     * @param p the pattern text / 패턴 문자열
     * @return the escaped text / 이스케이프한 문자열
     */
    static String escapeSource(String p) {
        if (p.length() == 0) {
            return "(?:)";
        }
        StringBuilder sb = new StringBuilder(p.length() + 8);
        boolean inClass = false;
        // 패턴을 앞에서부터 읽으며 이스케이프 열(\x)은 그대로 두고, 클래스 안인지 추적한다.
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '\\' && i + 1 < p.length()) {
                char n = p.charAt(i + 1);
                if (n == '\n') {
                    sb.append("\\n");
                } else if (n == '\r') {
                    sb.append("\\r");
                } else if (n == (char) 0x2028) {
                    sb.append("\\u2028");
                } else if (n == (char) 0x2029) {
                    sb.append("\\u2029");
                } else {
                    sb.append(c).append(n);
                }
                i++;
                continue;
            }
            if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            }
            if (c == '/' && !inClass) {
                sb.append("\\/");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == (char) 0x2028) {
                sb.append("\\u2028");
            } else if (c == (char) 0x2029) {
                sb.append("\\u2029");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the receiver's flag bit for an accessor such as <code>global</code> (ECMA-262 21.2.5.4 and similar).
     * <p>
     * <code>global</code> 같은 접근자를 위해 수신 객체의 플래그 비트를 반환한다(ECMA-262 21.2.5.4 등).
     *
     * @param r    the realm / realm
     * @param v    the this value / this 값
     * @param bit  the flag bit / 플래그 비트
     * @param name the accessor name / 접근자 이름
     * @return true/false, or undefined for RegExp.prototype itself / true/false. RegExp.prototype 자체이면 undefined
     */
    private static JsValue flagValue(JsRealm r, JsValue v, int bit, String name) {
        JsObject o = Support.requireObject(v, "get RegExp.prototype." + name);
        if (!(o instanceof RegExpObject) || !((RegExpObject) o).isInitialized()) {
            if (o == r.getIntrinsic("%RegExpPrototype%")) {
                return Js.UNDEFINED;
            }
            throw JsException.typeError("RegExp.prototype." + name + " getter called on non-RegExp object");
        }
        return Js.bool((((RegExpObject) o).getProgram().getFlags() & bit) != 0);
    }

    /**
     * Defines one flag accessor.
     * <p>
     * 플래그 접근자 하나를 정의한다.
     *
     * @param realm the realm / realm
     * @param proto RegExp.prototype / RegExp.prototype
     * @param name  accessor name / 접근자 이름
     * @param bit   flag bit / 플래그 비트
     */
    private static void flagAccessor(JsRealm realm, JsObject proto, final String name, final int bit) {
        realm.getter(proto, name, new NativeFunction.Call() {
            /**
             * Reads the flag of the receiver.
             * <p>
             * 수신 객체의 플래그를 읽는다.
             *
             * @param r         realm / realm
             * @param thisValue the receiver / 수신 객체
             * @param args      arguments (ignored) / 인수(무시)
             * @return the flag / 플래그
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return flagValue(r, thisValue, bit, name);
            }
        });
    }

    /**
     * Installs the accessors <code>flags</code>, <code>source</code> and the single flag accessors.
     * <p>
     * 접근자 <code>flags</code>, <code>source</code> 와 개별 플래그 접근자를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto RegExp.prototype / RegExp.prototype
     */
    private static void installAccessors(JsRealm realm, JsObject proto) {
        flagAccessor(realm, proto, "global", RegExpFlags.GLOBAL);
        flagAccessor(realm, proto, "ignoreCase", RegExpFlags.IGNORE_CASE);
        flagAccessor(realm, proto, "multiline", RegExpFlags.MULTILINE);
        flagAccessor(realm, proto, "dotAll", RegExpFlags.DOT_ALL);
        flagAccessor(realm, proto, "unicode", RegExpFlags.UNICODE);
        flagAccessor(realm, proto, "sticky", RegExpFlags.STICKY);
        realm.getter(proto, "flags", new NativeFunction.Call() {
            /**
             * Builds the flags text from the individual accessors in the order g, i, m, s, u, y.
             * <p>
             * 개별 접근자로 g, i, m, s, u, y 순서의 플래그 문자열을 만든다.
             *
             * @param r         realm / realm
             * @param thisValue the receiver / 수신 객체
             * @param args      arguments (ignored) / 인수(무시)
             * @return the flags text / 플래그 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Support.requireObject(thisValue, "get RegExp.prototype.flags");
                StringBuilder sb = new StringBuilder(6);
                if (Ops.toBoolean(o.get("global"))) {
                    sb.append('g');
                }
                if (Ops.toBoolean(o.get("ignoreCase"))) {
                    sb.append('i');
                }
                if (Ops.toBoolean(o.get("multiline"))) {
                    sb.append('m');
                }
                if (Ops.toBoolean(o.get("dotAll"))) {
                    sb.append('s');
                }
                if (Ops.toBoolean(o.get("unicode"))) {
                    sb.append('u');
                }
                if (Ops.toBoolean(o.get("sticky"))) {
                    sb.append('y');
                }
                return Js.str(sb.toString());
            }
        });
        realm.getter(proto, "source", new NativeFunction.Call() {
            /**
             * Returns the escaped pattern text.
             * <p>
             * 이스케이프한 패턴 문자열을 반환한다.
             *
             * @param r         realm / realm
             * @param thisValue the receiver / 수신 객체
             * @param args      arguments (ignored) / 인수(무시)
             * @return the source text / source 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Support.requireObject(thisValue, "get RegExp.prototype.source");
                if (!(o instanceof RegExpObject) || !((RegExpObject) o).isInitialized()) {
                    if (o == r.getIntrinsic("%RegExpPrototype%")) {
                        return Js.str("(?:)");
                    }
                    throw JsException.typeError("RegExp.prototype.source getter called on non-RegExp object");
                }
                return Js.str(escapeSource(((RegExpObject) o).getSource()));
            }
        });
    }

    /**
     * Installs <code>exec</code>, <code>test</code>, <code>toString</code> and <code>compile</code>.
     * <p>
     * <code>exec</code>, <code>test</code>, <code>toString</code>, <code>compile</code> 을 설치한다.
     *
     * @param realm the realm / realm
     * @param proto RegExp.prototype / RegExp.prototype
     */
    private static void installMethods(JsRealm realm, JsObject proto) {
        realm.method(proto, "exec", 1, new NativeFunction.Call() {
            /**
             * RegExp.prototype.exec(string).
             * <p>
             * RegExp.prototype.exec(string).
             *
             * @param r         realm / realm
             * @param thisValue the regular expression / 정규식
             * @param args      arguments: the string / 인수: 문자열
             * @return the result array or null / 결과 배열 또는 null
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                RegExpObject x = thisRegExp(thisValue, "exec");
                return builtinExec(r, x, Ops.toString(Js.arg(args, 0)));
            }
        });
        realm.method(proto, "test", 1, new NativeFunction.Call() {
            /**
             * RegExp.prototype.test(string).
             * <p>
             * RegExp.prototype.test(string).
             *
             * @param r         realm / realm
             * @param thisValue the regular expression-like object / 정규식 같은 객체
             * @param args      arguments: the string / 인수: 문자열
             * @return true if there is a match / 일치가 있으면 true
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Support.requireObject(thisValue, "RegExp.prototype.test");
                String s = Ops.toString(Js.arg(args, 0));
                return Js.bool(!regExpExec(r, o, s).isNull());
            }
        });
        realm.method(proto, "toString", 0, new NativeFunction.Call() {
            /**
             * RegExp.prototype.toString(): "/source/flags".
             * <p>
             * RegExp.prototype.toString(): "/source/flags".
             *
             * @param r         realm / realm
             * @param thisValue the receiver / 수신 객체
             * @param args      arguments (ignored) / 인수(무시)
             * @return the text / 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject o = Support.requireObject(thisValue, "RegExp.prototype.toString");
                String p = Ops.toString(o.get("source"));
                String f = Ops.toString(o.get("flags"));
                return Js.str("/" + p + "/" + f);
            }
        });
        realm.method(proto, "compile", 2, new NativeFunction.Call() {
            /**
             * Annex B compile(pattern, flags): re-initializes the regular expression in place.
             * <p>
             * 부속서 B compile(pattern, flags): 정규식을 그 자리에서 다시 초기화한다.
             *
             * @param r         realm / realm
             * @param thisValue the regular expression / 정규식
             * @param args      arguments / 인수
             * @return the receiver / 수신 객체
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                RegExpObject o = thisRegExp(thisValue, "compile");
                JsValue pattern = Js.arg(args, 0);
                JsValue flags = Js.arg(args, 1);
                JsValue p;
                JsValue f;
                if (pattern instanceof RegExpObject && ((RegExpObject) pattern).isInitialized()) {
                    if (!flags.isUndefined()) {
                        throw JsException.typeError("flags must be undefined when the pattern is a RegExp");
                    }
                    p = Js.str(((RegExpObject) pattern).getSource());
                    f = Js.str(((RegExpObject) pattern).getOriginalFlags());
                } else {
                    p = pattern;
                    f = flags;
                }
                initialize(r, o, p, f);
                return o;
            }
        });
    }

    /**
     * Installs the symbol-keyed methods.
     * <p>
     * 심볼 키 메서드를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto RegExp.prototype / RegExp.prototype
     */
    private static void installSymbolMethods(JsRealm realm, JsObject proto) {
        realm.method(proto, WellKnownSymbols.MATCH, 1, new NativeFunction.Call() {
            /**
             * RegExp.prototype[@@match](string).
             * <p>
             * RegExp.prototype[@@match](string).
             *
             * @param r         realm / realm
             * @param thisValue the regular expression-like object / 정규식 같은 객체
             * @param args      arguments: the string / 인수: 문자열
             * @return the match result, an array of all matches, or null / 일치 결과, 모든 일치의 배열, 또는 null
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return symbolMatch(r, Support.requireObject(thisValue, "RegExp.prototype[Symbol.match]"), Ops.toString(Js.arg(args, 0)));
            }
        });
        realm.method(proto, WellKnownSymbols.REPLACE, 2, new NativeFunction.Call() {
            /**
             * RegExp.prototype[@@replace](string, replaceValue).
             * <p>
             * RegExp.prototype[@@replace](string, replaceValue).
             *
             * @param r         realm / realm
             * @param thisValue the regular expression-like object / 정규식 같은 객체
             * @param args      arguments / 인수
             * @return the new string / 새 문자열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject rx = Support.requireObject(thisValue, "RegExp.prototype[Symbol.replace]");
                return Js.str(symbolReplace(r, rx, Ops.toString(Js.arg(args, 0)), Js.arg(args, 1)));
            }
        });
        realm.method(proto, WellKnownSymbols.SEARCH, 1, new NativeFunction.Call() {
            /**
             * RegExp.prototype[@@search](string).
             * <p>
             * RegExp.prototype[@@search](string).
             *
             * @param r         realm / realm
             * @param thisValue the regular expression-like object / 정규식 같은 객체
             * @param args      arguments: the string / 인수: 문자열
             * @return the index of the first match or -1 / 첫 일치의 위치 또는 -1
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject rx = Support.requireObject(thisValue, "RegExp.prototype[Symbol.search]");
                String s = Ops.toString(Js.arg(args, 0));
                JsValue previous = rx.get("lastIndex");
                rx.setStrict("lastIndex", Js.num(0));
                JsValue result = regExpExec(r, rx, s);
                rx.setStrict("lastIndex", previous);
                if (result.isNull()) {
                    return Js.num(-1);
                }
                return ((JsObject) result).get("index");
            }
        });
        realm.method(proto, WellKnownSymbols.SPLIT, 2, new NativeFunction.Call() {
            /**
             * RegExp.prototype[@@split](string, limit).
             * <p>
             * RegExp.prototype[@@split](string, limit).
             *
             * @param r         realm / realm
             * @param thisValue the regular expression-like object / 정규식 같은 객체
             * @param args      arguments / 인수
             * @return the array of pieces / 조각 배열
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return symbolSplit(r, Support.requireObject(thisValue, "RegExp.prototype[Symbol.split]"), Ops.toString(Js.arg(args, 0)), Js.arg(args, 1));
            }
        });
    }

    /**
     * RegExp.prototype[@@match] (ECMA-262 21.2.5.6).
     * <p>
     * RegExp.prototype[@@match] (ECMA-262 21.2.5.6).
     *
     * @param r  the realm / realm
     * @param rx the regular expression-like object / 정규식 같은 객체
     * @param s  the string / 문자열
     * @return the result / 결과
     */
    private static JsValue symbolMatch(JsRealm r, JsObject rx, String s) {
        boolean global = Ops.toBoolean(rx.get("global"));
        if (!global) {
            return regExpExec(r, rx, s);
        }
        boolean fullUnicode = Ops.toBoolean(rx.get("unicode"));
        rx.setStrict("lastIndex", Js.num(0));
        JsArray a = r.newArray();
        int n = 0;
        // 더 이상 일치가 없을 때까지 반복하며 일치한 문자열을 모은다. 빈 일치는 lastIndex 를 한 칸 옮겨 무한 반복을 막는다.
        while (true) {
            JsValue result = regExpExec(r, rx, s);
            if (result.isNull()) {
                return n == 0 ? Js.NULL : a;
            }
            String matchStr = Ops.toString(((JsObject) result).get("0"));
            a.createDataPropertyOrThrow(String.valueOf(n), Js.str(matchStr));
            if (matchStr.length() == 0) {
                double thisIndex = Ops.toLength(rx.get("lastIndex"));
                rx.setStrict("lastIndex", Js.num(advance(s, thisIndex, fullUnicode)));
            }
            n++;
        }
    }

    /**
     * RegExp.prototype[@@replace] (ECMA-262 21.2.5.8).
     * <p>
     * RegExp.prototype[@@replace] (ECMA-262 21.2.5.8).
     *
     * @param r            the realm / realm
     * @param rx           the regular expression-like object / 정규식 같은 객체
     * @param s            the string / 문자열
     * @param replaceValue the replacement function or text / 바꿀 함수 또는 문자열
     * @return the new string / 새 문자열
     */
    private static String symbolReplace(JsRealm r, JsObject rx, String s, JsValue replaceValue) {
        int lengthS = s.length();
        boolean functional = replaceValue.isCallable();
        String replacement = functional ? null : Ops.toString(replaceValue);
        boolean global = Ops.toBoolean(rx.get("global"));
        boolean fullUnicode = false;
        if (global) {
            fullUnicode = Ops.toBoolean(rx.get("unicode"));
            rx.setStrict("lastIndex", Js.num(0));
        }
        List<JsObject> results = new ArrayList<JsObject>();
        // 일치 결과를 모두 모은다. 전역이 아니면 한 번만, 전역이면 일치가 없을 때까지 반복한다.
        while (true) {
            JsValue result = regExpExec(r, rx, s);
            if (result.isNull()) {
                break;
            }
            results.add((JsObject) result);
            if (!global) {
                break;
            }
            String matchStr = Ops.toString(((JsObject) result).get("0"));
            if (matchStr.length() == 0) {
                double thisIndex = Ops.toLength(rx.get("lastIndex"));
                rx.setStrict("lastIndex", Js.num(advance(s, thisIndex, fullUnicode)));
            }
        }
        StringBuilder accumulated = new StringBuilder();
        int nextSourcePosition = 0;
        // 모은 결과를 차례로 바꿔 넣는다. 겹치는(이미 지나간) 위치의 결과는 건너뛴다.
        for (JsObject result : results) {
            double nCaptures = Math.max(Ops.toLength(result.get("length")) - 1, 0);
            String matched = Ops.toString(result.get("0"));
            int matchLength = matched.length();
            double posD = Math.max(Math.min(Ops.toInteger(result.get("index")), lengthS), 0);
            int position = (int) posD;
            int caps = (int) Math.min(nCaptures, 1000000);
            JsValue[] captureValues = new JsValue[caps];
            String[] captureTexts = new String[caps];
            // 캡처마다 undefined 가 아니면 문자열로 바꿔 보관한다.
            for (int i = 0; i < caps; i++) {
                JsValue cap = result.get(String.valueOf(i + 1));
                if (!cap.isUndefined()) {
                    captureTexts[i] = Ops.toString(cap);
                    captureValues[i] = Js.str(captureTexts[i]);
                } else {
                    captureValues[i] = Js.UNDEFINED;
                }
            }
            String rep;
            if (functional) {
                JsValue[] fargs = new JsValue[caps + 3];
                fargs[0] = Js.str(matched);
                System.arraycopy(captureValues, 0, fargs, 1, caps);
                fargs[caps + 1] = Js.num(position);
                fargs[caps + 2] = Js.str(s);
                rep = Ops.toString(Ops.call(replaceValue, Js.UNDEFINED, fargs));
            } else {
                rep = substitution(matched, s, position, captureTexts, replacement);
            }
            if (position >= nextSourcePosition) {
                accumulated.append(s, nextSourcePosition, position).append(rep);
                nextSourcePosition = position + matchLength;
            }
        }
        if (nextSourcePosition >= lengthS) {
            return accumulated.toString();
        }
        return accumulated.append(s, nextSourcePosition, lengthS).toString();
    }

    /**
     * GetSubstitution (ECMA-262 21.1.3.14.1): expands <code>$$</code>, <code>$&amp;</code>, <code>$`</code>, <code>$'</code>, <code>$n</code> and
     * <code>$nn</code>. As in all browsers, a two digit reference that is out of range falls back to its first digit; references to groups that
     * do not exist stay as written. <code>$&lt;name&gt;</code> (ES2018) is not expanded.
     * <p>
     * GetSubstitution (ECMA-262 21.1.3.14.1): <code>$$</code>, <code>$&amp;</code>, <code>$`</code>, <code>$'</code>, <code>$n</code>, <code>$nn</code> 을
     * 펼친다. 모든 브라우저처럼 범위를 벗어난 두 자리 참조는 첫 자리로 되돌아가며, 없는 그룹에 대한 참조는 쓴 그대로 둔다.
     * <code>$&lt;name&gt;</code>(ES2018)은 펼치지 않는다.
     *
     * @param matched  the matched text / 일치한 문자열
     * @param str      the whole string / 전체 문자열
     * @param position index of the match / 일치 위치
     * @param captures capture texts, null for undefined groups / 캡처 문자열. 정의되지 않은 그룹은 null
     * @param template the replacement template / 바꿀 문자열 틀
     * @return the expanded text / 펼친 문자열
     */
    static String substitution(String matched, String str, int position, String[] captures, String template) {
        int m = captures.length;
        int len = template.length();
        StringBuilder sb = new StringBuilder(len + matched.length());
        int i = 0;
        // 틀을 앞에서부터 읽으며 $ 로 시작하는 특별한 열을 펼친다.
        while (i < len) {
            char c = template.charAt(i);
            if (c != '$' || i + 1 >= len) {
                sb.append(c);
                i++;
                continue;
            }
            char n = template.charAt(i + 1);
            if (n == '$') {
                sb.append('$');
                i += 2;
            } else if (n == '&') {
                sb.append(matched);
                i += 2;
            } else if (n == '`') {
                sb.append(str, 0, position);
                i += 2;
            } else if (n == '\'') {
                int tail = Math.min(position + matched.length(), str.length());
                sb.append(str, tail, str.length());
                i += 2;
            } else if (n >= '0' && n <= '9') {
                int d1 = n - '0';
                int index = -1;
                int consumed = 2;
                if (i + 2 < len && template.charAt(i + 2) >= '0' && template.charAt(i + 2) <= '9') {
                    int two = d1 * 10 + (template.charAt(i + 2) - '0');
                    if (two >= 1 && two <= m) {
                        index = two;
                        consumed = 3;
                    }
                }
                if (index < 0 && d1 >= 1 && d1 <= m) {
                    index = d1;
                }
                if (index < 0) {
                    sb.append('$');
                    i++;
                } else {
                    String cap = captures[index - 1];
                    if (cap != null) {
                        sb.append(cap);
                    }
                    i += consumed;
                }
            } else {
                sb.append('$');
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * RegExp.prototype[@@split] (ECMA-262 21.2.5.11).
     * <p>
     * RegExp.prototype[@@split] (ECMA-262 21.2.5.11).
     *
     * @param r     the realm / realm
     * @param rx    the regular expression-like object / 정규식 같은 객체
     * @param s     the string / 문자열
     * @param limit the limit argument / 제한 인수
     * @return the array of pieces / 조각 배열
     */
    private static JsValue symbolSplit(JsRealm r, JsObject rx, String s, JsValue limit) {
        JsObject c = Support.speciesConstructor(rx, r.getIntrinsic("%RegExp%"));
        String flags = Ops.toString(rx.get("flags"));
        boolean unicodeMatching = flags.indexOf('u') >= 0;
        String newFlags = flags.indexOf('y') >= 0 ? flags : flags + "y";
        JsObject splitter = Ops.construct(c, new JsValue[] { rx, Js.str(newFlags) });
        JsArray a = r.newArray();
        long lengthA = 0;
        long lim = limit.isUndefined() ? 4294967295L : Ops.toUint32(limit);
        int size = s.length();
        if (lim == 0) {
            return a;
        }
        if (size == 0) {
            JsValue z = regExpExec(r, splitter, s);
            if (!z.isNull()) {
                return a;
            }
            a.createDataPropertyOrThrow("0", Js.str(s));
            return a;
        }
        int p = 0;
        double q = p;
        // 위치 q 에서 고정(sticky) 일치를 시도하며 구분자를 찾는다. 일치하면 p..q 를 조각으로 잘라 낸다.
        while (q < size) {
            splitter.setStrict("lastIndex", Js.num(q));
            JsValue z = regExpExec(r, splitter, s);
            if (z.isNull()) {
                q = advance(s, q, unicodeMatching);
            } else {
                double e = Math.min(Ops.toLength(splitter.get("lastIndex")), size);
                if (e == p) {
                    q = advance(s, q, unicodeMatching);
                } else {
                    a.createDataPropertyOrThrow(String.valueOf(lengthA), Js.str(s.substring(p, (int) q)));
                    lengthA++;
                    if (lengthA == lim) {
                        return a;
                    }
                    p = (int) e;
                    double numberOfCaptures = Math.max(Ops.toLength(((JsObject) z).get("length")) - 1, 0);
                    // 구분자의 캡처도 조각으로 이어 붙인다.
                    for (double i = 1; i <= numberOfCaptures; i++) {
                        a.createDataPropertyOrThrow(String.valueOf(lengthA), ((JsObject) z).get(String.valueOf((long) i)));
                        lengthA++;
                        if (lengthA == lim) {
                            return a;
                        }
                    }
                    q = p;
                }
            }
        }
        a.createDataPropertyOrThrow(String.valueOf(lengthA), Js.str(s.substring(p, size)));
        return a;
    }
}
