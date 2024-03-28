import spinal.core._
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
    def PHYSICAL_WIDTH : Int
}

case class LockPort() extends Bundle with IMasterSlave {
    val valid = Bool()
    val address = UInt(PHYSICAL_WIDTH bits)
    
    override def asMaster() = out(this)
}

case class DataLoadPort(dataWidth : Int,
                        refillCount : Int,
                        rspAt : Int) extends Bundle with IMasterSlave {
    val cmd = Stream

}


// Loads data into CPU from Cache
case class DataLoadCmd(cpuWordWidth : Int) extends Bundle {
   val address = UInt(cpuWordWidth bits) 
}
case class DataLoadRsp(cpuWordWidth : Int) extends Bundle {
   val data = Bits(cpuWordWidth bits) 
}
// Stores data in Cache from CPU
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
case class L1CacheComponent() extends Component with L1CacheParameters {
    val io = new Bundle {
        val load = slave(DataLoadPort(cpuWordWidth))
        val store = slave(DataStorePort(cpuWordWidth))
    }
    
    def nSets: Int = 256
    def nWays: Int = 1
    def blockSizeWords: Int = 16
    def addressWidth: Int = 32
    def cpuWordWidth: Int = 32
    
    

    case class Tag() extends Bundle {
        val valid = Bool()
        val tag = UInt(tagBits bits)
    }
    // case class Status() extends Bundle {
    //     val dirty = Bool()
    // }
    

    case class Block() extends Bundle {
        val data = Vec.fill(blockSizeWords)(Bits (cpuWordWidth bits))
    }

    val mem = Mem(Block(), nSets)
    val tags = Mem(Tag(), nSets)
    
    io.load.cmd.setIdle()
    io.load.rsp.setIdle()
    io.store.cmd.setIdle()
    io.store.rsp.setIdle()

    val load = new Area {
        when (io.load.cmd.valid && io.load.cmd.ready) {
            val address = io.load.cmd.payload.address
            val tag = address(31 downto 14)
            val index = address(13 downto 6)
            val blockOff = address(5 downto 2)

            if (tags.readAsync(index).tag == tag) {
               io.load.rsp.payload.data := mem.readAsync(index).data(blockOff)
               io.load.rsp.valid := True
            } else {/* Go UP heirarchy and request from L2?*/}


        }

    }  
    
    val store = new Area {
        when (io.store.cmd.valid && io.load.cmd.ready) {
            val address = io.store.cmd.payload.address
            val tag = address(31 downto 14)
            val index = address(13 downto 6)
            val blockOff = address(5 downto 2)
            
            
        }

    }

    
    
}

