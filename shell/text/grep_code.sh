#!/usr/bin/env bash

function grep_code {
    dir=.
    if [ $# -eq 0 ]; then
        echo "Usage: grep_code <keyword> [<dir>]"
        return
    elif [ $# -eq 2 ]; then
        dir="$2"
    fi

    grep -r $(printf -- '--include=*.%s ' {java,go,py,rs,php}) $1 $dir
}

if [ $# != 0 ]; then grep_code $@; fi

