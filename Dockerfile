FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/clojureproject.jar /clojureproject/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/clojureproject/app.jar"]
