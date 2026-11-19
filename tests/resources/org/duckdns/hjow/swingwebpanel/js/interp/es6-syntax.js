// P13-06: object/array literals, shorthand, computed keys, default and rest parameters, spread, destructuring, templates.

// ---- object literals
t("shorthand property", (function () { var a = 1, b = 2; var o = { a, b }; return o.a + o.b; })(), 3);
t("shorthand method", ({ m() { return 5; } }).m(), 5);
t("computed key", (function () { var k = "x"; return ({ [k]: 1 }).x; })(), 1);
t("computed key expression", ({ ["a" + "b"]: 2 }).ab, 2);
t("computed key order", (function () { var s = ""; function k(n) { s += n; return n; } ({ [k("a")]: k("b"), [k("c")]: k("d") }); return s; })(), "abcd");
t("computed symbol key", (function () { var s = Symbol(); return ({ [s]: 3 })[s]; })(), 3);
t("computed method", (function () { var n = "dyn"; return ({ [n + "M"]() { return 4; } }).dynM(); })(), 4);
t("computed getter", (function () { var n = "g"; return ({ get [n]() { return 5; } }).g; })(), 5);
t("numeric keys", ({ 1: "a", 2.5: "b", 0x10: "c" })[16], "c");
t("numeric key text", Object.keys({ 1.5: 1, 2: 2 }).join(), "2,1.5");
t("string keys", ({ "a b": 1 })["a b"], 1);
t("keyword keys", ({ if: 1, class: 2, new: 3 }).class, 2);
t("keyword shorthand methods", ({ delete() { return 6; } }).delete(), 6);
t("duplicate keys last wins", ({ a: 1, a: 2 }).a, 2);
t("getter and setter", (function () { var o = { _v: 1, get v() { return this._v; }, set v(x) { this._v = x; } }; o.v = 8; return o.v; })(), 8);
t("getter only", (function () { var o = { get g() { return 1; } }; o.g = 2; return o.g; })(), 1);
t("accessor property attributes", (function () { var d = Object.getOwnPropertyDescriptor({ get g() { return 1; } }, "g"); return d.enumerable + "," + d.configurable + "," + (typeof d.set); })(), "true,true,undefined");
t("method is enumerable and writable", (function () { var d = Object.getOwnPropertyDescriptor({ m() { } }, "m"); return d.enumerable + "," + d.writable + "," + d.configurable; })(), "true,true,true");
t("__proto__ value sets the prototype", Object.getPrototypeOf({ __proto__: Array.prototype }) === Array.prototype, true);
t("__proto__ with a non object is ignored", Object.getPrototypeOf({ __proto__: 5 }) === Object.prototype, true);
t("computed __proto__ is an own property", (function () { var o = { ["__proto__"]: 1 }; return o.hasOwnProperty("__proto__"); })(), true);
t("shorthand __proto__ is an own property", (function () { var __proto__ = 1; return ({ __proto__ }).hasOwnProperty("__proto__"); })(), true);
t("super in an object method", (function () { var base = { hi() { return "base"; } }; var o = { __proto__: base, hi() { return "obj>" + super.hi(); } }; return o.hi(); })(), "obj>base");
t("generator method in a literal", (function () { return drain(({ *g() { yield 1; yield 2; } }).g()); })(), "1,2");
t("trailing commas", ({ a: 1, }).a + [1, 2, ].length, 3);

// ---- array literals
t("holes", [1, , 3].length, 3);
t("hole is missing", (function () { return 1 in [1, , 3]; })(), false);
t("trailing comma adds no element", [1, 2, ].length, 2);
t("elision at the end", [1, 2, , ].length, 3);
t("spread in array", [...[1, 2], 3, ...[4]].join(), "1,2,3,4");
t("spread of a string", [..."héé"].length, 3);
t("spread of a set-like iterable", [...{ [Symbol.iterator]() { var i = 0; return { next() { return i < 2 ? { value: i++, done: false } : { done: true }; } }; } }].join(), "0,1");
t("spread of a generator", [...(function* () { yield 1; yield 2; })()].join(), "1,2");
t("spread fills holes with undefined", [...[1, , 3]].hasOwnProperty(1), true);
terr("spread of a non iterable", function () { [...5]; }, "TypeError");
terr("spread of undefined", function () { [...undefined]; }, "TypeError");
t("array element order", (function () { var s = ""; function g(n) { s += n; return n; } [g(1), g(2), ...[g(3)]]; return s; })(), "123");

