package org.duckdns.hjow.swingwebpanel.js.builtins.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * The expected ECMAScript built-in property shapes from ECMA-262, 17th edition, chapters 19 to 28 and Annex B.2.
 * {@link MetaConformanceTest} checks property attributes, callable names and lengths, alias identity, and prototype chains.
 * <p>
 * Line format: <code>path: kind item item ...</code> where an item is <code>name</code>, <code>name=length</code> or (for tags)
 * <code>name=value</code>. Paths are dotted; <code>@@iterator</code> names a well-known symbol; the root <code>global</code> is the global object,
 * <code>%Name%</code> is a named intrinsic and <code>%TypedArray%</code> the parent constructor of the typed array constructors. Kinds:
 * <code>ctor</code> (constructor with a <code>prototype</code>), <code>ctor0</code> (constructor without prototype), <code>fn</code>
 * (function that is not a constructor), <code>getter</code> (accessor with a getter named "get x" and no setter), <code>const</code>
 * (read-only data), <code>tag</code> (@@toStringTag string), <code>val</code> (writable data), <code>obj</code> (object valued data),
 * <code>rfn</code> (function, read-only but configurable), <code>lfn</code> (function with all attributes false), <code>accessor</code> (getter and setter),
 * <code>robj</code> (object valued, read-only, configurable), <code>size</code> (writable numeric data with fixed attributes),
 * <code>link</code> (<code>a=b</code>: the property a is the same value as the path b), <code>chain</code> (<code>a=b</code>:
 * the prototype of a is b), <code>poison</code> (a restricted accessor whose two functions are %ThrowTypeError%).
 * <p>
 * ECMA-262 제17판 19~28장과 부속서 B.2에 따른 ECMAScript 내장 프로퍼티 표.
 * {@link MetaConformanceTest}가 프로퍼티의 기술자, 함수 이름과 길이, 별칭과 프로토타입 사슬을 확인한다.
 */
final class ConformanceTable {

    /**
     * Private constructor: static data holder.
     * <p>
     * 비공개 생성자: 정적 자료 보관 클래스이다.
     */
    private ConformanceTable() {
    }

