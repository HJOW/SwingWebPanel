// P13-08: classes, inheritance, super, accessors, static members and new.target.

// ---- basic classes
class Point {
    constructor(x, y) { this.x = x; this.y = y; }
    len2() { return this.x * this.x + this.y * this.y; }
    static origin() { return new Point(0, 0); }
    get sum() { return this.x + this.y; }
    set sum(v) { this.x = v; this.y = 0; }
    toString() { return "(" + this.x + "," + this.y + ")"; }
}
var pt = new Point(3, 4);
t("method", pt.len2(), 25);
t("static method", Point.origin().x, 0);
t("getter", pt.sum, 7);
t("setter", (function () { var p = new Point(1, 1); p.sum = 9; return p.x + "," + p.y; })(), "9,0");
t("toString through template", `${pt}`, "(3,4)");
t("typeof class", typeof Point, "function");
t("class prototype methods are shared", new Point(1, 1).len2 === new Point(2, 2).len2, true);
t("instance own properties", Object.keys(pt).join(), "x,y");
t("methods are not enumerable", Object.keys(Point.prototype).length, 0);
t("method attributes", (function () { var d = Object.getOwnPropertyDescriptor(Point.prototype, "len2"); return d.writable + "," + d.enumerable + "," + d.configurable; })(), "true,false,true");
t("accessor attributes", (function () { var d = Object.getOwnPropertyDescriptor(Point.prototype, "sum"); return d.enumerable + "," + d.configurable + "," + (typeof d.get) + (typeof d.set); })(), "false,true,functionfunction");
t("static method attributes", (function () { var d = Object.getOwnPropertyDescriptor(Point, "origin"); return d.writable + "," + d.enumerable + "," + d.configurable; })(), "true,false,true");
t("constructor property", Point.prototype.constructor === Point, true);
t("constructor property is not enumerable", Object.getOwnPropertyDescriptor(Point.prototype, "constructor").enumerable, false);
t("prototype property is not writable", Object.getOwnPropertyDescriptor(Point, "prototype").writable, false);
t("prototype property is not configurable", Object.getOwnPropertyDescriptor(Point, "prototype").configurable, false);
t("class length is the constructor length", Point.length, 2);
t("class name", Point.name, "Point");
t("class without constructor", (function () { class E { } return typeof new E(); })(), "object");
t("class without constructor has length 0", (class { }).length, 0);
t("instanceof", pt instanceof Point, true);
t("getPrototypeOf instance", Object.getPrototypeOf(pt) === Point.prototype, true);
t("class bodies are strict", (function () { class S { m() { return this; } } var m = new S().m; return m(); })(), undefined);
t("class constructor strict this", (function () { class S { constructor() { this.strict = (function () { return this; })() === undefined; } } return new S().strict; })(), true);
terr("class called without new", function () { Point(1, 2); }, "TypeError");
terr("class called with call", function () { Point.call({}, 1, 2); }, "TypeError");
t("class toString is the source", (class Q { m() { } }).toString(), "class Q { m() { } }");

// ---- declarations and expressions
terr("class declaration is in the TDZ before it", function () { new Later(); class Later { } }, "ReferenceError");
t("class declaration binding is let-like", (function () { class Z { } var before = Z; Z = 1; return typeof before + typeof Z; })(), "functionnumber");
t("class expression", (function () { var C = class { m() { return 1; } }; return new C().m(); })(), 1);
t("class expression name is inferred", (function () { var C = class { }; return C.name; })(), "C");
t("named class expression", (function () { var C = class Inner { who() { return Inner.name; } }; return new C().who(); })(), "Inner");
t("named class expression name is not visible outside", (function () { var C = class Inner { }; return typeof Inner; })(), "undefined");
terr("inner class name is constant", function () { var C = class Inner { static f() { Inner = 1; } }; C.f(); }, "TypeError");
t("class name inside is the class even if reassigned outside", (function () { class A { static self() { return A; } } var B = A; A = null; return B.self() === B; })(), true);
t("class expression as an argument", (function (K) { return new K().v; })(class { constructor() { this.v = 5; } }), 5);
t("class in an object literal", ({ K: class { } }).K.name, "K");
t("class declaration inside a block is block scoped", (function () { { class Blk { } } return typeof Blk; })(), "undefined");
t("anonymous class has an empty name", (class { }).name, "");

