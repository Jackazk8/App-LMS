package com.mechrobotix.aprendels

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.mechrobotix.aprendels.databinding.ActivityLearningModulesBinding
import com.mechrobotix.aprendels.databinding.ItemModuleCardBinding

class LearningModulesActivity:ComponentActivity() {
    /** Ícono, degradado y frase de cada módulo. Los módulos nuevos del JSON usan el aspecto por omisión. */
    private data class Look(val icon:Int,val from:Int,val to:Int,val blurb:String)

    private fun look(id:String):Look = when {
        id.contains("salud") -> Look(R.drawable.ic_hand_wave,R.color.mod_greet_a,R.color.mod_greet_b,"Saluda y despídete")
        id.contains("famil") -> Look(R.drawable.ic_family,R.color.mod_family_a,R.color.mod_family_b,"Las personas que quieres")
        id.contains("color") -> Look(R.drawable.ic_palette,R.color.mod_colors_a,R.color.mod_colors_b,"Dale color a tu vocabulario")
        id.contains("numer") -> Look(R.drawable.ic_numbers,R.color.mod_numbers_a,R.color.mod_numbers_b,"Cuenta con señas")
        else -> Look(R.drawable.ic_book,R.color.mod_default_a,R.color.mod_default_b,"Aprende nuevas señas")
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val b=ActivityLearningModulesBinding.inflate(layoutInflater);setContentView(b.root);AppUi.insets(b.root)
        b.btnBack.setOnClickListener{finish()}
        var shown=0
        fun card(look:Look,title:String,chip:String,enabled:Boolean=true,action:()->Unit) {
            val c=ItemModuleCardBinding.inflate(layoutInflater,b.moduleList,false)
            val from=ContextCompat.getColor(this,if(enabled)look.from else R.color.mod_locked_a)
            val to=ContextCompat.getColor(this,if(enabled)look.to else R.color.mod_locked_b)
            val root=c.cardRoot
            root.background=AppUi.gradient(this,from,to,28)
            root.clipToOutline=true
            if(Build.VERSION.SDK_INT>=28) { root.outlineSpotShadowColor=from;root.outlineAmbientShadowColor=from } // sombra de color
            c.moduleIcon.setImageResource(look.icon)
            c.moduleBadge.text=chip
            c.moduleTitle.text=title
            c.moduleSubtitle.text=look.blurb
            c.moduleChevron.setImageResource(if(enabled)R.drawable.ic_chevron else R.drawable.ic_lock)
            c.moduleChevron.imageTintList=ColorStateList.valueOf(from)
            if(enabled) {
                root.setOnClickListener{action()}
                // Al tocar, la tarjeta se "hunde" un poco.
                root.setOnTouchListener{ v,e ->
                    when(e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> v.animate().setStartDelay(0).scaleX(.97f).scaleY(.97f).setDuration(90).start()
                        MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> v.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(140).start()
                    }
                    false
                }
            } else {
                root.isClickable=false;root.isFocusable=false;root.alpha=.92f
            }
            b.moduleList.addView(root)
            // Entrada escalonada: las tarjetas suben y aparecen una tras otra.
            val finalAlpha=root.alpha
            root.alpha=0f
            root.translationY=AppUi.dp(this,28).toFloat()
            root.animate().alpha(finalAlpha).translationY(0f).setStartDelay(shown*80L).setDuration(360).start()
            shown++
        }
        card(Look(R.drawable.ic_abc,R.color.mod_alpha_a,R.color.mod_alpha_b,"Practica cada letra con la cámara"),"Abecedario","Letras"){
            startActivity(Intent(this,PracticeSignActivity::class.java))
        }
        try {
            WordCatalog.load(this).forEach{module ->
                val n=module.lessons.size
                val chip=when {
                    n==0 -> "Próximamente"
                    n==1 -> "1 palabra"
                    else -> "$n palabras"
                }
                card(look(module.id),module.title,chip,enabled=n>0){
                    startActivity(Intent(this,WordPracticeActivity::class.java).putExtra("module_id",module.id))
                }
            }
        }catch(e:Exception){b.moduleList.addView(TextView(this).apply{text="No se pudo cargar el catálogo: ${e.message}"})}
        Fonts.apply(b.root)
    }
}
