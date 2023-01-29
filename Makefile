.PHONY: all test clean jar docker

all: test jar

test:
	lein spec

clean:
	rm -rf target docs

jar:
	lein jar

# release:
# 	lein release

# doc:
# 	lein codox

docker:
	docker build --progress tty -t clj-org .
