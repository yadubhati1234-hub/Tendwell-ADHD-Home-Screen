package app.tendwell.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutRulesTest {
    @Test fun phoneUprightIsOneColumn() = assertFalse(LayoutRules.isWide(411f, 914f))

    @Test fun phoneSidewaysIsTwoColumns() = assertTrue(LayoutRules.isWide(914f, 411f))

    @Test fun smallTabletSidewaysIsTwoColumns() = assertTrue(LayoutRules.isWide(1024f, 600f))

    @Test fun bigTabletSidewaysIsTwoColumns() = assertTrue(LayoutRules.isWide(1280f, 800f))

    @Test fun tabletUprightIsOneColumn() = assertFalse(LayoutRules.isWide(800f, 1280f))

    @Test fun squareFoldableIsOneColumn() = assertFalse(LayoutRules.isWide(840f, 840f))

    @Test fun narrowSplitScreenIsOneColumn() = assertFalse(LayoutRules.isWide(500f, 300f))
}
