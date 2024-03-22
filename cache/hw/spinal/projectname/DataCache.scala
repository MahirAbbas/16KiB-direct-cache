package projectname

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._


// Loads data from Cache into CPU
case class DataLoadPort(datawidth : Int) extends Bundle with IMasterSlave {
  
  val cmd = Stream(DataLoadCmd(datawidth))
  val rsp = Flow(DataLoadRsp(datawidth))

  override def asMaster(): Unit = {
    master(cmd)
    slave(rsp)
  }

}

case class DataLoadCmd(datawidth : Int) extends Bundle {
  val address = UInt(datawidth bits)

}

case class DataLoadRsp(datawidth : Int) extends Bundle {

  val data = Bits(datawidth bits)
  val fault = Bool()
  
}

// Stores data from CPU into Cache

case class DataStorePort(datawidth : Int) extends Bundle with IMasterSlave {

  val cmd = Stream(DataStoreCmd(datawidth))
  val rsp = Flow(DataStoreRsp(datawidth))

  override def asMaster(): Unit = {
    master(cmd)
    slave(rsp)
  }

}

case class DataStoreCmd(datawidth : Int) extends Bundle {
  val address = UInt(datawidth bits)
  val data = Bits(datawidth bits)
  
}

case class DataStoreRsp(datawidth : Int) extends Bundle {
  val address = UInt(datawidth bits)
  val success = Bool()
}


// Hardware definition
// 

case class DataCacheParameters(cacheSize: Int,
                               cpuDataWidth: Int,
                               lineSize: Int = 64,
                               loadReadBanksAt: Int = 0,
                               loadReadTagsAt: Int = 1,
                               loadBankMuxesAt: Int = 1,
                               loadBankMuxAt: Int = 2,
                               loadControlAt: Int = 2,
                               storeControlAt: Int = 2,
                               tagsReadAsync : Boolean = true,
                               reducedBankWidth : Boolean = false){
}
                               



class DataCache(datawidth : Int) extends Component {

  case class Tag() extends Bundle {
    
    val valid = Bool()
    val tag = UInt(18 bits)
  }
  
  case class Status() extends Bundle {
    val dirty = Bool()
  }



  case class Block() extends Bundle {

    val data = Vec.fill(16)(Bits (32 bits))
  }

  val byteOffset = io.address(1 downto 0)
  val blockOffset = io.address(5 downto 2)
  val index = io.address(13 downto 6)
  val tag = io.address(31 downto 14)
  
  
  val io = new Bundle {

    val load = slave(DataLoadPort(datawidth))
    val store = slave(DataStorePort(datawidth))

    val valid = out port Bool() 
    val ready = out port Bool()

    val dataReadPort = out port(Bits (32 bits))
    val address = in port (Bits (32 bits))
    val dataWritePort = in port (Bits (32 bits))

  }
  val cpuWordWidth = 32
  val cacheSize = 4096 // words
  val tagWidth = 13 

  val cpuWordWidth = cpuDataWidth
  val waySize = cacheSize/wayCount
  val linePerWay = waySize/lineSize
  val memDataPerWay = waySize/bytePerMemWord
  val memData = HardType(Bits(memDataWidth bits))
  val memWordPerLine = lineSize/bytePerMemWord
  val tagWidth = postTranslationWidth-log2Up(waySize)

  
  val tagRange = postTranslationWidth-1 downto log2Up(linePerWay*lineSize)
  val lineRange = tagRange.low-1 downto log2Up(lineSize)
  val refillRange = tagRange.high downto lineRange.low

  val bankCount = wayCount
  val bankWidth =  if(!reducedBankWidth) memDataWidth else Math.max(cpuWordWidth, memDataWidth/wayCount)
  val bankByteSize = cacheSize/bankCount
  val bankWordCount = bankByteSize*8/bankWidth
  val bankWordToCpuWordRange = log2Up(bankWidth/8)-1 downto log2Up(bytePerFetchWord)
  val memToBankRatio = bankWidth*bankCount / memDataWidth
  val bankWord = HardType(Bits(bankWidth bits))
  val bankWordPerLine = lineSize*8/bankWidth

  val hit = Bool()


  val tagCache = Mem(Tag(), 256)
  val blockCache = Mem(Block(), 256)






}
