package com.example.planner

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

object Periods {

    fun key(period: PeriodType, date: LocalDate): String = when (period) {
        PeriodType.DAILY -> date.toString()
        PeriodType.WEEKLY -> "%d-W%02d".format(
            date.get(IsoFields.WEEK_BASED_YEAR),
            date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        )
        PeriodType.MONTHLY -> "%d-%02d".format(date.year, date.monthValue)
    }

    /** Cantidad de períodos COMPLETOS ya cerrados entre la creación y hoy (sin contar el actual). */
    fun closedPeriods(period: PeriodType, created: LocalDate, today: LocalDate): Long {
        if (today.isBefore(created)) return 0
        return when (period) {
            PeriodType.DAILY -> ChronoUnit.DAYS.between(created, today)
            PeriodType.WEEKLY -> {
                val a = created.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val b = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ChronoUnit.WEEKS.between(a, b)
            }
            PeriodType.MONTHLY -> ChronoUnit.MONTHS.between(
                created.withDayOfMonth(1), today.withDayOfMonth(1)
            )
        }
    }

    fun label(period: PeriodType): String = when (period) {
        PeriodType.DAILY -> "Diario"
        PeriodType.WEEKLY -> "Semanal"
        PeriodType.MONTHLY -> "Mensual"
    }
}
