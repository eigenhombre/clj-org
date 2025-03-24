.PHONY: all test clean jar docker

all: test jar

lint:
	lein do bikeshed, kibit, eastwood

test:
	lein test

clean:
	rm -rf target docs

jar:
	lein jar

# release:
# 	lein release

# doc:
# 	lein codox

docker:
	docker build --progress plain -t clj-org .