// ---- default parameters
t("default used for undefined", (function (a = 5) { return a; })(), 5);
t("default not used for null", (function (a = 5) { return a; })(null), null);
t("default not used for a value", (function (a = 5) { return a; })(0), 0);
t("default sees earlier parameters", (function (a, b = a * 2) { return b; })(4), 8);
t("default evaluated at call time", (function () { var n = 0; function f(a = ++n) { return a; } f(); f(); return n; })(), 2);
t("default not evaluated when supplied", (function () { var n = 0; function f(a = ++n) { return a; } f(1); return n; })(), 0);
t("default can call functions", (function () { function d() { return "dflt"; } return (function (a = d()) { return a; })(); })(), "dflt");
t("default sees the outer scope", (function () { var outer = "o"; return (function (a = outer) { return a; })(); })(), "o");
t("default arrow capturing a parameter", (function (a, f = () => a) { return f(); })(3), 3);
t("default with this", (function () { return ({ v: 7, m(a = this.v) { return a; } }).m(); })(), 7);
t("default in method", ({ m(a = 1, b = a + 1) { return a + b; } }).m(), 3);
t("default in arrow", ((a = 1, b = 2) => a + b)(), 3);
t("default in constructor", (function () { function P(x = 4) { this.x = x; } return new P().x; })(), 4);
t("default value function name", (function (f = function () { }) { return f.name; })(), "f");
t("default destructured parameter", (function ({ a, b } = { a: 1, b: 2 }) { return a + b; })(), 3);
t("default array destructured parameter", (function ([x, y] = [5, 6]) { return x * y; })(), 30);
t("parameter named like a default of another", (function (a, b = () => a, c = b()) { return c; })(9), 9);
t("arguments object and defaults", (function (a = 1) { return arguments.length; })(), 0);

// ---- rest parameters
t("rest collects the rest", (function (a, ...r) { return r.join(); })(1, 2, 3), "2,3");
t("rest of nothing is an empty array", (function (...r) { return r.length; })(), 0);
t("rest is an array", (function (...r) { return Array.isArray(r); })(1), true);
t("rest in arrow", ((...r) => r.length)(1, 2, 3), 3);
t("rest in method", ({ m(...r) { return r.length; } }).m(1, 2), 2);
t("rest with destructuring", (function (...[a, b]) { return a + b; })(1, 2), 3);
t("rest does not map arguments", (function (...r) { return arguments.length + r.length; })(1, 2), 4);
t("rest length excluded", (function (a, b, ...r) { }).length, 2);

// ---- spread calls
t("spread call", Math_max(...[1, 5, 3]), 5);
function Math_max() { var m = -Infinity; for (var i = 0; i < arguments.length; i++) { if (arguments[i] > m) m = arguments[i]; } return m; }
t("spread with other arguments", Math_max(10, ...[1, 5], 2), 10);
t("multiple spreads", Math_max(...[1], ...[7], ...[3]), 7);
t("spread an empty iterable", Math_max(...[], 4), 4);
t("spread in new", (function () { function P(a, b, c) { this.v = a + b + c; } return new P(...[1, 2], 3).v; })(), 6);
t("spread keeps this for method calls", (function () { var o = { v: 3, f: function (a, b) { return this.v + a + b; } }; return o.f(...[1, 2]); })(), 6);
t("spread of arguments", (function () { function inner(a, b) { return a + b; } return (function () { return inner(...arguments); })(1, 2); })(), 3);
t("spread evaluates the iterable once", (function () { var n = 0; function it() { n++; return [1]; } Math_max(...it()); return n; })(), 1);

