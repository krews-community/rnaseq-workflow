package workflow.model

import krews.file.File

data class FastqReplicatePE (
    override val name: String,
    val r1: List<File>,
    val r2: List<File>,
    val adapter1: File? = null,
    val adapter2: File? = null
) : Replicate

data class FastqReplicateSE (
    override val name: String,
    val r1: List<File>,
    val adapter1: File? = null
) : Replicate

data class MergedFastqReplicateSE (
    override val name: String,
    val r1: File
) : Replicate

data class MergedFastqReplicatePE (
    override val name: String,
    val r1: File,
    val r2: File
) : Replicate
