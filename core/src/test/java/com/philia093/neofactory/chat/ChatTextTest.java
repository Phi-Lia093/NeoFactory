package com.philia093.neofactory.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the cutting of a long line, which keeps the end of it visible. */
class ChatTextTest {

    /** Measures every character as one pixel wide, which is enough for the cutting. */
    private static final ChatText.Width PER_CHARACTER = text -> text.length();

    @Test
    void aShortTextIsKeptWhole() {
        assertEquals("abc", ChatText.tail("abc", 10.0f, PER_CHARACTER));
        assertEquals(0, ChatText.firstVisibleIndex("abc", 3.0f, PER_CHARACTER));
    }

    @Test
    void aLongTextKeepsItsEnd() {
        assertEquals("cdef", ChatText.tail("abcdef", 4.0f, PER_CHARACTER));
        assertEquals(2, ChatText.firstVisibleIndex("abcdef", 4.0f, PER_CHARACTER));
    }

    @Test
    void nothingIsDrawnWhenNothingFits() {
        assertEquals("", ChatText.tail("abc", 0.0f, PER_CHARACTER));
        assertEquals("", ChatText.tail("abc", -4.0f, PER_CHARACTER));
        assertEquals(3, ChatText.firstVisibleIndex("abc", 0.0f, PER_CHARACTER));
    }

    @Test
    void theBeginningOfALongTextIsKept() {
        assertEquals("abc", ChatText.head("abcdef", 3.0f, PER_CHARACTER));
        assertEquals("abcdef", ChatText.head("abcdef", 40.0f, PER_CHARACTER));
        assertEquals("", ChatText.head("abc", 0.0f, PER_CHARACTER));
        assertEquals("", ChatText.head(null, 10.0f, PER_CHARACTER));
    }

    @Test
    void anEmptyTextStaysEmpty() {
        assertEquals("", ChatText.tail("", 20.0f, PER_CHARACTER));
        assertEquals("", ChatText.tail(null, 20.0f, PER_CHARACTER));
        assertTrue(ChatText.tail("/give stone 64", 100.0f, PER_CHARACTER).startsWith("/"));
    }
}