// ---- method forms
class Forms {
    static s() { return "s"; }
    static get sg() { return "sg"; }
    static set ss(v) { Forms.stored = v; }
    ["comp" + "uted"]() { return "computed"; }
    static ["sc"]() { return "sc"; }
    *gen() { yield 1; yield 2; }
    static *sgen() { yield "a"; }
    get() { return "get-method"; }
    set() { return "set-method"; }
    static() { return "static-method"; }
    async() { return "async-method"; }
    [Symbol.iterator]() { return [7, 8][Symbol.iterator](); }
    "quoted"() { return "quoted"; }
    42() { return "number"; }
}
var forms = new Forms();
t("static", Forms.s(), "s");
t("static getter", Forms.sg, "sg");
t("static setter", (function () { Forms.ss = 4; return Forms.stored; })(), 4);
t("computed method", forms.computed(), "computed");
t("computed static method", Forms.sc(), "sc");
t("generator method", drain(forms.gen()), "1,2");
t("static generator method", drain(Forms.sgen()), "a");
t("method named get", forms.get(), "get-method");
t("method named set", forms.set(), "set-method");
t("method named static", forms.static(), "static-method");
t("method named async", forms.async(), "async-method");
t("symbol iterator method", [...forms].join(), "7,8");
t("quoted method name", forms.quoted(), "quoted");
t("numeric method name", forms[42](), "number");
t("semicolons between members", (function () { class S { a() { return 1; }; b() { return 2; }; ; } return new S().a() + new S().b(); })(), 3);
t("getter and setter pair merge", (function () { class P { get v() { return this._v; } set v(x) { this._v = x; } } var p = new P(); p.v = 3; return p.v; })(), 3);
t("getter without setter ignores writes in sloppy callers", (function () { class P { get v() { return 1; } } var p = new P(); try { p.v = 2; } catch (e) { return "threw"; } return p.v; })(), 1);
t("class bodies are strict so a getter-only write throws inside", (function () { class P { get v() { return 1; } set2() { this.v = 2; } } try { new P().set2(); } catch (e) { return e.name; } })(), "TypeError");
t("static members are inherited", (function () { class A { static sm() { return "A"; } } class B extends A { } return B.sm(); })(), "A");
t("static getter this", (function () { class A { static get who() { return this.name; } } class B extends A { } return B.who; })(), "B");

