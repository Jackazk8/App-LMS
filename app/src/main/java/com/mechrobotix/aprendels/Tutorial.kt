package com.mechrobotix.aprendels

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import androidx.annotation.DrawableRes
import com.mechrobotix.aprendels.databinding.DialogTipBinding
import com.mechrobotix.aprendels.databinding.ItemTipRowBinding

/** Tutorial de bienvenida (se muestra una vez) y consejos que aparecen la primera vez en cada práctica. */
object Tutorial {
    private const val PREFS="tutorial"
    private const val KEY_ONBOARDING="onboarding_done"

    private fun prefs(c:Context)=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE)

    fun isDone(c:Context)=prefs(c).getBoolean(KEY_ONBOARDING,false)
    fun markDone(c:Context){prefs(c).edit().putBoolean(KEY_ONBOARDING,true).apply()}
    fun start(c:Context){c.startActivity(Intent(c,OnboardingActivity::class.java))}

    /** Consejos de la práctica de palabras (cámara + video de ejemplo). */
    fun wordPracticeTip(a:Activity)=showOnce(a,"word_practice",R.drawable.ic_hand_wave,"Cómo practicar una palabra",listOf(
        "Arriba a la derecha está el video de ejemplo. Toca «Repetir ejemplo» para verlo otra vez.",
        "Colócate para que la cámara vea tus manos y brazos completos, con buena luz.",
        "La barra se llena mientras haces bien la seña. Al lograrla, la app pasa sola a la siguiente."
    ))

    /** Consejos de la práctica del abecedario. */
    fun letterPracticeTip(a:Activity)=showOnce(a,"letter_practice",R.drawable.ic_abc,"Cómo practicar el abecedario",listOf(
        "Toca la imagen de referencia para verla en grande.",
        "Pon tu mano frente a la cámara, con buena luz, y copia la seña.",
        "La barra de arriba se llena cuando tu seña se parece a la guía. Al lograrlo, toca «Siguiente».",
        "«Modo espejo» voltea la guía para que la copies como en un espejo."
    ))

    private fun showOnce(a:Activity,key:String,@DrawableRes icon:Int,title:String,tips:List<String>) {
        val p=prefs(a)
        if(p.getBoolean("tip_$key",false) || a.isFinishing || a.isDestroyed)return
        p.edit().putBoolean("tip_$key",true).apply() // se marca al mostrarse para no repetirlo
        val d=DialogTipBinding.inflate(a.layoutInflater)
        d.tipIcon.setImageResource(icon)
        d.tipTitle.text=title
        tips.forEachIndexed{ i,line ->
            val row=ItemTipRowBinding.inflate(a.layoutInflater,d.tipList,false)
            row.tipNumber.text=(i+1).toString()
            row.tipText.text=line
            d.tipList.addView(row.root)
        }
        AppUi.style(d.tipOk)
        Fonts.apply(d.root)
        val dialog=Dialog(a).apply{
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(d.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width=minOf((a.resources.displayMetrics.widthPixels*0.9f).toInt(),AppUi.dp(a,440))
            window?.setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT)
        }
        d.tipOk.setOnClickListener{dialog.dismiss()}
        dialog.show()
    }
}
