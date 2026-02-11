# Use Python 3.9 for best TensorFlow compatibility
FROM python:3.9-slim

# Set working directory
WORKDIR /code

# Install system dependencies (curl for healthcheck, libgomp1 for XGBoost)
RUN apt-get update && apt-get install -y \
    curl \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first to leverage cache
COPY requirements.txt .

# Install dependencies (no-cache to save space)
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application
COPY . .

# Grant permissions
RUN chmod -R 777 /code

# Expose the correct port
EXPOSE 7860

# Run the application
CMD ["python", "api.py"]