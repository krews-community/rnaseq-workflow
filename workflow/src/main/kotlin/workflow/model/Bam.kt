package workflow.model

import krews.file.File

data class BamReplicate (
    override val name: String,
    val pairedEnd: Boolean,
    val genomicAlignments: File? = null,
    val transcriptomicAlignments: File? = null
) : Replicate
