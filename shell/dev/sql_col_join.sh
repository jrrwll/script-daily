#!/usr/bin/env bash

# brew install grep
# alias grep='ggrep'

# 提取ddl sql中所有的字段名

function sql_col_join() {
    cat - | awk 'tolower($0) ~ /create table /,/^\)/' | \
    sed '/create table /Id;/^)/d' | \
    grep -oP '`?[^`]+`?' | grep -viE '^(primary|unique|key)' | \
    awk '{print $1}' | paste -sd, -
}
