# Colores y revisión del proyecto — versión 1.11

## Entrega

Se integraron los **19 videos de Colores** en el catálogo. El módulo deja de aparecer como “Próximamente” y abre la misma pantalla de práctica que Familia y Saludos. El catálogo conserva 10 lecciones de Saludos y despedidas y 33 de Familia; Números sigue vacío.

La revisión incluye los videos actuales, la extracción de detecciones, la reproducción del motor Java, las dependencias entre archivos, los recursos de Android y el protocolo del sketch Arduino adjunto. No se certificó la corrección lingüística de las señas ni se probaron alumnos o dispositivos físicos en este entorno.

## Funciones de Colores

- Preparación automática al abrir cada palabra, con progreso de generación.
- Detección de manos, validación por etapas y mensaje de seña completada mediante la cámara frontal.
- Avance automático después del éxito, con una espera de **3 segundos**. Al terminar la última palabra se cierra el módulo, igual que en Familia.
- Botones para repetir el ejemplo, pasar tras una validación y finalizar.
- Cancelación de trabajo antiguo al cambiar de palabra; reinicio de intento y temporizador al pausar la pantalla.
- Referencia independiente por video: su SHA-256, el modelo y el perfil del generador determinan si la caché puede reutilizarse.
- Etapas y tolerancias editables por palabra en `app/src/main/assets/word_lessons.json`.

No se creó una segunda implementación de cámara, caché o avance: Colores usa `WordCatalog`, `WordPracticeActivity`, `WordReferenceStore`, `WordVideoReferenceGenerator`, `WordPracticeEngine` y `WordAutoAdvance`, como los otros módulos de palabras.

### Reemplazar un video

Sustituye el MP4 en `app/src/main/res/raw/`, conserva su nombre, compila e instala la actualización y abre esa palabra. La app calcula la huella del contenido instalado: si cambió, regenera únicamente esa referencia. No hace falta borrar datos, desinstalar ni mapear manualmente. Para actualizar sobre la app existente debes conservar el identificador y la firma.

Agregar un **nombre nuevo** requiere también una entrada en el catálogo; el reemplazo de un nombre ya registrado sí se detecta automáticamente. El archivo de Anaranjado se conserva con el nombre que recibimos: **`c_arananjado.mp4`**. Su texto en pantalla es “Anaranjado”. Para reemplazarlo conserva ese nombre; si decides corregirlo, cambia también `id`/`video` del catálogo y actualiza las pruebas correspondientes.

### Perfil inicial de Colores

Se usa `shape: 0.85`, `meanFlex: 60`, `maxFlex: 115`, `minMotion: 0.04`, `endHoldMs: 140` y `minAttemptMs: 600`. En validación: `motionSamples: 2`, `minDisplacement: 0.12`, `motionNoise: 0.03`, `matchGraceMs: 400`, `maxFingerMismatches: 0`, `finalPoseScale: 1.15` y `sharedRouteOrigin: true`. Las señas de dos fases reciben cuatro controles; las restantes, tres.

`recoverSparseMotion: true` permite recuperar cuadros reales que el selector descarta cuando hay poco movimiento. No crea manos, puntos ni fotogramas inventados. `pathTolerance` vale `0.60`, excepto Blanco, que usa `0.70` para admitir la variante rápida sin bloquear su recorrido.

## Revisión individual de los 19 videos

Los intervalos indican el tramo elegido por el generador. “3/3” significa aprobación del recorrido original, una variante con desplazamiento reducido 20 % y ruido simulado, y una variante con muestreo más rápido. Las observaciones son visuales y técnicas.

