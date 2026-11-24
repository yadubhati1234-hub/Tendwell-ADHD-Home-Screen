package app.tendwell.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureNoteTest {
    @Test
    fun affectedMakersGetTheNote() {
        assertEquals("Xiaomi", GestureNote.brandFor("Xiaomi", "Redmi"))
        assertEquals("Xiaomi", GestureNote.brandFor("unknown", "POCO"))
        assertEquals("Oppo", GestureNote.brandFor("OPPO", "OPPO"))
        assertEquals("Realme", GestureNote.brandFor("realme", "realme"))
    }

    @Test
    fun otherPhonesDoNot() {
        assertNull(GestureNote.brandFor("Google", "google"))
        assertNull(GestureNote.brandFor("samsung", "samsung"))
        assertNull(GestureNote.brandFor(null, null))
    }
}
