package com.mechrobotix.aprendels

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/**
 * Tipografía de la app (Poppins, licencia OFL). Llama a Fonts.apply(vista) después de inflar el layout
 * y después de agregar vistas creadas por código. Los textos con textStyle="bold" usan Poppins Bold,
 * el resto Poppins Regular. Los botones ya estilizados con AppUi usan SemiBold y no se tocan.
 */
object Fonts {
    private var regular:Typeface?=null
    private var semibold:Typeface?=null
    private var bold:Typeface?=null

    private fun load(context:Context,res:Int,fallback:Typeface):Typeface =
        runCatching { ResourcesCompat.getFont(context.applicationContext,res) }.getOrNull() ?: fallback

    fun regular(context:Context):Typeface=regular ?: load(context,R.font.poppins_regular,Typeface.DEFAULT).also { regular=it }
    fun semibold(context:Context):Typeface=semibold ?: load(context,R.font.poppins_semibold,Typeface.DEFAULT_BOLD).also { semibold=it }
    fun bold(context:Context):Typeface=bold ?: load(context,R.font.poppins_bold,Typeface.DEFAULT_BOLD).also { bold=it }

    fun apply(root:View) {
        val c=root.context
        val r=regular(c);val s=semibold(c);val b=bold(c)
        fun visit(v:View) {
            if(v is TextView) {
                val current=v.typeface
                if(current!==r && current!==s && current!==b) v.typeface=if(current?.isBold==true) b else r
            }
            if(v is ViewGroup) for(i in 0 until v.childCount) visit(v.getChildAt(i))
        }
        visit(root)
    }
}
