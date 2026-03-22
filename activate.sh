#!/usr/bin/env bash

SCRIPT_HOME=$(cd "$(dirname $0)" && pwd -P)
SCRIPT_SHELL_HOME=$SCRIPT_HOME/shell

# java
exec_cmd="bash $SCRIPT_SHELL_HOME/dev/run_java_module.sh"
alias es-op="$exec_cmd es-op \$@"
alias mongo-op="$exec_cmd mongo-op \$@"
unset exec_cmd

# shell
for i in common ops; do
    ls $SCRIPT_SHELL_HOME/$i | while read j; do
        [[ $i != \*.sh ]] && continue
        source "$SCRIPT_SHELL_HOME/$i/$j"
    done
done

while read i; do
   [[ $i = \#* ]] && continue
   source "$SCRIPT_SHELL_HOME/$i"
done <<EOF
dev/add_gradle_submodule.sh
dev/deploy_war_to_tomcat.sh
dev/init_gradle_java_project.sh
dev/upgrade_gradle.sh
ffmpeg/ffmpeg_merge_ts.sh
ops/docker.sh
ops/docker_build.sh
ops/docker_build_jar.sh
os/recurse_dir.sh
text/wc_code_line.sh
EOF