| Palabra y video | Observación | Etapas | Hitos / tramo de referencia | Variantes |
| --- | --- | --- | --- | --- |
| Color (`c_color.mp4`) | Dos manos con giro corto del índice sobre la mano de apoyo. | 3 | 5 / 0.1–4.5 s | 3/3 |
| Amarillo (`c_amarillo.mp4`) | Ambas manos cercanas y movimientos cortos; se usa recuperación de hitos reales. | 3 | 8 / 0.2–4 s | 3/3 |
| Rojo (`c_rojo.mp4`) | Una mano junto al rostro y variación pequeña de dedos. | 3 | 5 / 0.1–5.1 s | 3/3 |
| Azul (`c_azul.mp4`) | Mano activa apoyada sobre la otra; cambios de orientación pequeños. | 3 | 11 / 0.1–6.2 s | 3/3 |
| Verde (`c_verde.mp4`) | Manos que se superponen durante el movimiento. | 3 | 7 / 0.1–4.7 s | 3/3 |
| Anaranjado (`c_arananjado.mp4`) | Una mano junto al rostro; se conserva el nombre original del MP4. | 3 | 5 / 0.1–4.7 s | 3/3 |
| Rosa (`c_rosa.mp4`) | Configuración inicial de dedos seguida de movimiento hacia la palma de apoyo. | 4 | 14 / 0.1–7.8 s | 3/3 |
| Morado (`c_morado.mp4`) | Dedos apuntando hacia la mano inferior; posibles superposiciones. | 3 | 9 / 0.1–5.1 s | 3/3 |
| Café (`c_cafe.mp4`) | Dos manos próximas con giro corto de muñeca. | 3 | 9 / 0.1–4.9 s | 3/3 |
| Negro (`c_negro.mp4`) | Movimiento sobre la palma de apoyo; requiere ambas manos. | 3 | 10 / 0.1–5.8 s | 3/3 |
| Blanco (`c_blanco.mp4`) | Entrada de la segunda mano durante la secuencia. Bloqueo corregido y recorrido ampliado a 0.70. | 4 | 16 / 0.1–7.1 s | 3/3 |
| Gris (`c_gris.mp4`) | Postura de dedos sostenida con movimiento corto sobre la palma. | 3 | 8 / 0.1–4.9 s | 3/3 |
| Claro (`c_claro.mp4`) | Dos manos pasan de juntas/cerradas a abiertas. | 3 | 8 / 0.1–3.1 s | 3/3 |
| Oscuro (`c_oscuro.mp4`) | Dos manos pasan de abiertas a juntas/cerradas; inicio y final conservados. | 3 | 11 / 0.1–5 s | 3/3 |
| Brillante (`c_brillante.mp4`) | Apertura y elevación de ambas manos. | 3 | 7 / 0.1–3.3 s | 3/3 |
| Oro (`c_oro.mp4`) | Una mano inicia la configuración y luego aparece la de apoyo. Bloqueo de recorrido corregido. | 4 | 10 / 0.1–7 s | 3/3 |
| Dorado (`c_dorado.mp4`) | Cambio de configuración y entrada de la mano de apoyo. Bloqueo de recorrido corregido. | 4 | 12 / 0.1–5.9 s | 3/3 |
| Plata (`c_plata.mp4`) | Configuración inicial de dedos y desplazamiento a la palma. | 4 | 10 / 0.1–5.9 s | 3/3 |
| Bronce (`c_bronce.mp4`) | Cambio de postura y movimiento sobre la palma de apoyo. | 4 | 8 / 0.1–6.2 s | 3/3 |

## Corrección del bloqueo de recorrido

La primera prueba dejaba a Blanco, Dorado y Oro esperando el último recorrido incluso al reproducir sus propios ejemplos. El motor no actualizaba el historial de recorrido cuando faltaba una mano requerida por **el siguiente control visible**, aunque el hito que se estaba recorriendo solo necesitaba una mano.

En `WordPracticeEngine.java` se movió la observación del recorrido antes de esa comprobación del control visible. Cada hito sigue exigiendo sus propias manos; una mano oculta no se considera detectada. Esto permite registrar la primera fase de una sola mano mientras se espera la incorporación de la segunda. Después de esa corrección solo quedó la variante rápida de Blanco, resuelta con el margen de recorrido indicado arriba.

El generador de referencias no cambió: esta corrección se aplica al motor que interpreta las referencias existentes y nuevas. No necesita borrar las cachés del teléfono.

## Comprobación de los módulos anteriores

En el archivo recibido también cambiaron los videos de “Nos vemos pronto” y “Nos vemos la próxima”. Por eso se regeneraron sus detecciones de escritorio: no se reutilizaron las del video anterior. La configuración recibida de las tres despedidas tenía `pathTolerance: 0.90` y cinco etapas; con ella “Nos vemos pronto” no terminaba en las tres variantes y el recorrido amplio admitía saltos en las comprobaciones de cierre.

Se ajustaron únicamente estas entradas anteriores:

| Lección | Etapas recibidas → entrega | Recorrido recibido → entrega |
| --- | --- | --- |
| Nos vemos pronto | 5 → 6 | 0.90 → 0.40 |
| Nos vemos mañana | 5 → 5 | 0.90 → 0.35 |
| Nos vemos la próxima | 5 → 5 | 0.90 → 0.60 |

