#!/usr/bin/env bash

function docker_build_jar() {
    # cd to project root dir
    PROJECT_DIRETORY=$1
    APPLICATION_NAME=$2
    PUSH=$3
    if [[ -z $PROJECT_DIRETORY || -z $APPLICATION_NAME ]]; then
        log_info "Usage: docker-build-jar <project_diretory> [:][repository_name/]image_name [--push]"
        log_info "\t              : \t gradle submodule"
        log_info "\t repository_name \t docker repository, specify --push to push it"
        log_info "\t     image_name \t image name, must same as gradle project name"
        return 1
    fi

    cd $PROJECT_DIRETORY ||(error "Cannot cd to $PROJECT_DIRETORY" && return 1)

    # -f, -d
    if [ -e "docker-tmp" ]; then
        error "The directory docker-tmp already exists, please change another diretory or move it"
        return 1
    else
        mkdir docker-tmp || (error "Cannot mkdir docker-tmp" && return 1)
    fi

    # gradle submodule
    if [ ":" = "${APPLICATION_NAME:0:1}" ]; then
        if [ "/" = "`echo $APPLICATION_NAME | grep -o /`" ]; then
            REPOSITORY_NAME=`echo $APPLICATION_NAME | cut -d "/" -f 1`
            REPOSITORY_NAME=${REPOSITORY_NAME:1:${#REPOSITORY_NAME}}
            IMAGE_NAME=`echo $APPLICATION_NAME | cut -d "/" -f 2`
        else
            IMAGE_NAME=${APPLICATION_NAME:1:${#APPLICATION_NAME}}
        fi
        SUBMODULE_NAME=:$IMAGE_NAME
        VERSION=`../gradlew $SUBMODULE_NAME:printVersion -q`
        log_info "Building jar package: $IMAGE_NAME"
        ../gradlew $SUBMODULE_NAME:clean && ../gradlew $SUBMODULE_NAME:build -x test
    else
        if [ "/" = "`echo $APPLICATION_NAME | grep -o /`" ]; then
            REPOSITORY_NAME=`echo $APPLICATION_NAME | cut -d "/" -f 1`
            IMAGE_NAME=`echo $APPLICATION_NAME | cut -d "/" -f 2`
        else
            IMAGE_NAME=$APPLICATION_NAME
        fi
        VERSION=`./gradlew printVersion -q`
        log_info "Building jar package: $IMAGE_NAME"
        ./gradlew clean && ./gradlew build -x test
    fi

    if [ -z $VERSION ]; then
            error "Please specify the task 'printProjectVersion' in build.gradle"
            rm -rf docker-tmp
            return 1
    fi

    JAR_NAME=${IMAGE_NAME}-${VERSION}.jar
    if [ -z $REPOSITORY_NAME ]; then
        DOCKER_NAME=${IMAGE_NAME}:v${VERSION}
    else
        DOCKER_NAME=$REPOSITORY_NAME/${IMAGE_NAME}:v${VERSION}
    fi

    echo "********************************************************"
    echo "Build ${DOCKER_NAME} docker image"
    echo "********************************************************"
    echo "Building..."
    cp build/libs/${JAR_NAME} docker-tmp/${IMAGE_NAME}.jar

    OLD_DOCKER_HASH=$(docker images -q -f reference=${DOCKER_NAME})

    cp ./docker_build_jar.Dockerfile ./docker-tmp/Dockerfile
    docker build -f ./docker-tmp/Dockerfile -t ${DOCKER_NAME} ./docker-tmp
    echo "Cleaning ..."
    if [ -e "docker-tmp" ]; then
        rm -rf docker-tmp
    fi

    if [ -z $OLD_DOCKER_HASH ]; then
        echo "No image cache, skip it"
    else
        echo "Cleaning the old image $OLD_DOCKER_HASH ..."
        docker rmi ${OLD_DOCKER_HASH}
    fi

    if [ -z $REPOSITORY_NAME ] || [ "--push" != "$PUSH" ]; then
        echo "Skip to push image $DOCKER_NAME"
        echo "Done"
        return 0
    fi

    echo "Uploading docker image: $DOCKER_NAME"
    docker push ${DOCKER_NAME}
    echo "Done"
}

if [ $# != 0 ]; then docker_build_jar $@; fi
