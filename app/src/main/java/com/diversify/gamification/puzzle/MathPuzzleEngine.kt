package com.diversify.gamification.puzzle

import kotlin.random.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MathPuzzleEngine @Inject constructor() {
    
    private val random = Random(System.currentTimeMillis())
    
    fun generatePuzzle(difficulty: PuzzleDifficulty = PuzzleDifficulty.MEDIUM): MathPuzzle {
        return when (difficulty) {
            PuzzleDifficulty.EASY -> generateEasyPuzzle()
            PuzzleDifficulty.MEDIUM -> generateMediumPuzzle()
            PuzzleDifficulty.HARD -> generateHardPuzzle()
        }
    }
    
    private fun generateEasyPuzzle(): MathPuzzle {
        val type = random.nextInt(2)
        
        return when (type) {
            0 -> generateAddition(1, 10)
            else -> generateSubtraction(1, 10, positive = true)
        }
    }
    
    private fun generateMediumPuzzle(): MathPuzzle {
        val type = random.nextInt(3)
        
        return when (type) {
            0 -> generateAddition(5, 20)
            1 -> generateSubtraction(5, 20, positive = true)
            else -> generateMultiplication(2, 5)
        }
    }
    
    private fun generateHardPuzzle(): MathPuzzle {
        val type = random.nextInt(4)
        
        return when (type) {
            0 -> generateAddition(10, 50)
            1 -> generateSubtraction(10, 50, positive = false)
            2 -> generateMultiplication(3, 8)
            else -> generateDivision()
        }
    }
    
    private fun generateAddition(min: Int, max: Int): MathPuzzle {
        val a = random.nextInt(min, max)
        val b = random.nextInt(min, max)
        
        return MathPuzzle(
            id = UUID.randomUUID().toString(),
            question = "$a + $b = ?",
            answer = (a + b).toString(),
            type = PuzzleType.ADDITION,
            difficulty = PuzzleDifficulty.MEDIUM
        )
    }
    
    private fun generateSubtraction(min: Int, max: Int, positive: Boolean): MathPuzzle {
        if (positive) {
            val a = random.nextInt(min, max)
            val b = random.nextInt(min, a)
            return MathPuzzle(
                id = UUID.randomUUID().toString(),
                question = "$a - $b = ?",
                answer = (a - b).toString(),
                type = PuzzleType.SUBTRACTION,
                difficulty = PuzzleDifficulty.MEDIUM
            )
        } else {
            val a = random.nextInt(min, max)
            val b = random.nextInt(min, max)
            return MathPuzzle(
                id = UUID.randomUUID().toString(),
                question = "$a - $b = ?",
                answer = (a - b).toString(),
                type = PuzzleType.SUBTRACTION,
                difficulty = PuzzleDifficulty.HARD
            )
        }
    }
    
    private fun generateMultiplication(min: Int, max: Int): MathPuzzle {
        val a = random.nextInt(min, max)
        val b = random.nextInt(min, max)
        
        return MathPuzzle(
            id = UUID.randomUUID().toString(),
            question = "$a × $b = ?",
            answer = (a * b).toString(),
            type = PuzzleType.MULTIPLICATION,
            difficulty = PuzzleDifficulty.MEDIUM
        )
    }
    
    private fun generateDivision(): MathPuzzle {
        val b = random.nextInt(2, 5)
        val result = random.nextInt(2, 10)
        val a = b * result
        
        return MathPuzzle(
            id = UUID.randomUUID().toString(),
            question = "$a ÷ $b = ?",
            answer = result.toString(),
            type = PuzzleType.DIVISION,
            difficulty = PuzzleDifficulty.HARD
        )
    }
    
    fun verifyAnswer(puzzle: MathPuzzle, userAnswer: String): Boolean {
        return puzzle.answer == userAnswer.trim()
    }
}
