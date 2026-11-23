package org.duckdns.hjow.swingwebpanel.js.builtins.datajson;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsNumber;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsPrimitiveObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsStringObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.num.NumberConversion;
import org.duckdns.hjow.swingwebpanel.js.text.JsonSyntaxException;
import org.duckdns.hjow.swingwebpanel.js.text.JsonText;
import org.duckdns.hjow.swingwebpanel.js.text.JsonTokenizer;

/**
 * The built-in <code>JSON</code> object (ECMA-262 6th edition 24.3): <code>JSON.parse</code> with reviver and <code>JSON.stringify</code> with
 * replacer function, replacer property list, indentation, <code>toJSON</code>, wrapper object unboxing and cycle detection. Text scanning
 * comes from {@link JsonTokenizer} and string quoting from {@link JsonText}; neither <code>eval</code> nor Java serialization is used.
 * <p>
 * Design notes: parsing is iterative, so any nesting depth of the input text works; creating objects uses CreateDataProperty, so a key named
 * <code>__proto__</code> becomes an ordinary own property. Serializing and reviving walk the value recursively and stop with a
 * <code>RangeError</code> deeper than {@link #MAX_DEPTH} levels (a Java stack overflow would otherwise kill the engine).
 * <p>
 * 내장 <code>JSON</code> 객체(ECMA-262 6판 24.3): reviver 가 있는 <code>JSON.parse</code> 와 replacer 함수, replacer 속성 목록, 들여쓰기,
 * <code>toJSON</code>, 래퍼 객체 풀기, 순환 검출이 있는 <code>JSON.stringify</code>. 텍스트 읽기는 {@link JsonTokenizer}, 문자열 인용은 {@link JsonText} 를 쓰며
 * <code>eval</code> 도 Java 직렬화도 쓰지 않는다.
 * <p>
 * 설계 메모: 파싱은 반복 방식이라 입력 텍스트의 중첩 깊이에 제한이 없고, 객체는 CreateDataProperty 로 만들므로 <code>__proto__</code> 라는 키도 평범한 자체
 * 프로퍼티가 된다. 직렬화와 reviver 는 값을 재귀로 훑으며 {@link #MAX_DEPTH} 단계보다 깊으면 <code>RangeError</code> 로 멈춘다(Java 스택 오버플로가 엔진을
 * 죽이지 않도록).
 */
public final class JsonBuiltins {

    /**
     * Largest nesting depth of values that stringify and the reviver walk accept.
     * <p>
     * stringify 와 reviver 가 받아들이는 값 중첩의 최대 깊이.
     */
    public static final int MAX_DEPTH = 1000;

    /**
     * Parser state: a value is required.
     * <p>
     * 파서 상태: 값이 필요하다.
     */
    private static final int EXPECT_VALUE = 0;

    /**
     * Parser state: a value or the end of the array is required.
     * <p>
     * 파서 상태: 값 또는 배열의 끝이 필요하다.
     */
    private static final int EXPECT_VALUE_OR_END = 1;

    /**
     * Parser state: a key or the end of the object is required.
     * <p>
     * 파서 상태: 키 또는 객체의 끝이 필요하다.
     */
    private static final int EXPECT_KEY_OR_END = 2;

    /**
     * Parser state: a key is required.
     * <p>
     * 파서 상태: 키가 필요하다.
     */
    private static final int EXPECT_KEY = 3;

    /**
     * Parser state: a colon is required.
     * <p>
     * 파서 상태: 콜론이 필요하다.
     */
    private static final int EXPECT_COLON = 4;

    /**
     * Parser state: a comma or a closing bracket is required.
     * <p>
     * 파서 상태: 쉼표 또는 닫는 괄호가 필요하다.
     */
    private static final int AFTER_VALUE = 5;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private JsonBuiltins() {
    }

