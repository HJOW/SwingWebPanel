# BUILTIN-E 진행 기록 (P14-06 ArrayBuffer / DataView / TypedArray, P14-09 검사)

작업자: BUILTIN-E. 소유 경로: `src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/**`, `tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/**`, 이 문서.
설치 진입점: `org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedLibInstaller` (`StandardBuiltins` 가 이름으로 선택 적재; core → corelib → stringarray → datajson → **typed** → meta 순서).
시험 방법: `scripts/pkgtest.sh bie "js/builtins/typed" "js/builtins/typed"`(최종 확인은 이것으로 했다). 초기에는 JS-CORE 의 `JsRealm`/`StandardBuiltins` 가 작업 중이라 작업 트리가 컴파일되지 않아, 마지막 commit 의 `src` 스냅샷(`git archive HEAD src`)을
의존 소스 경로로 쓰고 내 패키지만 작업 트리에서 컴파일하는 개인 스크립트로 JDK 6u45 에서 실행했다. (`pkgtest.sh bie "js/builtins/typed" "js/builtins/typed"` 도 JS-CORE 가 안정되면 같은 결과여야 한다.)

## 작업 기록 (최신이 위)


### 2026-11-16 · P14-06 스크립트 수준 시험 · 최종

```text
현재 작업 번호: P14-06 최종 확인
완료한 내용: JsRealm.create()(표준 realm, StandardBuiltins 가 TypedLibInstaller 를 이름으로 적재) + JsRealm.evaluateScript 로 TypedArrayScriptTest 작성
변경 파일: tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/TypedArrayScriptTest.java, docs/progress/js-builtins-e.md
검증 결과: JDK 6u45, scripts/pkgtest.sh bie "js/builtins/typed" "js/builtins/typed"(작업 트리 전체 빌드) → 87 passed, 0 failed, 2 skipped(이유 있음), tools/sourcecheck 0 위반.
  스크립트로 확인한 것: 값 변환(Int8/Uint8/Uint8Clamped/Float32), Object.prototype.toString 태그, 대괄호 접근과 정규 숫자 키, for-in/Object.keys, 프로퍼티 기술자,
  for-of/전개/구조 분해/entries(), %TypedArray% 상속 관계, from/of, 클래스 `extends Uint8Array`(map 결과가 하위 클래스 인스턴스), map/filter/reduce/sort/fill/reverse/subarray/copyWithin/set(겹침),
  ArrayBuffer.slice, DataView 엔디언, 오류 종류(RangeError/TypeError)와 Object.freeze 동작.
건너뜀(이유 포함): testArrayFromAcceptsTypedArrays — Array.from 이 아직 없음(StringArray 작업자 영역), testJsonStringifyOfTypedArray — JSON 이 표준 realm 에 아직 연결되지 않음(datajson 설치기 미연결).
  두 함수가 생기면 같은 시험이 자동으로 실제 검사를 수행한다.
미해결 사항: '규격과의 차이와 가정' 절, 이터레이터 프로토타입 공유(조정자 요청).
다음 작업 번호: 없음. 
체크박스 후보: P14-06 (ArrayBuffer/DataView/TypedArray 구현과 Java·스크립트 수준 검증 완료).
                P14-09 는 내 객체(ArrayBuffer, DataView, %TypedArray%, 구체 생성자 9종)에 대한 이름/length/속성 검사만 완료 — 전체 P14-09 후보 아님(다른 내장 객체와 Annex B 추적은 조정자/D 작업자).
```

### 2026-11-16 · P14-06 (2/2) · %TypedArray%, 아홉 개 형식화 배열 생성자, P14-09 검사

