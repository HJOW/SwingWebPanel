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
     * Tests toArray, reduce, and forEach with different accumulator states and callback arguments.
     * <p>
     * 누산값과 콜백 인수의 차이를 포함해 toArray, reduce, forEach를 검증한다.
     */
    public void testEagerIteratorConsumption() {
        String script = "function error(f){try{f();return 'missing';}catch(e){return e.name;}}"
                + "const log=[];const array=Iterator.from('ab').toArray();"
                + "const sum=Iterator.from([2,3,6]).reduce(function(a,v,i){'use strict';log.push(i,this===undefined,arguments.length);return a+v;});"
                + "const initial=Iterator.from([7]).reduce(function(a,v,i){return [String(a),v,i].join(',');},undefined);"
                + "const done=Iterator.from([4,6]).forEach((v,i)=>log.push(v+i));"
                + "[array instanceof Array,array.join(),sum,initial,done===undefined,log.join(),"
                + "Iterator.from([]).reduce(()=>{throw 'called';},undefined)===undefined,"
                + "error(()=>Iterator.from([]).reduce(()=>0)),Iterator.prototype.toArray.length,"
                + "Iterator.prototype.reduce.length,Iterator.prototype.forEach.length].join('|')";
        Check.equal("true|a,b|11|undefined,7,0|true|1,true,3,2,true,3,4,7|true|TypeError|0|1|1", evaluate(JsRealm.create(), script),
                "eager methods consume the receiver and preserve callback argument order");
    }

    /**
     * Predicate methods close a generator on a short circuit, including when find matches undefined.
     * <p>
     * 조건 검사 메서드는 단락할 때 제너레이터를 닫으며 find에서 undefined가 일치하는 경우에도 닫는다.
     */
    public void testEagerPredicateShortCircuit() {
        String script = "var closed='';function* seq(tag){try{yield undefined;yield 3;yield 8;}finally{closed+=tag;}}"
                + "const first=seq('S').some((v,i)=>i===1);const every=seq('E').every((v,i)=>i<1);"
                + "const found=seq('F').find((v,i)=>i===0);const last=seq('L').find(v=>v===8);"
                + "[first,every,found===undefined,last,closed,Iterator.from([]).some(()=>true),"
                + "Iterator.from([]).every(()=>false),Iterator.from([]).find(()=>true)===undefined].join('|')";
        Check.equal("true|false|true|8|SEFL|false|true|true", evaluate(JsRealm.create(), script),
                "early exit closes sources and empty iterators return per-method identities");
    }

    /**
     * Validates the order and precedence of iterator getter, callback and return errors.
     * <p>
     * 이터레이터 getter, 콜백 및 return 오류의 순서와 우선순위를 검증한다.
     */
    public void testEagerIteratorAbruptClose() {
        String script = "let log='';function fail(f){try{f();return 'missing';}catch(e){return e.name||e;}}"
                + "const p=Iterator.prototype;function input(mode){return {get next(){log+='n';if(mode===1)throw 'next getter';"
                + "return ()=>({done:false,get value(){if(mode===2)throw 'step';return 1;}});},"
                + "return(){log+='r';if(mode===3)throw 'return';return {};}};}"
                + "const invalid=fail(()=>p.some.call(input(1),0));"
                + "const next=fail(()=>p.forEach.call(input(1),()=>0));"
                + "const step=fail(()=>p.every.call(input(2),()=>false));"
                + "const callback=fail(()=>p.some.call(input(3),()=>{throw 'callback';}));"
                + "const close=fail(()=>p.find.call(input(3),()=>true));"
                + "[invalid,next,step,callback,close,log,fail(()=>p.toArray.call('abc'))].join('|')";
        Check.equal("TypeError|next getter|step|callback|return|rnnnrnr|TypeError", evaluate(JsRealm.create(), script),
                "invalid callbacks close before next lookup and step errors are propagated directly");
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
