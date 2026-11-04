# Ajustes de validación de Saludos y Familia — versión 1.10

## Qué quedó corregido

Se revisaron los 33 videos actuales de Familia mediante fotogramas de distintas fases, sus detecciones y la reproducción del motor Java. También se verificaron las 10 lecciones de Saludos y despedidas. Se comprobaron las huellas del video, modelo y detecciones; los datos antiguos se volvieron a generar con los MP4 de esta entrega.

La configuración original reproducía cinco fallos positivos: Familia fallaba en los tres recorridos de prueba; Prima y Tía fallaban al reproducirse más rápido. La configuración corregida pasa las 129 reproducciones completas: 43 ejemplos, cada uno normal, con recorrido 20 % menor y ruido simulado, y con muestreo más rápido. Esto es una comprobación de software sobre los ejemplos disponibles, no una medición de precisión con alumnos ni una certificación lingüística de los videos.

El cierre tiene más margen en la mayoría de palabras, sin cambiar las tolerancias iniciales de forma y flexión. Se conservaron valores más estrictos en palabras que se confunden con otras. También se separó, de forma opcional, la zona donde se sostiene el final del margen utilizado para recorrer la secuencia.

## El archivo que debes editar

El nombre correcto es **`app/src/main/assets/word_lessons.json`**, en plural. Busca el `id` de la palabra y edita sus bloques `tolerance` y `validation`. Cada palabra tiene su propia configuración; no hace falta cambiar los valores globales de `WordTolerance.java`.

**Instala el código de esta entrega junto con el JSON.** La versión anterior solo admitía `finalPoseScale` hasta `1` y desconocía `finalPositionTolerance`. Copiar únicamente el JSON nuevo en el código antiguo puede impedir cargar el catálogo.

| Parámetro | Dónde | Qué cambia y cómo darle más margen |
| --- | --- | --- |
| `finalPoseScale` | `validation` | Multiplica el margen de postura del cierre. Menor que `1` exige más precisión; mayor que `1` admite más variación. Esta versión acepta `0.25–1.5`; la mayoría de palabras quedaron en `1.15`. No elimina la comprobación independiente de dedos distintos. |
| `endHoldMs` | `tolerance` | Tiempo de postura final válida. Quedó en `140` ms, antes `160` o `200`. Admite como mínimo `100`; bajar el valor hace más rápido el cierre. |
| `matchGraceMs` | `validation` | Conserva el tiempo válido acumulado durante fallos breves. Quedó en `400` ms. El intervalo incorrecto no se suma al tiempo de postura válida. |
| `pathTolerance` | `validation` | Error permitido del recorrido, expresado en tamaños de palma. Un valor mayor permite más desviación. `0` desactiva esta comprobación y puede facilitar aprobaciones prematuras. No debe aumentarse igual en todas las palabras. |
| `finalPositionTolerance` | `validation` | Nuevo margen opcional para sostener la posición final **después de haber alcanzado el recorrido**. `0` u omitirlo hereda `pathTolerance`; se usa `0.85` en ciertas palabras de Familia. No se amplió en las tres despedidas porque adelantaba su cierre en las pruebas. |
| `stages` | `validation` | Cantidad de controles visibles, entre `3` y `6`. Tres significa inicio, movimiento y final. Con `pathTolerance > 0` siguen comprobándose los hitos del recorrido, aunque haya menos controles visibles. |
| `minDisplacement` | `validation` | Cambio mínimo observable para aprobar un control de movimiento. Bajarlo ayuda a movimientos cortos. Familia quedó en `0.10`, antes `0.25`. Rango permitido: `0.10–2`. |
| `minMotion` y `motionNoise` | `tolerance` / `validation` | Umbrales para contar cambios separados del detector. En Familia quedaron en `0.04` y `0.03`. Mantener `motionSamples: 2` evita que un único salto de imagen cuente como movimiento continuo. |
| `maxFingerMismatches` | `validation` | Dedos claramente abiertos/cerrados que pueden diferir. Normalmente se conservó en `0`; Nieta conserva el `1` que ya tenía. Subirlo puede admitir una configuración de dedos de otra seña. |
| `sharedRouteOrigin` | `validation` | Hace que ambas manos usen el origen inicial de la mano principal para comparar el recorrido. **No reduce etapas ni desactiva controles.** Se conservó la selección que ya tenía Familia. |

