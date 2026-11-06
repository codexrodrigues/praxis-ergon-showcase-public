"""Verify the immutable deployment SDK inputs without contacting a registry."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[2]
manifest = json.loads((root / "deployment-artifacts.lock.json").read_text(encoding="utf-8"))
expected_sums = "".join(item["sha256"] + "  " + item["file"] + "\n" for item in manifest["artifacts"])
if (root / "deployment-artifacts.sha256").read_text(encoding="utf-8") != expected_sums:
    raise SystemExit("Container checksum inputs differ from the deployment lock")
for artifact in manifest["artifacts"]:
    path = (root / artifact["file"]).resolve()
    if not path.is_relative_to(root) or not path.is_file():
        raise SystemExit(f"Missing deployment artifact: {artifact['gav']}:{artifact['extension']}")
    if hashlib.sha256(path.read_bytes()).hexdigest() != artifact["sha256"]:
        raise SystemExit(f"Deployment artifact hash mismatch: {artifact['gav']}:{artifact['extension']}")
print(f"Verified {len(manifest['artifacts'])} immutable private SDK inputs")
