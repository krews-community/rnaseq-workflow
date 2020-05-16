package testutil

import org.assertj.core.api.Assertions.assertThat
import java.security.MessageDigest
import java.io.File
import java.nio.file.Path

fun File.md5(): String {
    return MessageDigest.getInstance("MD5").digest(this.readBytes()).joinToString("") { "%02x".format(it) }
}

fun pearsonr(a: Map<String, Float>, b:Map<String, Float>): Float {
    val keys = a.keys.toList()
    return pearsonr(keys.map { a[it]!! }, keys.map { b[it]!! })
}

fun pearsonr(a: List<Float>, b: List<Float>): Float {
    
    var sumX: Float = 0.0F
    var sumY: Float = 0.0F
    var sumXY: Float = 0.0F
    var squareSumX: Float = 0.0F
    var squareSumY: Float = 0.0F

    a.forEachIndexed { i, it ->
        sumX += it
        sumY += b[i]
        sumXY += it * b[i]
        squareSumX += it * it
        squareSumY += b[i] * b[i]
    }

    return ((a.size * sumXY - sumX * sumY) / Math.sqrt(
        ((a.size * squareSumX - sumX * sumX) * (a.size * squareSumY - sumY * sumY)).toDouble()
    )).toFloat()

}

fun readQuantifications(file: Path): Map<String, Float> {
    val m: MutableMap<String, Float> = mutableMapOf()
    file.toFile().forEachLine {
        val s = it.split('\t')
        if (s.size < 6 || s[5] == "TPM") return@forEachLine
        m[s[1]] = s[5].toFloat()
    }
    return m
}

fun assertMD5(f: Path, value: String) {
    assertThat(f).exists()
    assertThat(f.toFile().md5()).isEqualTo(value)
}

fun assertPearsonR(f: Path, c: Path, t: Float = 0.9F) {
    assertThat(f).exists()
    assertThat(pearsonr(
        readQuantifications(f),
        readQuantifications(c)
    )).isGreaterThan(t)
}
