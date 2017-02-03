NAME = hermes-mock
VERSION = $(shell bash -c 'cat docker.version')

.PHONY: all build
all: build

build:
	docker build -t $(NAME):latest .

push:
	docker push $(NAME):latest

run_local:
	docker run -p 8090:8090 --name $(NAME) $(NAME):$(VERSION)
