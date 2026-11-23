// P13-04: control flow statements and completion records.

// ---- if / else
function ifElse(x) { if (x > 0) return "pos"; else if (x < 0) return "neg"; else return "zero"; }
t("if", ifElse(1) + ifElse(-1) + ifElse(0), "posnegzero");
t("dangling else binds to the nearest if", (function () { var r = ""; if (true) if (false) r = "a"; else r = "b"; return r; })(), "b");
t("if without braces and empty statement", (function () { var r = 0; if (true); r = 1; return r; })(), 1);

// ---- for / while / do-while
t("for", (function () { var s = 0; for (var i = 1; i <= 4; i++) s += i; return s; })(), 10);
t("for with missing parts", (function () { var i = 0; for (; ;) { if (++i > 3) break; } return i; })(), 4);
t("for with comma update", (function () { var a = 0, b = 10; for (; a < b; a++, b--); return a + "," + b; })(), "5,5");
t("for declares multiple", (function () { for (var i = 0, j = 10; i < j; i += 5, j -= 1); return i + "," + j; })(), "10,8");
t("while", (function () { var i = 0; while (i < 5) i++; return i; })(), 5);
t("while never runs", (function () { var r = 0; while (false) r = 1; return r; })(), 0);
t("do-while runs once", (function () { var c = 0; do { c++; } while (false); return c; })(), 1);
t("do-while condition after body", (function () { var c = 0; do { c++; } while (c < 3); return c; })(), 3);
t("do-while continue evaluates the condition", (function () { var c = 0, n = 0; do { c++; if (c < 3) continue; n++; } while (c < 3); return c + "," + n; })(), "3,1");
t("do-while without semicolon", (function () { var i = 0; do i++; while (i < 3) return i; })(), 3);

// ---- break / continue / labels
t("break", (function () { var i = 0; for (; ; i++) { if (i == 3) break; } return i; })(), 3);
t("continue", (function () { var s = ""; for (var i = 0; i < 5; i++) { if (i % 2) continue; s += i; } return s; })(), "024");
t("continue in for runs the update", (function () { var n = 0; for (var i = 0; i < 3; i++) { continue; } return i; })(), 3);
t("break from nested loop only exits the inner loop", (function () { var s = ""; for (var i = 0; i < 2; i++) { for (var j = 0; j < 3; j++) { if (j == 1) break; s += i + "" + j + ";"; } } return s; })(), "00;10;");
t("labeled break exits the outer loop", (function () { var s = ""; outer: for (var i = 0; i < 3; i++) { for (var j = 0; j < 3; j++) { if (j == 1) break outer; s += i + "" + j; } } return s; })(), "00");
t("labeled continue", (function () { var s = ""; outer: for (var i = 0; i < 3; i++) { for (var j = 0; j < 3; j++) { if (j == 1) continue outer; s += i + "" + j + ";"; } } return s; })(), "00;10;20;");
t("labeled block break", (function () { var r = 0; blk: { r = 1; break blk; r = 2; } return r; })(), 1);
t("label on while", (function () { var n = 0; w: while (true) { while (true) { n++; if (n > 2) break w; } } return n; })(), 3);
t("labels nest", (function () { var r = ""; a: b: for (var i = 0; i < 2; i++) { r += i; continue a; } return r; })(), "01");
t("label does not leak into functions", (function () { var r = 0; x: { r = (function () { var o = 1; x: { o = 2; } return o; })(); } return r; })(), 2);
t("labeled if", (function () { var r = 0; l: if (true) { r = 1; break l; } return r; })(), 1);
t("label reused after the statement", (function () { var r = 0; l: for (var i = 0; i < 1; i++) { r++; } l: for (var i = 0; i < 1; i++) { r++; } return r; })(), 2);

// ---- switch
function sw(x) { switch (x) { case 1: return "one"; case "1": return "string"; case 2: case 3: return "twothree"; default: return "other"; } }
t("switch strict comparison", sw(1) + "," + sw("1") + "," + sw(3) + "," + sw(9), "one,string,twothree,other");
t("switch fallthrough", (function () { var r = ""; switch (2) { case 1: r += "1"; case 2: r += "2"; case 3: r += "3"; break; case 4: r += "4"; } return r; })(), "23");
t("switch default in the middle", (function () { var r = ""; switch (9) { case 1: r += "1"; default: r += "d"; case 2: r += "2"; } return r; })(), "d2");
t("switch default not taken when a later case matches", (function () { var r = ""; switch (2) { default: r += "d"; case 2: r += "2"; } return r; })(), "2");
t("switch without match and without default", (function () { var r = "x"; switch (5) { case 1: r = "no"; } return r; })(), "x");
t("switch case expressions are lazy", (function () { var s = ""; function c(v) { s += v; return v; } switch (2) { case c(1): case c(2): case c(3): } return s; })(), "12");
t("switch discriminant evaluated once", (function () { var c = 0; function d() { c++; return 1; } switch (d()) { case 0: case 1: case 2: } return c; })(), 1);
t("switch continue inside loop", (function () { var r = ""; for (var i = 0; i < 3; i++) { switch (i) { case 1: continue; } r += i; } return r; })(), "02");
t("switch break only leaves the switch", (function () { var r = ""; for (var i = 0; i < 2; i++) { switch (i) { case 0: break; } r += i; } return r; })(), "01");
t("empty switch", (function () { switch (1) { } return 1; })(), 1);
t("switch compares NaN as never equal", (function () { switch (NaN) { case NaN: return "eq"; default: return "ne"; } })(), "ne");
t("switch with strings", (function () { switch ("b") { case "a": return 1; case "b": return 2; } })(), 2);

