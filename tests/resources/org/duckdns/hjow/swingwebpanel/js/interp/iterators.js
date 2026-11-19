// P13-09: iteration protocol, for-of, generators, yield, yield*, iterator closing.

function counterIterable(n, log) {
    var it = {};
    it[Symbol.iterator] = function () {
        var i = 0;
        return {
            next: function () { return i < n ? { value: i++, done: false } : { value: undefined, done: true }; },
            return: function (v) { if (log) log.push("return"); return { value: v, done: true }; }
        };
    };
    return it;
}

// ---- for-of
t("for-of over an array", (function () { var s = ""; for (var v of [1, 2, 3]) s += v; return s; })(), "123");
t("for-of over a string", (function () { var s = ""; for (var c of "abc") s += c + "-"; return s; })(), "a-b-c-");
t("for-of over code points", (function () { var n = 0; for (var c of "a\u{1F600}b") n++; return n; })(), 3);
t("for-of over a custom iterable", (function () { var s = ""; for (var v of counterIterable(3)) s += v; return s; })(), "012");
t("for-of over arguments", (function () { var s = 0; for (var v of arguments) s += v; return s; })(1, 2, 3), 6);
t("for-of over array entries", (function () { var s = ""; for (var [i, v] of ["a", "b"].entries()) s += i + v; return s; })(), "0a1b");
t("for-of over keys", (function () { var s = ""; for (var k of ["x", "y"].keys()) s += k; return s; })(), "01");
t("for-of over holes yields undefined", (function () { var n = 0; for (var v of [1, , 3]) n++; return n; })(), 3);
t("for-of sees pushed elements", (function () { var a = [1], n = 0; for (var v of a) { if (a.length < 3) a.push(v + 1); n++; } return n; })(), 3);
t("for-of with a member expression target", (function () { var o = {}; for (o.x of [1, 2]); return o.x; })(), 2);
t("for-of with let and const", (function () { var s = ""; for (let a of [1, 2]) s += a; for (const b of [3, 4]) s += b; return s; })(), "1234");
t("for-of var is hoisted and keeps the last value", (function () { for (var v of [1, 2]); return v; })(), 2);
t("for-of break", (function () { var s = ""; for (var v of [1, 2, 3]) { if (v == 2) break; s += v; } return s; })(), "1");
t("for-of continue", (function () { var s = ""; for (var v of [1, 2, 3]) { if (v == 2) continue; s += v; } return s; })(), "13");
t("for-of nested", (function () { var s = ""; for (var a of [1, 2]) for (var b of [3, 4]) s += a * b + ","; return s; })(), "3,4,6,8,");
t("for-of labeled continue", (function () { var s = ""; outer: for (var a of [1, 2]) { for (var b of [1, 2, 3]) { if (b == 2) continue outer; s += a + "" + b; } } return s; })(), "1121");
terr("for-of over a non iterable", function () { for (var v of {}) { } }, "TypeError");
terr("for-of over undefined", function () { for (var v of undefined) { } }, "TypeError");
terr("for-of over null", function () { for (var v of null) { } }, "TypeError");
terr("for-of over a number", function () { for (var v of 5) { } }, "TypeError");
terr("iterator without next", function () { var o = {}; o[Symbol.iterator] = function () { return {}; }; for (var v of o) { } }, "TypeError");
terr("iterator result is not an object", function () { var o = {}; o[Symbol.iterator] = function () { return { next: function () { return 1; } }; }; for (var v of o) { } }, "TypeError");
terr("@@iterator is not a function", function () { var o = {}; o[Symbol.iterator] = 1; for (var v of o) { } }, "TypeError");
terr("@@iterator returns a primitive", function () { var o = {}; o[Symbol.iterator] = function () { return 1; }; for (var v of o) { } }, "TypeError");
t("done is read before value", (function () { var order = []; var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { get done() { order.push("done"); return true; }, get value() { order.push("value"); } }; } }; }; for (var v of o) { } return order.join(); })(), "done");
t("next is read at every step (ES2015 IteratorStep)", (function () { var reads = 0; var o = {}; o[Symbol.iterator] = function () { var i = 0; return { get next() { reads++; return function () { return i++ < 3 ? { value: i, done: false } : { done: true }; }; } }; }; for (var v of o) { } return reads; })(), 4);

