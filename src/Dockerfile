FROM eclipse-temurin:17-jdk-jammy AS client-build
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac client/*.java


FROM eclipse-temurin:17-jdk-jammy AS server-build
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac coordinator/*.java

