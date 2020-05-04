package task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import model.MergedFastqReplicate
import model.MergedFastqReplicatePE
import model.MergedFastqReplicateSE
import org.reactivestreams.Publisher


data class BamtoSignalParams(
        val chrsz: File,
        val strandedness:String = "Stranded"
)

data class BamtoSignalInput(
        val bamFile: File,
        val repName:String
)

data class  BamtoSignalOutput(
        val repName: String,
        val minusAll: File?,
        val minusUniq: File?,
        val All: File,
        val Uniq: File

)

fun WorkflowBuilder.bamtosignalTask(name: String, i: Publisher< BamtoSignalInput>) = this.task<BamtoSignalInput,  BamtoSignalOutput>(name, i) {
    val params = taskParams<BamtoSignalParams>()

    dockerImage = "genomealmanac/rnaseq-bamtosignal:v1.0.0"

    val prefix = "bamtosignal/${input.repName}"
    output =
            BamtoSignalOutput(
                    repName = input.repName,
                    minusAll = if(params.strandedness=="Unstranded") null else OutputFile("${prefix}_minusAll.bw") ,
                    minusUniq = if(params.strandedness=="Unstranded") null else OutputFile("${prefix}_minusUniq.bw") ,
                    All =  if(params.strandedness=="Unstranded") OutputFile("${prefix}_All.bw") else OutputFile("${prefix}_plusAll.bw"),
                    Uniq =  if(params.strandedness=="Unstranded") OutputFile("${prefix}_Uniq.bw") else OutputFile("${prefix}_plusUniq.bw")
            )

    command =
            """
          java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -jar /app/rnaseq.jar \
                -bamFile ${input.bamFile.dockerPath} \
                -chromSizes ${params.chrsz.dockerPath} \
                -outputDir ${outputsDir}/bamtosignal \
                -outputPrefix ${input.repName} \
                -strandedness ${params.strandedness}
            """
}