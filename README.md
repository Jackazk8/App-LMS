# Aprende Lengua de Señas Mexicana — 1.13

Colores ahora requiere completar el recorrido de referencia antes de validar la postura final. La tolerancia es amplia y admite variaciones de velocidad; no exige copiar cada fotograma. Dorado usa tres fases porque su mano de apoyo aparece y desaparece en la referencia.

- Revisión vigente: `GUIA_COLORES_Y_REVISION_1_13.md`.
- Catálogo editable: `app/src/main/assets/word_lessons.json`.
- Motor compartido: `WordPracticeEngine.java`.

El cambio de ruta se activa únicamente con `phaseValidation` en Colores. Familia mantiene sus tolerancias y reglas. Se conserva el avance automático a los 3 segundos después de un acierto.

La revisión 1.12 contiene resultados de pruebas de la versión previa; no son resultados de la 1.13. Esta actualización no se probó en cámara real ni se ejecutaron las suites del proyecto. Instala el código y el JSON juntos y prueba Colores, en particular Dorado, antes de distribuir la app.
