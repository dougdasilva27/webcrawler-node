#!/usr/bin/env bash

mvn clean package
docker-compose build webcrawler
docker-compose up webcrawler
