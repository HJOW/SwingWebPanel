#!/bin/sh
# Maven으로 브라우저 앱을 빌드하고 JDK 6의 java -jar 로 실행한다. 뒤의 인수(시작 주소 등)는 앱에 전달한다.
# 앱은 Java 6 이상에서 동작한다. 다른 JRE로 실행하려면 빌드한 뒤 java -jar desktopapp/target/swingwebpanel-desktopapp.jar 를 직접 실행한다.
cd "$(dirname "$0")/.." || exit 1
mvn -B -pl desktopapp -am -Dmaven.test.skip=true package || exit 1
. ./scripts/jdk6.sh
exec "$JAVA" -jar desktopapp/target/swingwebpanel-desktopapp.jar "$@"
