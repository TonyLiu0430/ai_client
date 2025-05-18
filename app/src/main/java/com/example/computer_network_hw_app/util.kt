package com.example.computer_network_hw_app

fun decodeString(encoded: String): String {
    val decoded = StringBuilder()
    var flag = false;
    for (i in encoded.indices) {
        if(flag) {
            flag = false
            continue
        }
        val char = encoded[i]
        if (char == '\\') {
            flag = true
            when (encoded[i + 1]) {
                'n' -> decoded.append('\n')
                '\\' -> decoded.append('\\')
            }
        } else {
            decoded.append(char)
        }
    }
    return decoded.toString()
}