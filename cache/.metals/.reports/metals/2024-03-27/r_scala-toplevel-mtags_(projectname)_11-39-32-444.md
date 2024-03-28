error id: file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:[872..876) in Input.VirtualFile("file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala", "import spinal.core._
import spinal.lib._


trait L1CacheParameters {
    
    def nSets : Int
    def nWays : Int
    def blockSizeWords : Int
    def addressBits : Int
    def blockOffsetBits = log2Up(blockSizeWords)
    def indxBits = log2Up(nSets)
    def untagBits = blockOffsetBits + indxBits
    def tagBits = addressBits - untagBits
    def wayBits = log2Up(nWays)
    def isDM = nWays == 1

}

case class DataLoadPort(addressWidth : Int) extends Bundle with IMasterSlave {
    
    val cmd = Stream(UInt(addressWidth bits)) // Address
    val rsp = Flow(Bits(addressWidth bits)) // Bits (data)
    // add signal for stall in case cache doesnt have data?
    
    override def asMaster(): Unit = {
        master(cmd)
        slave(rsp)
    }
}

case class DataStorePort(addressWidth : Int) extends Bundle with IMasterSlave {
    val cmd = Stream()
}


case class 
case class L1CacheComponent() extends Component with L1CacheParameters {

}

")
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:38: error: expected identifier; obtained case
case class L1CacheComponent() extends Component with L1CacheParameters {
^
#### Short summary: 

expected identifier; obtained case