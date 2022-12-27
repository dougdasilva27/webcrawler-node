#!/usr/bin/env bash
set -e

$(aws ecr get-login --no-include-email)

ECR_IMAGE_URL=868884350453.dkr.ecr.us-east-1.amazonaws.com/team-data-capture/webcrawler:dev

echo "Run Maven"

docker-compose -f build.yml run --rm maven

echo "building docker image"

docker build -f Dockerfile --no-cache --force-rm -t $ECR_IMAGE_URL .

#docker-compose -f build.yml build webcrawler

echo "pushing docker image to $ECR_IMAGE_URL"
docker push $ECR_IMAGE_URL

echo "cleaning up $ECR_IMAGE_URL older versions..."
docker rmi $(docker image ls -q $ECR_IMAGE_URL)
