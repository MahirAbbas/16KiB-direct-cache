file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala
### java.lang.NullPointerException: Cannot invoke "scala.reflect.internal.Types$Type.typeSymbol()" because "tp" is null

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.12.18
Classpath:
<WORKSPACE>/.bloop/projectname/bloop-bsp-clients-classes/classes-Metals-LYD5-QbgTsOdhROfVJpzIw== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.9.9/semanticdb-javac-0.9.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.18/scala-library-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-core_2.12/1.10.1/spinalhdl-core_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-lib_2.12/1.10.1/spinalhdl-lib_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-plugin_2.12/1.10.1/spinalhdl-idsl-plugin_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-sim_2.12/1.10.1/spinalhdl-sim_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalactic/scalactic_2.12/3.2.10/scalactic_2.12-3.2.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.12.18/scala-reflect-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.12/4.1.0/scopt_2.12-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.12/0.3.0/sourcecode_2.12-0.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-compiler/2.12.18/scala-compiler-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-payload_2.12/1.10.1/spinalhdl-idsl-payload_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/openhft/affinity/3.23.2/affinity-3.23.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.5/slf4j-simple-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/oshi/oshi-core/6.4.0/oshi-core-6.4.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.12/2.1.0/scala-xml_2.12-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.5/slf4j-api-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.12.1/jna-5.12.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.12.1/jna-platform-5.12.1.jar [exists ]
Options:
-Yrangepos -Xplugin-require:semanticdb


action parameters:
offset: 794
uri: file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala
text:
```scala
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

  override def @@


}

case class DataStoreCmd(datawidth : Int) {
  val address = UInt(datawidth bits)
  val data = Bits(datawidth bits)
  
}

case class DataStoreRsp(datawidth : Int) {
  val address = UInt(datawidth bits)
  val success = Bool()
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

```



#### Error stacktrace:

```
scala.reflect.internal.Definitions$DefinitionsClass.isByNameParamType(Definitions.scala:424)
	scala.reflect.internal.TreeInfo.isStableIdent(TreeInfo.scala:140)
	scala.reflect.internal.TreeInfo.isStableIdentifier(TreeInfo.scala:113)
	scala.reflect.internal.TreeInfo.isPath(TreeInfo.scala:102)
	scala.reflect.internal.TreeInfo.admitsTypeSelection(TreeInfo.scala:158)
	scala.tools.nsc.interactive.Global.stabilizedType(Global.scala:960)
	scala.tools.nsc.interactive.Global.typedTreeAt(Global.scala:808)
	scala.meta.internal.pc.AutoImportsProvider.autoImports(AutoImportsProvider.scala:26)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$autoImports$1(ScalaPresentationCompiler.scala:281)
```
#### Short summary: 

java.lang.NullPointerException: Cannot invoke "scala.reflect.internal.Types$Type.typeSymbol()" because "tp" is null