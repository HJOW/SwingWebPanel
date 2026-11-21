// P13-03: arithmetic, comparison, logical, bitwise, conditional, assignment, property references, delete/typeof/in/instanceof, calls.

// ---- arithmetic
t("add numbers", 1 + 2, 3);
t("add strings", "a" + "b", "ab");
t("add number and string", 1 + "2", "12");
t("add string and number", "1" + 2, "12");
t("add null and undefined", 1 + null, 1);
t("add undefined", 1 + undefined, NaN);
t("add boolean", true + true, 2);
t("add array and number", [1, 2] + 3, "1,23");
t("add object", ({}) + "", "[object Object]");
t("subtract strings", "5" - "2", 3);
t("multiply", 3 * "4", 12);
t("divide", 7 / 2, 3.5);
t("divide by zero", 1 / 0, Infinity);
t("negative divide by zero", -1 / 0, -Infinity);
t("zero divide zero", 0 / 0, NaN);
t("remainder", 7 % 3, 1);
t("negative remainder", -7 % 3, -1);
t("remainder sign follows dividend", 7 % -3, 1);
t("remainder of fractions", 5.5 % 2, 1.5);
t("remainder by zero", 5 % 0, NaN);
t("remainder with infinity", 5 % Infinity, 5);
t("negative zero remainder", 1 / (-4 % 2), -Infinity);
t("unary minus", -"3", -3);
t("unary plus converts", +"  42  ", 42);
t("unary plus on empty string", +"", 0);
t("unary plus on invalid", +"x", NaN);
t("unary plus on array", +[5], 5);
t("negative zero", 1 / -0, -Infinity);
t("zero minus zero", 1 / (0 - 0), Infinity);
t("multiply zero by negative", 1 / (0 * -1), -Infinity);
t("hex literal", 0xff, 255);
t("octal literal", 0o17, 15);
t("binary literal", 0b101, 5);
t("legacy octal literal", 017, 15);
t("exponent literal", 1e3, 1000);
t("large precision", 0.1 + 0.2, 0.30000000000000004);
t("increment", (function () { var a = 1; return a++ + ++a; })(), 4);
t("decrement", (function () { var a = 5; return a-- - --a; })(), 2);
t("postfix on string", (function () { var s = "5"; s++; return s; })(), 6);
t("postfix returns old numeric value", (function () { var s = "5"; return s++; })(), 5);
t("increment undefined", (function () { var u; u++; return u; })(), NaN);

// ---- comparison
t("less", 1 < 2, true);
t("greater equal", 2 >= 2, true);
t("string comparison", "a" < "b", true);
t("string comparison by code units", "Z" < "a", true);
t("longer string greater", "ab" > "a", true);
t("numeric string vs number", "10" > 9, true);
t("both strings are not numeric", "10" > "9", false);
t("NaN comparisons", NaN < 1, false);
t("undefined comparison", undefined < 1, false);
t("null comparison", null >= 0, true);
t("object comparison uses toPrimitive", ({ valueOf: function () { return 3; } }) > 2, true);
t("comparison evaluation order left first", (function () { var s = ""; ({ valueOf: function () { s += "L"; return 1; } }) < ({ valueOf: function () { s += "R"; return 2; } }); return s; })(), "LR");
t("greater swaps toPrimitive order back", (function () { var s = ""; ({ valueOf: function () { s += "L"; return 1; } }) > ({ valueOf: function () { s += "R"; return 2; } }); return s; })(), "LR");

// ---- equality
t("loose number string", 1 == "1", true);
t("loose null undefined", null == undefined, true);
t("loose null zero", null == 0, false);
t("loose boolean", true == 1, true);
t("loose boolean string", false == "", true);
t("loose NaN", NaN == NaN, false);
t("loose object toPrimitive", ({ toString: function () { return "x"; } }) == "x", true);
t("loose objects are identity", ({}) == ({}), false);
t("loose array to string", [1, 2] == "1,2", true);
t("loose empty array", [] == false, true);
t("loose symbol", Symbol.iterator == Symbol.iterator, true);
t("strict number string", 1 === "1", false);
t("strict null undefined", null === undefined, false);
t("strict NaN", NaN === NaN, false);
t("strict zeros", 0 === -0, true);
t("strict object identity", (function () { var o = {}; return o === o; })(), true);
t("not equal", 1 != 2, true);
t("not strictly equal", "1" !== 1, true);
t("strict equal strings", "ab" === "a" + "b", true);

