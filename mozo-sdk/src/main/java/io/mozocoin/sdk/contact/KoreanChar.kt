package io.mozocoin.sdk.contact

object KoreanChar {

    private val CHOSEONG_COUNT = 19
    private val JUNGSEONG_COUNT = 21
    private val JONGSEONG_COUNT = 28
    private val HANGUL_SYLLABLE_COUNT = CHOSEONG_COUNT * JUNGSEONG_COUNT * JONGSEONG_COUNT
    private val HANGUL_SYLLABLES_BASE = 0xAC00
    private val HANGUL_SYLLABLES_END = HANGUL_SYLLABLES_BASE + HANGUL_SYLLABLE_COUNT

    private val COMPAT_CHOSEONG_MAP = intArrayOf(0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139, 0x3141, 0x3142, 0x3143, 0x3145, 0x3146, 0x3147, 0x3148, 0x3149, 0x314A, 0x314B, 0x314C, 0x314D, 0x314E)

    private fun isSyllable(c: Char): Boolean {
        return c.toInt() in HANGUL_SYLLABLES_BASE..(HANGUL_SYLLABLES_END - 1)
    }

    fun getCompatChoseong(value: Char?): Char {
        if (value == null || !isSyllable(value))
            return '\u0000'

        val choseongIndex = getChoseongIndex(value)
        return COMPAT_CHOSEONG_MAP.getOrNull(choseongIndex)?.toChar() ?: '\u0000'
    }

    private fun getChoseongIndex(syllable: Char): Int {
        val syllableIndex = syllable.toInt() - HANGUL_SYLLABLES_BASE
        return syllableIndex / (JUNGSEONG_COUNT * JONGSEONG_COUNT)
    }
}