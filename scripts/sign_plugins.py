#!/usr/bin/env python3
"""Sign plugins-builtin/*/plugin.json with Ed25519 (development key). Writes plugin.sig next to each file."""
import argparse, base64, pathlib, sys
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey

ROOT = pathlib.Path(__file__).resolve().parent.parent
KEY = ROOT / "keys" / "dev_ed25519.pem"

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--new-key", action="store_true", help="create a new development key")
    args = ap.parse_args()
    if args.new_key:
        KEY.parent.mkdir(exist_ok=True)
        if KEY.exists():
            print(f"{KEY} already exists; delete it first if you really want a new key"); return 1
        key = Ed25519PrivateKey.generate()
        KEY.write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    if not KEY.exists():
        print("No key. Run with --new-key first."); return 1
    key = serialization.load_pem_private_key(KEY.read_bytes(), password=None)
    pub = key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
    print("Public key (X.509, Base64) for PluginKeys.kt:\n" + base64.b64encode(pub).decode())
    for f in sorted((ROOT / "plugins-builtin").glob("*/plugin.json")):
        sig = key.sign(f.read_bytes())
        (f.parent / "plugin.sig").write_text(base64.b64encode(sig).decode())
        print(f"signed {f.relative_to(ROOT)}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