// ---- inheritance
class Animal {
    constructor(name) { this.name = name; }
    speak() { return this.name + " makes a sound"; }
    get kind() { return "animal"; }
    static create(n) { return new this(n); }
}
class Dog extends Animal {
    constructor(name) { super(name); this.tricks = []; }
    speak() { return super.speak() + " (woof)"; }
    get kind() { return "dog/" + super.kind; }
}
var rex = Dog.create("rex");
t("subclass method with super", rex.speak(), "rex makes a sound (woof)");
t("subclass getter with super", rex.kind, "dog/animal");
t("static this in a subclass", rex instanceof Dog, true);
t("instance of base", rex instanceof Animal, true);
t("prototype chain", Object.getPrototypeOf(Dog.prototype) === Animal.prototype, true);
t("constructor chain", Object.getPrototypeOf(Dog) === Animal, true);
t("super call sets up this", rex.name + rex.tricks.length, "rex0");
t("default derived constructor forwards arguments", (function () { class A { constructor(a, b) { this.s = a + b; } } class B extends A { } return new B(1, 2).s; })(), 3);
t("default derived constructor has length 0", (class extends Object { }).length, 0);
t("extends null", (function () { class N extends null { } return Object.getPrototypeOf(N.prototype); })(), null);
t("extends null constructor prototype", (function () { class N extends null { } return Object.getPrototypeOf(N) === Function.prototype; })(), true);
terr("extends null cannot be constructed without a constructor", function () { class N extends null { } new N(); }, "TypeError");
t("extends an expression", (function () { function mix(B) { return class extends B { mixed() { return "m"; } }; } class A { } return new (mix(A))().mixed(); })(), "m");
t("extends a function constructor", (function () { function F(x) { this.x = x; } F.prototype.f = function () { return "F" + this.x; }; class C extends F { } return new C(1).f(); })(), "F1");
terr("extends a bound function has no prototype property", function () { function F() { } var B = F.bind(null); class C extends B { } }, "TypeError");
t("extends Object", (function () { class O extends Object { } return new O() instanceof Object; })(), true);
t("extends Array", (function () { class A extends Array { } var a = new A(); a.push(1); a.push(2); return a.length + "," + (a instanceof A) + "," + Array.isArray(a); })(), "2,true,true");
t("extends Array length behavior", (function () { class A extends Array { } var a = new A(); a[3] = 1; return a.length; })(), 4);
t("extends Error", (function () { class MyErr extends Error { constructor(m) { super(m); this.name = "MyErr"; } } var e = new MyErr("bad"); return e instanceof Error && e instanceof MyErr && e.message === "bad" && String(e) === "MyErr: bad"; })(), true);
t("extends Error keeps the message", (function () { class E2 extends Error { } return new E2("x").message; })(), "x");
t("extends Function", (function () { class F extends Function { } return new F("return 4")(); })(), 4);
t("extends Boolean/Number/String wrappers", (function () { class N extends Number { } class S extends String { } return (new N(5) + 1) + "," + new S("ab").length; })(), "6,2");
t("extends a generator-free iterator class", (function () { class It { [Symbol.iterator]() { return { i: 0, next() { return this.i < 2 ? { value: this.i++, done: false } : { done: true }; } }; } } class It2 extends It { } return [...new It2()].join(); })(), "0,1");
terr("extends a non constructor", function () { class C extends (() => 1) { } }, "TypeError");
terr("extends a non object", function () { class C extends 5 { } }, "TypeError");
terr("extends undefined", function () { var u; class C extends u { } }, "TypeError");
terr("prototype of the parent must be an object or null", function () { function F() { } F.prototype = 5; class C extends F { } }, "TypeError");
t("extends evaluates the heritage once", (function () { var n = 0; function p() { n++; return Object; } class C extends p() { } new C(); new C(); return n; })(), 1);
t("method override and super chain depth", (function () { class A { f() { return "A"; } } class B extends A { f() { return super.f() + "B"; } } class C extends B { f() { return super.f() + "C"; } } return new C().f(); })(), "ABC");
t("super property assignment writes to this", (function () { class A { } class B extends A { m() { super.x = 5; return this.x; } } return new B().m(); })(), 5);
t("super in static methods", (function () { class A { static f() { return "A"; } } class B extends A { static f() { return super.f() + "B"; } } return B.f(); })(), "AB");
t("super in getters and setters", (function () { class A { get v() { return 1; } set v(x) { this._v = x; } } class B extends A { get v() { return super.v + 1; } set v(x) { super.v = x * 2; } } var b = new B(); b.v = 4; return b.v + "," + b._v; })(), "2,8");
t("super in object literal methods in a class", (function () { class A { m() { return "A"; } } class B extends A { m() { var o = { m() { return "o"; } }; return super.m() + o.m(); } } return new B().m(); })(), "Ao");
t("super method keeps this", (function () { class A { who() { return this.id; } } class B extends A { constructor() { super(); this.id = "b"; } who() { return super.who(); } } return new B().who(); })(), "b");
t("super property lookup uses the home object not this", (function () { class A { m() { return "A"; } } class B extends A { m() { return super.m(); } } class C extends B { m() { return "C"; } } var c = new C(); return B.prototype.m.call(c); })(), "A");
t("super in a method moved to another object", (function () { class A { m() { return "A"; } } class B extends A { m() { return super.m(); } } var o = { m: B.prototype.m }; return o.m(); })(), "A");
t("method call on super with arguments", (function () { class A { add(a, b) { return a + b; } } class B extends A { add(a, b) { return super.add(a, b) * 2; } } return new B().add(1, 2); })(), 6);
t("super with spread", (function () { class A { constructor(a, b, c) { this.s = a + b + c; } } class B extends A { constructor(...args) { super(...args); } } return new B(...[1, 2, 3]).s; })(), 6);
t("super call result is this", (function () { class A { } class B extends A { constructor() { var r = super(); this.same = r === this; } } return new B().same; })(), true);