// ---- for-in
t("for-in own properties in insertion order", (function () { var r = []; for (var k in { b: 1, a: 2 }) r.push(k); return r.join(); })(), "b,a");
t("for-in integer keys first", (function () { var r = []; for (var k in { b: 1, 2: 1, a: 2, 1: 1 }) r.push(k); return r.join(); })(), "1,2,b,a");
t("for-in includes inherited", (function () { var p = { x: 1 }, o = Object.create(p); o.y = 2; var r = []; for (var k in o) r.push(k); return r.join(); })(), "y,x");
t("for-in skips shadowed non-enumerable", (function () { var p = { x: 1 }, o = Object.create(p); Object.defineProperty(o, "x", { value: 1, enumerable: false }); var r = []; for (var k in o) r.push(k); return r.length; })(), 0);
t("for-in skips symbols", (function () { var o = {}; o[Symbol()] = 1; o.a = 1; var r = []; for (var k in o) r.push(k); return r.join(); })(), "a");
t("for-in over array", (function () { var r = []; for (var k in ["x", "y"]) r.push(k); return r.join(); })(), "0,1");
t("for-in over string", (function () { var r = []; for (var k in "ab") r.push(k); return r.join(); })(), "0,1");
t("for-in over null does nothing", (function () { var n = 0; for (var k in null) n++; for (var k in undefined) n++; return n; })(), 0);
t("for-in deleted property is skipped", (function () { var o = { a: 1, b: 2, c: 3 }, r = []; for (var k in o) { r.push(k); delete o.b; } return r.join(); })(), "a,c");
t("for-in added property is not visited", (function () { var o = { a: 1 }, r = []; for (var k in o) { r.push(k); o.z = 1; } return r.join(); })(), "a");
t("for-in with member target", (function () { var o = {}, r = { }; for (o.k in { a: 1 }); return o.k; })(), "a");
t("for-in break and continue", (function () { var r = ""; for (var k in { a: 1, b: 2, c: 3 }) { if (k == "b") continue; if (k == "c") break; r += k; } return r; })(), "a");
t("for-in with destructuring target", (function () { var r = []; for (var [a, b] in { xy: 1 }) r.push(a + b); return r.join(); })(), "xy");
t("for-in with expression", (function () { var r = []; for (var k in (1, { a: 1 })) r.push(k); return r.join(); })(), "a");
t("for-in sees object coercion of primitive", (function () { var r = []; for (var k in 123) r.push(k); return r.length; })(), 0);

// ---- return / throw / try
t("return exits the function", (function () { for (var i = 0; ; i++) { if (i == 2) return i; } })(), 2);
t("return without value", (function () { return; })(), undefined);
t("return in nested loops", (function () { for (var i = 0; i < 3; i++) { for (var j = 0; j < 3; j++) { if (i * j == 2) return i + "" + j; } } })(), "12");
t("throw and catch a value", (function () { try { throw 42; } catch (e) { return e; } })(), 42);
t("throw an object", (function () { try { throw { code: 7 }; } catch (e) { return e.code; } })(), 7);
t("throw undefined", (function () { try { throw undefined; } catch (e) { return typeof e; } })(), "undefined");
t("catch an error", (function () { try { null.x; } catch (e) { return e.name; } })(), "TypeError");
t("catch does not run without a throw", (function () { var r = "ok"; try { } catch (e) { r = "bad"; } return r; })(), "ok");
t("catch with destructuring", (function () { try { throw { message: "m", code: 3 }; } catch ({ message, code }) { return message + code; } })(), "m3");
t("catch with array destructuring", (function () { try { throw [1, 2]; } catch ([a, b]) { return a + b; } })(), 3);
t("rethrow", (function () { try { try { throw "a"; } catch (e) { throw e + "b"; } } catch (e2) { return e2; } })(), "ab");
t("exception through function calls", (function () { function a() { b(); } function b() { throw "deep"; } try { a(); } catch (e) { return e; } })(), "deep");
t("catch parameter is a fresh binding per catch", (function () { var f = []; for (var i = 0; i < 2; i++) { try { throw i; } catch (e) { f.push(function () { return e; }); } } return f[0]() + "" + f[1](); })(), "01");
t("try without catch needs finally", (function () { var r = ""; try { try { throw 1; } finally { r += "f"; } } catch (e) { r += "c"; } return r; })(), "fc");
t("error object properties", (function () { try { throw new TypeError("boom"); } catch (e) { return e.name + ":" + e.message + ":" + (e instanceof Error); } })(), "TypeError:boom:true");
t("native errors are catchable", (function () { try { undefinedFunction(); } catch (e) { return e instanceof ReferenceError; } })(), true);
t("syntax errors from eval are catchable", (function () { try { eval("var 1 = 2;"); } catch (e) { return e instanceof SyntaxError; } })(), true);

