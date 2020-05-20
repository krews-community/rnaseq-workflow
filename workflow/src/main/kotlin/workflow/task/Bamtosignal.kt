package workflow.task

import krews.core.WorkflowBuilder
import krews.core.*
import workflow.model.*
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class BamtoSignalParams(
    val chromosomeSizes: File,
    val stranded: Boolean
)

data class BamtoSignalInput(
    val bam: File,
    override val name: String
) : Replicate

data class  BamtoSignalOutput(
    val repName: String,
    val minusAll: File?,
    val minusUniq: File?,
    val all: File?,
    val uniq: File?,
    val plusAll: File?,
    val plusUniq: File?
)

fun WorkflowBuilder.bamtosignalTask(name: String, i: Publisher<BamtoSignalInput>)
  = this.task<BamtoSignalInput,  BamtoSignalOutput>(name, i) {

    val params = taskParams<BamtoSignalParams>()
    dockerImage = "genomealmanac/rnaseq-bam-to-signal:1.0.3"
    val prefix = "${input.name}"

    output = BamtoSignalOutput(
        repName = input.name,
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
                --output-prefix ${input.name} \
                ${ if (params.stranded) "--stranded" else "" }
    """

}
