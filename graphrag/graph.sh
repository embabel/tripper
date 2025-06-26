
docker build -t neo4j-graphrag .


docker run -e OPENAI_API_KEY=$OPENAI_API_KEY -e NEO4J_URI="bolt://host.docker.internal:7687" -v ./test.txt:/app/data/input.txt neo4j-graphrag /app/data/input.txt
