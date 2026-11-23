package org.duckdns.hjow.swingwebpanel.js;

import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Array exotic object (plan item P12-03): length semantics, holes, truncation, sparse mode and attributes.
 * <p>
 * Array 이국적 객체 시험(계획 P12-03): length 의미, 빈 칸, 잘라내기, 희소 모드, 속성.
 */
public class ArrayModelTest {

    /**
     * The realm used by the cases.
     * <p>
     * 사례가 쓰는 realm.
     */
    private final JsRealm realm = new JsRealm();

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public ArrayModelTest() {
    }

    /**
     * Returns an array length as a long.
     * <p>
     * 배열 길이를 long 으로 반환한다.
     *
     * @param a the array / 배열
     * @return the length / 길이
     */
    private long len(JsObject a) {
        return (long) ((JsNumber) a.get("length")).getValue();
    }

    /**
     * Assigning to an index at or beyond the length extends the length.
     * <p>
     * 길이 이상의 인덱스에 대입하면 길이가 늘어난다.
     */
    public void testIndexAssignmentExtendsLength() {
        JsArray a = realm.newArray();
        Check.equal(0, len(a), "empty");
        a.set("0", Js.num(10));
        a.set("2", Js.num(30));
        Check.equal(3, len(a), "length is highest index + 1");
        Check.isTrue(a.get("1").isUndefined(), "hole reads as undefined");
        Check.isFalse(a.hasOwnProperty("1"), "hole is not an own property");
        Check.equal("[0, 2, length]", a.ownKeys().toString(), "keys skip holes");
    }

    /**
     * Setting length shorter deletes the elements; setting it longer creates holes.
     * <p>
     * length 를 줄이면 요소가 지워지고 늘리면 빈 칸이 생긴다.
     */
    public void testLengthTruncatesAndExtends() {
        JsArray a = realm.newArray(new JsValue[] { Js.num(1), Js.num(2), Js.num(3) });
        Check.isTrue(a.set("length", Js.num(1)), "truncate");
        Check.equal(1, len(a), "new length");
        Check.isTrue(a.get("2").isUndefined(), "element removed");
        a.set("length", Js.num(5));
        Check.equal(5, len(a), "extended");
        Check.isFalse(a.hasOwnProperty("3"), "extension only creates holes");
        Check.isTrue(a.hasOwnProperty("0"), "kept element");
    }

