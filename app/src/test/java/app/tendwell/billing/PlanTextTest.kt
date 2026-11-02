package app.tendwell.billing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class PlanTextTest {
    private val us = Locale.US
    private val yearly = PlanOffer(ProPlan.Yearly, "$24.99", 24_990_000, "USD", 7, "y")
    private val monthly = PlanOffer(ProPlan.Monthly, "$3.99", 3_990_000, "USD", 7, "m")
    private val lifetime = PlanOffer(ProPlan.Lifetime, "$49.99", 49_990_000, "USD", 0, null)

    @Test
    fun trialLengthFromBillingPeriod() {
        assertEquals(7, PlanText.trialDays("P7D"))
        assertEquals(7, PlanText.trialDays("P1W"))
        assertEquals(30, PlanText.trialDays("P1M"))
        assertEquals(0, PlanText.trialDays("nonsense"))
    }

    @Test
    fun yearlyShowsTheMonthlyEquivalent() {
        assertEquals("$2.08", PlanText.perMonth(24_990_000, "USD", us))
        assertEquals("€2.08", PlanText.perMonth(24_990_000, "EUR", us))
        assertEquals("About $2.08 a month · 7 days free", PlanText.subLine(yearly, us))
        assertEquals("About $2.08 a month", PlanText.subLine(yearly.copy(trialDays = 0), us))
    }

    @Test
    fun linesMatchTheMockup() {
        assertEquals("$24.99 a year", PlanText.priceLine(yearly))
        assertEquals("$3.99 a month", PlanText.priceLine(monthly))
        assertEquals("$49.99 once", PlanText.priceLine(lifetime))
        assertEquals("7 days free", PlanText.subLine(monthly, us))
        assertEquals("Pay once, keep Pro", PlanText.subLine(lifetime, us))
        assertEquals("Try Pro free for 7 days", PlanText.headline(yearly))
        assertEquals("Get Tendwell Pro", PlanText.headline(lifetime))
        assertEquals("Start 7-day free trial", PlanText.cta(yearly))
        assertEquals("Buy lifetime for $49.99", PlanText.cta(lifetime))
        assertEquals("Subscribe for $3.99 a month", PlanText.cta(monthly.copy(trialDays = 0)))
        assertEquals("7 days free, then $24.99 a year.", PlanText.termsLead(yearly))
        assertEquals("One payment of $49.99. No subscription.", PlanText.termsLead(lifetime))
        assertEquals("$3.99 a month, renewing until you cancel.", PlanText.termsLead(monthly.copy(trialDays = 0)))
    }

    @Test
    fun cancelDateIsTheEndOfTheTrial() {
        assertEquals("10 November", PlanText.cancelBy(LocalDate.of(2026, 11, 3), 7, us))
        assertEquals("4 January", PlanText.cancelBy(LocalDate.of(2026, 12, 28), 7, us))
    }
}
