# Use Python 3.13 slim image for smaller size
FROM python:3.13-slim

# Set working directory
WORKDIR /app

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Pass through OpenAI API key from host environment
ARG OPENAI_API_KEY
ENV OPENAI_API_KEY=$OPENAI_API_KEY

# Install system dependencies if needed
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
RUN pip install --upgrade pip
RUN pip install neo4j-graphrag
RUN pip install neo4j-graphrag[openai]

# Copy the Python script and config file
COPY build_graph.py .
COPY simple_kg_pipeline_config.yml .

# Create a data directory for mounted files
RUN mkdir -p /app/data

# Create a non-root user for security
RUN useradd --create-home --shell /bin/bash app && \
    chown -R app:app /app
USER app

# Set up entrypoint to run the script with arguments
ENTRYPOINT ["python", "build_graph.py"]