Las restantes configuraciones de Saludos y Familia se conservaron. El catálogo recibido está en `tools/catalogo_antes_1_11.json`; la comparación está en `tools/cambios_1_11.json`.

También se corrigieron las pruebas de cierre: sus cortes estaban fijados a tiempos de videos anteriores. Ahora `tools/closure_checks.json` vincula los tiempos revisados con el SHA-256 de cada video. Si vuelves a reemplazarlo, el verificador de escritorio avisa que hay que revisar esos cortes y no aplica tiempos de otro contenido. **Esto no detiene la generación automática dentro de la app.**

No todos los números de etapas resultan apropiados para todos los videos: se prueban los valores configurados de cada palabra. El verificador sigue comprobando que los planes de tres a seis etapas estén ordenados y tengan objetivos válidos.

## Archivos que preguntaste si se pueden borrar

| Archivo | Decisión y motivo |
| --- | --- |
| `JPracticeEngine.java` | **Conservar.** Evalúa la J dinámica y contiene `Sample`, `Stage`, `Tolerance` y operaciones geométricas compartidas con otras letras y palabras. Eliminarlo rompe también componentes de Familia, Saludos y Colores. |
| `JReferenceStore.kt` | **Conservar.** Contiene el tipo de fotograma que usan el mapeador y las referencias de letras dinámicas, calcula huellas y mantiene la compatibilidad con referencias J guardadas anteriormente. Se retiró una comprobación duplicada del mismo recurso. |
| `Esp32BluetoothClient.kt` | **Conservar.** Es el cliente BLE vigente, utilizado por el panel de sensores. Mantiene dos sesiones GATT independientes y recibe el formato BIN20 de tu sketch. No es el cliente SPP antiguo, aunque el nombre del archivo diga Bluetooth. |
| `Bmi160Sample.java` | **Conservar.** Decodifica los 20 bytes en aceleración y velocidad de giro. Se retiró el lector ASCII antiguo que ya no utilizaba la app. |
| `Bmi160Calibration.java` y `Bmi160Panel.kt` | **Conservar.** Calibración de cero y pantalla de lecturas separadas para cada mano. |

El nombre `J` no demuestra que un archivo esté sobrando: parte del código se fue compartiendo al ampliar el proyecto. Se conservaron las clases que todavía tienen dependencias, sin cambiarles el nombre para ocultar esa relación.

### Limpieza realizada

- Se retiraron del APK **10 JSON históricos** de `app/src/main/assets/word_references/`. La práctica ya utiliza las referencias privadas generadas en el teléfono, mediante `WordReferenceStore`. Esos JSON no participaban en el flujo actual.
- Se retiró `app/src/main/res/raw/hola.mp4`, sin llamadas desde código, catálogo o layouts. El video vigente de Hola es `s_hola.mp4`, que se conserva.
- Se retiraron del cliente BLE los accesos antiguos limitados a la mano derecha, el listado de dispositivos emparejados y `sendCommand`, que no tenía llamadas de producción y siempre lanzaba un error con el firmware actual.
- Se sustituyó el nombre de datos `PairedBluetoothDevice` por `BleSensorDevice`, ya que la búsqueda BLE no requiere emparejamiento.
- Se eliminó el parámetro de permisos que `Bmi160Panel` ignoraba y el lanzador duplicado en `MainActivity`. El panel conserva su registro de permisos BLE para búsqueda y conexión.
- Se reemplazaron las pruebas antiguas de sockets RFCOMM, líneas `IMU1` y comandos por pruebas de GATT y paquetes BIN20.
- Los verificadores antiguos conservan su entrada de compatibilidad, pero dirigen al verificador vigente para evitar resultados distintos entre scripts. La exportación histórica de escritorio ya no vuelve a escribir JSON sin uso dentro de `app/assets`.
- Bluetooth quedó como característica opcional del manifiesto, coherente con el uso de la cámara sin sensores.

Los 11 archivos retirados suman **514,713 bytes** sin comprimir. El detalle de rutas, tamaños y huellas está en `tools/limpieza_1_11.json`. Los videos actuales, el modelo de MediaPipe, las imágenes del abecedario, las guías y las herramientas vigentes se conservaron. La entrega excluye salidas de compilación y archivos locales generados por el entorno de Android Studio; se crean de nuevo al abrir y compilar.

## Compatibilidad con tu Arduino actual

