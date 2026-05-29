FROM nvidia/cuda:12.1.1-cudnn8-runtime-ubuntu22.04

# Cài đặt Python, pip và các thư viện hệ thống cần thiết cho OpenCV/PaddleOCR
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    libgl1-mesa-glx \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8090

# Biến môi trường tùy chỉnh cho PaddleOCR/CUDA
ENV PADDLE_WITH_CUDA=ON

# MinIO Configuration
ENV MINIO_ENDPOINT=http://localhost:9100
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=minioadmin
ENV MINIO_BUCKET=elearning-media

# Kafka Configuration
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV KAFKA_GROUP_ID=pdf-parser-service-group
ENV PDF_PARSER_REQUESTS_TOPIC=pdf-parser-requests-topic
ENV PDF_PARSER_RESULTS_TOPIC=pdf-parser-results-topic

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8090"]
