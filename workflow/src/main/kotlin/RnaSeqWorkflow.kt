import krews.core.*
import krews.run
import model.FastqSamples
import model.MergedFastqReplicateSE
import model.MergedFastqSamples
import reactor.core.publisher.toFlux
import task.*
fun main(args: Array<String>) = run(rnaSeqWorkflow, args)

data class RNASeqParams(
        val samples: FastqSamples
)

val rnaSeqWorkflow = workflow("encode-rnaseq-workflow") {

    val params = params<RNASeqParams>()


   val mergeFastqIpInput = params.samples.replicates.map {MergeFastqInput(it)}.toFlux()
   val mergeFastqIpTask = MergeFastqTask("mergeFastq-rep-ip",mergeFastqIpInput)

   val bwaInputIps = mergeFastqIpTask
            .map { mAlignerInput(it.mergedFileR1,it.mergedFileR2,it.repName,it.pairedEnd) }        
    val bwaTaskIps = malignTask("align-ips", bwaInputIps)

    val bamtosignalInput = bwaTaskIps
            .map { BamtoSignalInput(it.genomeBam,  it.repName ) }
    val bam2tanofiltTask = bamtosignalTask("bamtosignal",bamtosignalInput)

   val rsemquantInput = bwaTaskIps
            .map { RsemQuantInput(it.annoBam,  it.repName,it.pairedEnd ) }
    val rsemquantTask = rsemquantTask("rsemquant",rsemquantInput)


}

