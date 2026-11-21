// P13-05: functions, closures, this, arguments, call/apply/bind, new and prototypes.

// ---- function objects
function plain(a, b, c) { return a; }
t("length counts parameters", plain.length, 3);
t("length stops at default", (function (a, b = 1, c) { }).length, 1);
t("length excludes rest", (function (a, ...r) { }).length, 1);
t("length of destructuring parameter", (function ({ a }, [b]) { }).length, 2);
t("name of declaration", plain.name, "plain");
t("name of anonymous function expression", (function () { }).name, "");
var inferred = function () { };
t("name inferred for variable", inferred.name, "inferred");
var objNames = { prop: function () { }, arrow: () => 1, method() { }, get acc() { return 1; }, ["comp" + 1]: function () { } };
t("name inferred for property", objNames.prop.name, "prop");
t("name inferred for arrow property", objNames.arrow.name, "arrow");
t("name of method", objNames.method.name, "method");
t("name of computed key", objNames.comp1.name, "comp1");
t("name of getter", Object.getOwnPropertyDescriptor(objNames, "acc").get.name, "get acc");
t("name of named expression keeps its own name", (function own() { }).name, "own");
t("name inferred for default value", (function (f = function () { }) { return f.name; })(), "f");
t("name inferred for destructuring default", (function () { var { g = function () { } } = {}; return g.name; })(), "g");
t("name of assignment target", (function () { var h; h = function () { }; return h.name; })(), "h");
t("name inferred for symbol key", (function () { var s = Symbol("desc"), o = { [s]: function () { } }; return o[s].name; })(), "[desc]");
t("name of class", (class Named { }).name, "Named");
t("name property attributes", JSON_like(Object.getOwnPropertyDescriptor(plain, "name")), "false,false,true");
t("length property attributes", JSON_like(Object.getOwnPropertyDescriptor(plain, "length")), "false,false,true");
function JSON_like(d) { return d.writable + "," + d.enumerable + "," + d.configurable; }
t("function has prototype", typeof plain.prototype, "object");
t("prototype.constructor", plain.prototype.constructor === plain, true);
t("prototype attributes", JSON_like(Object.getOwnPropertyDescriptor(plain, "prototype")), "true,false,false");
t("arrow has no prototype", (() => 1).prototype, undefined);
t("method has no prototype", ({ m() { } }).m.prototype, undefined);
t("function prototype of functions", Object.getPrototypeOf(plain) === Function.prototype, true);
t("typeof Function.prototype", typeof Function.prototype, "function");
t("function toString shows the source", (function (a, b) { return a + b; }).toString(), "function (a, b) { return a + b; }");
t("toString of a declaration", plain.toString(), "function plain(a, b, c) { return a; }");
t("toString of an arrow", (x => x * 2).toString(), "x => x * 2");
t("toString of a method", ({ m() { return 1; } }).m.toString(), "m() { return 1; }");
t("toString of a class", (class K { }).toString(), "class K { }");
t("native function toString", Object.prototype.hasOwnProperty.toString(), "function hasOwnProperty() { [native code] }");
t("functions are objects", plain instanceof Object, true);
t("function properties", (function () { function f() { } f.extra = 1; return f.extra; })(), 1);
t("function name is not writable", (function () { function f() { } f.name = "x"; return f.name; })(), "f");

