# Local reference-rendering helper: serves ./www and saves POSTed canvas pixels to ./out
import http.server, os, sys, urllib.parse, json
ROOT = os.path.dirname(os.path.abspath(__file__))
WWW = os.path.join(ROOT, 'www'); OUT = os.path.join(ROOT, 'out')
os.makedirs(OUT, exist_ok=True)
class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k): super().__init__(*a, directory=WWW, **k)
    def do_POST(self):
        q = urllib.parse.urlparse(self.path)
        n = int(self.headers.get('Content-Length', 0)); body = self.rfile.read(n)
        name = urllib.parse.parse_qs(q.query).get('name', ['x'])[0]
        name = os.path.basename(name)
        with open(os.path.join(OUT, name), 'wb') as f: f.write(body)
        self.send_response(200); self.send_header('Access-Control-Allow-Origin', '*'); self.end_headers(); self.wfile.write(b'ok')
    def log_message(self, *a): pass
http.server.ThreadingHTTPServer(('127.0.0.1', int(sys.argv[1])), H).serve_forever()
