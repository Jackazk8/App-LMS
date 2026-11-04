# Colores, archivos y Bluetooth — revisión actualizada 1.13

## Actualización 1.13: completar el recorrido y corregir Dorado

### Diagnóstico en el código 1.12

La configuración de Colores tenía `phaseValidation: true` pero `pathTolerance: 0`. En ese modo el motor no llamaba al cursor de recorrido que sí utiliza Familia. Por eso podía encontrar una postura de final compatible después de una parte del movimiento y aprobarla sin confirmar que el recorrido de la referencia hubiera terminado. El requisito de movimiento final permitía desde `minTravelProgress: 0.18` y el intento mínimo era 1,100 ms; esos límites no garantizaban llegar al último tramo del video.

Dorado tenía además una condición distinta. Su referencia dura 6.1 s: la mano activa aparece al principio y al final, mientras que MediaPipe detecta dos manos aproximadamente entre 3.5 y 5.0 s. En el perfil de cuatro fases, el control intermedio podía depender de la mano de apoyo. Si la cámara del teléfono no la detectaba de forma estable, esa fase quedaba bloqueada. En la métrica de movimiento por fases tampoco se contaba el desplazamiento de muñeca cuando solo había una mano visible; se medían principalmente cambios de dedos y la relación entre dos manos.

### Correcciones aplicadas

- Se activó el seguimiento ordenado del recorrido en las 19 señas de Colores. El margen es `pathTolerance: 0.65`, y `0.75` para Dorado. Se usan hitos de referencia con tolerancia normalizada por tamaño de palma; no se exige copiar cada fotograma ni respetar el tiempo exacto del video.
- En las señas de tres fases, el motor permanece en “recorrido” hasta que el cursor alcanza el final de la secuencia. Solo entonces habilita la posición final. En las de cuatro fases se puede reconocer el cambio intermedio durante el gesto, pero no se valida el cierre antes de completar el recorrido.
- Si una referencia muestra dos manos y MediaPipe pierde brevemente una, el cursor puede avanzar cuando una mano visible coincide con el hito. Las comprobaciones de postura inicial y final conservan las manos requeridas por esas posturas.
- Cuando solo se detecta una mano, el movimiento de su muñeca también aporta evidencia de recorrido. Con dos manos se conserva la comparación relativa para reducir falsos movimientos por traslación común de la imagen.
- Dorado pasa de cuatro a tres fases: inicio → recorrido → final. Su video mantiene un movimiento continuo; quitar el control intermedio sensible a la segunda mano evita que una oclusión detenga el intento. El recorrido completo sigue siendo obligatorio.
- Saludos, Familia y Números conservan su configuración. El cambio de cursor por fases solo se activa con `phaseValidation` en Colores; los parámetros de Familia y su orden no se modifican.

### Comportamiento y progreso

| Fase | Comportamiento | Progreso orientativo |
| --- | --- | ---: |
| Inicio | Confirma la postura durante 350 ms. Mostrar las manos no cuenta como recorrido. | 0 % durante la preparación; 15 % al confirmar el inicio |
| Recorrido | Comprueba movimiento y avance ordenado por los hitos, con tolerancia amplia. En 3 fases no permite entrar al cierre antes de recorrer la referencia. | 15 % en 3 fases; 15 % y luego 50 % en 4 fases |
| Cambio clave | En las señas de 4 fases reconoce la postura intermedia sin pedir que se congele en un cuadro; retiene el resultado mientras sigue el recorrido. | 50 % |
| Final | Solo se habilita después de completar el recorrido. Confirma la postura final durante 200 ms y respeta el mínimo de intento de 1,100 ms. | 75 % en 3 fases; 80 % en 4 fases |
| Acierto | Conserva el avance automático de 3 segundos. | 100 % |

### Fases por color

| Palabra | Fases | Nota |
| --- | ---: | --- |
| Color | 3 | Inicio → recorrido → final |
| Amarillo | 3 | Inicio → recorrido → final |
| Rojo | 3 | Inicio → recorrido → final |
| Azul | 3 | Inicio → recorrido → final |
| Verde | 3 | Inicio → recorrido → final |
| Anaranjado | 3 | Inicio → recorrido → final |
| Rosa | 4 | Conserva el cambio clave |
| Morado | 3 | Inicio → recorrido → final |
| Café | 3 | Inicio → recorrido → final |
| Negro | 3 | Inicio → recorrido → final |
| Blanco | 3 | Inicio → recorrido → final |
| Gris | 3 | Inicio → recorrido → final |
| Claro | 3 | Inicio → recorrido → final |
| Oscuro | 3 | Inicio → recorrido → final |
| Brillante | 3 | Inicio → recorrido → final |
| Oro | 4 | Conserva el cambio clave |
| Dorado | 3 | Se elimina el control intermedio que dependía de detectar la mano de apoyo |
| Plata | 4 | Conserva el cambio clave |
| Bronce | 3 | Inicio → recorrido → final |

