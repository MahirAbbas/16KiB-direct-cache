import spinal.core._
import spinal.lib._
import spinal.lib.misc.pipeline.{Payload,Node}


// trait L1CacheParameters {
//     def cacheSizeWords : Int
//     def cacheSizeBytes : Int
    
//     assert(cacheSizeBytes*4 == cacheSizeWords)
    
//     def nSets : Int
//     def nWays : Int
//     def blockSizeWords : Int
//     def addressWidth : Int
//     def cpuWordWidth : Int
//     def blockOffsetBits = log2Up(blockSizeWords)
//     def indxBits = log2Up(nSets)
//     def byteOffsetBits = 2
//     def untagBits = blockOffsetBits + indxBits + byteOffsetBits
//     def tagBits = addressWidth - untagBits
//     def wayBits = log2Up(nWays)
//     def isDM = nWays == 1
//     def PHYSICAL_WIDTH : Int
// }

case class LockPort() extends Bundle with IMasterSlave {
    val valid = Bool()
    val address = UInt(PHYSICAL_WIDTH bits)
    
    override def asMaster() = out(this)
}

case class DataLoadPort(dataWidth : Int,
                        refillCount : Int,
                        rspAt : Int) extends Bundle with IMasterSlave {
    val cmd = Stream(DataLoadCmd(dataWidth))
    // val cancels = Bits(rspAt+1 bits)
    val rsp = Flow(DataLoadRsp(dataWidth, refillCount))
    
    override def asMaster() = {
        master(cmd)
        out(cancels)
        slave(rsp)
    }
}
// Loads data into CPU from Cache
case class DataLoadCmd(dataWidth : Int) extends Bundle {
   val address = UInt(cpuWordWidth bits) 
   val size = UInt(log2Up(log2Up(dataWidth/8)+1) bits) // Calculates/ gives size of memory access (eg w , hw, byte etc.)
//    val redoOnDataHazard = Bool()
//    val unlocked = Bool() // Does it need exclusive (locked) access to data?
//    val unique = bool() // Does the operation need exclusive access/ ownership of data?
}
case class DataLoadRsp(dataWidth: Int, refillCount : Int) extends Bundle {
   val data = Bits(dataWidth bits) 
//    val fault = Bool()
//    val redo = Bool()
//    val refillSlot = Bits(refillCount bits)
//    val refillSlotAny = Bool()
}
// Stores data in Cache from CPU
case class DataStorePort(datawidth : Int,
                         refillCount : Int) extends Bundle with IMasterSlave {
  val cmd = Stream(DataStoreCmd(datawidth))
  val rsp = Flow(DataStoreRsp(datawidth))
  override def asMaster(): Unit = {
    master(cmd)
    slave(rsp)
  }
}
case class DataStoreCmd(dataWidth: Int) extends Bundle {
    val address = UInt(dataWidth bits)
    val data = Bits(dataWidth bits)
    val mask = Bits(dataWidth/8 bits)
    // val generation = Bool()
    // val io = Bool()
    // val flush = Bool() // Flush all the ways for given address Line. May also ser rsp.redo
    // val flushFree = Bool() //Flush Cache Line and free up data
    // val prefetch = Bool()
}
case class DataStoreRsp(addressWidth: Int, refillCount : Int) extends Bundle {
    val fault = Bool()
    // val redo = Bool()
    // val refillSlot = Bits(refillCount bits) // Zero when refillSlotAny // Specifies which Slot should be refilled
    // val refillSlotAny = Bool() // not valid if !miss
    // val generationKo = Bool() // ignore everything else if this is set // Signals failure related to generation/version tracking of data
    val flush = Bool()
    val prefetch = Bool()
    val address = UInt(addressWidth bits)
    val io = Bool()
}


// Not Generic. For 16KiB direc-cache
// Make Generic Later
//
//


case class L1CacheParamaters(cacheSize: Int,
                             wayCount: Int,
                             refillCount: Int,
                             writebackCount: Int,
                             cpuDataWidth: Int,
                            //  withCoherency : Boolean = false,
                            //  loadRefillCheckEarly : Boolean = true,
                            //  storeRefillCheckEarly : Boolean = true,
                             lineSize : Int,
                            //  loadReadBanksAt: Int,
                            //  loadReadTagsAt: Int,
                            //  loadHitsAt: Int,
                            //  loadHitAt: Int,
                            //  loadBankMuxesAt: Int,
                            //  loadBankMuxAt: Int,
                            //  laodControlAt: Int,
                             loadRspAt: Int,
                            //  storeReadBanksAt: Int,
                            //  storeReadTagsAt: Int,
                            //  storeHitsAt: Int,
                            //  storeHitAt: Int,
                            //  storeControlAt: Int,
                            //  storeRspAt: Int,
                            //  tagsReadAsync: Boolean,
                             reducedBankWidth : Boolean = false) {
}

