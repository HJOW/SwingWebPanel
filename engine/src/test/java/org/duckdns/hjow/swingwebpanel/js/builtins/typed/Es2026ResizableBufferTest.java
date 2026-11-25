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
     * Exercises bounds, endianness and recovery of fixed and length-tracking DataViews.
     * <p>
     * 고정 길이와 길이 추적 DataView의 경계, 엔디언, 범위 복구를 검증한다.
     */
    public void testDataViewTrackingAndBounds() {
        checkViewScript("const b=new ArrayBuffer(5,{maxByteLength:10});"
                + "const d=new DataView(b,1),f=new DataView(b,1,4);"
                + "d.setUint16(0,0x1234);"
                + "resizeBuffer(b,3);t('tracking length',d.byteLength,2);"
                + "t('endian read',d.getUint16(0,true),0x3412);"
                + "try{f.byteLength;}catch(e){t('fixed length bounds',e.name,'TypeError');}"
                + "try{f.getUint8(0);}catch(e){t('fixed read bounds',e.name,'TypeError');}"
                + "try{d.getUint32(0);}catch(e){t('field does not fit',e.name,'RangeError');}"
                + "resizeBuffer(b,1);t('zero length at valid offset',d.byteLength,0);"
                + "t('original offset at valid zero length',d.byteOffset,1);"
                + "resizeBuffer(b,8);t('recovery',[d.byteLength,f.byteLength,f.getUint16(0)].join(','),'7,4,0');"
                + "try{d.setUint8(0,{valueOf(){resizeBuffer(b,0);return 5;}});}"
                + " catch(e){t('setter after value conversion',e.name,'TypeError');}"
                + "try{d.byteOffset;}catch(e){t('offset bounds',e.name,'TypeError');}"
                + "t('buffer retained',d.buffer===b,true);", 11);
    }

    /**
     * Rechecks the constructor's bounds after user code obtains the prototype.
     * <p>
     * 사용자 코드가 프로토타입을 반환한 뒤 생성자의 경계를 다시 검사한다.
     */
    public void testDataViewConstructorRechecksBufferAfterPrototypeLookup() {
        checkViewScript("const b=new ArrayBuffer(6,{maxByteLength:10});const log=[];"
                + "const target=new Proxy(function(){},{get(t,k){if(k==='prototype'){log.push(k);resizeBuffer(b,2);}return t[k];}});"
                + "try{Reflect.construct(DataView,[b,1,3],target);}"
                + " catch(e){t('fixed range after prototype',e.name,'RangeError');}"
                + "t('prototype was accessed',log.join(','),'prototype');"
                + "const auto=Reflect.construct(DataView,[b,1],target);"
                + "const getter=Object.getOwnPropertyDescriptor(DataView.prototype,'byteLength').get;"
                + "resizeBuffer(b,5);t('tracking through custom constructor',getter.call(auto),4);", 3);
    }

    /**
     * Uses the public resize and transfer methods and checks the original views after detachment.
     * <p>
     * 공개 resize 및 transfer 메서드를 사용하고 분리 뒤 원본 뷰를 확인한다.
     */
    public void testResizeAndTransferThroughJavaScript() {
        Scripts.checkInline("const a=new ArrayBuffer(4,{maxByteLength:8});"
                + "const typed=new Uint8Array(a),view=new DataView(a);typed.set([1,2,3,4]);"
                + "t('resize return',a.resize(2),undefined);a.resize(5);"
                + "t('growth does not restore discarded bytes',typed.join(','),'1,2,0,0,0');"
                + "try{a.transfer(9);}catch(e){t('resize limit preserved',e.name,'RangeError');}"
                + "t('failed transfer stays attached',a.detached,false);"
                + "Object.defineProperty(a,'constructor',{get(){throw 'species';}});"
                + "const b=a.transfer(6);"
                + "t('source detached',[a.detached,a.byteLength,a.maxByteLength,a.resizable,typed.length].join(','),'true,0,0,true,0');"
                + "try{view.getUint8(0);}catch(e){t('old DataView is detached',e.name,'TypeError');}"
                + "t('target resizable',[b.byteLength,b.maxByteLength,b.resizable].join(','),'6,8,true');"
                + "t('target bytes',new Uint8Array(b).join(','),'1,2,0,0,0,0');"
                + "const c=b.transferToFixedLength(10);"
                + "t('fixed length target',[c.byteLength,c.maxByteLength,c.resizable,b.detached].join(','),'10,10,false,true');"
                + "t('default transfer length',c.transfer().byteLength,10);", 10);
    }

    /**
     * Observes side effects of length conversion, failed transfers and shrinking during slice.
     * <p>
     * 길이 변환의 부수 효과, 실패한 전송, slice 도중의 축소를 확인한다.
     */
    public void testResizeAndTransferCoercionOrder() {
        Scripts.checkInline("let log=[];const fixed=new ArrayBuffer(1),r=new ArrayBuffer(4,{maxByteLength:8});"
                + "try{fixed.resize({valueOf(){log.push('fixed');return 0;}});}catch(e){t('fixed resize',e.name,'TypeError');}"
                + "new Uint8Array(r).set([7,8,9,10]);"
                + "const moved=r.transfer({valueOf(){log.push('transfer');r.resize(2);return 5;}});"
                + "t('reads current bytes after conversion',new Uint8Array(moved).join(','),'7,8,0,0,0');"
                + "try{r.transfer({valueOf(){log.push('detached');return 4;}});}catch(e){t('detached transfer',e.name,'TypeError');}"
                + "try{moved.resize({valueOf(){log.push('resize');moved.transfer();return 0;}});}"
                + " catch(e){t('detached during resize',e.name,'TypeError');}"
                + "t('conversion order',log.join(','),'transfer,detached,resize');"
                + "const x=new ArrayBuffer(5,{maxByteLength:6});"
                + "const slice=x.slice({valueOf(){x.resize(1);return 2;}});"
                + "t('slice start beyond new length',new Uint8Array(slice).join(','),'0,0,0');", 6);
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
