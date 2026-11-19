FROM python:3.12-slim
WORKDIR /app
COPY scripts/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY scripts/seed_demo.py scripts/seed_demo.py
COPY test-data/eod_positions.csv test-data/eod_positions.csv
COPY test-data/intraday_trades.jsonl test-data/intraday_trades.jsonl
COPY test-data/test_trades.csv test-data/test_trades.csv
CMD ["python", "scripts/seed_demo.py"]
