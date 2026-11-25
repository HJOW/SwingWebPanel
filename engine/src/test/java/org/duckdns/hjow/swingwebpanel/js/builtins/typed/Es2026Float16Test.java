package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.interp.Scripts;
import org.duckdns.hjow.swingwebpanel.js.num.Float16;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Covers IEEE 754 half precision and its ECMAScript entry points.
 * <p>
 * IEEE 754 반정밀도와 그 ECMAScript 진입점을 검증한다.
 */
public class Es2026Float16Test {

    /**
     * Checks numeric transitions using the adjacent double on either side of a rounding tie.
     * <p>
     * 반올림 동점 양쪽의 인접한 double로 수치 경계를 확인한다.
     */
    public void testHalfPrecisionRoundingBoundaries() {
        double firstTie = Math.scalb(1.0, -25);
        Check.equal(0x0000, Float16.encode(firstTie), "half of the least subnormal rounds to zero");
        Check.equal(0x0001, Float16.encode(Math.nextUp(firstTie)), "the next double rounds to the least subnormal");
        Check.equal(0x8000, Float16.encode(-firstTie), "negative tie keeps zero's sign");
        double normalTie = Math.scalb(1023.5, -24);
        Check.equal(0x03ff, Float16.encode(Math.nextAfter(normalTie, 0)), "below the least normal tie");
        Check.equal(0x0400, Float16.encode(normalTie), "ties into the least normal");
        double middle = 1.0 + Math.scalb(1.0, -11);
        Check.equal(0x3c00, Float16.encode(middle), "ties toward the even significand");
        Check.equal(0x3c01, Float16.encode(Math.nextUp(middle)), "the next double survives without double rounding");
        Check.equal(0x7bff, Float16.encode(Math.nextAfter(65520.0, 0)), "the largest finite half");
        Check.equal(0x7c00, Float16.encode(65520.0), "ties to infinity at overflow");
        Check.equal(0xfc00, Float16.encode(-65520.0), "negative overflow");
    }

    /**
     * Round trips every finite, infinite and quiet NaN bit pattern.
     * <p>
     * 유한 값, 무한대, 조용한 NaN의 모든 비트 패턴을 왕복한다.
     */
    public void testAllHalfBitPatterns() {
        // 모든 16비트 패턴을 검증한다. JRE가 signaling NaN을 quiet NaN으로 바꿀 수 있다.
        for (int bits = 0; bits < 65536; bits++) {
            double value = Float16.decode(bits);
            if ((bits & 0x7c00) == 0x7c00 && (bits & 0x1ff) != 0 && (bits & 0x0200) == 0) {
                Check.isTrue(Double.isNaN(value), "signaling NaN decodes as NaN");
            } else if (Float16.encode(value) != bits) {
                Check.equal(bits, Float16.encode(value), "round trip 0x" + Integer.toHexString(bits));
            }
        }
    }

    /**
     * Exercises Math, Float16Array and DataView endianness through JavaScript.
     * <p>
     * JavaScript에서 Math와 Float16Array 및 DataView의 바이트 순서를 확인한다.
     */
    public void testJavaScriptHalfPrecision() {
        Scripts.checkInline("const a=new Float16Array([1+2**-11,1+3*2**-11,-0]);"
                + "t('array bytes',new Uint16Array(a.buffer).join(','),'15360,15362,32768');"
                + "t('element size',Float16Array.BYTES_PER_ELEMENT,2);"
                + "t('same type',a.toReversed() instanceof Float16Array,true);"
                + "t('f16round coercion',Math.f16round('1.00048828125'),1);"
                + "t('negative zero',Object.is(Math.f16round(-Number.MIN_VALUE),-0),true);"
                + "t('overflow',Math.f16round(65520),Infinity);"
                + "const d=new DataView(new ArrayBuffer(6));"
                + "d.setFloat16(1,1.5,true);"
                + "t('little endian write',d.getUint16(1),62);"
                + "t('little endian read',d.getFloat16(1,true),1.5);"
                + "d.setUint16(0,0xfe55);d.setFloat16(4,d.getFloat16(0));"
                + "t('NaN payload',d.getUint16(4),0xfe55);"
                + "try{d.getFloat16(5);}catch(e){t('width check',e.name,'RangeError');}", 10);
    }
}
