#!/usr/bin/env bash

#ls | while read i; do (cd $i && [[ -f pom.xml ]] && mvn clean -o); done
#ls | while read i; do (cd $i && [[ -x gradlew ]] && ./gradlew clean --offline); done

find . -name ".DS_Store" | xargs rm -v