// ---- this binding
t("this in sloppy plain call is the global object", (function () { return this === globalThis; })(), true);
t("this in strict plain call is undefined", (function () { "use strict"; return this; })(), undefined);
t("this in method", ({ m: function () { return this.v; }, v: 1 }).m(), 1);
t("sloppy this boxes a primitive", (function () { return typeof this; }).call(5), "object");
t("strict this keeps a primitive", (function () { "use strict"; return typeof this; }).call(5), "number");
t("sloppy this null becomes the global object", (function () { return this === globalThis; }).call(null), true);
t("sloppy this undefined becomes the global object", (function () { return this === globalThis; }).call(undefined), true);
t("strict this null stays null", (function () { "use strict"; return this; }).call(null), null);
t("sloppy this string is a String object", (function () { return this instanceof String; }).call("x"), true);
t("sloppy this boolean is a Boolean object", (function () { return this instanceof Boolean; }).call(true), true);
t("this of nested function is not the outer this", (function () { var o = { v: 1, f: function () { return (function () { return this === globalThis; })(); } }; return o.f(); })(), true);
t("this saved in a variable", (function () { var o = { v: 3, f: function () { var self = this; return (function () { return self.v; })(); } }; return o.f(); })(), 3);
t("this in constructor", (function () { function P() { this.a = 1; } var p = new P(); return p.a; })(), 1);
t("this in accessors", (function () { var o = { _v: 2, get v() { return this._v; }, set v(x) { this._v = x * 2; } }; o.v = 5; return o.v; })(), 10);
t("this at top level of eval", eval("this === globalThis"), true);
t("this in a callback called by a built-in", (function () { var r; [1].forEach(function () { r = this === globalThis; }); return r; })(), true);
t("this in a callback with thisArg", (function () { var r, o = {}; [1].forEach(function () { r = this === o; }, o); return r; })(), true);

// ---- call, apply, bind
function who() { return this.name + ":" + Array.prototype.join.call(arguments, "+"); }
t("call", who.call({ name: "c" }, 1, 2), "c:1+2");
t("apply with array", who.apply({ name: "a" }, [1, 2]), "a:1+2");
t("apply without arguments", who.apply({ name: "n" }), "n:");
t("apply with null arguments", who.apply({ name: "z" }, null), "z:");
t("apply with array-like", who.apply({ name: "l" }, { length: 2, 0: "x", 1: "y" }), "l:x+y");
t("apply with arguments object", (function () { function outer() { return who.apply({ name: "o" }, arguments); } return outer(7, 8); })(), "o:7+8");
terr("apply with a primitive argument list", function () { who.apply({}, 5); }, "TypeError");
t("call without arguments", (function () { function f() { return arguments.length; } return f.call(null); })(), 0);
terr("call on a non function", function () { Function.prototype.call.call({}); }, "TypeError");
t("bind fixes this", (function () { var f = who.bind({ name: "b" }); return f(1); })(), "b:1");
t("bind ignores a later this", (function () { var f = who.bind({ name: "b" }); return f.call({ name: "other" }, 2); })(), "b:2");
t("bind partial arguments", (function () { var f = who.bind({ name: "p" }, 1, 2); return f(3, 4); })(), "p:1+2+3+4");
t("bind twice keeps the first this", (function () { var f = who.bind({ name: "first" }).bind({ name: "second" }); return f(); })(), "first:");
t("bound function name", who.bind(null).name, "bound who");
t("bound function length", (function (a, b, c) { }).bind(null, 1).length, 2);
t("bound length is not negative", (function (a) { }).bind(null, 1, 2, 3).length, 0);
t("bound function has no prototype", who.bind(null).prototype, undefined);
t("bound constructor", (function () { function P(a, b) { this.s = a + b; } var B = P.bind(null, 1); return new B(2).s; })(), 3);
t("bound constructor instance of target", (function () { function P() { } var B = P.bind(null); return new B() instanceof P; })(), true);
t("bound constructor ignores bound this", (function () { function P() { this.v = 1; } var B = P.bind({ v: 9 }); return new B().v; })(), 1);
t("new.target of a bound constructor is the target", (function () { function P() { return new.target === P; } var B = P.bind(null); return new B() instanceof P; })(), true);
t("bind keeps working with strict functions", (function () { var f = (function () { "use strict"; return this; }).bind(7); return f(); })(), 7);
t("bind on an arrow keeps the lexical this", (function () { var o = { v: 4, f: function () { return (() => this.v).bind({ v: 9 })(); } }; return o.f(); })(), 4);

