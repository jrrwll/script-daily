#!/usr/bin/env bash

function mvn_dep_to_gradle() {
    mvn dependency:resolve -o -nsu | grep -E '^\[INFO\] [\\+]-' | grep jar | grep compile | awk '{print $2}' | while read i; do
        group=`echo $i | cut -d':' -f1`
        name=`echo $i | cut -d':' -f2`
        version=`echo $i | cut -d':' -f4`
        echo "implementation '$group:$name:$version'"
    done
}

function show_mvn_dep_jar() {
    mvn dependency:resolve -o -nsu | grep -E '^\[INFO\] [\\+]-' | grep jar | grep compile | awk '{print $2}' | while read i; do
        group=`echo $i | cut -d':' -f1`
        name=`echo $i | cut -d':' -f2`
        version=`echo $i | cut -d':' -f4`
        groupDir=`echo $group | tr '.' '/'`
        echo "$HOME/.m2/repository/$groupDir/$name/$version/$name-$version.jar"
    done
}
