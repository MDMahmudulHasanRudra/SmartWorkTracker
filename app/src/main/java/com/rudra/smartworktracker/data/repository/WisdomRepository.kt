package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.model.Wisdom
import com.rudra.smartworktracker.model.WisdomCategory
import java.time.LocalDate

/** Built-in quote library shown on the Wisdom screen. */
class WisdomRepository {

    fun getWisdom(): List<Wisdom> = allWisdom

    /** A stable quote for the day, so it doesn't change on every recomposition. */
    fun quoteOfTheDay(date: LocalDate = LocalDate.now()): Wisdom =
        allWisdom[(date.toEpochDay() % allWisdom.size).toInt()]

    private val allWisdom = listOf(
        // Productivity
        Wisdom(text = "The key is not to prioritize what's on your schedule, but to schedule your priorities.", author = "Stephen Covey", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Work on your most important task for 90 minutes, without interruption. You'll be amazed at what you can accomplish.", author = "The 90/90/1 Rule", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Don't wait for the perfect moment. Take the moment and make it perfect.", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "The only way to do great work is to love what you do.", author = "Steve Jobs", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Success is not final, failure is not fatal: it is the courage to continue that counts.", author = "Winston Churchill", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Don't watch the clock; do what it does. Keep going.", author = "Sam Levenson", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "Hard work beats talent when talent doesn't work hard.", author = "Tim Notke", category = WisdomCategory.PRODUCTIVITY),
        Wisdom(text = "The body achieves what the mind believes.", author = "Napoleon Hill", category = WisdomCategory.PRODUCTIVITY),

        // Mindfulness
        Wisdom(text = "The present moment is filled with joy and happiness. If you are attentive, you will see it.", author = "Thich Nhat Hanh", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor.", author = "Thich Nhat Hanh", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "You can't stop the waves, but you can learn to surf.", author = "Jon Kabat-Zinn", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "Let food be thy medicine and medicine be thy food.", author = "Hippocrates", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "A healthy outside starts from the inside.", author = "Robert Urich", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "Movement is a medicine for creating change in a person's physical, emotional, and mental states.", author = "Carol Welch", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "What we think determines how we feel. What we feel determines how we act.", author = "Albert Ellis", category = WisdomCategory.MINDFULNESS),
        Wisdom(text = "The greatest wealth is health.", author = "Virgil", category = WisdomCategory.MINDFULNESS),

        // Habits
        Wisdom(text = "You do not rise to the level of your goals. You fall to the level of your systems.", author = "James Clear", category = WisdomCategory.HABITS),
        Wisdom(text = "Every action you take is a vote for the type of person you wish to become.", author = "James Clear", category = WisdomCategory.HABITS),
        Wisdom(text = "The secret of getting ahead is getting started.", author = "Mark Twain", category = WisdomCategory.HABITS),
        Wisdom(text = "The future depends on what you do today.", author = "Mahatma Gandhi", category = WisdomCategory.HABITS),
        Wisdom(text = "It does not matter how slowly you go as long as you do not stop.", author = "Confucius", category = WisdomCategory.HABITS),
        Wisdom(text = "Quality is not an act, it is a habit.", author = "Aristotle", category = WisdomCategory.HABITS),
        Wisdom(text = "If it doesn't challenge you, it won't change you.", author = "Fred DeVito", category = WisdomCategory.HABITS),
        Wisdom(text = "Small daily improvements are the key to staggering long-term results.", author = "Robin Sharma", category = WisdomCategory.HABITS),
        Wisdom(text = "Your diet is a bank account. Good food choices are good investments.", author = "Bethenny Frankel", category = WisdomCategory.HABITS)
    )
}
