package com.mechrobotix.aprendels

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CancellationException

/** Ejecutar en un teléfono/emulador: incluye decodificador Android, MediaPipe y caché real. */
class WordAutoReferenceTest {
    @Test fun phaseProfileIsEnabledOnlyForColors() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val modules=WordCatalog.load(context)
        val colors=modules.single{it.id=="colores"}.lessons
        assertTrue(colors.isNotEmpty())
        assertTrue(colors.all{it.validation.phaseValidation && it.validation.stages in 3..4})
        assertTrue(modules.filter{it.id!="colores"}.flatMap{it.lessons}.none{it.validation.phaseValidation})
    }
    @Test fun sameNameReplacementInAllWordModules() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val modules=WordCatalog.load(context)
        val folder=File(context.noBackupFilesDir,"word_references_auto")
        // Los tres módulos llegan al mismo almacén. Los alias no tocan las cachés del alumno.
        for((id,firstId,replacementId) in listOf(
            Triple("saludos_despedidas","s_hola","s_yo_bien"),
            Triple("familia","f_papa","f_yerno"),
            Triple("colores","c_color","c_rojo"))) {
            val module=modules.single{it.id==id}
            val name="test_replacement_$id"
            val cache=File(folder,"$name.json")
            try {
                val lesson=module.lessons.single{it.id==firstId}.copy(videoName=name)
                WordReferenceStore.loadOrGenerate(context,lesson)
                val original=cache.readBytes()
                WordReferenceStore.loadOrGenerate(context,lesson,onProgress={fail("Regeneró sin cambiar $id")})
                assertArrayEquals(original,cache.readBytes())
                var progress=0
                WordReferenceStore.loadOrGenerate(context,
                    lesson.copy(video=module.lessons.single{it.id==replacementId}.video),
                    onProgress={progress++})
                assertTrue("No detectó el reemplazo en $id",progress>0)
                assertFalse(original.contentEquals(cache.readBytes()))
            } finally {
                listOf(".json",".json.bak",".json.new").forEach{File(folder,name+it).delete()}
            }
        }
    }
    @Test fun cacheReplacementCorruptionAndCancellation() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val folder=File(context.noBackupFilesDir,"word_references_auto")
        val name="test_auto_cache"
        val other="test_auto_untouched"
        val cache=File(folder,"$name.json")
        val untouched=File(folder,"$other.json")
        val lesson=WordLesson(name,"Prueba",name,R.raw.s_hola,WordTolerance())
        fun clear() {listOf(name,other).forEach{n->listOf(".json",".json.bak",".json.new").forEach{File(folder,n+it).delete()}}}
        clear()
        try {
            var progress=0
            val first=WordReferenceStore.loadOrGenerate(context,lesson,onProgress={progress++})
            assertTrue(progress>0);assertTrue(first.keys.size>=4)
            val oldBytes=cache.readBytes()
            val cached=WordReferenceStore.loadOrGenerate(context,lesson,onProgress={fail("Se regeneró sin cambiar el video")})
            assertEquals(first.startMs,cached.startMs);assertArrayEquals(oldBytes,cache.readBytes())
            WordReferenceStore.loadOrGenerate(context,lesson.copy(videoName=other))
            val otherBytes=untouched.readBytes()
            val replaced=lesson.copy(video=R.raw.s_yo_bien) // Mismo nombre, contenido distinto.
            progress=0
            WordReferenceStore.loadOrGenerate(context,replaced,onProgress={progress++})
            assertTrue(progress>0);assertFalse(oldBytes.contentEquals(cache.readBytes()))
            assertArrayEquals(otherBytes,untouched.readBytes())
            // Cambiar el perfil invalida solo esta referencia; el resto sigue intacto.
            progress=0
            val fine=replaced.copy(recoverSparseMotion=true)
            WordReferenceStore.loadOrGenerate(context,fine,onProgress={progress++})
            assertTrue(progress>0)
            WordReferenceStore.loadOrGenerate(context,fine,onProgress={fail("No reutilizó el perfil fino")})
            assertArrayEquals(otherBytes,untouched.readBytes())
            val validBytes=cache.readBytes()
            try {
                var cancel=false
                WordReferenceStore.loadOrGenerate(context,lesson,cancelled={cancel},onProgress={cancel=true})
                fail("La generación no se canceló")
            }catch(_:CancellationException){}
            assertArrayEquals(validBytes,cache.readBytes())
            cache.writeText("JSON incompleto")
            progress=0
            WordReferenceStore.loadOrGenerate(context,replaced,onProgress={progress++})
            assertTrue(progress>0)
        }finally{clear()}
    }
}
