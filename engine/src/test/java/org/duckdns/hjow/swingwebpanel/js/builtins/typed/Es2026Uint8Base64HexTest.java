package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.interp.Scripts;

/**
 * Covers the text encoding APIs on Uint8Array and its prototype.
 * <p>
 * Uint8Array와 프로토타입의 텍스트 인코딩 API를 검증한다.
 */
public class Es2026Uint8Base64HexTest {

    /**
     * Checks hexadecimal conversions, consumed length and retention of complete bytes after an error.
     * <p>
     * 16진 변환, 소비한 길이와 오류 후 완성된 바이트의 보존을 확인한다.
     */
    public void testHexEncodingAndDecoding() {
        Scripts.checkInline("class Bytes extends Uint8Array {};"
                + "const base=Bytes.fromHex('AaFf0180');"
                + "t('intrinsic constructor',Object.getPrototypeOf(base)===Uint8Array.prototype,true);"
                + "t('lowercase byte encoding',base.toHex(),'aaff0180');"
                + "t('offset encoding',base.subarray(1,3).toHex(),'ff01');"
                + "const part=base.subarray(1,2),read=part.setFromHex('abzz');"
                + "t('destination limit',[read.read,read.written,base.toHex()].join(','),'2,1,aaab0180');"
                + "try{base.setFromHex('120g');}catch(e){t('invalid digit',e.name,'SyntaxError');}"
                + "t('complete prefix saved',base.toHex(),'12ab0180');"
                + "try{base.setFromHex('abc');}catch(e){t('odd input',e.name,'SyntaxError');}"
                + "t('odd input leaves bytes unchanged',base.toHex(),'12ab0180');"
                + "try{Uint8Array.fromHex('a ');}catch(e){t('whitespace',e.name,'SyntaxError');}"
                + "try{Uint8Array.fromHex(new String('00'));}catch(e){t('primitive string',e.name,'TypeError');}"
                + "try{Uint8Array.prototype.toHex.call(new Uint8ClampedArray(1));}catch(e){t('brand check',e.name,'TypeError');}", 11);
    }

    /**
     * Reads the current contents of an attached and in-bounds view.
     * <p>
     * 분리되지 않고 범위 안에 있는 뷰의 현재 내용을 읽는다.
     */
    public void testHexBufferWitness() {
        Scripts.checkInline("const b=new ArrayBuffer(4,{maxByteLength:8});"
                + "const fixed=new Uint8Array(b,1,2),tracking=new Uint8Array(b,1);"
                + "b.resize(2);"
                + "const r=tracking.setFromHex('fe9800');"
                + "t('tracking capacity',[r.read,r.written,tracking.toHex()].join(','),'2,1,fe');"
                + "try{fixed.toHex();}catch(e){t('out of bounds',e.name,'TypeError');}"
                + "b.resize(5);t('fixed view recovers',fixed.toHex(),'fe00');"
                + "b.transfer();"
                + "try{tracking.setFromHex('11');}catch(e){t('detached',e.name,'TypeError');}", 4);
    }
}
