// SPDX-FileCopyrightText: 2023 "Everybody"
//
// SPDX-License-Identifier: MIT

package naxriscv.lsu

//allows to lock a physical address into unique state
case class LockPort() extends Bundle with IMasterSlave {
  val valid = Bool()
  val address = UInt(PHYSICAL_WIDTH bits)

  override def asMaster() = out(this)
}

case class DataLoadPort(
                        dataWidth : Int,
                        refillCount : Int,
                        rspAt : Int,
                        translatedAt : Int) extends Bundle with IMasterSlave {
  val cmd = Stream(DataLoadCmd(dataWidth))
  val translated = DataLoadTranslated()
  val cancels = Bits(rspAt+1 bits)
  val rsp = Flow(DataLoadRsp(dataWidth, refillCount)) //The rsp.valid is fondamentaly necessary, as it has a fixed latency

  override def asMaster() = {
    master(cmd)
    out(translated)
    out(cancels)
    slave(rsp)
  }
}

case class DataLoadCmd(dataWidth : Int) extends Bundle {
  val virtual = UInt(bits)
  val size = UInt(log2Up(log2Up(dataWidth/8)+1) bits)
  val redoOnDataHazard = Bool() //Usefull for access not protected by the LSU (ex MMU refill)
  val unlocked = Bool()
  val unique = Bool()  //Used by atomic to ensure that the line is owned in a coherent unique state
}

case class DataLoadTranslated(physicalWidth : Int) extends Bundle {
  val physical   = UInt(physicalWidth bits)
  val abord = Bool()
}

case class DataLoadRsp(dataWidth : Int, refillCount : Int) extends Bundle {
  val data = Bits(dataWidth bits)
  val fault = Bool() //redo win against fault
  val redo = Bool()
  val refillSlot = Bits(refillCount bits) //Zero when refillSlotAny
  val refillSlotAny = Bool() //Not valid if !miss
}

case class DataStorePort(postTranslationWidth: Int,
                         dataWidth: Int,
                         refillCount : Int) extends Bundle with IMasterSlave {
  val cmd = Stream(DataStoreCmd(postTranslationWidth, dataWidth))
  val rsp = Flow(DataStoreRsp(postTranslationWidth, refillCount))

  override def asMaster() = {
    master(cmd)
    slave(rsp)
  }
}
case class DataStoreCmd(postTranslationWidth: Int,
                        dataWidth: Int) extends Bundle {
  val address = UInt(postTranslationWidth bits)
  val data = Bits(dataWidth bits)
  val mask = Bits(dataWidth/8 bits)
  val generation = Bool()
  val io = Bool()
  val flush = Bool() //Flush all the ways for the given address's line. May also set rsp.redo
  val flushFree = Bool()
  val prefetch = Bool()
}

case class DataStoreRsp(addressWidth : Int, refillCount : Int) extends Bundle {
  val fault = Bool()
  val redo = Bool()
  val refillSlot = Bits(refillCount bits) //Zero when refillSlotAny
  val refillSlotAny = Bool() //Not valid if !miss
  val generationKo = Bool() //Ignore everything else if this is set
  val flush = Bool()
  val prefetch = Bool()
  val address = UInt(addressWidth bits)
  val io = Bool()
}


case class DataCacheParameters(cacheSize: Int,
                               wayCount: Int,
                               refillCount : Int,
                               writebackCount : Int,
                               memDataWidth: Int,
                               cpuDataWidth: Int,
                               preTranslationWidth: Int,
                               postTranslationWidth: Int,
                               withCoherency : Boolean = false,
                               var probeIdWidth : Int = -1,
                               var ackIdWidth   : Int = -1,
                               loadRefillCheckEarly : Boolean = true,
                               storeRefillCheckEarly : Boolean = true,
                               lineSize: Int = 64,
                               loadReadBanksAt: Int = 0,
                               loadReadTagsAt: Int = 1,
                               loadTranslatedAt : Int = 1,
                               loadHitsAt: Int = 1,
                               loadHitAt: Int = 1,
                               loadBankMuxesAt: Int = 1,
                               loadBankMuxAt: Int = 2,
                               loadControlAt: Int = 2,
                               loadRspAt: Int = 2,
                               storeReadBanksAt: Int = 0,
                               storeReadTagsAt: Int = 1,
                               storeHitsAt: Int = 1,
                               storeHitAt: Int = 1,
                               storeControlAt: Int = 2,
                               storeRspAt: Int = 2,
                               tagsReadAsync : Boolean = true,
                               reducedBankWidth : Boolean = false){
  def memParameter = DataMemBusParameter(
    addressWidth  = postTranslationWidth,
    dataWidth     = memDataWidth,
    readIdCount   = refillCount,
    writeIdCount  = writebackCount,
    probeIdWidth  = probeIdWidth,
    ackIdWidth    = ackIdWidth,
    lineSize      = lineSize,
    withReducedBandwidth = false,
    withCoherency = withCoherency
  )

}

class DataCache(val p : DataCacheParameters) extends Component {
  import p._

  val io = new Bundle {
    val load = slave(DataLoadPort(
      dataWidth     = cpuDataWidth,
      rspAt         = loadRspAt,
    ))
    val store = slave(DataStorePort(
      dataWidth     = cpuDataWidth,
    ))
    val writebackEvent = out Bool()
    val writebackBusy = out Bool()
    val tagEvent = out Bool()
  }

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

  assert(bankWidth <= memDataWidth)


  val CPU_WORD = Stageable(Bits(cpuWordWidth bits))
  val LOCKED, UNLOCKED = Stageable(Bool())


  case class Tag() extends Bundle{
    val loaded = Bool()
    val address = UInt(tagWidth bits)
    val fault = Bool()
    val unique = withCoherency generate Bool()
  }

  case class Status() extends Bundle{
    val dirty = Bool()
  }

  val STATUS = Stageable(Vec.fill(wayCount)(Status()))
  val BANKS_WORDS = Stageable(Vec.fill(bankCount)(bankWord()))
  val WAYS_TAGS = Stageable(Vec.fill(wayCount)(Tag()))
  val WAYS_HITS = Stageable(Bits(wayCount bits))
  val WAYS_HIT = Stageable(Bool())
  val MISS = Stageable(Bool())
  val FAULT = Stageable(Bool())
  val REDO = Stageable(Bool())
  val IO = Stageable(Bool())
  val REFILL_SLOT = Stageable(Bits(refillCount bits))
  val REFILL_SLOT_FULL = Stageable(Bool())
  val GENERATION, GENERATION_OK = Stageable(Bool())
  val PREFETCH = Stageable(Bool())
  val PROBE = Stageable(Bool())
  val ALLOW_UNIQUE = Stageable(Bool())
  val ALLOW_SHARED = Stageable(Bool())
  val ALLOW_PROBE_DATA = Stageable(Bool())
  val PROBE_ID = Stageable(UInt(probeIdWidth bits))
  val FLUSH = Stageable(Bool())
  val FLUSH_FREE = Stageable(Bool())

