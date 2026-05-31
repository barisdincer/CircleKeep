package com.barisdincer.circlekeep.data

object PhoneNumberNormalizer {
    fun normalize(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        if (digits.isBlank()) return ""

        return when {
            digits.startsWith("00") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> "90${digits.drop(1)}"
            digits.length == 10 -> "90$digits"
            else -> digits
        }
    }

    fun matches(left: String, right: String): Boolean {
        val normalizedLeft = normalize(left)
        val normalizedRight = normalize(right)
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) return false

        return normalizedLeft == normalizedRight ||
            normalizedLeft.takeLast(10) == normalizedRight.takeLast(10)
    }
}
