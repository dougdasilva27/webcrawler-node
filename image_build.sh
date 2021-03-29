#!/bin/bash

docker-compose -f build.yml run --rm maven
docker-compose -f build.yml build webcrawler