// ---- constructor semantics
terr("this before super throws", function () { class A { } class B extends A { constructor() { this.x = 1; super(); } } new B(); }, "ReferenceError");
terr("missing super call throws", function () { class A { } class B extends A { constructor() { } } new B(); }, "ReferenceError");
terr("super called twice throws", function () { class A { } class B extends A { constructor() { super(); super(); } } new B(); }, "ReferenceError");
t("super after other statements", (function () { class A { constructor() { this.a = 1; } } class B extends A { constructor() { var x = 5; super(); this.b = x; } } var b = new B(); return b.a + b.b; })(), 6);
t("derived constructor may return an object", (function () { class A { } class B extends A { constructor() { super(); return { custom: 1 }; } } return new B().custom; })(), 1);
terr("derived constructor returning a primitive throws", function () { class A { } class B extends A { constructor() { super(); return 1; } } new B(); }, "TypeError");
t("derived constructor returning undefined uses this", (function () { class A { } class B extends A { constructor() { super(); return undefined; } } return new B() instanceof B; })(), true);
t("base constructor may return an object", (function () { class A { constructor() { return { over: 1 }; } } return new A().over; })(), 1);
t("base constructor ignores primitive results", (function () { class A { constructor() { this.k = 1; return 5; } } return new A().k; })(), 1);
t("new.target in a base constructor", (function () { var seen; class A { constructor() { seen = new.target; } } new A(); return seen === A; })(), true);
t("new.target in a derived constructor chain", (function () { var seen; class A { constructor() { seen = new.target; } } class B extends A { } new B(); return seen === B; })(), true);
t("abstract pattern using new.target", (function () { class Shape { constructor() { if (new.target === Shape) { throw new TypeError("abstract"); } } } class Sq extends Shape { } try { new Shape(); } catch (e) { return (new Sq() instanceof Shape) + e.message; } })(), "trueabstract");
t("instance created with the new.target prototype", (function () { class A { } class B extends A { } return Object.getPrototypeOf(new B()) === B.prototype; })(), true);
t("Reflect-free subclass instance receives fields set by base", (function () { class A { constructor() { this.fromA = 1; } } class B extends A { constructor() { super(); this.fromB = 2; } } return Object.keys(new B()).join(); })(), "fromA,fromB");
t("calling a method before super is allowed via the prototype", (function () { class A { } class B extends A { constructor() { var m = B.prototype.helper; super(); this.v = m(); } helper() { return 3; } } return new B().v; })(), 3);
t("this after super in nested arrow", (function () { class A { } class B extends A { constructor() { super(); this.f = () => this; } } var b = new B(); return b.f() === b; })(), true);
t("constructor is not enumerable and not an own property of instances", (function () { class A { } return Object.keys(new A()).length + "," + new A().hasOwnProperty("constructor"); })(), "0,false");
t("accessing a class from its own static method", (function () { class A { static make() { return new A(); } } return A.make() instanceof A; })(), true);
t("instances do not share state", (function () { class C { constructor() { this.list = []; } } var a = new C(), b = new C(); a.list.push(1); return b.list.length; })(), 0);
t("class method can be detached and called with this", (function () { class A { constructor() { this.v = 4; } get() { return this.v; } } var a = new A(); return a.get.call({ v: 9 }); })(), 9);
t("symbol toStringTag via a getter", (function () { class T { get [Symbol.toStringTag]() { return "Tee"; } } return Object.prototype.toString.call(new T()); })(), "[object Tee]");
t("instanceof with Symbol.hasInstance static method", (function () { class Even { static [Symbol.hasInstance](n) { return n % 2 === 0; } } return (2 instanceof Even) + "," + (3 instanceof Even); })(), "true,false");
t("Object.prototype members available on instances", (function () { class A { } var a = new A(); return a.hasOwnProperty("x") + "," + a.toString(); })(), "false,[object Object]");
t("class with a method named constructor as a string key is the constructor", (function () { class A { "constructor"() { this.v = 1; } } return new A().v; })(), 1);
t("static method named prototype is an error at parse time", (function () { try { eval("class A { static prototype() { } }"); } catch (e) { return e.name; } })(), "SyntaxError");
t("duplicate constructors are a syntax error", (function () { try { eval("class A { constructor() { } constructor() { } }"); } catch (e) { return e.name; } })(), "SyntaxError");
t("generator constructors are a syntax error", (function () { try { eval("class A { *constructor() { } }"); } catch (e) { return e.name; } })(), "SyntaxError");
t("getter constructor is a syntax error", (function () { try { eval("class A { get constructor() { } }"); } catch (e) { return e.name; } })(), "SyntaxError");
t("super outside methods is a syntax error", (function () { try { eval("function f() { super.x; }"); } catch (e) { return e.name; } })(), "SyntaxError");
t("super call outside a derived constructor is a syntax error", (function () { try { eval("class A { constructor() { super(); } }"); } catch (e) { return e.name; } })(), "SyntaxError");
