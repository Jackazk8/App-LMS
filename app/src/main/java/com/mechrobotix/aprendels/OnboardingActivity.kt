package com.mechrobotix.aprendels

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.mechrobotix.aprendels.databinding.ActivityOnboardingBinding
import kotlin.math.abs

/** Tutorial de bienvenida: se muestra la primera vez y se puede repetir desde "Ver tutorial". */
class OnboardingActivity:ComponentActivity() {
    private data class Page(val icon:Int,val title:String,val text:String,val top:Int,val bottom:Int)
    private val pages=listOf(
        Page(R.drawable.ic_hand_wave,"¡Bienvenido!","Aprende Lengua de Señas Mexicana practicando con tu cámara, a tu ritmo y sin presión.",R.color.mod_alpha_a,R.color.mod_family_b),
        Page(R.drawable.ic_book,"Elige un tema","Empieza con el abecedario o con palabras de saludos, familia y colores. Cada tema reúne las señas que necesitas.",R.color.mod_family_b,R.color.mod_family_a),
        Page(R.drawable.ic_play,"Mira el ejemplo","Cada seña tiene un video de ejemplo que se repite solo. Puedes verlo otra vez cuando quieras.",R.color.mod_family_a,R.color.mod_colors_a),
        Page(R.drawable.ic_camera,"Practica con la cámara","Colócate donde la cámara vea tus manos y brazos completos, con buena luz. La barra se llena cuando tu seña coincide.",R.color.mod_greet_a,R.color.mod_greet_b),
        Page(R.drawable.ic_check,"Avanza paso a paso","Cuando lo logras, pasas a la siguiente seña. Si no sale, repite el ejemplo e inténtalo de nuevo.",R.color.mod_colors_a,R.color.mod_colors_b),
        Page(R.drawable.ic_bluetooth,"Sensores opcionales","Si tienes los ESP32, conéctalos desde el inicio para ver las lecturas de cada mano. No son necesarios para practicar.",R.color.mod_numbers_a,R.color.mod_numbers_b)
    )
    private lateinit var b:ActivityOnboardingBinding
    private val background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.BLACK,Color.BLACK))
    private var shownTop=0
    private var shownBottom=0
    private var page=0
    private var colorAnimator:ValueAnimator?=null

    private val gestures by lazy {
        GestureDetector(this,object:GestureDetector.SimpleOnGestureListener(){
            override fun onFling(e1:MotionEvent?,e2:MotionEvent,velocityX:Float,velocityY:Float):Boolean {
                if(e1==null)return false
                val dx=e2.x-e1.x;val dy=e2.y-e1.y
                if(abs(dx)>AppUi.dp(this@OnboardingActivity,80) && abs(dx)>abs(dy)*1.5f) {
                    if(dx<0)next() else previous()
                    return true
                }
                return false
            }
        })
    }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        // Pantalla completa de borde a borde: el degradado cubre también la barra de estado.
        WindowCompat.setDecorFitsSystemWindows(window,false)
        b=ActivityOnboardingBinding.inflate(layoutInflater);setContentView(b.root);AppUi.insets(b.onbColumn)
        val controller=WindowCompat.getInsetsController(window,b.root)
        controller.isAppearanceLightStatusBars=false
        controller.isAppearanceLightNavigationBars=false
        window.statusBarColor=Color.TRANSPARENT
        window.navigationBarColor=Color.TRANSPARENT
        b.onbRoot.background=background
        page=(savedInstanceState?.getInt("page") ?: 0).coerceIn(0,pages.lastIndex)
        AppUi.styleLight(b.onbNext)
        AppUi.styleGlass(b.onbPrev)
        b.onbNext.setOnClickListener{next()}
        b.onbPrev.setOnClickListener{previous()}
        b.onbSkip.setOnClickListener{finishTutorial()}
        onBackPressedDispatcher.addCallback(this){finishTutorial()}
        Fonts.apply(b.root)
        render(animate=false,forward=true)
    }

    override fun onSaveInstanceState(outState:Bundle) {
        super.onSaveInstanceState(outState);outState.putInt("page",page)
    }

    override fun onDestroy() {
        colorAnimator?.cancel();super.onDestroy()
    }

    override fun dispatchTouchEvent(ev:MotionEvent?):Boolean {
        if(ev!=null)gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun next() {
        if(page>=pages.lastIndex){finishTutorial();return}
        page++;render(animate=true,forward=true)
    }
    private fun previous() {
        if(page<=0)return
        page--;render(animate=true,forward=false)
    }
    private fun finishTutorial() {
        Tutorial.markDone(this);finish()
    }

    /** Cambia suavemente el degradado de fondo hacia los colores de la página actual. */
    private fun fadeBackground(top:Int,bottom:Int,animate:Boolean) {
        colorAnimator?.cancel()
        val fromTop=shownTop;val fromBottom=shownBottom
        shownTop=top;shownBottom=bottom
        if(!animate){background.colors=intArrayOf(top,bottom);return}
        val eval=ArgbEvaluator()
        colorAnimator=ValueAnimator.ofFloat(0f,1f).apply {
            duration=400
            addUpdateListener{
                val t=it.animatedFraction
                background.colors=intArrayOf(eval.evaluate(t,fromTop,top) as Int,eval.evaluate(t,fromBottom,bottom) as Int)
            }
            start()
        }
    }

    private fun render(animate:Boolean,forward:Boolean) {
        val p=pages[page]
        fadeBackground(ContextCompat.getColor(this,p.top),ContextCompat.getColor(this,p.bottom),animate)
        val bind={
            b.onbIcon.setImageResource(p.icon)
            b.onbTitle.text=p.title
            b.onbText.text=p.text
            b.onbNext.text=if(page==pages.lastIndex)"¡Empezar!" else "Siguiente"
            b.onbPrev.visibility=if(page==0)View.INVISIBLE else View.VISIBLE
            b.onbSkip.visibility=if(page==pages.lastIndex)View.INVISIBLE else View.VISIBLE
            drawDots()
        }
        if(!animate){bind();return}
        // El contenido sale hacia un lado y el nuevo entra desde el otro.
        val shift=AppUi.dp(this,40)*(if(forward)1 else -1).toFloat()
        b.onbContent.animate().cancel()
        b.onbContent.animate().alpha(0f).translationX(-shift).setDuration(120).withEndAction{
            bind()
            b.onbContent.translationX=shift
            b.onbContent.animate().alpha(1f).translationX(0f).setDuration(200).start()
        }.start()
    }

    private fun drawDots() {
        b.onbDots.removeAllViews()
        val h=AppUi.dp(this,8);val gap=AppUi.dp(this,4)
        val wide=AppUi.dp(this,28);val narrow=AppUi.dp(this,8)
        pages.indices.forEach{ i ->
            val selected=i==page
            val dot=View(this)
            dot.background=GradientDrawable().apply{cornerRadius=h/2f;setColor(if(selected)Color.WHITE else 0x59FFFFFF)}
            b.onbDots.addView(dot,LinearLayout.LayoutParams(if(selected)wide else narrow,h).apply{marginStart=gap;marginEnd=gap})
        }
    }
}
