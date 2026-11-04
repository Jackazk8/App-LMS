import com.mechrobotix.aprendels.*
import kotlin.math.abs
fun main(){
    check(Bmi160Sample.parseBle(null,0)==null)
    for(n in listOf(0,19,21))check(Bmi160Sample.parseBle(ByteArray(n),0)==null)
    val decoded=Bmi160Sample.parseBle(packet(0xffffffffL,0xfffffffeL,ax=-16384,ay=8192,az=16384,gx=-164,gy=328,gz=-492),123)!!
    check(decoded.sequence==0xffffffffL&&decoded.deviceMs==0xfffffffeL&&decoded.receivedMs==123L)
    check(decoded.ax==-1.0&&decoded.ay==.5&&decoded.az==1.0)
    check(abs(decoded.gx+10)<1e-8&&abs(decoded.gy-20)<1e-8&&abs(decoded.gz+30)<1e-8)
    println("PASS BIN20 de firmware: longitud, little-endian, signos, escalas y uint32")
    val left=Bmi160Calibration();val right=Bmi160Calibration();left.start();right.start()
    for(i in 0..160){
        left.add(Bmi160Sample.parseBle(packet(i.toLong(),gx=7),i*20L)!!)
        right.add(Bmi160Sample.parseBle(packet(i.toLong(),gx=-10),i*20L)!!)
    }
    check(left.ready&&right.ready)
    check(abs(left.bx-7/16.4)<1e-8&&abs(right.bx+10/16.4)<1e-8)
    left.reset();check(!left.ready&&right.ready)
    left.start();for(i in 0..200)left.add(Bmi160Sample.parseBle(packet(i.toLong(),gx=164),i*20L)!!)
    check(!left.ready&&left.running)
    println("PASS calibraciones independientes, reinicio por mano y rechazo de movimiento")
}
