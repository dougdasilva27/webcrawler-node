#!/usr/bin/env bash
set -e

$(aws ecr get-login --no-include-email)

export ENVIROMENT_DEPLOY=$1
export ECR_IMAGE_URL=868884350453.dkr.ecr.us-east-1.amazonaws.com/team-data-capture/webcrawler:$1


if [ "$ENVIROMENT_DEPLOY" == "prod-high" ]; then "this.containerDefinitions[0].memoryReservation"="1500"
fi

npx json -I -f Dockerrun.aws.json -e "this.containerDefinitions[0].image"="'$ECR_IMAGE_URL'"

docker-compose -f build.yml run --rm maven
docker-compose -f build.yml build webcrawler

docker push $ECR_IMAGE_URL
docker rmi $(docker image ls -q $ECR_IMAGE_URL)