// ---- logical
t("and returns first falsy", 0 && 5, 0);
t("and returns last", 1 && 5, 5);
t("or returns first truthy", 0 || 5, 5);
t("or returns last", 0 || "", "");
t("not", !"", true);
t("double not", !!"a", true);
t("or short circuits", (function () { var c = 0; true || c++; return c; })(), 0);
t("and short circuits", (function () { var c = 0; false && c++; return c; })(), 0);
t("conditional picks branch", true ? "a" : "b", "a");
t("conditional evaluates one branch", (function () { var c = 0; true ? c++ : c--; return c; })(), 1);
t("nested conditional", (1 ? 0 ? "a" : "b" : "c"), "b");
t("comma", (1, 2, 3), 3);
t("void", void 0, undefined);
t("void evaluates operand", (function () { var c = 0; void c++; return c; })(), 1);
t("falsy values", !NaN && !null && !undefined && !"" && !0 && !-0, true);
t("truthy objects", !!{} && !![] && !!function () { } && !!"0" && !!"false", true);

// ---- bitwise
t("and", 5 & 3, 1);
t("or", 5 | 3, 7);
t("xor", 5 ^ 3, 6);
t("not", ~5, -6);
t("left shift", 1 << 4, 16);
t("shift count is masked", 1 << 33, 2);
t("signed right shift", -16 >> 2, -4);
t("unsigned right shift", -1 >>> 0, 4294967295);
t("unsigned right shift of negative", -16 >>> 28, 15);
t("to int32 wraps", 4294967296 | 0, 0);
t("to int32 of large", 2147483648 | 0, -2147483648);
t("to int32 of fraction", 3.9 | 0, 3);
t("to int32 of negative fraction", -3.9 | 0, -3);
t("to int32 of NaN", NaN | 0, 0);
t("to int32 of infinity", Infinity | 0, 0);
t("to int32 of string", "12" | 0, 12);
t("bitwise on huge", 1e20 | 0, 1661992960);
t("to uint32", (-1) >>> 0, 4294967295);
t("left shift overflow", 1 << 31, -2147483648);

// ---- assignment
t("simple assignment value", (function () { var a; return (a = 3); })(), 3);
t("chained assignment", (function () { var a, b; a = b = 4; return a + b; })(), 8);
t("compound add", (function () { var a = 1; a += 2; return a; })(), 3);
t("compound string", (function () { var a = "x"; a += 1; return a; })(), "x1");
t("compound sub mul div mod", (function () { var a = 20; a -= 2; a *= 2; a /= 4; a %= 5; return a; })(), 4);
t("compound shifts", (function () { var a = 1; a <<= 3; a >>= 1; a >>>= 1; return a; })(), 2);
t("compound bit ops", (function () { var a = 6; a &= 3; a |= 8; a ^= 1; return a; })(), 11);
t("compound on property", (function () { var o = { n: 1 }; o.n += 4; return o.n; })(), 5);
t("compound on element", (function () { var o = [1, 2]; o[1] *= 5; return o[1]; })(), 10);
t("compound evaluates the reference once", (function () { var c = 0, o = { n: 1 }; function g() { c++; return o; } g().n += 1; return c; })(), 1);
t("compound reads the left value before evaluating the right side", (function () { var a = 1; a += (a = 10); return a; })(), 11);
t("property increment", (function () { var o = { n: 1 }; o.n++; ++o.n; return o.n; })(), 3);
t("element increment evaluates key once", (function () { var c = 0, o = [5]; function k() { c++; return 0; } o[k()]++; return c + "," + o[0]; })(), "1,6");
t("assignment to string index is ignored (sloppy)", (function () { var s = "abc"; s[0] = "x"; return s; })(), "abc");
t("assignment returns the assigned value", (function () { var o = { set p(v) { } }; return (o.p = 5); })(), 5);
t("assignment evaluation order: target first", (function () {
    var log = [];
    function t1() { log.push("t"); return { }; }
    function k() { log.push("k"); return "x"; }
    function v() { log.push("v"); return 1; }
    t1()[k()] = v();
    return log.join("");
})(), "tkv");
t("property key is converted after the right side (as Chromium does; ES2015 text converts earlier)", (function () {
    var log = [];
    var key = { toString: function () { log.push("key"); return "p"; } };
    var o = {};
    o[key] = (log.push("rhs"), 1);
    return log.join(",");
})(), "rhs,key");
t("swap through destructuring", (function () { var a = 1, b = 2; [a, b] = [b, a]; return a + "," + b; })(), "2,1");

