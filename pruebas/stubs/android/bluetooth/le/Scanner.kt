package android.bluetooth.le
import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
open class ScanCallback {
    open fun onScanResult(callbackType:Int,result:ScanResult){}
    open fun onBatchScanResults(results:MutableList<ScanResult>){}
    open fun onScanFailed(errorCode:Int){}
}
class ScanResult(val device:BluetoothDevice,val scanRecord:ScanRecord?=null)
class ScanRecord(val deviceName:String?)
class ScanFilter(val serviceUuid:ParcelUuid?) {
    class Builder {
        private var uuid:ParcelUuid?=null
        fun setServiceUuid(value:ParcelUuid)=apply{uuid=value}
        fun build()=ScanFilter(uuid)
    }
}
class ScanSettings {
    class Builder { fun setScanMode(mode:Int)=this;fun build()=ScanSettings() }
    companion object { const val SCAN_MODE_LOW_LATENCY=2 }
}
class BluetoothLeScanner {
    var callback:ScanCallback?=null
    var filters:List<ScanFilter> = emptyList()
    fun startScan(filters:List<ScanFilter>,settings:ScanSettings,callback:ScanCallback){this.filters=filters;this.callback=callback}
    fun stopScan(callback:ScanCallback){if(this.callback===callback)this.callback=null}
}