// ---- arguments object
t("arguments length", (function () { return arguments.length; })(1, 2, 3), 3);
t("arguments indexes", (function () { return arguments[0] + arguments[2]; })(1, 2, 3), 4);
t("arguments missing index", (function () { return arguments[5]; })(1), undefined);
t("arguments is not an array", (function () { return Array.isArray(arguments); })(), false);
t("arguments class", (function () { return Object.prototype.toString.call(arguments); })(), "[object Arguments]");
t("arguments mapped to parameters (sloppy)", (function (a) { arguments[0] = 9; return a; })(1), 9);
t("parameters mapped to arguments (sloppy)", (function (a) { a = 8; return arguments[0]; })(1), 8);
t("arguments unmapped in strict mode", (function (a) { "use strict"; arguments[0] = 9; return a; })(1), 1);
t("arguments unmapped with default parameters", (function (a, b = 2) { arguments[0] = 9; return a; })(1), 1);
t("arguments unmapped with rest", (function (a, ...r) { arguments[0] = 9; return a; })(1), 1);
t("arguments unmapped with destructuring", (function ({ x }) { arguments[0] = 9; return x; })({ x: 1 }), 1);
t("arguments not mapped beyond the passed count", (function (a, b) { arguments[1] = 9; return b; })(1), undefined);
t("arguments mapping removed by delete", (function (a) { delete arguments[0]; arguments[0] = 7; return a; })(1), 1);
t("arguments mapping removed by defineProperty", (function (a) { Object.defineProperty(arguments, "0", { writable: false }); a = 5; return arguments[0]; })(1), 1);
t("arguments callee in sloppy mode", (function f() { return arguments.callee === f; })(), true);
terr("arguments callee in strict mode throws", function () { (function () { "use strict"; return arguments.callee; })(); }, "TypeError");
t("arguments iterator", (function () { var r = []; for (var v of arguments) r.push(v); return r.join(); })(1, 2, 3), "1,2,3");
t("arguments spread", (function () { return [...arguments].length; })(1, 2), 2);
t("arguments length is writable", (function () { arguments.length = 1; return arguments.length; })(1, 2, 3), 1);
t("arguments in nested function is its own", (function () { function inner() { return arguments.length; } return inner(1); })(1, 2, 3), 1);
t("arguments shadowed by a parameter", (function (arguments) { return arguments; })(5), 5);
t("arguments shadowed by a var initialiser", (function () { var arguments = 3; return arguments; })(), 3);
t("arguments shadowed by a function declaration", (function () { function arguments() { } return typeof arguments; })(), "function");
t("arguments keys", (function () { return Object.keys(arguments).join(); })("a", "b"), "0,1");
t("arguments toString tag", (function () { return String(arguments); })(), "[object Arguments]");

// ---- closures
function makeAdder(n) { return function (m) { return n + m; }; }
t("closure adder", makeAdder(2)(3), 5);
t("closures keep separate environments", makeAdder(1)(1) + makeAdder(10)(1), 13);
t("closure outlives the function", (function () { var keep; (function () { var secret = "s"; keep = function () { return secret; }; })(); return keep(); })(), "s");
t("closure modifies the outer variable", (function () { var n = 0; function inc() { n++; } inc(); inc(); return n; })(), 2);
t("closures in an object share state", (function () { function mk() { var v = 0; return { inc: function () { return ++v; }, get: function () { return v; } }; } var o = mk(); o.inc(); o.inc(); return o.get(); })(), 2);
t("memoize with closure", (function () { var calls = 0; function memo(f) { var cache = {}; return function (x) { if (!(x in cache)) { cache[x] = f(x); } return cache[x]; }; } var sq = memo(function (x) { calls++; return x * x; }); sq(3); sq(3); return calls; })(), 1);
t("parameters live in the function scope", (function (a) { return (function () { return a; })(); })(4), 4);
t("default parameter closure sees the parameter scope", (function (a, f = function () { return a; }) { a = 9; return f(); })(1), 9);
t("body var of the same name is a separate binding from parameter expressions", (function (a, f = function () { return a; }) { var a = 5; return f() + "," + a; })(1), "1,5");
t("immediately invoked function expression", (function () { return 7; })(), 7);
t("IIFE with unary operator", (function () { var r = 0; !function () { r = 1; }(); return r; })(), 1);

