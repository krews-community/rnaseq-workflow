package workflow.workflow

import krews.core.*
import krews.run
import reactor.core.publisher.toFlux

import workflow.model.*
import workflow.task.*

data class RNASeqParams(
    val samples: FastqSamples
)

val rnaSeqWorkflow = workflow("encode-rnaseq-workflow") {

    val params = params<RNASeqParams>()

    val mergeFastqInput = params.samples.replicates.map { MergeFastqInput(it) }.toFlux()
    val mergeFastqTask = MergeFastqTask("merge", mergeFastqInput)

    val starInput = mergeFastqTask
        .map { AlignerInput(it.mergedFileR1, it.mergedFileR2, it.repName, it.pairedEnd) }        
    val starTask = alignTask("align", starInput)

    val bamToSignalInput = starTask.map { BamtoSignalInput(it.genomeBam, it.repName) }
    bamtosignalTask("signal", bamToSignalInput)

    val rsemquantInput = starTask.map { RsemQuantInput(it.annoBam, it.repName, it.pairedEnd) }
    rsemquantTask("quant", rsemquantInput)

}
