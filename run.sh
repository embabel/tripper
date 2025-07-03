#!/usr/bin/env bash

#./scripts/support/check_env.sh


echo "Running web profile: go to http://localhost:8080"

#export SPRING_PROFILES_ACTIVE=neo
mvn -Dmaven.test.skip=true spring-boot:run