// ---- property references
t("dot access", ({ a: 1 }).a, 1);
t("bracket access", ({ a: 1 })["a"], 1);
t("computed bracket", (function () { var k = "b"; return ({ b: 2 })[k]; })(), 2);
t("number key converts to string", ({ 1: "x" })[1], "x");
t("array index as string", [10, 20]["1"], 20);
t("missing property", ({}).missing, undefined);
t("access on string primitive", "abc".length, 3);
t("index on string primitive", "abc"[1], "b");
t("access on number primitive", (5).toString(), "5");
t("method on number literal with radix", (255).toString(16), "ff");
t("inherited property", (function () { var p = { a: 1 }, o = Object.create(p); return o.a; })(), 1);
terr("property of undefined", function () { undefined.x; }, "TypeError");
terr("property of null", function () { null.x; }, "TypeError");
terr("call of undefined method", function () { var o = {}; o.f(); }, "TypeError");
terr("assignment to property of undefined", function () { var u; u.x = 1; }, "TypeError");
terr("call of non function", function () { (1)(); }, "TypeError");
t("setting a property on a primitive is ignored (sloppy)", (function () { var n = 5; n.p = 1; return n.p; })(), undefined);
terr("setting a property on a primitive throws (strict)", function () { "use strict"; var n = 5; n.p = 1; }, "TypeError");
terr("writing a read-only property throws (strict)", function () { "use strict"; var o = {}; Object.defineProperty(o, "r", { value: 1 }); o.r = 2; }, "TypeError");
t("writing a read-only property is ignored (sloppy)", (function () { var o = {}; Object.defineProperty(o, "r", { value: 1 }); o.r = 2; return o.r; })(), 1);
terr("getter-only property write throws (strict)", function () { "use strict"; var o = { get g() { return 1; } }; o.g = 2; }, "TypeError");
terr("adding to a frozen object throws (strict)", function () { "use strict"; var o = Object.freeze({}); o.x = 1; }, "TypeError");
t("getter call count", (function () { var c = 0, o = { get g() { c++; return 1; } }; o.g; o.g; return c; })(), 2);
t("getter is called once by compound assignment", (function () { var c = 0, o = { get g() { c++; return 1; }, set g(v) { } }; o.g += 1; return c; })(), 1);
t("setter receives the value", (function () { var r, o = { set s(v) { r = v; } }; o.s = 9; return r; })(), 9);
t("accessor this is the receiver", (function () { var p = { get me() { return this; } }, o = Object.create(p); return o.me === o; })(), true);

// ---- delete, typeof, in, instanceof
t("delete property", (function () { var o = { a: 1 }; var r = delete o.a; return r + "," + ("a" in o); })(), "true,false");
t("delete missing property", delete ({}).x, true);
t("delete non configurable (sloppy)", (function () { var o = {}; Object.defineProperty(o, "p", { value: 1 }); return delete o.p; })(), false);
terr("delete non configurable (strict)", function () { "use strict"; var o = {}; Object.defineProperty(o, "p", { value: 1 }); delete o.p; }, "TypeError");
t("delete array element leaves a hole", (function () { var a = [1, 2, 3]; delete a[1]; return a.length + "," + (1 in a); })(), "3,false");
t("delete of a non reference", delete 5, true);
t("delete variable is false", (function () { var v = 1; return delete v; })(), false);
t("typeof undefined", typeof undefined, "undefined");
t("typeof null", typeof null, "object");
t("typeof boolean", typeof true, "boolean");
t("typeof number", typeof 1, "number");
t("typeof string", typeof "", "string");
t("typeof symbol", typeof Symbol(), "symbol");
t("typeof function", typeof function () { }, "function");
t("typeof class", typeof class { }, "function");
t("typeof object", typeof {}, "object");
t("typeof array", typeof [], "object");
t("typeof arrow", typeof (() => 1), "function");
t("typeof generator", typeof function* () { }, "function");
t("typeof builtin", typeof Object, "function");
t("typeof precedence", typeof 1 + 1, "number1");
t("in operator", "a" in { a: 1 }, true);
t("in operator with prototype", "toString" in {}, true);
t("in operator with array index", 1 in [5, 6], true);
t("in operator with number key", 2 in [5, 6], false);
t("in operator with symbol", Symbol.iterator in [], true);
terr("in operator with primitive", function () { "a" in "abc"; }, "TypeError");
t("instanceof", [] instanceof Array, true);
t("instanceof chain", [] instanceof Object, true);
t("instanceof fails for primitives", 1 instanceof Number, false);
t("instanceof with function", (function () { function F() { } return new F() instanceof F; })(), true);
terr("instanceof non callable", function () { ({}) instanceof 5; }, "TypeError");
terr("instanceof without prototype object", function () { function F() { } F.prototype = 1; ({}) instanceof F; }, "TypeError");
t("instanceof uses Symbol.hasInstance", ({}) instanceof ({ [Symbol.hasInstance]: function (v) { return true; } }), true);
t("instanceof with bound function", (function () { function F() { } var B = F.bind(null); return new F() instanceof B; })(), true);

