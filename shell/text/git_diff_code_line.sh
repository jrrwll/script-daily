#!/usr/bin/env bash

# 统计两个分支间的变更行数

function git_diff_code_line() {
    base_branch_name=$1
    branch_name=$2
    if [ -z $base_branch_name ]; then
        base_branch_name="origin/master"
        branch_name="HEAD"
    elif [ -z $branch_name ]; then
        branch_name=$base_branch_name
        base_branch_name="origin/master"
    fi

    git diff $base_branch_name $branch_name --numstat | awk '{s+=$1 + $2} END {print s}'
}

if [ $# != 0 ]; then git_diff_code_line $@; fi
