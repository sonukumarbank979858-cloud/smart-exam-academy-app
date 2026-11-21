package com.smartexam.academy.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.smartexam.academy.data.Attempt
import com.smartexam.academy.data.TestBundle

sealed class Route {
    // bottom-nav roots
    data object Home : Route()
    data class Exams(val categoryId: String? = null) : Route()
    data object Offline : Route()
    data object Profile : Route()

    data class ExamDetail(val examId: String) : Route()
    data class PackTests(val packId: String, val title: String, val isFree: Boolean) : Route()
    data class Instructions(val testId: String) : Route()
    data class TakeTest(val bundle: TestBundle, val hindi: Boolean) : Route()
    data class Result(val bundle: TestBundle, val attempt: Attempt) : Route()
    data class Analysis(val bundle: TestBundle, val attempt: Attempt) : Route()
    data class Review(val bundle: TestBundle, val attempt: Attempt) : Route()
    data class Leaderboard(val testId: String, val title: String) : Route()
    data object History : Route()
    data object EditProfile : Route()

    val isRoot get() = this is Home || this is Exams || this is Offline || this is Profile
}

class Nav {
    val stack = mutableStateListOf<Route>(Route.Home)
    val top: Route get() = stack.last()

    fun push(r: Route) { stack.add(r) }
    fun pop(): Boolean = if (stack.size > 1) { stack.removeAt(stack.size - 1); true } else false
    fun replace(r: Route) { stack[stack.size - 1] = r }
    fun tab(r: Route) { stack.clear(); stack.add(r) }

    /** Pop back to the nearest screen matching [pred]; if none, go home. */
    fun popTo(pred: (Route) -> Boolean) {
        val i = stack.indexOfLast(pred)
        if (i >= 0) while (stack.size > i + 1) stack.removeAt(stack.size - 1) else tab(Route.Home)
    }
}

val LocalNav = staticCompositionLocalOf<Nav> { error("Nav not provided") }
