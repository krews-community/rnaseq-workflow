package model

import krews.file.File

interface BamGenomeSamples {
    val alignments: List<BamGenomeAlignment>
}
data class BamGenomeSampleFiles(override val alignments: List<BamGenomeAlignmentFiles>) : BamGenomeSamples

interface BamGenomeAlignment {
    val name: String
}

data class BamGenomeAlignmentFiles(override val name: String, val bam: File) : BamGenomeAlignment

interface BamAnnotationSamples {
    val alignments: List<BamAnnotationAlignment>
}
data class BamAnnotationSampleFiles(override val alignments: List<BamAnnotationAlignmentFiles>) : BamAnnotationSamples

interface BamAnnotationAlignment {
    val name: String
}

data class BamAnnotationAlignmentFiles(override val name: String, val bam: File, val pairedend: Boolean ) : BamAnnotationAlignment
