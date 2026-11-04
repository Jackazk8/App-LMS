package android.content
import android.bluetooth.BluetoothManager
open class Context {
    val manager=BluetoothManager()
    open val applicationContext:Context get()=this
    open fun getSystemService(name:String):Any?=if(name==BLUETOOTH_SERVICE)manager else null
    companion object { const val BLUETOOTH_SERVICE="bluetooth" }
}
