#!/bin/bash

(
  set -euxo pipefail

  cd $(dirname $0)/../..

  if [[ ! -f "jdbc-op/aot/build/native/nativeCompile/jdbc-op" ]]; then
      if [[ ! -d "jdbc-op/aot/resources/META-INF" ]]; then
        ./gradlew :jdbc-op-aot:test --info --stacktrace
      fi
      ./gradlew :jdbc-op-aot:nativeCompile
  fi

  function jdbc-op() {
      ./jdbc-op/aot/build/native/nativeCompile/jdbc-op "$@"
  }

  jdbc-op -h
  jdbc-op import-csv -h
  jdbc-op import-excel -h
  jdbc-op insert-random -h

  echo '\nstart to run real tests\n'
  jdbc-op import-csv -j "jdbc:sqlite:build/temp.sqlite" -c "build/test.csv"
)

