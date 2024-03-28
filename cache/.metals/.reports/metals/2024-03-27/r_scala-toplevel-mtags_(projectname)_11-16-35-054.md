error id: file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:[415..415) in Input.VirtualFile("file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala", "import spinal.core._
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

case class 

")
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:22: error: expected identifier; obtained eof

^
#### Short summary: 

expected identifier; obtained eof