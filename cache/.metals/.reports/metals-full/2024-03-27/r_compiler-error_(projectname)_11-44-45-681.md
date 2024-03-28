file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
### java.lang.StringIndexOutOfBoundsException: offset 836, count -6, length 1475

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.12.18
Classpath:
<WORKSPACE>/.bloop/projectname/bloop-bsp-clients-classes/classes-Metals-jIMB_bfaQ0aBcEmQULPxiA== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.9.9/semanticdb-javac-0.9.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.18/scala-library-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-core_2.12/1.10.1/spinalhdl-core_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-lib_2.12/1.10.1/spinalhdl-lib_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-plugin_2.12/1.10.1/spinalhdl-idsl-plugin_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-sim_2.12/1.10.1/spinalhdl-sim_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scalactic/scalactic_2.12/3.2.10/scalactic_2.12-3.2.10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.12.18/scala-reflect-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.12/4.1.0/scopt_2.12-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.12/0.3.0/sourcecode_2.12-0.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-compiler/2.12.18/scala-compiler-2.12.18.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/spinalhdl/spinalhdl-idsl-payload_2.12/1.10.1/spinalhdl-idsl-payload_2.12-1.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/openhft/affinity/3.23.2/affinity-3.23.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.5/slf4j-simple-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/oshi/oshi-core/6.4.0/oshi-core-6.4.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.12/2.1.0/scala-xml_2.12-2.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.5/slf4j-api-2.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna/5.12.1/jna-5.12.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/java/dev/jna/jna-platform/5.12.1/jna-platform-5.12.1.jar [exists ]
Options:
-Yrangepos -Xplugin-require:semanticdb


action parameters:
offset: 848
uri: file://<WORKSPACE>/hw/spinal/projectname/L1HellaCache.scala
text:
```scala
import spinal.core._
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

case class DataLoadCmd(addressWidth : Int) extends Bundle {
   val address = UInt(addressWidth bits) 
}
case class DataLoadRsp(addressWidth : Int) extends Bundle {
   val data = Bits(addressWidth bits) 
}
case class DataStoreCmd(addressWidth : Int) extends Bundle {
    val address = UInt(addressWidth bits)
    val data = Bits(addressWidth bits)
}
case class DataStoreRsp(addressWidth : Int) extends Bundle {
    val address = UInt(addressWidth @@)
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


case class L1CacheIO(addressWidth : Int) extends Bundle with IMasterSlave {
    
}
case class L1CacheComponent() extends Component with L1CacheParameters {

}


```



#### Error stacktrace:

```
java.base/java.lang.String.checkBoundsOffCount(String.java:3304)
	java.base/java.lang.String.rangeCheck(String.java:280)
	java.base/java.lang.String.<init>(String.java:276)
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

java.lang.StringIndexOutOfBoundsException: offset 836, count -6, length 1475