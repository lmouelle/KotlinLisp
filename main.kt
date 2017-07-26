/**
 * Created by lu on 5/28/17.
 *
 * So what did I learn from CMSC330? Once you have a BNF like grammar defined,
 * it maps fairly closely to mutually recursive functions. Of course while this is
 * easily done in ML Kotlin would likely object to the naive implementation.
 *
 *
 * Let's try to implement clojure's EDN format first
 */


fun main(args: Array<String>) {
    // Let's do some tests
    val vecs = listOf("[1 2 3]", "[ 2 3 5]", "[      5   6]", "[false true false false]", "[]")
    val lists = listOf("(1 2 3)", "(false true false)", "(false true 3 9999999)")
    val maps = listOf("{1 false, 4 true, 6 9000}")

    vecs.map {
        val tokenized = tokenize(it)
        val parsed =  matchFile(ArrayList(tokenized))
    }
}