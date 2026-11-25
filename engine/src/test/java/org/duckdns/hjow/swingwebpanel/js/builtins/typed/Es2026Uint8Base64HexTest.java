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

    /**
     * Checks Base64 output, both alphabets and all final chunk modes.
     * <p>
     * Base64 출력, 두 알파벳, 마지막 묶음의 모든 처리 방식을 확인한다.
     */
    public void testBase64AlphabetsAndFinalChunks() {
        Scripts.checkInline("const vectors=[['',''],['66','Zg=='],['666f','Zm8='],['666f6f','Zm9v'],"
                + "['666f6f62','Zm9vYg=='],['666f6f6261','Zm9vYmE='],['666f6f626172','Zm9vYmFy']];"
                + "for(const [hex,encoded] of vectors){t('encoded '+hex,Uint8Array.fromHex(hex).toBase64(),encoded);"
                + " t('decoded '+hex,Uint8Array.fromBase64(encoded,{lastChunkHandling:'strict'}).toHex(),hex);}"
                + "const url={alphabet:'base64url',omitPadding:true};"
                + "t('base64url alphabet',Uint8Array.fromHex('fbff').toBase64(url),'-_8');"
                + "t('base64url decode',Uint8Array.fromBase64('-_8',url).toHex(),'fbff');"
                + "t('ASCII whitespace',Uint8Array.fromBase64(' Z m\\t9 v \\r\\n\\f').toHex(),'666f6f');"
                + "t('loose unused bits',Uint8Array.fromBase64('Zh==').toHex(),'66');"
                + "t('stop before lone pad',Uint8Array.fromBase64('Zm9v Zg=',{lastChunkHandling:'stop-before-partial'}).toHex(),'666f6f');"
                + "function bad(text,opts){try{Uint8Array.fromBase64(text,opts);return 'ok';}catch(e){return e.name;}}"
                + "t('strict padding',bad('Zg',{lastChunkHandling:'strict'}),'SyntaxError');"
                + "t('strict tail bits',bad('Zh==',{lastChunkHandling:'strict'}),'SyntaxError');"
                + "t('strict tail bits three digits',bad('Zm9=',{lastChunkHandling:'strict'}),'SyntaxError');"
                + "t('mixed alphabet',bad('+/8=',url),'SyntaxError');"
                + "t('vertical tab is invalid',bad('Zg\\v=='),'SyntaxError');"
                + "t('missing second pad',bad('Zg='),'SyntaxError');"
                + "t('trailing data',bad('Zg==Zm9v'),'SyntaxError');", 26);
    }

    /**
     * Checks bounded decoding and readable prefixes when decoding fails.
     * <p>
     * 용량을 제한한 디코딩과 오류 뒤 읽을 수 있는 접두부를 확인한다.
     */
    public void testBase64ReadWrittenAndPartialErrors() {
        Scripts.checkInline("function counts(a,s,opts){const r=a.setFromBase64(s,opts);return [r.read,r.written,a.toHex()].join(',');}"
                + "t('exact capacity',counts(new Uint8Array(3),'Zm9v  '),'4,3,666f6f');"
                + "t('larger capacity consumes spaces',counts(new Uint8Array(4),'Zm9v  '),'6,3,666f6f00');"
                + "t('an entire chunk must fit',counts(new Uint8Array(2),'Zm9v'),'0,0,0000');"
                + "t('a short unpadded chunk fits',counts(new Uint8Array(2),'Zm8'),'3,2,666f');"
                + "t('stop before partial input',counts(new Uint8Array(4),'Zm9v Zg',{lastChunkHandling:'stop-before-partial'}),'4,3,666f6f00');"
                + "t('complete padded last chunk',counts(new Uint8Array(4),'Zm9v Zg==',{lastChunkHandling:'stop-before-partial'}),'9,4,666f6f66');"
                + "t('zero capacity',counts(new Uint8Array(0),'???'),'0,0,');"
                + "let a=new Uint8Array(4);try{a.setFromBase64('Zm9v??');}catch(e){t('error name',e.name,'SyntaxError');}"
                + "t('prefix persisted',a.toHex(),'666f6f00');"
                + "try{new Uint8Array(1).setFromBase64('Zg!');}catch(e){t('validate digit before capacity',e.name,'SyntaxError');}", 10);
    }

    /**
     * Observes option access order and the buffer bounds after those accesses.
     * <p>
     * 옵션을 읽는 순서와 읽은 뒤의 버퍼 경계를 관찰한다.
     */
    public void testBase64OptionEvaluationAndBounds() {
        Scripts.checkInline("const log=[],b=new ArrayBuffer(4,{maxByteLength:8}),a=new Uint8Array(b,1);"
                + "const r=a.setFromBase64('Zg==',{get alphabet(){log.push('alphabet');return undefined;},"
                + " get lastChunkHandling(){log.push('last');b.resize(2);return 'strict';}});"
                + "t('option order',log.join(','),'alphabet,last');"
                + "t('length after options',[r.read,r.written,a.toHex()].join(','),'4,1,66');"
                + "try{a.toBase64({get omitPadding(){log.push('omit');b.transfer();return true;}});}"
                + " catch(e){t('detached after option',e.name,'TypeError');}"
                + "try{Uint8Array.prototype.toBase64.call(new Int8Array(1),{get alphabet(){log.push('wrong');}});}"
                + " catch(e){t('brand before options',e.name,'TypeError');}"
                + "try{Uint8Array.fromBase64('AA==',{alphabet:new String('base64'),get lastChunkHandling(){log.push('wrong');}});}"
                + " catch(e){t('wrong alphabet before last chunk option',e.name,'TypeError');}"
                + "try{Uint8Array.fromBase64('AA==',null);}catch(e){t('null options',e.name,'TypeError');}"
                + "t('observed options',log.join(','),'alphabet,last,omit');", 7);
    }
}