    /**
     * Installs <code>JSON</code> into a realm.
     * <p>
     * realm 에 <code>JSON</code> 을 설치한다.
     *
     * @param realm the realm / realm
     */
    static void install(JsRealm realm) {
        JsObject json = realm.newObject();
        realm.getGlobalObject().defineBuiltin("JSON", json);
        realm.setIntrinsic("%JSON%", json);
        Support.defineToStringTag(json, "JSON");
        realm.method(json, "parse", 2, new NativeFunction.Call() {
            /**
             * JSON.parse(text, reviver).
             * <p>
             * JSON.parse(text, reviver).
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the parsed value / 해석한 값
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                String text = Ops.toString(Js.arg(args, 0));
                JsValue unfiltered = parse(r, text);
                JsValue reviver = Js.arg(args, 1);
                if (reviver.isCallable()) {
                    JsObject root = r.newObject();
                    root.createDataProperty("", unfiltered);
                    return internalize(root, "", reviver, 0);
                }
                return unfiltered;
            }
        });
        realm.method(json, "stringify", 3, new NativeFunction.Call() {
            /**
             * JSON.stringify(value, replacer, space).
             * <p>
             * JSON.stringify(value, replacer, space).
             *
             * @param r         realm / realm
             * @param thisValue this / this
             * @param args      arguments / 인수
             * @return the JSON text or undefined / JSON 문자열 또는 undefined
             */
            public JsValue call(JsRealm r, JsValue thisValue, JsValue[] args) {
                return stringify(r, Js.arg(args, 0), Js.arg(args, 1), Js.arg(args, 2));
            }
        });
    }

    /**
     * Builds the SyntaxError message for an unexpected token or the end of the input.
     * <p>
     * 예상치 못한 토큰이나 입력 끝에 대한 SyntaxError 메시지를 만든다.
     *
     * @param text the JSON text / JSON 텍스트
     * @param at   offset of the problem / 문제 위치
     * @return the exception to throw / 던질 예외
     */
    private static JsException unexpected(String text, int at) {
        if (at >= text.length()) {
            return JsException.syntaxError("Unexpected end of JSON input");
        }
        return JsException.syntaxError("Unexpected token " + text.charAt(at) + " in JSON at position " + at);
    }

    /**
     * Parses JSON text into JavaScript values with an explicit stack (no recursion).
     * <p>
     * 명시적 스택으로(재귀 없이) JSON 텍스트를 JavaScript 값으로 해석한다.
     *
     * @param r    the realm / realm
     * @param text the JSON text / JSON 텍스트
     * @return the value / 값
     */
    static JsValue parse(JsRealm r, String text) {
        JsonTokenizer t = new JsonTokenizer(text);
        List<JsObject> containers = new ArrayList<JsObject>();
        List<String> keys = new ArrayList<String>();
        int state = EXPECT_VALUE;
        JsValue result = null;
        // 토큰을 하나씩 읽으며 상태 기계로 구조를 확인하고 값을 만든다. 컨테이너 스택은 열린 배열/객체를 가리킨다.
        while (true) {
            int tok;
            try {
                tok = t.next();
            } catch (JsonSyntaxException e) {
                throw JsException.syntaxError(e.getMessage());
            }
            if (tok == JsonTokenizer.EOF) {
                if (state == AFTER_VALUE && containers.isEmpty()) {
                    return result;
                }
                throw unexpected(text, text.length());
            }
            JsValue value = null;
            if (state == EXPECT_VALUE || state == EXPECT_VALUE_OR_END) {
                if (tok == JsonTokenizer.END_ARRAY && state == EXPECT_VALUE_OR_END) {
                    value = containers.remove(containers.size() - 1);
                } else if (tok == JsonTokenizer.BEGIN_ARRAY) {
                    containers.add(r.newArray());
                    state = EXPECT_VALUE_OR_END;
                } else if (tok == JsonTokenizer.BEGIN_OBJECT) {
                    containers.add(r.newObject());
                    keys.add(null);
                    state = EXPECT_KEY_OR_END;
                } else if (tok == JsonTokenizer.STRING) {
                    value = Js.str(t.getStringValue());
                } else if (tok == JsonTokenizer.NUMBER) {
                    value = Js.num(NumberConversion.stringToNumber(t.getNumberText()));
                } else if (tok == JsonTokenizer.TRUE) {
                    value = Js.TRUE;
                } else if (tok == JsonTokenizer.FALSE) {
                    value = Js.FALSE;
                } else if (tok == JsonTokenizer.NULL) {
                    value = Js.NULL;
                } else {
                    throw unexpected(text, t.getTokenStart());
                }
            } else if (state == EXPECT_KEY_OR_END || state == EXPECT_KEY) {
                if (tok == JsonTokenizer.STRING) {
                    keys.set(keys.size() - 1, t.getStringValue());
                    state = EXPECT_COLON;
                } else if (tok == JsonTokenizer.END_OBJECT && state == EXPECT_KEY_OR_END) {
                    keys.remove(keys.size() - 1);
                    value = containers.remove(containers.size() - 1);
                } else {
                    throw unexpected(text, t.getTokenStart());
                }
            } else if (state == EXPECT_COLON) {
                if (tok != JsonTokenizer.COLON) {
                    throw unexpected(text, t.getTokenStart());
                }
                state = EXPECT_VALUE;
            } else {
                if (containers.isEmpty()) {
                    throw unexpected(text, t.getTokenStart());
                }
                boolean inArray = containers.get(containers.size() - 1) instanceof JsArray;
                if (tok == JsonTokenizer.COMMA) {
                    state = inArray ? EXPECT_VALUE : EXPECT_KEY;
                } else if (inArray && tok == JsonTokenizer.END_ARRAY) {
                    value = containers.remove(containers.size() - 1);
                } else if (!inArray && tok == JsonTokenizer.END_OBJECT) {
                    keys.remove(keys.size() - 1);
                    value = containers.remove(containers.size() - 1);
                } else {
                    throw unexpected(text, t.getTokenStart());
                }
            }
            if (value != null) {
                // 완성된 값을 바깥 컨테이너에 넣거나, 최상위이면 결과로 삼는다.
                if (containers.isEmpty()) {
                    result = value;
                } else {
                    JsObject top = containers.get(containers.size() - 1);
                    if (top instanceof JsArray) {
                        ((JsArray) top).add(value);
                    } else {
                        top.createDataProperty(keys.get(keys.size() - 1), value);
                    }
                }
                state = AFTER_VALUE;
            }
        }
    }

