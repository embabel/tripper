# Tripper: Embabel Travel Planner Agent

![Build](https://github.com/embabel/embabel-agent/actions/workflows/maven.yml/badge.svg)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![JSON](https://img.shields.io/badge/JSON-000?logo=json&logoColor=fff)
![htmx](https://img.shields.io/badge/htmx-3366CC.svg?style=for-the-badge&logo=htmx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJIDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)

<img align="left" src="https://github.com/embabel/embabel-agent/blob/main/embabel-agent-api/images/315px-Meister_der_Weltenchronik_001.jpg?raw=true" width="180">

&nbsp;&nbsp;&nbsp;&nbsp;

&nbsp;&nbsp;&nbsp;&nbsp;

# Embabel Travel Planner Agent

Set your `BRAVE_API_KEY` in the Embabel server's environment for image search.

Start up background services, local model runners, and MCP gateway by running:

```bash
docker compose --file compose.yaml --file compose.dmr.yaml up
```

Run under your IDE (e.g., an IntelliJ Spring Boot Run Configuration) or run the shell script to start Embabel:

```bash
./run.sh
```

The travel planner will be available at `http://localhost:8080/travel/journey`.
Go to `http://localhost:8080` for a directory.

## Architecture

https://github.com/neo4j/neo4j-ogm-spring