// ---- finally
t("finally runs after try", (function () { var r = ""; try { r += "t"; } finally { r += "f"; } return r; })(), "tf");
t("finally runs after a throw", (function () { var r = ""; try { try { throw 1; } finally { r += "f"; } } catch (e) { r += "c"; } return r; })(), "fc");
t("finally runs after return", (function () { var r = ""; function f() { try { return "x"; } finally { r += "f"; } } return f() + r; })(), "xf");
t("finally can override return", (function () { try { return 1; } finally { return 2; } })(), 2);
t("finally can override a throw", (function () { try { throw "e"; } finally { return "swallowed"; } })(), "swallowed");
t("finally return overrides catch return", (function () { try { throw 1; } catch (e) { return "c"; } finally { return "f"; } })(), "f");
t("finally without return keeps the return value", (function () { var x = 1; try { return x; } finally { x = 2; } })(), 1);
t("finally break overrides return", (function () { for (; ;) { try { return "r"; } finally { break; } } return "after"; })(), "after");
t("finally continue overrides break", (function () { var n = 0; for (var i = 0; i < 2; i++) { try { break; } finally { n++; continue; } } return n; })(), 2);
t("finally runs for break", (function () { var r = ""; for (; ;) { try { break; } finally { r += "f"; } } return r; })(), "f");
t("finally runs for continue", (function () { var r = ""; for (var i = 0; i < 2; i++) { try { continue; } finally { r += "f"; } } return r; })(), "ff");
t("nested finally order", (function () { var r = ""; try { try { throw 1; } finally { r += "a"; } } catch (e) { r += "b"; } finally { r += "c"; } return r; })(), "abc");
t("throw inside finally replaces the pending completion", (function () { try { try { throw "first"; } finally { throw "second"; } } catch (e) { return e; } })(), "second");
t("finally with labeled break", (function () { var r = ""; l: { try { break l; } finally { r += "f"; } r += "x"; } return r; })(), "f");
t("return value is evaluated before finally", (function () { var r = []; function f() { try { return r.push("ret"); } finally { r.push("fin"); } } f(); return r.join(); })(), "ret,fin");
t("finally in loop runs for each iteration", (function () { var c = 0; for (var i = 0; i < 3; i++) { try { } finally { c++; } } return c; })(), 3);
t("nested try in catch", (function () { try { throw 1; } catch (e) { try { throw 2; } catch (e2) { return e + e2; } } })(), 3);

// ---- completion values (seen through eval)
t("completion of expression statement", eval("1; 2; 3"), 3);
t("completion of if", eval("1; if (true) { 2; }"), 2);
t("completion of empty if is undefined", eval("1; if (true) { }"), undefined);
t("completion of for", eval("for (var i = 0; i < 3; i++) { i; }"), 2);
t("completion of while with break", eval("var n = 0; while (true) { n++; if (n == 3) { n * 10; break; } n; }"), 30);
t("completion of switch", eval("switch (1) { case 1: 'a'; break; }"), "a");
t("completion of try", eval("try { 'tried'; } finally { 'fin'; }"), "tried");
t("completion of catch", eval("try { throw 1; } catch (e) { 'caught'; }"), "caught");
t("completion of labeled statement", eval("l: { 5; break l; }"), 5);
t("completion of var statement is empty", eval("1; var z = 2;"), 1);
t("completion of function declaration is empty", eval("7; function fz() { }"), 7);
t("completion of block", eval("1; { 2; }"), 2);
t("completion of do-while is the value of the last body", eval("var k = 0; do { k++; } while (k < 3);"), 2);
t("completion of with", eval("with ({}) { 'w'; }"), "w");
t("completion of continue in loop", eval("for (var i = 0; i < 2; i++) { 'body'; continue; }"), "body");
t("completion of empty program", eval(""), undefined);
t("completion of class declaration is empty", eval("4; class CZ { }"), 4);
