// P13: ES2015 14.6 proper tail calls in strict mode code.

// ---- direct and indirect recursion in tail position runs without growing the stack
t("self recursion", (function () { 'use strict'; function f(n) { return n === 0 ? "done" : f(n - 1); } return f(100000); })(), "done");
t("accumulator", (function () { 'use strict'; function sum(n, acc) { if (n === 0) return acc; return sum(n - 1, acc + n); } return sum(50000, 0); })(), 1250025000);
t("mutual recursion", (function () { 'use strict'; function even(n) { return n === 0 ? true : odd(n - 1); } function odd(n) { return n === 0 ? false : even(n - 1); } return even(60001); })(), false);
t("logical and", (function () { 'use strict'; function f(n) { return n === 0 || f(n - 1); } return f(50000); })(), true);
t("logical or", (function () { 'use strict'; function f(n) { return n !== 0 && f(n - 1); } return f(50000); })(), false);
t("comma expression", (function () { 'use strict'; function f(n) { return (n, n === 0 ? "c" : f(n - 1)); } return f(50000); })(), "c");
t("nested conditionals", (function () { 'use strict'; function f(n) { return n === 0 ? "z" : n % 2 ? f(n - 1) : n % 3 ? f(n - 1) : f(n - 1); } return f(50000); })(), "z");
t("parenthesized call", (function () { 'use strict'; function f(n) { return (n === 0 ? "p" : (f(n - 1))); } return f(50000); })(), "p");
t("arrow with an expression body", (function () { 'use strict'; var f = n => n === 0 ? "arrow" : f(n - 1); return f(50000); })(), "arrow");
t("arrow with a block body", (function () { 'use strict'; var f = n => { return n === 0 ? "ab" : f(n - 1); }; return f(50000); })(), "ab");
t("method call", (function () { 'use strict'; var o = { m(n) { return n === 0 ? "m" : this.m(n - 1); } }; return o.m(50000); })(), "m");
t("class method", (function () { class C { m(n) { return n === 0 ? "cm" : this.m(n - 1); } static s(n) { return n === 0 ? "cs" : C.s(n - 1); } } return new C().m(50000) + C.s(50000); })(), "cmcs");
t("call and apply", (function () { 'use strict'; function f(n) { return n === 0 ? "ca" : f.call(null, n - 1); } return f(50); })(), "ca");
t("via a variable of another function", (function () { 'use strict'; var g; function f(n) { return n === 0 ? "v" : g(n - 1); } g = f; return f(50000); })(), "v");
t("returning the result of a deeper chain", (function () { 'use strict'; function a(n) { return n ? b(n - 1) : "a"; } function b(n) { return n ? c(n - 1) : "b"; } function c(n) { return n ? a(n - 1) : "c"; } return a(60000); })(), "a");