### Archivos actualizados

| Archivo | Cambio |
| --- | --- |
| `WordPracticeEngine.java` | El cursor ordenado funciona también en fase de Colores; el cierre espera su final. Movimiento de muñeca para una mano, tolerancia a mano de apoyo ocluida durante el recorrido y memoria del control intermedio. |
| `word_lessons.json` | `pathTolerance: 0.65` en Colores (`0.75` Dorado); Dorado queda en 3 fases. Los otros módulos conservan sus valores. |
| `app/build.gradle.kts` | Versión 1.13, código 14. |
| `README.md` | Explica la causa, el cambio y el paso de instalación. |
| Este archivo | Se conserva el historial anterior de videos, archivos y Bluetooth. |

### Comprobación y alcance

Se revisaron el código, las opciones de Colores y Familia, el JSON y la secuencia de detecciones de `c_dorado.mp4`. El perfil nuevo mantiene las validaciones anteriores de postura y avance automático. **No se ejecutaron las pruebas del proyecto ni una prueba con cámara real para esta actualización**; el APK debe instalarse y probarse en el teléfono, especialmente en Dorado y en los cierres de Negro, Rosa, Oro y Plata. Los resultados de la sección 1.12 que sigue corresponden a la versión anterior y no son resultados de esta corrección.

---

## Historial conservado: actualización 1.12

Se actualiza **este mismo archivo de revisión**. Las notas anteriores de videos, limpieza y Bluetooth se conservan más abajo como historial de la entrega 1.11. Esta sección describe el comportamiento vigente de Colores.

### Problema reportado y causa

Al presentar las manos, algunas palabras ya mostraban cerca de la mitad de la validación. Había dos problemas: la barra otorgaba un tercio o un cuarto del total solo por reconocer la postura inicial, y el control de movimiento aceptaba cambios demasiado pequeños del detector. Además, algunos controles intermedios se comprobaban después de que la persona ya había pasado por esa postura.

Se cambió el criterio de evaluación, no solo el número de pasos. Colores ahora tiene un perfil **por fases amplias**: reconoce el inicio, reúne evidencia de un gesto real y comprueba su cierre. No exige recorrer todos los hitos ni durar lo mismo que el video. El perfil nuevo está activado únicamente en las 19 lecciones de Colores.

### Cómo se evalúa ahora

| Momento | Qué se exige | Progreso mostrado |
| --- | --- | --- |
| Prepararse y colocar las manos | Una postura inicial compatible y estable durante 350 ms. Los cuadros de preparación no se guardan como recorrido. | 0 % mientras se confirma. |
| Inicio reconocido | Ya se puede empezar el gesto. Mostrar las manos, sostenerlas o moverlas juntas no basta para seguir avanzando. | 15 %. |
| Recorrido | Cambios distribuidos en varias observaciones reales: dedos, orientación de la mano o relación entre ambas manos. | Pasa a comprobar el cierre: 75 % en tres fases; 50 % en cuatro fases. |
| Cambio clave, cuando hace falta | Una configuración intermedia importante, observada durante el gesto o al llegar al control. No requiere congelarse en un fotograma del ejemplo. | 80 % al pasar a comprobar el cierre en cuatro fases. |
| Cierre | Postura y región final compatibles, estabilidad durante 200 ms y tiempo mínimo de intento cumplido. | 100 % y luego avance automático tras tres segundos. |

Los porcentajes orientan la pantalla; no son una medida de cuánto video se copió. **Reconocer el inicio deja la barra en 15 %, no a la mitad.** La barra no supera esa preparación sin evidencia de movimiento.

En las palabras con cuatro fases, el motor recuerda un cambio válido que haya visto durante el recorrido. Así no te pide volver a una postura que ya realizaste mientras estaba reuniendo evidencia del movimiento. Se requieren dos observaciones válidas del cambio y un recorrido confirmado antes de aprovechar ese recuerdo.

### Qué movimientos de preparación se descartan