  val BANKS_MUXES = Stageable(Vec.fill(bankCount)(Bits(cpuWordWidth bits)))

  val banks = for(id <- 0 until bankCount) yield new Area{
    val mem = Mem(Bits(bankWidth bits), bankWordCount)
    val write = mem.writePortWithMask(mem.getWidth/8)
    val read = new Area{
      val usedByWriteBack = False
      val cmd = Flow(mem.addressType)
      val rsp = mem.readSync(cmd.payload, cmd.valid)
      KeepAttribute(rsp)

      cmd.setIdle() //TODO revert it !
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

    //Used for hazard tracking in a pipelined way
    val maskLast = RegNext(mask)
    val addressLast = RegNext(address)
  }

  val ways = for(id <- 0 until wayCount) yield new Area {
    val mem = Mem.fill(linePerWay)(Tag())
    mem.write(waysWrite.address, waysWrite.tag, waysWrite.mask(id))
    val loadRead = new Area{
      val cmd = Flow(mem.addressType)
      val rsp = if(tagsReadAsync) mem.readAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
      KeepAttribute(rsp)
    }
    val storeRead = new Area{
      val cmd = Flow(mem.addressType)
      val rsp = if(tagsReadAsync) mem.readAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
      KeepAttribute(rsp)
    }
  }

  val status = new Area{
    //Hazard between load/store is solved by the fact that only one can write trigger a refill/change the status at a given time
    val mem = Mem.fill(linePerWay)(Vec.fill(wayCount)(Status()))
    val write = mem.writePort.setIdle()
    val loadRead = new Area{
      val cmd = Flow(mem.addressType)
      val rsp = if(tagsReadAsync) mem.readAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
      KeepAttribute(rsp)
    }
    val storeRead = new Area{
      val cmd = Flow(mem.addressType)
      val rsp = if(tagsReadAsync) mem.readAsync(cmd.payload) else mem.readSync(cmd.payload, cmd.valid)
      KeepAttribute(rsp)
    }
    val writeLast = write.stage()


    def bypass(status : Vec[Status], address : UInt, withLast : Boolean): Vec[Status] ={
      val ret = CombInit(status)
      if(withLast) when(writeLast.valid && writeLast.address === address(lineRange)){
        ret  := writeLast.data
      }
      when(write.valid && write.address === address(lineRange)){
        ret  := write.data
      }
      ret
    }
    def bypass(stage: Stage, address : Stageable[UInt], withLast : Boolean): Unit ={
      stage.overloaded(STATUS) := bypass(stage(STATUS), stage(address), withLast)
    }
  }

  val wayRandom = CounterFreeRun(wayCount)

  val PLRU = Stageable(Plru.State(wayCount))
  val plru = new Area {
    val ram = Mem.fill(linePerWay)(Plru.State(wayCount))
    val write = ram.writePort
    val fromLoad, fromStore = cloneOf(write)
    write.valid := fromLoad.valid || fromStore.valid
    write.payload := fromLoad.valid.mux(fromLoad.payload, fromStore.payload)
  }

  val invalidate = new Area{
    val counter = Reg(UInt(log2Up(linePerWay)+1 bits)) init(0)
    val done = counter.msb
    val reservation = tagsOrStatusWriteArbitration.create(0) //Warning assume no refill at the same time
    when(!done && reservation.win){
      reservation.takeIt()
      counter := counter + 1
      waysWrite.mask.setAll()
      waysWrite.address := counter.resized
      waysWrite.tag.loaded := False
    }

    val firstEver = RegInit(True) clearWhen (done)
    when(!done && firstEver){
      plru.write.valid := True
      plru.write.address := counter.resized
      plru.write.data.clearAll()
    }
  }

  class PriorityArea(slots : Seq[(Bool, Bits)]) extends Area{
    val slotsWithId = slots.zipWithIndex.map(e => (e._1._1, e._1._2, e._2))
    val hits = B(slots.map(_._1))
    val hit = hits.orR
    val oh = hits & B(slotsWithId.map(slot => (B(slotsWithId.filter(_ != slot).map(other => hits(other._3))) & slot._2) === 0))
    val sel = OHToUInt(oh)
    val lock = RegNext(oh) init(0)
    when(lock.orR){ oh := lock }
  }

  val refill = new Area {
    val slots = for (refillId <- 0 until refillCount) yield new Area {
      val id = refillId
      val valid = RegInit(False)
      val address = Reg(UInt(postTranslationWidth bits))
      val way = Reg(UInt(log2Up(wayCount) bits))
      val cmdSent = Reg(Bool())
      val priority = Reg(Bits(refillCount-1 bits)) //TODO Check it
      val unique = withCoherency generate Reg(Bool())
      val data = withCoherency generate Reg(Bool())
      val ackId = withCoherency generate Reg(UInt(ackIdWidth bits))
      val ackValid = withCoherency generate RegInit(False)

      // This counter ensure that load/store which started before the end of the refill memory transfer but ended after the end
      // of the memory transfer do see that there was a refill ongoing and that they need to retry
      val loaded = Reg(Bool())
      val loadedCounterMax = (loadControlAt-1) max (storeControlAt-1)
      val loadedCounter = Reg(UInt(log2Up(loadedCounterMax+1) bits))
      val loadedDone = loadedCounter === loadedCounterMax
      loadedCounter := loadedCounter + U(loaded && !loadedDone).resized
      valid clearWhen (loadedDone && withCoherency.mux(!ackValid, True))

      val free = !valid

      val victim = Reg(Bits(writebackCount bits))
      val writebackHazards = Reg(Bits(writebackCount bits)) //TODO Check it
    }

    //Ignore the way, allowing coherent BtoT to detect ongoing NtoB
    def isLineBusy(address : UInt) = slots.map(s => s.valid && s.address(lineRange) === address(lineRange)).orR

    val free = B(OHMasking.first(slots.map(_.free)))
    val full = slots.map(!_.free).andR

    val push = Flow(new Bundle{
      val address = UInt(postTranslationWidth bits)
      val way = UInt(log2Up(wayCount) bits)
      val victim = Bits(writebackCount bits)
      val unique = Bool()
      val data = Bool()
    }).setIdle()

    import spinal.core.sim._
    val pushCounter = Reg(UInt(32 bits)) init(0) simPublic()
    when(push.valid) {pushCounter := pushCounter + 1}

    for (slot <- slots) when(push.valid) {
      when(free(slot.id)) {
        slot.valid := True
        slot.address := push.address
        slot.way := push.way
        slot.cmdSent := False
        slot.priority.setAll()
        slot.loaded := False
        slot.loadedCounter := 0
        slot.victim := push.victim
        slot.writebackHazards := 0
        if(withCoherency) {
          slot.unique := push.unique
          slot.data := push.data
        }
      } otherwise {
        val freeFiltred = free.asBools.patch(slot.id, Nil, 1)
        (slot.priority.asBools, freeFiltred).zipped.foreach(_ clearWhen(_))
      }
    }

    val read = new Area{
      val arbiter = new PriorityArea(slots.map(s => (s.valid && !s.cmdSent && s.victim === 0 && s.writebackHazards === 0, s.priority)))

      val writebackHazards = Bits(writebackCount bits)
      val writebackHazard = writebackHazards.orR
      when(io.mem.read.cmd.fire || writebackHazard){ arbiter.lock := 0 }

      val cmdAddress = slots.map(_.address(tagRange.high downto lineRange.low)).read(arbiter.sel) @@ U(0, lineRange.low bit)
      io.mem.read.cmd.valid := arbiter.hit && !writebackHazard
      io.mem.read.cmd.id := arbiter.sel
      io.mem.read.cmd.address := cmdAddress
      if(withCoherency) {
        io.mem.read.cmd.unique := slots.map(_.unique).read(arbiter.sel)
        io.mem.read.cmd.data := slots.map(_.data).read(arbiter.sel)
      }
      whenMasked(slots, arbiter.oh){slot =>
        slot.writebackHazards := writebackHazards
        slot.cmdSent setWhen(io.mem.read.cmd.ready && !writebackHazard)
      }

      val rspAddress = slots.map(_.address).read(io.mem.read.rsp.id)
      val way = slots.map(_.way).read(io.mem.read.rsp.id)
      val wordIndex = KeepAttribute(Reg(UInt(log2Up(memWordPerLine) bits)) init (0))
      val rspWithData = p.withCoherency.mux(io.mem.read.rsp.withData, True)
      if(withCoherency) assert(!(io.mem.read.rsp.valid && !rspWithData && slots.map(_.data).read(io.mem.read.rsp.id)), "Data cache asked for data but didn't recieved any :(")

      val bankWriteNotif = B(0, bankCount bits).allowOverride()
      for ((bank, bankId) <- banks.zipWithIndex) {
        if (!reducedBankWidth) {
          bankWriteNotif(bankId) := io.mem.read.rsp.valid && rspWithData && way === bankId
          bank.write.valid := bankWriteNotif(bankId)
          bank.write.address := rspAddress(lineRange) @@ wordIndex
          bank.write.data := io.mem.read.rsp.data
        } else {
          val sel = U(bankId) - way
          val groupSel = way(log2Up(bankCount) - 1 downto log2Up(bankCount / memToBankRatio))
          val subSel = sel(log2Up(bankCount / memToBankRatio) - 1 downto 0)
          bankWriteNotif(bankId) := io.mem.read.rsp.valid && rspWithData && groupSel === (bankId >> log2Up(bankCount / memToBankRatio))
          bank.write.valid := bankWriteNotif(bankId)
          bank.write.address := rspAddress(lineRange) @@ wordIndex @@ (subSel)
          bank.write.data := io.mem.read.rsp.data.subdivideIn(bankCount / memToBankRatio slices)(subSel)
        }
        banks(bankId).write.mask := (default -> true)
      }

      val hadError = RegInit(False) setWhen(io.mem.read.rsp.valid && io.mem.read.rsp.error)
      val fire = False
      val reservation = tagsOrStatusWriteArbitration.create(0)
      val faulty = hadError || io.mem.read.rsp.error

      io.refillCompletions := 0
      io.mem.read.rsp.ready := True
      when(io.mem.read.rsp.valid) {
        when(rspWithData) {
          wordIndex := wordIndex + 1
        }
        when(wordIndex === wordIndex.maxValue || !rspWithData) {
          hadError := False
          fire := True
          if(!withCoherency) io.refillCompletions(io.mem.read.rsp.id) := True
          reservation.takeIt()
          waysWrite.mask(way) := True
          waysWrite.address := rspAddress(lineRange)
          waysWrite.tag.fault := faulty
          waysWrite.tag.address := rspAddress(tagRange)
          waysWrite.tag.loaded := True
          if(withCoherency){
            waysWrite.tag.unique := io.mem.read.rsp.unique
          }
          slots.onSel(io.mem.read.rsp.id){ s =>
            s.loaded := True
            if(withCoherency) {
              s.ackValid := True
              s.ackId := io.mem.read.rsp.ackId
            }
          }
        }
      }
    }

    val ackSender = withCoherency generate new Area{
      val ack = cloneOf(io.mem.read.ack)
      val requests = slots.map(_.ackValid)
      val oh = OHMasking.first(requests)
      ack.valid := requests.orR
      ack.ackId := OhMux.or(oh, slots.map(_.ackId))
      when(ack.ready){
        io.refillCompletions.asBools.onMask(oh)(_ := True)
        slots.onMask(oh)(_.ackValid := False)
      }

      val buffer = ack.m2sPipe()
      val counter = Reg(UInt(2 bits)) init(0)
      when(buffer.valid){
        counter := counter + 1
      }
      io.mem.read.ack << buffer.haltWhen(counter =/= 3) //Give some time for the CPU to do forward progress
    }
  }

  val writeback = new Area{
    val slots = for (writebackId <- 0 until writebackCount) yield new Area {
      val id = writebackId
      val fire = False
      val valid = RegInit(False) clearWhen (fire)
      val address = Reg(UInt(postTranslationWidth bits))
      val way = Reg(UInt(log2Up(wayCount) bits))
      val priority = Reg(Bits(writebackCount-1 bits)) //TODO Check it
      val readCmdDone = Reg(Bool())
      val victimBufferReady = Reg(Bool())
      val readRspDone = Reg(Bool())
      val writeCmdDone = Reg(Bool())

      val coherency = withCoherency generate new Area{
        val release = Reg(Bool())
        val dirty = Reg(Bool())
        val fromUnique = Reg(Bool())
        val toShared = Reg(Bool())
        val probeId = Reg(UInt(probeIdWidth bits))
      }

      val free = !valid

      refill.read.writebackHazards(id) := valid && address(refillRange) === refill.read.cmdAddress(refillRange)
      when(fire){ refill.slots.foreach(_.writebackHazards(id) := False) }
    }

    io.writebackBusy := slots.map(_.valid).orR

    def isLineBusy(address : UInt) = False//slots.map(s => s.valid && s.way === way && s.address(lineRange) === address(lineRange)).orR

    val free = B(OHMasking.first(slots.map(_.free)))
    val full = slots.map(!_.free).andR

    val push = Flow(new Bundle{
      val address = UInt(postTranslationWidth bits)
      val way = UInt(log2Up(wayCount) bits)

      //TtoB TtoN BtoN
      val dirty = withCoherency generate Bool()
      val fromUnique = withCoherency generate Bool()
      val toShared = withCoherency generate Bool()
      val release = withCoherency generate Bool()
      val probeId = withCoherency generate UInt(probeIdWidth bits)
    }).setIdle()

    for (slot <- slots) when(push.valid) {
      when(free(slot.id)) {
        slot.valid := True
        slot.address := push.address
        slot.way := push.way

        slot.writeCmdDone := False
        slot.priority.setAll()
        if(withCoherency) {
          slot.coherency.release := push.release
          slot.coherency.dirty := push.dirty
          slot.coherency.fromUnique := push.fromUnique
          slot.coherency.toShared := push.toShared
          slot.coherency.probeId := push.probeId
          slot.readCmdDone := !push.dirty
          slot.readRspDone := !push.dirty
          slot.victimBufferReady := !push.dirty
        } else {
          slot.readCmdDone := False
          slot.readRspDone := False
          slot.victimBufferReady := False
        }
      } otherwise {
        val freeFiltred = free.asBools.patch(slot.id, Nil, 1)
        (slot.priority.asBools, freeFiltred).zipped.foreach(_ clearWhen (_))
      }
    }

    val victimBuffer = Mem.fill(writebackCount*memWordPerLine)(Bits(memDataWidth bits))
    val read = new Area{
      val arbiter = new PriorityArea(slots.map(s => (s.valid && !s.readCmdDone, s.priority)))

      val address = slots.map(_.address).read(arbiter.sel)
      val way = slots.map(_.way).read(arbiter.sel)
      val wordIndex = KeepAttribute(Reg(UInt(log2Up(memWordPerLine) bits)) init (0))

      val slotRead = Flow(new Bundle {
        val id = UInt(log2Up(writebackCount) bits)
        val last = Bool()
        val wordIndex = UInt(log2Up(memWordPerLine) bits)
        val way = UInt(log2Up(wayCount) bits)
      })
      slotRead.valid := arbiter.hit
      slotRead.id := arbiter.sel
      slotRead.wordIndex := wordIndex
      slotRead.way := way
      slotRead.last := wordIndex === wordIndex.maxValue
      wordIndex := wordIndex + U(slotRead.valid)
      when(slotRead.valid && slotRead.last){
        whenMasked(slots, arbiter.oh){ _.readCmdDone := True }
        arbiter.lock := 0
      }
      when(slotRead.fire){
        for(slot <- refill.slots) slot.victim(slotRead.id) := False
      }

      for ((bank, bankId) <- banks.zipWithIndex) {
        if (!reducedBankWidth) {
          when(slotRead.valid && way === bankId) {
            bank.read.cmd.valid := True
            bank.read.cmd.payload := address(lineRange) @@ wordIndex
            bank.read.usedByWriteBack := True
          }
        } else {
          val sel = U(bankId) - way
          val groupSel = way(log2Up(bankCount) - 1 downto log2Up(bankCount / memToBankRatio))
          val subSel = sel(log2Up(bankCount / memToBankRatio) - 1 downto 0)
          when(arbiter.hit && groupSel === (bankId >> log2Up(bankCount / memToBankRatio))) {
            bank.read.cmd.valid := True
            bank.read.cmd.payload := address(lineRange) @@ wordIndex @@ (subSel)
            bank.read.usedByWriteBack := True
          }
        }
      }

      val slotReadLast = slotRead.stage()
      val readedData = Bits(memDataWidth bits)

      if (!reducedBankWidth) {
        readedData := banks.map(_.read.rsp).read(slotReadLast.way)
      } else {
        for((slice, sliceId) <- readedData.subdivideIn(bankWidth bits).zipWithIndex) {
          ???
        }
      }


      when(slotReadLast.valid){
        victimBuffer.write(slotReadLast.id @@ slotReadLast.wordIndex, readedData)
        whenIndexed(slots, slotReadLast.id) { _.victimBufferReady := True }
        when(slotReadLast.last) {
          whenIndexed(slots, slotReadLast.id) { _.readRspDone := True }
        }
      }
    }

    val write = new Area{
      val arbiter = new PriorityArea(slots.map(s => (s.valid && s.victimBufferReady && !s.writeCmdDone, s.priority)))
      val wordIndex = KeepAttribute(Reg(UInt(log2Up(memWordPerLine) bits)) init (0))
      val last = wordIndex === wordIndex.maxValue

      val bufferRead = Stream(new Bundle {
        val id = UInt(log2Up(writebackCount) bits)
        val address = UInt(postTranslationWidth bits)
        val last = Bool()
        val coherency = withCoherency generate new Bundle{
          val release = Bool()
          val dirty = Bool()
          val fromUnique = Bool()
          val toShared   = Bool()
          val probeId = UInt(p.probeIdWidth bits)
        }
      })
      bufferRead.valid := arbiter.hit
      bufferRead.id := arbiter.sel
      bufferRead.last := last
      bufferRead.address := slots.map(_.address).read(arbiter.sel)
      val c = withCoherency generate new Area {
        last setWhen(!bufferRead.coherency.dirty)
        bufferRead.coherency.release := slots.map(_.coherency.release).read(arbiter.sel)
        bufferRead.coherency.dirty := slots.map(_.coherency.dirty).read(arbiter.sel)
        bufferRead.coherency.fromUnique := slots.map(_.coherency.fromUnique).read(arbiter.sel)
        bufferRead.coherency.toShared := slots.map(_.coherency.toShared).read(arbiter.sel)
        bufferRead.coherency.probeId := slots.map(_.coherency.probeId).read(arbiter.sel)
      }
      wordIndex := wordIndex + U(bufferRead.fire && withCoherency.mux(bufferRead.coherency.dirty, True))
      when(bufferRead.fire && last){
        whenMasked(slots, arbiter.oh)(_.writeCmdDone := True)
        arbiter.lock := 0
      }

      val cmd = bufferRead.stage()
      val word = victimBuffer.readSync(bufferRead.id @@ wordIndex, bufferRead.ready)
      io.mem.write.cmd.arbitrationFrom(cmd)
      io.mem.write.cmd.address := cmd.address
      io.mem.write.cmd.data := word
      io.mem.write.cmd.id := cmd.id
      io.mem.write.cmd.last := cmd.last
      if(withCoherency) {
        io.mem.write.cmd.coherent.release := cmd.coherency.release
        io.mem.write.cmd.coherent.dirty := cmd.coherency.dirty
        io.mem.write.cmd.coherent.fromUnique := cmd.coherency.fromUnique
        io.mem.write.cmd.coherent.toShared := cmd.coherency.toShared
        io.mem.write.cmd.coherent.probeId := cmd.coherency.probeId
        when(cmd.fire && cmd.last && !cmd.coherency.release){
          slots.onSel(cmd.id) { s =>
            s.fire := True
          }
        }
      }

      when(io.mem.write.rsp.valid){
        whenIndexed(slots, io.mem.write.rsp.id) { s =>
          s.fire := True
        }
      }
    }
  }

  def isLineBusy(address : UInt) = refill.isLineBusy(address) || writeback.isLineBusy(address)



  def waysHazard(stages : Seq[Stage], address : Stageable[UInt]): Unit ={
    for(s <- stages){
      s.overloaded(WAYS_HAZARD) := s(WAYS_HAZARD) | waysWrite.maskLast.andMask(waysWrite.addressLast === s(address)(lineRange))
    }
  }

  val load = new Area {
    val pipeline = new Pipeline{
      val stages = Array.fill(loadRspAt+1)(newStage())
      connect(stages)(List(M2S()))

      for((stage, stageId) <- stages.zipWithIndex){
        stage.throwIt(io.load.cancels(stageId))
      }
    }

    val readBanksStage  = pipeline.stages(loadReadBanksAt)
    val readTagsStage   = pipeline.stages(loadReadTagsAt)
    val translatedStage = pipeline.stages(loadTranslatedAt)
    val hitsStage       = pipeline.stages(loadHitsAt)
    val hitStage        = pipeline.stages(loadHitAt)
    val bankMuxesStage  = pipeline.stages(loadBankMuxesAt)
    val bankMuxStage    = pipeline.stages(loadBankMuxAt)
    val preControlStage = pipeline.stages(loadControlAt - 1)
    val controlStage    = pipeline.stages(loadControlAt)
    val rspStage        = pipeline.stages(loadRspAt)


    waysHazard((loadReadBanksAt+1 to loadControlAt).map(pipeline.stages(_)), ADDRESS_PRE_TRANSLATION)
    val start = new Area {
      val stage = pipeline.stages.head

      import stage._

      io.load.cmd.ready := True
      isValid := io.load.cmd.valid
      ADDRESS_PRE_TRANSLATION := io.load.cmd.virtual
      REDO_ON_DATA_HAZARD := io.load.cmd.redoOnDataHazard
      WAYS_HAZARD := 0
      UNLOCKED := io.load.cmd.unlocked
      NEED_UNIQUE := io.load.cmd.unique
    }

    val fetch = new Area {
      for ((bank, bankId) <- banks.zipWithIndex) yield new Area {
        {
          import readBanksStage._
          BANK_BUSY(bankId) := bank.read.usedByWriteBack
          when(!BANK_BUSY(bankId)) { //Not the best way of muxing it
            bank.read.cmd.valid := !isStuck
            bank.read.cmd.payload := ADDRESS_PRE_TRANSLATION(lineRange.high downto log2Up(bankWidth / 8))
          }
          overloaded(BANK_BUSY)(bankId) := BANK_BUSY(bankId) || bank.write.valid && REDO_ON_DATA_HAZARD
        }

        {
          val stage = pipeline.stages(loadReadBanksAt + 1)
          import stage._
          BANKS_WORDS(bankId) := banks(bankId).read.rsp

          def wayToBank(way : Int) : UInt = {
            val wayId = U(way, log2Up(wayCount) bits)
            if(!reducedBankWidth) return wayId
            (wayId >> log2Up(bankCount/memToBankRatio)) @@ ((wayId + (ADDRESS_PRE_TRANSLATION(log2Up(bankWidth/8), log2Up(bankCount) bits))).resize(log2Up(bankCount/memToBankRatio)))
          }

          BANK_BUSY_REMAPPED(bankId) := BANK_BUSY(wayToBank(bankId))
        }

        {
          import bankMuxesStage._;
          BANKS_MUXES(bankId) := BANKS_WORDS(bankId).subdivideIn(cpuWordWidth bits).read(ADDRESS_PRE_TRANSLATION(bankWordToCpuWordRange))
        }
      }

      val bankMuxStd = !reducedBankWidth generate new Area {
        import bankMuxStage._
        CPU_WORD := OhMux.or(WAYS_HITS, BANKS_MUXES)
      }

      val bankMuxReduced = reducedBankWidth generate new Area {
        import bankMuxStage._
        val wayId = OHToUInt(WAYS_HITS)
        val bankId = (wayId >> log2Up(bankCount/memToBankRatio)) @@ ((wayId + (ADDRESS_PRE_TRANSLATION(log2Up(bankWidth/8), log2Up(bankCount) bits))).resize(log2Up(bankCount/memToBankRatio)))
        CPU_WORD := BANKS_MUXES.read(bankId) //MuxOH(WAYS_HITS, BANKS_MUXES)
      }


      translatedStage(ADDRESS_POST_TRANSLATION) := io.load.translated.physical
      translatedStage(ABORD) := io.load.translated.abord

      readTagsStage(PLRU) := plru.ram.readAsync(readTagsStage(ADDRESS_PRE_TRANSLATION)(lineRange))

      for ((way, wayId) <- ways.zipWithIndex) yield new Area {
        {
          import readTagsStage._
          way.loadRead.cmd.valid := !isStuck
          way.loadRead.cmd.payload := ADDRESS_PRE_TRANSLATION(lineRange)
        }
        pipeline.stages(loadReadTagsAt + (!tagsReadAsync).toInt)(WAYS_TAGS)(wayId) := ways(wayId).loadRead.rsp;
        {
          import hitsStage._;
          WAYS_HITS(wayId) := WAYS_TAGS(wayId).loaded && WAYS_TAGS(wayId).address === ADDRESS_POST_TRANSLATION(tagRange)
        }
      }

      {
        import hitStage._;
        WAYS_HIT := B(WAYS_HITS).orR
      }


      status.loadRead.cmd.valid := !readTagsStage.isStuck
      status.loadRead.cmd.payload := readTagsStage(ADDRESS_PRE_TRANSLATION)(lineRange)
      pipeline.stages(loadReadTagsAt + (!tagsReadAsync).toInt)(STATUS) := status.loadRead.rsp

      val statusBypassOn = (loadReadTagsAt + (!tagsReadAsync).toInt until loadControlAt).map(pipeline.stages(_))
      statusBypassOn.foreach(stage => status.bypass(stage, ADDRESS_POST_TRANSLATION, stage == statusBypassOn.head))
    }


    val refillCheckEarly = loadRefillCheckEarly generate new Area{
      val stage = pipeline.stages(loadControlAt-1)
      import stage._

      REFILL_HITS_EARLY := B(refill.slots.map(r => r.valid && r.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)))
      val refillPushHit = refill.push.valid && refill.push.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)
      when(refillPushHit){
        whenMasked(REFILL_HITS_EARLY.asBools, refill.free)(_ := True)
      }

      controlStage(REFILL_HITS) := controlStage(REFILL_HITS_EARLY) & refill.slots.map(_.valid).asBits()
    }

