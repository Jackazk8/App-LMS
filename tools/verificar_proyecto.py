#!/usr/bin/env python3
"""Verifica catálogo, recursos y protocolo del firmware incluido; no sustituye compilar Android."""
from pathlib import Path
import hashlib,json,re,xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
main=root/'app/src/main'
catalog=json.loads((main/'assets/word_lessons.json').read_text(encoding='utf-8'))
modules=catalog['modules'];ids=set();module_ids=set();rows=[]
for module in modules:
    assert module['id'] not in module_ids, f"Módulo duplicado: {module['id']}"
    module_ids.add(module['id'])
    for row in module['lessons']:
        assert row['id'] not in ids, f"Lección duplicada: {row['id']}"
        ids.add(row['id']);rows.append(row)
        assert (main/'res/raw'/f"{row['video']}.mp4").is_file(),f"Falta video: {row['video']}"
colors={p.stem for p in (main/'res/raw').glob('c_*.mp4')}
registered={row['video'] for m in modules if m['id']=='colores' for row in m['lessons']}
assert colors==registered,f"Colores sin registrar o sin video: {colors ^ registered}"
print('PASS catálogo:',', '.join(f"{m['title']}={len(m['lessons'])}" for m in modules))

sources=list((main/'java').rglob('*.kt'))+list((main/'java').rglob('*.java'))
texts=[p.read_text(encoding='utf-8') for p in sources]
for path,text in zip(sources,texts):
    for kind,name in re.findall(r'(?<!android\.)\bR\.(raw|drawable|layout)\.([a-z][a-z0-9_]*)',text):
        assert any(p.stem.split('.')[0]==name for d in (main/'res').glob(kind+'*') for p in d.iterdir()),f"Recurso faltante en {path.name}: {kind}/{name}"
ns='{http://schemas.android.com/apk/res/android}'
manifest=ET.parse(main/'AndroidManifest.xml').getroot()
for activity in manifest.findall('./application/activity'):
    name=activity.attrib[ns+'name'].split('.')[-1]
    assert any(p.stem==name for p in sources),f"Actividad ausente: {name}"
print('PASS recursos explícitos y actividades del manifiesto')

model=hashlib.sha256((main/'assets/hand_landmarker.task').read_bytes()).hexdigest()
for row in rows:
    name=row['video'];fixture=root/'tools/fixtures_auto'/f'{name}.txt'
    meta=json.loads(fixture.with_suffix('.metadata.json').read_text())
    assert meta['videoSha256']==hashlib.sha256((main/'res/raw'/f'{name}.mp4').read_bytes()).hexdigest(),f'Video cambió: {name}'
    assert meta['modelSha256']==model,f'Modelo cambió: {name}'
    assert meta['detectionsSha256']==hashlib.sha256(fixture.read_bytes()).hexdigest(),f'Detecciones cambiaron: {name}'
print(f'PASS integridad por SHA-256 de {len(rows)} videos, modelo y detecciones')

firmware=(root/'hardware/ESP32_C3_BMI160/ESP32_C3_BMI160.ino').read_text()
client=(main/'java/com/mechrobotix/aprendels/Esp32BluetoothClient.kt').read_text()
for key,field in [('SERVICE_UUID','serviceUuid'),('DATA_UUID','dataUuid')]:
    fw=re.search(rf'{key}\s*=\s*"([^"]+)"',firmware).group(1)
    app=re.search(rf'{field}.*UUID.fromString\("([^"]+)"',client).group(1)
    assert fw==app,f'UUID distinto: {key}'
assert 'uint8_t packet[20]' in firmware and 'PROPERTY_NOTIFY' in firmware
assert 'createRfcommSocket' not in '\n'.join(texts)
assert 'sendCommand(' not in '\n'.join(texts)
assert 'Bmi160Sample.parse(' not in '\n'.join(texts)
assert not (main/'assets/word_references').exists()
print('PASS UUID BLE, BIN20 de 20 bytes y retiro de compatibilidad SPP sin llamadas')
print('Pendiente: compilación Android y pruebas físicas de cámara/ESP32.')
