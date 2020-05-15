package task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import model.FastqReplicate
import model.MergedFastqReplicate
import model.MergedFastqReplicatePE
import model.MergedFastqReplicateSE
import org.reactivestreams.Publisher
import model.*



data class MergeFastqInput(
        val mergedRep: FastqReplicate
)

data class MergeFastqOutput(
        val repName: String,
        val pairedEnd: Boolean,
        val mergedFileR1: File,
        val mergedFileR2: File?
)

fun WorkflowBuilder.MergeFastqTask(name: String, i: Publisher<MergeFastqInput>) = this.task<MergeFastqInput, MergeFastqOutput>(name, i) {

    dockerImage = "genomealmanac/chipseq-mergefastq:v1.0.0"

    val prefix = "mergefastq/${input.mergedRep.name}"
    output =
            MergeFastqOutput(
                    repName = input.mergedRep.name,
                    pairedEnd = input.mergedRep is FastqReplicatePE,
                    mergedFileR1 = OutputFile("mergefastq/R1/${input.mergedRep.name}.R1.merged.fastq.gz"),
                    mergedFileR2 = if(input.mergedRep is FastqReplicatePE) OutputFile("mergefastq/R2/${input.mergedRep.name}.R2.merged.fastq.gz") else null
            )

    val mergedRep = input.mergedRep
    command =
            """
          java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -jar /app/chipseq.jar \
                -outputDir ${outputsDir}/mergefastq \
                -outputPrefix ${mergedRep.name} \
               ${if (mergedRep is FastqReplicatePE) "-pairedEnd"  else ""} \
               ${if (mergedRep is FastqReplicateSE) "${mergedRep.fastqs.joinToString(" ") { " -repFile1  ${it.dockerPath}" }}"  else ""} \
               ${if (mergedRep is FastqReplicatePE) "${mergedRep.fastqsR1.joinToString(" ") { " -repFile1  ${it.dockerPath}" }}"  else ""} \
               ${if (mergedRep is FastqReplicatePE) "${mergedRep.fastqsR2.joinToString(" ") { " -repFile2  ${it.dockerPath}" }}"  else ""}
            """
}
