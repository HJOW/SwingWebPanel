package org.duckdns.hjow.swingwebpanel.js.interp;

/**
 * BigInt primitive, operator and built-in tests (plan item P28-04).
 * <p>
 * BigInt 원시값·연산자·내장 객체 시험(계획 P28-04).
 */
public class BigIntCoreTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public BigIntCoreTest() {
    }

    /**
     * Checks primitive type, conversion, wrapping and prototype methods.
     * <p>
     * 원시 형식, 변환, 래핑, 프로토타입 메서드를 검사한다.
     */
    public void testPrimitiveAndConstructor() {
        Scripts.checkInline(
                "t('typeof', typeof 4n, 'bigint');"
                + "t('boolean', Boolean(0n)+':'+Boolean(2n), 'false:true');"
                + "t('function empty', BigInt(), 0n);"
                + "t('decimal', BigInt('  -12  '), -12n);"
                + "t('hex', BigInt('0xff'), 255n);"
                + "t('integer number', BigInt(5), 5n);"
                + "t('wrapper', Object(7n).valueOf(), 7n);"
                + "t('radix', (255n).toString(16), 'ff');"
                + "t('tag', Object.prototype.toString.call(1n), '[object BigInt]');"
                + "terr('new', function(){ new BigInt(1); }, 'TypeError');"
                + "terr('fraction', function(){ BigInt(1.5); }, 'RangeError');"
                + "terr('invalid', function(){ BigInt('1.0'); }, 'SyntaxError');"
                + "terr('unary plus', function(){ return +1n; }, 'TypeError');",
                13);
    }

    /**
     * Checks arithmetic, bitwise and update operators including mixed-type errors.
     * <p>
     * 산술·비트·갱신 연산자와 혼합 형식 오류를 검사한다.
     */
    public void testOperators() {
        Scripts.checkInline(
                "t('add', 8n+3n, 11n); t('sub', 8n-3n, 5n); t('mul', 8n*3n, 24n);"
                + "t('div', -7n/2n, -3n); t('mod', -7n%3n, -1n); t('pow', 3n**4n, 81n);"
                + "t('not', ~5n, -6n); t('and', 14n&9n, 8n); t('or', 14n|9n, 15n); t('xor', 14n^9n, 7n);"
                + "t('left', 3n<<2n, 12n); t('right', -8n>>2n, -2n);"
                + "var x=4n; t('post', x++, 4n); t('prefix', ++x, 6n);"
                + "terr('mix add', function(){ 1n+1; }, 'TypeError');"
                + "terr('mix mul', function(){ 1n*1; }, 'TypeError');"
                + "terr('unsigned', function(){ 1n>>>0n; }, 'TypeError');"
                + "terr('zero div', function(){ 1n/0n; }, 'RangeError');"
                + "terr('negative pow', function(){ 2n**-1n; }, 'RangeError');",
                19);
    }

    /**
     * Checks BigInt comparisons and Number cross-comparison rules.
     * <p>
     * BigInt 비교와 Number 교차 비교 규칙을 검사한다.
     */
    public void testComparison() {
        Scripts.checkInline(
                "t('strict', 3n===3n, true); t('not strict', 3n===3, false);"
                + "t('loose integer', 3n==3, true); t('loose fractional', 3n==3.1, false);"
                + "t('loose decimal string', 3n=='03', true); t('loose bad string', 3n=='3x', false);"
                + "t('bool true', 1n==true, true); t('bool false', 0n==false, true);"
                + "t('less', 9n<10, true); t('greater exact', 9007199254740993n>9007199254740992, true);"
                + "t('equal sort', 3n<=3, true); t('negative', -2n<-1, true);"
                + "t('nan', 0n<NaN, false);",
                13);
    }

    /**
     * Checks asIntN and asUintN including width zero and two's-complement wrapping.
     * <p>
     * 폭 0과 2의 보수 감싸기를 포함해 asIntN·asUintN 을 검사한다.
     */
    public void testTruncation() {
        Scripts.checkInline(
                "t('u small', BigInt.asUintN(4, 19n), 3n);"
                + "t('u negative', BigInt.asUintN(4, -1n), 15n);"
                + "t('i positive', BigInt.asIntN(4, 7n), 7n);"
                + "t('i negative', BigInt.asIntN(4, 9n), -7n);"
                + "t('i wrap', BigInt.asIntN(4, 31n), -1n);"
                + "t('zero width', BigInt.asUintN(0, 99n), 0n);"
                + "terr('negative width', function(){ BigInt.asIntN(-1, 1n); }, 'RangeError');",
                7);
    }

    /**
     * Checks the JSON rule while allowing an explicit replacer conversion.
     * <p>
     * 명시적 replacer 변환은 허용하면서 JSON 규칙을 검사한다.
     */
    public void testJsonSerialization() {
        Scripts.checkInline(
                "terr('primitive', function(){ JSON.stringify(1n); }, 'TypeError');"
                + "terr('wrapper', function(){ JSON.stringify(Object(2n)); }, 'TypeError');"
                + "terr('array', function(){ JSON.stringify([1n]); }, 'TypeError');"
                + "t('replacer', JSON.stringify({x:1n}, function(k,v){ return typeof v==='bigint' ? v.toString() : v; }), '{\"x\":\"1\"}');"
                + "t('toJSON', JSON.stringify({toJSON:function(){return 3n;}}, function(k,v){ return typeof v==='bigint' ? '3' : v; }), '\"3\"');",
                5);
    }
}
