#!/usr/bin/env bash

./scripts/support/check_env.sh

# Base profiles without shell
BASE_PROFILES="starwars,neo,docker-desktop,docker"

# Check if --shell flag is provided
if [[ "$*" == *"--shell"* ]]; then
    echo "Running with shell profile"
    export SPRING_PROFILES_ACTIVE="shell,$BASE_PROFILES"
else
    echo "Running web profile: go to http://localhost:8080"
    export SPRING_PROFILES_ACTIVE="$BASE_PROFILES"
fi

mvn -P agent-examples-kotlin -Dmaven.test.skip=true spring-boot:run