# SVG 기준 자료 생성 방법 (문서용, 빌드와 시험이 실행하지 않는다)

`../cases/*.svg` 는 `gen_cases.py`, `gen_cases2.py`, `gen_cases3.py` 가 만든다(`python gen_cases.py <출력 디렉터리>`).
`../refs/*.png` 는 Chrome 156 (Claude 데스크톱 앱의 내장 브라우저, 2026-11-16 실행)이 같은 SVG 를 `img` 요소로 읽어 캔버스에 그린 픽셀이다.

1. `refserver.py <포트>` 는 `www/` 를 서빙하고 POST 로 받은 캔버스 픽셀을 `out/` 에 저장하는 작은 서버이다.
   `www/svg/` 에 사례 SVG 를, `www/list.txt` 에 사례 파일 이름(한 줄에 하나)을 둔다. `ref.html` 도 `www/` 에 둔다.
2. 브라우저로 `http://127.0.0.1:<포트>/ref.html` 을 열면 `ref.html` 이 사례마다 `Image` 로 읽고 자연 크기의 캔버스에 그린 뒤
   `getImageData` 의 RGBA 바이트(앞에 너비/높이 u32 리틀 엔디언 두 개)를 `out/<이름>.svg.rgba` 로 보낸다. 마지막에 `_info.json`(자연 크기)을 보낸다.
3. `mkrefs.py <out> <내 렌더링 PNG 디렉터리> <cases> <refs> <index.txt>` 가 `out/*.rgba` 를 PNG 로 바꿔 `refs/` 에 쓰고
   이 프로젝트의 렌더링과의 오차(평균 오차, 40 넘게 다른 픽셀의 비율)에 여유를 둔 제한값으로 `index.txt` 를 만든다.
   `compare.py` 는 차이를 눈으로 볼 수 있는 이미지(Chrome | 이 프로젝트 | 차이)를 만든다.

주의: Chrome 의 언어와 색 구성표(prefers-color-scheme)는 실행 환경에 따라 다르므로 이에 의존하는 사례(`systemLanguage="en"`, 어두운 테마 미디어 질의)는 기준에서 뺐고
`SvgDecoderTest` 가 직접 시험한다. Chrome 은 SVG 안의 CSS 애니메이션도 실행하므로 애니메이션을 쓰는 사례도 기준에 넣지 않았다.
