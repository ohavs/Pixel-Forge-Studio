# facecheck
Generates WFF XML for every preset (plus a face with every layer kind) from `editor/FaceModel.kt`
on the JVM and validates it against the official WFF XSD — catches XML errors without a phone.

```sh
cd tools/facecheck && gradle -q run          # writes out/*.xml
git clone -q --depth 1 --filter=blob:none --sparse https://github.com/google/watchface.git /tmp/wff
(cd /tmp/wff && git sparse-checkout set third_party/wff/specification)
pip install xmlschema
python3 -I check.py /tmp/wff/third_party/wff/specification/documents/1/watchface.xsd out/*.xml
```
