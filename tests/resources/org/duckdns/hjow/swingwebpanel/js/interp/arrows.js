// P13-07: arrow functions.

// ---- forms
t("single parameter", (x => x + 1)(1), 2);
t("parenthesized parameter", ((x) => x + 1)(1), 2);
t("no parameters", (() => 7)(), 7);
t("two parameters", ((a, b) => a * b)(3, 4), 12);
t("block body", ((a) => { var r = a * 2; return r; })(4), 8);
t("block body without return", (() => { 1; })(), undefined);
t("object literal body needs parentheses", (() => ({ a: 1 }))().a, 1);
t("block body with a label-like object", (() => { a: 1; })(), undefined);
t("nested arrows", (a => b => c => a + b + c)(1)(2)(3), 6);
t("arrow returning an arrow", (() => () => "inner")()(), "inner");
t("arrow in a conditional", (true ? x => 1 : x => 2)(0), 1);
t("arrow with default parameters", ((a, b = 2) => a + b)(1), 3);
t("arrow with rest", ((...r) => r.length)(1, 2, 3), 3);
t("arrow with destructuring", (({ a }, [b]) => a + b)({ a: 1 }, [2]), 3);
t("arrow with destructuring defaults", (({ a = 5 } = {}) => a)(), 5);
t("arrow as an argument", [1, 2, 3].map(x => x * 2).join(), "2,4,6");
t("arrow with forEach", (function () { var s = 0; [1, 2, 3].forEach(x => { s += x; }); return s; })(), 6);
t("arrow body is an assignment expression", (function () { var o = {}; var f = () => o.x = 5; f(); return o.x; })(), 5);
t("arrow with comma body in parentheses", (() => (1, 2))(), 2);
t("arrow with a conditional body", (x => x > 0 ? "pos" : "non")(1), "pos");
t("arrow with a template body", (x => `v${x}`)(3), "v3");
t("arrow immediately invoked with new line formatting", (
    () => "ok"
)(), "ok");
t("arrow with a typeof body", (x => typeof x)(1), "number");
t("arrow with yield as an identifier in sloppy code", (function () { var f = yield => yield; return f(3); })(), 3);

// ---- this
t("this is lexical", (function () { var o = { v: 5, f: function () { return (() => this.v)(); } }; return o.f(); })(), 5);
t("this of a method is not used by an arrow property", (function () { var o = { v: 5, a: () => typeof this }; return o.a(); })(), "object");
t("call does not rebind this", (function () { var o = { v: 1, f: function () { return (() => this.v).call({ v: 2 }); } }; return o.f(); })(), 1);
t("apply does not rebind this", (function () { var o = { v: 1, f: function () { return (() => this.v).apply({ v: 2 }); } }; return o.f(); })(), 1);
t("bind does not rebind this", (function () { var o = { v: 1, f: function () { return (() => this.v).bind({ v: 2 })(); } }; return o.f(); })(), 1);
t("this survives asynchronous style callbacks", (function () { var o = { n: 0, run: function () { [1, 2, 3].forEach(() => { this.n++; }); return this.n; } }; return o.run(); })(), 3);
t("nested arrows share this", (function () { var o = { v: 9, f: function () { return (() => () => this.v)()(); } }; return o.f(); })(), 9);
t("arrow created in a constructor", (function () { function C() { this.v = 4; this.get = () => this.v; } var c = new C(); var g = c.get; return g(); })(), 4);
t("arrow in a strict function", (function () { "use strict"; return (() => this)(); })(), undefined);
t("arrow at top level sees the global this", (() => this === globalThis)(), true);
t("arrow in an eval keeps the lexical this", (function () { var o = { v: 3, f: function () { return eval("(() => this.v)()"); } }; return o.f(); })(), 3);
t("arrow in a getter", (function () { var o = { v: 2, get g() { return (() => this.v)(); } }; return o.g; })(), 2);
t("arrow in a default parameter", (function () { var o = { v: 6, f(a = () => this.v) { return a(); } }; return o.f(); })(), 6);

