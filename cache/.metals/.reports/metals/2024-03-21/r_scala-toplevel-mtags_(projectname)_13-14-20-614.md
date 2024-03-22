error id: file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala:[268..269) in Input.VirtualFile("file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala", "package projectname

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._


case class DataLoadPort(datawidth : Int) extends Bundle with IMasterSlave {
  
  val cmd = Stream(DataLoadCmd(datawidth))
  val rsp = Flow(DataLoadRsp(datawidth))

  override def

}

case class DataLoadCmd(datawidth : Int) extends Bundle {
  val address = UInt(datawidth bits)

}

case class DataLoadRsp(datawidth : Int) extends Bundle {

  val data = Bits(datawidth bits)
  val fault = Bool()
  
}

case class DataStorePort() extends Bundle {


}

case class CacheResult() extends Bundle {

    val data = out port Bits(32 bits)
    val ready = out port Bool()

}


case class DataMemRequest() extends Bundle {


  val address = UInt(32 bits)
  val data = in port Bits (128 bits)
  val rw = Bool() // 0 = read, 1 = write
  val valid = Bool()
  
}

case class DataMemResponse() extends Bundle {

  val data = in port Bits(128 bits)
  val ready = in port Bool()

}


class Controller extends Component {
  val io = new Bundle {

    // define 2 classes. one that requests data from cache to CPU
    // and one that CPU writes to cache
    // so 2 sets of connections. one CPU <=> CACHE
    // one MEM <=> CACHE
    // DataStoreCMD
    // DatarequestCMD

  }

  val request = CpuRequestPort()

  val VALID, HIT = Bool()


  val controllerFsm = new StateMachine {
    val IDLE, COMPARE, ALLOCATE, WRITEBACK = new State 
    setEntry(IDLE)

    IDLE
      .whenIsActive {
        when (request.valid === True) {
          goto(COMPARE)
        }


      }

    COMPARE
      .whenIsActive {
        

      }

  }


}


// Hardware definition
class DataCache() extends Component {

  case class Tag() extends Bundle {
    
    val valid = Bool()
    val dirty = Bool()
    val tag = UInt(18 bits)
  }



  case class Block() extends Bundle {

    val data = Vec.fill(16)(Bits (32 bits))
  }

  val io = new Bundle {

    val read = in Bool()
    val write = in Bool()

    val valid = out port Bool() 
    val ready = out port Bool()

    val dataReadPort = out port(Bits (32 bits))
    val address = in port (Bits (32 bits))
    val dataWritePort = in port (Bits (32 bits))

  }


  val byteOffset = io.address(1 downto 0)
  val blockOffset = io.address(5 downto 2)
  val index = io.address(13 downto 6)
  val tag = io.address(31 downto 14)

  val hit = Bool()


  val tagCache = Mem(Tag(), 256)
  val blockCache = Mem(Block(), 256)






}
")
file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala
file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala:15: error: expected identifier; obtained rbrace
}
^
#### Short summary: 

expected identifier; obtained rbrace