case class L1CacheComponent(val p : L1CacheParamaters) extends Component {
    import p._

    val io = new Bundle {
        val lock = slave(LockPort())
        val load = slave(DataLoadPort(
            dataWidth = cpuDataWidth,
            refillCount = refillCount,
            rspAt = loadRspAt,
        ))
        val store = slave(DataStorePort(
            datawidth = cpuDataWidth,
            refillCount = refillCount
        ))
        // val refillCompletions = out Bool()
        // val refillEvent = out Bool()
        // val writebackEvent = out Bool()
        // val writebackBusy = out Bool()
        // val tagEvent = out Bool()
    }
    val cpuWordWidth = cpuDataWidth
    val bytePerFetchWord = cpuDataWidth/8
    val waySize = cacheSize/wayCount
    val linePerWay = waySize/lineSize
    // val tagWidth = cpuDataWidth - untagBits 

    //TODO:
    val tagRange = postTranslationWidth-1 downto log2Up(linePerWay*lineSize)
    val lineRange = tagRange.low-1 downto log2Up(lineSize)
    val refillRange = tagRange.high downto lineRange.low
    
    
    //TODO:
    // val bankCount = wayCount
    // val bankWidth =  if(!reducedBankWidth) memDataWidth else Math.max(cpuWordWidth, memDataWidth/wayCount)
    // val bankByteSize = cacheSize/bankCount
    // val bankWordCount = bankByteSize*8/bankWidth
    // val bankWordToCpuWordRange = log2Up(bankWidth/8)-1 downto log2Up(bytePerFetchWord)
    // val memToBankRatio = bankWidth*bankCount / memDataWidth
    // val bankWord = HardType(Bits(bankWidth bits))
    // val bankWordPerLine = lineSize*8/bankWidth

    
    val ADDRESS_POST_TRANSLATION = Payload(UInt(postTranslationWidth bits))
    val ABORT = Payload(Bool())
    val CPU_WORD = Payload(Bits(cpuWordWidth bits))
    val CPU_MASK = Payload(Bits(cpuWordWidth/8 bits))
    val WAYS_HAZARD = Payload(Bits(wayCount bits))
    val REDO_ON_DATA_HAZARD = Payload(Bool())
    val BANK_BUSY = Payload(Bits(bankCount bits))
    val BANK_BUSY_REMAPPED = Payload(Bits(bankCount bits))
    val REFILL_HITS_EARLY = Payload(Bits(refillCount bits))
    val REFILL_HITS = Payload(Bits(refillCount bits))
    val LOCKED, UNLOCKED = Payload(Bool())
    val NEED_UNIQUE = Payload(Bool())
    
    case class Tag() extends Bundle {
        val address = UInt(tagWidth bits)
        val fault = Bool()
        val valid = Bool()
    }
    case class Status() extends Bundle {
        val dirty = Bool()
    }
    
    val STATUS = Payload(Vec.fill(wayCount))(Status())
    val BANKS_WORDS = Payload(Vec.fill(bankCount)(bankWord()))
    val WAYS_TAGS = Payload(Vec.fill(wayCount)(Tag()))
    val WAYS_HITS = Payload(Bits(wayCOunt bits))
    val WAYS_HIT = Payload(Bool())
    val MISS = Payload(Bool())
    val FAULT = Payload(Bool())
    val REDO = Payload(Bool())
    val IO = Payload(Bool())
    val REFILL_SLOT = Payload(Bits(refillCount bits))
    val REFILL_SLOT_FULL_= Payload(Bool())
    val GENERATION, GENERATION_OK = Payload(Bool())
    val PREFETCH  = Payload(Bool())
    val PROBE = Payload(Bool())
    val ALLOW_UNIQUE= Payload(Bool())
    val ALLOW_SHARED= Payload(Bool())
    val ALLOW_PROBE_= Payload(Bool())
    val PROBE_ID = Payload(UInt(probeIdWidth bits))
    val FLUSH  = Payload(Bool())
    val FLUSH_FREE = Payload(Bool())


    val BANKS_MUXES = Stageable(Vec.fill(bankCount)(Bits(couWordWidth bits)))
        
    val banks = for(id <- 0 until bankCount) yield new Area {
        val mem = Mem(Bits(benkWidth bits), bankWordCount)
        val write = mem.writePortWithMask(mem.getWidth/8)
        val read = new Area {
            val usedByWriteBack = False
            val cmd = Flow(mem.addressType)
            val rsp = mem.readSync(cmd.payload, cmd.valid)
            KeepAttribute(rsp)
            
            cmd.setIdle()
        }
    }

    val tagsOrStatusWriteArbitration = new Reservation()
    
    val waysWrite = new Area{
        val mask = Bits(wayCount bits)
        val address = UInt(log2Up(linePerWay) bits)
        val tag = Tag()

        mask := 0
        address.assignDontCare()
        tag.assignDontCare()

        // Used for hazard tracking in a pipelined way
        val maskLast = RegNext(mask)
        val addressLast = RegNext(address)
    }

    val ways = for(id <- 0 until wayCount) yield new Area {
        val mem = Mem.fill(lineperway)(Tag())
        mem.write(waysWrite.address, waysWrite.tag, waysWrite.mask(id))
        val loadRead = new Area {
            val cmd = Flow(mem.addressType)
            val rsp = if(tagsReadAsync) mem.ReadAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
            KeepAttribute(rsp)
        }
        val storeRead = new Area {
            val cmd = Flow(mem.addressType)
            val rsp = if(tagsReadAsync) mem.readAsync (cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
            KeepAttribute(rsp)
        }
    }
    
    val status = new Area{
      // Hazard betwen load/store is solved by the fact that only one can write trigger a refill/cajmge the status at a given time
      val mem = Mem.fill(linePerWay)(Vec.fill(wayCount)(Status()))
      val write = mem.writePort.setIdle()
      val loadRead = new Area {
        val cmd = Flow(mem.addressType)
        val rsp = if(tagsReadAsync) mem.readAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
        KeepAttribute(rsp)
      }
      val storeRead = new Area {
        val cmd = Flow(mem.addressType)
        val rsp = if(tagsReadAsync) mem.readAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
        KeepAttribute(rsp)
      }
      val writeLast = write.stage()
    }


    
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

