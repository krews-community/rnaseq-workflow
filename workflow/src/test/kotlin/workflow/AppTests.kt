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
            samples: {
                -type = "workflow.model.FastqSamplesSE"
                replicates: [{
                    name = "testrep1"
                    fastqs: [{
                        -type = "krews.file.LocalInputFile"
                        local-path = "$testDir/test.fastq.gz"
                    }]
                }]
            }
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

        assertThat(outputsDir.resolve("testrep1.merged.r1.fastq.gz")).exists()
        assertThat(outputsDir.resolve("testrep1.merged.r1.fastq.gz").toFile().md5()).isEqualTo("6b0d6ea05ab99a717a247e0b6ace5228")

        assertThat(outputsDir.resolve("testrep1_anno.bam")).exists()
        assertThat(outputsDir.resolve("testrep1_anno.bam").toFile().md5()).isEqualTo("1644cde899e6e71de60b669855e315bc")
        assertThat(outputsDir.resolve("testrep1_anno_flagstat.txt")).exists()
        assertThat(outputsDir.resolve("testrep1_anno_flagstat.txt").toFile().md5()).isEqualTo("87c78efa9ad0a76dec66ab5c8e8e7754")

        assertThat(outputsDir.resolve("testrep1_genome.bam")).exists()
        assertThat(outputsDir.resolve("testrep1_genome.bam").toFile().md5()).isEqualTo("01a7dc3459d80c0ae66dbd8fe4226dcb")
        assertThat(outputsDir.resolve("testrep1_genome_flagstat.txt")).exists()
        assertThat(outputsDir.resolve("testrep1_genome_flagstat.txt").toFile().md5()).isEqualTo("a96d66f579ad1d88239764e059fc2554")
        
        assertThat(outputsDir.resolve("testrep1_minusAll.bw")).exists()
        assertThat(outputsDir.resolve("testrep1_minusAll.bw").toFile().md5()).isEqualTo("ebe7ec1e26273ce59081e5b481075832")
        assertThat(outputsDir.resolve("testrep1_minusUniq.bw")).exists()
        assertThat(outputsDir.resolve("testrep1_minusUniq.bw").toFile().md5()).isEqualTo("ebe7ec1e26273ce59081e5b481075832")

        assertThat(outputsDir.resolve("testrep1_plusAll.bw")).exists()
        assertThat(outputsDir.resolve("testrep1_plusAll.bw").toFile().md5()).isEqualTo("e671d20dd2e22fbef92e09b4dd890567")
        assertThat(outputsDir.resolve("testrep1_plusUniq.bw")).exists()
        assertThat(outputsDir.resolve("testrep1_plusUniq.bw").toFile().md5()).isEqualTo("e671d20dd2e22fbef92e09b4dd890567")

        assertThat(outputsDir.resolve("testrep1.isoforms.results")).exists()
        assertThat(pearsonr(
            readQuantifications(outputsDir.resolve("testrep1.genes.results")),
            readQuantifications(testDir.resolve("genes.expected.results"))
        )).isGreaterThan(0.9F)
        assertThat(outputsDir.resolve("testrep1.genes.results")).exists()
        assertThat(pearsonr(
            readQuantifications(outputsDir.resolve("testrep1.isoforms.results")),
            readQuantifications(testDir.resolve("isoforms.expected.results"))
        )).isGreaterThan(0.9F)

    }

}
