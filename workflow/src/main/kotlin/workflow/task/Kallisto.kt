package workflow.task

import krews.core.WorkflowBuilder
import krews.core.*
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class KallistoParams (
    val index: File,
    val fragmentLength: Int? = 100,
    val sdFragmentLength: Int? = 10,
    val strandedness: String = "unstranded",
    val cores: Int = 1
)

data class KallistoInput (
    val r1: File,
    val r2: File? = null,
    val name: String
)

data class KallistoOutput (
    val name: String,
    val quantifications: File
)

fun WorkflowBuilder.kallistoTask(name: String, i: Publisher<KallistoInput>)
  = this.task<KallistoInput, KallistoOutput>(name, i) {
    
    val params = taskParams<KallistoParams>()
    dockerImage = "docker.pkg.github.com/krews-community/rnaseq-kallisto-task/rnaseq-kallisto:1.1.0"
    val prefix = "${input.name}"

    output = KallistoOutput(
        name = input.name,
        quantifications = OutputFile("${prefix}.abundance.tsv")
    )

    command = """
        java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 \
            -jar /app/kallisto.jar quant \
                --index ${params.index.dockerPath} \
                --output-directory ${outputsDir} \
                --cores ${params.cores} \
                --strandedness ${params.strandedness} \
                ${ if (input.r2 === null) "--fragment-length ${params.fragmentLength}" else "" } \
                ${ if (input.r2 === null) "--sd-fragment-length ${params.sdFragmentLength}" else "" } \
                --output-prefix ${input.name} \
                --r1 ${input.r1.dockerPath} \
                ${ if (input.r2 !== null) "--r2  ${input.r2!!.dockerPath}" else "" }
    """

}