// ---- array destructuring
t("basic", (function () { var [a, b] = [1, 2]; return a + b; })(), 3);
t("skipping", (function () { var [, , c] = [1, 2, 3]; return c; })(), 3);
t("default", (function () { var [a = 5, b = 6] = [1]; return a + b; })(), 7);
t("default not used for null", (function () { var [a = 5] = [null]; return a; })(), null);
t("rest element", (function () { var [a, ...r] = [1, 2, 3]; return a + "|" + r.join(); })(), "1|2,3");
t("nested", (function () { var [a, [b, [c]]] = [1, [2, [3]]]; return a + b + c; })(), 6);
t("from a string", (function () { var [a, b] = "xy"; return a + b; })(), "xy");
t("from an iterator", (function () { var [a, b] = (function* () { yield 1; yield 2; yield 3; })(); return a + b; })(), 3);
t("missing elements are undefined", (function () { var [a, b] = [1]; return typeof b; })(), "undefined");
terr("from undefined", function () { var [a] = undefined; }, "TypeError");
terr("from null", function () { var [a] = null; }, "TypeError");
terr("from a non iterable object", function () { var [a] = {}; }, "TypeError");
t("with member targets", (function () { var o = {}, arr = []; [o.x, arr[0]] = [1, 2]; return o.x + arr[0]; })(), 3);
t("assignment expression value", (function () { var a, b; var r = ([a, b] = [1, 2]); return r.length; })(), 2);
t("swap", (function () { var a = 1, b = 2; [a, b] = [b, a]; return a + "," + b; })(), "2,1");
t("rotate", (function () { var a = 1, b = 2, c = 3; [a, b, c] = [b, c, a]; return a + "" + b + c; })(), "231");
t("holes in the source", (function () { var [a, b = "d"] = [1, ,]; return a + b; })(), "1d");
t("default is evaluated lazily", (function () { var n = 0; var [a = ++n, b = ++n] = [1]; return n + "," + b; })(), "1,1");
t("default sees earlier targets", (function () { var [a, b = a] = [4]; return b; })(), 4);
t("default function name", (function () { var [f = function () { }] = []; return f.name; })(), "f");
t("destructuring the iterator is closed when not exhausted", (function () {
    var closed = 0;
    var it = { [Symbol.iterator]() { return { next() { return { value: 1, done: false }; }, return() { closed++; return {}; } }; } };
    var [a] = it;
    return closed;
})(), 1);
t("destructuring does not close an exhausted iterator", (function () {
    var closed = 0, i = 0;
    var it = { [Symbol.iterator]() { return { next() { return i++ < 1 ? { value: 1, done: false } : { done: true }; }, return() { closed++; return {}; } }; } };
    var [a, b] = it;
    return closed;
})(), 0);
t("rest consumes the iterator without closing", (function () {
    var closed = 0, i = 0;
    var it = { [Symbol.iterator]() { return { next() { return i++ < 2 ? { value: i, done: false } : { done: true }; }, return() { closed++; return {}; } }; } };
    var [...all] = it;
    return all.length + "," + closed;
})(), "2,0");
t("iterator closed when the target throws", (function () {
    var closed = 0;
    var it = { [Symbol.iterator]() { return { next() { return { value: 1, done: false }; }, return() { closed++; return {}; } }; } };
    try { var [{ x }] = [null]; } catch (e) { }
    try { [({ set p(v) { throw 1; } }).p] = it; } catch (e) { }
    return closed;
})(), 1);