```text
현재 작업 번호: P14-06 (형식화 배열 완료), P14-09 (내 객체의 이름/length/속성 표 검사 완료)
완료한 내용: TypedArrayObject(정수 인덱스 이국적 객체: 정규 숫자 문자열, [[Get]]/[[Set]]/[[Has]]/[[GetOwnProperty]]/[[DefineOwnProperty]]/[[Delete]]/[[OwnPropertyKeys]]),
             TypedArrayFactory(생성 형태: 인수 없음/길이/형식화 배열 복사(버퍼 species)/ArrayBuffer+위치+길이(정렬·범위 RangeError)/이터러블/유사 배열),
             TypedArrayLib(%TypedArray%: from, of, @@species, 구체 생성자 9개, BYTES_PER_ELEMENT, 중간 프로토타입),
             TypedArrayMethods(접근자 5개 + 메서드 24개, species 생성, 안정 병합 정렬, 겹치는 set/copyWithin),
             TypedArrayIterator(keys/values/entries/@@iterator 이터레이터), TypedArrays(다른 부분용 공개 Java API)
변경 파일: src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/{ElementType,TypedArrayObject,TypedArrayFactory,TypedArrayLib,TypedArrayMethods,TypedArrayIterator,TypedArrays,TypedSupport,TypedLibInstaller}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/{TypedArrayConstructorTest,TypedArrayExoticTest,TypedArrayMethodsTest,TypedArrayStaticsTest,BuiltinPropertiesTest}.java
검증 결과: JDK 6u45, 개인 스크립트(마지막 commit 스냅샷 + 내 패키지) → 80 passed(기반 시설 6 포함), 0 failed. tools/sourcecheck 0 위반.
           표 기반 검사(BuiltinPropertiesTest)가 ArrayBuffer/DataView/%TypedArray%/구체 생성자와 프로토타입의 자체 키 집합, 모든 함수의 이름/length/생성자 여부/속성을 확인한다.
미해결 사항: 아래 '규격과의 차이'. 스크립트 수준 시험은 JsRealm.evaluateScript 로 별도 추가 예정(작업 트리에는 있으나 JS-CORE 가 아직 commit 하지 않음).
다음 작업 번호: 없음(할당된 범위 끝). 스크립트 수준 시험 추가 후 최종 보고.
```
### 2026-11-16 · P14-06 (1/2) · ArrayBuffer, DataView

```text
현재 작업 번호: P14-06 (ArrayBuffer, DataView 완료 → 다음 형식화 배열)
완료한 내용: ElementType(9개 요소 형식, 엔디언 지정 읽기/쓰기, ToUint8Clamp), ArrayBufferObject(byte[] 저장소, detach), DataViewObject,
             ArrayBufferLib(생성자, isView, @@species getter, byteLength getter, slice(species 검증), @@toStringTag),
             DataViewLib(생성자 길이 3, buffer/byteLength/byteOffset getter, get/set Int8~Float64 16개, 엔디언 인수, @@toStringTag),
             TypedSupport(ToIndex, 상대 인덱스, ValidateTypedArray, SpeciesConstructor, TypedArrayCreate/SpeciesCreate, 버퍼 할당),
             TypedLibInstaller(ArrayBuffer, DataView 설치. 형식화 배열은 다음 작업)
변경 파일: src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/{package-info,ElementType,ArrayBufferObject,DataViewObject,TypedArrayObject,TypedSupport,ArrayBufferLib,DataViewLib,TypedLibInstaller}.java,
           tests/src/org/duckdns/hjow/swingwebpanel/js/builtins/typed/{TypedTestSupport,ArrayBufferTest,DataViewTest}.java
검증 결과: JDK 6u45, 개인 스크립트 → 25 passed(기반 시설 6 포함), 0 failed. tools/sourcecheck 0 위반.
미해결 사항: 스크립트 수준 시험은 JsRealm.evaluateScript 가 생긴 뒤에 추가(아직 없음). 이 단계의 시험은 Java 수준(Ops.call/invoke).
다음 작업 번호: P14-06 (2/2) 형식화 배열
```

### 가정과 규격 대조 (ES2015 본문을 읽을 수 없어 지식에 따름)

- 길이/위치 인수는 **ToIndex**(ES2017, 브라우저 동작)로 변환한다: undefined/NaN → 0, 소수는 버림, 음수와 2^53-1 초과는 RangeError.
  ES2015 본문은 소수와 NaN 을 RangeError 로 거부하므로, 그런 입력을 받는 ES2015 적합성 사례가 있다면 차이가 난다(의도적 선택, 브라우저 호환).
- ArrayBuffer 할당 한계는 `Integer.MAX_VALUE - 8` 바이트이며 그보다 크거나 메모리가 부족하면 RangeError("Array buffer allocation failed").
- 분리(detach)는 Java API(`ArrayBufferObject.detach()`)로만 가능하다. 분리된 버퍼 위의 뷰/배열 메서드는 TypeError(ES2015 의 ValidateTypedArray), 요소 읽기는 undefined, 길이는 0.

### 규격과의 차이와 가정 (ES2015 본문을 읽을 수 없어 지식에 따름)