// ---- closing iterators
t("break closes the iterator", (function () { var log = []; for (var v of counterIterable(5, log)) { break; } return log.join(); })(), "return");
t("return closes the iterator", (function () { var log = []; (function () { for (var v of counterIterable(5, log)) { return; } })(); return log.join(); })(), "return");
t("throw closes the iterator", (function () { var log = []; try { for (var v of counterIterable(5, log)) { throw 1; } } catch (e) { } return log.join(); })(), "return");
t("labeled break closes inner iterators", (function () { var log = []; outer: for (var a of [1]) { for (var b of counterIterable(5, log)) { break outer; } } return log.join(); })(), "return");
t("labeled continue closes inner iterators", (function () { var log = []; outer: for (var a of [1, 2]) { for (var b of counterIterable(5, log)) { continue outer; } } return log.join(); })(), "return,return");
t("normal completion does not call return", (function () { var log = []; for (var v of counterIterable(2, log)) { } return log.length; })(), 0);
t("continue does not close the loop's iterator", (function () { var log = []; for (var v of counterIterable(3, log)) { continue; } return log.length; })(), 0);
t("both break out of nested for-of loops close both iterators", (function () { var log = []; outer: for (var a of counterIterable(3, log)) { for (var b of counterIterable(3, log)) { break outer; } } return log.length; })(), 2);
terr("return result must be an object when closing for break", function () { var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { value: 1, done: false }; }, return: function () { return 5; } }; }; for (var v of o) { break; } }, "TypeError");
t("return errors are suppressed when the loop threw", (function () { var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { value: 1, done: false }; }, return: function () { throw "from-return"; } }; }; try { for (var v of o) { throw "from-body"; } } catch (e) { return e; } })(), "from-body");
t("return errors surface for break", (function () { var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { value: 1, done: false }; }, return: function () { throw "from-return"; } }; }; try { for (var v of o) { break; } } catch (e) { return e; } })(), "from-return");
t("missing return method is fine", (function () { var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { value: 1, done: false }; } }; }; for (var v of o) { break; } return "ok"; })(), "ok");
t("a failing binding pattern closes the iterator", (function () { var log = []; var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { value: null, done: false }; }, return: function () { log.push("return"); return {}; } }; }; try { for (var { x } of o) { } } catch (e) { } return log.join(); })(), "return");
t("next throwing does not call return", (function () { var log = []; var o = {}; o[Symbol.iterator] = function () { return { next: function () { throw "n"; }, return: function () { log.push("r"); return {}; } }; }; try { for (var v of o) { } } catch (e) { } return log.length; })(), 0);
t("spread does not close", (function () { var log = []; [...counterIterable(2, log)]; return log.length; })(), 0);

