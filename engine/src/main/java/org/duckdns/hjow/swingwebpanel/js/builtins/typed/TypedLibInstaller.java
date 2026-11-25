package org.duckdns.hjow.swingwebpanel.js.builtins.typed;

import org.duckdns.hjow.swingwebpanel.js.BuiltinInstaller;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Installs the binary data library of ECMAScript 2015 into a realm: <code>ArrayBuffer</code>, <code>DataView</code> and the typed arrays
 * <code>Int8Array</code>, <code>Uint8Array</code>, <code>Uint8ClampedArray</code>, <code>Int16Array</code>, <code>Uint16Array</code>,
 * <code>Int32Array</code>, <code>Uint32Array</code>, <code>Float16Array</code>, <code>Float32Array</code>, <code>Float64Array</code>
 * and the BigInt typed arrays together with the abstract
 * <code>%TypedArray%</code> intrinsic. The standard built-in setup creates this class by name and calls {@link #install(JsRealm)} after the core,
 * the base library, the string/array library and the data library have been installed (so that <code>Array.prototype.toString</code> exists);
 * if it does not exist yet, an equivalent function is created for <code>%TypedArray%.prototype.toString</code>.
 * <p>
 * ECMAScript 2015 의 이진 데이터 라이브러리를 realm 에 설치한다: <code>ArrayBuffer</code>, <code>DataView</code>, 형식화 배열
 * <code>Int8Array</code>, <code>Uint8Array</code>, <code>Uint8ClampedArray</code>, <code>Int16Array</code>, <code>Uint16Array</code>,
 * <code>Int32Array</code>, <code>Uint32Array</code>, <code>Float16Array</code>, <code>Float32Array</code>, <code>Float64Array</code>,
 * BigInt 형식화 배열과 추상 내장 객체
 * <code>%TypedArray%</code>. 표준 내장 객체 설정이 이 클래스를 이름으로 만들어 코어, 기본 라이브러리, 문자열/배열 라이브러리, 데이터 라이브러리를
 * 설치한 뒤에 {@link #install(JsRealm)} 을 호출한다(그래서 <code>Array.prototype.toString</code> 이 이미 있다). 아직 없으면
 * <code>%TypedArray%.prototype.toString</code> 을 위해 같은 동작의 함수를 만든다.
 */
public class TypedLibInstaller implements BuiltinInstaller {

    /**
     * Creates the installer (the standard setup creates it by name with this public no-argument constructor).
     * <p>
     * 설치기를 만든다(표준 설정이 이 공개 무인수 생성자로 이름을 통해 만든다).
     */
    public TypedLibInstaller() {
    }

    /**
     * Installs ArrayBuffer, SharedArrayBuffer, DataView, %TypedArray% and concrete typed array constructors, and registers their intrinsics
     * (<code>%ArrayBuffer%</code>, <code>%DataView%</code>, <code>%TypedArray%</code>, <code>%Int8Array%</code> ... with the matching
     * <code>...Prototype%</code> objects).
     * <p>
     * ArrayBuffer, SharedArrayBuffer, DataView, %TypedArray% 및 구체 형식화 배열 생성자를 설치하고 내장 객체
     * (<code>%ArrayBuffer%</code>, <code>%DataView%</code>, <code>%TypedArray%</code>, <code>%Int8Array%</code> 등과 대응하는
     * <code>...Prototype%</code>)를 등록한다.
     *
     * @param realm the realm to install into / 설치할 realm
     */
    public void install(JsRealm realm) {
        ArrayBufferLib.install(realm);
        SharedArrayBufferLib.install(realm);
        DataViewLib.install(realm);
        TypedArrayLib.install(realm);
    }
}
