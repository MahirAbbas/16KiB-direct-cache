error id: file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:[458..459) in Input.VirtualFile("file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala", "import spinal.core._
import spinal.lib._


trait L1CacheParams {
    def nSets:              Int
    def nWays:              Int
    def blockSizeBytes :    Int
    def blockSizeWords :    Int 
    

    // protected def assertBlockSize: Unit = {
    //     assert(blockSizeBytes*4 == (blockSizeWords))
    // }
}

trait HasL1CacheParameters {
    val cacheParams : L1CacheParams
    
    def nSets = cacheParams.nSets
    // def blockOffsetBits = 
    def 
}

")
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala:23: error: expected identifier; obtained rbrace
}
^
#### Short summary: 

expected identifier; obtained rbrace