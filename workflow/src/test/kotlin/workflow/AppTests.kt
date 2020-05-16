package workflow

import workflow.workflow.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.nio.file.*
import com.beust.klaxon.Klaxon
import java.security.MessageDigest
import kotlin.math.sqrt

import testutil.md5
import testutil.pearsonr
import testutil.readQuantifications
import testutil.assertMD5
import testutil.assertPearsonR

/**
 * Recursively delete directory if it exists
 */
fun deleteDir(dir: Path) {
    if (Files.isDirectory(dir)) {
        Files.walk(dir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppTests {

    private val testDir = Paths.get("src/test/resources")
    private val testRunDir = testDir.resolve("run")
    private val outputsDir = testRunDir.resolve("outputs")

    private fun config(): String = """
        working-dir = $testRunDir

        params {
            experiments: [{
                replicates: [{
                    -type = "workflow.model.FastqReplicateSE"
                    name = "testrep1"
                    r1: [{
                        -type = "krews.file.LocalInputFile"
                        local-path = "$testDir/test.fastq.gz"
                    }]
                }]
            }, {
                replicates: [{
                    -type = "workflow.model.BamReplicate"
                    name = "testbam1"
                    paired-end = "false"
                    genomic-alignments = {
                        -type = "krews.file.LocalInputFile"
                        local-path = "$testDir/genome.bam"
                    }
                    transcriptomic-alignments = {
                        -type = "krews.file.LocalInputFile"
                        local-path = "$testDir/transcriptome.bam"
                    }
                }]
            }]
        }
        
        task.align.params {
            index = {
                -type = "krews.file.LocalInputFile"
                local-path = "$testDir/star.index.tar.gz"
            }
        }

        task.signal.params {
            chromosome-sizes = {
                -type = "krews.file.LocalInputFile"
                local-path = "$testDir/chrom.sizes"
            }
            stranded = "true"
        }
        
        task.quant.params {
            index = {
                -type = "krews.file.LocalInputFile"
                local-path = "$testDir/test.index.tar.gz"
            }
        }
    """.trimIndent()

    @BeforeAll
    fun beforeTests() { deleteDir(testRunDir) }

    @AfterAll
    fun afterTests() { deleteDir(testRunDir) }

    @Test
    fun `run a simple workflow locally`() {

        val t = createTempFile()
        t.bufferedWriter().use {
            it.write(config())
        }
        krews.run(rnaSeqWorkflow, arrayOf("--on", "local", "--config", t.absolutePath))
        t.delete()

        /* FASTQ merging task */
        assertMD5(outputsDir.resolve("testrep1.merged.r1.fastq.gz"), "6b0d6ea05ab99a717a247e0b6ace5228")

        /* STAR task */
        assertMD5(outputsDir.resolve("testrep1_anno.bam"), "1644cde899e6e71de60b669855e315bc")
        assertMD5(outputsDir.resolve("testrep1_anno_flagstat.txt"), "87c78efa9ad0a76dec66ab5c8e8e7754")
        assertMD5(outputsDir.resolve("testrep1_genome.bam"), "01a7dc3459d80c0ae66dbd8fe4226dcb")
        assertMD5(outputsDir.resolve("testrep1_genome_flagstat.txt"), "a96d66f579ad1d88239764e059fc2554")
        
        /* signal generation task starting from FASTQs */
        assertMD5(outputsDir.resolve("testrep1_minusAll.bw"), "ebe7ec1e26273ce59081e5b481075832")
        assertMD5(outputsDir.resolve("testrep1_minusUniq.bw"), "ebe7ec1e26273ce59081e5b481075832")
        assertMD5(outputsDir.resolve("testrep1_plusAll.bw"), "e671d20dd2e22fbef92e09b4dd890567")
        assertMD5(outputsDir.resolve("testrep1_plusUniq.bw"), "e671d20dd2e22fbef92e09b4dd890567")

        /* RSEM task starting from FASTQs */
        assertPearsonR(outputsDir.resolve("testrep1.genes.results"), testDir.resolve("genes.expected.results"))
        assertPearsonR(outputsDir.resolve("testrep1.isoforms.results"), testDir.resolve("isoforms.expected.results"))

        /* RSEM task starting from BAM */
        assertPearsonR(outputsDir.resolve("testbam1.genes.results"), testDir.resolve("genes.bam.expected.results"))
        assertPearsonR(outputsDir.resolve("testbam1.isoforms.results"),testDir.resolve("isoforms.bam.expected.results"))

        /* signal generation task starting from BAM */
        assertMD5(outputsDir.resolve("testbam1_minusAll.bw"), "ebe7ec1e26273ce59081e5b481075832")
        assertMD5(outputsDir.resolve("testbam1_minusUniq.bw"), "ebe7ec1e26273ce59081e5b481075832")
        assertMD5(outputsDir.resolve("testbam1_plusAll.bw"), "e671d20dd2e22fbef92e09b4dd890567")
        assertMD5(outputsDir.resolve("testbam1_plusUniq.bw"), "e671d20dd2e22fbef92e09b4dd890567")

    }

}
