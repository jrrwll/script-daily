#!/usr/bin/env bash

function git_pull_all() {
    [[ -z "$1" ]] && (echo "usage: git_pull_all [dir] [-y]" && return 1)

    work_dir=$(cd "$1" && pwd -P)

    ls "$work_dir" | while read category; do
      category_dir=$work_dir/$category
      [[ ! -d "$category_dir" ]] && continue

      ls "$category_dir" | while read project; do
        current=$category_dir/$project
        if [[ -d "$current/.git" ]]; then
          if [[ $(git status | grep '^nothing to commit') ]]; then
            echo "performing: git pull --all --git-dir '$current'"
            [[ "$2" = "-y" ]] && (git pull --all --git-dir "$current")
          else
            echo "$current: has uncommitted changes"
          fi
        fi
      done
    done
}

if [ $# != 0 ]; then git_pull_all $@; fi