    /**
     * Invalid lengths throw a RangeError.
     * <p>
     * 올바르지 않은 길이는 RangeError 를 던진다.
     */
    public void testInvalidLengthThrowsRangeError() {
        final JsArray a = realm.newArray();
        JsException e = (JsException) Check.throwsException(JsException.class, new Block() {
            public void run() {
                a.set("length", Js.num(-1));
            }
        }, "negative length");
        Check.equal(ErrorKind.RANGE_ERROR, e.getKind(), "RangeError kind");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                a.set("length", Js.num(1.5));
            }
        }, "fractional length");
        Check.throwsException(JsException.class, new Block() {
            public void run() {
                a.set("length", Js.num(4294967296.0));
            }
        }, "length 2^32");
    }

    /**
     * Array indices are canonical numeric strings below 2^32 - 1; other numeric-looking keys are plain properties.
     * <p>
     * 배열 인덱스는 2^32 - 1 미만의 정규 숫자 문자열이며, 숫자처럼 보이는 다른 키는 일반 프로퍼티이다.
     */
    public void testArrayIndexDefinition() {
        JsArray a = realm.newArray();
        a.set("01", Js.num(1));
        a.set("-1", Js.num(1));
        a.set("1.5", Js.num(1));
        a.set("4294967295", Js.num(1));
        Check.equal(0, len(a), "none of these is an array index");
        a.set("4294967294", Js.num(1));
        Check.equal(4294967295L, len(a), "largest index gives the maximum length");
    }

    /**
     * Huge indices switch to sparse mode without allocating memory and keep working.
     * <p>
     * 아주 큰 인덱스는 메모리를 쓰지 않고 희소 모드로 바뀌며 계속 동작한다.
     */
    public void testSparseArrayWithHugeIndex() {
        JsArray a = realm.newArray();
        a.set("0", Js.str("a"));
        a.set("1000000000", Js.str("z"));
        Check.equal(1000000001L, len(a), "length");
        Check.equal("a", a.getString("0"), "first element survives the mode switch");
        Check.equal("z", a.getString("1000000000"), "huge index");
        Check.equal("[0, 1000000000, length]", a.ownKeys().toString(), "keys in numeric order");
        a.set("length", Js.num(5));
        Check.equal(5, len(a), "truncation in sparse mode");
        Check.isTrue(a.get("1000000000").isUndefined(), "removed");
        Check.equal("a", a.getString("0"), "kept");
    }

    /**
     * A non-configurable element stops truncation (the length ends up one above it).
     * <p>
     * 설정 불가 요소가 있으면 잘라내기가 멈춘다(길이는 그 요소 바로 위가 된다).
     */
    public void testNonConfigurableElementStopsTruncation() {
        JsArray a = realm.newArray(new JsValue[] { Js.num(1), Js.num(2), Js.num(3), Js.num(4) });
        Check.isTrue(a.defineOwnProperty("1", PropertyDescriptor.data(Js.num(2), true, true, false)), "make element 1 permanent");
        Check.isFalse(a.defineOwnProperty("length", PropertyDescriptor.valueOnly(Js.num(0))), "truncation reports failure");
        Check.equal(2, len(a), "length stops above the permanent element");
        Check.isTrue(a.hasOwnProperty("0") && a.hasOwnProperty("1"), "elements below remain");
        Check.isFalse(a.hasOwnProperty("2"), "elements above are removed");
    }

    /**
     * A non-writable length blocks growth and the writable flag cannot be restored.
     * <p>
     * writable 이 아닌 length 는 증가를 막고 writable 을 되돌릴 수 없다.
     */
    public void testReadOnlyLength() {
        JsArray a = realm.newArray(new JsValue[] { Js.num(1) });
        PropertyDescriptor d = new PropertyDescriptor();
        d.writable = Boolean.FALSE;
        Check.isTrue(a.defineOwnProperty("length", d), "make length read-only");
        Check.isFalse(a.set("1", Js.num(2)), "append rejected");
        Check.equal(1, len(a), "length unchanged");
        Check.isFalse(a.set("length", Js.num(0)), "length cannot be changed");
        d = new PropertyDescriptor();
        d.writable = Boolean.TRUE;
        Check.isFalse(a.defineOwnProperty("length", d), "cannot become writable again");
        Check.isFalse(a.getOwnProperty("length").isWritableTrue(), "attribute reported");
        Check.isFalse(a.getOwnProperty("length").isEnumerableTrue(), "length is not enumerable");
    }

    /**
     * Non-default element attributes work through the sparse mode and survive.
     * <p>
     * 기본이 아닌 요소 속성은 희소 모드를 거쳐 유지된다.
     */
    public void testElementAttributes() {
        JsArray a = realm.newArray(new JsValue[] { Js.num(1), Js.num(2) });
        Check.isTrue(a.defineOwnProperty("0", PropertyDescriptor.data(Js.num(1), false, true, true)), "read-only element");
        Check.isFalse(a.set("0", Js.num(9)), "assignment rejected");
        Check.equal(1, (long) ((JsNumber) a.get("0")).getValue(), "value kept");
        Check.equal(2, (long) ((JsNumber) a.get("1")).getValue(), "other elements moved intact");
        Check.isFalse(a.getOwnProperty("0").isWritableTrue(), "attribute is reported");
        Check.isTrue(a.delete("0"), "configurable element can be deleted");
        Check.isFalse(a.hasOwnProperty("0"), "gone");
        Check.equal(2, len(a), "deleting does not change the length");
    }

    /**
     * delete on a dense array leaves a hole; the length property cannot be deleted.
     * <p>
     * 조밀 배열에서 delete 는 빈 칸을 남기며 length 는 지울 수 없다.
     */
    public void testDeleteLeavesHole() {
        JsArray a = realm.newArray(new JsValue[] { Js.num(1), Js.num(2) });
        Check.isTrue(a.delete("0"), "delete element");
        Check.isFalse(a.hasOwnProperty("0"), "hole");
        Check.equal(2, len(a), "length unchanged");
        Check.isFalse(a.delete("length"), "length is permanent");
    }

    /**
     * Fast element access helpers and add() keep the array consistent with the generic operations.
     * <p>
     * 빠른 요소 접근 도우미와 add() 가 일반 연산과 일관되게 동작한다.
     */
    public void testFastAccessHelpers() {
        JsArray a = realm.newArray();
        for (int i = 0; i < 100; i++) { // 요소 100 개를 차례로 추가한다.
            a.add(Js.num(i));
        }
        Check.equal(100, a.getLength(), "length after add");
        Check.equal(7, (long) ((JsNumber) a.getOwnElement(7)).getValue(), "getOwnElement");
        Check.isTrue(a.setOwnElement(7, Js.str("x")), "setOwnElement");
        Check.equal("x", a.getString("7"), "visible through the generic path");
        Check.isTrue(a.getOwnElement(100) == null, "beyond the end");
        Check.isFalse(a.setOwnElement(100, Js.num(1)), "cannot write beyond the end with the fast path");
        Check.isTrue(a.isArray() && Ops.isArray(a), "IsArray");
        Check.isFalse(Ops.isArray(realm.newObject()), "plain object is not an array");
        Check.equal("Array", a.className(), "builtin tag");
    }
}
