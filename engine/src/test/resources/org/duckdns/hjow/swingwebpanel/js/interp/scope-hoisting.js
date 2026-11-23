// P13-02: var / function hoisting, let / const block scopes, TDZ, redeclaration and reassignment limits.

// ---- var hoisting
t("var before assignment", typeof hv, "undefined");
var hv = 5;
t("var after assignment", hv, 5);
function varFn() { var r = typeof inner; var inner = 1; return r + inner; }
t("var inside function", varFn(), "undefined1");
function varInBlock() { if (true) { var inBlock = 7; } return inBlock; }
t("var ignores blocks", varInBlock(), 7);
function varInLoop() { for (var i = 0; i < 3; i++) { } return i; }
t("var leaks from for", varInLoop(), 3);
function varInTry() { try { var a = 1; throw 0; } catch (e) { var b = 2; } return a + b; }
t("var in try/catch", varInTry(), 3);
function dupVar() { var x = 1; var x; return x; }
t("duplicate var keeps value", dupVar(), 1);
function varAfterReturn() { return v; var v = 1; }
t("var hoisted over return", varAfterReturn(), undefined);

// ---- function declarations
t("function hoisted", hoisted(), "h");
function hoisted() { return "h"; }
function overrideOrder() { return typeof f; function f() { } var f; }
t("function wins over uninitialised var", overrideOrder(), "function");
function overrideOrder2() { var f = 1; return typeof f; function f() { } }
t("assignment replaces function", overrideOrder2(), "number");
function twice() { return f(); function f() { return 1; } function f() { return 2; } }
t("last function declaration wins", twice(), 2);
function nestedHoist() { return g(); function g() { return h(); function h() { return "deep"; } } }
t("nested function hoisting", nestedHoist(), "deep");
function paramVsFn(a) { return typeof a; function a() { } }
t("function overrides parameter", paramVsFn(1), "function");
function paramVsVar(a) { var a; return a; }
t("var does not reset parameter", paramVsVar(4), 4);

// ---- global bindings are properties of the global object, let/const/class are not
var globalVar = 1;
function globalFn() { }
let globalLet = 2;
const globalConst = 3;
class GlobalClass { }
t("var is a global property", Object.getOwnPropertyDescriptor(this, "globalVar").configurable, false);
t("function is a global property", typeof this.globalFn, "function");
t("let is not a global property", this.globalLet, undefined);
t("const is not a global property", this.globalConst, undefined);
t("class is not a global property", this.GlobalClass, undefined);
t("let readable as identifier", globalLet + globalConst, 5);
t("this at top level is the global object", this === globalThis, true);

// ---- let / const blocks
let shadow = "outer";
{ let shadow = "inner"; t("block shadows let", shadow, "inner"); }
t("outer let restored", shadow, "outer");
function blockScope() { { let a = 1; } return typeof a; }
t("let invisible after block", blockScope(), "undefined");
function constInBlock() { const c = 1; { const c = 2; } return c; }
t("const shadowing", constInBlock(), 1);
function ifBlock() { if (true) { let z = 3; return z; } }
t("let in if block", ifBlock(), 3);
function sameNameVarLet() { var r = []; { let v = 1; r.push(v); } { let v = 2; r.push(v); } return r.join(); }
t("independent blocks", sameNameVarLet(), "1,2");

// ---- temporal dead zone
terr("let read before declaration", function () { tdz1; let tdz1 = 1; }, "ReferenceError");
terr("const read before declaration", function () { tdz2; const tdz2 = 1; }, "ReferenceError");
terr("class read before declaration", function () { new TdzC(); class TdzC { } }, "ReferenceError");
terr("typeof sees TDZ", function () { typeof tdz3; let tdz3; }, "ReferenceError");
terr("self reference in initialiser", function () { let tdz4 = tdz4 + 1; }, "ReferenceError");
terr("closure called early", function () { var f = function () { return later; }; f(); let later = 1; }, "ReferenceError");
terr("write before declaration", function () { tdz5 = 1; let tdz5; }, "ReferenceError");
function tdzOk() { var f = function () { return later; }; let later = 9; return f(); }
t("closure called after init", tdzOk(), 9);
terr("TDZ in switch case", function () { switch (1) { case 0: let sw = 1; break; case 1: sw = 2; } }, "ReferenceError");
t("let without initialiser is undefined", (function () { let u; return u; })(), undefined);
terr("parameter default TDZ", function () { (function (a = b, b = 1) { })(); }, "ReferenceError");
t("parameter default sees earlier", (function (a, b = a + 1) { return b; })(1), 2);

// ---- const
terr("const assignment", function () { const c = 1; c = 2; }, "TypeError");
terr("const increment", function () { const c = 1; c++; }, "TypeError");
terr("const compound", function () { const c = 1; c += 1; }, "TypeError");
terr("const in for-of", function () { for (const x of [1]) { x = 2; } }, "TypeError");
t("const in for-of fresh binding", (function () { var r = []; for (const x of [1, 2]) { r.push(x); } return r.join(); })(), "1,2");
t("const object stays mutable", (function () { const o = { a: 1 }; o.a = 2; return o.a; })(), 2);
t("const in for-in", (function () { var r = []; for (const k in { a: 1, b: 2 }) { r.push(k); } return r.join(); })(), "a,b");
terr("const for-loop update", function () { for (const i = 0; i < 2; i++) { } }, "TypeError");
t("const for-loop without update", (function () { var n = 0; for (const i = 0; n < 2;) { n++; } return n; })(), 2);

