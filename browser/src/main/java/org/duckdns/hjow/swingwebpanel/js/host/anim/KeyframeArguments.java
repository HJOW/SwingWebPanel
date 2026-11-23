package org.duckdns.hjow.swingwebpanel.js.host.anim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.anim.EffectTiming;
import org.duckdns.hjow.swingwebpanel.css.anim.KeyframeEffectModel;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The script arguments of Web Animations Level 1 (sections 6.5 and 6.6): "process a keyframes argument" (the array form and the property-indexed
 * form of the keyframes, with the order of the property reads, the conversions of the WebIDL types and the TypeErrors of the specification), the
 * timing dictionaries (<code>EffectTiming</code>, <code>OptionalEffectTiming</code>, <code>KeyframeEffectOptions</code> and
 * <code>KeyframeAnimationOptions</code>, read in the member order of WebIDL), and the objects that <code>getKeyframes()</code>,
 * <code>getTiming()</code> and <code>getComputedTiming()</code> return.
 * <p>
 * Web Animations Level 1 의 스크립트 인수(6.5 절과 6.6 절): "process a keyframes argument"(키프레임의 배열 형식과 속성별 형식, 속성 읽기 순서, WebIDL 형 변환,
 * 규격의 TypeError), 시간 사전(<code>EffectTiming</code>, <code>OptionalEffectTiming</code>, <code>KeyframeEffectOptions</code>,
 * <code>KeyframeAnimationOptions</code>. WebIDL 의 멤버 순서로 읽는다), <code>getKeyframes()</code>, <code>getTiming()</code>,
 * <code>getComputedTiming()</code> 이 돌려주는 객체.
 */
final class KeyframeArguments {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private KeyframeArguments() {
    }

    /**
     * The members of a timing dictionary as the script passed them; a member that is missing is null (or false).
     * <p>
     * 스크립트가 넘긴 시간 사전의 멤버. 없는 멤버는 null(또는 false)이다.
     */
    static final class TimingInput {
        /** The start delay. 시작 지연. */
        Double delay;
        /** The end delay. 끝 지연. */
        Double endDelay;
        /** The fill mode. 채움 모드. */
        String fill;
        /** The iteration start. 반복 시작. */
        Double iterationStart;
        /** The iteration count. 반복 횟수. */
        Double iterations;
        /** The duration, NaN for <code>auto</code>. 지속 시간. <code>auto</code> 이면 NaN. */
        Double duration;
        /** The direction. 방향. */
        String direction;
        /** The easing. 완화. */
        String easing;
        /** The composite operation of <code>KeyframeEffectOptions</code>. <code>KeyframeEffectOptions</code> 의 합성 연산. */
        String composite;
        /** The iteration composite operation (Level 2). 반복 합성 연산(Level 2). */
        String iterationComposite;
        /** True if <code>pseudoElement</code> is present. <code>pseudoElement</code> 가 있으면 true. */
        boolean pseudoPresent;
        /** The pseudo-element selector or null. 의사 요소 선택자 또는 null. */
        String pseudoElement;
        /** The id of the animation options. 애니메이션 옵션의 id. */
        String id;
        /** True if <code>timeline</code> is present. <code>timeline</code> 이 있으면 true. */
        boolean timelinePresent;
        /** The timeline value of the options. 옵션의 타임라인 값. */
        JsValue timeline;

        /**
         * Creates an empty input.
         * <p>
         * 빈 입력을 만든다.
         */
        TimingInput() {
        }

        /**
         * Checks the values (6.5.4) and throws the TypeError of the specification.
         * <p>
         * 값을 검사하고(6.5.4) 규격의 TypeError 를 던진다.
         *
         * @param context the prefix of the message / 메시지 앞부분
         */
        void validate(String context) {
            Double dur = duration == null || duration.isNaN() && durationAuto ? null : duration;
            String error = EffectTiming.validate(iterationStart == null ? Double.NaN : iterationStart.doubleValue(), iterations, dur, easing);
            if (error != null) {
                throw JsException.typeError(context + error);
            }
        }

        /** True if the duration is <code>auto</code>. 지속 시간이 <code>auto</code> 이면 true. */
        boolean durationAuto;

