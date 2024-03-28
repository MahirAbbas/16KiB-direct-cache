error id: file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:[264..267) in Input.VirtualFile("file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala", "import spinal.core._
import spinal.lib._


trait L1CacheParameters {
    
    def nSets : Int
    def nWays : Int
    def blockSizeWords : Int
    def addressBits : Int
    def blockOffsetBits = log2Up(blockSizeWords)
    def indxBits = log2Up(nSets)
    def 
    def untagBits = blockOffsetBits + indxBits
    def tagBits = addressBits - untagBits
    def wayBits = log2Up(nWays)
    def isDM = nWays == 1

}

case class DataLoadCmd(addressWidth : Int) extends Bundle {
   val address = UInt(addressWidth bits) 
}
case class DataLoadRsp(addressWidth : Int) extends Bundle {
   val data = Bits(addressWidth bits) 
}
case class DataStoreCmd(addressWidth : Int) extends Bundle {
    val address = UInt(addressWidth bits)
    val data = Bits(addressWidth bits)
}
case class DataStoreRsp(addressWidth : Int) extends Bundle {
    val address = UInt(addressWidth bits)
    val success = Bool()
}

case class DataLoadPort(datawidth : Int) extends Bundle with IMasterSlave {
  val cmd = Stream(DataLoadCmd(datawidth))
  val rsp = Flow(DataLoadRsp(datawidth))
  override def asMaster(): Unit = {
    master(cmd)
    slave(rsp)
  }
}
case class DataStorePort(datawidth : Int) extends Bundle with IMasterSlave {
  val cmd = Stream(DataStoreCmd(datawidth))
  val rsp = Flow(DataStoreRsp(datawidth))
  override def asMaster(): Unit = {
    master(cmd)
    slave(rsp)
  }
}
case class L1CacheComponent(addressWidth : Int) extends Component with L1CacheParameters {
    val io = new Bundle {
        val load = slave(DataLoadPort(addressWidth))
        val store = slave(DataStorePort(addressWidth))
    }
}

")
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:14: error: expected identifier; obtained def
    def untagBits = blockOffsetBits + indxBits
    ^
#### Short summary: 

expected identifier; obtained def