// ---- closures capture bindings
t("closure shares variable", (function () { var x = 1; var get = function () { return x; }; x = 2; return get(); })(), 2);
t("var loop closures share", (function () { var f = []; for (var i = 0; i < 3; i++) { f.push(function () { return i; }); } return f[0]() + "" + f[1]() + f[2](); })(), "333");
t("let loop closures differ", (function () { var f = []; for (let i = 0; i < 3; i++) { f.push(function () { return i; }); } return f[0]() + "" + f[1]() + f[2](); })(), "012");
t("let loop copies for the update expression", (function () { var f = []; for (let i = 0; i < 3; i++) { f.push(function () { return i; }); i += 0; } return f.length; })(), 3);
t("closure sees later loop-body change", (function () { var f; for (let i = 0; i < 1; i++) { f = function () { return i; }; i = 5; } return f(); })(), 5);
t("for-of let closures differ", (function () { var f = []; for (let x of [1, 2, 3]) { f.push(function () { return x; }); } return f[0]() + "" + f[2](); })(), "13");
t("for-in let closures differ", (function () { var f = []; for (let k in { a: 1, b: 2 }) { f.push(function () { return k; }); } return f[0]() + f[1](); })(), "ab");
t("closure over a parameter", (function (p) { return function () { return p++; }; })(4)(), 4);
t("counter closures are independent", (function () {
    function mk() { var n = 0; return function () { return ++n; }; }
    var a = mk(), b = mk(); a(); a();
    return a() + "," + b();
})(), "3,1");

// ---- block level functions (ES2015 13.3.3 and Annex B.3.3)
t("block function visible in block", (function () { { function bf() { return 1; } return bf(); } })(), 1);
t("block function hoisted to its block start", (function () { { var r = bf(); function bf() { return "up"; } } return r; })(), "up");
t("annex B block function visible after block", (function () { { function bf() { return 2; } } return bf(); })(), 2);
t("annex B function var is undefined before the block", (function () { var r = typeof bf; { function bf() { } } return r; })(), "undefined");
t("annex B skipped for a conflicting let", (function () { let bf = "let"; { function bf() { } } return bf; })(), "let");
(function () {
    "use strict";
    { function strictBlock() { } }
    t("strict block function stays in the block", typeof strictBlock, "undefined");
})();

// ---- named function expressions
t("named function expression binding", (function () { var f = function fact(n) { return n < 2 ? 1 : n * fact(n - 1); }; return f(4); })(), 24);
t("named function expression name is not leaked", (function () { var f = function inner() { }; return typeof inner; })(), "undefined");
t("named function expression name is read only (sloppy)", (function () { var f = function inner() { inner = 1; return typeof inner; }; return f(); })(), "function");
terr("named function expression name is read only (strict)", function () { (function inner() { "use strict"; inner = 1; })(); }, "TypeError");
t("inner var shadows the function name", (function () { var f = function inner() { var inner = 3; return inner; }; return f(); })(), 3);

// ---- catch parameter scope
t("catch parameter scope", (function () { var e = "outer"; try { throw "inner"; } catch (e) { var seen = e; } return e + "," + seen; })(), "outer,inner");
t("catch block let", (function () { try { throw 1; } catch (e) { let k = e + 1; return k; } })(), 2);

// ---- with statement (sloppy mode only)
t("with reads object properties", (function () { var o = { a: 1 }; with (o) { return a; } })(), 1);
t("with writes object properties", (function () { var o = { a: 1 }; with (o) { a = 2; } return o.a; })(), 2);
t("with falls back to outer scope", (function () { var o = {}, b = 3; with (o) { return b; } })(), 3);
t("with var declaration goes to function scope", (function () { var o = { v: 1 }; with (o) { var v = 2; } return o.v + "," + v; })(), "2,undefined");
t("with respects unscopables", (function () { var o = { a: 1, b: 2 }; o[Symbol.unscopables] = { a: true }; var a = "outer"; with (o) { return a + b; } })(), "outer2");
t("with and function calls", (function () { var o = { f: function () { return this === o; } }; with (o) { return f(); } })(), true);

// ---- global object interplay
t("undeclared assignment creates global (sloppy)", (function () { undeclaredGlobal = 5; return globalThis.undeclaredGlobal; })(), 5);
terr("undeclared assignment throws (strict)", function () { "use strict"; undeclaredStrict = 1; }, "ReferenceError");
terr("undeclared read", function () { undeclaredRead; }, "ReferenceError");
t("typeof undeclared", typeof undeclaredAnything, "undefined");
t("delete global var fails", delete globalVar, false);
t("delete undeclared global works", (function () { globalThis.dyn = 1; return delete dyn; })(), true);
t("global var assignment is visible through this", (function () { globalVar = 10; return this === undefined ? globalThis.globalVar : this.globalVar; })(), 10);
