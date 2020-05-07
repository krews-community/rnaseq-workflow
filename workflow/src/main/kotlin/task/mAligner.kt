package task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import model.MergedFastqReplicate
import model.MergedFastqReplicatePE
import model.MergedFastqReplicateSE
import org.reactivestreams.Publisher



data class mAlignerInput(
        val repFile1: File,
        val repFile2: File?,
        val repName: String,
        val pairedEnd: Boolean
)



data class mAlignerOutput(
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

fun WorkflowBuilder.malignTask(name: String, i: Publisher<mAlignerInput>) = this.task<mAlignerInput, mAlignerOutput>(name, i) {
    val params = taskParams<BwaParams>()

    dockerImage = "genomealmanac/rnaseq-align:v1.0.0"

    val prefix = "align/${input.repName}"
    output =
            mAlignerOutput(
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    genomeBam = OutputFile("${prefix}_genome.bam"),
                    annoBam = OutputFile("${prefix}_anno.bam"),
                    genomeFlagstat = OutputFile("${prefix}_genome_flagstat.txt"),
                    annoFlagstat = OutputFile("${prefix}_anno_flagstat.txt"),
                    log = OutputFile("${prefix}_Log.final.out")
            )

    command =
            """
          java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -jar /app/rnaseq.jar \
                -indexFile ${params.idxTar.dockerPath} \
                -outputDir ${outputsDir}/align \
                -ramGB 16 \
                -outputPrefix ${input.repName} \
                ${if (!input.pairedEnd) "-repFile1  ${input.repFile1.dockerPath}" else ""} \
                 ${if (input.pairedEnd) "-repFile1  ${input.repFile1.dockerPath}" else ""} \
                   ${if (input.pairedEnd) "-repFile2  ${input.repFile2!!.dockerPath}" else ""} \
                     ${if (input.pairedEnd) "-pairedEnd" else ""} \


            """
}