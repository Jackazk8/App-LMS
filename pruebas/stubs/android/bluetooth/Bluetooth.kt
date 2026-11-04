package android.bluetooth
import android.content.Context
import android.bluetooth.le.BluetoothLeScanner
import java.util.UUID
open class BluetoothDevice(open val name:String?,val address:String) {
    open fun connectGatt(c:Context,auto:Boolean,cb:BluetoothGattCallback):BluetoothGatt?=null
    open fun connectGatt(c:Context,auto:Boolean,cb:BluetoothGattCallback,transport:Int):BluetoothGatt?=connectGatt(c,auto,cb)
    companion object { const val TRANSPORT_LE=2 }
}
open class BluetoothGatt {
    open fun discoverServices()=true
    open fun getService(uuid:UUID):BluetoothGattService?=null
    open fun setCharacteristicNotification(c:BluetoothGattCharacteristic,enabled:Boolean)=true
    open fun writeDescriptor(d:BluetoothGattDescriptor)=true
    open fun disconnect(){}
    open fun close(){}
    companion object { const val GATT_SUCCESS=0 }
}
open class BluetoothGattCallback {
    open fun onConnectionStateChange(g:BluetoothGatt,status:Int,newState:Int){}
    open fun onServicesDiscovered(g:BluetoothGatt,status:Int){}
    open fun onDescriptorWrite(g:BluetoothGatt,d:BluetoothGattDescriptor,status:Int){}
    open fun onCharacteristicChanged(g:BluetoothGatt,c:BluetoothGattCharacteristic){}
}
object BluetoothProfile { const val STATE_DISCONNECTED=0;const val STATE_CONNECTED=2 }
open class BluetoothGattService { open fun getCharacteristic(uuid:UUID):BluetoothGattCharacteristic?=null }
open class BluetoothGattCharacteristic(val uuid:UUID,val properties:Int) {
    var value:ByteArray?=null
    open fun getDescriptor(uuid:UUID):BluetoothGattDescriptor?=null
    companion object { const val PROPERTY_NOTIFY=16 }
}
class BluetoothGattDescriptor(val uuid:UUID) {
    var value:ByteArray?=null
    companion object { val ENABLE_NOTIFICATION_VALUE=byteArrayOf(1,0) }
}
class BluetoothAdapter { var isEnabled=true;val bluetoothLeScanner=BluetoothLeScanner() }
class BluetoothManager { val adapter=BluetoothAdapter() }