// ---- the results and the call semantics stay correct
t("this of a tail called method", (function () { 'use strict'; var o = { v: 7, get() { return this.v; } }; function f() { return o.get(); } return f(); })(), 7);
t("this of a tail called plain function in strict mode", (function () { 'use strict'; function g() { return this; } function f() { return g(); } return f(); })(), undefined);
t("sloppy callee gets the global this", (function () { function g() { return this === globalThis; } function f() { 'use strict'; return g(); } return f(); })(), true);
t("arguments of the tail call", (function () { 'use strict'; function g() { return arguments.length + ":" + Array.prototype.join.call(arguments, ","); } function f() { return g(1, 2, 3); } return f(); })(), "3:1,2,3");
t("spread arguments", (function () { 'use strict'; function g(a, b, c) { return a + b + c; } function f() { return g(...[1, 2], 3); } return f(); })(), 6);
t("arguments are evaluated before the call", (function () { 'use strict'; var log = []; function g(x) { log.push("g" + x); return x; } function a() { log.push("a"); return 1; } function f() { return g(a()); } f(); return log.join(); })(), "a,g1");
t("the callee expression is evaluated first", (function () { 'use strict'; var log = []; function mk() { log.push("mk"); return function (x) { log.push("call" + x); return x; }; } function arg() { log.push("arg"); return 1; } function f() { return mk()(arg()); } f(); return log.join(); })(), "mk,arg,call1");
t("non function target is a TypeError", (function () { 'use strict'; function f() { var o = {}; return o.nope(); } try { f(); } catch (e) { return e.name; } })(), "TypeError");
t("class constructors cannot be tail called", (function () { 'use strict'; class K { } function f() { return K(); } try { f(); } catch (e) { return e.name; } })(), "TypeError");
t("native targets work", (function () { 'use strict'; function f() { return [1, 2, 3].map(function (x) { return x * 2; }); } return f().join(); })(), "2,4,6");
t("bound targets work", (function () { 'use strict'; function g(a, b) { return this.k + a + b; } var h = g.bind({ k: 1 }, 2); function f() { return h(3); } return f(); })(), 6);
t("generator targets work", (function () { 'use strict'; function* g() { yield 5; } function f() { return g(); } return f().next().value; })(), 5);
t("exceptions from the tail callee propagate to the right handler", (function () { 'use strict'; function g() { throw new RangeError("deep"); } function f() { return g(); } try { f(); } catch (e) { return e.message; } })(), "deep");
t("exceptions pass through an outer try", (function () { 'use strict'; function g(n) { if (n === 0) throw "bottom"; return g(n - 1); } try { return g(5000); } catch (e) { return e; } })(), "bottom");
t("the callee sees its own variables", (function () { 'use strict'; function g() { return typeof secret; } function f() { var secret = 1; return g(); } return f(); })(), "undefined");
t("closures created before the tail call keep their variables", (function () { 'use strict'; var kept; function g() { return kept(); } function f() { var secret = "s"; kept = function () { return secret; }; return g(); } return f(); })(), "s");
t("eval is not a tail call but works", (function () { 'use strict'; function f() { var x = 3; return eval("x + 1"); } return f(); })(), 4);
t("getter in tail position", (function () { 'use strict'; var o = { get v() { return 9; } }; function f() { return o.v; } return f(); })(), 9);
t("new in tail position", (function () { 'use strict'; function P() { this.a = 1; } function f() { return new P(); } return f().a; })(), 1);
t("template and operators in tail position are normal", (function () { 'use strict'; function g() { return 2; } function f() { return g() + 1; } return f(); })(), 3);
t("tail call in a block inside a loop", (function () { 'use strict'; function g(i) { return i * 2; } function f() { for (var i = 0; i < 5; i++) { if (i === 3) { return g(i); } } } return f(); })(), 6);
t("tail call in a switch", (function () { 'use strict'; function g() { return "g"; } function f(x) { switch (x) { case 1: return g(); default: return "d"; } } return f(1) + f(2); })(), "gd");
t("tail call in a labeled block", (function () { 'use strict'; function g() { return "L"; } function f() { l: { return g(); } } return f(); })(), "L");

// ---- positions that are not tail calls keep the call depth limit
terr("addition after the call", function () { 'use strict'; function f(n) { return n === 0 ? 0 : 1 + f(n - 1); } f(100000); }, "RangeError");
terr("call inside try", function () { 'use strict'; function f(n) { try { return n === 0 ? 0 : f(n - 1); } finally { } } f(100000); }, "RangeError");
terr("call inside try with catch", function () { 'use strict'; function f(n) { try { return n === 0 ? 0 : f(n - 1); } catch (e) { throw e; } } f(100000); }, "RangeError");
terr("call inside catch", function () { 'use strict'; function f(n) { try { throw 1; } catch (e) { return n === 0 ? 0 : f(n - 1); } } f(100000); }, "RangeError");
terr("call inside for-of", function () { 'use strict'; function f(n) { for (var x of [1]) { return n === 0 ? 0 : f(n - 1); } } f(100000); }, "RangeError");
terr("call inside for-in", function () { 'use strict'; function f(n) { for (var k in { a: 1 }) { return n === 0 ? 0 : f(n - 1); } } f(100000); }, "RangeError");
terr("result used in an assignment", function () { 'use strict'; function f(n) { var r = n === 0 ? 0 : f(n - 1); return r; } f(100000); }, "RangeError");
terr("sloppy function", function () { function f(n) { return n === 0 ? 0 : f(n - 1); } f(100000); }, "RangeError");
terr("sloppy arrow", function () { var f = n => n === 0 ? 0 : f(n - 1); f(100000); }, "RangeError");
terr("constructor recursion", function () { 'use strict'; function C(n) { if (n > 0) { return new C(n - 1); } } new C(100000); }, "RangeError");
terr("call as an argument", function () { 'use strict'; function id(x) { return x; } function f(n) { return n === 0 ? 0 : id(f(n - 1)); } f(100000); }, "RangeError");
terr("generator bodies are not tail calling", function () { 'use strict'; function* g(n) { return n === 0 ? 0 : g(n - 1).next(); } g(100000).next(); }, "RangeError");
t("function with the use strict directive after other directives", (function () { function f(n) { 'other'; 'use strict'; return n === 0 ? "ok" : f(n - 1); } return f(30000); })(), "ok");
t("strictness comes from the caller", (function () { function sloppyCallee(n) { return n; } function f(n) { 'use strict'; return n === 0 ? sloppyCallee("s") : f(n - 1); } return f(30000); })(), "s");
t("a strict function called from sloppy code still iterates", (function () { function strictRec(n) { 'use strict'; return n === 0 ? "x" : strictRec(n - 1); } function sloppy() { return strictRec(50000); } return sloppy(); })(), "x");