1. **ToIndex(ES2017/브라우저)**: ArrayBuffer/DataView/형식화 배열의 길이·위치 인수는 소수를 버리고 undefined/NaN 을 0 으로 본다. ES2015 본문은 `ToLength` 와 `SameValueZero` 로 소수/NaN 을 RangeError 로 거부한다.
2. **범위 밖 쓰기**: `ta[100] = 1`, `ta['-1'] = 1` 은 오류 없이 무시한다([[Set]] 이 true). ES2015 본문은 IntegerIndexedElementSet 이 false 를 돌려주어 엄격 모드에서 TypeError 이다(ES2017+ 와 브라우저 동작을 따름).
   분리된 버퍼의 요소 읽기는 TypeError 대신 undefined(ES2017+)이고 메서드는 ValidateTypedArray 로 TypeError(ES2015)이다.
3. **이터레이터 프로토타입**: 배열과 `%ArrayIteratorPrototype%` 를 공유하지 않고 `%TypedArrayIteratorPrototype%`(`%IteratorPrototype%` 상속, `next`, toStringTag "Array Iterator")를 쓴다.
   코어가 다른 객체용 배열 이터레이터 생성 API 를 제공하면 `TypedArrayIterator.create` 한 곳만 바꾸면 된다. **조정자 요청**: 코어가 `%ArrayIteratorPrototype%` 를 만들 때 임의의 `JsObject` 와 종류(keys/values/entries)로 배열 이터레이터를 만드는 공개 팩터리를 제공해 달라.
4. **엔디언**: 형식화 배열은 리틀 엔디언으로 저장(플랫폼 엔디언은 구현 정의). DataView 는 호출마다 지정. NaN 은 표준 비트 패턴으로 저장(페이로드 비보존).
5. **ES2016 `includes`** 는 ES2015 에 없으므로 설치하지 않았다(확장 항목 ESX 후보). `TypedArray.prototype.toString` 은 설치 시점의 `Array.prototype.toString` 과 같은 함수 객체이며, 아직 없으면 같은 동작(join 호출)의 함수를 만든다.
6. **크기 한계**: ArrayBuffer 최대 `Integer.MAX_VALUE - 8` 바이트(Java 배열 한계). 초과하거나 메모리가 부족하면 RangeError. 형식화 배열 길이는 int 이내.
7. 분리(detach)는 Java API(`ArrayBufferObject.detach()`)로만 가능(구조화 복제 전달은 P20-09 Worker 구현에서 사용 예정).

### Public API (다른 부분용, `org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays`)

```java
TypedLibInstaller installer = new TypedLibInstaller();          // BuiltinInstaller. StandardBuiltins 가 이름으로 만들어 install(realm) 호출
ArrayBufferObject buf = TypedArrays.newArrayBuffer(realm, byte[] data);                       // 복사 없이 감쌈(저장소 공유)
ArrayBufferObject cpy = TypedArrays.newArrayBufferCopy(realm, byte[] data, int off, int len);  // 구간 복사
TypedArrayObject u8  = TypedArrays.newUint8Array(realm, buf, byteOffset, length);             // 복사 없는 뷰
TypedArrayObject u8b = TypedArrays.newUint8Array(realm, byte[] data);                         // byte[] 전체를 감싼 Uint8Array
TypedArrayObject c8  = TypedArrays.newUint8ClampedArray(realm, buf, byteOffset, length);      // 캔버스 ImageData.data 용
TypedArrayObject f32 = TypedArrays.newFloat32Array(realm, buf, byteOffset, length);
TypedArrayObject any = TypedArrays.newTypedArray(realm, ElementType.INT16, buf, byteOffset, length);  // 정렬/범위 검사, 위반 시 IllegalArgumentException
TypedArrays.ByteView v = TypedArrays.bytesOf(jsValue);   // ArrayBuffer/형식화 배열/DataView 의 바이트 구간(array, offset, length), 아니거나 분리되면 null. 배열은 스크립트와 공유
TypedArrays.isArrayBuffer(v) / isTypedArray(v) / isDataView(v)
```

- 요소 접근(Java): `TypedArrayObject.getElement(int)` / `setElement(int, double)` (올바른 인덱스만), `getLength()`, `getByteOffset()`, `getBuffer()`, `getType()`.
- 스레드: 이 객체들은 문서 소유 스레드에서만 만들고 바꾼다(JS 엔진과 같은 규칙). 분리/크기 변경은 없다.
