FROM adoptopenjdk:11-jre-hotspot

RUN apt-get -qq -y update
RUN apt-get -qq -y upgrade

RUN apt-get install -qq -y leiningen make

WORKDIR /home/janice
COPY . /home/janice

RUN make test

