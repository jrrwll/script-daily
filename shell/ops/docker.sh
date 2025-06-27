function docker_save_images() {
    docker image ls | tail -n +2 | while read line; do
        name=$(echo $line | awk '{print $1}')
        ver=$(echo $line | awk '{print $2}')
        img=$(echo $line | awk '{print $3}')
        output_name=$(echo $name | sed 's/docker.io\///' | sed 's/library\///' | sed 's/\//_/g')
        if [[ $ver = "latest" ]]; then ver=; else ver="-$ver"; fi
        docker save $img -o "$output_name$ver.tar" &
    done
}

function docker_find() {
    docker ps -a --format "{{.Names}}" | grep $1
}

function docker_untag() {
     if [ -z $DOCKER_MIRROR ]; then
         echo 'please define DOCKER_MIRROR first'
         return 1
     fi

     for image in $(docker images | grep $DOCKER_MIRROR | awk '{print $1 ":" $2}'); do
         real_image=$(echo $image | cut -d'/' -f2-)
         echo docker tag $image $real_image
         echo docker rmi $image
     done
}