// ---- generator objects
function* three() { yield 1; yield 2; yield 3; }
t("generator is iterable", drain(three()), "1,2,3");
t("generator done result", (function () { var g = three(); g.next(); g.next(); g.next(); var r = g.next(); return r.done + "," + r.value; })(), "true,undefined");
t("generator return value", (function () { function* g() { yield 1; return "r"; } var it = g(); it.next(); var r = it.next(); return r.value + "," + r.done; })(), "r,true");
t("generator is its own iterator", (function () { var g = three(); return g[Symbol.iterator]() === g; })(), true);
t("generator function is a function", typeof three, "function");
t("generator prototype chain", (function () { var g = three(); return Object.getPrototypeOf(g) === three.prototype; })(), true);
t("generator prototype inherits from the generator prototype", (function () { var GP = Object.getPrototypeOf(three.prototype); return typeof GP.next + typeof GP.return + typeof GP.throw; })(), "functionfunctionfunction");
t("generator function inherits from GeneratorFunction.prototype", (function () { var GFP = Object.getPrototypeOf(three); return GFP !== Function.prototype && Object.getPrototypeOf(GFP.prototype) === Object.getPrototypeOf(Object.getPrototypeOf([][Symbol.iterator]())); })(), true);
t("generator toString tag", Object.prototype.toString.call(three()), "[object Generator]");
t("generator function toString tag", Object.prototype.toString.call(three), "[object GeneratorFunction]");
t("generator functions have a prototype property", typeof three.prototype, "object");
t("generator prototype has no constructor of its own", three.prototype.hasOwnProperty("constructor"), false);
terr("generator is not a constructor", function () { new three(); }, "TypeError");
t("generator body does not run until next", (function () { var ran = false; function* g() { ran = true; yield 1; } var it = g(); return ran; })(), false);
t("generator body runs up to the first yield", (function () { var s = ""; function* g() { s += "a"; yield 1; s += "b"; yield 2; s += "c"; } var it = g(); it.next(); var r1 = s; it.next(); var r2 = s; it.next(); return r1 + "," + r2 + "," + s; })(), "a,ab,abc");
t("generator arguments are bound at call time", (function () { function* g(a, b) { yield a + b; } return g(1, 2).next().value; })(), 3);
t("generator default parameters are evaluated at call time", (function () { var n = 0; function* g(a = ++n) { yield a; } g(); g(); return n; })(), 2);
t("generator this", (function () { var o = { v: 5, *g() { yield this.v; } }; return o.g().next().value; })(), 5);
t("generator arguments object", (function () { function* g() { yield arguments.length; } return g(1, 2, 3).next().value; })(), 3);
t("generator in a class", (function () { class C { *[Symbol.iterator]() { yield "a"; yield "b"; } } return [...new C()].join(""); })(), "ab");
t("generator expression", (function () { var g = function* () { yield 1; }; return g().next().value; })(), 1);
t("named generator expression", (function () { var g = function* inner() { yield typeof inner; }; return g().next().value; })(), "function");
t("generators are independent", (function () { var a = three(), b = three(); a.next(); a.next(); return b.next().value; })(), 1);
t("infinite generators", (function () { function* nat() { var n = 0; while (true) yield n++; } var g = nat(), s = 0; for (var i = 0; i < 100; i++) s += g.next().value; return s; })(), 4950);
t("generator with a loop and early exit", (function () { function* g() { for (var i = 0; ; i++) yield i; } var out = []; for (var v of g()) { if (v > 3) break; out.push(v); } return out.join(); })(), "0,1,2,3");
t("generator as a lazy pipeline", (function () {
    function* map(it, f) { for (var v of it) yield f(v); }
    function* take(it, n) { var i = 0; for (var v of it) { if (i++ >= n) return; yield v; } }
    function* nat() { var n = 1; for (; ;) yield n++; }
    return [...take(map(nat(), function (x) { return x * x; }), 5)].join();
})(), "1,4,9,16,25");
t("fibonacci", (function () { function* f() { var a = 0, b = 1; for (; ;) { yield a; var t = a + b; a = b; b = t; } } var it = f(), r = []; for (var i = 0; i < 10; i++) r.push(it.next().value); return r.join(); })(), "0,1,1,2,3,5,8,13,21,34");
t("recursive generator through yield*", (function () { function* walk(n) { if (n) { yield* walk(n.l); yield n.v; yield* walk(n.r); } } return drain(walk({ v: 2, l: { v: 1 }, r: { v: 3, r: { v: 4 } } })); })(), "1,2,3,4");
t("many suspended generators", (function () { var gs = []; function* g(i) { yield i; yield i * 2; } for (var i = 0; i < 200; i++) { gs.push(g(i)); gs[i].next(); } var s = 0; for (var j = 0; j < 200; j++) s += gs[j].next().value; return s; })(), 39800);

