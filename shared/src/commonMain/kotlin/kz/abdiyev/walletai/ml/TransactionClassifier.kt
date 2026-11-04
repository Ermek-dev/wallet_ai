package kz.abdiyev.walletai.ml

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.TransactionData
import kotlin.math.log

class TransactionClassifier {

    private val stopWords = setOf(
        "the", "and", "a", "of", "to", "in", "for", "on", "at", "by", "from", "with",
        "и", "в", "на", "с", "по", "за", "для", "от", "из", "к", "о", "об", "при"
    )

    fun tokenize(description: String): List<String> =
        description.lowercase()
            .replace(Regex("[^a-zA-Zа-яА-ЯёЁ0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length > 1 && it !in stopWords }

    class TrainedModel(
        val categoryPriors: Map<Long, Double>,
        val wordLikelihoods: Map<Long, Map<String, Double>>,
        val totalCatWords: Map<Long, Int>,
        val vocabSize: Int,
        val defaultCategoryId: Long
    )

    fun train(transactions: List<TransactionData>, categories: List<CategoryData>): TrainedModel {
        val confirmed = transactions.filter { tx ->
            !tx.isAutoCategorized && !tx.needsReview && categories.any { it.id == tx.categoryId }
        }
        if (confirmed.isEmpty() || categories.isEmpty()) {
            return TrainedModel(emptyMap(), emptyMap(), emptyMap(), 0, categories.firstOrNull()?.id ?: 0L)
        }

        val totalDocs = confirmed.size.toDouble()
        val categoryCounts = mutableMapOf<Long, Int>()
        val categoryWordCounts = mutableMapOf<Long, Int>()
        val categoryWordFreqs = mutableMapOf<Long, MutableMap<String, Int>>()
        val vocabulary = mutableSetOf<String>()

        for (category in categories) {
            categoryCounts[category.id] = 0
            categoryWordCounts[category.id] = 0
            categoryWordFreqs[category.id] = mutableMapOf()
        }

        for (tx in confirmed) {
            val categoryId = tx.categoryId
            categoryCounts[categoryId] = (categoryCounts[categoryId] ?: 0) + 1
            val wordFreqs = categoryWordFreqs.getValue(categoryId)
            for (token in tokenize(tx.description)) {
                wordFreqs[token] = (wordFreqs[token] ?: 0) + 1
                categoryWordCounts[categoryId] = categoryWordCounts.getValue(categoryId) + 1
                vocabulary.add(token)
            }
        }

        val vocabSize = vocabulary.size
        val categoryPriors = categories.associate { category ->
            val count = categoryCounts[category.id] ?: 0
            category.id to log((count + 1).toDouble() / (totalDocs + categories.size), 10.0)
        }

        val wordLikelihoods = categories.associate { category ->
            val catId = category.id
            val freqs = categoryWordFreqs[catId] ?: emptyMap()
            val totalCatWords = categoryWordCounts[catId] ?: 0
            catId to vocabulary.associateWith { word ->
                val wordCount = freqs[word] ?: 0
                log((wordCount + 1).toDouble() / (totalCatWords + vocabSize + 1), 10.0)
            }
        }

        val defaultCategoryId = categoryCounts.maxByOrNull { it.value }?.key ?: categories.first().id
        return TrainedModel(categoryPriors, wordLikelihoods, categoryWordCounts, vocabSize, defaultCategoryId)
    }

    fun classify(description: String, model: TrainedModel, categories: List<CategoryData>): Long {
        if (model.categoryPriors.isEmpty() || categories.isEmpty()) return model.defaultCategoryId

        val tokens = tokenize(description)
        if (tokens.isEmpty()) {
            return findBestFallbackCategory(description, categories) ?: model.defaultCategoryId
        }

        var bestCategoryId = model.defaultCategoryId
        var maxLogPosterior = Double.NEGATIVE_INFINITY

        for (category in categories) {
            val catId = category.id
            var logPosterior = model.categoryPriors[catId] ?: 0.0
            val likelihoods = model.wordLikelihoods[catId] ?: emptyMap()
            val totalCatWords = model.totalCatWords[catId] ?: 0

            for (token in tokens) {
                logPosterior += likelihoods[token]
                    ?: run {
                        val pUnseen = 1.0 / (totalCatWords + model.vocabSize + 1)
                        log(pUnseen, 10.0)
                    }
            }

            if (logPosterior > maxLogPosterior) {
                maxLogPosterior = logPosterior
                bestCategoryId = catId
            }
        }
        return bestCategoryId
    }

    private fun findBestFallbackCategory(description: String, categories: List<CategoryData>): Long? {
        val lowerDesc = description.lowercase()
        val keywordMap = mapOf(
            "magnum" to "Продукты", "small" to "Продукты", "kaspi" to "Продукты",
            "halyk" to "Продукты", "wolt" to "Кофе", "glovo" to "Кофе", "onay" to "Транспорт",
            "пятёрочка" to "Продукты", "пятерочка" to "Продукты", "перекресток" to "Продукты",
            "яндекс.еда" to "Кофе", "такси" to "Транспорт", "метро" to "Транспорт",
            "кофе" to "Кофе", "ресторан" to "Кофе", "кафе" to "Кофе",
            "жкх" to "ЖКХ", "кварплата" to "ЖКХ", "netflix" to "Подписки",
            "аптека" to "Здоровье", "зарплата" to "Зарплата", "аванс" to "Зарплата"
        )
        for ((keyword, catName) in keywordMap) {
            if (lowerDesc.contains(keyword)) {
                categories.find {
                    it.name.contains(catName, ignoreCase = true) ||
                        catName.contains(it.name, ignoreCase = true)
                }?.let { return it.id }
            }
        }
        return null
    }
}
