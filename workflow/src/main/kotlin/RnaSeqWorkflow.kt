import krews.core.*
import krews.run
import model.*
import model.MergedFastqReplicateSE
import model.MergedFastqSamples
import reactor.core.publisher.toFlux
import task.*
fun main(args: Array<String>) = run(rnaSeqWorkflow, args)

data class RNASeqParams(
        val fastqsamples: FastqSamples?,
        val bamgenomesamples : BamGenomeSamples?,
        val bamannotationsamples : BamAnnotationSamples?
)

val rnaSeqWorkflow = workflow("rna-seq-workflow") {

    val params = params<RNASeqParams>()

   if(params.fastqsamples!=null){
        
        val mergeFastqIpInput = params.fastqsamples.replicates.map {MergeFastqInput(it)}.toFlux()
        val mergeFastqIpTask = MergeFastqTask("mergefastq",mergeFastqIpInput)

        val bwaInputIps = mergeFastqIpTask
                .map { mAlignerInput(it.mergedFileR1,it.mergedFileR2,it.repName,it.pairedEnd) }        
        val bwaTaskIps = malignTask("align", bwaInputIps)

        val bamtosignalInput = bwaTaskIps
                .map { BamtoSignalInput(it.genomeBam,  it.repName ) }
        val bam2tanofiltTask = bamtosignalTask("bamtosignal",bamtosignalInput)

        val rsemquantInput = bwaTaskIps
                .map { RsemQuantInput(it.annoBam,  it.repName, it.pairedEnd ) }
        val rsemquantTask = rsemquantTask("rsemquant",rsemquantInput)
   } else if(params.bamgenomesamples!=null){
        val bamtosignalInputs = params.bamgenomesamples.alignments
                .map { value -> 
                    val bamFile:BamGenomeAlignmentFiles = value as BamGenomeAlignmentFiles
                    BamtoSignalInput(bamFile.bam,bamFile.name)                    
                }
                .toFlux()
        val bam2tanofiltTask = bamtosignalTask("bamtosignal",bamtosignalInputs)
   } else if(params.bamannotationsamples!=null){
        val rsemInputs = params.bamannotationsamples.alignments
                .map { value -> 
                    val bamFile:BamAnnotationAlignmentFiles = value as BamAnnotationAlignmentFiles
                    RsemQuantInput(bamFile.bam,bamFile.name,bamFile.pairedend)                    
                }
                .toFlux()
        val rsemquantTask = rsemquantTask("rsemquant",rsemInputs)
   }
   
}