// ---- next, return, throw
t("next passes a value into yield", (function () { function* g() { var a = yield 1; var b = yield a + 1; return a + b; } var it = g(); it.next("ignored"); var r1 = it.next(10); var r2 = it.next(20); return r1.value + "," + r2.value; })(), "11,30");
t("first next argument is ignored", (function () { function* g() { var a = yield; return a; } var it = g(); it.next("x"); return it.next("y").value; })(), "y");
t("yield without operand yields undefined", (function () { function* g() { yield; } return typeof g().next().value; })(), "undefined");
t("return completes the generator", (function () { var it = three(); it.next(); var r = it.return(9); return r.value + "," + r.done + "," + it.next().done; })(), "9,true,true");
t("return before the first next", (function () { var ran = false; function* g() { ran = true; yield 1; } var it = g(); var r = it.return(5); return r.value + "," + r.done + "," + ran + "," + it.next().done; })(), "5,true,false,true");
t("return on a completed generator", (function () { var it = three(); for (var v of it); var r = it.return(7); return r.value + "," + r.done; })(), "7,true");
t("return runs finally blocks", (function () { var s = ""; function* g() { try { yield 1; } finally { s += "f"; } } var it = g(); it.next(); it.return(); return s; })(), "f");
t("return in finally can yield", (function () { function* g() { try { yield 1; } finally { yield "cleanup"; } } var it = g(); it.next(); var r1 = it.return(5); var r2 = it.next(); return r1.value + "," + r1.done + "," + r2.value + "," + r2.done; })(), "cleanup,false,5,true");
t("finally can override the return value", (function () { function* g() { try { yield 1; } finally { return "over"; } } var it = g(); it.next(); return it.return("orig").value; })(), "over");
t("throw into a generator", (function () { function* g() { try { yield 1; } catch (e) { yield "caught " + e; } } var it = g(); it.next(); return it.throw("E").value; })(), "caught E");
t("throw with no handler propagates", (function () { function* g() { yield 1; } var it = g(); it.next(); try { it.throw("boom"); } catch (e) { return e + "," + it.next().done; } })(), "boom,true");
t("throw before the first next", (function () { var ran = false; function* g() { ran = true; yield 1; } var it = g(); try { it.throw("early"); } catch (e) { return e + "," + ran + "," + it.next().done; } })(), "early,false,true");
t("throw on a completed generator", (function () { var it = three(); for (var v of it); try { it.throw("late"); } catch (e) { return e; } })(), "late");
t("exception inside the generator completes it", (function () { function* g() { yield 1; throw "inside"; } var it = g(); it.next(); try { it.next(); } catch (e) { } return it.next().done; })(), true);
t("catch then continue yielding", (function () { function* g() { while (true) { try { yield "ok"; } catch (e) { yield "err:" + e; } } } var it = g(); it.next(); var a = it.throw("1").value; var b = it.next().value; return a + "," + b; })(), "err:1,ok");
terr("re-entering a running generator", function () { var it; function* g() { it.next(); } it = g(); it.next(); }, "TypeError");
terr("next on a non generator", function () { three().next.call({}); }, "TypeError");
terr("next on a primitive", function () { three().next.call(1); }, "TypeError");
t("generator methods named next return/throw exist once", (function () { var GP = Object.getPrototypeOf(three.prototype); return GP.next.length + "," + GP.return.length + "," + GP.throw.length; })(), "1,1,1");

