package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import java.text.Collator;
import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsStringObject;
import org.duckdns.hjow.swingwebpanel.js.JsSymbol;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.datajson.DataAccess;
import org.duckdns.hjow.swingwebpanel.js.text.CodePoints;
import org.duckdns.hjow.swingwebpanel.js.text.TextRangeException;
import org.duckdns.hjow.swingwebpanel.js.text.TextTransform;

/**
 * The String built-in (ECMA-262 6th edition 21.1): the constructor, <code>fromCharCode</code>, <code>fromCodePoint</code>, <code>raw</code>,
 * all prototype methods, the iterator and the Annex B HTML methods. Methods that take a pattern (<code>match</code>, <code>replace</code>,
 * <code>search</code>, <code>split</code>) delegate to the <code>@@match</code>, <code>@@replace</code>, <code>@@search</code>,
 * <code>@@split</code> methods of the argument when it has them, so they work with the RegExp built-in or any other object that implements the
 * protocol; for a plain string argument they work on the string itself.
 * <p>
 * <code>localeCompare</code> normalizes both operands with Unicode 18 NFC before comparing them using the JRE root collator (canonical
 * decomposition, tertiary strength). <code>toLocaleLowerCase/UpperCase</code> use the default Unicode case conversions. Extensions
 * beyond ES2015 that real pages use are installed by {@link #installExtensions(JsRealm, JsObject)}: <code>trimStart</code>, <code>trimEnd</code>,
 * <code>trimLeft</code>, <code>trimRight</code>, <code>padStart</code>, <code>padEnd</code>.
 * <p>
 * String 내장 객체(ECMA-262 6판 21.1): 생성자, <code>fromCharCode</code>, <code>fromCodePoint</code>, <code>raw</code>, 모든 프로토타입 메서드,
 * 이터레이터, 부속서 B 의 HTML 메서드. 패턴을 받는 메서드(<code>match</code>, <code>replace</code>, <code>search</code>, <code>split</code>)는
 * 인수가 <code>@@match</code>, <code>@@replace</code>, <code>@@search</code>, <code>@@split</code> 메서드를 가지면 거기에 위임하므로 RegExp 내장 객체나
 * 같은 프로토콜을 구현한 다른 객체와 함께 동작하고, 단순 문자열 인수이면 문자열 자체를 대상으로 동작한다.
 * <p>
 * <code>localeCompare</code> 는 두 값을 Unicode 18 NFC로 정규화한 뒤 JRE 루트 로케일 콜레이터(정준 분해, 제3 강도)로 비교한다.
 * <code>toLocaleLowerCase/UpperCase</code> 는 Unicode 기본 대소문자 변환을 쓴다. 실제 페이지가 쓰는 ES2015 이후 확장은
 * {@link #installExtensions(JsRealm, JsObject)} 가 설치한다: <code>trimStart</code>, <code>trimEnd</code>, <code>trimLeft</code>,
 * <code>trimRight</code>, <code>padStart</code>, <code>padEnd</code>.
 */
final class StringBuiltins {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private StringBuiltins() {
    }

    /**
     * Installs the String constructor, its static functions and the prototype.
     * <p>
     * String 생성자, 정적 함수, 프로토타입을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject proto = realm.getStringPrototype();
        NativeFunction string = new NativeFunction(realm, "String", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                if (args.length == 0) {
                    return JsString.EMPTY;
                }
                JsValue v = args[0];
                if (v instanceof JsSymbol) {
                    return Js.str(symbolDescriptiveString((JsSymbol) v));
                }
                return Ops.toStringValue(v);
            }
        }, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                String s = args.length == 0 ? "" : Ops.toString(args[0]);
                JsObject p = Ops.getPrototypeFromConstructor(r, newTarget, r.getStringPrototype());
                return new JsStringObject(p, s);
            }
        });
        string.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", string);
        realm.getGlobalObject().defineBuiltin("String", string);
        realm.setIntrinsic("%String%", string);
        installStatics(realm, string);
        installAccess(realm, proto);
        installSearch(realm, proto);
        installPattern(realm, proto);
        installTransform(realm, proto);
        installHtml(realm, proto);
        installExtensions(realm, proto);
        installIterator(realm, proto);
    }

    /**
     * SymbolDescriptiveString: the text <code>Symbol(description)</code>.
     * <p>
     * SymbolDescriptiveString: <code>Symbol(설명)</code> 형태의 문자열.
     *
     * @param s the symbol / 심볼
     * @return the text / 문자열
     */
    private static String symbolDescriptiveString(JsSymbol s) {
        String d = s.getDescription();
        return "Symbol(" + (d == null ? "" : d) + ")";
    }

