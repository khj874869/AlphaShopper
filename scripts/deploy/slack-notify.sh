#!/usr/bin/env bash
set -euo pipefail

: "${SLACK_WEBHOOK_URL:?SLACK_WEBHOOK_URL is required}"
: "${SLACK_TITLE:?SLACK_TITLE is required}"
: "${SLACK_STATUS:?SLACK_STATUS is required}"

slack_details="${SLACK_DETAILS:-}"
slack_link="${SLACK_LINK:-}"

python3 - <<'PY' | curl -fsSL -X POST -H 'Content-type: application/json' --data @- "$SLACK_WEBHOOK_URL"
import json
import os

title = os.environ["SLACK_TITLE"]
status = os.environ["SLACK_STATUS"].lower()
details = os.environ.get("SLACK_DETAILS", "").strip()
link = os.environ.get("SLACK_LINK", "").strip()

color_map = {
    "success": "#2eb886",
    "failed": "#e01e5a",
    "warning": "#ecb22e",
    "info": "#36c5f0",
}

emoji_map = {
    "success": ":white_check_mark:",
    "failed": ":x:",
    "warning": ":warning:",
    "info": ":information_source:",
}

color = color_map.get(status, "#36c5f0")
emoji = emoji_map.get(status, ":information_source:")

text_lines = [f"{emoji} *{title}*"]
if details:
    text_lines.append(details)
if link:
    text_lines.append(f"<{link}|Open workflow run>")

payload = {
    "attachments": [
        {
            "color": color,
            "mrkdwn_in": ["text"],
            "text": "\n".join(text_lines),
        }
    ]
}

print(json.dumps(payload))
PY
