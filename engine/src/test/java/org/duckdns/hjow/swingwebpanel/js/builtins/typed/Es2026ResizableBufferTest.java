package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.interp.Scripts;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Covers resizable buffers, their views and transfer behavior.
 * <p>
 * 가변 길이 버퍼와 그 위의 뷰, 전송 동작을 검증한다.
 */
public class Es2026ResizableBufferTest {

    /**
     * Checks maxByteLength option processing and the initial buffer state.
     * <p>
     * maxByteLength 옵션 처리와 버퍼의 초기 상태를 확인한다.
     */
    public void testResizableConstructorOption() {
        Scripts.checkInline("const log=[];"
                + "const b=new ArrayBuffer({valueOf(){log.push('length');return 2;}},"
                + " {get maxByteLength(){log.push('get');return {valueOf(){log.push('max');return 8;}};}});"
                + "t('conversion order',log.join(','),'length,get,max');"
                + "t('resizable',[b.byteLength,b.maxByteLength,b.resizable,b.detached].join(','),'2,8,true,false');"
                + "const f=new ArrayBuffer(2,4);"
                + "t('primitive options ignored',[f.byteLength,f.maxByteLength,f.resizable].join(','),'2,2,false');"
                + "t('zero maximum',new ArrayBuffer(0,{maxByteLength:0}).resizable,true);"
                + "try{new ArrayBuffer(2,{maxByteLength:1});}catch(e){t('max exceeded',e.name,'RangeError');}"
                + "try{new ArrayBuffer(0,{maxByteLength:-1});}catch(e){t('negative max',e.name,'RangeError');}", 6);
    }

    /**
     * Resizes the backing array without exposing bytes discarded by a shrink.
     * <p>
     * 크기를 줄이며 버린 바이트가 나중에 다시 보이지 않도록 저장소를 재할당한다.
     */
    public void testStorageResizeAndDetach() {
        final ArrayBufferObject buffer = new ArrayBufferObject(null, new byte[] { 1, 2, 3, 4 }, 5);
        buffer.resizeTo(2);
        Check.equal(new byte[] { 1, 2 }, buffer.getData(), "shrink keeps prefix");
        buffer.resizeTo(5);
        Check.equal(new byte[] { 1, 2, 0, 0, 0 }, buffer.getData(), "growth zero-fills bytes");
        TypedTestSupport.expectError(ErrorKind.RANGE_ERROR, new Block() {
            public void run() {
                buffer.resizeTo(6);
            }
        }, "declared maximum");
        Check.equal(5, buffer.getByteLength(), "failed growth leaves the same length");
        buffer.detach();
        Check.equal(0, buffer.getMaxByteLength(), "detached maximum length");
        Check.isTrue(buffer.isResizable(), "resizability remains after detach");
        TypedTestSupport.expectError(ErrorKind.TYPE_ERROR, new Block() {
            public void run() {
                buffer.resizeTo(0);
            }
        }, "detached buffer");
    }

    /**
     * Exercises length-tracking and fixed typed array views across backing size changes.
     * <p>
     * 저장소 크기가 바뀔 때 길이 추적 뷰와 고정 길이 형식화 배열을 확인한다.
     */
    public void testTypedArrayTrackingAndRecovery() {
        checkViewScript("const b=new ArrayBuffer(5,{maxByteLength:10});"
                + "const tracking=new Uint16Array(b,2),fixed=new Uint16Array(b,2,1);"
                + "const sub=tracking.subarray(0),limited=tracking.subarray(0,1),iter=fixed.values();"
                + "t('odd bytes',tracking.length,1);tracking[0]=0x1234;"
                + "resizeBuffer(b,3);"
                + "t('fixed out of bounds',[fixed.length,fixed.byteLength,fixed.byteOffset,fixed[0]].join(','),'0,0,0,');"
                + "t('no index descriptors',Object.keys(fixed).length,0);"
                + "t('tracking empty at valid offset',[tracking.length,tracking.byteOffset].join(','),'0,2');"
                + "try{iter.next();}catch(e){t('iterator bounds',e.name,'TypeError');}"
                + "try{new Uint16Array(fixed);}catch(e){t('copy bounds',e.name,'TypeError');}"
                + "resizeBuffer(b,8);"
                + "t('restored fixed',iter.next().value,52);"
                + "t('resized views',[tracking.length,sub.length,limited.length,fixed.length].join(','),'3,3,1,1');", 8);
    }

    /**
     * Distinguishes captured lengths from current bounds after user code resizes a buffer.
     * <p>
     * 사용자 코드가 버퍼를 줄인 뒤 미리 읽은 길이와 현재 경계를 구분한다.
     */
    public void testTypedArrayResizeDuringCallbacks() {
        checkViewScript("const b=new ArrayBuffer(4,{maxByteLength:8}),a=new Uint8Array(b);"
                + "a.set([1,2,3,4]);"
                + "t('at uses captured length',a.at({valueOf(){resizeBuffer(b,2);return -1;}}),undefined);"
                + "t('grow during with',a.with(3,{valueOf(){resizeBuffer(b,4);return 50;}}).join(','),'1,2');"
                + "const log=[];"
                + "a.set({get length(){resizeBuffer(b,1);return 2;},"
                + " get 0(){log.push(0);return 7;},get 1(){log.push(1);return {valueOf(){log.push(2);return 8;}};}});"
                + "t('set reads both values',log.join(','),'0,1,2');t('invalid write suppressed',a.join(','),'7');"
                + "resizeBuffer(b,4);const f=new Uint8Array(b,0,4);"
                + "try{f.fill({valueOf(){resizeBuffer(b,1);return 2;}},0,0);}catch(e){t('fill checks current bounds',e.name,'TypeError');}"
                + "resizeBuffer(b,4);"
                + "const seen=[];a.findLastIndex((v,i)=>{seen.push(String(v));if(i===3)resizeBuffer(b,1);return false;});"
                + "t('findLast observes current values',seen.join(','),'0,undefined,undefined,7');", 6);
    }

    /**
     * Runs view assertions with a host hook that calls the underlying resize operation.
     * <p>
     * 내부 크기 변경 연산을 호출하는 호스트 함수를 연결해 뷰 단언을 실행한다.
     *
     * @param source test script / 시험 스크립트
     * @param assertions minimum expected assertions / 기대하는 최소 단언 수
     */
    private static void checkViewScript(String source, int assertions) {
        Scripts.Capture capture = new Scripts.Capture();
        JsRealm realm = Scripts.realm(capture);
        realm.getGlobalObject().defineBuiltin("resizeBuffer", new NativeFunction(realm, "resizeBuffer", 2, new NativeFunction.Call() {
            public JsValue call(JsRealm r, JsValue receiver, JsValue[] args) {
                ((ArrayBufferObject) args[0]).resizeTo((int) Ops.toNumber(args[1]));
                return Js.UNDEFINED;
            }
        }));
        Scripts.check(realm, capture, source, "resizable-views.js", assertions);
    }
}
