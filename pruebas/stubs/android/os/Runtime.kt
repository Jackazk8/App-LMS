package android.os
import java.util.UUID
object Build { object VERSION { var SDK_INT=36 } }
object SystemClock { fun elapsedRealtime()=System.nanoTime()/1000000 }
class ParcelUuid(val uuid:UUID)