    /**
     * InternalizeJSONProperty (ECMA-262 24.3.1.1): applies the reviver bottom-up.
     * <p>
     * InternalizeJSONProperty (ECMA-262 24.3.1.1): reviver 를 아래에서 위로 적용한다.
     *
     * @param holder  the object holding the property / 프로퍼티를 가진 객체
     * @param name    the property name / 프로퍼티 이름
     * @param reviver the reviver function / reviver 함수
     * @param depth   current depth / 현재 깊이
     * @return the revived value / 변환된 값
     */
    private static JsValue internalize(JsObject holder, String name, JsValue reviver, int depth) {
        if (depth > MAX_DEPTH) {
            throw JsException.rangeError("Maximum call stack size exceeded");
        }
        JsValue val = holder.get(name);
        if (val instanceof JsObject) {
            JsObject o = (JsObject) val;
            if (Ops.isArray(val)) {
                double len = Ops.toLength(o.get("length"));
                // 배열의 각 요소를 먼저 변환하고, undefined 이면 삭제한다.
                for (double i = 0; i < len; i++) {
                    String p = String.valueOf((long) i);
                    replace(o, p, internalize(o, p, reviver, depth + 1));
                }
            } else {
                List<String> keys = Ops.enumerableOwnKeys(o);
                // 객체의 열거 가능한 자체 키마다 같은 방식으로 변환한다.
                for (int i = 0; i < keys.size(); i++) {
                    String p = keys.get(i);
                    replace(o, p, internalize(o, p, reviver, depth + 1));
                }
            }
        }
        return Ops.call(reviver, holder, new JsValue[] { Js.str(name), val });
    }

    /**
     * Stores a revived element: undefined deletes the property, anything else is stored with CreateDataProperty. Failures are ignored.
     * <p>
     * 변환된 요소를 저장한다: undefined 이면 프로퍼티를 삭제하고, 아니면 CreateDataProperty 로 저장한다. 실패는 무시한다.
     *
     * @param o   the object / 객체
     * @param key the key / 키
     * @param v   the revived value / 변환된 값
     */
    private static void replace(JsObject o, String key, JsValue v) {
        if (v.isUndefined()) {
            o.delete(key);
        } else {
            o.createDataProperty(key, v);
        }
    }

    /**
     * The state of one JSON.stringify call.
     * <p>
     * JSON.stringify 호출 하나의 상태.
     */
    private static final class Serializer {
        /**
         * The realm this serialization belongs to.
         * <p>
         * 이 직렬화가 속한 realm.
         */
        private final JsRealm realm;

        /**
         * The replacer function or null.
         * <p>
         * replacer 함수 또는 null.
         */
        private final JsValue replacerFunction;

        /**
         * The property list built from an array replacer, or null.
         * <p>
         * 배열 replacer 로 만든 속성 목록. 없으면 null.
         */
        private final List<String> propertyList;

        /**
         * The indentation unit, empty for compact output.
         * <p>
         * 들여쓰기 단위. 압축 출력이면 빈 문자열.
         */
        private final String gap;

        /**
         * Current indentation.
         * <p>
         * 현재 들여쓰기.
         */
        private String indent = "";

        /**
         * Objects currently being serialized (for cycle detection).
         * <p>
         * 직렬화 중인 객체(순환 검출용).
         */
        private final List<JsObject> stack = new ArrayList<JsObject>();