        /**
         * Assigns the present members to a timing.
         * <p>
         * 있는 멤버를 시간에 할당한다.
         *
         * @param t the timing / 시간
         */
        void applyTo(EffectTiming t) {
            if (delay != null) {
                t.setDelay(delay.doubleValue());
            }
            if (endDelay != null) {
                t.setEndDelay(endDelay.doubleValue());
            }
            if (fill != null) {
                t.setFill(fill);
            }
            if (iterationStart != null) {
                t.setIterationStart(iterationStart.doubleValue());
            }
            if (iterations != null) {
                t.setIterations(iterations.doubleValue());
            }
            if (duration != null) {
                t.setDuration(durationAuto ? Double.NaN : duration.doubleValue());
            }
            if (direction != null) {
                t.setDirection(direction);
            }
            if (easing != null) {
                t.setEasing(easing);
            }
        }
    }

    /**
     * Reads a timing argument: a number is the duration, undefined or null is the empty dictionary, an object is read as a dictionary.
     * <p>
     * 시간 인수를 읽는다: 숫자는 지속 시간, undefined 나 null 은 빈 사전, 객체는 사전으로 읽는다.
     *
     * @param v           the argument / 인수
     * @param withOptions true to read the members of <code>KeyframeEffectOptions</code> / <code>KeyframeEffectOptions</code> 멤버를 읽으려면 true
     * @param withAnimate true to read the members of <code>KeyframeAnimationOptions</code> / <code>KeyframeAnimationOptions</code> 멤버를 읽으려면 true
     * @param context     the prefix of the messages / 메시지 앞부분
     * @return the input / 입력
     */
    static TimingInput readOptions(JsValue v, boolean withOptions, boolean withAnimate, String context) {
        TimingInput in = new TimingInput();
        if (v.isUndefined() || v.isNull()) {
            return in;
        }
        if (!v.isObject()) {
            // 숫자는 지속 시간이다(unrestricted double).
            in.duration = Double.valueOf(Ops.toNumber(v));
            return in;
        }
        JsObject o = (JsObject) v;
        readTiming(o, in, context);
        // KeyframeEffectOptions 의 멤버(사전 이름 순).
        if (withOptions) {
            JsValue c = o.get("composite");
            if (!c.isUndefined()) {
                in.composite = enumValue(c, new String[] { "replace", "add", "accumulate" }, "CompositeOperation", context);
            }
            JsValue ic = o.get("iterationComposite");
            if (!ic.isUndefined()) {
                in.iterationComposite = enumValue(ic, new String[] { "replace", "accumulate" }, "IterationCompositeOperation", context);
            }
            JsValue pe = o.get("pseudoElement");
            if (!pe.isUndefined()) {
                in.pseudoPresent = true;
                in.pseudoElement = pe.isNull() ? null : Ops.toString(pe);
            }
        }
        // KeyframeAnimationOptions 의 멤버.
        if (withAnimate) {
            JsValue id = o.get("id");
            if (!id.isUndefined()) {
                in.id = Ops.toString(id);
            }
            JsValue tl = o.get("timeline");
            if (!tl.isUndefined()) {
                in.timelinePresent = true;
                in.timeline = tl;
            }
        }
        return in;
    }

