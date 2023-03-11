fun encode(str: String): String {
    val result = mutableListOf<String>()
    var isFirst = true
    var lastBit = '0'
    for (ch in str) {
        val binaryCharAsString = Integer.toBinaryString(ch.code)
                .toString()
                .padStart(7, '0')
        for (bit in binaryCharAsString.slice(0 ..6)) {
            if (lastBit == bit && !isFirst)
                result.add("0")
            else {
                if (!isFirst)
                    result.add(" ")
                isFirst = false
                result.add(when (bit) {
                    '1' -> "0 0"
                    else -> "00 0"
                })
                lastBit = bit
            }
        }
    }
    return result.joinToString("")
}

fun decode(str: String): String {
    val cypher = str.split(" ").toMutableList()
    cypher.remove("")
    if (cypher.isEmpty() || cypher.size % 2 != 0)
        throw IllegalArgumentException("Encode string is not valid.")
    val bits = mutableListOf<Char>()
    for (i in cypher.indices step 2) {
        val curChar = when(cypher[i]) {
            "0" -> '1'
            "00" -> '0'
            else -> throw IllegalArgumentException("Encode string is not valid.")
        }
        bits.addAll(if (bits.isEmpty()) 0 else bits.lastIndex + 1,
                curChar.toString().repeat(cypher[i + 1].length).toMutableList())
    }
    val result = mutableListOf<Char>()
    if (bits.size % 7 != 0)
        throw IllegalArgumentException("Encode string is not valid.")
    for (i in bits.indices step 7) {
        val binaryString = String(bits.subList(i, i+7).toCharArray())
        result.add(Integer.parseInt(binaryString, 2).toChar())
    }
    return result.joinToString("")
}

fun main() {
    while (true) {
        println("Please input operation (encode/decode/exit):")
        val request = readln()
        when (request) {
            "encode" -> {
                println("Input string:")
                val line = readln()
                println("Encoded string:")
                println(encode(line))
            }
            "decode" -> {
                println("Input encoded string:")
                val line = readln()
                var decodedString = ""
                try {
                    decodedString = decode(line)
                } catch (e: IllegalArgumentException) {
                    println(e.message)
                }
                if (decodedString.isNotBlank()) {
                    println("Decoded string:")
                    println(decodedString)
                }
            }
            "exit" -> {
                println("Bye!")
                break
            }
            else -> {
                println("There is no '$request' operation")
            }
        }
        println()
    }
}
