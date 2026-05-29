FROM nvidia/cuda:11.8.0-cudnn8-runtime-ubuntu22.04

# Cài đặt Python và các thư viện cần thiết
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8091

ENV WHISPER_MODEL=base
ENV WHISPER_DEVICE=cuda
ENV WHISPER_COMPUTE_TYPE=float16
ENV MINIO_ENDPOINT=http://localhost:9100
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=minioadmin
ENV MINIO_BUCKET=elearning-media
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV KAFKA_GROUP_ID=stt-service-group
ENV STT_REQUESTS_TOPIC=stt-requests-topic
ENV STT_RESULTS_TOPIC=stt-results-topic

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8091"]
