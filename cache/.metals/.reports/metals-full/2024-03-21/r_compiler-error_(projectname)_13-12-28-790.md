file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala
### java.lang.StringIndexOutOfBoundsException: offset 431, count -6, length 2386

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.12.18
Classpath:
<WORKSPACE>/.bloop/projectname/bloop-bsp-clients-classes/classes-Metals-4hlvI7BATDKt4kQOyDS63Q== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.9.9/semanticdb-javac-0.9.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.18/scala-library-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-core_2.12/1.10.1/spinalhdl-core_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-lib_2.12/1.10.1/spinalhdl-lib_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-plugin_2.12/1.10.1/spinalhdl-idsl-plugin_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-sim_2.12/1.10.1/spinalhdl-sim_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalactic/scalactic_2.12/3.2.10/scalactic_2.12-3.2.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.12.18/scala-reflect-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.12/4.1.0/scopt_2.12-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.12/0.3.0/sourcecode_2.12-0.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-compiler/2.12.18/scala-compiler-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-payload_2.12/1.10.1/spinalhdl-idsl-payload_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/openhft/affinity/3.23.2/affinity-3.23.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.5/slf4j-simple-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/oshi/oshi-core/6.4.0/oshi-core-6.4.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.12/2.1.0/scala-xml_2.12-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.5/slf4j-api-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.12.1/jna-5.12.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.12.1/jna-platform-5.12.1.jar [exists ]
Options:
-Yrangepos -Xplugin-require:semanticdb


action parameters:
offset: 440
uri: file://<WORKSPACE>/hw/spinal/projectname/DataCache.scala
text:
```scala
package projectname

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._


case class DataLoadPort() extends Bundle with IMasterSlave {

    val address = in port UInt(32 bits)
    val request_data = in port Bits(32 bits) // request data


}

case class DataLoadCmd(datawidth : Int) extends Bundle {
  val address = UInt(datawidth bits)

}

case class DataLoadRsp(datawidth : Int) extends Bundle {

  val data = Bits(datawidth @@)
  
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

```



#### Error stacktrace:

```
java.base/java.lang.String.checkBoundsOffCount(String.java:4586)
	java.base/java.lang.String.rangeCheck(String.java:304)
	java.base/java.lang.String.<init>(String.java:300)
	scala.tools.nsc.interactive.Global.typeCompletions$1(Global.scala:1231)
	scala.tools.nsc.interactive.Global.completionsAt(Global.scala:1254)
	scala.meta.internal.pc.SignatureHelpProvider.$anonfun$treeSymbol$1(SignatureHelpProvider.scala:390)
	scala.Option.map(Option.scala:230)
	scala.meta.internal.pc.SignatureHelpProvider.treeSymbol(SignatureHelpProvider.scala:388)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCall$.unapply(SignatureHelpProvider.scala:205)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCallTraverser.visit(SignatureHelpProvider.scala:316)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCallTraverser.traverse(SignatureHelpProvider.scala:310)
	scala.meta.internal.pc.SignatureHelpProvider$MethodCallTraverser.fromTree(SignatureHelpProvider.scala:279)
	scala.meta.internal.pc.SignatureHelpProvider.signatureHelp(SignatureHelpProvider.scala:27)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$signatureHelp$1(ScalaPresentationCompiler.scala:310)
```
#### Short summary: 

java.lang.StringIndexOutOfBoundsException: offset 431, count -6, length 2386