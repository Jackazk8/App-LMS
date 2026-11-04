"""Compila el cliente BLE real contra dobles GATT. Requiere JDK 17+ y Python 3.
No sustituye una prueba con Android/ESP32. LMS_TEST_DEPS permite reutilizar los JAR.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
from urllib.request import urlretrieve
from concurrent.futures import ThreadPoolExecutor
import os, subprocess, zipfile
here=Path(__file__).resolve().parent
source=here.parent/'app/src/main/java/com/mechrobotix/aprendels'
items=[('org.jetbrains.kotlin','kotlin-compiler-embeddable','2.2.0'),('org.jetbrains.kotlin','kotlin-stdlib','2.2.0'),('org.jetbrains.kotlin','kotlin-script-runtime','2.2.0'),('org.jetbrains.kotlin','kotlin-reflect','1.6.10'),('org.jetbrains.kotlin','kotlin-daemon-embeddable','2.2.0'),('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.8.0'),('org.jetbrains','annotations','13.0')]
deps=Path(os.environ.get('LMS_TEST_DEPS',Path.home()/'.cache/aprendels-kotlin-2.2.0'))
deps.mkdir(parents=True,exist_ok=True)
with TemporaryDirectory(prefix='aprendels-test-') as directory:
    work=Path(directory);classes=work/'classes';classes.mkdir()
    def download(item):
        g,a,v=item
        target=deps/(a+'.jar')
        if not target.exists() or not zipfile.is_zipfile(target):
            temporary=target.with_suffix('.part')
            urlretrieve('https://repo.maven.apache.org/maven2/'+g.replace('.','/')+'/'+a+'/'+v+'/'+a+'-'+v+'.jar',temporary)
            if not zipfile.is_zipfile(temporary):raise RuntimeError(f'Descarga incompleta de {a}; vuelve a ejecutar')
            temporary.replace(target)
    with ThreadPoolExecutor(max_workers=4) as executor:
        list(executor.map(download,items))
    def run(args):subprocess.run([str(x) for x in args],check=True)
    run(['java','com.sun.tools.javac.Main','--release','11','-d',classes,source/'Bmi160Sample.java',source/'Bmi160Calibration.java'])
    cp=os.pathsep.join([str(classes),str(deps/'kotlin-stdlib.jar')])
    run(['java','-cp',str(deps/'*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','11','-classpath',cp,'-d',classes,here/'stubs',source/'Esp32BluetoothClient.kt',here/'DualClientTest.kt',here/'CalibrationTest.kt'])
    run(['java','-cp',cp,'DualClientTestKt'])
    run(['java','-cp',cp,'CalibrationTestKt'])