// ---- arguments and new.target
t("arguments is lexical", (function () { return (() => arguments[0])(); })(5), 5);
t("arguments length is lexical", (function () { return (() => arguments.length)(1, 2, 3); })(1, 2), 2);
t("nested arrow arguments", (function () { return (() => () => arguments[1])()(); })(1, 2), 2);
t("arrow has its own parameters not arguments", (function () { return ((a) => a)(9); })(1), 9);
t("arrow rest does not touch arguments", (function () { return ((...r) => r.length + arguments.length)(1, 2); })(7), 3);
t("new.target is lexical", (function () { function F() { return (() => new.target === F)(); } return new F() instanceof F; })(), true);
t("new.target of a plain call in an arrow", (function () { function F() { return (() => typeof new.target)(); } return F(); })(), "undefined");
t("arguments at the top level is undeclared", (() => typeof arguments)(), "undefined");

// ---- not constructors
terr("new on an arrow throws", function () { new (() => 1)(); }, "TypeError");
terr("new on an arrow variable throws", function () { var f = () => 1; new f(); }, "TypeError");
t("arrow has no prototype property", (() => 1).hasOwnProperty("prototype"), false);
t("arrow is not a constructor for instanceof", (function () { var f = () => 1; return typeof f.prototype; })(), "undefined");
t("arrow is callable", typeof (() => 1), "function");
t("arrow length", ((a, b) => 1).length, 2);
t("arrow name from variable", (function () { var f = () => 1; return f.name; })(), "f");
t("arrow has no own arguments or caller", Object.getOwnPropertyNames(() => 1).join(), "length,name");
t("arrow inherits Function.prototype", Object.getPrototypeOf(() => 1) === Function.prototype, true);
t("arrow toString shows its source", (a => a).toString(), "a => a");
t("arrow expression body with strings", (() => "a" + "b")(), "ab");

// ---- super in arrows inside methods
t("super property inside an arrow", (function () {
    var base = { hello() { return "base"; } };
    var o = { __proto__: base, hello() { return (() => super.hello())(); } };
    return o.hello();
})(), "base");
t("super in an arrow in a class", (function () {
    class A { who() { return "A"; } }
    class B extends A { who() { return (() => super.who() + "B")(); } }
    return new B().who();
})(), "AB");
t("super call in an arrow in a constructor", (function () {
    class A { constructor() { this.a = 1; } }
    class B extends A { constructor() { var f = () => super(); f(); this.b = 2; } }
    var b = new B();
    return b.a + b.b;
})(), 3);
t("this in an arrow before super is a reference error", (function () {
    class A { }
    class B extends A { constructor() { var f = () => this; try { f(); } catch (e) { var name = e.name; } super(); return; } }
    try { new B(); return "constructed"; } catch (e) { return e.name; }
})(), "constructed");

// ---- other behaviors
t("arrows are closures", (function () { var n = 0; var inc = () => ++n; inc(); inc(); return n; })(), 2);
t("recursive arrow through a variable", (function () { var fact = n => n <= 1 ? 1 : n * fact(n - 1); return fact(6); })(), 720);
t("arrow with generator-free iteration", (function () { var out = []; for (var x of [1, 2]) { out.push((() => x)()); } return out.join(); })(), "1,2");
t("arrow captures the loop binding of let", (function () { var f = []; for (let i = 0; i < 3; i++) f.push(() => i); return f[0]() + "" + f[2](); })(), "02");
t("arrow as a getter value", ({ get g() { return (() => 4)(); } }).g, 4);
t("arrow as a default", (function (f = () => "d") { return f(); })(), "d");
t("arrow returning this binding in class field style", (function () { function C() { this.v = 1; this.f = () => this.v; } return new C().f(); })(), 1);
t("chained arrow calls with arguments", (function () { var add = a => b => a + b; return add(1)(2) + add(3)(4); })(), 10);
t("arrow in an array literal", [() => 1, () => 2].map(f => f()).join(), "1,2");
t("arrow in an object literal", ({ f: () => "x" }).f(), "x");
t("arrow with a regular expression body free", (() => 1 / 2)(), 0.5);