// ---- object destructuring
t("basic object", (function () { var { a, b } = { a: 1, b: 2 }; return a + b; })(), 3);
t("renaming", (function () { var { a: x, b: y } = { a: 1, b: 2 }; return x + y; })(), 3);
t("default", (function () { var { a = 5, b = 6 } = { a: 1 }; return a + b; })(), 7);
t("renaming with default", (function () { var { a: x = 9 } = {}; return x; })(), 9);
t("nested", (function () { var { a: { b: { c } } } = { a: { b: { c: 3 } } }; return c; })(), 3);
t("nested with default", (function () { var { a: { b } = { b: 8 } } = {}; return b; })(), 8);
t("computed key", (function () { var k = "kk"; var { [k]: v } = { kk: 7 }; return v; })(), 7);
t("computed key with default", (function () { var { ["x" + 1]: v = 2 } = {}; return v; })(), 2);
t("numeric key", (function () { var { 0: a, 1: b } = ["p", "q"]; return a + b; })(), "pq");
t("string primitive source", (function () { var { length, 0: first } = "abc"; return length + first; })(), "3a");
t("number primitive source", (function () { var { toFixed } = 5; return typeof toFixed; })(), typeof (5).toFixed);
t("empty pattern still checks the value", (function () { var {} = 1; return 1; })(), 1);
terr("from undefined", function () { var { a } = undefined; }, "TypeError");
terr("from null", function () { var { a } = null; }, "TypeError");
terr("empty pattern from null", function () { var {} = null; }, "TypeError");
t("with member targets", (function () { var o = {}; ({ a: o.x, b: o["y"] } = { a: 1, b: 2 }); return o.x + o.y; })(), 3);
t("assignment pattern needs parentheses and returns the source", (function () { var a, src = { a: 1 }; return ({ a } = src) === src; })(), true);
t("shorthand with default in assignment", (function () { var a; ({ a = 4 } = {}); return a; })(), 4);
t("getters are invoked once", (function () { var n = 0; var { v } = { get v() { n++; return 1; } }; return n; })(), 1);
t("inherited properties are read", (function () { var { x } = Object.create({ x: 3 }); return x; })(), 3);
t("evaluation order", (function () {
    var s = "";
    var src = { get a() { s += "a"; return 1; }, get b() { s += "b"; return 2; } };
    var { b, a } = src;
    return s;
})(), "ba");
t("default function name in object pattern", (function () { var { f = function () { } } = {}; return f.name; })(), "f");
t("destructuring in for-of", (function () { var r = []; for (var { a, b } of [{ a: 1, b: 2 }, { a: 3, b: 4 }]) r.push(a + b); return r.join(); })(), "3,7");
t("destructuring array in for-of", (function () { var r = []; for (var [k, v] of [[1, 2], [3, 4]]) r.push(k * v); return r.join(); })(), "2,12");
t("destructuring with let in for-of", (function () { var r = []; for (let { n } of [{ n: 1 }, { n: 2 }]) r.push(function () { return n; }); return r[0]() + r[1](); })(), 3);
t("destructuring in catch", (function () { try { throw { m: "x" }; } catch ({ m }) { return m; } })(), "x");
t("destructuring parameters", (function ({ a, b }, [c, d]) { return a + b + c + d; })({ a: 1, b: 2 }, [3, 4]), 10);
t("destructuring parameters with defaults", (function ({ a = 1, b = 2 } = {}) { return a + b; })(), 3);
t("destructuring in arrow parameters", (({ a }, [b]) => a + b)({ a: 1 }, [2]), 3);
t("mixed nesting", (function () { var { a: [x, { y }] } = { a: [1, { y: 2 }] }; return x + y; })(), 3);
t("let and const patterns", (function () { let [a] = [1]; const { b } = { b: 2 }; return a + b; })(), 3);
terr("const pattern is read only", function () { const [a] = [1]; a = 2; }, "TypeError");
t("var pattern is hoisted", (function () { var r = typeof z; var [z] = [1]; return r; })(), "undefined");

