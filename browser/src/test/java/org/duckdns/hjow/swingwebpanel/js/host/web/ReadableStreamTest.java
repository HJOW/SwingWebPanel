package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the readable stream bindings with actual browser script execution.
 * <p>
 * 실제 브라우저 스크립트 실행으로 읽기 스트림 바인딩을 검사한다.
 */
public final class ReadableStreamTest {
    /** Creates a suite for the runner. / 실행기에 제공할 시험 묶음을 만든다. */
    public ReadableStreamTest() {
    }

    /** Checks FIFO reads, queued data on close and brand checks. / FIFO 읽기, 닫힐 때 대기 데이터와 인터페이스 타입을 검사한다. */
    public void testReaderClosesAfterQueuedChunks() {
        WebFixture f = new WebFixture();
        f.js("var log=[], saved; var stream=new ReadableStream({start(c){saved=c;}});"
                + "var r=stream.getReader(); log.push(stream.locked, r instanceof ReadableStreamDefaultReader);"
                + "try{stream.getReader();}catch(e){log.push(e.name);}"
                + "r.read().then(item=>log.push(item.value+':'+item.done)); saved.enqueue('a'); saved.enqueue('b'); saved.close();"
                + "r.closed.then(()=>log.push('closed'));"
                + "r.read().then(item=>log.push(item.value+':'+item.done));"
                + "r.read().then(item=>{log.push(item.done,stream.locked);r.releaseLock();log.push(stream.locked);});");
        f.run();
        Check.equal("true,true,TypeError,a:false,closed,b:false,true,true,false", f.js("log.join(',')"), "read and close order");
    }

    /** Checks lock release with pending reads and asynchronous cancellation. / 대기 읽기가 있는 잠금 해제와 비동기 취소를 검사한다. */
    public void testReleaseAndCancel() {
        WebFixture f = new WebFixture();
        f.js("var log=[], saved, finish; var stream=new ReadableStream({start(c){saved=c;},cancel(reason){"
                + "log.push('cancel '+reason); return new Promise(resolve=>finish=resolve);}});"
                + "var first=stream.getReader();var original=first.closed;first.read().catch(e=>log.push('read '+e.name));"
                + "original.catch(e=>log.push('closed '+e.name));first.releaseLock();"
                + "var r=new ReadableStreamDefaultReader(stream);log.push(original===first.closed);"
                + "stream.cancel().catch(e=>log.push('lock '+e.name));r.read().then(item=>log.push('done '+item.done));");
        f.run();
        Check.equal("true,closed TypeError,read TypeError,lock TypeError", f.js("log.join(',')"), "old promises reject on release");
        f.js("r.cancel('stop').then(value=>log.push('settled '+value));");
        f.run();
        Check.equal("true,closed TypeError,read TypeError,lock TypeError,cancel stop,done true", f.js("log.join(',')"),
                "pending read closes while cancellation waits for the source");
        f.js("finish(7);");
        f.run();
        Check.equal("settled undefined", f.js("log.pop()"), "the result of cancelling is undefined");
    }

    /** Exercises start, pull, asynchronous iteration and Array.fromAsync. / start, pull, 비동기 반복과 Array.fromAsync를 시험한다. */
    public void testAsyncIterationAndFromAsync() {
        WebFixture f = new WebFixture();
        f.js("var log=[], next=0, start; var stream=new ReadableStream({"
                + "start(c){return new Promise(resolve=>start=resolve);},pull(c){c.enqueue(++next);if(next===3)c.close();}},"
                + "{highWaterMark:0}); var values=Array.fromAsync(stream,x=>x*2);"
                + "values.then(x=>log.push(x.join('|'),stream.locked));"
                + "var other=new ReadableStream({start(c){c.enqueue(5);c.enqueue(6);c.close();}});"
                + "(async()=>{let n=0;for await(const x of other)n+=x;log.push(n,other.locked);})();");
        f.run();
        Check.equal("0,11,false", f.js("[next].concat(log).join(',')"), "pull waits for start while another stream finishes");
        f.js("start();");
        f.run();
        Check.equal("11,false,2|4|6,false", f.js("log.join(',')"), "Array.fromAsync releases the lock after consuming the source");
    }

    /** Checks queued iterator return, preventCancel and errors during iteration. / 대기 중인 반복자 반환, preventCancel과 반복 오류를 검사한다. */
    public void testIteratorReturnAndFailure() {
        WebFixture f = new WebFixture();
        f.js("var log=[], saved; var stream=new ReadableStream({start(c){saved=c;},cancel(){log.push('cancel');}});"
                + "var it=stream.values({preventCancel:true});it.next().then(r=>log.push(r.value));"
                + "it.return('end').then(r=>log.push(r.value,stream.locked)); saved.enqueue('first'); saved.enqueue('second');");
        f.run();
        Check.equal("first,end,false", f.js("log.join(',')"), "return is queued behind next and unlocks without cancelling");
        f.js("var broken=stream.values();broken.next().then(r=>log.push(r.value));"
                + "broken.next().catch(e=>log.push(e,stream.locked));");
        f.run();
        Check.equal("false", f.js("saved.error('broken');String(stream.locked)"), "the read error unlocks before the iterator promise reacts");
        f.run();
        Check.equal("first,end,false,second,broken,false", f.js("log.join(',')"), "queued chunk and subsequent error are delivered in order");
        f.js("var last=new ReadableStream({start(c){c.enqueue(1);},cancel(reason){log.push('cancel:'+reason);}});"
                + "var lastIt=last[Symbol.asyncIterator]();lastIt.next();lastIt.return('early').then(r=>log.push(r.done,last.locked));");
        f.run();
        Check.equal("cancel:early,true,false", f.js("log.slice(-3).join(',')"), "early return normally cancels the stream");
    }
}
