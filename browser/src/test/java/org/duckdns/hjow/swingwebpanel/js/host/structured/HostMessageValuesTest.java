package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.embed.JsHostFunction;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Checks Java-origin web message snapshots in a receiving script realm.
 * <p>
 * 수신 스크립트 렐름에서 Java발 웹 메시지 스냅샷을 확인한다.
 */
public final class HostMessageValuesTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HostMessageValuesTest() {
    }

    /**
     * Evaluates a check inside a fresh receiving realm.
     * <p>
     * 새 수신 렐름에서 확인식을 실행한다.
     *
     * @param message serialized Java input / 직렬화한 Java 입력
     * @param expression receiving script / 수신 스크립트
     * @return the string result / 문자열 결과
     */
    private static String receive(ClonedValue message, String expression) {
        JsRealm realm = JsRealm.create();
        realm.getGlobalObject().set("v", StructuredClone.deserialize(realm, message));
        return Ops.toString(realm.evaluateScript(expression, "host-message"));
    }

    /**
     * Verifies supported containers, prototypes, aliases and independence from mutation.
     * <p>
     * 지원 컨테이너, 프로토타입, 별칭과 원본 변경의 독립성을 검증한다.
     */
    public void testContainersAndSnapshot() {
        byte[] buffer = {1, -1};
        Date date = new Date(123456789L);
        StringBuilder text = new StringBuilder("before");
        Map<String, Object> sent = new LinkedHashMap<String, Object>();
        List<Object> list = new ArrayList<Object>(Arrays.asList(buffer, date, text));
        sent.put("__proto__", "own");
        sent.put("value", list);
        sent.put("same", new Object[] {list, buffer, date});
        sent.put("primitiveArrays", new Object[] {new boolean[] {true}, new char[] {'A'}, new short[] {2}, new int[] {3},
                new long[] {4}, new float[] {5f}, new double[] {6d}});
        ClonedValue snapshot = HostMessageValues.fromJava(sent);
        buffer[0] = 42;
        date.setTime(0);
        text.append("!");
        list.clear();
        sent.clear();
        Check.equal("__proto__,value,same,primitiveArrays|own|true|true|true|true|true|1,255|123456789|before|"
                + "true,A,2,3,4,5,6", receive(snapshot,
                "[Object.keys(v).join(),v.__proto__,Object.getPrototypeOf(v)===Object.prototype,"
                + "v.value===v.same[0],v.value[0]===v.same[1],v.value[1]===v.same[2],v.value[1] instanceof Date,"
                + "new Uint8Array(v.value[0]).join(),v.value[1].getTime(),v.value[2],v.primitiveArrays.flat().join()].join('|');"),
                "copied containers carry receiver prototypes, order and aliases");
        receive(snapshot, "new Uint8Array(v.value[0])[0]=99;v.value[1].setTime(1);");
        Check.equal("1|123456789", receive(snapshot, "[new Uint8Array(v.value[0])[0],v.value[1].getTime()].join('|');"),
                "later deserializations allocate independent buffers and dates");
    }

    /**
     * Verifies primitive conversion, precision rules and nullable data.
     * <p>
     * 원시값 변환, 정밀도 규칙과 null 데이터를 검증한다.
     */
    public void testPrimitiveValues() {
        BigInteger integer = new BigInteger("999999999999999999999999999999999");
        ClonedValue snapshot = HostMessageValues.fromJava(Arrays.asList(null, Boolean.FALSE, integer, Long.valueOf(Long.MAX_VALUE),
                Double.valueOf(-0d), Double.valueOf(Double.NaN), Float.valueOf(Float.POSITIVE_INFINITY), Character.valueOf('한')));
        Check.equal("true|false|true|true|true|true|true|한", receive(snapshot,
                "[v[0]===null,v[1],v[2]===999999999999999999999999999999999n,"
                + "v[3]===9223372036854775808,Object.is(v[4],-0),Number.isNaN(v[5]),v[6]===Infinity,v[7]].join('|')"),
                "Java numeric kinds map to the corresponding JS primitives");
    }

    /**
     * Rejects unsupported input without exposing Java methods or accepting recursive data.
     * <p>
     * Java 메서드를 노출하지 않고 미지원 입력과 순환 데이터를 거부한다.
     */
    public void testInvalidValuesAndLimits() {
        final List<Object> circular = new ArrayList<Object>();
        circular.add(circular);
        reject(circular);
        reject(Collections.singletonMap(Integer.valueOf(2), "bad key"));
        reject(new Object());
        reject(Js.num(2));
        reject(JsEngine.UNDEFINED);
        reject(new JsEngine().eval("({secret:true})"));
        reject(new JsHostFunction() {
            public Object call(Object receiver, Object[] args) {
                throw new AssertionError("a host function was called");
            }
        });
        reject(new Date(Long.MAX_VALUE));
        reject(new byte[HostMessageValues.MAX_MESSAGE_UNITS]);
        reject(String.valueOf(new char[HostMessageValues.MAX_MESSAGE_UNITS]));
        reject(Collections.nCopies(HostMessageValues.MAX_MESSAGE_UNITS, null));
        Object nested = "bottom";
        // 한도보다 한 단계 더 깊은 단일 가지로 중첩 한도를 검증한다.
        for (int i = 0; i <= HostMessageValues.MAX_DEPTH; i++) {
            nested = new Object[] {nested};
        }
        reject(nested);
    }

    /**
     * Checks one rejected Java-origin value.
     * <p>
     * 거부할 Java발 값 하나를 확인한다.
     *
     * @param value rejected value / 거부할 값
     */
    private static void reject(final Object value) {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            public void run() {
                HostMessageValues.fromJava(value);
            }
        }, "invalid host message");
    }
}
