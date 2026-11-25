package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.interp.Scripts;

/**
 * BigInt64Array, BigUint64Array and DataView BigInt accessors (plan item P28-04-b).
 * <p>
 * BigInt64Array, BigUint64Array 와 DataView BigInt 접근자 시험(계획 P28-04-b).
 */
public class BigIntTypedArrayTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public BigIntTypedArrayTest() {
    }

    /**
     * Checks construction, 64-bit wrapping, tags and element access.
     * <p>
     * 생성, 64비트 감싸기, 태그, 요소 접근을 검사한다.
     */
    public void testConstructionAndElements() {
        Scripts.checkInline(
                "t('signed bytes', BigInt64Array.BYTES_PER_ELEMENT, 8);"
                + "t('unsigned bytes', BigUint64Array.prototype.BYTES_PER_ELEMENT, 8);"
                + "var a=new BigInt64Array([1n,-1n,9223372036854775808n,-9223372036854775809n]);"
                + "t('signed minmax', a.join('|'), '1|-1|-9223372036854775808|9223372036854775807');"
                + "var u=new BigUint64Array([-1n,18446744073709551616n,18446744073709551617n]);"
                + "t('unsigned wrap', u.join('|'), '18446744073709551615|0|1');"
                + "t('kind', a[0]===1n && typeof a[0], 'bigint');"
                + "t('tag', Object.prototype.toString.call(u), '[object BigUint64Array]');"
                + "t('properties', (0 in a) + ':' + (4 in a), 'true:false');",
                7);
    }

    /**
     * Checks ToBigInt conversions and mixed-element-type errors.
     * <p>
     * ToBigInt 변환과 요소 형식 혼합 오류를 검사한다.
     */
    public void testConversionRules() {
        Scripts.checkInline(
                "var hits=0; var v={valueOf:function(){hits++; return '12';}};"
                + "var a=new BigInt64Array(2); a[0]=v; a[99]=true;"
                + "t('converted', a[0]+':'+hits, '12:1');"
                + "t('boolean', new BigInt64Array([false,true]).join('|'), '0|1');"
                + "terr('invalid index number', function(){ var x=new BigInt64Array(1); x[9]=1; }, 'TypeError');"
                + "terr('number from bigint', function(){ new Int32Array(new BigInt64Array([1n])); }, 'TypeError');"
                + "terr('bigint from number', function(){ new BigInt64Array(new Int32Array([1])); }, 'TypeError');"
                + "terr('length bigint', function(){ new BigInt64Array(2n); }, 'TypeError');",
                6);
    }

    /**
     * Checks BigInt-aware iteration, fill, sort, copy, set and subarray views.
     * <p>
     * BigInt 를 보존하는 반복, 채우기, 정렬, 복사, set, subarray 뷰를 검사한다.
     */
    public void testMethodsPreserveBigInt() {
        Scripts.checkInline(
                "var a=new BigInt64Array([3n,-2n,1n]);"
                + "t('map', a.map(function(x){return x*2n;}).join('|'), '6|-4|2');"
                + "a.sort(); t('sort', a.join('|'), '-2|1|3');"
                + "a.fill(5n,1); t('fill', a.join('|'), '-2|5|5');"
                + "var b=a.slice(); a.reverse(); t('copy', b.join('|')+':'+a.join('|'), '-2|5|5:5|5|-2');"
                + "var c=new BigInt64Array(4); c.set(b,1); t('set', c.join('|'), '0|-2|5|5');"
                + "var s=c.subarray(1,3); s[0]=9n; t('view', c.join('|'), '0|9|5|5');"
                + "t('iterator', Array.from(a).every(function(x){return typeof x==='bigint';}), true);",
                7);
    }

    /**
     * Checks DataView BigInt reads and writes in both byte orders.
     * <p>
     * DataView BigInt 읽기·쓰기를 두 바이트 순서로 검사한다.
     */
    public void testDataViewBigInt() {
        Scripts.checkInline(
                "var b=new ArrayBuffer(24), v=new DataView(b);"
                + "v.setBigUint64(0, 0x0102030405060708n, false);"
                + "v.setBigInt64(8, -2n, true);"
                + "t('uint be', v.getBigUint64(0,false).toString(16), '102030405060708');"
                + "t('signed le', v.getBigInt64(8,true), -2n);"
                + "var bytes=new Uint8Array(b);"
                + "t('bytes', bytes[0]+':'+bytes[7], '1:8');"
                + "v.setBigUint64(16, -1n, true);"
                + "t('wrap', v.getBigUint64(16,true), 18446744073709551615n);"
                + "terr('number value', function(){ v.setBigInt64(0,1,false); }, 'TypeError');"
                + "terr('missing bounds conversion', function(){ v.setBigInt64(100,1,false); }, 'TypeError');",
                6);
    }
}
