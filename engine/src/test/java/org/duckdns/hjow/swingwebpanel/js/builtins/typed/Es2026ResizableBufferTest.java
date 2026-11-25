package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
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
}