    val refillCheckLate = !loadRefillCheckEarly generate new Area{
      import controlStage._
      REFILL_HITS := B(refill.slots.map(r => r.valid && r.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)))
    }

    preControlStage(LOCKED) := !preControlStage(UNLOCKED) && io.lock.valid && io.lock.address(lineRange) === preControlStage(ADDRESS_PRE_TRANSLATION)(lineRange)

    val ctrl = new Area {
      import controlStage._

      val plruLogic = new Area {
        val core = new Plru(wayCount, false)
        core.io.context.state := PLRU
        core.io.update.id.assignDontCare()

        plru.fromLoad.valid := False
        plru.fromLoad.address := ADDRESS_PRE_TRANSLATION(lineRange)
        plru.fromLoad.data := core.io.update.state
      }

      val reservation = tagsOrStatusWriteArbitration.create(2)
      val refillWay = CombInit(plruLogic.core.io.evict.id)
//      val refillWay = CombInit(wayRandom.value)
      val refillWayNeedWriteback = WAYS_TAGS(refillWay).loaded && withCoherency.mux(True, STATUS(refillWay).dirty)
      val refillHit = REFILL_HITS.orR
      val refillLoaded = (B(refill.slots.map(_.loaded)) & REFILL_HITS).orR
      val lineBusy = isLineBusy(ADDRESS_PRE_TRANSLATION)
      val bankBusy = (BANK_BUSY_REMAPPED & WAYS_HITS) =/= 0
      val waysHitHazard = (WAYS_HITS & resulting(WAYS_HAZARD)).orR
      val hitUnique = p.withCoherency.mux((WAYS_HITS & WAYS_TAGS.map(_.unique).asBits).orR, True)
      val uniqueMiss = NEED_UNIQUE && !hitUnique

      REDO := !WAYS_HIT || waysHitHazard || bankBusy || refillHit || LOCKED || uniqueMiss
      MISS := !WAYS_HIT && !waysHitHazard && !refillHit && !LOCKED
      FAULT := (WAYS_HITS & WAYS_TAGS.map(_.fault).asBits).orR
      val canRefill = !refill.full && !lineBusy && reservation.win && !(refillWayNeedWriteback && writeback.full) && !resulting(WAYS_HAZARD)(refillWay)
      val askRefill = MISS && canRefill && !refillHit
      val askUpgrade = !MISS && canRefill && uniqueMiss
      val startRefill = isValid && askRefill
      val startUpgrade = isValid && askUpgrade
      val wayId = OHToUInt(WAYS_HITS)

      when(ABORD){
        REDO := False
        MISS := False
        askUpgrade := False
      }

      when(startRefill || startUpgrade){
        reservation.takeIt()

        refill.push.valid := True
        refill.push.address := ADDRESS_POST_TRANSLATION
        refill.push.unique := NEED_UNIQUE
        refill.push.data := askRefill
      }

      when(askUpgrade){
        refill.push.way := wayId
        refill.push.victim := 0
      } otherwise {
        refill.push.way := refillWay
        refill.push.victim := writeback.free.andMask(refillWayNeedWriteback && STATUS(refillWay).dirty)
      }

      when(startRefill){
        status.write.valid := True
        status.write.address := ADDRESS_PRE_TRANSLATION(lineRange)
        status.write.data := STATUS
        status.write.data(refillWay).dirty := False

        waysWrite.mask(refillWay) := True
        waysWrite.address := ADDRESS_PRE_TRANSLATION(lineRange)
        waysWrite.tag.loaded := False

        writeback.push.valid := refillWayNeedWriteback
        writeback.push.address := (WAYS_TAGS(refillWay).address @@ ADDRESS_PRE_TRANSLATION(lineRange)) << lineRange.low
        writeback.push.way := refillWay
        if(withCoherency) {
          writeback.push.dirty := STATUS(refillWay).dirty
          writeback.push.fromUnique := WAYS_TAGS(refillWay).unique
          writeback.push.toShared := False
          writeback.push.release := True
        }

        plru.fromLoad.valid := True
        plruLogic.core.io.update.id := refillWay
      }

      when(isValid && !REDO && !MISS){
        plru.fromLoad.valid := True
        plruLogic.core.io.update.id := wayId
      }

      REFILL_SLOT_FULL := MISS && !refillHit && refill.full
      REFILL_SLOT := REFILL_HITS.andMask(!refillLoaded) | refill.free.andMask(askRefill)
    }

    val inject = new Area {
      import rspStage._

      io.load.rsp.valid := isValid
      io.load.rsp.data := CPU_WORD
      io.load.rsp.fault := FAULT
      io.load.rsp.redo  := REDO

      (loadRspAt-loadControlAt) match {
        case 0 =>{
          io.load.rsp.refillSlotAny := REFILL_SLOT_FULL
          io.load.rsp.refillSlot := REFILL_SLOT
        }
        case 1 => {
          io.load.rsp.refillSlotAny := REFILL_SLOT_FULL && !io.refillCompletions.orR
          io.load.rsp.refillSlot    := REFILL_SLOT & io.refillCompletions
        }
      }
    }

    pipeline.build()
  }

  val store = new Area{
    val pipeline = new Pipeline{
      val stages = Array.fill(storeRspAt+1)(newStage())
      connect(stages)(List(M2S()))

      val discardAll = False
      for((stage, stageId) <- stages.zipWithIndex){
        stage.throwIt(discardAll)
      }
    }

    val readBanksStage      = pipeline.stages(storeReadBanksAt)
    val readTagsStage      = pipeline.stages(storeReadTagsAt)
    val hitsStage      = pipeline.stages(storeHitsAt)
    val hitStage       = pipeline.stages(storeHitAt)
    val controlStage   = pipeline.stages(storeControlAt)
    val rspStage       = pipeline.stages(storeRspAt)

    val target = RegInit(False)

    waysHazard((storeReadBanksAt+1 to storeControlAt).map(pipeline.stages(_)), ADDRESS_POST_TRANSLATION)
    val start = new Area {
      val stage = pipeline.stages.head

      import stage._

      isValid := io.store.cmd.valid
      ADDRESS_POST_TRANSLATION := io.store.cmd.address
      CPU_WORD := io.store.cmd.data
      CPU_MASK := io.store.cmd.mask
      IO := io.store.cmd.io && !io.store.cmd.flush
      FLUSH := io.store.cmd.flush
      FLUSH_FREE := io.store.cmd.flushFree
      PREFETCH := io.store.cmd.prefetch

      GENERATION := io.store.cmd.generation
      WAYS_HAZARD := 0

      io.store.cmd.ready := True
      if(withCoherency) {
        PROBE := io.mem.probe.cmd.valid
        ALLOW_SHARED := io.mem.probe.cmd.allowShared
        ALLOW_UNIQUE := io.mem.probe.cmd.allowUnique
        ALLOW_PROBE_DATA := io.mem.probe.cmd.getDirtyData
        PROBE_ID := io.mem.probe.cmd.id
        when(io.mem.probe.cmd.valid){
          io.store.cmd.ready := False
          isValid := True
          ADDRESS_POST_TRANSLATION := io.mem.probe.cmd.address
          IO := False
          FLUSH := False
          FLUSH_FREE := False
          PREFETCH := False
        }
      }
    }

    val fetch = new Area {
      readTagsStage(PLRU) := plru.ram.readAsync(readTagsStage(ADDRESS_POST_TRANSLATION)(lineRange))

      for ((way, wayId) <- ways.zipWithIndex) yield new Area {
        {
          import readTagsStage._
          way.storeRead.cmd.valid := !isStuck
          way.storeRead.cmd.payload := ADDRESS_POST_TRANSLATION(lineRange)
        }
        pipeline.stages(storeReadTagsAt + (!tagsReadAsync).toInt)(WAYS_TAGS)(wayId) := ways(wayId).storeRead.rsp;
        {
          import hitsStage._;
          WAYS_HITS(wayId) := WAYS_TAGS(wayId).loaded && WAYS_TAGS(wayId).address === ADDRESS_POST_TRANSLATION(tagRange)
        }
      }

      {
        import hitStage._;
        WAYS_HIT := B(WAYS_HITS).orR
      }

      status.storeRead.cmd.valid := !readTagsStage.isStuck
      status.storeRead.cmd.payload := readTagsStage(ADDRESS_POST_TRANSLATION)(lineRange)
      pipeline.stages(storeReadTagsAt + (!tagsReadAsync).toInt)(STATUS) := status.storeRead.rsp


      val statusBypassOn = (storeReadTagsAt + (!tagsReadAsync).toInt until storeControlAt).map(pipeline.stages(_))
      statusBypassOn.foreach(stage => status.bypass(stage, ADDRESS_POST_TRANSLATION,  stage == statusBypassOn.head))
    }

    val refillCheckEarly = storeRefillCheckEarly generate new Area{
      val stage = pipeline.stages(storeControlAt-1)
      import stage._

      REFILL_HITS_EARLY := B(refill.slots.map(r => r.valid && r.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)))
      val refillPushHit = refill.push.valid && refill.push.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)
      when(refillPushHit){
        whenMasked(REFILL_HITS_EARLY.asBools, refill.free)(_ := True)
      }

      controlStage(REFILL_HITS) := controlStage(REFILL_HITS_EARLY) & refill.slots.map(_.valid).asBits()
    }

    val refillCheckLate = !storeRefillCheckEarly generate new Area{
      import controlStage._
      REFILL_HITS := B(refill.slots.map(r => r.valid && r.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)))
    }

    val ctrl = new Area {
      import controlStage._
      if(!withCoherency) PROBE := False


      val plruLogic = new Area {
        val core = new Plru(wayCount, false)
        core.io.context.state := PLRU
        core.io.update.id.assignDontCare()

        plru.fromStore.valid := False
        plru.fromStore.address := ADDRESS_POST_TRANSLATION(lineRange)
        plru.fromStore.data := core.io.update.state
      }

      GENERATION_OK := GENERATION === target || PREFETCH || PROBE

      val reservation = tagsOrStatusWriteArbitration.create(3)
      val replacedWay = CombInit(plruLogic.core.io.evict.id)
//      val replacedWay = CombInit(wayRandom.value)
      val replacedWayNeedWriteback = WAYS_TAGS(replacedWay).loaded && withCoherency.mux(True, STATUS(replacedWay).dirty)
      val refillHit = (REFILL_HITS & B(refill.slots.map(_.valid))).orR
      val lineBusy = isLineBusy(ADDRESS_POST_TRANSLATION)
      val waysHitHazard = (WAYS_HITS & resulting(WAYS_HAZARD)).orR
      val wasClean = !(B(STATUS.map(_.dirty)) & WAYS_HITS).orR
      val bankBusy = !FLUSH && !PREFETCH && !PROBE && (WAYS_HITS & refill.read.bankWriteNotif).orR
      val hitUnique = withCoherency.mux((WAYS_HITS & B(WAYS_TAGS.map(_.unique))).orR, True)
      val hitFault = (WAYS_HITS & B(WAYS_TAGS.map(_.fault))).orR
      val refillWay = CombInit(replacedWay)

      REDO := MISS || waysHitHazard || bankBusy || refillHit || (wasClean && !reservation.win) || !hitUnique
      MISS := !WAYS_HIT && !waysHitHazard && !refillHit

      val canRefill = !refill.full && !lineBusy && !load.ctrl.startRefill && reservation.win && !resulting(WAYS_HAZARD)(refillWay)
      val askRefill = MISS && canRefill && !refillHit && !(replacedWayNeedWriteback && writeback.full)
      val askUpgrade = !MISS && canRefill && !hitUnique
      val startRefill = isValid && GENERATION_OK && askRefill
      val startUpgrade = isValid && GENERATION_OK && askUpgrade

      REFILL_SLOT_FULL := MISS && !refillHit && refill.full
      REFILL_SLOT := refill.free.andMask(askRefill || askUpgrade)

      val writeCache = isValid && GENERATION_OK && !REDO && !PREFETCH && !PROBE
      val setDirty = writeCache && wasClean
      val wayId = OHToUInt(WAYS_HITS)
      val bankHitId = if(!reducedBankWidth) wayId else (wayId >> log2Up(bankCount/memToBankRatio)) @@ ((wayId + (ADDRESS_POST_TRANSLATION(log2Up(bankWidth/8), log2Up(bankCount) bits))).resize(log2Up(bankCount/memToBankRatio)))

      //Only valid for FLUSH === TRUE
      val needFlushs = B(WAYS_TAGS.map(_.loaded)) & B(STATUS.map(_.dirty))
      val needFlushOh = OHMasking.firstV2(needFlushs)
      val needFlushSel = OHToUInt(needFlushOh)
      val needFlush = needFlushs.orR
      val canFlush = reservation.win && !writeback.full && !refill.slots.map(_.valid).orR && !resulting(WAYS_HAZARD).orR
      val startFlush = isValid && FLUSH && GENERATION_OK && needFlush && canFlush


      when(!MISS && !hitUnique){
        refillWay := wayId
      }

      when(FLUSH){
        REDO := needFlush || resulting(WAYS_HAZARD).orR
        setDirty := False
        writeCache := False
        startRefill := False
        startUpgrade := False
      }

      when(IO){
        REDO := False
        MISS := False
        setDirty := False
        writeCache := False
        startUpgrade := False
      }

      when(startRefill || startUpgrade || setDirty || startFlush){
        reservation.takeIt()
        status.write.valid := True
        status.write.address := ADDRESS_POST_TRANSLATION(lineRange)
        status.write.data := STATUS
      }

      when(startRefill || startFlush){
        writeback.push.valid := replacedWayNeedWriteback || startFlush
        writeback.push.address := (WAYS_TAGS(writeback.push.way).address @@ ADDRESS_POST_TRANSLATION(lineRange)) << lineRange.low
        writeback.push.way := FLUSH ? needFlushSel | refillWay
        if(withCoherency) {
          writeback.push.dirty := STATUS(refillWay).dirty
          writeback.push.fromUnique := WAYS_TAGS(refillWay).unique
          writeback.push.toShared := False
          writeback.push.release := True
          when(startFlush){
            writeback.push.toShared := True
            writeback.push.dirty := STATUS(needFlushSel).dirty
            writeback.push.fromUnique := WAYS_TAGS(needFlushSel).unique
            status.write.data.onSel(needFlushSel)(_.dirty := False)
          }
        }
      }

      when(startRefill || startUpgrade){
        refill.push.valid := True
        refill.push.address := ADDRESS_POST_TRANSLATION
        refill.push.way := refillWay
        refill.push.victim := writeback.free.andMask(replacedWayNeedWriteback && askRefill && STATUS(refillWay).dirty)
        refill.push.unique := True
        refill.push.data := askRefill

        waysWrite.mask(refillWay) := True
        waysWrite.address := ADDRESS_POST_TRANSLATION(lineRange)
        waysWrite.tag.loaded := False

        whenIndexed(status.write.data, refillWay)(_.dirty := False)
      }

      when(startRefill){
        plru.fromStore.valid := True
        plruLogic.core.io.update.id := refillWay
      }

      when(writeCache){
        plru.fromStore.valid := True
        plruLogic.core.io.update.id := wayId

        for((bank, bankId) <- banks.zipWithIndex) when(WAYS_HITS(bankId)){
          bank.write.valid := bankId === bankHitId
          bank.write.address := ADDRESS_POST_TRANSLATION(lineRange.high downto log2Up(bankWidth / 8))
          bank.write.data.subdivideIn(cpuWordWidth bits).foreach(_ := CPU_WORD)
          bank.write.mask := 0
          bank.write.mask.subdivideIn(cpuWordWidth/8 bits)(ADDRESS_POST_TRANSLATION(bankWordToCpuWordRange)) := CPU_MASK
        }
      }
      when(setDirty){
        whenMasked(status.write.data, WAYS_HITS)(_.dirty := True)
      }
      when(startFlush){
        whenMasked(status.write.data, needFlushOh)(_.dirty := False)
        when(FLUSH_FREE) {
          whenMasked(waysWrite.mask.asBools, needFlushOh)(_ := True)
        }
        waysWrite.address := ADDRESS_POST_TRANSLATION(lineRange)
        if(withCoherency) when(startFlush) {
          waysWrite.mask(needFlushSel) := True
          waysWrite.tag.loaded := True
          waysWrite.tag.fault := B(WAYS_TAGS.map(_.fault))(needFlushSel)
          waysWrite.tag.unique := False
          waysWrite.tag.address := WAYS_TAGS(writeback.push.way).address
        } else {
          waysWrite.tag.loaded := False
        }
      }



      when(isValid && REDO && GENERATION_OK && !PREFETCH && !PROBE){
        target := !target
      }

      val snoop = withCoherency generate new Area {
        val askSomething = PROBE && WAYS_HIT
        val askWriteback = !wasClean && !ALLOW_UNIQUE && ALLOW_PROBE_DATA
        val askTagUpdate = (!ALLOW_SHARED || !ALLOW_UNIQUE && hitUnique)
        val canUpdateTag = !(askSomething && askTagUpdate && !reservation.win)
        val canWriteback =  !(askSomething && askWriteback && (!reservation.win || writeback.full))
        val alreadyInWb = writeback.slots.map(slot => slot.valid && slot.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange)).orR
        val isUnique = (WAYS_TAGS.map(_.unique).asBits() & WAYS_HITS).orR
        val locked = io.lock.valid && io.lock.address(refillRange) === ADDRESS_POST_TRANSLATION(refillRange) && isUnique
        val success = !waysHitHazard && canUpdateTag && canWriteback && !alreadyInWb && !locked

        val didTagUpdate = RegNext(False) init(False)
        io.writebackBusy setWhen(didTagUpdate)

        //Disable side-effects of the regular store pipeline
        when(PROBE){
          askRefill := False
          askUpgrade := False
        }

        when(isValid && askSomething) {
          when(askWriteback || askTagUpdate) {
            reservation.takeIt()
          }

          when(success) {
            when(askWriteback || askTagUpdate) {
              reservation.takeIt()

              waysWrite.mask(wayId) := True
              waysWrite.address := ADDRESS_POST_TRANSLATION(lineRange)
              waysWrite.tag.loaded := ALLOW_SHARED || ALLOW_UNIQUE
              waysWrite.tag.fault := hitFault
              waysWrite.tag.unique := hitUnique && ALLOW_UNIQUE
              waysWrite.tag.address := ADDRESS_POST_TRANSLATION(tagRange)

              didTagUpdate := True
            }

            when(askWriteback) {
              writeback.push.valid := True
              writeback.push.address := ADDRESS_POST_TRANSLATION
              writeback.push.way := wayId
              writeback.push.dirty := (STATUS.map(_.dirty).asBits() & WAYS_HITS).orR
              writeback.push.fromUnique := isUnique
              writeback.push.toShared := ALLOW_SHARED
              writeback.push.release := False
              writeback.push.probeId := PROBE_ID

              status.write.valid := True
              status.write.address := ADDRESS_POST_TRANSLATION(lineRange)
              status.write.data := STATUS
              whenMasked(status.write.data, WAYS_HITS)(_.dirty := False)
            }
          }
        }

        io.mem.probe.rsp.valid  := isValid && PROBE
        io.mem.probe.rsp.toShared := WAYS_HIT && ALLOW_SHARED && !(ALLOW_UNIQUE && hitUnique)
        io.mem.probe.rsp.toUnique := WAYS_HIT && ALLOW_UNIQUE && hitUnique
        io.mem.probe.rsp.fromUnique := WAYS_HIT &&  WAYS_TAGS(refillWay).unique
        io.mem.probe.rsp.fromShared := WAYS_HIT && !WAYS_TAGS(refillWay).unique
        io.mem.probe.rsp.address := ADDRESS_POST_TRANSLATION
        io.mem.probe.rsp.id := PROBE_ID
        io.mem.probe.rsp.redo   := !success
        io.mem.probe.rsp.allowUnique := ALLOW_UNIQUE
        io.mem.probe.rsp.allowShared := ALLOW_SHARED
        io.mem.probe.rsp.getDirtyData := ALLOW_PROBE_DATA
        io.mem.probe.rsp.writeback := askWriteback
      }
    }

    val inject = new Area {
      import rspStage._

      assert(rspStage == controlStage, "Need to implement refillSlot bypass otherwise")
      io.store.rsp.valid := isValid && !PROBE
      io.store.rsp.generationKo := !GENERATION_OK
      io.store.rsp.fault := False //TODO
      io.store.rsp.redo := REDO
      io.store.rsp.refillSlotAny := REFILL_SLOT_FULL
      io.store.rsp.refillSlot := REFILL_SLOT
      io.store.rsp.flush := FLUSH
      io.store.rsp.prefetch := PREFETCH
      io.store.rsp.address := ADDRESS_POST_TRANSLATION
      io.store.rsp.io := IO
    }
    pipeline.build()
  }

//  when(waysWrite.mask === 8 && waysWrite.address===0x1f){
//    report(L"GOT IT at $REPORT_TIME")
//  }
  io.refillEvent := refill.push.valid
  io.writebackEvent := writeback.push.valid
  io.tagEvent := waysWrite.mask.orR
}
