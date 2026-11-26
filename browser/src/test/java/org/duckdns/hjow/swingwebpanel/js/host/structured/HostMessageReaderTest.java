package org.duckdns.hjow.swingwebpanel.js.host.structured;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.host.PageFixture;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the plain Java data copied from page messages and the atomicity of buffer transfers.
 * <p>
 * 페이지 메시지의 Java 복사값과 버퍼 전달의 원자성을 검증한다.
 */
public final class HostMessageReaderTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HostMessageReaderTest() {
    }

    /**
     * Evaluates the source and converts its value without transferring buffers.
     * <p>
     * 소스를 평가한 뒤 버퍼 전달 없이 값을 변환한다.
     *
     * @param script source code / 소스 코드
     * @return Java data / Java 데이터
     */
    private static Object send(String script) {
        JsRealm realm = JsRealm.create();
        return HostMessageValues.fromScript(realm.evaluateScript(script, "message"), null);
    }

    /**
     * Checks sparse arrays, aliases, Date, primitives and boxed primitives.
     * <p>
     * 희소 배열, 별칭, 날짜, 원시값과 래퍼 객체를 확인한다.
     */
    public void testJavaDataAndAliases() {
        Map<?, ?> data = (Map<?, ?>) send("var v={key:'own'}, d=new Date(1234);"
                + "({first:v,arr:[,undefined,v,null,d,new String('s'),new Boolean(false),Object(99999999999999999999n),"
                + "new Number(-0),NaN,Infinity], date:d, copied:new Number(2)});");
        Check.equal(Arrays.asList("first", "arr", "date", "copied"), new ArrayList<Object>(data.keySet()), "own data order");
        final List<?> array = (List<?>) data.get("arr");
        Check.same(JsEngine.UNDEFINED, array.get(0), "an array hole is represented as undefined");
        Check.same(JsEngine.UNDEFINED, array.get(1), "explicit undefined");
        Check.same(data.get("first"), array.get(2), "the repeated object is one Java object");
        Check.isNull(array.get(3), "null stays null");
        Check.same(data.get("date"), array.get(4), "the repeated date is one Java copy");
        Check.equal(1234L, ((Date) array.get(4)).getTime(), "date milliseconds");
        Check.equal("s", array.get(5), "boxed string");
        Check.equal(Boolean.FALSE, array.get(6), "boxed boolean");
        Check.equal(new BigInteger("99999999999999999999"), array.get(7), "boxed bigint");
        Check.equal(Double.valueOf(-0d), array.get(8), "boxed negative zero");
        Check.equal(Double.valueOf(Double.NaN), array.get(9), "NaN");
        Check.equal(Double.valueOf(Double.POSITIVE_INFINITY), array.get(10), "infinity");
        Check.equal(Double.valueOf(2), data.get("copied"), "number is always Double");
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            public void run() {
                array.clear();
            }
        }, "the copied array is read-only");
    }

    /**
     * Checks the insertion order and identity rule of Map and Set data.
     * <p>
     * Map과 Set 데이터의 삽입 순서와 정체성 규칙을 확인한다.
     */
    public void testMapAndSetKeys() {
        Map<?, ?> data = (Map<?, ?>) send("var a={}, b={}, wrapper=new String('s');"
                + "({keys:[a,b,wrapper],map:new Map([[a,1],[b,2],['s',3],[wrapper,4],[NaN,5]]),set:new Set([b,a,wrapper,'s'])});");
        List<?> keys = (List<?>) data.get("keys");
        final Map<?, ?> map = (Map<?, ?>) data.get("map");
        final Set<?> set = (Set<?>) data.get("set");
        Check.equal(5, map.size(), "distinct objects and boxed values remain distinct keys");
        Check.equal(Double.valueOf(1), map.get(keys.get(0)), "lookup by original empty object identity");
        Check.equal(Double.valueOf(2), map.get(keys.get(1)), "lookup by second empty object identity");
        Check.equal(Double.valueOf(3), map.get("s"), "lookup by primitive");
        Check.equal(Double.valueOf(4), map.get(keys.get(2)), "lookup by wrapper");
        Check.equal(Double.valueOf(5), map.get(Double.valueOf(Double.NaN)), "lookup by NaN");
        Object[] members = set.toArray();
        Check.equal(4, members.length, "Set keeps a boxed string and the primitive separately");
        Check.same(keys.get(1), members[0], "first member is the second key");
        Check.same(keys.get(0), members[1], "second member is the first key");
        Check.same(keys.get(2), members[2], "boxed member");
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            public void run() {
                map.entrySet().iterator().next().setValue(null);
            }
        }, "the copied map entries are immutable");
        Check.throwsException(UnsupportedOperationException.class, new Block() {
            public void run() {
                set.clear();
            }
        }, "the copied set is read-only");
    }

    /**
     * Checks the byte ranges, resizable storage and aliases of buffers and views.
     * <p>
     * 버퍼와 뷰의 바이트 구간, 가변 저장소와 별칭을 확인한다.
     */
    public void testBufferAndViewSnapshots() {
        List<?> data = (List<?>) send("var b=new ArrayBuffer(12,{maxByteLength:16}), v=new Uint8Array(b);"
                + "for(var i=0;i<v.length;i++) v[i]=i;"
                + "var half=new Float16Array(b,2,2), big=new BigUint64Array(b,0,1);"
                + "[b,half,big,new DataView(b,3,5),new Uint16Array(b,4),half];");
        Check.isTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}, (byte[]) data.get(0)), "array buffer");
        Check.isTrue(Arrays.equals(new byte[] {2, 3, 4, 5}, (byte[]) data.get(1)), "Float16Array range");
        Check.isTrue(Arrays.equals(new byte[] {0, 1, 2, 3, 4, 5, 6, 7}, (byte[]) data.get(2)), "BigUint64Array range");
        Check.isTrue(Arrays.equals(new byte[] {3, 4, 5, 6, 7}, (byte[]) data.get(3)), "DataView range");
        Check.isTrue(Arrays.equals(new byte[] {4, 5, 6, 7, 8, 9, 10, 11}, (byte[]) data.get(4)), "length-tracking typed array");
        Check.same(data.get(1), data.get(5), "aliases to the same view reuse one byte array");
    }

    /**
     * Checks that transfer is committed only after the Java value and limit checks succeed.
     * <p>
     * Java 값 변환과 한도 검사를 통과한 뒤 버퍼를 분리하는지 확인한다.
     */
    public void testTransferCommitAndFailure() {
        final JsRealm realm = JsRealm.create();
        JsObject buffer = (JsObject) realm.evaluateScript("var b=new ArrayBuffer(8,{maxByteLength:16});"
                + "new Uint8Array(b).set([1,2,3,4]);b;", "buffer");
        final List<JsObject> transfer = Collections.singletonList(buffer);
        final JsValue cycle = realm.evaluateScript("var o={b:b};o.self=o;o;", "cycle");
        Check.throwsException(StructuredCloneException.class, new Block() {
            public void run() {
                HostMessageValues.fromScript(cycle, transfer);
            }
        }, "a failed Java conversion must preserve ownership");
        Check.equal("false", Ops.toString(realm.evaluateScript("b.detached", "preserved")), "transferred buffer stays attached on failure");
        JsValue accepted = realm.evaluateScript("var view = new Uint8Array(b,2);"
                + "({view:view, get later(){b.resize(4);return b;}});", "resize");
        Map<?, ?> data = (Map<?, ?>) HostMessageValues.fromScript(accepted, transfer);
        Check.isTrue(Arrays.equals(new byte[] {3, 4}, (byte[]) data.get("view")), "view uses the final transferred length");
        Check.isTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, (byte[]) data.get("later")), "resized transferred buffer");
        Check.equal("true", Ops.toString(realm.evaluateScript("b.detached", "committed")), "successful transfer detaches the buffer");
    }

    /**
     * Rejects unsupported graphs, DOM objects and transferable ports.
     * <p>
     * 미지원 그래프, DOM 객체와 포트 전달을 거부한다.
     */
    public void testRejectedScriptValues() {
        final JsRealm realm = JsRealm.create();
        // 각 소스가 같은 구조화 복제 오류를 만드는지 확인한다.
        for (String script : new String[] {"()=>{}", "Symbol()", "new SharedArrayBuffer(8)", "new Date(NaN)", "/a/", "new Error('x')",
                "Array(" + HostMessageValues.MAX_MESSAGE_UNITS + ")", "var a={};a.self=a;a;"}) {
            reject(realm.evaluateScript(script, "unsupported"), null);
        }
        PageFixture page = new PageFixture().load("<p id=a>page</p>");
        try {
            JsRealm pageRealm = page.scripts.getRealm();
            reject(pageRealm.evaluateScript("document.getElementById('a')", "node"), null);
            JsObject port = (JsObject) pageRealm.evaluateScript("new MessageChannel().port1", "port");
            reject(realm.evaluateScript("1", "value"), Collections.singletonList(port));
        } finally {
            page.scripts.dispose();
        }
    }

    /**
     * Checks the host conversion error category.
     * <p>
     * 호스트 변환 오류 종류를 확인한다.
     *
     * @param value source value / 원본 값
     * @param transfer associated transfer list / 관련 전달 목록
     */
    private static void reject(final JsValue value, final List<? extends JsObject> transfer) {
        StructuredCloneException e = (StructuredCloneException) Check.throwsException(StructuredCloneException.class, new Block() {
            public void run() {
                HostMessageValues.fromScript(value, transfer);
            }
        }, "invalid script message");
        Check.equal("DataCloneError", e.getName(), "browser converts the exception to DataCloneError");
    }
}
