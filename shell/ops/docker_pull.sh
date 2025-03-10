#!/usr/bin/env bash

# brew install yq jq

function docker_pull() {
    docker_mirror=$1
    if [ -z "$docker_mirror" ]; then
      log_error "docker_mirror is empty"
      return 1
    fi

    yq -o json ./docker-compose.yaml | \
    jq '.services | to_entries | map(select(.key)) | .[].value.image' | \
    grep -v null | cut -d'"' -f2 | while read i; do
        image_name=$i
        # like: docker.io/repo/image
        if [ $(echo $i | grep -E '^([^/]*/[^/]*/|/[^/]*/[^/]*)') ]; then
            image_name=`echo $i | sed "s|^[^/]*/|$docker_mirror/|"`
        # like: nginx:latest
        elif [ $(echo $i | grep -v '/') ]; then
            image_name="$docker_mirror/library/$image_name"
        # like repo/image
        else
            image_name="$docker_mirror/$image_name"
        fi
        echo "docker pull $image_name"
        docker pull $image_name
        echo "docker tag $image_name $i"
        docker tag $image_name $i
    done
}