// ---- recursion and function values
t("recursion with named function", (function () { function fact(n) { return n <= 1 ? 1 : n * fact(n - 1); } return fact(10); })(), 3628800);
t("mutual recursion", (function () { function even(n) { return n == 0 ? true : odd(n - 1); } function odd(n) { return n == 0 ? false : even(n - 1); } return even(10); })(), true);
t("functions as arguments", (function () { function apply(f, x) { return f(x); } return apply(function (v) { return v * 3; }, 4); })(), 12);
t("returning functions", (function () { return function () { return function () { return "deep"; }; }; })()()(), "deep");
t("function in an array", [function () { return 1; }][0](), 1);
t("method chaining", (function () { var o = { v: 0, add: function (n) { this.v += n; return this; } }; return o.add(1).add(2).v; })(), 3);
t("function declared in a conditional is hoisted per annex B", (function () { if (true) { function cond() { return 1; } } return cond(); })(), 1);
t("function expression assigned later", (function () { var f; f = function () { return "later"; }; return f(); })(), "later");

// ---- new and prototypes
function Animal(name) { this.name = name; }
Animal.prototype.speak = function () { return this.name + " speaks"; };
function Dog(name) { Animal.call(this, name); }
Dog.prototype = Object.create(Animal.prototype);
Dog.prototype.constructor = Dog;
Dog.prototype.speak = function () { return Animal.prototype.speak.call(this) + " woof"; };
var dog = new Dog("rex");
t("prototype inheritance", dog.speak(), "rex speaks woof");
t("instanceof along the chain", dog instanceof Animal && dog instanceof Dog, true);
t("getPrototypeOf", Object.getPrototypeOf(dog) === Dog.prototype, true);
t("own vs inherited property", dog.hasOwnProperty("name") + "," + dog.hasOwnProperty("speak"), "true,false");
t("constructor property through the chain", dog.constructor === Dog, true);
t("isPrototypeOf", Animal.prototype.isPrototypeOf(dog), true);
t("prototype methods are shared", (function () { var a = new Animal("a"), b = new Animal("b"); return a.speak === b.speak; })(), true);
t("changing the prototype later is visible", (function () { function F() { } var f = new F(); F.prototype.late = "yes"; return f.late; })(), "yes");
t("replacing the prototype later is not visible to old instances", (function () { function F() { } var f = new F(); F.prototype = { other: 1 }; return f.other; })(), undefined);
t("replaced prototype is used by new instances", (function () { function F() { } F.prototype = { other: 1 }; return new F().other; })(), 1);
t("non object prototype falls back to Object.prototype", (function () { function F() { } F.prototype = 5; return Object.getPrototypeOf(new F()) === Object.prototype; })(), true);
t("new.target in a function", (function () { function F() { return new.target; } return F() === undefined && typeof new F().constructor; })(), "function");
t("new.target value", (function () { var nt; function F() { nt = new.target; } new F(); return nt === F; })(), true);
t("Object.create with null prototype", Object.getPrototypeOf(Object.create(null)), null);
t("__proto__ accessor", (function () { var p = { x: 1 }, o = {}; o.__proto__ = p; return o.x + "," + (o.__proto__ === p); })(), "1,true");
t("__proto__ in an object literal", ({ __proto__: { inherited: 1 } }).inherited, 1);
t("setPrototypeOf", (function () { var o = {}; Object.setPrototypeOf(o, { s: 2 }); return o.s; })(), 2);
terr("prototype cycles are rejected", function () { var a = {}, b = Object.create(a); Object.setPrototypeOf(a, b); }, "TypeError");
t("function call with a getter on the prototype chain", (function () { var p = { get g() { return this.k; } }, o = Object.create(p); o.k = 6; return o.g; })(), 6);
t("constructor called as a function in sloppy mode pollutes the global object", (function () { function P() { this.polluted = 1; } P(); var r = globalThis.polluted; delete globalThis.polluted; return r; })(), 1);
terr("constructor called as a function in strict mode throws", function () { function P() { "use strict"; this.x = 1; } P(); }, "TypeError");
t("Symbol.hasInstance on a function", (function () { function F() { } return F[Symbol.hasInstance]({}); })(), false);
t("own property names of a function", (function () { function f(a) { } return Object.getOwnPropertyNames(f).join(); })(), "length,name,prototype");
t("own property names of an arrow function", Object.getOwnPropertyNames(() => 1).join(), "length,name");
t("recursion depth is reasonable", (function () { function d(n) { return n == 0 ? 0 : 1 + d(n - 1); } return d(200); })(), 200);