// ---- yield in expression positions
t("yield in an expression", (function () { function* g() { var x = (yield 1) + (yield 2); return x; } var it = g(); it.next(); it.next(10); return it.next(20).value; })(), 30);
t("yield in call arguments", (function () { function f(a, b) { return a * b; } function* g() { return f(yield 1, yield 2); } var it = g(); it.next(); it.next(3); return it.next(4).value; })(), 12);
t("yield in an array literal", (function () { function* g() { return [yield 1, yield 2]; } var it = g(); it.next(); it.next("a"); return it.next("b").value.join(); })(), "a,b");
t("yield in an object literal", (function () { function* g() { return { a: yield 1, b: yield 2 }; } var it = g(); it.next(); it.next("x"); var o = it.next("y").value; return o.a + o.b; })(), "xy");
t("yield in a computed key", (function () { function* g() { return { [yield 1]: 5 }; } var it = g(); it.next(); return it.next("k").value.k; })(), 5);
t("yield in a template literal", (function () { function* g() { return `a${yield 1}b${yield 2}`; } var it = g(); it.next(); it.next("X"); return it.next("Y").value; })(), "aXbY");
t("yield in a conditional", (function () { function* g() { return (yield 1) ? yield "t" : yield "f"; } var it = g(); it.next(); return it.next(false).value; })(), "f");
t("yield in a logical expression", (function () { function* g() { return (yield 1) || (yield 2); } var it = g(); it.next(); var r = it.next(0); var r2 = it.next("v"); return r.value + "," + r2.value; })(), "2,v");
t("yield short circuits", (function () { function* g() { return (yield 1) && (yield 2); } var it = g(); it.next(); var r = it.next(0); return r.value + "," + r.done; })(), "0,true");
t("yield in an assignment target", (function () { function* g() { var o = {}; o[yield 1] = yield 2; return o; } var it = g(); it.next(); it.next("k"); return it.next("v").value.k; })(), "v");
t("yield in a compound assignment", (function () { function* g() { var n = 1; n += yield 1; return n; } var it = g(); it.next(); return it.next(5).value; })(), 6);
t("yield in destructuring defaults", (function () { function* g() { var [a = yield 1] = []; return a; } var it = g(); it.next(); return it.next("d").value; })(), "d");
t("yield in an argument evaluated once", (function () { var calls = 0; function side() { calls++; return 1; } function* g() { var r = side() + (yield 1) + side(); return r; } var it = g(); it.next(); var v = it.next(10).value; return v + "," + calls; })(), "12,2");
t("yield with an iterating loop condition", (function () { function* g() { var i = 0; while ((yield i) < 3) i++; return "end"; } var it = g(); it.next(); it.next(1); it.next(2); return it.next(5).value; })(), "end");
t("yield in a for loop header", (function () { function* g() { for (var i = yield "init"; i < 3; i++) yield i; } var it = g(); it.next(); return drain(it) ; })(), "");
t("yield in switch discriminant and case", (function () { function* g() { switch (yield 1) { case "a": yield "A"; break; default: yield "D"; } } var it = g(); it.next(); return it.next("a").value; })(), "A");
t("yield in a try block with a finally return", (function () { function* g() { try { yield 1; } finally { } return 2; } var it = g(); it.next(); return it.next().value; })(), 2);
t("yield inside a for-in", (function () { function* g() { for (var k in { a: 1, b: 2 }) yield k; } return drain(g()); })(), "a,b");
t("yield inside for-of with destructuring", (function () { function* g() { for (var [a, b] of [[1, 2], [3, 4]]) yield a + b; } return drain(g()); })(), "3,7");
t("yield inside nested functions is not allowed to see the generator", (function () { function* g() { var f = function () { return typeof yield_; }; yield f(); } return g().next().value; })(), "undefined");
t("yield inside labeled loops", (function () { function* g() { outer: for (var i = 0; i < 3; i++) { for (var j = 0; j < 3; j++) { if (j == 1) continue outer; yield i * 10 + j; } } } return drain(g()); })(), "0,10,20");
t("yield loop with break", (function () { function* g() { for (var i = 0; ; i++) { if (i == 3) break; yield i; } yield "done"; } return drain(g()); })(), "0,1,2,done");
t("yield with do-while", (function () { function* g() { var i = 0; do { yield i; } while (++i < 3); } return drain(g()); })(), "0,1,2");
t("yield in finally after break", (function () { function* g() { for (var i = 0; i < 2; i++) { try { if (i == 0) continue; break; } finally { yield "f" + i; } } } return drain(g()); })(), "f0,f1");
t("complex try catch finally with yields", (function () { function* g() { for (var i = 0; i < 3; i++) { try { if (i == 1) throw "e"; yield i; } catch (e) { yield "c"; } finally { yield "f" + i; } } } return drain(g()); })(), "0,f0,c,f1,2,f2");
t("yield precedence", (function () { function* g() { var x = yield 1 + 1; return x; } var it = g(); return it.next().value; })(), 2);
t("yield as a statement keeps asi", (function () { function* g() { yield
1; } return typeof g().next().value; })(), "undefined");
t("yield star in an expression statement", (function () { function* g() { yield* [1, 2]; yield* "ab"; } return drain(g()); })(), "1,2,a,b");

