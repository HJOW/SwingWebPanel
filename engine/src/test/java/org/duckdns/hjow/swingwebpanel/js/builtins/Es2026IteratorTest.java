package org.duckdns.hjow.swingwebpanel.js.builtins;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the global Iterator constructor, the existing prototype and iterator wrappers.
 * <p>
 * 전역 Iterator 생성자와 기존 프로토타입, 이터레이터 래퍼를 검증한다.
 */
public class Es2026IteratorTest {

    /**
     * Iterator is abstract and shares the base prototype used by built-in iterators.
     * <p>
     * Iterator는 추상 생성자이며 내장 이터레이터의 기반 프로토타입을 사용한다.
     */
    public void testIteratorConstructorAndExistingPrototypes() {
        String script = "function error(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "class Child extends Iterator { next(){ return {value:3,done:false}; } }"
                + "const a=[1].values(), g=(function*(){yield 4;})(), child=new Child();"
                + "const x=Reflect.construct(Iterator,[],function Other(){});"
                + "[Iterator.name,Iterator.length,Iterator.from.length,error(()=>Iterator()),error(()=>new Iterator()),"
                + "child instanceof Iterator,child.next().value,child[Symbol.iterator]()===child,"
                + "Object.getPrototypeOf(Object.getPrototypeOf(a))===Iterator.prototype,"
                + "a instanceof Iterator,g instanceof Iterator,Iterator.from(a)===a,Iterator.from(g)===g,"
                + "Object.getPrototypeOf(x)!==Iterator.prototype,Iterator.prototype.constructor===Iterator,"
                + "Object.prototype.toString.call(child)].join('|')";
        Check.equal("Iterator|0|1|TypeError|TypeError|true|3|true|true|true|true|true|true|true|true|[object Iterator]",
                evaluate(JsRealm.create(), script), "the standard constructor uses the existing iterator prototype");
    }

    /**
     * Iterator.from records next once, fetches return when used and forwards raw values with no extra arguments.
     * <p>
     * Iterator.from은 next를 한 번 읽고 return을 사용할 때 가져오며 추가 인수 없이 원래 반환값을 전달한다.
     */
    public void testFromForwardsRawNextAndReturn() {
        String script = "let log='';function error(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const source={get next(){log+='get';return function(){log+=this===source && arguments.length===0?'n':'!';return 7;};},"
                + "get return(){log+='ret';return function(){log+=this===source && arguments.length===0?'r':'!';return 'closed';};}};"
                + "const wrapped=Iterator.from(source); Object.defineProperty(source,'next',{value(){throw 'wrong';}});"
                + "const first=wrapped.next(9), stop=wrapped.return(10), again=wrapped.next();"
                + "const text=Iterator.from('ab'), complete=Iterator.from({next(){return {done:true};}}).return();"
                + "[wrapped!==source,wrapped instanceof Iterator,Iterator.from(wrapped)===wrapped,first,stop,again,log,"
                + "text.next().value,text.next().value,text.next().done,complete.done,complete.value===undefined,"
                + "error(()=>Iterator.from(4)),error(()=>Iterator.from({[Symbol.iterator](){return 4;}})),"
                + "error(()=>wrapped.next.call(source)),wrapped.next.length,wrapped.return.length].join('|')";
        Check.equal("true|true|true|7|closed|7|getnretrn|a|b|true|true|true|TypeError|TypeError|TypeError|0|0",
                evaluate(JsRealm.create(), script), "wrapper next and return delegate using the original iterator as receiver");
    }

    /**
     * The prototype accessors permit receiver-local overrides and reject writes directly to the iterator prototype.
     * <p>
     * 프로토타입 접근자는 수신 객체의 자체 프로퍼티 재정의를 허용하고 이터레이터 프로토타입의 직접 쓰기를 거부한다.
     */
    public void testIteratorPrototypeAccessors() {
        String script = "function error(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const p=Iterator.prototype, d=Object.getOwnPropertyDescriptor(p,'constructor'),"
                + "tag=Object.getOwnPropertyDescriptor(p,Symbol.toStringTag);"
                + "const x=Object.create(p);x.constructor=7;x[Symbol.toStringTag]='custom';"
                + "const own=Object.getOwnPropertyDescriptor(x,'constructor'); const sealed=Object.preventExtensions(Object.create(p));"
                + "const withSetter={set constructor(v){this.recorded=v;}};d.set.call(withSetter,9);"
                + "[d.get.name,d.get.length,d.set.name,d.set.length,d.enumerable,d.configurable,"
                + "tag.get.name,tag.set.name,tag.get.call(null),d.get.call(0)===Iterator,own.value,own.enumerable,"
                + "Object.prototype.toString.call(x),withSetter.recorded,error(()=>d.set.call(p,1)),"
                + "error(()=>tag.set.call(1,'x')),error(()=>d.set.call(sealed,1))].join('|')";
        Check.equal("get constructor|0|set constructor|1|false|true|get [Symbol.toStringTag]|set [Symbol.toStringTag]|"
                + "Iterator|true|7|true|[object custom]|9|TypeError|TypeError|TypeError", evaluate(JsRealm.create(), script),
                "prototype accessor attributes and receiver overrides");
    }

    /**
     * A foreign iterator gets a wrapper of the receiving realm and delegates iteration to the original object.
     * <p>
     * 다른 realm의 이터레이터는 받는 realm의 래퍼를 사용하며 순회는 원래 객체에서 진행한다.
     */
    public void testFromWrapsAnIteratorFromAnotherRealm() {
        JsRealm source = JsRealm.create();
        JsRealm receiving = JsRealm.create();
        receiving.getGlobalObject().defineBuiltin("foreign", source.evaluateScript("[5,6].values()", "foreign.js"));
        Check.equal("false|true|true|5|6", evaluate(receiving, "const w=Iterator.from(foreign);"
                + "[foreign instanceof Iterator,w instanceof Iterator,w!==foreign,w.next().value,foreign.next().value].join('|')"),
                "the wrapper has a local prototype and shares the iterator's state");
    }

    /**
     * Runs a script in the given realm and converts the result for assertions.
     * <p>
     * 주어진 realm에서 스크립트를 실행하고 검증에 쓸 문자열로 바꾼다.
     *
     * @param realm evaluation realm / 평가할 realm
     * @param script test script / 시험 스크립트
     * @return the result as text / 결과 문자열
     */
    static String evaluate(JsRealm realm, String script) {
        return Ops.toString(realm.evaluateScript(script, "iterator-test.js"));
    }
}
