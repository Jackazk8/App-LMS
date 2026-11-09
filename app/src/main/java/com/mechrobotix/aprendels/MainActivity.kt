package com.mechrobotix.aprendels

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import com.mechrobotix.aprendels.databinding.ActivityMainBinding

class MainActivity:ComponentActivity() {
    private lateinit var b:ActivityMainBinding
    private lateinit var sensors:Bmi160Panel
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState);b=ActivityMainBinding.inflate(layoutInflater);setContentView(b.root);AppUi.insets(b.root)
        sensors=Bmi160Panel(this)
        sensors.attachInline(b.sensorContainer)
        b.btnLearn.setOnClickListener { startActivity(Intent(this,LearningModulesActivity::class.java)) }
        b.btnTutorial.setOnClickListener { Tutorial.start(this) }
        AppUi.styleButtons(b.root)
        Fonts.apply(b.root)
        // Primera vez: muestra el tutorial (savedInstanceState==null evita repetirlo al rotar la pantalla).
        if(savedInstanceState==null && !Tutorial.isDone(this)) Tutorial.start(this)
    }
    override fun onResume() { super.onResume();sensors.resume();waveHand() }
    override fun onPause() { sensors.pause();wave?.cancel();b.heroHand.rotation=0f;super.onPause() }

    /** La mano de la cabecera saluda un par de veces al abrir la pantalla. */
    private var wave:ObjectAnimator?=null
    private fun waveHand() {
        wave?.cancel()
        b.heroHand.post {
            b.heroHand.pivotX=b.heroHand.width*.5f;b.heroHand.pivotY=b.heroHand.height*.9f
            wave=ObjectAnimator.ofFloat(b.heroHand,View.ROTATION,-14f,14f).apply {
                duration=360;repeatCount=7;repeatMode=ValueAnimator.REVERSE;start()
            }
        }
    }
    override fun onDestroy() { sensors.destroy();super.onDestroy() }
}
