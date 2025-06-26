"""In this example, the pipeline is defined in a JSON ('simple_kg_pipeline_config.json')
or YAML ('simple_kg_pipeline_config.yaml') file.

According to the configuration file, some parameters will be read from the env vars
(Neo4j credentials and the OpenAI API key).
"""

import asyncio
import logging
import os
import sys
from neo4j_graphrag.experimental.pipeline.config.runner import PipelineRunner
from neo4j_graphrag.experimental.pipeline.pipeline import PipelineResult
from pathlib import Path

# If env vars are in a .env file, uncomment:
# (requires pip install python-dotenv)
# from dotenv import load_dotenv
# load_dotenv()

logging.basicConfig()
logging.getLogger("neo4j_graphrag").setLevel(logging.DEBUG)

# Use host.docker.internal to connect to host services from container
os.environ["NEO4J_URI"] = os.getenv("NEO4J_URI", "bolt://host.docker.internal:7687")
os.environ["NEO4J_USER"] = os.getenv("NEO4J_USER", "neo4j")
os.environ["NEO4J_PASSWORD"] = os.getenv("NEO4J_PASSWORD", "brahmsian")
# os.environ["OPENAI_API_KEY"] = "sk-..."

root_dir = Path(__file__).parent


async def main() -> PipelineResult:
    if len(sys.argv) < 3:
        print("Usage: python build_graph.py <config_file_path> <text_file_path>")
        print("Example: python build_graph.py /app/data/config.yml /app/data/input.txt")
        sys.exit(1)

    config_file_path = sys.argv[1]

    text_file_path = sys.argv[2]

    try:
        with open(text_file_path, 'r', encoding='utf-8') as f:
            text_content = f.read()

        print(f"Processing text from: {text_file_path}")
        print(f"Text length: {len(text_content)} characters")

        pipeline = PipelineRunner.from_config_file(config_file_path)
        return await pipeline.run({"text": text_content})

    except FileNotFoundError:
        print(f"Error: File '{text_file_path}' not found")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading file: {e}")
        sys.exit(1)


if __name__ == "__main__":
    result = asyncio.run(main())
    print("Pipeline completed successfully!")
    print(result)