// ---- template literals
t("plain template", `abc`, "abc");
t("substitution", (function () { var n = "x"; return `a${n}b`; })(), "axb");
t("expression substitution", `${1 + 2}`, "3");
t("multiple substitutions", `${1}${2}${3}`, "123");
t("adjacent text", `a${1}b${2}c`, "a1b2c");
t("nested template", `a${`b${"c"}`}d`, "abcd");
t("object to string", `${{ toString() { return "ts"; } }}`, "ts");
t("array to string", `${[1, 2]}`, "1,2");
t("null and undefined", `${null} ${undefined}`, "null undefined");
t("symbol throws", (function () { try { return `${Symbol()}`; } catch (e) { return e.name; } })(), "TypeError");
t("escapes in templates", `a\tb\n`.length, 4);
t("unicode escape in template", `\u0041\x41`, "AA");
t("dollar without brace", `$a$`, "$a$");
t("escaped dollar brace", `\${x}`, "${x}");
t("backtick escape", `\``, "`");
t("multi line template", `a
b`, "a\nb");
t("line continuation", `a\
b`, "ab");
t("carriage returns are normalised", eval("`a\r\nb\rc`"), "a\nb\nc");
t("template with braces in the expression", `${{ a: 1 }.a}`, "1");
t("template with a function in the expression", `${(function () { return "f"; })()}`, "f");
t("template toString order", (function () { var s = ""; var a = { toString() { s += "a"; return "A"; } }, b = { toString() { s += "b"; return "B"; } }; `${a}${b}`; return s; })(), "ab");
t("template does not call valueOf first", `${{ valueOf() { return 1; }, toString() { return "s"; } }}`, "s");

// ---- tagged templates
function tag(strings, ...vals) { return strings.join("|") + "#" + vals.join(","); }
t("tagged", tag`a${1}b${2}c`, "a|b|c#1,2");
t("tagged without substitutions", tag`only`, "only#");
t("tagged raw strings", (function (s) { return s.raw[0]; })`a\nb`, "a\\nb");
t("tagged cooked strings", (function (s) { return s[0]; })`a\nb`, "a\nb");
t("tagged invalid escape gives undefined cooked", (function (s) { return typeof s[0] + "," + s.raw[0]; })`\unicode`, "undefined,\\unicode");
t("strings array is frozen", (function (s) { return Object.isFrozen(s) && Object.isFrozen(s.raw); })`x`, true);
t("same call site gives the same strings object", (function () { function id(s) { return s; } function site() { return id`x`; } return site() === site(); })(), true);
t("different call sites differ", (function () { function id(s) { return s; } return id`x` === id`x`; })(), false);
t("tag as member expression", (function () { var o = { tag(s) { return this === o; } }; return o.tag`x`; })(), true);
t("tag as a function call result", (function () { function mk() { return function (s) { return s[0]; }; } return mk()`z`; })(), "z");
t("tag called with this undefined for plain identifiers", (function () { "use strict"; function f() { return this; } return f`x`; })(), undefined);
t("tagged template result may be any value", (function (s) { return 42; })`x`, 42);
t("raw preserves line continuation", (function (s) { return s.raw[0].length; })`a\
b`, 4);
t("String.raw like usage", (function (s, ...v) { var r = s.raw[0]; for (var i = 0; i < v.length; i++) r += v[i] + s.raw[i + 1]; return r; })`a\n${1}b`, "a\\n1b");
t("tagged template evaluates substitutions before the call", (function () { var s = ""; function tg() { s += "T"; } function v() { s += "v"; return 1; } tg`${v()}${v()}`; return s; })(), "vvT");

// ---- literals and identifiers (ES2015 additions)
t("binary literal", 0b1010, 10);
t("uppercase binary", 0B11, 3);
t("octal literal", 0o755, 493);
t("uppercase octal", 0O17, 15);
t("unicode code point escape in a string", "\u{41}", "A");
t("astral code point escape", "\u{1F600}".length, 2);
t("astral code point values", "\u{1F600}".charCodeAt ? 1 : 1, 1);
t("unicode escape in an identifier", (function () { var \u0061bc = 5; return abc; })(), 5);
t("code point escape in an identifier", (function () { var \u{61}bcd = 6; return abcd; })(), 6);
t("non ASCII identifiers", (function () { var 변수 = 1, café = 2; return 변수 + café; })(), 3);
t("new.target parses in functions", (function () { function f() { return typeof new.target; } return f(); })(), "undefined");
t("property names can be keywords after a dot", ({ default: 1 }).default, 1);
t("let as an identifier in sloppy code", (function () { var let = 1; return let; })(), 1);
t("yield as an identifier in sloppy code", (function () { var yield = 2; return yield; })(), 2);
t("static as an identifier in sloppy code", (function () { var static = 3; return static; })(), 3);