`shape`, `meanFlex` y `maxFlex` afectan la forma y flexión durante las posturas de toda la palabra. Sus valores originales se conservaron. Para un problema únicamente al terminar, conviene empezar por los parámetros del cierre y no aumentar todos los márgenes de la palabra.

### Ejemplo real: Familia

Estos son los bloques completos de `f_familia` en esta entrega:

```json
"tolerance": {
  "shape": 0.85,
  "meanFlex": 60,
  "maxFlex": 115,
  "travel": 2.2,
  "travelRelative": 0.45,
  "minMotionRatio": 0.12,
  "minMotion": 0.04,
  "directionCosine": -0.35,
  "minTravelProgress": 0.18,
  "stability": 0.75,
  "startHoldMs": 120,
  "endHoldMs": 140,
  "lostGraceMs": 2200,
  "minAttemptMs": 600
},
"validation": {
  "stages": 3,
  "motionSamples": 2,
  "minDisplacement": 0.10,
  "motionNoise": 0.03,
  "poseHoldMs": 100,
  "matchGraceMs": 400,
  "maxFingerMismatches": 0,
  "pathTolerance": 0.99,
  "finalPoseScale": 1.15,
  "sharedRouteOrigin": true
}
```

El bloqueo reproducido en Familia era «Desplaza un poco más la mano». El recorrido completo del video no alcanzaba el mínimo de `0.25` exigido por ese control. Bajar solo `finalPoseScale` no podía resolverlo: el motor todavía estaba en movimiento y no había llegado al cierre.

### Cómo interpretar el mensaje cuando se queda en el final

| Mensaje | Qué está pendiente | Qué revisar |
| --- | --- | --- |
| «Completa el último recorrido antes de mantener el final» | Falta un hito anterior del recorrido. | Ejecución completa y `pathTolerance`; no lo resuelve mantener la mano quieta más tiempo. |
| «Acerca las manos a la zona final del video» | El recorrido ya se alcanzó, pero la posición actual se alejó de la zona final. | `finalPositionTolerance`, cuando esté configurado; de lo contrario, `pathTolerance`. |
| «Lleva la mano a la zona final del video» | No coincide la región/dirección respecto al control anterior. | `travel`, `travelRelative`, `directionCosine` y `minTravelProgress`. Estos valores se conservaron. |
| «Ajusta la posición final de dedos y manos» | No coincide la postura final. | `finalPoseScale` y posibles oclusiones; `maxFingerMismatches` también puede bloquear aunque el margen de forma sea amplio. |
| «Bien, mantén un instante el final» | La postura sí coincide; falta sostenerla o cumplir el tiempo mínimo de intento. | `endHoldMs`, `matchGraceMs` y `minAttemptMs`. |
| «Recupera la mano que estaba participando» | El detector perdió una mano requerida. | Encuadre y superposición de manos. Una tolerancia de postura mayor no reconstruye una mano que no se detectó. |

## Revisión de Familia, video por video

“Pruebas” indica cuántas de tres variantes completas terminaron con éxito, antes y después. “Cierre” indica `finalPoseScale`. Las observaciones describen lo que aparece en los videos y los bloqueos reproducidos; no afirman que todas esas señas fallaran con tu teléfono.

