#!/usr/bin/env bash

./scripts/support/check_env.sh

export SPRING_PROFILES_ACTIVE=shell,starwars,neo
mvn -P agent-examples-kotlin -Dmaven.test.skip=true spring-boot:run
