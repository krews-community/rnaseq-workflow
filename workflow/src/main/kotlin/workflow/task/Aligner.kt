package workflow.task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class AlignerParams (
    val index: File
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
    dockerImage = "docker.pkg.github.com/krews-community/rnaseq-star-task/rnaseq-star:1.0.3"
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
        java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 \
            -jar /app/star.jar \
                --index ${params.index.dockerPath} \
                --output-directory ${outputsDir} \
                --ram-gb 16 \
                --output-prefix ${input.name} \
                --r1 ${input.repFile1.dockerPath} \
                ${ if (input.pairedEnd) "--r2  ${input.repFile2!!.dockerPath}" else "" }
    """

}