    /**
     * RequireObjectCoercible(this) followed by ToString, the first steps of most prototype methods.
     * <p>
     * RequireObjectCoercible(this) 와 ToString, 대부분의 프로토타입 메서드의 첫 단계.
     *
     * @param thisValue the this value / this 값
     * @param method    method name for the error message / 오류 메시지용 메서드 이름
     * @return the string / 문자열
     */
    private static String thisString(JsValue thisValue, String method) {
        if (thisValue.isNullOrUndefined()) {
            throw JsException.typeError("String.prototype." + method + " called on null or undefined");
        }
        return Ops.toString(thisValue);
    }

    /**
     * thisStringValue: the string of a String primitive or String object; any other value is a TypeError.
     * <p>
     * thisStringValue: String 원시 값이나 String 객체의 문자열. 다른 값이면 TypeError.
     *
     * @param v      the this value / this 값
     * @param method method name for the error message / 오류 메시지용 메서드 이름
     * @return the string value / 문자열 값
     */
    private static JsValue thisStringValue(JsValue v, String method) {
        if (v instanceof JsString) {
            return v;
        }
        if (v instanceof JsStringObject) {
            return Js.str(((JsStringObject) v).getStringValue());
        }
        throw JsException.typeError("String.prototype." + method + " requires that 'this' be a String");
    }

    /**
     * IsRegExp (7.2.8): true for objects whose <code>@@match</code> property is truthy, or that are RegExp objects.
     * <p>
     * IsRegExp(7.2.8): <code>@@match</code> 프로퍼티가 참인 객체이거나 RegExp 객체이면 true.
     *
     * @param v the value / 값
     * @return true if it is a regular expression / 정규식이면 true
     */
    private static boolean isRegExp(JsValue v) {
        if (!(v instanceof JsObject)) {
            return false;
        }
        JsObject o = (JsObject) v;
        JsValue matcher = o.get(WellKnownSymbols.MATCH);
        if (!matcher.isUndefined()) {
            return Ops.toBoolean(matcher);
        }
        return DataAccess.isRegExp(o);
    }

    /**
     * Requires global matching when the string operation receives an IsRegExp argument.
     * <p>
     * 문자열 연산의 인수가 IsRegExp 검사에 해당하면 전역 일치를 요구한다.
     *
     * @param value proposed pattern / 패턴 인수
     * @param method string method name / 문자열 메서드 이름
     */
    private static void requireGlobalRegExp(JsValue value, String method) {
        if (isRegExp(value)) {
            JsValue flags = Ops.requireObjectCoercible(((JsObject) value).get("flags"));
            if (Ops.toString(flags).indexOf('g') < 0) {
                throw JsException.typeError("String.prototype." + method + " called with a non-global RegExp argument");
            }
        }
    }

    /**
     * Converts the searchString argument of includes/startsWith/endsWith, rejecting regular expressions.
     * <p>
     * includes/startsWith/endsWith 의 searchString 인수를 변환하며 정규식은 거부한다.
     *
     * @param v      the argument / 인수
     * @param method method name for the message / 메시지용 메서드 이름
     * @return the search string / 검색 문자열
     */
    private static String searchString(JsValue v, String method) {
        if (isRegExp(v)) {
            throw JsException.typeError("First argument to String.prototype." + method + " must not be a regular expression");
        }
        return Ops.toString(v);
    }

    /**
     * Clamps a position argument to 0..len.
     * <p>
     * 위치 인수를 0..len 으로 제한한다.
     *
     * @param pos the ToInteger'd value / ToInteger 한 값
     * @param len the length / 길이
     * @return the clamped position / 제한한 위치
     */
    private static int clamp(double pos, int len) {
        return (int) Math.min(Math.max(pos, 0), len);
    }