// ---- evaluation order of operands
t("left operand evaluated first", (function () { var s = ""; function a() { s += "a"; return 1; } function b() { s += "b"; return 2; } a() + b(); return s; })(), "ab");
t("operands evaluated before conversion", (function () {
    var s = "";
    var l = { valueOf: function () { s += "L"; return 1; } }, r = { valueOf: function () { s += "R"; return 2; } };
    function a() { s += "a"; return l; } function b() { s += "b"; return r; }
    a() + b();
    return s;
})(), "abLR");
t("call arguments evaluate left to right", (function () { var s = ""; function f() { } function g(x) { s += x; return x; } f(g(1), g(2), g(3)); return s; })(), "123");
t("callee evaluated before arguments", (function () { var s = ""; function callee() { s += "c"; return function () { }; } function arg() { s += "a"; } callee()(arg()); return s; })(), "ca");
t("member object evaluated before key", (function () { var s = ""; function o() { s += "o"; return [1]; } function k() { s += "k"; return 0; } o()[k()]; return s; })(), "ok");
t("array literal evaluates in order", (function () { var s = ""; function g(x) { s += x; return x; } [g(1), g(2)]; return s; })(), "12");
t("object literal evaluates in order", (function () { var s = ""; function g(x) { s += x; return x; } ({ a: g(1), b: g(2) }); return s; })(), "12");
t("template literal evaluates in order", (function () { var s = ""; function g(x) { s += x; return x; } `${g(1)}${g(2)}`; return s; })(), "12");

// ---- calls and new
t("function call", (function () { function f(a, b) { return a - b; } return f(5, 2); })(), 3);
t("missing arguments are undefined", (function () { function f(a, b) { return typeof b; } return f(1); })(), "undefined");
t("extra arguments are ignored", (function () { function f(a) { return a; } return f(1, 2, 3); })(), 1);
t("method call this", (function () { var o = { v: 1, f: function () { return this.v; } }; return o.f(); })(), 1);
t("bracket call this", (function () { var o = { v: 2, f: function () { return this.v; } }; return o["f"](); })(), 2);
t("detached method loses this (strict)", (function () { var o = { f: function () { "use strict"; return this; } }; var f = o.f; return f(); })(), undefined);
t("parenthesized call keeps this", (function () { var o = { v: 3, f: function () { return this.v; } }; return (o.f)(); })(), 3);
t("comma expression drops this", (function () { var o = { v: 3, f: function () { return this === undefined ? "u" : typeof this.v; } }; return (0, o.f)(); })(), "undefined");
t("new with arguments", (function () { function P(x) { this.x = x; } return new P(7).x; })(), 7);
t("new without parentheses", (function () { function P() { this.y = 1; } return (new P).y; })(), 1);
t("new member expression", (function () { var ns = { P: function () { this.z = 2; } }; return new ns.P().z; })(), 2);
t("new returns constructor object result", (function () { function P() { return { custom: 1 }; } return new P().custom; })(), 1);
t("new ignores primitive result", (function () { function P() { this.k = 1; return 5; } return new P().k; })(), 1);
terr("new of non constructor", function () { new (function () { return 1; }.bind())(); new ({}.x)(); }, "TypeError");
terr("new of arrow", function () { new (() => 1)(); }, "TypeError");
terr("new of method", function () { new ({ m() { } }).m(); }, "TypeError");
t("spread in call", (function () { function f(a, b, c) { return a + b + c; } return f(...[1, 2], 3); })(), 6);
t("spread in new", (function () { function P(a, b) { this.s = a + b; } return new P(...[1, 2]).s; })(), 3);
t("spread of string", (function () { function f() { return arguments.length; } return f(..."abc"); })(), 3);
t("spread in the middle", (function () { function f() { return Array.prototype.join.call(arguments, ""); } return f(0, ...[1, 2], 3); })(), "0123");
t("comma in arguments", (function () { function f(a) { return a; } return f((1, 2)); })(), 2);
t("recursion", (function fib(n) { return n < 2 ? n : fib(n - 1) + fib(n - 2); })(15), 610);
t("call result is not a reference", (function () { function f() { return 1; } return typeof f(); })(), "number");
