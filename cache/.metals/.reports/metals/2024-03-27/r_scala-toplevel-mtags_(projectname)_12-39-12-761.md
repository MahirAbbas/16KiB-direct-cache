error id: file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:[1887..1891) in Input.VirtualFile("file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala", "import spinal.core._
import spinal.lib._


trait L1CacheParameters {
    def cacheSizeWords : Int
    def cacheSizeBytes : Int
    
    assert(cacheSizeBytes*4 == cacheSizeWords)
    
    def nSets : Int
    def nWays : Int
    def blockSizeWords : Int
    def addressWidth : Int
    def cpuWordWidth : Int
    def blockOffsetBits = log2Up(blockSizeWords)
    def indxBits = log2Up(nSets)
    def byteOffsetBits = 2
    def untagBits = blockOffsetBits + indxBits + byteOffsetBits
    def tagBits = addressWidth - untagBits
    def wayBits = log2Up(nWays)
    def isDM = nWays == 1
    

}

case class DataLoadCmd(cpuWordWidth : Int) extends Bundle {
   val address = UInt(cpuWordWidth bits) 
}
case class DataLoadRsp(cpuWordWidth : Int) extends Bundle {
   val data = Bits(cpuWordWidth bits) 
}
case class DataStoreCmd(cpuWordWidth : Int) extends Bundle {
    val address = UInt(cpuWordWidth bits)
    val data = Bits(cpuWordWidth bits)
}
case class DataStoreRsp(cpuWordWidth : Int) extends Bundle {
    val address = UInt(cpuWordWidth bits)
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

// Not Generic. For 16KiB direc-cache
// Make Generic Later
//
case class L1CacheComponent(cpuWordWidth : Int) extends Component with L1CacheParameters {
    val io = new Bundle {
        val load = slave(DataLoadPort(cpuWordWidth))
        val store = slave(DataStorePort(cpuWordWidth))
    }
    
    def nSets: Int = ???
    def
    
    

    case class Tag() extends Bundle {
        val valid = Bool()
        val tag = UInt(tagBits bits)
    }
    case class Status() extends Bundle {
        val dirty = Bool()
    }
    case class Block() extends Bundle {
        val data = Vec.fill(blockSizeWords)(Bits (cpuWordWidth bits))
    }

    
    val mem = Mem(Block(), 256)
    

    
    
}

")
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:73: error: expected identifier; obtained case
    case class Tag() extends Bundle {
    ^
#### Short summary: 

expected identifier; obtained case