    /**
     * Reads the members of <code>EffectTiming</code> in their WebIDL order (delay, direction, duration, easing, endDelay, fill, iterationStart,
     * iterations).
     * <p>
     * <code>EffectTiming</code> 의 멤버를 WebIDL 순서로 읽는다(delay, direction, duration, easing, endDelay, fill, iterationStart, iterations).
     *
     * @param o       the dictionary object / 사전 객체
     * @param in      receives the members / 멤버를 받는다
     * @param context the prefix of the messages / 메시지 앞부분
     */
    static void readTiming(JsObject o, TimingInput in, String context) {
        JsValue v = o.get("delay");
        if (!v.isUndefined()) {
            in.delay = Double.valueOf(finite(v, "delay", context));
        }
        v = o.get("direction");
        if (!v.isUndefined()) {
            in.direction = enumValue(v, new String[] { "normal", "reverse", "alternate", "alternate-reverse" }, "PlaybackDirection", context);
        }
        v = o.get("duration");
        if (!v.isUndefined()) {
            // (unrestricted double or DOMString): 숫자는 숫자, 그 밖에는 문자열이며 문자열은 auto 만 허용한다.
            if (v instanceof org.duckdns.hjow.swingwebpanel.js.JsNumber) {
                in.duration = Double.valueOf(Ops.toNumber(v));
            } else {
                String s = Ops.toString(v);
                if (!"auto".equals(s)) {
                    throw JsException.typeError(context + "Invalid duration '" + s + "'.");
                }
                in.duration = Double.valueOf(Double.NaN);
                in.durationAuto = true;
            }
        }
        v = o.get("easing");
        if (!v.isUndefined()) {
            in.easing = Ops.toString(v);
        }
        v = o.get("endDelay");
        if (!v.isUndefined()) {
            in.endDelay = Double.valueOf(finite(v, "endDelay", context));
        }
        v = o.get("fill");
        if (!v.isUndefined()) {
            in.fill = enumValue(v, new String[] { "none", "forwards", "backwards", "both", "auto" }, "FillMode", context);
        }
        v = o.get("iterationStart");
        if (!v.isUndefined()) {
            in.iterationStart = Double.valueOf(finite(v, "iterationStart", context));
        }
        v = o.get("iterations");
        if (!v.isUndefined()) {
            in.iterations = Double.valueOf(Ops.toNumber(v));
        }
    }