// ---- yield*
function* inner() { var x = yield "i1"; yield "i2:" + x; return "ret"; }
t("yield star yields inner values", (function () { function* g() { yield* inner(); yield "o"; } return drain(g()); })(), "i1,i2:undefined,o");
t("yield star result is the inner return value", (function () { function* g() { var r = yield* inner(); yield "r=" + r; } return drain(g()); })(), "i1,i2:undefined,r=ret");
t("yield star passes next values", (function () { function* g() { yield* inner(); } var it = g(); it.next(); return it.next("passed").value; })(), "i2:passed");
t("yield star delegates throw", (function () { function* in2() { try { yield 1; } catch (e) { yield "in:" + e; } } function* g() { yield* in2(); } var it = g(); it.next(); return it.throw("T").value; })(), "in:T");
t("yield star delegates return", (function () { var s = ""; function* in2() { try { yield 1; } finally { s += "f"; } } function* g() { yield* in2(); } var it = g(); it.next(); var r = it.return("R"); return r.value + "," + r.done + "," + s; })(), "R,true,f");
t("yield star over a custom iterator", (function () { function* g() { yield* counterIterable(3); } return drain(g()); })(), "0,1,2");
t("yield star over an array", (function () { function* g() { yield* [1, 2]; } return drain(g()); })(), "1,2");
t("yield star over a string", (function () { function* g() { yield* "xy"; } return drain(g()); })(), "x,y");
t("yield star with return method on early exit", (function () { var log = []; function* g() { yield* counterIterable(5, log); } var it = g(); it.next(); it.return(); return log.join(); })(), "return");
t("yield star throw to an iterator without throw closes it", (function () { var log = []; function* g() { yield* counterIterable(5, log); } var it = g(); it.next(); try { it.throw("x"); } catch (e) { return e.name + "," + log.join(); } })(), "TypeError,return");
terr("yield star over a non iterable", function () { function* g() { yield* 5; } g().next(); }, "TypeError");
t("yield star evaluates its operand once", (function () { var n = 0; function src() { n++; return [1]; } function* g() { yield* src(); } drain(g()); return n; })(), 1);
t("yield star passes the result objects through", (function () { var o = {}; o[Symbol.iterator] = function () { return { next: function () { return { value: "v", done: false, extra: 1 }; } }; }; function* g() { yield* o; } return g().next().extra; })(), 1);
t("nested yield star", (function () { function* a() { yield 1; } function* b() { yield* a(); yield 2; } function* c() { yield* b(); yield 3; } return drain(c()); })(), "1,2,3");

// ---- iterators of the built-ins
t("array iterator next", (function () { var it = [7, 8][Symbol.iterator](); var a = it.next(), b = it.next(), c = it.next(); return a.value + "," + b.value + "," + c.done; })(), "7,8,true");
t("array iterator stays done", (function () { var it = [1][Symbol.iterator](); it.next(); it.next(); return it.next().done; })(), true);
t("array iterator sees growth before completion", (function () { var a = [1], it = a[Symbol.iterator](); it.next(); a.push(2); return it.next().value; })(), 2);
t("array iterator toString tag", Object.prototype.toString.call([][Symbol.iterator]()), "[object Array Iterator]");
t("iterator prototype is iterable", (function () { var IP = Object.getPrototypeOf(Object.getPrototypeOf([][Symbol.iterator]())); return IP[Symbol.iterator].call(5) === 5; })(), true);
t("string iterator", (function () { var it = "ab"[Symbol.iterator](); return it.next().value + it.next().value + it.next().done; })(), "abtrue");
t("string iterator toString tag", Object.prototype.toString.call(""[Symbol.iterator]()), "[object String Iterator]");
t("array values and Symbol.iterator are the same function", Array.prototype[Symbol.iterator] === Array.prototype.values, true);
t("array-like iteration with call", (function () { var it = Array.prototype[Symbol.iterator].call({ length: 2, 0: "p", 1: "q" }); return it.next().value + it.next().value; })(), "pq");
