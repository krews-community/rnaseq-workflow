package task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import model.MergedFastqReplicate
import model.MergedFastqReplicatePE
import model.MergedFastqReplicateSE
import org.reactivestreams.Publisher


data class BwaParams(
        val idxTar: File,
        val multimapping: Int? = 4,
        val scoreMin: String? = null
)

data class AlignerInput(
        val mergedRep: MergedFastqReplicate
)

data class AlignerOutput(
        val repName: String,
        val pairedEnd: Boolean,
        val genomeBam: File,
        val annoBam: File,
        val genomeFlagstat: File,
        val annoFlagstat: File,
        //val alignLog: File, missing from bwa
        val log: File
        //val readLenLog: File
)

fun WorkflowBuilder.alignTask(name: String, i: Publisher<AlignerInput>) = this.task<AlignerInput, AlignerOutput>(name, i) {
    val params = taskParams<BwaParams>()

    dockerImage = "genomealmanac/rnaseq-align:v1.0.0"

    val prefix = "align/${input.mergedRep.name}"
    output =
            AlignerOutput(
                    repName = input.mergedRep.name,
                    pairedEnd = input.mergedRep is MergedFastqReplicatePE,
                    genomeBam = OutputFile("${prefix}_genome.bam"),
                    annoBam = OutputFile("${prefix}_anno.bam"),
                    genomeFlagstat = OutputFile("${prefix}_genome_flagstat.txt"),
                    annoFlagstat = OutputFile("${prefix}_anno_flagstat.txt"),
                    log = OutputFile("${prefix}_Log.final.out")
            )

    val mergedRep = input.mergedRep
    command =
            """
          java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -jar /app/rnaseq.jar \
                -indexFile ${params.idxTar.dockerPath} \
                -outputDir ${outputsDir}/align \
                -outputPrefix ${mergedRep.name} \
                ${if (mergedRep is MergedFastqReplicateSE) "-repFile1  ${mergedRep.merged.dockerPath}" else ""} \
                 ${if (mergedRep is MergedFastqReplicatePE) "-repFile1  ${mergedRep.mergedR1.dockerPath}" else ""} \
                   ${if (mergedRep is MergedFastqReplicatePE) "-repFile2  ${mergedRep.mergedR2.dockerPath}" else ""} \
                     ${if (mergedRep is MergedFastqReplicatePE) "-pairedEnd" else ""} \


            """
}