- Trasladar ambas manos juntas por la imagen no cuenta por sí solo como gesto.
- Acercarlas o alejarlas juntas, cambiando su tamaño aparente, tampoco cuenta por sí solo.
- Mantener una postura con pequeñas variaciones del detector no completa el recorrido.
- Un salto de una sola imagen al final, seguido de espera o ruido, no completa el recorrido.

Para distinguirlos se comparan puntos de cada mano relativos a su muñeca y tamaño de palma, cambios de flexión y separación relativa de ambas manos. El cambio debe aparecer repartido entre observaciones reales. No se interpolan posiciones para convertir un salto en movimiento continuo.

Si una mano se pierde brevemente, no se inventa su posición. Se pueden conservar observaciones reales de la mano visible durante el margen de recuperación; las posturas que requieren ambas manos siguen exigiéndolas para aprobar.

### Tres o cuatro fases por palabra

Se usan tres fases generales para los movimientos continuos. Rosa, Oro, Dorado y Plata conservan un cuarto control porque tienen un cambio intermedio útil. Blanco y Bronce pasan de cuatro a tres para evitar un control intermedio que obligaba a regresar a una postura anterior.

| Palabra | Fases | Criterio general |
| --- | --- | --- |
| Color (`c_color`) | 3 | Inicio → recorrido → cierre |
| Amarillo (`c_amarillo`) | 3 | Inicio → recorrido → cierre |
| Rojo (`c_rojo`) | 3 | Inicio → recorrido → cierre |
| Azul (`c_azul`) | 3 | Inicio → recorrido → cierre |
| Verde (`c_verde`) | 3 | Inicio → recorrido → cierre |
| Anaranjado (`c_arananjado`) | 3 | Inicio → recorrido → cierre |
| Rosa (`c_rosa`) | 4 | Inicio → recorrido con cambio clave → cierre |
| Morado (`c_morado`) | 3 | Inicio → recorrido → cierre |
| Café (`c_cafe`) | 3 | Inicio → recorrido → cierre |
| Negro (`c_negro`) | 3 | Inicio → recorrido → cierre |
| Blanco (`c_blanco`) | 3 | Inicio → recorrido → cierre |
| Gris (`c_gris`) | 3 | Inicio → recorrido → cierre |
| Claro (`c_claro`) | 3 | Inicio → recorrido → cierre |
| Oscuro (`c_oscuro`) | 3 | Inicio → recorrido → cierre |
| Brillante (`c_brillante`) | 3 | Inicio → recorrido → cierre |
| Oro (`c_oro`) | 4 | Inicio → recorrido con cambio clave → cierre |
| Dorado (`c_dorado`) | 4 | Inicio → recorrido con cambio clave → cierre |
| Plata (`c_plata`) | 4 | Inicio → recorrido con cambio clave → cierre |
| Bronce (`c_bronce`) | 3 | Inicio → recorrido → cierre |

### Parámetros vigentes y su significado

Los ajustes están en `app/src/main/assets/word_lessons.json`, por palabra.

| Parámetro | Colores 1.12 | Función |
| --- | --- | --- |
| `validation.phaseValidation` | `true` | Activa la evaluación por fases amplias. En los otros módulos conserva su valor predeterminado `false`. |
| `validation.stages` | `3` o `4` | Cantidad de fases significativas; no cantidad de fotogramas que hay que reproducir. |
| `tolerance.startHoldMs` | `350` | Confirmación de la postura inicial antes de registrar el gesto. |
| `tolerance.stability` | `0.20` | Reduce cuánto puede variar la mano mientras se confirma inicio o cierre. El cambio intermedio puede observarse durante el movimiento. |
| `validation.minMotionMs` | `250`; Anaranjado `200` | Evidencia temporal de movimiento válido. Estar quieto no acumula este requisito. |
| `validation.motionSamples` | `3`; Anaranjado `2` | Observaciones útiles del movimiento. Cada una debe contener un cambio distribuido entre varias lecturas reales. |
| `validation.minDisplacement` | `0.14` | Cambio mínimo normalizado del gesto. Se ajusta también a la amplitud del ejemplo; no se mide en píxeles. |
| `validation.phasePositionTolerance` | `0.90` | Región amplia de colocación relativa entre manos al evaluar las posturas. No es un trazado exacto. |
| `validation.pathTolerance` | `0` | El perfil por fases no usa el cursor que exige visitar todos los hitos del video. La evaluación permanece activa mediante los controles anteriores. |
| `tolerance.endHoldMs` | `200` | Confirmación estable del final. |
| `tolerance.minAttemptMs` | `1100` | Duración mínima desde el inicio confirmado; no sustituye la evidencia de movimiento. |
| `tolerance.shape` / `meanFlex` / `maxFlex` | `0.65` / `50` / `100` | Se ajustaron las posturas para conservar diferencias entre colores al ampliar el criterio de recorrido. |
| `validation.finalPoseScale` | `1.10` | Margen adicional de la postura final. |
| `validation.maxFingerMismatches` | `0` | Conserva la comprobación de dedos claramente abiertos/cerrados. |