    /**
     * Converts a value to a finite double (the WebIDL type <code>double</code>).
     * <p>
     * 값을 유한한 double 로 바꾼다(WebIDL 형 <code>double</code>).
     *
     * @param v       the value / 값
     * @param name    the member name / 멤버 이름
     * @param context the prefix of the message / 메시지 앞부분
     * @return the number / 숫자
     */
    static double finite(JsValue v, String name, String context) {
        double d = Ops.toNumber(v);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw JsException.typeError(context + "Failed to read the '" + name + "' property: The provided double value is non-finite.");
        }
        return d;
    }

    /**
     * Converts a value to a WebIDL enumeration value.
     * <p>
     * 값을 WebIDL 열거 값으로 바꾼다.
     *
     * @param v       the value / 값
     * @param allowed the allowed values / 허용 값
     * @param type    the enumeration name / 열거 이름
     * @param context the prefix of the message / 메시지 앞부분
     * @return the value / 값
     */
    static String enumValue(JsValue v, String[] allowed, String type, String context) {
        String s = Ops.toString(v);
        // 허용 값 중 같은 것을 찾는다.
        for (String a : allowed) {
            if (a.equals(s)) {
                return a;
            }
        }
        throw JsException.typeError(context + "The provided value '" + s + "' is not a valid enum value of type " + type + ".");
    }

    /**
     * Returns the enumeration value or null if the value is not one (the setter of an enumeration attribute ignores such values).
     * <p>
     * 열거 값을 반환하며 그렇지 않은 값이면 null 을 반환한다(열거 속성의 설정자는 그런 값을 무시한다).
     *
     * @param v       the value / 값
     * @param allowed the allowed values / 허용 값
     * @return the value or null / 값 또는 null
     */
    static String enumOrNull(JsValue v, String[] allowed) {
        String s = Ops.toString(v);
        // 허용 값 중 같은 것을 찾는다.
        for (String a : allowed) {
            if (a.equals(s)) {
                return a;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------ keyframes argument

    /**
     * One keyframe while it is being processed.
     * <p>
     * 처리 중인 키프레임 하나.
     */
    private static final class Raw {
        /** The offset or null. 오프셋 또는 null. */
        Double offset;
        /** The computed offset while merging property-indexed keyframes. 속성별 키프레임을 합칠 때의 계산 오프셋. */
        double computed;
        /** The easing text. 완화 문자열. */
        String easing = "linear";
        /** The composite operation or null for auto. 합성 연산. auto 이면 null. */
        String composite;
        /** The property values by CSS name in the order of the IDL names. IDL 이름 순서의 CSS 이름별 속성 값. */
        final Map<String, String> values = new LinkedHashMap<String, String>();
    }

    /**
     * Runs "process a keyframes argument" (6.6.3) and returns the keyframes.
     * <p>
     * "process a keyframes argument"(6.6.3)를 실행하고 키프레임을 반환한다.
     *
     * @param realm   the realm / realm
     * @param arg     the argument / 인수
     * @param context the prefix of the messages / 메시지 앞부분
     * @return the keyframes / 키프레임
     */
    static List<KeyframeEffectModel.Keyframe> process(JsRealm realm, JsValue arg, String context) {
        List<Raw> raws = new ArrayList<Raw>();
        List<String> unusedEasings = new ArrayList<String>();
        if (arg.isUndefined() || arg.isNull()) {
            return new ArrayList<KeyframeEffectModel.Keyframe>();
        }
        if (!arg.isObject()) {
            throw JsException.typeError(context + "The provided value is not of type 'object'.");
        }
        JsObject o = (JsObject) arg;
        JsValue method = Ops.getMethod(o, WellKnownSymbols.ITERATOR);
        if (!method.isUndefined()) {
            JsObject it = IteratorHelper.getIteratorFromMethod(o, method);
            // 이터레이터의 항목마다 키프레임 하나를 읽는다.
            while (true) {
                JsObject step = IteratorHelper.step(it);
                if (step == null) {
                    break;
                }
                JsValue item = IteratorHelper.value(step);
                if (!item.isUndefined() && !item.isNull() && !item.isObject()) {
                    throw JsException.typeError(context + "Keyframes must be objects, or null or undefined.");
                }
                raws.add(readKeyframe(item, context));
            }
        } else {
            raws = readPropertyIndexed(realm, o, unusedEasings, context);
        }
        List<Double> offsets = new ArrayList<Double>();
        // 오프셋을 모아 순서와 범위를 검사한다.
        for (Raw r : raws) {
            offsets.add(r.offset);
        }
        String error = KeyframeEffectModel.checkOffsetValues(offsets);
        if (error != null) {
            throw JsException.typeError(context + error);
        }
        // 완화 문자열은 모든 읽기가 끝난 뒤에 검사한다(쓰이지 않은 것도).
        // 키프레임마다 완화를 해석해 본다.
        for (Raw r : raws) {
            if (EffectTiming.parseEasing(r.easing) == null) {
                throw JsException.typeError(context + "'" + r.easing + "' is not a valid value for easing");
            }
        }
        // 쓰이지 않은 완화도 해석해 본다.
        for (String e : unusedEasings) {
            if (EffectTiming.parseEasing(e) == null) {
                throw JsException.typeError(context + "'" + e + "' is not a valid value for easing");
            }
        }
        List<KeyframeEffectModel.Keyframe> out = new ArrayList<KeyframeEffectModel.Keyframe>();
        // 키프레임 모델을 만든다(해석되지 않는 속성 값은 여기서 버린다).
        for (Raw r : raws) {
            out.add(KeyframeEffectModel.newKeyframe(r.offset, r.easing, r.composite, r.values));
        }
        return out;
    }

    /**
     * Processes a keyframe-like object without lists (one keyframe of the array form).
     * <p>
     * 목록 없이 키프레임 같은 객체를 처리한다(배열 형식의 키프레임 하나).
     *
     * @param item    the object, null or undefined / 객체, null 또는 undefined
     * @param context the prefix of the messages / 메시지 앞부분
     * @return the keyframe / 키프레임
     */
    private static Raw readKeyframe(JsValue item, String context) {
        Raw r = new Raw();
        if (!item.isObject()) {
            return r;
        }
        JsObject o = (JsObject) item;
        // BaseKeyframe 사전의 멤버를 이름 순(composite, easing, offset)으로 읽는다.
        JsValue c = o.get("composite");
        if (!c.isUndefined()) {
            String op = enumValue(c, new String[] { "replace", "add", "accumulate", "auto" }, "CompositeOperationOrAuto", context);
            r.composite = "auto".equals(op) ? null : op;
        }
        JsValue e = o.get("easing");
        if (!e.isUndefined()) {
            r.easing = Ops.toString(e);
        }
        JsValue off = o.get("offset");
        if (!off.isUndefined() && !off.isNull()) {
            r.offset = Double.valueOf(finite(off, "offset", context));
        }
        // 애니메이션할 수 있는 속성을 IDL 이름 순으로 읽어 문자열로 바꾼다.
        for (String name : propertyNames(o)) {
            JsValue raw = o.get(name);
            r.values.put(KeyframeEffectModel.animatableProperty(name), Ops.toString(raw));
        }
        return r;
    }

    /**
     * Returns the keys of an object that name animatable properties, sorted by their code points.
     * <p>
     * 객체의 키 중 애니메이션할 수 있는 속성을 가리키는 것을 코드 포인트 순으로 정렬해 반환한다.
     *
     * @param o the object / 객체
     * @return the IDL names / IDL 이름
     */
    private static List<String> propertyNames(JsObject o) {
        List<String> names = new ArrayList<String>();
        // 열거 가능한 자체 키 중 애니메이션할 수 있는 속성만 고른다.
        for (String k : Ops.enumerableOwnKeys(o)) {
            if (KeyframeEffectModel.animatableProperty(k) != null) {
                names.add(k);
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Reads a value that may be one value or an iterable of values.
     * <p>
     * 값 하나이거나 값의 이터러블일 수 있는 값을 읽는다.
     *
     * @param realm the realm / realm
     * @param v     the value / 값
     * @return the values, or null if the value is a single value / 값 목록. 값 하나이면 null
     */
    private static List<JsValue> sequence(JsRealm realm, JsValue v) {
        if (!v.isObject()) {
            return null;
        }
        JsValue method = Ops.getMethod((JsObject) v, WellKnownSymbols.ITERATOR);
        if (method.isUndefined()) {
            return null;
        }
        JsObject it = IteratorHelper.getIteratorFromMethod(v, method);
        List<JsValue> out = new ArrayList<JsValue>();
        // 이터레이터가 끝날 때까지 값을 모은다.
        while (true) {
            JsObject step = IteratorHelper.step(it);
            if (step == null) {
                return out;
            }
            out.add(IteratorHelper.value(step));
        }
    }

    /**
     * Processes the property-indexed form of the keyframes (6.6.3).
     * <p>
     * 키프레임의 속성별 형식을 처리한다(6.6.3).
     *
     * @param realm         the realm / realm
     * @param o             the object / 객체
     * @param unusedEasings receives the easings that no keyframe uses / 어느 키프레임도 쓰지 않는 완화를 받는다
     * @param context       the prefix of the messages / 메시지 앞부분
     * @return the keyframes / 키프레임
     */
    private static List<Raw> readPropertyIndexed(JsRealm realm, JsObject o, List<String> unusedEasings, String context) {
        // BasePropertyIndexedKeyframe 사전의 멤버를 이름 순(composite, easing, offset)으로 읽는다.
        List<String> composites = new ArrayList<String>();
        JsValue c = o.get("composite");
        if (!c.isUndefined()) {
            List<JsValue> seq = sequence(realm, c);
            String[] allowed = { "replace", "add", "accumulate", "auto" };
            if (seq == null) {
                composites.add(enumValue(c, allowed, "CompositeOperationOrAuto", context));
            } else {
                // 목록의 항목마다 열거 값으로 바꾼다.
                for (JsValue x : seq) {
                    composites.add(enumValue(x, allowed, "CompositeOperationOrAuto", context));
                }
            }
        }
        List<String> easings = new ArrayList<String>();
        JsValue e = o.get("easing");
        if (!e.isUndefined()) {
            List<JsValue> seq = sequence(realm, e);
            if (seq == null) {
                easings.add(Ops.toString(e));
            } else {
                // 목록의 항목마다 문자열로 바꾼다.
                for (JsValue x : seq) {
                    easings.add(Ops.toString(x));
                }
            }
        }
        List<Double> offsets = new ArrayList<Double>();
        JsValue off = o.get("offset");
        if (!off.isUndefined()) {
            List<JsValue> seq = off.isNull() ? null : sequence(realm, off);
            if (seq == null) {
                offsets.add(off.isNull() ? null : Double.valueOf(finite(off, "offset", context)));
            } else {
                // 목록의 항목마다 null 또는 유한한 수로 바꾼다.
                for (JsValue x : seq) {
                    offsets.add(x.isNull() || x.isUndefined() ? null : Double.valueOf(finite(x, "offset", context)));
                }
            }
        }
        List<Raw> processed = new ArrayList<Raw>();
        // 속성마다 그 값 목록으로 키프레임을 만들고 오프셋을 고르게 나눈다.
        for (String name : propertyNames(o)) {
            JsValue raw = o.get(name);
            List<JsValue> seq = sequence(realm, raw);
            List<String> values = new ArrayList<String>();
            if (seq == null) {
                values.add(Ops.toString(raw));
            } else {
                // 목록의 항목마다 문자열로 바꾼다.
                for (JsValue x : seq) {
                    values.add(Ops.toString(x));
                }
            }
            String css = KeyframeEffectModel.animatableProperty(name);
            int n = values.size();
            // 값마다 키프레임을 만들고 계산 오프셋을 준다(값이 하나이면 1, 여럿이면 0 부터 1 까지 고르게).
            for (int i = 0; i < n; i++) {
                Raw k = new Raw();
                k.computed = n == 1 ? 1 : (double) i / (n - 1);
                k.values.put(css, values.get(i));
                processed.add(k);
            }
        }
        // 계산 오프셋 순으로 안정 정렬한다.
        Collections.sort(processed, new java.util.Comparator<Raw>() {
            /**
             * Orders two keyframes by their computed offsets.
             * <p>
             * 두 키프레임을 계산 오프셋 순으로 정한다.
             *
             * @param a the first / 첫째
             * @param b the second / 둘째
             * @return the order / 순서
             */
            public int compare(Raw a, Raw b) {
                return Double.compare(a.computed, b.computed);
            }
        });
        List<Raw> merged = new ArrayList<Raw>();
        // 계산 오프셋이 같은 이웃 키프레임을 합친다.
        for (Raw k : processed) {
            Raw last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && last.computed == k.computed) {
                last.values.putAll(k.values);
            } else {
                merged.add(k);
            }
        }
        // 합친 키프레임 안의 속성 순서를 IDL 이름 순으로 맞춘다.
        for (Raw k : merged) {
            List<String> keys = new ArrayList<String>(k.values.keySet());
            Collections.sort(keys, new java.util.Comparator<String>() {
                /**
                 * Orders two CSS names by their IDL names.
                 * <p>
                 * 두 CSS 이름을 IDL 이름 순으로 정한다.
                 *
                 * @param a the first / 첫째
                 * @param b the second / 둘째
                 * @return the order / 순서
                 */
                public int compare(String a, String b) {
                    return KeyframeEffectModel.idlName(a).compareTo(KeyframeEffectModel.idlName(b));
                }
            });
            Map<String, String> sorted = new LinkedHashMap<String, String>();
            // 정렬한 이름 순으로 값을 다시 넣는다.
            for (String key : keys) {
                sorted.put(key, k.values.get(key));
            }
            k.values.clear();
            k.values.putAll(sorted);
        }
        // offset 목록을 같은 위치의 키프레임에 준다.
        for (int i = 0; i < offsets.size() && i < merged.size(); i++) {
            merged.get(i).offset = offsets.get(i);
        }
        if (easings.isEmpty()) {
            easings.add("linear");
        }
        // easing 목록을 되풀이해 키프레임마다 주고 남는 것은 쓰이지 않은 완화로 둔다.
        for (int i = 0; i < merged.size(); i++) {
            merged.get(i).easing = easings.get(i % easings.size());
        }
        // 키프레임보다 많은 완화는 쓰이지 않은 완화로 모은다.
        for (int i = merged.size(); i < easings.size(); i++) {
            unusedEasings.add(easings.get(i));
        }
        // composite 목록을 되풀이해 auto 가 아닌 것을 준다.
        for (int i = 0; !composites.isEmpty() && i < merged.size(); i++) {
            String op = composites.get(i % composites.size());
            merged.get(i).composite = "auto".equals(op) ? null : op;
        }
        return merged;
    }

    // ------------------------------------------------------------------------------------------------ results

    /**
     * Converts a time to a script value: null for NaN.
     * <p>
     * 시간을 스크립트 값으로 바꾼다: NaN 이면 null.
     *
     * @param d the time / 시간
     * @return the value / 값
     */
    static JsValue time(double d) {
        return Double.isNaN(d) ? Js.NULL : Js.num(d);
    }

    /**
     * Returns the keyframes as <code>getKeyframes()</code> reports them: offset, computedOffset, easing, composite and the property values by IDL name.
     * <p>
     * <code>getKeyframes()</code> 가 알려 주는 대로 키프레임을 반환한다: offset, computedOffset, easing, composite, IDL 이름별 속성 값.
     *
     * @param realm the realm / realm
     * @param list  the keyframes / 키프레임
     * @return the array / 배열
     */
    static JsValue keyframesToJs(JsRealm realm, List<KeyframeEffectModel.Keyframe> list) {
        JsValue[] items = new JsValue[list.size()];
        // 키프레임마다 객체를 만든다.
        for (int i = 0; i < items.length; i++) {
            KeyframeEffectModel.Keyframe k = list.get(i);
            JsObject o = realm.newObject();
            o.createDataProperty("offset", k.getOffset() == null ? Js.NULL : Js.num(k.getOffset().doubleValue()));
            o.createDataProperty("computedOffset", Js.num(k.getComputedOffset()));
            o.createDataProperty("easing", Js.str(k.getEasing()));
            o.createDataProperty("composite", Js.str(k.getComposite() == null ? "auto" : k.getComposite()));
            // 속성 값을 IDL 이름으로 더한다.
            for (Map.Entry<String, String> v : k.getValues().entrySet()) {
                o.createDataProperty(KeyframeEffectModel.idlName(v.getKey()), Js.str(v.getValue()));
            }
            items[i] = o;
        }
        return realm.newArray(items);
    }

    /**
     * Adds the members of <code>EffectTiming</code> to an object.
     * <p>
     * 객체에 <code>EffectTiming</code> 의 멤버를 더한다.
     *
     * @param o        the object / 객체
     * @param t        the timing / 시간
     * @param computed true for the computed values (fill and duration resolved) / 계산 값이면(채움과 지속 시간을 푼다) true
     */
    private static void timingMembers(JsObject o, EffectTiming t, boolean computed) {
        o.createDataProperty("delay", Js.num(t.getDelay()));
        o.createDataProperty("direction", Js.str(t.getDirection()));
        if (computed) {
            o.createDataProperty("duration", Js.num(t.getIterationDuration()));
        } else {
            o.createDataProperty("duration", Double.isNaN(t.getDuration()) ? Js.str("auto") : Js.num(t.getDuration()));
        }
        o.createDataProperty("easing", Js.str(t.getEasing()));
        o.createDataProperty("endDelay", Js.num(t.getEndDelay()));
        o.createDataProperty("fill", Js.str(computed ? t.getComputedFill() : t.getFill()));
        o.createDataProperty("iterationStart", Js.num(t.getIterationStart()));
        o.createDataProperty("iterations", Js.num(t.getIterations()));
    }

    /**
     * Returns the object of <code>getTiming()</code>.
     * <p>
     * <code>getTiming()</code> 의 객체를 반환한다.
     *
     * @param realm the realm / realm
     * @param t     the timing / 시간
     * @return the object / 객체
     */
    static JsObject timingToJs(JsRealm realm, EffectTiming t) {
        JsObject o = realm.newObject();
        timingMembers(o, t, false);
        return o;
    }

    /**
     * Returns the object of <code>getComputedTiming()</code>.
     * <p>
     * <code>getComputedTiming()</code> 의 객체를 반환한다.
     *
     * @param realm the realm / realm
     * @param t     the timing / 시간
     * @param c     the calculations at the local time / 지역 시간에서의 계산
     * @return the object / 객체
     */
    static JsObject computedTimingToJs(JsRealm realm, EffectTiming t, EffectTiming.Computed c) {
        JsObject o = realm.newObject();
        timingMembers(o, t, true);
        o.createDataProperty("activeDuration", Js.num(t.getActiveDuration()));
        o.createDataProperty("currentIteration", time(c.currentIteration));
        o.createDataProperty("endTime", Js.num(t.getEndTime()));
        o.createDataProperty("localTime", time(c.localTime));
        o.createDataProperty("progress", time(c.progress));
        return o;
    }
}