    /**
     * The table lines.
     * <p>
     * 표의 줄.
     */
    static final String[] LINES = {
        // ---- 19 The Global Object (and Annex B.2.1)
        "global: const NaN Infinity undefined",
        "global: link globalThis=global",
        "global: fn eval=1 isFinite=1 isNaN=1 parseFloat=1 parseInt=2 decodeURI=1 decodeURIComponent=1 encodeURI=1 encodeURIComponent=1 escape=1 unescape=1",
        "global: ctor Object=1 Function=1 Boolean=1 Symbol=0 Error=1 EvalError=1 RangeError=1 ReferenceError=1 SyntaxError=1 TypeError=1 URIError=1"
                + " AggregateError=2 Number=1 BigInt=1 String=1 RegExp=2 Date=7 Array=1 Map=0 Set=0 WeakMap=0 WeakSet=0 ArrayBuffer=1 DataView=3 Promise=1"
                + " WeakRef=1 FinalizationRegistry=1 SharedArrayBuffer=1 Iterator=0"
                + " Int8Array=3 Uint8Array=3 Uint8ClampedArray=3 Int16Array=3 Uint16Array=3 Int32Array=3 Uint32Array=3"
                + " Float16Array=3 Float32Array=3 Float64Array=3 BigInt64Array=3 BigUint64Array=3",
        "global: ctor0 Proxy=2",
        "global: obj Math JSON Reflect Atomics",

        // ---- 20.1 Object (+ B.2.2)
        "Object: fn assign=2 create=2 defineProperties=2 defineProperty=3 freeze=1 getOwnPropertyDescriptor=2 getOwnPropertyNames=1"
                + " entries=1 fromEntries=1 getOwnPropertyDescriptors=1 getOwnPropertySymbols=1 getPrototypeOf=1 hasOwn=2 is=2 isExtensible=1"
                + " isFrozen=1 isSealed=1 keys=1 preventExtensions=1 seal=1 setPrototypeOf=2 values=1 groupBy=2",
        "Object.prototype: fn hasOwnProperty=1 isPrototypeOf=1 propertyIsEnumerable=1 toLocaleString=0 toString=0 valueOf=0"
                + " __defineGetter__=2 __defineSetter__=2 __lookupGetter__=1 __lookupSetter__=1",
        "Object.prototype: accessor __proto__",
        "Object.prototype: val constructor",

        // ---- 20.2 Function
        "Function.prototype: fn apply=2 bind=1 call=1 toString=0",
        "Function.prototype: lfn @@hasInstance=1",
        "Function.prototype: poison caller arguments",
        "Function.prototype: val constructor",

        // ---- 20.3 Boolean, 20.4 Symbol, 20.5 Error
        "Boolean.prototype: fn toString=0 valueOf=0",
        "Symbol: fn for=1 keyFor=1",
        "Symbol: const hasInstance isConcatSpreadable iterator asyncIterator match matchAll replace search species split toPrimitive toStringTag unscopables",
        "Symbol.prototype: fn toString=0 valueOf=0",
        "Symbol.prototype: getter description",
        "Symbol.prototype: rfn @@toPrimitive=1",
        "Symbol.prototype: tag @@toStringTag=Symbol",
        "Error: fn isError=1",
        "Error.prototype: fn toString=0",
        "Error.prototype: val name message",
        "EvalError.prototype: val name message",
        "RangeError.prototype: val name message",
        "ReferenceError.prototype: val name message",
        "SyntaxError.prototype: val name message",
        "TypeError.prototype: val name message",
        "URIError.prototype: val name message",
        "AggregateError.prototype: val name message",

        // ---- 21 Numbers and Dates
        "Number: const EPSILON MAX_SAFE_INTEGER MAX_VALUE MIN_SAFE_INTEGER MIN_VALUE NaN NEGATIVE_INFINITY POSITIVE_INFINITY",
        "Number: fn isFinite=1 isInteger=1 isNaN=1 isSafeInteger=1",
        "Number: link parseFloat=global.parseFloat parseInt=global.parseInt",
        "Number.prototype: fn toExponential=1 toFixed=1 toLocaleString=0 toPrecision=1 toString=1 valueOf=0",
        "BigInt: fn asIntN=2 asUintN=2",
        "BigInt.prototype: fn toLocaleString=0 toString=0 valueOf=0",
        "BigInt.prototype: tag @@toStringTag=BigInt",
        "Math: const E LN10 LN2 LOG10E LOG2E PI SQRT1_2 SQRT2",
        "Math: fn abs=1 acos=1 acosh=1 asin=1 asinh=1 atan=1 atanh=1 atan2=2 cbrt=1 ceil=1 clz32=1 cos=1 cosh=1 exp=1 expm1=1 floor=1 fround=1"
                + " hypot=2 imul=2 log=1 log1p=1 log10=1 log2=1 max=2 min=2 pow=2 random=0 round=1 sign=1 sin=1 sinh=1 sqrt=1 tan=1 tanh=1 trunc=1"
                + " f16round=1 sumPrecise=1",
        "Math: tag @@toStringTag=Math",
        "Date: fn now=0 parse=1 UTC=7",
        "Date.prototype: fn getDate=0 getDay=0 getFullYear=0 getHours=0 getMilliseconds=0 getMinutes=0 getMonth=0 getSeconds=0 getTime=0"
                + " getTimezoneOffset=0 getUTCDate=0 getUTCDay=0 getUTCFullYear=0 getUTCHours=0 getUTCMilliseconds=0 getUTCMinutes=0 getUTCMonth=0"
                + " getUTCSeconds=0 setDate=1 setFullYear=3 setHours=4 setMilliseconds=1 setMinutes=3 setMonth=2 setSeconds=2 setTime=1 setUTCDate=1"
                + " setUTCFullYear=3 setUTCHours=4 setUTCMilliseconds=1 setUTCMinutes=3 setUTCMonth=2 setUTCSeconds=2 toDateString=0 toISOString=0"
                + " toJSON=1 toLocaleDateString=0 toLocaleString=0 toLocaleTimeString=0 toString=0 toTimeString=0 toUTCString=0 valueOf=0"
                + " getYear=0 setYear=1",
        "Date.prototype: rfn @@toPrimitive=1",
        "Date.prototype: link toGMTString=Date.prototype.toUTCString",

        // ---- 22 Text Processing
        "String: fn fromCharCode=1 fromCodePoint=1 raw=1",
        "String.prototype: const length=0",
        "String.prototype: fn charAt=1 charCodeAt=1 codePointAt=1 concat=1 endsWith=1 includes=1 indexOf=1 lastIndexOf=1 localeCompare=1 match=1"
                + " normalize=0 repeat=1 replace=2 search=1 slice=2 split=2 startsWith=1 substring=2 toLocaleLowerCase=0 toLocaleUpperCase=0"
                + " toLowerCase=0 toString=0 toUpperCase=0 trim=0 valueOf=0 at=1 matchAll=1 replaceAll=2 @@iterator=0"
                + " isWellFormed=0 toWellFormed=0 padEnd=1 padStart=1 trimEnd=0 trimStart=0"
                + " anchor=1 big=0 blink=0 bold=0 fixed=0 fontcolor=1 fontsize=1 italics=0 link=1 small=0 strike=0 sub=0 substr=2 sup=0",
        "String.prototype: link trimLeft=String.prototype.trimStart trimRight=String.prototype.trimEnd",
        "RegExp: getter @@species",
        "RegExp: fn escape=1",
        "RegExp.prototype: fn exec=1 test=1 toString=0 compile=2 @@match=1 @@matchAll=1 @@replace=2 @@search=1 @@split=2",
        "RegExp.prototype: getter dotAll flags global hasIndices ignoreCase multiline source sticky unicode unicodeSets",

        // ---- 23 Indexed Collections
        "Array: fn from=1 fromAsync=1 isArray=1 of=0",
        "Array: getter @@species",
        "Array.prototype: size length=0",
        "Array.prototype: fn concat=1 copyWithin=2 entries=0 every=1 fill=1 filter=1 find=1 findIndex=1 forEach=1 indexOf=1 join=1 keys=0"
                + " lastIndexOf=1 map=1 pop=0 push=1 reduce=1 reduceRight=1 reverse=0 shift=0 slice=2 some=1 sort=1 splice=2 toLocaleString=0"
                + " toString=0 unshift=1 values=0 at=1 flat=0 flatMap=1 findLast=1 findLastIndex=1 includes=1"
                + " toReversed=0 toSorted=1 toSpliced=2 with=2",
        "Array.prototype: link @@iterator=Array.prototype.values",
        "Array.prototype: robj @@unscopables",
        "%TypedArray%: fn from=1 of=0",
        "%TypedArray%: getter @@species",
        "%TypedArray%.prototype: getter buffer byteLength byteOffset length @@toStringTag",
        "%TypedArray%.prototype: fn copyWithin=2 entries=0 every=1 fill=1 filter=1 find=1 findIndex=1 forEach=1 indexOf=1 join=1 keys=0"
                + " lastIndexOf=1 map=1 reduce=1 reduceRight=1 reverse=0 set=1 slice=2 some=1 sort=1 subarray=2 toLocaleString=0 values=0 at=1"
                + " findLast=1 findLastIndex=1 includes=1 toReversed=0 toSorted=1 with=2",
        "%TypedArray%.prototype: link @@iterator=%TypedArray%.prototype.values toString=Array.prototype.toString",
        "Int8Array: const BYTES_PER_ELEMENT=1",
        "Int8Array.prototype: const BYTES_PER_ELEMENT=1",
        "Uint8Array: const BYTES_PER_ELEMENT=1",
        "Uint8Array.prototype: const BYTES_PER_ELEMENT=1",
        "Uint8ClampedArray: const BYTES_PER_ELEMENT=1",
        "Uint8ClampedArray.prototype: const BYTES_PER_ELEMENT=1",
        "Int16Array: const BYTES_PER_ELEMENT=2",
        "Int16Array.prototype: const BYTES_PER_ELEMENT=2",
        "Uint16Array: const BYTES_PER_ELEMENT=2",
        "Uint16Array.prototype: const BYTES_PER_ELEMENT=2",
        "Int32Array: const BYTES_PER_ELEMENT=4",
        "Int32Array.prototype: const BYTES_PER_ELEMENT=4",
        "Uint32Array: const BYTES_PER_ELEMENT=4",
        "Uint32Array.prototype: const BYTES_PER_ELEMENT=4",
        "Float16Array: const BYTES_PER_ELEMENT=2",
        "Float16Array.prototype: const BYTES_PER_ELEMENT=2",
        "Float32Array: const BYTES_PER_ELEMENT=4",
        "Float32Array.prototype: const BYTES_PER_ELEMENT=4",
        "Float64Array: const BYTES_PER_ELEMENT=8",
        "Float64Array.prototype: const BYTES_PER_ELEMENT=8",
        "BigInt64Array: const BYTES_PER_ELEMENT=8",
        "BigInt64Array.prototype: const BYTES_PER_ELEMENT=8",
        "BigUint64Array: const BYTES_PER_ELEMENT=8",
        "BigUint64Array.prototype: const BYTES_PER_ELEMENT=8",
        "Uint8Array: fn fromBase64=1 fromHex=1",
        "Uint8Array.prototype: fn setFromBase64=1 setFromHex=1 toBase64=0 toHex=0",

        // ---- 24 Keyed Collections
        "Map: getter @@species",
        "Map: fn groupBy=2",
        "Map.prototype: fn clear=0 delete=1 entries=0 forEach=1 get=1 getOrInsert=2 getOrInsertComputed=2 has=1 keys=0 set=2 values=0",
        "Map.prototype: getter size",
        "Map.prototype: link @@iterator=Map.prototype.entries",
        "Map.prototype: tag @@toStringTag=Map",
        "Set: getter @@species",
        "Set.prototype: fn add=1 clear=0 delete=1 difference=1 entries=0 forEach=1 has=1 intersection=1 isDisjointFrom=1"
                + " isSubsetOf=1 isSupersetOf=1 symmetricDifference=1 union=1 values=0",
        "Set.prototype: getter size",
        "Set.prototype: link @@iterator=Set.prototype.values keys=Set.prototype.values",
        "Set.prototype: tag @@toStringTag=Set",
        "WeakMap.prototype: fn delete=1 get=1 getOrInsert=2 getOrInsertComputed=2 has=1 set=2",
        "WeakMap.prototype: tag @@toStringTag=WeakMap",
        "WeakSet.prototype: fn add=1 delete=1 has=1",
        "WeakSet.prototype: tag @@toStringTag=WeakSet",

        // ---- 25 Structured Data
        "ArrayBuffer: fn isView=1",
        "ArrayBuffer: getter @@species",
        "ArrayBuffer.prototype: getter byteLength detached maxByteLength resizable",
        "ArrayBuffer.prototype: fn resize=1 slice=2 transfer=0 transferToFixedLength=0",
        "ArrayBuffer.prototype: tag @@toStringTag=ArrayBuffer",
        "SharedArrayBuffer: getter @@species",
        "SharedArrayBuffer.prototype: getter byteLength growable maxByteLength",
        "SharedArrayBuffer.prototype: fn grow=1 slice=2",
        "SharedArrayBuffer.prototype: tag @@toStringTag=SharedArrayBuffer",
        "DataView.prototype: getter buffer byteLength byteOffset",
        "DataView.prototype: fn getBigInt64=1 getBigUint64=1 getFloat16=1 getFloat32=1 getFloat64=1 getInt16=1 getInt32=1 getInt8=1"
                + " getUint16=1 getUint32=1 getUint8=1 setBigInt64=2 setBigUint64=2 setFloat16=2 setFloat32=2 setFloat64=2"
                + " setInt16=2 setInt32=2 setInt8=2 setUint16=2 setUint32=2 setUint8=2",
        "DataView.prototype: tag @@toStringTag=DataView",
        "Atomics: fn add=3 and=3 compareExchange=4 exchange=3 isLockFree=1 load=2 notify=3 or=3 store=3 sub=3 wait=4 waitAsync=4 xor=3",
        "Atomics: tag @@toStringTag=Atomics",
        "JSON: fn isRawJSON=1 parse=2 rawJSON=1 stringify=3",
        "JSON: tag @@toStringTag=JSON",

        // ---- 26 Managing Memory
        "WeakRef.prototype: fn deref=0",
        "WeakRef.prototype: tag @@toStringTag=WeakRef",
        "FinalizationRegistry.prototype: fn register=2 unregister=1",
        "FinalizationRegistry.prototype: tag @@toStringTag=FinalizationRegistry",

        // ---- 27 Control Abstraction Objects
        "Iterator: fn concat=0 from=1",
        "%IteratorPrototype%: accessor constructor @@toStringTag",
        "%IteratorPrototype%: fn @@iterator=0 drop=1 every=1 filter=1 find=1 flatMap=1 forEach=1 map=1 reduce=1 some=1 take=1 toArray=0",
        "%WrapForValidIteratorPrototype%: fn next=0 return=0",
        "%IteratorHelperPrototype%: fn next=0 return=0",
        "%IteratorHelperPrototype%: tag @@toStringTag=Iterator Helper",
        "Promise: fn all=1 allSettled=1 any=1 race=1 reject=1 resolve=1 try=1 withResolvers=0",
        "Promise: getter @@species",
        "Promise.prototype: fn catch=1 finally=1 then=2",
        "Promise.prototype: tag @@toStringTag=Promise",
        "%ArrayIteratorPrototype%: fn next=0",
        "%ArrayIteratorPrototype%: tag @@toStringTag=Array Iterator",
        "%StringIteratorPrototype%: fn next=0",
        "%StringIteratorPrototype%: tag @@toStringTag=String Iterator",
        "%RegExpStringIteratorPrototype%: fn next=0",
        "%RegExpStringIteratorPrototype%: tag @@toStringTag=RegExp String Iterator",
        "%MapIteratorPrototype%: fn next=0",
        "%MapIteratorPrototype%: tag @@toStringTag=Map Iterator",
        "%SetIteratorPrototype%: fn next=0",
        "%SetIteratorPrototype%: tag @@toStringTag=Set Iterator",
        "%GeneratorPrototype%: fn next=1 return=1 throw=1",
        "%GeneratorPrototype%: tag @@toStringTag=Generator",
        "%GeneratorFunction.prototype%: tag @@toStringTag=GeneratorFunction",

        // ---- 28 Reflection
        "Proxy: fn revocable=2",
        "Reflect: fn apply=3 construct=2 defineProperty=3 deleteProperty=2 get=2 getOwnPropertyDescriptor=2 getPrototypeOf=1 has=2"
                + " isExtensible=1 ownKeys=1 preventExtensions=1 set=3 setPrototypeOf=2",
        "Reflect: tag @@toStringTag=Reflect",

        // ---- prototype chains (6.1.7.4 and the clauses of each built-in)
        "Object.prototype: chain Object.prototype=null",
        "global: chain Function.prototype=Object.prototype Array.prototype=Object.prototype String.prototype=Object.prototype"
                + " Number.prototype=Object.prototype BigInt.prototype=Object.prototype Boolean.prototype=Object.prototype"
                + " Symbol.prototype=Object.prototype Date.prototype=Object.prototype"
                + " RegExp.prototype=Object.prototype Error.prototype=Object.prototype Map.prototype=Object.prototype Set.prototype=Object.prototype"
                + " Promise.prototype=Object.prototype ArrayBuffer.prototype=Object.prototype SharedArrayBuffer.prototype=Object.prototype"
                + " DataView.prototype=Object.prototype WeakMap.prototype=Object.prototype WeakSet.prototype=Object.prototype"
                + " WeakRef.prototype=Object.prototype FinalizationRegistry.prototype=Object.prototype"
                + " %TypedArray%.prototype=Object.prototype %IteratorPrototype%=Object.prototype",
        "global: chain EvalError.prototype=Error.prototype RangeError.prototype=Error.prototype ReferenceError.prototype=Error.prototype"
                + " SyntaxError.prototype=Error.prototype TypeError.prototype=Error.prototype URIError.prototype=Error.prototype"
                + " AggregateError.prototype=Error.prototype",
        "global: chain EvalError=Error RangeError=Error ReferenceError=Error SyntaxError=Error TypeError=Error URIError=Error"
                + " AggregateError=Error Object=Function.prototype Function=Function.prototype Array=Function.prototype"
                + " Boolean=Function.prototype Symbol=Function.prototype Error=Function.prototype Number=Function.prototype BigInt=Function.prototype"
                + " Date=Function.prototype String=Function.prototype RegExp=Function.prototype"
                + " Promise=Function.prototype Proxy=Function.prototype Map=Function.prototype Set=Function.prototype"
                + " WeakMap=Function.prototype WeakSet=Function.prototype ArrayBuffer=Function.prototype SharedArrayBuffer=Function.prototype"
                + " DataView=Function.prototype %TypedArray%=Function.prototype WeakRef=Function.prototype FinalizationRegistry=Function.prototype"
                + " Iterator=Function.prototype",
        "global: chain Int8Array=%TypedArray% Uint8Array=%TypedArray% Uint8ClampedArray=%TypedArray% Int16Array=%TypedArray% Uint16Array=%TypedArray%"
                + " Int32Array=%TypedArray% Uint32Array=%TypedArray% Float16Array=%TypedArray% Float32Array=%TypedArray%"
                + " Float64Array=%TypedArray% BigInt64Array=%TypedArray% BigUint64Array=%TypedArray%"
                + " Int8Array.prototype=%TypedArray%.prototype Uint8Array.prototype=%TypedArray%.prototype"
                + " Uint8ClampedArray.prototype=%TypedArray%.prototype Int16Array.prototype=%TypedArray%.prototype"
                + " Uint16Array.prototype=%TypedArray%.prototype Int32Array.prototype=%TypedArray%.prototype"
                + " Uint32Array.prototype=%TypedArray%.prototype Float16Array.prototype=%TypedArray%.prototype"
                + " Float32Array.prototype=%TypedArray%.prototype Float64Array.prototype=%TypedArray%.prototype"
                + " BigInt64Array.prototype=%TypedArray%.prototype BigUint64Array.prototype=%TypedArray%.prototype",
        "global: chain Math=Object.prototype JSON=Object.prototype Reflect=Object.prototype Atomics=Object.prototype",
        "%ArrayIteratorPrototype%: chain %ArrayIteratorPrototype%=%IteratorPrototype%",
        "%StringIteratorPrototype%: chain %StringIteratorPrototype%=%IteratorPrototype%",
        "%RegExpStringIteratorPrototype%: chain %RegExpStringIteratorPrototype%=%IteratorPrototype%",
        "%MapIteratorPrototype%: chain %MapIteratorPrototype%=%IteratorPrototype%",
        "%SetIteratorPrototype%: chain %SetIteratorPrototype%=%IteratorPrototype%",
        "%IteratorHelperPrototype%: chain %IteratorHelperPrototype%=%IteratorPrototype%",
        "%WrapForValidIteratorPrototype%: chain %WrapForValidIteratorPrototype%=%IteratorPrototype%",
    };

    /**
     * Returns the table lines as a list.
     * <p>
     * 표의 줄을 목록으로 돌려준다.
     *
     * @return the lines / 줄 목록
     */
    static List<String> lines() {
        List<String> out = new ArrayList<String>();
        // 배열의 줄을 순서대로 목록에 옮긴다.
        for (int i = 0; i < LINES.length; i++) {
            out.add(LINES[i]);
        }
        return out;
    }
}