    /**
     * Installs fromCharCode, fromCodePoint and raw.
     * <p>
     * fromCharCode, fromCodePoint, raw 를 설치한다.
     *
     * @param realm  the realm / realm
     * @param string the String constructor / String 생성자
     */
    private static void installStatics(JsRealm realm, JsObject string) {
        Support.def(realm, string, "fromCharCode", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                char[] units = new char[args.length];
                // 각 인수를 ToUint16 한 코드 단위로 모은다.
                for (int i = 0; i < args.length; i++) {
                    units[i] = (char) Ops.toUint16(args[i]);
                }
                return Js.str(new String(units));
            }
        });
        Support.def(realm, string, "fromCodePoint", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                StringBuilder sb = new StringBuilder();
                // 각 인수가 0..0x10FFFF 의 정수인지 확인하며 코드 포인트를 덧붙인다.
                for (int i = 0; i < args.length; i++) {
                    double next = Ops.toNumber(args[i]);
                    if (next != Ops.toInteger(next) || next < 0 || next > 0x10FFFF || (next != next)) {
                        throw JsException.rangeError("Invalid code point " + Ops.toString(args[i]));
                    }
                    sb.appendCodePoint((int) next);
                    Support.checkStringLength(sb.length());
                }
                return Js.str(sb.toString());
            }
        });
        Support.def(realm, string, "raw", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsObject cooked = Ops.toObject(r, Js.arg(args, 0));
                JsObject raw = Ops.toObject(r, cooked.get("raw"));
                double literalSegments = Ops.lengthOf(raw);
                if (literalSegments <= 0) {
                    return JsString.EMPTY;
                }
                StringBuilder sb = new StringBuilder();
                double next = 0;
                // 원본 조각과 대체 값을 번갈아 덧붙인다.
                while (true) {
                    sb.append(Ops.toString(Support.getAt(raw, next)));
                    Support.checkStringLength(sb.length());
                    if (next + 1 == literalSegments) {
                        return Js.str(sb.toString());
                    }
                    if (next + 1 < args.length) {
                        sb.append(Ops.toString(args[(int) next + 1]));
                    }
                    next++;
                }
            }
        });
    }

    /**
     * Installs the character access methods: charAt, charCodeAt, codePointAt, concat, slice, substring, substr, toString, valueOf.
     * <p>
     * 문자 접근 메서드를 설치한다: charAt, charCodeAt, codePointAt, concat, slice, substring, substr, toString, valueOf.
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    private static void installAccess(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "charAt", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "charAt");
                double pos = Ops.toInteger(Js.arg(args, 0));
                if (pos < 0 || pos >= s.length()) {
                    return JsString.EMPTY;
                }
                return Js.str(String.valueOf(s.charAt((int) pos)));
            }
        });
        Support.def(realm, proto, "at", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "at");
                double relative = Ops.toInteger(Js.arg(args, 0));
                double index = relative < 0 ? s.length() + relative : relative;
                return index < 0 || index >= s.length() ? Js.UNDEFINED : Js.str(String.valueOf(s.charAt((int) index)));
            }
        });
        Support.def(realm, proto, "charCodeAt", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "charCodeAt");
                double pos = Ops.toInteger(Js.arg(args, 0));
                if (pos < 0 || pos >= s.length()) {
                    return Js.num(Double.NaN);
                }
                return Js.num(s.charAt((int) pos));
            }
        });
        Support.def(realm, proto, "codePointAt", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "codePointAt");
                double pos = Ops.toInteger(Js.arg(args, 0));
                if (pos < 0 || pos >= s.length()) {
                    return Js.UNDEFINED;
                }
                int p = (int) pos;
                char first = s.charAt(p);
                // 앞 서로게이트 뒤에 뒤 서로게이트가 오면 한 코드 포인트로 합친다.
                if (Character.isHighSurrogate(first) && p + 1 < s.length() && Character.isLowSurrogate(s.charAt(p + 1))) {
                    return Js.num(Character.toCodePoint(first, s.charAt(p + 1)));
                }
                return Js.num(first);
            }
        });
        Support.def(realm, proto, "concat", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "concat");
                StringBuilder sb = new StringBuilder(s);
                // 인수를 차례로 문자열로 바꿔 이어 붙인다.
                for (int i = 0; i < args.length; i++) {
                    sb.append(Ops.toString(args[i]));
                    Support.checkStringLength(sb.length());
                }
                return Js.str(sb.toString());
            }
        });
        Support.def(realm, proto, "slice", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "slice");
                int len = s.length();
                int from = (int) Support.relative(Js.arg(args, 0), len, 0);
                int to = (int) Support.relative(Js.arg(args, 1), len, len);
                return Js.str(from < to ? s.substring(from, to) : "");
            }
        });
        Support.def(realm, proto, "substring", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "substring");
                int len = s.length();
                int start = clamp(Ops.toInteger(Js.arg(args, 0)), len);
                JsValue endArg = Js.arg(args, 1);
                int end = endArg.isUndefined() ? len : clamp(Ops.toInteger(endArg), len);
                return Js.str(s.substring(Math.min(start, end), Math.max(start, end)));
            }
        });
        Support.def(realm, proto, "substr", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "substr");
                int size = s.length();
                double start = Ops.toInteger(Js.arg(args, 0));
                if (start == Double.NEGATIVE_INFINITY) {
                    start = 0;
                } else if (start < 0) {
                    start = Math.max(size + start, 0);
                } else {
                    start = Math.min(start, size);
                }
                JsValue lengthArg = Js.arg(args, 1);
                double length = lengthArg.isUndefined() ? Double.POSITIVE_INFINITY : Ops.toInteger(lengthArg);
                length = Math.min(Math.max(length, 0), size);
                double end = Math.min(start + length, size);
                if (start >= end) {
                    return JsString.EMPTY;
                }
                return Js.str(s.substring((int) start, (int) end));
            }
        });
        Support.def(realm, proto, "toString", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisStringValue(thisValue, "toString");
            }
        });
        Support.def(realm, proto, "valueOf", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return thisStringValue(thisValue, "valueOf");
            }
        });
    }

    /**
     * Installs the plain string search methods: includes, startsWith, endsWith, indexOf, lastIndexOf, localeCompare.
     * <p>
     * 일반 문자열 검색 메서드를 설치한다: includes, startsWith, endsWith, indexOf, lastIndexOf, localeCompare.
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    private static void installSearch(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "includes", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "includes");
                String search = searchString(Js.arg(args, 0), "includes");
                int start = clamp(Ops.toInteger(Js.arg(args, 1)), s.length());
                return Js.bool(s.indexOf(search, start) >= 0);
            }
        });
        Support.def(realm, proto, "startsWith", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "startsWith");
                String search = searchString(Js.arg(args, 0), "startsWith");
                int start = clamp(Ops.toInteger(Js.arg(args, 1)), s.length());
                return Js.bool(s.startsWith(search, start));
            }
        });
        Support.def(realm, proto, "endsWith", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "endsWith");
                String search = searchString(Js.arg(args, 0), "endsWith");
                JsValue endArg = Js.arg(args, 1);
                int end = endArg.isUndefined() ? s.length() : clamp(Ops.toInteger(endArg), s.length());
                int start = end - search.length();
                return Js.bool(start >= 0 && s.startsWith(search, start));
            }
        });
        Support.def(realm, proto, "indexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "indexOf");
                String search = Ops.toString(Js.arg(args, 0));
                int start = clamp(Ops.toInteger(Js.arg(args, 1)), s.length());
                return Js.num(s.indexOf(search, start));
            }
        });
        Support.def(realm, proto, "lastIndexOf", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "lastIndexOf");
                String search = Ops.toString(Js.arg(args, 0));
                double numPos = Ops.toNumber(Js.arg(args, 1));
                double pos = numPos != numPos ? Double.POSITIVE_INFINITY : Ops.toInteger(numPos);
                int start = clamp(pos, s.length());
                return Js.num(s.lastIndexOf(search, start));
            }
        });
        Support.def(realm, proto, "localeCompare", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "localeCompare");
                String that = Ops.toString(Js.arg(args, 0));
                try {
                    s = TextTransform.normalize(s, null);
                    that = TextTransform.normalize(that, null);
                } catch (TextRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
                Collator c = Collator.getInstance(Locale.ROOT);
                c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
                c.setStrength(Collator.TERTIARY);
                int cmp = c.compare(s, that);
                return Js.num(cmp < 0 ? -1 : (cmp > 0 ? 1 : 0));
            }
        });
    }

    /**
     * Installs match, replace, search and split.
     * <p>
     * match, replace, search, split 을 설치한다.
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    private static void installPattern(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "match", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue regexp = Js.arg(args, 0);
                if (!regexp.isNullOrUndefined()) {
                    JsValue matcher = Ops.getMethod(r, regexp, WellKnownSymbols.MATCH);
                    if (!matcher.isUndefined()) {
                        return Ops.call(matcher, regexp, new JsValue[] { o });
                    }
                }
                String s = Ops.toString(o);
                JsValue rx = regExpCreate(r, regexp);
                return Ops.invoke(r, rx, WellKnownSymbols.MATCH, new JsValue[] { Js.str(s) });
            }
        });
        Support.def(realm, proto, "matchAll", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue regexp = Js.arg(args, 0);
                // 전역 검사 후에만 Symbol.matchAll을 얻는다.
                requireGlobalRegExp(regexp, "matchAll");
                if (!regexp.isNullOrUndefined()) {
                    JsValue matcher = Ops.getMethod(r, regexp, WellKnownSymbols.MATCH_ALL);
                    if (!matcher.isUndefined()) {
                        return Ops.call(matcher, regexp, new JsValue[] { o });
                    }
                }
                JsValue ctor = r.getIntrinsic("%RegExp%");
                JsValue rx = Ops.construct(ctor, new JsValue[] { regexp, Js.str("g") });
                return Ops.invoke(r, rx, WellKnownSymbols.MATCH_ALL, new JsValue[] { Js.str(Ops.toString(o)) });
            }
        });
        Support.def(realm, proto, "search", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue regexp = Js.arg(args, 0);
                if (!regexp.isNullOrUndefined()) {
                    JsValue searcher = Ops.getMethod(r, regexp, WellKnownSymbols.SEARCH);
                    if (!searcher.isUndefined()) {
                        return Ops.call(searcher, regexp, new JsValue[] { o });
                    }
                }
                String s = Ops.toString(o);
                JsValue rx = regExpCreate(r, regexp);
                return Ops.invoke(r, rx, WellKnownSymbols.SEARCH, new JsValue[] { Js.str(s) });
            }
        });
        Support.def(realm, proto, "replace", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue searchValue = Js.arg(args, 0);
                JsValue replaceValue = Js.arg(args, 1);
                if (!searchValue.isNullOrUndefined()) {
                    JsValue replacer = Ops.getMethod(r, searchValue, WellKnownSymbols.REPLACE);
                    if (!replacer.isUndefined()) {
                        return Ops.call(replacer, searchValue, new JsValue[] { o, replaceValue });
                    }
                }
                String string = Ops.toString(o);
                String searchString = Ops.toString(searchValue);
                boolean functional = replaceValue.isCallable();
                String replacement = functional ? null : Ops.toString(replaceValue);
                int pos = string.indexOf(searchString);
                if (pos < 0) {
                    return Js.str(string);
                }
                String replStr;
                if (functional) {
                    JsValue v = Ops.call(replaceValue, Js.UNDEFINED, new JsValue[] { Js.str(searchString), Js.num(pos), Js.str(string) });
                    replStr = Ops.toString(v);
                } else {
                    replStr = Substitution.expand(searchString, string, pos, Js.NO_ARGS, replacement);
                }
                Support.checkStringLength((double) string.length() - searchString.length() + replStr.length());
                return Js.str(string.substring(0, pos) + replStr + string.substring(pos + searchString.length()));
            }
        });
        Support.def(realm, proto, "replaceAll", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue searchValue = Js.arg(args, 0);
                JsValue replaceValue = Js.arg(args, 1);
                requireGlobalRegExp(searchValue, "replaceAll");
                if (!searchValue.isNullOrUndefined()) {
                    JsValue replacer = Ops.getMethod(r, searchValue, WellKnownSymbols.REPLACE);
                    if (!replacer.isUndefined()) {
                        return Ops.call(replacer, searchValue, new JsValue[] { o, replaceValue });
                    }
                }
                String string = Ops.toString(o);
                String search = Ops.toString(searchValue);
                boolean functional = replaceValue.isCallable();
                String replacement = functional ? null : Ops.toString(replaceValue);
                StringBuilder out = new StringBuilder();
                if (search.length() == 0) {
                    // 빈 검색어는 UTF-16 코드 단위 사이마다 치환한다.
                    for (int i = 0; i <= string.length(); i++) {
                        JsValue next = functional
                                ? Ops.call(replaceValue, Js.UNDEFINED, new JsValue[] { JsString.EMPTY, Js.num(i), Js.str(string) })
                                : Js.str(replacement);
                        out.append(functional ? Ops.toString(next)
                                : Substitution.expand("", string, i, Js.NO_ARGS, replacement));
                        if (i < string.length()) {
                            out.append(string.charAt(i));
                        }
                    }
                    Support.checkStringLength(out.length());
                    return Js.str(out.toString());
                }
                int from = 0;
                // 겹치지 않는 다음 일치 위치를 계속 찾아 전부 치환한다.
                while (true) {
                    int position = string.indexOf(search, from);
                    if (position < 0) {
                        break;
                    }
                    out.append(string, from, position);
                    JsValue replacementValue = functional
                            ? Ops.call(replaceValue, Js.UNDEFINED, new JsValue[] { Js.str(search), Js.num(position), Js.str(string) })
                            : Js.str(replacement);
                    String text = functional ? Ops.toString(replacementValue)
                            : Substitution.expand(search, string, position, Js.NO_ARGS, replacement);
                    out.append(text);
                    from = position + search.length();
                }
                out.append(string, from, string.length());
                Support.checkStringLength(out.length());
                return Js.str(out.toString());
            }
        });
        Support.def(realm, proto, "split", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                JsValue o = Ops.requireObjectCoercible(thisValue);
                JsValue separator = Js.arg(args, 0);
                JsValue limit = Js.arg(args, 1);
                if (!separator.isNullOrUndefined()) {
                    JsValue splitter = Ops.getMethod(r, separator, WellKnownSymbols.SPLIT);
                    if (!splitter.isUndefined()) {
                        return Ops.call(splitter, separator, new JsValue[] { o, limit });
                    }
                }
                String s = Ops.toString(o);
                JsArray a = r.newArray();
                long lim = limit.isUndefined() ? 4294967295L : Ops.toUint32(limit);
                String sep = Ops.toString(separator);
                if (lim == 0) {
                    return a;
                }
                if (separator.isUndefined()) {
                    a.add(Js.str(s));
                    return a;
                }
                return splitString(a, s, sep, lim);
            }
        });
    }

    /**
     * The string based part of split (separator without a <code>@@split</code> method): splits at every occurrence of a non-empty separator,
     * or into code units for the empty separator, and stops after <code>lim</code> elements.
     * <p>
     * split 의 문자열 기반 부분(<code>@@split</code> 메서드가 없는 구분자): 비어 있지 않은 구분자는 나타날 때마다, 빈 구분자는 코드 단위마다 나누며
     * <code>lim</code> 개가 되면 멈춘다.
     *
     * @param a   result array / 결과 배열
     * @param s   the string to split / 나눌 문자열
     * @param sep the separator / 구분자
     * @param lim maximum number of elements / 최대 요소 수
     * @return the array / 배열
     */
    private static JsValue splitString(JsArray a, String s, String sep, long lim) {
        int size = s.length();
        if (size == 0) {
            if (sep.length() != 0) {
                a.add(Js.str(s));
            }
            return a;
        }
        if (sep.length() == 0) {
            // 빈 구분자이면 코드 단위마다 한 요소로 나눈다.
            for (int i = 0; i < size && i < lim; i++) {
                a.add(Js.str(String.valueOf(s.charAt(i))));
            }
            return a;
        }
        int p = 0;
        // 구분자가 나타나는 곳마다 앞부분을 요소로 잘라 낸다.
        while (true) {
            int idx = s.indexOf(sep, p);
            if (idx < 0) {
                break;
            }
            a.add(Js.str(s.substring(p, idx)));
            if (a.getLength() == lim) {
                return a;
            }
            p = idx + sep.length();
        }
        a.add(Js.str(s.substring(p)));
        return a;
    }

    /**
     * RegExpCreate(P, undefined) for match and search: constructs a regular expression object from the argument with the realm's RegExp
     * constructor (intrinsic <code>%RegExp%</code> or the global <code>RegExp</code>).
     * <p>
     * match 와 search 를 위한 RegExpCreate(P, undefined): realm 의 RegExp 생성자(내장 객체 <code>%RegExp%</code> 또는 전역 <code>RegExp</code>)로
     * 인수에서 정규식 객체를 만든다.
     *
     * @param realm   the realm / realm
     * @param pattern pattern argument (undefined means empty) / 패턴 인수(undefined 이면 빈 패턴)
     * @return the regular expression object / 정규식 객체
     */
    private static JsValue regExpCreate(JsRealm realm, JsValue pattern) {
        JsValue ctor = realm.getIntrinsic("%RegExp%");
        if (ctor == null) {
            ctor = realm.getGlobalObject().get("RegExp");
        }
        if (!Ops.isConstructor(ctor)) {
            throw JsException.typeError("RegExp is not available: String.prototype.match/search with a string argument needs the RegExp built-in");
        }
        return Ops.construct(ctor, new JsValue[] { pattern.isNull() ? Js.str("null") : pattern });
    }

    /**
     * Installs the conversions: normalize, repeat, trim and the case conversions.
     * <p>
     * 변환 메서드를 설치한다: normalize, repeat, trim, 대소문자 변환.
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    private static void installTransform(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "normalize", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "normalize");
                JsValue form = Js.arg(args, 0);
                try {
                    return Js.str(TextTransform.normalize(s, form.isUndefined() ? null : Ops.toString(form)));
                } catch (TextRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
            }
        });
        Support.def(realm, proto, "repeat", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String s = thisString(thisValue, "repeat");
                double n = Ops.toInteger(Js.arg(args, 0));
                if (n < 0 || Double.isInfinite(n)) {
                    throw JsException.rangeError("Invalid count value");
                }
                Support.checkStringLength(n * s.length());
                try {
                    return Js.str(TextTransform.repeat(s, n));
                } catch (TextRangeException e) {
                    throw JsException.rangeError(e.getMessage());
                }
            }
        });
        Support.def(realm, proto, "trim", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(Support.trim(thisString(thisValue, "trim"), true, true));
            }
        });
        Support.def(realm, proto, "toLowerCase", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(TextTransform.toLowerCase(thisString(thisValue, "toLowerCase")));
            }
        });
        Support.def(realm, proto, "toLocaleLowerCase", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(TextTransform.toLowerCase(thisString(thisValue, "toLocaleLowerCase")));
            }
        });
        Support.def(realm, proto, "toUpperCase", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(TextTransform.toUpperCase(thisString(thisValue, "toUpperCase")));
            }
        });
        Support.def(realm, proto, "toLocaleUpperCase", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(TextTransform.toUpperCase(thisString(thisValue, "toLocaleUpperCase")));
            }
        });
    }

    /**
     * Builds the markup of an Annex B HTML method (CreateHTML, B.2.3.2).
     * <p>
     * 부속서 B HTML 메서드의 마크업을 만든다(CreateHTML, B.2.3.2).
     *
     * @param thisValue the this value / this 값
     * @param method    method name / 메서드 이름
     * @param tag       element name / 요소 이름
     * @param attribute attribute name or null / 속성 이름 또는 null
     * @param value     attribute value argument / 속성 값 인수
     * @return the markup / 마크업
     */
    private static JsValue createHtml(JsValue thisValue, String method, String tag, String attribute, JsValue value) {
        String s = thisString(thisValue, method);
        StringBuilder sb = new StringBuilder("<").append(tag);
        if (attribute != null) {
            String v = Ops.toString(value).replace("\"", "&quot;");
            sb.append(' ').append(attribute).append("=\"").append(v).append('"');
        }
        sb.append('>').append(s).append("</").append(tag).append('>');
        Support.checkStringLength(sb.length());
        return Js.str(sb.toString());
    }

    /**
     * Installs the Annex B HTML methods (anchor, big, blink, bold, fixed, fontcolor, fontsize, italics, link, small, strike, sub, sup).
     * <p>
     * 부속서 B 의 HTML 메서드를 설치한다(anchor, big, blink, bold, fixed, fontcolor, fontsize, italics, link, small, strike, sub, sup).
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    private static void installHtml(JsRealm realm, JsObject proto) {
        // 이름, 요소, 속성, 인수 개수를 표로 두고 같은 모양의 메서드를 한꺼번에 만든다.
        final String[][] table = { { "anchor", "a", "name", "1" }, { "big", "big", null, "0" }, { "blink", "blink", null, "0" },
            { "bold", "b", null, "0" }, { "fixed", "tt", null, "0" }, { "fontcolor", "font", "color", "1" },
            { "fontsize", "font", "size", "1" }, { "italics", "i", null, "0" }, { "link", "a", "href", "1" },
            { "small", "small", null, "0" }, { "strike", "strike", null, "0" }, { "sub", "sub", null, "0" }, { "sup", "sup", null, "0" } };
        // 표의 줄마다 메서드 하나를 만든다.
        for (int i = 0; i < table.length; i++) {
            final String method = table[i][0];
            final String tag = table[i][1];
            final String attribute = table[i][2];
            int length = Integer.parseInt(table[i][3]);
            Support.def(realm, proto, method, length, new NativeFunction.Call() {
                public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                    return createHtml(thisValue, method, tag, attribute, Js.arg(args, 0));
                }
            });
        }
    }

    /**
     * Installs the ES2016 and later string methods that real pages use: trimStart, trimEnd (and their aliases trimLeft, trimRight that are the
     * same function objects), padStart and padEnd. They are extensions, not part of ES2015.
     * <p>
     * 실제 페이지가 쓰는 ES2016 이후 문자열 메서드를 설치한다: trimStart, trimEnd(와 같은 함수 객체인 별칭 trimLeft, trimRight), padStart, padEnd.
     * ES2015 가 아닌 확장이다.
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    static void installExtensions(JsRealm realm, JsObject proto) {
        Support.def(realm, proto, "isWellFormed", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.bool(CodePoints.isWellFormed(thisString(thisValue, "isWellFormed")));
            }
        });
        Support.def(realm, proto, "toWellFormed", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(CodePoints.toWellFormed(thisString(thisValue, "toWellFormed")));
            }
        });
        NativeFunction trimStart = Support.def(realm, proto, "trimStart", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(Support.trim(thisString(thisValue, "trimStart"), true, false));
            }
        });
        NativeFunction trimEnd = Support.def(realm, proto, "trimEnd", 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return Js.str(Support.trim(thisString(thisValue, "trimEnd"), false, true));
            }
        });
        proto.defineBuiltin("trimLeft", trimStart);
        proto.defineBuiltin("trimRight", trimEnd);
        Support.def(realm, proto, "padStart", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return pad(thisString(thisValue, "padStart"), args, true);
            }
        });
        Support.def(realm, proto, "padEnd", 1, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return pad(thisString(thisValue, "padEnd"), args, false);
            }
        });
    }

    /**
     * StringPad (padStart and padEnd).
     * <p>
     * StringPad(padStart 와 padEnd).
     *
     * @param s     the string / 문자열
     * @param args  the arguments (maxLength, fillString) / 인수(maxLength, fillString)
     * @param start true to pad at the start / 앞쪽을 채우려면 true
     * @return the padded string / 채운 문자열
     */
    private static JsValue pad(String s, JsValue[] args, boolean start) {
        double maxLength = Ops.toLength(Js.arg(args, 0));
        if (maxLength <= s.length()) {
            return Js.str(s);
        }
        JsValue fillArg = Js.arg(args, 1);
        String fill = fillArg.isUndefined() ? " " : Ops.toString(fillArg);
        if (fill.length() == 0) {
            return Js.str(s);
        }
        Support.checkStringLength(maxLength);
        int fillLen = (int) maxLength - s.length();
        StringBuilder sb = new StringBuilder((int) maxLength);
        // 채울 길이가 될 때까지 채움 문자열을 되풀이해 붙인다.
        while (sb.length() < fillLen) {
            sb.append(fill);
        }
        sb.setLength(fillLen);
        return Js.str(start ? sb.toString() + s : s + sb.toString());
    }

    /**
     * Installs <code>String.prototype[@@iterator]</code>.
     * <p>
     * <code>String.prototype[@@iterator]</code> 를 설치한다.
     *
     * @param realm the realm / realm
     * @param proto String.prototype / String.prototype
     */
    private static void installIterator(JsRealm realm, JsObject proto) {
        StringIteratorObject.install(realm);
        Support.def(realm, proto, WellKnownSymbols.ITERATOR, 0, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return StringIteratorObject.create(r, thisString(thisValue, "[Symbol.iterator]"));
            }
        });
    }
}
