package projectname

import spinal.core._


// Word Count = 4096 words
// 16KiB
// 16-words / block
// [18 bits]  [8 bits]     [4 bits]        [2 bits]
// Tag bits -- Index --- Block offset -- Byte offset
// (31 - 14)  (13 - 6)    (5 - 2)         (1 - 0)


class Tag() extends Bundle {
  
  val valid = Bool()
  val tag = UInt(18 bits)
}

class Block() extends Bundle {

  val data = Vec.fill(16)(Bits (32 bits))
}

// Hardware definition
case class Cache() extends Component {
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

object MyTopLevelVerilog extends App {
  Config.spinal.generateVerilog(Cache())
}

object MyTopLevelVhdl extends App {
  Config.spinal.generateVhdl(Cache())
}