        /**
         * Creates a serializer.
         * <p>
         * 직렬화기를 만든다.
         *
         * @param realm            the realm / realm
         * @param replacerFunction replacer function or null / replacer 함수 또는 null
         * @param propertyList     property list or null / 속성 목록 또는 null
         * @param gap              indentation unit / 들여쓰기 단위
         */
        Serializer(JsRealm realm, JsValue replacerFunction, List<String> propertyList, String gap) {
            this.realm = realm;
            this.replacerFunction = replacerFunction;
            this.propertyList = propertyList;
            this.gap = gap;
        }

        /**
         * SerializeJSONProperty (ECMA-262 24.3.2.1).
         * <p>
         * SerializeJSONProperty (ECMA-262 24.3.2.1).
         *
         * @param key    the property key / 프로퍼티 키
         * @param holder the object that holds it / 그것을 가진 객체
         * @return JSON text, or null for "undefined" / JSON 문자열. "undefined" 이면 null
         */
        String property(String key, JsObject holder) {
            JsValue value = holder.get(key);
            if (value instanceof JsObject) {
                JsValue toJson = ((JsObject) value).get("toJSON");
                if (toJson.isCallable()) {
                    value = Ops.call(toJson, value, new JsValue[] { Js.str(key) });
                }
            }
            if (replacerFunction != null) {
                value = Ops.call(replacerFunction, holder, new JsValue[] { Js.str(key), value });
            }
            if (value instanceof JsObject) {
                value = unwrap((JsObject) value);
            }
            if (value.isNull()) {
                return "null";
            }
            if (value == Js.TRUE) {
                return "true";
            }
            if (value == Js.FALSE) {
                return "false";
            }
            if (value instanceof JsString) {
                return JsonText.quote(((JsString) value).getValue());
            }
            if (value instanceof JsNumber) {
                double d = ((JsNumber) value).getValue();
                return Double.isNaN(d) || Double.isInfinite(d) ? "null" : Ops.toString(value);
            }
            if (value instanceof JsObject && !value.isCallable()) {
                return Ops.isArray(value) ? array((JsObject) value) : object((JsObject) value);
            }
            return null;
        }

        /**
         * Unwraps Number, String and Boolean wrapper objects.
         * <p>
         * Number, String, Boolean 래퍼 객체를 푼다.
         *
         * @param o the object / 객체
         * @return the primitive for wrappers, otherwise the object itself / 래퍼이면 원시 값, 아니면 객체 자신
         */
        private JsValue unwrap(JsObject o) {
            if (o instanceof JsStringObject) {
                return Js.str(Ops.toString(o));
            }
            if (o instanceof JsPrimitiveObject) {
                String tag = o.className();
                if ("Number".equals(tag)) {
                    return Js.num(Ops.toNumber(o));
                }
                if ("Boolean".equals(tag)) {
                    return ((JsPrimitiveObject) o).getPrimitive();
                }
            }
            return o;
        }

        /**
         * Registers a value as being serialized, rejecting cycles and excessive depth.
         * <p>
         * 값이 직렬화 중임을 등록하며 순환과 지나친 깊이를 거부한다.
         *
         * @param value the object / 객체
         */
        private void enter(JsObject value) {
            // 이미 직렬화 중인 객체이면 순환이다.
            for (int i = 0; i < stack.size(); i++) {
                if (stack.get(i) == value) {
                    throw JsException.typeError("Converting circular structure to JSON");
                }
            }
            if (stack.size() >= MAX_DEPTH) {
                throw JsException.rangeError("Maximum call stack size exceeded");
            }
            stack.add(value);
        }

        /**
         * SerializeJSONObject (ECMA-262 24.3.2.3).
         * <p>
         * SerializeJSONObject (ECMA-262 24.3.2.3).
         *
         * @param value the object / 객체
         * @return the JSON text / JSON 문자열
         */
        private String object(JsObject value) {
            enter(value);
            String stepback = indent;
            indent = indent + gap;
            List<String> keys = propertyList != null ? propertyList : Ops.enumerableOwnKeys(value);
            List<String> partial = new ArrayList<String>();
            // 키마다 값을 직렬화하고 undefined 가 아닌 것만 "키":값 으로 모은다.
            for (int i = 0; i < keys.size(); i++) {
                String strP = property(keys.get(i), value);
                if (strP != null) {
                    partial.add(JsonText.quote(keys.get(i)) + ":" + (gap.length() > 0 ? " " : "") + strP);
                }
            }
            String result;
            if (partial.isEmpty()) {
                result = "{}";
            } else if (gap.length() == 0) {
                result = "{" + join(partial, ",") + "}";
            } else {
                String separator = ",\n" + indent;
                result = "{\n" + indent + join(partial, separator) + "\n" + stepback + "}";
            }
            stack.remove(stack.size() - 1);
            indent = stepback;
            return result;
        }

