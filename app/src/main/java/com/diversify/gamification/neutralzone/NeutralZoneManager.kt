package com.diversify.gamification.neutralzone

import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeutralZoneManager @Inject constructor(
    private val threeDoorsGame: ThreeDoorsGame
) {
    
    private val random = Random(System.currentTimeMillis())
    private var partnerContent = listOf<PartnerContent>()
    
    suspend fun getNextContent(sessionId: String): NeutralZoneContent {
        val availablePartner = partnerContent.filter { it.isActive }
        
        return if (availablePartner.isNotEmpty() && random.nextDouble() < 0.4) {
            val partner = availablePartner.random()
            PartnerSpotlightContent(
                id = partner.id,
                title = partner.title,
                message = partner.message,
                ctaText = partner.ctaText,
                ctaUrl = partner.ctaUrl,
                durationSeconds = partner.durationSeconds,
                imageUrl = partner.imageUrl
            )
        } else {
            when (random.nextInt(3)) {
                0 -> ThreeDoorsGameContent(threeDoorsGame.newGame())
                1 -> TrendingTokensContent(getTrendingTokens())
                else -> QuickTriviaContent(generateTrivia())
            }
        }
    }
    
    suspend fun updatePartnerContent(content: List<PartnerContent>) {
        partnerContent = content
    }
    
    private suspend fun getTrendingTokens(): List<TrendingToken> {
        return listOf(
            TrendingToken("BONK", "+24%", "#1 swapped"),
            TrendingToken("WIF", "+15%", "Meme season"),
            TrendingToken("JTO", "+8%", "Staking live"),
            TrendingToken("PYTH", "+12%", "New oracle")
        )
    }
    
    private fun generateTrivia(): TriviaQuestion {
        val questions = listOf(
            TriviaQuestion(
                question = "What is Solana's block time?",
                options = listOf("400ms", "800ms", "1.2s", "2s"),
                correctIndex = 0,
                explanation = "Solana has 400ms block time"
            ),
            TriviaQuestion(
                question = "What is the maximum supply of BONK?",
                options = listOf("100B", "1T", "100T", "Infinite"),
                correctIndex = 1,
                explanation = "BONK has 1 trillion total supply"
            ),
            TriviaQuestion(
                question = "Which DEX is most popular on Solana?",
                options = listOf("Uniswap", "Jupiter", "PancakeSwap", "Curve"),
                correctIndex = 1,
                explanation = "Jupiter is the leading DEX aggregator on Solana"
            )
        )
        
        return questions.random()
    }
}
