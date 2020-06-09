package workflow.task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class AlignerParams (
    val index: File,
    val cores: Int = 1,
    val ramGb: Int = 16,
    val indexTarPrefix: String? = null,
    val minReadLength: Int = 20
)

data class AlignerInput (
    val repFile1: File,
    val repFile2: File?,
    val name: String,
    val pairedEnd: Boolean
)

data class AlignerOutput (
    val name: String,
    val pairedEnd: Boolean,
    val genomeBam: File,
    val annoBam: File,
    val genomeFlagstat: File,
    val annoFlagstat: File,
    val log: File
)

fun WorkflowBuilder.alignTask(name: String, i: Publisher<AlignerInput>)
  = this.task<AlignerInput, AlignerOutput>(name, i) {
    
    val params = taskParams<AlignerParams>()
    dockerImage = "genomealmanac/rnaseq-star:1.1.1"
    val prefix = "${input.name}"

    output = AlignerOutput(
        name = input.name,
        pairedEnd = input.pairedEnd,
        genomeBam = OutputFile("${prefix}_genome.bam"),
        annoBam = OutputFile("${prefix}_anno.bam"),
        genomeFlagstat = OutputFile("${prefix}_genome_flagstat.txt"),
        annoFlagstat = OutputFile("${prefix}_anno_flagstat.txt"),
        log = OutputFile("${prefix}_Log.final.out")
    )

    command = """
        export TMPDIR="${outputsDir}"
        java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 \
            -jar /app/star.jar \
                --index ${params.index.dockerPath} \
                --output-directory ${outputsDir} \
                --output-prefix ${input.name} \
                --r1 ${input.repFile1.dockerPath} \
                --cores ${params.cores} \
                --ram-gb ${params.ramGb} \
                --min-read-length ${params.minReadLength} \
                ${ if (params.indexTarPrefix !== null) "--index-tar-prefix ${params.indexTarPrefix}" else "" } \
                ${ if (input.pairedEnd) "--r2  ${input.repFile2!!.dockerPath}" else "" }
    """

}
