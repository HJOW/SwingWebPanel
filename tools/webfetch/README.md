# webfetch - 웹 페이지 조회 도구

규격 문서(W3C, ECMA)나 Chromium 소스 같은 웹 자료를 명령행에서 내려받는 개발 보조 도구입니다.
제품(SwingWebPanel)에 포함되지 않으며, 규격 URL과 판본, 조회 날짜를 문서에 기록할 때 사용합니다.

## 준비

JDK 6 이 필요합니다. 기본 경로는 `C:\Program Files\Java\jdk1.6.0_45` 이며,
다른 위치라면 환경 변수 `JAVA6_HOME` 을 지정하세요. JDK 6 을 찾지 못하면 오류로 종료합니다.

```
tools\webfetch\build.bat      (Windows)
tools/webfetch/build.sh       (sh)
```

## 사용법

```
tools\webfetch\run.bat [옵션] URL
```

주요 옵션: `--text`(태그 제거), `--out 파일`(파일로 저장), `--base64`(Gitiles `?format=TEXT` 해독),
`--header "이름: 값"`, `--timeout 밀리초`, `--max-bytes 바이트`, `--quiet`.
출처 정보(원본 URL, 조회 날짜, 상태, 사용한 JRE)는 표준 오류에 `# 이름: 값` 형태로 출력됩니다.

## 중요: HTTPS 는 새 JRE 로 실행하세요

JDK 6u45 의 JSSE 는 TLS 1.2 와 SNI 를 지원하지 않아 요즘 HTTPS 사이트에 접속하지 못합니다
(실제로 `handshake_failure` 가 발생함을 확인했습니다). 도구는 JDK 6 으로 컴파일하되,
실행할 때는 `WEBFETCH_JAVA_HOME` 으로 더 새 JRE 를 지정합니다.

```
set WEBFETCH_JAVA_HOME=C:\Program Files\Java\zuu25
tools\webfetch\run.bat --text https://www.w3.org/TR/2014/REC-html5-20141028/
```

## 종료 코드

| 코드 | 의미 |
| --- | --- |
| 0 | 성공 |
| 1 | 사용법 오류 |
| 2 | 네트워크/TLS 실패 |
| 3 | HTTP 오류 상태(400 이상) |
