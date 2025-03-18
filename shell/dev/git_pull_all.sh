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

RED='\033[0;31m'
NC='\033[0m' # No Color

# 查找所有的git仓库，并拉取最新代码(如果有未提交的变更则跳过)
function git_pull_all() {
    # -mindepth 1 -maxdepth 3
    find . -type d -name .git -maxdepth 3 | while read -r gitdir; do
        repo_dir=$(dirname "$gitdir")

        # 使用子shell处理每个仓库，避免影响主脚本的工作目录
        (
            cd "$repo_dir" || exit 1  # 进入仓库目录
            echo "Updating repository in $repo_dir"
            git pull --all
            # 检查是否有未提交的变更
            if [[ -n $(git status --porcelain) ]]; then
                echo -e "${RED}$repo_dir has uncommitted changes${NC}"
            fi

            ## 删除除 README.md 之外的所有文件和文件夹
            #find . -mindepth 1 -maxdepth 1 ! -name "README.md" -exec rm -rf {} \;
        )
    done
}

if [ $# != 0 ]; then git_pull_all $@; fi

}