| Video | Observación y ajuste | Pruebas antes → ahora | Etapas antes → ahora | Cierre antes → ahora |
| --- | --- | --- | --- | --- |
| Familia (`f_familia`) | Movimiento corto con brazos cruzados. Fallaba el mínimo de desplazamiento; se bajó a 0.10 y el ruido a 0.03. | 0/3 → 3/3 | 3 → 3 | 0.9 → 1.15 |
| Hombre (`f_hombre`) | Mano junto al mentón. Se conserva el margen de postura para no añadir confusiones; cierre temporal más breve. | 3/3 → 3/3 | 3 → 3 | 0.9 → 0.9 |
| Mujer (`f_mujer`) | Índice cerca del rostro. Se conserva el margen de postura; cierre temporal más breve. | 3/3 → 3/3 | 4 → 4 | 0.9 → 0.9 |
| Niño (`f_nino`) | Movimiento corto junto al mentón. Se conserva la recuperación de hitos y el mínimo de 0.15; cierre más flexible. | 3/3 → 3/3 | 3 → 3 | 0.9 → 1 |
| Niña (`f_nina`) | Secuencia junto al mentón y salida a palma baja. Se mantienen sus cinco controles. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Joven (`f_joven`) | Dos manos frente al pecho con cambios repetidos. Se conserva el recorrido y sus tres controles. | 3/3 → 3/3 | 3 → 3 | 0.99 → 1.15 |
| Compañero (`f_companero`) | Mano activa sobre el antebrazo opuesto; superposición durante el giro. Más margen de cierre. | 3/3 → 3/3 | 4 → 4 | 0.9 → 1.15 |
| Compañera (`f_companera`) | Secuencia sobre el antebrazo y salida a palma baja. Se mantienen cinco controles. | 3/3 → 3/3 | 5 → 5 | 0.9 → 1.15 |
| Papá (`f_papa`) | Dos dedos junto al rostro y movimiento pequeño. Se conserva el perfil de recuperación y el mínimo de 0.10. | 3/3 → 3/3 | 3 → 3 | 0.9 → 0.95 |
| Mamá (`f_mama`) | Desplazamiento hacia el rostro. Más margen de recorrido, conservando el cierre espacial original. | 3/3 → 3/3 | 3 → 3 | 0.9 → 1.15 |
| Esposo (`f_esposo`) | Dos manos se superponen al cerrar. Se conserva recuperación de hitos; el factor final sube desde 0.80. | 3/3 → 3/3 | 4 → 4 | 0.8 → 1.15 |
| Esposa (`f_esposa`) | Superposición de manos y salida posterior a palma baja. Se conservan cuatro controles. | 3/3 → 3/3 | 4 → 4 | 1 → 1.15 |
| Novio (`f_novio`) | Cambios pequeños de dedos y muñeca, con repeticiones. Más margen para sostener la zona final. | 3/3 → 3/3 | 5 → 5 | 0.9 → 1.15 |
| Novia (`f_novia`) | Secuencia de dedos seguida de palma baja. Se amplía recorrido y zona final. | 3/3 → 3/3 | 5 → 5 | 1 → 1.15 |
| Hijo (`f_hijo`) | Cambio de postura sobre el pecho. El factor final se limita a 1.10 para no añadir cruces. | 3/3 → 3/3 | 5 → 5 | 0.9 → 1.1 |
| Hija (`f_hija`) | Postura sobre el pecho y salida a palma baja. Se conserva la secuencia de cinco controles. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Hermano (`f_hermano`) | Ambas manos juntas, con dedos que se superponen. Más margen de recorrido y de zona final. | 3/3 → 3/3 | 5 → 5 | 0.9 → 1.15 |
| Hermana (`f_hermana`) | Ambas manos juntas y posterior salida a palma baja. Más margen para sostener el final. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Abuelo (`f_abuelo`) | Dos manos con acercamientos repetidos. Se mantienen cinco controles y el recorrido. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Abuela (`f_abuela`) | Secuencia de ambas manos y salida final a palma baja. Se conservan seis controles. | 3/3 → 3/3 | 6 → 6 | 1 → 1.15 |
| Nieto (`f_nieto`) | Dos manos apuntan hacia abajo y se acercan. El cierre original 0.70 era especialmente estricto. | 3/3 → 3/3 | 6 → 6 | 0.7 → 1.15 |
| Nieta (`f_nieta`) | Secuencia de dos manos seguida de palma baja. Se conserva el permiso original de un dedo distinto. | 3/3 → 3/3 | 5 → 5 | 1 → 1.15 |
| Tío (`f_tio`) | Cambios pequeños de dedos con ambas manos frente al cuerpo. Más margen para sostener el final. | 3/3 → 3/3 | 5 → 5 | 1 → 1.15 |
| Tía (`f_tia`) | El control de postura intermedia se bloqueaba al perder una mano en la variante rápida. Tres controles y recorrido conservado. | 2/3 → 3/3 | 5 → 3 | 0.99 → 1.15 |
| Sobrino (`f_sobrino`) | Dos manos con movimientos cortos y repetidos. Más margen para sostener la zona final. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Sobrina (`f_sobrina`) | Secuencia de dos manos y salida a palma baja. Se mantienen cinco controles. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Primo (`f_primo`) | Dos manos se acercan y separan con cambios de dedos. Se conservan los cinco controles. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Prima (`f_prima`) | La variante rápida perdía la continuidad de controles y regresaba al inicio. Tres controles, manteniendo los hitos del recorrido. | 2/3 → 3/3 | 5 → 3 | 0.99 → 1.15 |
| Yerno (`f_yerno`) | Ambas manos bajan y cambian de orientación. Se conservan tres controles; zona final más amplia. | 3/3 → 3/3 | 3 → 3 | 0.9 → 1.15 |
| Nuera (`f_nuera`) | Movimiento corto de ambas manos hacia abajo. Más margen de recorrido y de zona final. | 3/3 → 3/3 | 3 → 3 | 0.9 → 1.15 |
| Cuñado (`f_cunado`) | Video corto con cambio de orientación de ambas manos. Se conservan tres controles; más margen de recorrido. | 3/3 → 3/3 | 3 → 3 | 0.9 → 1.15 |
| Cuñada (`f_cunada`) | Cambio de orientación seguido de palma baja. Se amplía el recorrido original 0.40 a 0.65. | 3/3 → 3/3 | 5 → 5 | 0.99 → 1.15 |
| Divorciado (`f_divorciado`) | Dos manos se separan hacia los lados. Se conserva el recorrido y se amplía la zona de sostén final. | 3/3 → 3/3 | 3 → 3 | 0.9 → 1.15 |

