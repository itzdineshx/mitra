# ML (Python 3.11)

Planned contents (tasks T06, T11):
- `recorder_export/` — turn on-device step logs (with consent) into training examples.
- `finetune/` — LoRA fine-tuning of Gemma 4 E2B for step JSON output.
- `export/` — convert to `.litertlm` for LiteRT-LM.
- `classifier/` — small injection/scam text classifier for the Guardian.

Model files are large: **never commit them** (`*.litertlm`, `*.tflite`, `*.safetensors` are git-ignored).
Download links and checksums go in `ml/MODELS.md`.
