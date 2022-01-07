#!/usr/bin/env bash
set -e

$(aws ecr get-login --no-include-email)

export ENVIROMENT_DEPLOY=$1
export ECR_IMAGE_URL=868884350453.dkr.ecr.us-east-1.amazonaws.com/team-data-capture/webcrawler:$1

#npx json -I -f docker-compose.dev.yml -e "this.services.webscraper."="'$ECR_IMAGE_URL'"
echo ECR_IMAGE_URL
grep image docker-compose.dev.yml | sed -i 's/""/$ECR_IMAGE_URL/' docker-compose.dev.yml

docker-compose -f build.yml run --rm maven
docker-compose -f build.yml build webcrawler

docker push $ECR_IMAGE_URL
docker rmi $(docker image ls -q $ECR_IMAGE_URL)