**Usa el código y el catálogo actualizados juntos.** La versión anterior no admite los nuevos campos del JSON. Bajar únicamente `pathTolerance` en el código anterior no implementa este comportamiento.

La espera de tres segundos después del éxito y la regeneración por SHA-256 al reemplazar un MP4 conservan su funcionamiento. No hay que borrar las referencias del teléfono: este cambio afecta cómo se evalúan sus datos al practicar. Si cambias un video, instala la nueva compilación para que la app reciba ese contenido y detecte su cambio.

### Archivos actualizados en esta revisión

| Archivo | Actualización |
| --- | --- |
| `WordPracticeEngine.java` | Evidencia de gesto independiente de la entrada en cámara, protección contra saltos, progreso por fases y memoria del cambio intermedio. |
| `WordStagePlan.java` | Alternativas amplias de cada fase, sin exigir visitar toda la secuencia de hitos. |
| `WordStageOptions.java` / `WordCatalog.kt` | Nuevos campos `phaseValidation`, `minMotionMs` y `phasePositionTolerance`, con lectura y límites válidos. |
| `app/src/main/assets/word_lessons.json` | Perfil nuevo de las 19 lecciones de Colores. Las entradas de Saludos, Familia y Números se conservaron. |
| `tools/WordPhaseCheck.java` | Pruebas del problema reportado: preparación, temblor, entrada/escala y saltos al final. |
| `tools/WordModuleCheck.java` / `tools/verificar_modulos.py` | Incluyen esas pruebas nuevas junto con las comprobaciones existentes. |
| `WordAutoReferenceTest.kt` | Añade una comprobación Android de que el perfil nuevo solo esté activo en Colores; pendiente de ejecutar en teléfono/emulador. |
| `app/build.gradle.kts` | Versión 1.12, código 13. |
| Este archivo y `README.md` | Comportamiento vigente, parámetros, evidencias y límites. Las anotaciones 1.11 se conservan como historial. |

No se eliminaron más archivos del proyecto ni se modificó el firmware en esta actualización. La revisión de dependencias y Bluetooth de 1.11 sigue documentada en el historial.

### Resultados de comprobación de la actualización

- **57/57** reproducciones completas de Colores aprobadas: normal, recorrido reducido con ruido y muestreo más rápido.
- **95/95** casos nuevos de preparación o avance falso rechazados: cinco por cada color. Se probaron postura fija, postura con ruido, traslación/escala común de entrada, salto al final y salto al final seguido de ruido. En esos casos el progreso no superó el **15 %** de preparación; antes de confirmar el inicio permaneció en 0 %.
- **186/186** reproducciones completas aprobadas entre los 62 videos de palabras. También se rechazaron 62 casos inmóviles y 62 saltos directos al final.
- Las 43 lecciones anteriores conservaron exactamente sus resultados de reproducción y diagnóstico, además de sus configuraciones. Pasaron las 28 interrupciones de despedidas y los tres saltos de su tramo intermedio al final.
- Los cruces entre colores distintos aceptados bajaron de **31/342 a 25/342** en estos ejemplos; los del conjunto completo, de **227/3,782 a 212/3,782**. Persisten confusiones y se aceptan 16 secuencias invertidas en el diagnóstico; esto no equivale a una certificación lingüística ni mide precisión con alumnos.
- Los componentes Java centrales se compilaron para Java 11 y pasó la auditoría de catálogo, recursos y huellas. Las pruebas de avance automático y de límites de configuración también pasaron.
- Se intentó generar el APK, pero la descarga de Gradle 9.7.1 sigue bloqueada por la red de este entorno. **Falta comprobar el resultado con la cámara real del teléfono.** La prueba Android nueva tampoco se ejecutó aquí. Los resultados BLE anteriores se conservan; no se atribuye una nueva prueba física de sensores a esta actualización.

Evidencia: `tools/resultados_modulos_1_12.txt`, `tools/auditoria_estatica_1_12.txt`, `tools/compilacion_android_1_12.txt`, `tools/cambios_1_12.json` y `tools/catalogo_antes_1_12.json`.