Se incluyó una copia **sin cambios** del sketch recibido en `hardware/ESP32_C3_BMI160/ESP32_C3_BMI160.ino`.

| Elemento | Firmware y app |
| --- | --- |
| Transporte | BLE GATT, no Bluetooth clásico SPP. |
| Servicio | `920b0001-4e8d-4f7f-9e51-50b7c41a1600` |
| Característica de datos | `920b0002-4e8d-4f7f-9e51-50b7c41a1600`, notificaciones. |
| Paquete | 20 bytes: secuencia `uint32`, reloj del ESP32 `uint32`, seis valores `int16`. Todos en little-endian. |
| Orden de valores | `ax`, `ay`, `az`, `gx`, `gy`, `gz`. |
| Aceleración | Datos crudos divididos entre `16384`, en g; incluye gravedad. |
| Giroscopio | Datos crudos divididos entre `16.4`, en grados por segundo; no son ángulos de orientación. |
| Frecuencia | 50 muestras/s nominales según el periodo de 20 ms; se conservan los tiempos reales del dispositivo. |
| Dos manos | Direcciones distintas, sesiones y contadores separados. No se permite asignar la misma dirección a ambas manos. |

El sketch distingue los nombres `LSM_C3_Izq` y `LSM_C3_Der` con `MANO_IZQUIERDA`. Para que cada placa anuncie el nombre correspondiente, usa `true` para la izquierda y `false` para la derecha. La app permite asignarlas desde el panel y no exige emparejarlas en Ajustes.

**El sketch recibido solo transmite sensores.** No tiene una característica de comandos escribible, código del motor vibrador ni un comando de vibración al aprobar. Por eso no se atribuye esa función a esta entrega. El estado de éxito de las señas se calcula con la cámara; las lecturas BMI160 no se usan para aprobar Colores ni los otros módulos de palabras.

## Mapa de los archivos principales conservados

| Archivo | Papel actual |
| --- | --- |
| `AppUi.kt` | Estilos, tamaños e inserciones de las pantallas. |
| `Bmi160Calibration.java` | Calcula el cero del giroscopio cuando el dispositivo está inmóvil. |
| `Bmi160Panel.kt` | Busca sensores BLE, asigna ambas manos y muestra/calibra lecturas. |
| `Bmi160Sample.java` | Decodifica BIN20 y conserva tiempos, secuencia y unidades físicas. |
| `BundledMotionMapper.kt` | Genera las referencias de las letras dinámicas incluidas. |
| `Esp32BluetoothClient.kt` | Mantiene dos conexiones GATT, notificaciones y contadores por mano. |
| `HandLandmarkSamples.kt` | Convierte detecciones de letras en muestras geométricas. |
| `HandOverlayView.kt` | Dibuja los puntos y la guía del abecedario estático. |
| `JPracticeEngine.java` | Motor de la J y tipos/operaciones compartidos por los demás evaluadores. |
| `JReferenceStore.kt` | Fotogramas y huellas de letras dinámicas; compatibilidad con referencias J anteriores. |
| `LearningModulesActivity.kt` | Construye el menú de módulos a partir del catálogo. |
| `LetterMotionEngine.kt` | Adapta los motores de letras dinámicas al flujo de práctica. |
| `MainActivity.kt` | Pantalla de entrada, panel de sensores y acceso a los módulos. |
| `MotionHandOverlayView.kt` | Superposición de seguimiento de letras dinámicas; se usa desde XML. |
| `MotionLessonFlow.java` | Controla la demostración y la práctica de una letra dinámica. |
| `MotionReferenceStore.kt` | Guarda/carga referencias y videos de las seis letras dinámicas. |
| `PracticeSignActivity.kt` | Cámara, guías y práctica del abecedario estático y dinámico. |
| `ReferenceTemplate.kt` | Puntos y contornos de las referencias del abecedario estático. |
| `ReferenceViewerActivity.kt` | Vista de la referencia que se abre desde la práctica. |
| `SequencePracticeEngine.java` | Secuencias de letras dinámicas distintas de J. |
| `WordAutoAdvance.java` | Espera única después del éxito y avance cancelable entre palabras. |
| `WordCatalog.kt` | Lee los módulos, videos y opciones por palabra desde el JSON. |
| `WordHandSamples.kt` | Muestras y asociación de las dos manos para palabras. |
| `WordPracticeActivity.kt` | Pantalla compartida de Saludos, Familia y Colores. |
| `WordPracticeEngine.java` | Evalúa postura, movimiento, recorrido y cierre de cada palabra. |
| `WordReferenceBuilder.java` | Selecciona hitos reales y comprueba calidad del video. |
| `WordReferenceStore.kt` | Caché atómica privada por video, huellas y regeneración selectiva. |
| `WordStageOptions.java` | Opciones y límites válidos de las etapas. |
| `WordStagePlan.java` | Convierte los hitos en controles ordenados. |
| `WordTolerance.java` | Tolerancias generales de palabras que el catálogo puede sobrescribir. |
| `WordVideoReferenceGenerator.kt` | Decodifica el video en Android y obtiene detecciones MediaPipe. |

