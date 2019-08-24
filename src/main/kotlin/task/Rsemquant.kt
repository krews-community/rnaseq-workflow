package task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import model.MergedFastqReplicate
import model.MergedFastqReplicatePE
import model.MergedFastqReplicateSE
import org.reactivestreams.Publisher


data class RsemQuantParams(
        val rsemindex: File,
        val read_strand:String = "Unstranded",
        val rndSeed:Int = 12345
)

data class RsemQuantInput(
        val bamFile: File,
        val repName:String,
        val pairedEnd: Boolean
)

data class  RsemQuantOutput(
        val genesFile: File,
        val isoformsFile: File,
        val repName: String

)

fun WorkflowBuilder.rsemquantTask(name: String, i: Publisher< RsemQuantInput>) = this.task<RsemQuantInput,  RsemQuantOutput>(name, i) {
    val params = taskParams<RsemQuantParams>()

    dockerImage = "genomealmanac/rnaseq-rsemquant:v1.0.0"

    val prefix = "rsemquant/${input.repName}"
    output =
            RsemQuantOutput(
                    repName = input.repName,
                    genesFile =  OutputFile("${prefix}_rsem.genes.results") ,
                    isoformsFile =OutputFile("${prefix}_rsem.isoforms.results")
            )

    command =
            """
          java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -jar /app/rnaseq.jar \
                -annobam ${input.bamFile.dockerPath} \
                -rsemindex ${params.rsemindex.dockerPath} \
                -outputDir ${outputsDir}/rsemquant \
                -outputPrefix ${input.repName} \
                -readstrand ${params.read_strand} \
                -randomseed ${params.rndSeed} \
               ${if (input.pairedEnd) "-pairedEnd" else ""}
            """
}