        /**
         * SerializeJSONArray (ECMA-262 24.3.2.4).
         * <p>
         * SerializeJSONArray (ECMA-262 24.3.2.4).
         *
         * @param value the array / 배열
         * @return the JSON text / JSON 문자열
         */
        private String array(JsObject value) {
            enter(value);
            String stepback = indent;
            indent = indent + gap;
            List<String> partial = new ArrayList<String>();
            double len = Ops.toLength(value.get("length"));
            // 요소마다 직렬화하되 undefined 는 null 로 쓴다.
            for (double i = 0; i < len; i++) {
                String strP = property(String.valueOf((long) i), value);
                partial.add(strP == null ? "null" : strP);
            }
            String result;
            if (partial.isEmpty()) {
                result = "[]";
            } else if (gap.length() == 0) {
                result = "[" + join(partial, ",") + "]";
            } else {
                String separator = ",\n" + indent;
                result = "[\n" + indent + join(partial, separator) + "\n" + stepback + "]";
            }
            stack.remove(stack.size() - 1);
            indent = stepback;
            return result;
        }

        /**
         * Joins strings with a separator.
         * <p>
         * 구분자로 문자열을 잇는다.
         *
         * @param parts     the pieces / 조각
         * @param separator the separator / 구분자
         * @return the joined text / 이은 문자열
         */
        private static String join(List<String> parts, String separator) {
            StringBuilder sb = new StringBuilder();
            // 조각 사이에 구분자를 넣으며 이어 붙인다.
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    sb.append(separator);
                }
                sb.append(parts.get(i));
            }
            return sb.toString();
        }
    }

    /**
     * JSON.stringify (ECMA-262 24.3.2).
     * <p>
     * JSON.stringify (ECMA-262 24.3.2).
     *
     * @param r        the realm / realm
     * @param value    the value to serialize / 직렬화할 값
     * @param replacer replacer function, property list or anything else / replacer 함수, 속성 목록, 그 밖의 값
     * @param space    indentation argument / 들여쓰기 인수
     * @return JSON text or undefined / JSON 문자열 또는 undefined
     */
    static JsValue stringify(JsRealm r, JsValue value, JsValue replacer, JsValue space) {
        JsValue replacerFunction = null;
        List<String> propertyList = null;
        if (replacer instanceof JsObject) {
            if (replacer.isCallable()) {
                replacerFunction = replacer;
            } else if (Ops.isArray(replacer)) {
                propertyList = new ArrayList<String>();
                JsObject ro = (JsObject) replacer;
                double len = Ops.toLength(ro.get("length"));
                // 배열 replacer 의 각 요소 중 문자열, 숫자, 래퍼 객체만 중복 없이 속성 이름으로 모은다.
                for (double i = 0; i < len; i++) {
                    JsValue v = ro.get(String.valueOf((long) i));
                    String item = null;
                    if (v instanceof JsString) {
                        item = ((JsString) v).getValue();
                    } else if (v instanceof JsNumber) {
                        item = Ops.toString(v);
                    } else if (v instanceof JsStringObject || (v instanceof JsPrimitiveObject && "Number".equals(((JsObject) v).className()))) {
                        item = Ops.toString(v);
                    }
                    if (item != null && !propertyList.contains(item)) {
                        propertyList.add(item);
                    }
                }
            }
        }
        JsValue sp = space;
        if (sp instanceof JsStringObject) {
            sp = Js.str(Ops.toString(sp));
        } else if (sp instanceof JsPrimitiveObject && "Number".equals(((JsObject) sp).className())) {
            sp = Js.num(Ops.toNumber(sp));
        }
        String gap = "";
        if (sp instanceof JsNumber) {
            int n = (int) Math.min(10, Ops.toInteger(sp));
            StringBuilder sb = new StringBuilder();
            // 들여쓰기 칸 수만큼 공백을 만든다.
            for (int i = 0; i < n; i++) {
                sb.append(' ');
            }
            gap = sb.toString();
        } else if (sp instanceof JsString) {
            String s = ((JsString) sp).getValue();
            gap = s.length() <= 10 ? s : s.substring(0, 10);
        }
        JsObject wrapper = r.newObject();
        wrapper.createDataProperty("", value);
        String out = new Serializer(r, replacerFunction, propertyList, gap).property("", wrapper);
        return out == null ? Js.UNDEFINED : Js.str(out);
    }
}
