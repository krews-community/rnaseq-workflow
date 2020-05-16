package workflow.task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import workflow.model.MergedFastqReplicate
import workflow.model.MergedFastqReplicatePE
import workflow.model.MergedFastqReplicateSE
import org.reactivestreams.Publisher

data class BamtoSignalParams(
    val chromosomeSizes: File,
    val stranded: Boolean
)

data class BamtoSignalInput(
    val bam: File,
    val repName: String
)

data class  BamtoSignalOutput(
    val repName: String,
    val minusAll: File?,
    val minusUniq: File?,
    val all: File?,
    val uniq: File?,
    val plusAll: File?,
    val plusUniq: File?
)

fun WorkflowBuilder.bamtosignalTask(name: String, i: Publisher< BamtoSignalInput>)
  = this.task<BamtoSignalInput,  BamtoSignalOutput>(name, i) {

    val params = taskParams<BamtoSignalParams>()
    dockerImage = "docker.pkg.github.com/krews-community/rnaseq-bamtosignal-task/rnaseq-bam-to-signal:1.0.2"
    val prefix = "${input.repName}"

    output = BamtoSignalOutput(
        repName = input.repName,
        minusAll = if (params.stranded) OutputFile("${prefix}_minusAll.bw") else null,
        minusUniq = if (params.stranded) OutputFile("${prefix}_minusUniq.bw") else null,
        all = if (params.stranded) null else OutputFile("${prefix}_All.bw"),
        uniq = if (params.stranded) null else OutputFile("${prefix}_Uniq.bw"),
        plusAll = if (params.stranded) OutputFile("${prefix}_plusAll.bw") else null,
        plusUniq = if (params.stranded) OutputFile("${prefix}_plusUniq.bw") else null
    )

    command = """
        java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 \
            -jar /app/bamtosignal.jar \
                --bam ${input.bam.dockerPath} \
                --chromosome-sizes ${params.chromosomeSizes.dockerPath} \
                --output-directory ${outputsDir} \
                --output-prefix ${input.repName} \
                ${ if (params.stranded) "--stranded" else "" }
    """

}