### Comprobación recomendada en el teléfono

Instala la actualización desde Android Studio y abre Colores. Presenta las manos y déjalas quietas: la barra debe quedarse en preparación. Mueve ambas manos juntas hacia la cámara sin cambiar el gesto: eso tampoco debe completar el recorrido. Después realiza una seña completa y comprueba su cierre y el avance tras tres segundos. Prueba especialmente los movimientos pequeños de Azul, Gris y Anaranjado, y las superposiciones de Negro, Dorado y Rosa.

Para repetir las pruebas de escritorio: `python tools/verificar_modulos.py --module colores` y `python tools/verificar_modulos.py`. Si cambias videos, regenera sus detecciones de escritorio con `--reanalyze`; la generación dentro de la app sigue siendo automática.

---

## Historial conservado: revisión de videos, archivos y Bluetooth 1.11

Las notas siguientes describen la entrega anterior. Sus cifras y su perfil inicial de Colores son históricos; para el comportamiento vigente utiliza la actualización 1.12 de arriba.


### Entrega

Se integraron los **19 videos de Colores** en el catálogo. El módulo deja de aparecer como “Próximamente” y abre la misma pantalla de práctica que Familia y Saludos. El catálogo conserva 10 lecciones de Saludos y despedidas y 33 de Familia; Números sigue vacío.

La revisión incluye los videos actuales, la extracción de detecciones, la reproducción del motor Java, las dependencias entre archivos, los recursos de Android y el protocolo del sketch Arduino adjunto. No se certificó la corrección lingüística de las señas ni se probaron alumnos o dispositivos físicos en este entorno.

### Funciones de Colores

- Preparación automática al abrir cada palabra, con progreso de generación.
- Detección de manos, validación por etapas y mensaje de seña completada mediante la cámara frontal.
- Avance automático después del éxito, con una espera de **3 segundos**. Al terminar la última palabra se cierra el módulo, igual que en Familia.
- Botones para repetir el ejemplo, pasar tras una validación y finalizar.
- Cancelación de trabajo antiguo al cambiar de palabra; reinicio de intento y temporizador al pausar la pantalla.
- Referencia independiente por video: su SHA-256, el modelo y el perfil del generador determinan si la caché puede reutilizarse.
- Etapas y tolerancias editables por palabra en `app/src/main/assets/word_lessons.json`.

No se creó una segunda implementación de cámara, caché o avance: Colores usa `WordCatalog`, `WordPracticeActivity`, `WordReferenceStore`, `WordVideoReferenceGenerator`, `WordPracticeEngine` y `WordAutoAdvance`, como los otros módulos de palabras.

#### Reemplazar un video

Sustituye el MP4 en `app/src/main/res/raw/`, conserva su nombre, compila e instala la actualización y abre esa palabra. La app calcula la huella del contenido instalado: si cambió, regenera únicamente esa referencia. No hace falta borrar datos, desinstalar ni mapear manualmente. Para actualizar sobre la app existente debes conservar el identificador y la firma.

Agregar un **nombre nuevo** requiere también una entrada en el catálogo; el reemplazo de un nombre ya registrado sí se detecta automáticamente. El archivo de Anaranjado se conserva con el nombre que recibimos: **`c_arananjado.mp4`**. Su texto en pantalla es “Anaranjado”. Para reemplazarlo conserva ese nombre; si decides corregirlo, cambia también `id`/`video` del catálogo y actualiza las pruebas correspondientes.

#### Perfil inicial de Colores

Se usa `shape: 0.85`, `meanFlex: 60`, `maxFlex: 115`, `minMotion: 0.04`, `endHoldMs: 140` y `minAttemptMs: 600`. En validación: `motionSamples: 2`, `minDisplacement: 0.12`, `motionNoise: 0.03`, `matchGraceMs: 400`, `maxFingerMismatches: 0`, `finalPoseScale: 1.15` y `sharedRouteOrigin: true`. Las señas de dos fases reciben cuatro controles; las restantes, tres.

`recoverSparseMotion: true` permite recuperar cuadros reales que el selector descarta cuando hay poco movimiento. No crea manos, puntos ni fotogramas inventados. `pathTolerance` vale `0.60`, excepto Blanco, que usa `0.70` para admitir la variante rápida sin bloquear su recorrido.

### Revisión individual de los 19 videos

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

### Corrección del bloqueo de recorrido

