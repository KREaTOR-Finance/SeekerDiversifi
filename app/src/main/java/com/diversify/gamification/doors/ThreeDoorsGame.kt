package com.diversify.gamification.doors

import kotlin.random.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreeDoorsGame @Inject constructor() {
    
    private val random = Random(System.currentTimeMillis())
    
    private val prizePool = listOf(
        Prize("100 BONK", "BONK", 0.0001),
        Prize("0.01 JTO", "JTO", 0.01),
        Prize("50 WIF", "WIF", 50.0),
        Prize("10 PYTH", "PYTH", 10.0),
        Prize("DRiP NFT", "DRIP", 1.0),
        Prize("5 USDC", "USDC", 5.0),
        Prize("Thanks for playing!", "NONE", 0.0)
    )
    
    private var currentGame: GameState? = null
    
    fun newGame(): GameState {
        val winningDoor = random.nextInt(1, 4)
        val prize = prizePool.random()
        
        val gameState = GameState(
            gameId = UUID.randomUUID().toString(),
            doors = listOf(
                Door(1, false, if (winningDoor == 1) prize else null),
                Door(2, false, if (winningDoor == 2) prize else null),
                Door(3, false, if (winningDoor == 3) prize else null)
            ),
            winningDoor = winningDoor,
            prize = prize,
            gameActive = true,
            startedAt = System.currentTimeMillis()
        )
        
        currentGame = gameState
        return gameState
    }
    
    fun selectDoor(doorNumber: Int): GameResult {
        val game = currentGame ?: return GameResult(
            won = false,
            prize = null,
            message = "No active game",
            gameComplete = false
        )
        
        if (!game.gameActive) {
            return GameResult(
                won = false,
                prize = null,
                message = "Game already complete",
                gameComplete = true
            )
        }
        
        val isWinner = doorNumber == game.winningDoor
        val prize = if (isWinner) game.prize else null
        
        val result = GameResult(
            won = isWinner,
            prize = prize,
            message = if (isWinner) "You won ${prize?.name}!" else "Try again!",
            gameComplete = true
        )
        
        currentGame = game.copy(gameActive = false)
        
        return result
    }
    
    fun getCurrentGame(): GameState? = currentGame
    
    data class GameState(
        val gameId: String,
        val doors: List<Door>,
        val winningDoor: Int,
        val prize: Prize,
        val gameActive: Boolean,
        val startedAt: Long
    )
    
    data class Door(
        val number: Int,
        val isOpen: Boolean,
        val prize: Prize?
    )
    
    data class Prize(
        val name: String,
        val tokenSymbol: String,
        val amount: Double
    )
    
    data class GameResult(
        val won: Boolean,
        val prize: Prize?,
        val message: String,
        val gameComplete: Boolean
    )
}