En todas las filas el cierre se sostiene `140` ms y `matchGraceMs` vale `400`. Hombre y Mujer conservan `finalPoseScale: 0.9`: ampliarlo a `1.15` aumentaba las confusiones. Niño quedó en `1.0`, Papá en `0.95` e Hijo en `1.1` por el mismo criterio. Esas palabras sí reciben el ajuste temporal del cierre.

Se amplió `pathTolerance` a `0.65` en Mamá, Novia, Hermano, Nuera, Cuñado y Cuñada. Los demás valores de Familia conservaron su margen de recorrido original.

Se añadió `finalPositionTolerance: 0.85` en Novio, Novia, Hermano, Hermana, Nieto, Tío, Sobrino, Sobrina, Yerno, Nuera, Cuñado, Cuñada y Divorciado. Así se admite más desplazamiento al sostener el final, después de recorrer los hitos. No se amplió en Niño, Papá, Mamá ni Hijo porque generaba nuevos cruces en las pruebas.

## Saludos y despedidas

Las diez palabras reciben `endHoldMs: 140` y `matchGraceMs: 400`. Todas tienen `finalPoseScale: 1.15`, excepto Yo bien, que queda en `1.0` para no añadir confusiones. Se conservan los controles visibles originales: Hola tiene 3; Yo bien y Yo mal, 4; Cómo estás y los tres saludos de hora, 5; las tres despedidas, 6.

En las despedidas, un recorrido muy amplio permitía llegar al estado de éxito sin terminar el último movimiento. Se corrigió el margen de recorrido y se amplió por separado el de postura:

| Video | `pathTolerance` antes → ahora | `finalPoseScale` antes → ahora | Interrupciones rechazadas |
| --- | --- | --- | --- |
| `d_nos_vemos_pronto` | 0.95 → 0.40 | 0.95 → 1.15 | 11/11 |
| `d_nos_vemos_manana` | 0.35 → 0.35 | 0.70 → 1.15 | 9/9 |
| `d_nos_vemos_proxima` | 0.90 → 0.50 | 0.95 → 1.15 | 10/10 |

No conviene copiar `pathTolerance: 0.99` a estas tres despedidas: en los ejemplos analizados se consumían hitos del final antes de terminar. Un margen amplio de postura final y un margen menor para el historial de recorrido cumplen funciones distintas.

## Reemplazar un video con el mismo nombre

**Sí: Saludos ya tenía el mismo remapeo automático por contenido que Familia.** Ambos módulos usan `WordPracticeActivity` → `WordReferenceStore.loadOrGenerate()` → `WordVideoReferenceGenerator`.

1. Sustituye, por ejemplo, `app/src/main/res/raw/s_hola.mp4`, conservando el nombre y la entrada del catálogo.
2. Compila e instala la actualización de la app. Cambiar el MP4 en la computadora no modifica el que ya está instalado en el teléfono.
3. Abre esa palabra. Se calcula el SHA-256 del contenido instalado. Si cambió, se prepara automáticamente una nueva referencia para ese video, aunque su nombre sea idéntico.
4. Las otras palabras conservan sus referencias. Las siguientes aperturas reutilizan la nueva referencia.

La identidad también tiene en cuenta el modelo, la versión del generador y el perfil de recuperación de movimiento corto. Los cambios de etapas o tolerancias se aplican al crear el motor de práctica y no requieren volver a detectar un video cuyo contenido no cambió.

No hace falta borrar datos ni desinstalar: instala sobre la versión anterior con el mismo identificador y firma. Si el nuevo video pierde las manos demasiado tiempo, está cortado o no permite extraer movimiento suficiente, se mostrará el error correspondiente en lugar de reutilizar silenciosamente la referencia anterior.

