# Graph Builder

https://github.com/neo4j/neo4j-graphrag-python/blob/main/examples/build_graph/from_config_files/simple_kg_pipeline_from_config_file.py

docker build -t neo4j-graphrag .

docker run -e OPENAI_API_KEY=$OPENAI_API_KEY \
-v ./travel_config.yml:/app/data/config.yml \
-v ./rod.txt:/app/data/input.txt \
neo4j-graphrag /app/data/config.yml /app/data/input.txt