Las actividades se revisaron contra el manifiesto. Las vistas de cámara y las superposiciones también tienen referencias desde layouts XML; no se clasificaron como innecesarias solo porque no apareciera un constructor explícito en Kotlin.

## Pruebas y límites

- **Colores: 57/57** variantes completas aprobadas, 19/19 manos inmóviles rechazadas y 19/19 saltos directos al final rechazados.
- **Conjunto completo: 186/186** variantes completas aprobadas para 62 videos. Se rechazaron 62/62 casos inmóviles y 62/62 saltos de extremos. Se comprueba además la ausencia de una mano cuando la referencia la exige.
- **Despedidas: 28/28** interrupciones revisadas rechazadas y tres saltos del tramo intermedio al final rechazados.
- Pasaron las comprobaciones de avance automático, límites de configuración, margen de postura y zona final.
- Se compilaron los componentes Java centrales para Java 11. El cliente BLE Kotlin real se compiló para JVM 11 contra dobles de Android y pasó: búsqueda por UUID, dos conexiones, contadores separados, duplicados, reinicios, fallos de servicio/suscripción, desconexión de una sola mano, cancelación y callbacks obsoletos.
- Pasaron la decodificación BIN20 con signos y `uint32`, las escalas del firmware y las calibraciones independientes.
- La auditoría estática comprobó el registro de los 19 MP4 de Colores, los recursos explícitos, las actividades, los UUID y las huellas de 62 videos, del modelo y de las detecciones.
- Persisten **31/342 cruces aceptados entre colores distintos** y **227/3,782** entre todos los videos. Se aceptaron 17 reproducciones invertidas en el diagnóstico, especialmente en movimientos repetidos. Estos números describen las pruebas de software disponibles; no equivalen a precisión con personas ni garantizan una evaluación lingüística estricta.
- **No se generó un APK**: se intentó compilar Android y la descarga de Gradle 9.7.1 quedó bloqueada por la red del entorno. Las pruebas de caché en Android se ampliaron a los tres módulos, pero requieren teléfono o emulador y no se ejecutaron aquí. Las simulaciones BLE no reemplazan una conexión física con tus ESP32.

Evidencia en `tools/resultados_modulos_1_11.txt`, `tools/resultados_ble_1_11.txt`, `tools/auditoria_estatica_1_11.txt` y `tools/compilacion_android_1_11.txt`.

## Abrir y comprobar en tu teléfono

1. Descomprime en una carpeta nueva y abre el proyecto en Android Studio.
2. Selecciona tu SDK local, sincroniza Gradle y ejecuta en el teléfono. No hace falta recuperar `app/build`; se genera al compilar.
3. Entra en Aprender LMS → Colores. La primera apertura de cada palabra prepara su referencia. Prueba especialmente Blanco, Dorado y Oro, que reproducían bloqueos en la primera comprobación.
4. Completa una seña, espera tres segundos y comprueba el avance. Prueba Repetir ejemplo, la última palabra y una pausa de la app.
5. Sustituye un MP4 conservando el nombre, vuelve a compilar e instalar y abre esa palabra para comprobar la regeneración por contenido.
6. Desde el panel de sensores, conecta ambos ESP32 y comprueba datos separados. Desconecta uno y verifica que el otro siga recibiendo.

Para repetir las comprobaciones de escritorio:

```bash
python tools/verificar_modulos.py
python tools/verificar_proyecto.py
python pruebas/verificar_conexiones.py
```

Si cambian videos o modelo, instala las dependencias de análisis y regenera las detecciones de escritorio:

```bash
python -m pip install -r tools/requirements-palabras.txt
python tools/verificar_modulos.py --reanalyze
```

Esto es para las pruebas en computadora. La app sigue generando sus referencias automáticamente.
