(function () {
    var frame = window.requestAnimationFrame;
    var fetchPage = window.fetch;
    var frames = 0;
    var frameTime = 0;
    var slowestFrame = 0;
    var completed = 0;
    var assetBytes = 0;
    var pending = 0;
    var firstAsset = -1;
    var lastAsset = -1;
    var since = performance.now();

    window.requestAnimationFrame = function (callback) {
        return frame.call(window, function (stamp) {
            var started = performance.now();
            try {
                return callback.call(window, stamp);
            } finally {
                var elapsed = performance.now() - started;
                frameTime += elapsed;
                slowestFrame = Math.max(slowestFrame, elapsed);
                frames++;
            }
        });
    };

    window.fetch = function () {
        if (firstAsset < 0) firstAsset = performance.now();
        pending++;
        return fetchPage.apply(window, arguments).then(function (response) {
            pending--;
            completed++;
            lastAsset = performance.now();
            var getReader = response.body && response.body.getReader;
            if (getReader) {
                response.body.getReader = function () {
                    var reader = getReader.apply(this, arguments);
                    var read = reader.read;
                    reader.read = function () {
                        return read.apply(this, arguments).then(function (result) {
                            if (!result.done && result.value) assetBytes += result.value.byteLength;
                            lastAsset = performance.now();
                            return result;
                        });
                    };
                    return reader;
                };
            }
            var arrayBuffer = response.arrayBuffer;
            response.arrayBuffer = function () {
                return arrayBuffer.apply(this, arguments).then(function (bytes) {
                    assetBytes += bytes.byteLength;
                    lastAsset = performance.now();
                    return bytes;
                });
            };
            return response;
        }, function (error) {
            pending--;
            throw error;
        });
    };

    window.setInterval(function () {
        var app = window.netstorm;
        var p = app && app._loadProgress;
        var now = performance.now();
        opener.postMessage([
            "sample", app ? app.state : "boot", p ? p.loaded : 0, p ? p.total : 0,
            Math.round(now), frames, Math.round(frameTime * 100) / 100,
            Math.round(slowestFrame * 100) / 100, Math.round(now - since),
            completed, pending, assetBytes, Math.round(lastAsset < 0 ? 0 : lastAsset - firstAsset),
            app ? app._audioStatus : "", app ? app._musicStatus : ""
        ].join("|"), "*");
        since = now;
        frames = frameTime = slowestFrame = 0;
    }, 1000);
})();
