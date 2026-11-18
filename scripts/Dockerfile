FROM python:3.12-slim
WORKDIR /app
COPY scripts/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY scripts/seed_demo.py scripts/seed_demo.py
COPY sample-data/eod_positions.csv sample-data/eod_positions.csv
COPY sample-data/intraday_trades.jsonl sample-data/intraday_trades.jsonl
CMD ["python", "scripts/seed_demo.py"]
