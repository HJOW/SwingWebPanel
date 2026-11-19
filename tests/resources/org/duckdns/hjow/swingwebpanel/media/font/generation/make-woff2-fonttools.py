# Generates WOFF2 test files of tests/src/org/duckdns/hjow/swingwebpanel/media/font/Woff2Test.java with an INDEPENDENT WOFF2 encoder: fontTools (fontTools.ttLib.woff2, the encoder
# that font build pipelines use), so that the decoder is not checked only with encoders written by the author of the decoder (plan item P10-08-e).
# Input: ../woff2/synthetic.ttf (the synthetic font that make-woff2-vectors.js makes; no real font). Output (../woff2/):
#   synthetic-fonttools-xf.woff2   glyf/loca and hmtx transformed
#   synthetic-fonttools-glyf.woff2 glyf/loca transformed, hmtx not
#   synthetic-fonttools-null.woff2 no table transformed
# Run once by hand (the tests and the product do not need Python):
#   python -m venv venv && venv/Scripts/python -m pip install fonttools==4.68.0 brotli==1.2.1
#   venv/Scripts/python -I tests/resources/org/duckdns/hjow/swingwebpanel/media/font/generation/make-woff2-fonttools.py
# Made on 2026-11-19 with Python 3.13.1, fontTools 4.68.0 and brotli 1.2.1 (Windows 10). fontTools reads the head table of the synthetic font, whose created and
# modified time stamps are arbitrary small numbers, as Unix times and writes them back converted ("timestamp seems very low"), so the head table of these files
# differs from the original also in those two fields; the test ignores them for these files. Everything else (the other tables byte for byte, every glyph) has to
# decode to the original font.
# 독립 WOFF2 인코더인 fontTools 로 Woff2Test 의 시험 파일을 만든다(디코더를 그 작성자가 쓴 인코더로만 확인하지 않도록, 계획 P10-08-e). 입력은 make-woff2-vectors.js 가
# 만드는 합성 글꼴(실제 글꼴 아님)이다. fontTools 는 합성 글꼴의 head 에 든 작은 임의 시각 값을 유닉스 시각으로 읽어 바꿔 쓰므로 이 파일들의 head 는 created/modified 도
# 원본과 다르며 시험은 이 파일에서 그 두 필드를 무시한다. 나머지(다른 테이블의 바이트, 모든 글리프)는 원본 글꼴로 풀려야 한다.
import os
from fontTools.ttLib import woff2

here = os.path.dirname(os.path.abspath(__file__))
out = os.path.join(here, "..", "woff2")
src = os.path.join(out, "synthetic.ttf")
woff2.compress(src, os.path.join(out, "synthetic-fonttools-xf.woff2"), transform_tables={"glyf", "loca", "hmtx"})
woff2.compress(src, os.path.join(out, "synthetic-fonttools-glyf.woff2"), transform_tables={"glyf", "loca"})
woff2.compress(src, os.path.join(out, "synthetic-fonttools-null.woff2"), transform_tables=set())
for name in ("synthetic-fonttools-xf.woff2", "synthetic-fonttools-glyf.woff2", "synthetic-fonttools-null.woff2"):
    print(name, os.path.getsize(os.path.join(out, name)), "bytes")