Se añadió una prueba Android que comprueba el reemplazo manteniendo el nombre en **ambos módulos**, además de la prueba existente de caché, corrupción y cancelación. Esa prueba Android necesita un teléfono o emulador; no pudo ejecutarse en este entorno.

## Archivos principales modificados

| Archivo | Función del cambio |
| --- | --- |
| `app/src/main/assets/word_lessons.json` | Ajustes por palabra de ambos módulos. |
| `app/src/main/java/com/mechrobotix/aprendels/WordStageOptions.java` | Admite `finalPoseScale` hasta 1.5 y el margen opcional `finalPositionTolerance`. |
| `app/src/main/java/com/mechrobotix/aprendels/WordCatalog.kt` | Lee y admite el nuevo parámetro del JSON. |
| `app/src/main/java/com/mechrobotix/aprendels/WordPracticeEngine.java` | Aplica el margen de zona final después del recorrido y distingue mejor los mensajes de cierre. |
| `app/src/androidTest/java/com/mechrobotix/aprendels/WordAutoReferenceTest.kt` | Añade cobertura del reemplazo con igual nombre en Saludos y Familia. |
| `app/build.gradle.kts` | Versión 1.10, código 11. |
| `tools/WordFinalToleranceCheck.java` | Prueba el margen final, la zona final, los saltos de recorrido y límites de configuración. |
| `tools/WordModuleCheck.java`, `tools/verificar_modulos.py` | Ejecutan también los controles de cierre y la nueva prueba. |
| `tools/fixtures_auto/` | Detecciones y huellas actualizadas para los videos que cambiaron. |

`WordReferenceStore.kt` y `WordVideoReferenceGenerator.kt` conservan la implementación automática existente; no hacía falta duplicarla ni crear otra para Saludos. No se editaron los MP4 ni el modelo de manos.

## Resultados y límites de la comprobación

- 129/129 reproducciones completas aprobadas: normal, recorrido reducido con ruido y muestreo más rápido.
- 43/43 manos inmóviles rechazadas y 43/43 saltos directos del inicio al final rechazados.
- 30/30 interrupciones de despedidas rechazadas. También se probó conservar esos controles al cambiar las etapas visibles de 3 a 6.
- Pasaron los controles del margen final, la zona final, los valores fuera de rango y el avance automático.
- En 1,806 cruces entre videos distintos siguen existiendo 98 aprobaciones, exactamente los mismos pares que en la configuración original. Dentro de Familia son 74/1,056 y dentro de Saludos 5/90; otros 19 cruces son entre módulos. Tampoco se corrigieron todas las secuencias invertidas que el motor original admitía. Estos casos limitan su uso como evaluación estricta.
- Las variantes son simuladas a partir de detecciones de los videos disponibles. Falta probar personas diferentes, manos, iluminación, velocidades reales y cámara frontal en el teléfono.
- Se intentó compilar Android, pero la descarga de Gradle 9.7.1 quedó bloqueada por la red del entorno. **Esta entrega contiene código y resultados de pruebas del motor; no contiene un APK compilado ni una validación Android ejecutada.**

Archivos de evidencia: `tools/resultados_validacion_1_10.txt`, `tools/resultados_antes_1_10.txt`, `tools/cambios_validacion_1_10.json` y `tools/compilacion_android_1_10.txt`. El catálogo anterior se conserva en `tools/catalogo_antes_1_10.json` para comparar tus ajustes.

Para repetir las pruebas del motor con Python 3 y JDK 17 o superior:

```bash
python tools/verificar_modulos.py
```

Si sustituyes videos y quieres repetir el análisis en la computadora:

```bash
python -m pip install -r tools/requirements-palabras.txt
python tools/verificar_modulos.py --reanalyze
```

La app prepara las referencias por sí misma; esos comandos son para verificar en escritorio.

## Abrir y probar la entrega

Descomprime el proyecto en una carpeta nueva, ábrelo en Android Studio y selecciona tu SDK local. Sincroniza Gradle y ejecuta en tu teléfono. Empieza probando Familia, Prima, Tía, Papá, Niño y las tres despedidas. Repite cada una varias veces y comprueba tanto una ejecución completa como una interrumpida. Si una palabra sigue bloqueándose, el mensaje de la tabla anterior permite identificar qué margen está pendiente sin aumentar todos los parámetros.