La primera prueba dejaba a Blanco, Dorado y Oro esperando el último recorrido incluso al reproducir sus propios ejemplos. El motor no actualizaba el historial de recorrido cuando faltaba una mano requerida por **el siguiente control visible**, aunque el hito que se estaba recorriendo solo necesitaba una mano.

En `WordPracticeEngine.java` se movió la observación del recorrido antes de esa comprobación del control visible. Cada hito sigue exigiendo sus propias manos; una mano oculta no se considera detectada. Esto permite registrar la primera fase de una sola mano mientras se espera la incorporación de la segunda. Después de esa corrección solo quedó la variante rápida de Blanco, resuelta con el margen de recorrido indicado arriba.

El generador de referencias no cambió: esta corrección se aplica al motor que interpreta las referencias existentes y nuevas. No necesita borrar las cachés del teléfono.

### Comprobación de los módulos anteriores

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

### Archivos que preguntaste si se pueden borrar

| Archivo | Decisión y motivo |
| --- | --- |
| `JPracticeEngine.java` | **Conservar.** Evalúa la J dinámica y contiene `Sample`, `Stage`, `Tolerance` y operaciones geométricas compartidas con otras letras y palabras. Eliminarlo rompe también componentes de Familia, Saludos y Colores. |
| `JReferenceStore.kt` | **Conservar.** Contiene el tipo de fotograma que usan el mapeador y las referencias de letras dinámicas, calcula huellas y mantiene la compatibilidad con referencias J guardadas anteriormente. Se retiró una comprobación duplicada del mismo recurso. |
| `Esp32BluetoothClient.kt` | **Conservar.** Es el cliente BLE vigente, utilizado por el panel de sensores. Mantiene dos sesiones GATT independientes y recibe el formato BIN20 de tu sketch. No es el cliente SPP antiguo, aunque el nombre del archivo diga Bluetooth. |
| `Bmi160Sample.java` | **Conservar.** Decodifica los 20 bytes en aceleración y velocidad de giro. Se retiró el lector ASCII antiguo que ya no utilizaba la app. |
| `Bmi160Calibration.java` y `Bmi160Panel.kt` | **Conservar.** Calibración de cero y pantalla de lecturas separadas para cada mano. |

El nombre `J` no demuestra que un archivo esté sobrando: parte del código se fue compartiendo al ampliar el proyecto. Se conservaron las clases que todavía tienen dependencias, sin cambiarles el nombre para ocultar esa relación.

#### Limpieza realizada

- Se retiraron del APK **10 JSON históricos** de `app/src/main/assets/word_references/`. La práctica ya utiliza las referencias privadas generadas en el teléfono, mediante `WordReferenceStore`. Esos JSON no participaban en el flujo actual.
- Se retiró `app/src/main/res/raw/hola.mp4`, sin llamadas desde código, catálogo o layouts. El video vigente de Hola es `s_hola.mp4`, que se conserva.
- Se retiraron del cliente BLE los accesos antiguos limitados a la mano derecha, el listado de dispositivos emparejados y `sendCommand`, que no tenía llamadas de producción y siempre lanzaba un error con el firmware actual.
- Se sustituyó el nombre de datos `PairedBluetoothDevice` por `BleSensorDevice`, ya que la búsqueda BLE no requiere emparejamiento.
- Se eliminó el parámetro de permisos que `Bmi160Panel` ignoraba y el lanzador duplicado en `MainActivity`. El panel conserva su registro de permisos BLE para búsqueda y conexión.
- Se reemplazaron las pruebas antiguas de sockets RFCOMM, líneas `IMU1` y comandos por pruebas de GATT y paquetes BIN20.
- Los verificadores antiguos conservan su entrada de compatibilidad, pero dirigen al verificador vigente para evitar resultados distintos entre scripts. La exportación histórica de escritorio ya no vuelve a escribir JSON sin uso dentro de `app/assets`.
- Bluetooth quedó como característica opcional del manifiesto, coherente con el uso de la cámara sin sensores.

Los 11 archivos retirados suman **514,713 bytes** sin comprimir. El detalle de rutas, tamaños y huellas está en `tools/limpieza_1_11.json`. Los videos actuales, el modelo de MediaPipe, las imágenes del abecedario, las guías y las herramientas vigentes se conservaron. La entrega excluye salidas de compilación y archivos locales generados por el entorno de Android Studio; se crean de nuevo al abrir y compilar.

### Compatibilidad con tu Arduino actual

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

### Mapa de los archivos principales conservados

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

### Pruebas y límites

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

### Abrir y comprobar en tu teléfono

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