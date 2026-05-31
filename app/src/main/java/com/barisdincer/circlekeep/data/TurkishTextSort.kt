package com.barisdincer.circlekeep.data

private val turkishAlphabetOrder = mapOf(
    'a' to "01",
    'b' to "02",
    'c' to "03",
    'ç' to "04",
    'd' to "05",
    'e' to "06",
    'f' to "07",
    'g' to "08",
    'ğ' to "09",
    'h' to "10",
    'ı' to "11",
    'i' to "12",
    'j' to "13",
    'k' to "14",
    'l' to "15",
    'm' to "16",
    'n' to "17",
    'o' to "18",
    'ö' to "19",
    'p' to "20",
    'r' to "21",
    's' to "22",
    'ş' to "23",
    't' to "24",
    'u' to "25",
    'ü' to "26",
    'v' to "27",
    'y' to "28",
    'z' to "29"
)

fun turkishSortKey(value: String): String {
    return value.trim()
        .map { char ->
            val normalized = char.toTurkishLowercase()
            turkishAlphabetOrder[normalized] ?: "99$normalized"
        }
        .joinToString(separator = ".")
}

fun <T> Iterable<T>.sortedByTurkish(selector: (T) -> String): List<T> {
    return sortedWith(compareBy<T> { turkishSortKey(selector(it)) }.thenBy(selector))
}

private fun Char.toTurkishLowercase(): Char {
    return when (this) {
        'I' -> 'ı'
        'İ' -> 'i'
        else -> lowercaseChar()
    }
}
