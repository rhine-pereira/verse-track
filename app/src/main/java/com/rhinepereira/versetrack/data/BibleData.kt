package com.rhinepereira.versetrack.data

object BibleData {
    val catholicBooks = listOf(
        // Old Testament
        "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
        "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah",
        "Tobit", "Judith", "Esther", "1 Maccabees", "2 Maccabees", "Job", "Psalms", "Psalm", "Proverbs", "Ecclesiastes",
        "Song of Songs", "Wisdom", "Sirach", "Isaiah", "Jeremiah", "Lamentations", "Baruch", "Ezekiel",
        "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah",
        "Haggai", "Zechariah", "Malachi",
        // New Testament
        "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians", "2 Corinthians", "Galatians",
        "Ephesians", "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy",
        "2 Timothy", "Titus", "Philemon", "Hebrews", "James", "1 Peter", "2 Peter", "1 John", "2 John",
        "3 John", "Jude", "Revelation"
    )

    val abbreviations = mapOf(
        "Gen" to "Genesis", "Ex" to "Exodus", "Lev" to "Leviticus", "Num" to "Numbers", "Deut" to "Deuteronomy",
        "Josh" to "Joshua", "Judg" to "Judges", "Ru" to "Ruth", "1 Sam" to "1 Samuel", "2 Sam" to "2 Samuel",
        "1 Kgs" to "1 Kings", "2 Kgs" to "2 Kings", "1 Chr" to "1 Chronicles", "2 Chr" to "2 Chronicles",
        "Neh" to "Nehemiah", "Tob" to "Tobit", "Jdt" to "Judith", "Est" to "Esther", "1 Mac" to "1 Maccabees",
        "2 Mac" to "2 Maccabees", "Ps" to "Psalms", "Prov" to "Proverbs", "Eccl" to "Ecclesiastes",
        "Cant" to "Song of Songs", "Wis" to "Wisdom", "Sir" to "Sirach", "Isa" to "Isaiah", "Jer" to "Jeremiah",
        "Lam" to "Lamentations", "Bar" to "Baruch", "Ezek" to "Ezekiel", "Dan" to "Daniel", "Hos" to "Hosea",
        "Jl" to "Joel", "Am" to "Amos", "Ob" to "Obadiah", "Jon" to "Jonah", "Mic" to "Micah", "Na" to "Nahum",
        "Hab" to "Habakkuk", "Zeph" to "Zephaniah", "Hag" to "Haggai", "Zech" to "Zechariah", "Mal" to "Malachi",
        "Mt" to "Matthew", "Mk" to "Mark", "Lk" to "Luke", "Jn" to "John", "Ac" to "Acts", "Rom" to "Romans",
        "1 Cor" to "1 Corinthians", "2 Cor" to "2 Corinthians", "Gal" to "Galatians", "Eph" to "Ephesians",
        "Phil" to "Philippians", "Col" to "Colossians", "1 Thes" to "1 Thessalonians", "2 Thes" to "2 Thessalonians",
        "1 Tim" to "1 Timothy", "2 Tim" to "2 Timothy", "Ti" to "Titus", "Phlm" to "Philemon", "Heb" to "Hebrews",
        "Jas" to "James", "1 Pet" to "1 Peter", "2 Pet" to "2 Peter", "1 Jn" to "1 John", "2 Jn" to "2 John",
        "3 Jn" to "3 John", "Jud" to "Jude", "Rev" to "Revelation"
    )

    val bibleRefRegex: Regex by lazy {
        val allPatterns = (catholicBooks + abbreviations.keys).sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        Regex("""\b(${allPatterns})\s+(\d+)(?::(\d+(?:-\d+)?(?:\s*,\s*\d+(?:-\d+)?)*))?\b""", RegexOption.IGNORE_CASE)
    }
}
