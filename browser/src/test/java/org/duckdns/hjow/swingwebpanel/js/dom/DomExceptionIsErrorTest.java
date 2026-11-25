package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.host.structured.StructuredClone;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Verifies the DOMException ErrorData slot and its identity in host cloning.
 * <p>
 * DOMException의 ErrorData 슬롯과 호스트 복제 후의 식별을 검증한다.
 */
public class DomExceptionIsErrorTest {

    /**
     * Checks explicitly constructed and DOM-thrown exceptions.
     * <p>
     * 생성자로 만든 예외와 DOM이 던진 예외를 검사한다.
     */
    public void testConstructedAndThrownDomException() {
        DomTestEnv env = new DomTestEnv("<!doctype html><html><body></body></html>");
        String script = "const e=new DOMException('reason','NotFoundError'),p=Object.create(DOMException.prototype);"
                + "let thrown;try{document.createElement('');}catch(err){thrown=err;}"
                + "const tag=Object.prototype.toString.call(e);"
                + "const keep=Error.isError(e);Object.setPrototypeOf(e,null);"
                + "[keep,Error.isError(e),Error.isError(thrown),thrown.name,Error.isError(p),"
                + "Error.isError(new Proxy(thrown,{})),tag,Object.prototype.toString.call(e)].join('|')";
        Check.equal("true|true|true|InvalidCharacterError|false|false|[object DOMException]|[object Error]", env.eval(script),
                "constructor and DOM exceptions have ErrorData irrespective of prototype and proxy wrappers");
    }

    /**
     * Checks cross-realm clone reconstruction with the receiver's DOMException prototype.
     * <p>
     * 다른 realm에 복제할 때 수신자의 DOMException 프로토타입으로 복원하는지 검사한다.
     */
    public void testCloningPreservesDomExceptionBrand() {
        DomTestEnv source = new DomTestEnv("<body>");
        DomTestEnv target = new DomTestEnv("<body>");
        JsValue exception = source.realm.evaluateScript("const e=new DOMException('missing','NotFoundError');"
                + "Object.setPrototypeOf(e,null);e.name='shadow';e;", "dom-exception-clone.js");
        target.realm.getGlobalObject().defineBuiltin("external", exception);
        target.realm.getGlobalObject().defineBuiltin("cloned", StructuredClone.copy(source.realm, target.realm, exception));
        Check.equal("true|true|true|true|NotFoundError|missing|8",
                target.eval("[Error.isError(external),Error.isError(cloned),cloned!==external,"
                        + "Object.getPrototypeOf(cloned)===DOMException.prototype,cloned.name,cloned.message,cloned.code].join('|')"),
                "structured clone keeps the serialized Web IDL values and receiver brand");
    }
}
