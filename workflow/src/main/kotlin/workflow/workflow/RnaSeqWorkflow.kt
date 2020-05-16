package workflow.workflow

import krews.core.*
import krews.run
import reactor.core.publisher.toFlux

import workflow.model.*
import workflow.task.*

data class RNASeqParams(
    val experiments: List<Experiment>
)

fun bamToSignalInput(v: BamReplicate): AlignerOutput
    = AlignerOutput(
        v.name,
        v.pairedEnd,
        v.genomicAlignments!!,
        v.genomicAlignments,
        v.genomicAlignments,
        v.genomicAlignments,
        v.genomicAlignments
    )

fun quantInput(v: BamReplicate): AlignerOutput
    = AlignerOutput(
        v.name,
        v.pairedEnd,
        v.transcriptomicAlignments!!,
        v.transcriptomicAlignments,
        v.transcriptomicAlignments,
        v.transcriptomicAlignments,
        v.transcriptomicAlignments
    )

val rnaSeqWorkflow = workflow("encode-rnaseq-workflow") {

    val params = params<RNASeqParams>()

    /* create tasks to merge reads for experiments starting from FASTQs */
    val mergeFastqInput = params.experiments.flatMap {
        it.replicates
            .filter { it is FastqReplicatePE || it is FastqReplicateSE }
            .map { MergeFastqInput(it) }
    }.toFlux()
    val mergeFastqTask = MergeFastqTask("merge", mergeFastqInput)

    /* create task to align reads for experiments starting from FASTQs */
    val starInput = mergeFastqTask
        .map { AlignerInput(it.mergedFileR1, it.mergedFileR2, it.repName, it.pairedEnd) }        
    val starTask = alignTask("align", starInput)

    /* create signal for aligned FASTQs and experiments starting from genomic BAMs */
    val signalBamInput = params.experiments.flatMap {
        it.replicates
            .filter { it is BamReplicate && it.genomicAlignments !== null }
            .map { bamToSignalInput(it as BamReplicate) }
    }.toFlux()
    bamtosignalTask("signal", starTask.concatWith(signalBamInput)
        .map { BamtoSignalInput(it.genomeBam, it.name) })

    /* create quantifications for aligned FASTQs and experiments starting from transcriptomic BAMs */
    val quantBamInput = params.experiments.flatMap {
        it.replicates
            .filter { it is BamReplicate && it.transcriptomicAlignments !== null }
            .map { quantInput(it as BamReplicate) }
    }.toFlux()
    rsemquantTask("quant", starTask.concatWith(quantBamInput)
        .map { RsemQuantInput(it.annoBam, it.name, it.pairedEnd) })

}
