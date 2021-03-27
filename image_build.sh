#!/bin/bash

docker-compose -f build.dev.yml run --rm maven
docker-compose -f build.dev.yml build webcrawler
