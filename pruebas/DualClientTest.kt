import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import com.mechrobotix.aprendels.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
private val DATA=UUID.fromString("920b0002-4e8d-4f7f-9e51-50b7c41a1600")
private val CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
fun packet(seq:Long,ms:Long=seq*20,ax:Int=0,ay:Int=0,az:Int=16384,gx:Int=0,gy:Int=0,gz:Int=0):ByteArray {
    val b=ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN).putInt(seq.toInt()).putInt(ms.toInt())
    for(v in intArrayOf(ax,ay,az,gx,gy,gz))b.putShort(v.toShort())
    return b.array()
}
class FakeGatt(val callback:BluetoothGattCallback,val missingService:Boolean=false,val rejectSubscription:Boolean=false):BluetoothGatt() {
    var closed=false;var subscribed=false
    val descriptor=BluetoothGattDescriptor(CCCD)
    val data=object:BluetoothGattCharacteristic(DATA,BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
        override fun getDescriptor(uuid:UUID)=descriptor.takeIf{it.uuid==uuid}
    }
    private val service=object:BluetoothGattService() {
        override fun getCharacteristic(uuid:UUID)=data.takeIf{it.uuid==uuid}
    }
    override fun getService(uuid:UUID)=service.takeIf{!missingService && uuid==Esp32BluetoothClient.serviceUuid}
    override fun discoverServices():Boolean {callback.onServicesDiscovered(this,0);return true}
    override fun writeDescriptor(d:BluetoothGattDescriptor):Boolean {
        subscribed=!rejectSubscription && d.value!!.contentEquals(byteArrayOf(1,0))
        callback.onDescriptorWrite(this,d,if(rejectSubscription)5 else 0);return true
    }
    override fun close(){closed=true}
    fun connected(){callback.onConnectionStateChange(this,0,BluetoothProfile.STATE_CONNECTED)}
    fun emit(bytes:ByteArray){data.value=bytes;callback.onCharacteristicChanged(this,data)}
    fun lose(){callback.onConnectionStateChange(this,8,BluetoothProfile.STATE_DISCONNECTED)}
}
class FakeDevice(address:String,val missingService:Boolean=false,val rejectSubscription:Boolean=false):BluetoothDevice("same-name",address) {
    @Volatile var gatt:FakeGatt?=null
    override fun connectGatt(c:Context,auto:Boolean,cb:BluetoothGattCallback,transport:Int):BluetoothGatt {
        check(!auto && transport==TRANSPORT_LE)
        return FakeGatt(cb,missingService,rejectSubscription).also{gatt=it}
    }
    fun discovered()=BleSensorDevice(name!!,address,this)
}
fun waitFor(message:String,test:()->Boolean){
    val until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3)
    while(!test()){check(System.nanoTime()<until){message};Thread.sleep(2)}
}
fun pending(context:Context,d:FakeDevice,hand:SensorHand,token:Int?=null):FutureTask<Unit> {
    val task=FutureTask<Unit>{
        if(token==null)Esp32BluetoothClient.connect(context,d.discovered(),hand)
        else Esp32BluetoothClient.connectIfCurrent(context,d.discovered(),token,hand)
    }
    thread(isDaemon=true){task.run()};return task
}
fun connect(context:Context,d:FakeDevice,hand:SensorHand):FakeGatt {
    val task=pending(context,d,hand);waitFor("connectGatt no inició"){d.gatt!=null}
    d.gatt!!.connected();task.get(3,TimeUnit.SECONDS)
    check(d.gatt!!.subscribed);return d.gatt!!
}
fun main(){
    val c=Esp32BluetoothClient;val ctx=Context();val l=SensorHand.LEFT;val r=SensorHand.RIGHT
    c.disconnect()
    val devices=mutableListOf<BleSensorDevice>()
    val stop=c.startScan(ctx,{devices.add(it)},{error(it)})
    val scanner=ctx.manager.adapter.bluetoothLeScanner
    check(scanner.filters.single().serviceUuid!!.uuid==c.serviceUuid)
    scanner.callback!!.onScanResult(0,ScanResult(FakeDevice("SCAN"),ScanRecord("LSM_C3_Der")))
    check(devices.single().name=="LSM_C3_Der");stop();check(scanner.callback==null)
    println("PASS búsqueda BLE por UUID y detención del escaneo")
    val left=connect(ctx,FakeDevice("AA"),l);val right=connect(ctx,FakeDevice("BB"),r)
    left.emit(packet(1,ax=1638));right.emit(packet(1,ax=14746))
    check(c.state(l).connected&&c.state(r).connected)
    check(c.state(l).sample!!.ax<.11&&c.state(r).sample!!.ax>.89)
    println("PASS dos sesiones GATT, nombres iguales y datos separados")
    left.emit(packet(4));right.emit(byteArrayOf(1,2,3));right.emit(packet(1))
    check(c.state(l).missed==2L&&c.state(l).invalid==0L)
    check(c.state(r).missed==0L&&c.state(r).invalid==2L)
    println("PASS pérdidas, paquete inválido y duplicados por mano")
    val duplicate=FakeDevice("AA")
    check(runCatching{c.connect(ctx,duplicate.discovered(),r)}.isFailure)
    check(duplicate.gatt==null&&c.state(l).connected&&c.state(r).connected)
    val epoch=c.state(l).epoch;val otherEpoch=c.state(r).epoch
    left.emit(packet(0,0));check(c.state(l).epoch>epoch&&c.state(r).epoch==otherEpoch)
    println("PASS dirección duplicada y reinicio aislado")
    c.disconnect(l);check(left.closed&&!c.state(l).connected&&c.state(l).sample==null&&c.state(r).connected)
    right.emit(packet(2));check(c.state(r).received==2L)
    for(device in listOf(FakeDevice("BAD",missingService=true),FakeDevice("REJECT",rejectSubscription=true))) {
        val task=pending(ctx,device,l);waitFor("fallo no iniciado"){device.gatt!=null};device.gatt!!.connected()
        check(runCatching{task.get(3,TimeUnit.SECONDS)}.isFailure)
        check(device.gatt!!.closed&&!c.state(l).connected&&c.state(r).connected)
    }
    println("PASS desconexión y fallos de servicio/suscripción sin afectar la otra mano")
    val slow=FakeDevice("SLOW");val task=pending(ctx,slow,l);waitFor("pendiente"){slow.gatt!=null}
    c.disconnect(l);check(runCatching{task.get(3,TimeUnit.SECONDS)}.isFailure)
    val replacement=connect(ctx,FakeDevice("NEW"),l)
    slow.gatt!!.connected();slow.gatt!!.emit(packet(9));slow.gatt!!.lose()
    check(c.state(l).address=="NEW"&&c.state(l).connected&&!replacement.closed)
    val token=c.connectionToken(l);c.disconnect(l);val stale=FakeDevice("STALE")
    check(runCatching{pending(ctx,stale,l,token).get(3,TimeUnit.SECONDS)}.isFailure)
    check(stale.gatt==null&&c.state(r).connected)
    right.lose();check(!c.state(r).connected&&c.state(r).sample==null&&right.closed)
    c.disconnect()
    println("PASS cancelación, callbacks antiguos, token obsoleto y pérdida de enlace")
}
