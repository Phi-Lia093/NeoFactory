package com.philia093.neofactory.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the input line: typing, editing, the command history and the two opening keys. */
class ChatPromptTest {

    private final ChatPrompt prompt = new ChatPrompt();

    @Test
    void theChatKeyDoesNotLeaveItsLetterInTheField() {
        prompt.open(true);

        prompt.type('t');

        assertEquals("", prompt.text(), "the letter of the opening key is dropped");
        prompt.type('h');
        prompt.type('i');
        assertEquals("hi", prompt.text());
    }

    @Test
    void theSlashOfTheCommandKeyIsKept() {
        prompt.open(false);

        prompt.type('/');

        assertEquals("/", prompt.text(), "the slash starts the command");
    }

    @Test
    void charactersAreAddedAtTheCursor() {
        prompt.open(false);
        prompt.type('a');
        prompt.type('c');
        prompt.moveLeft();

        prompt.type('b');

        assertEquals("abc", prompt.text());
        assertEquals(2, prompt.cursor());
    }

    @Test
    void backspaceAndDeleteRemoveAroundTheCursor() {
        prompt.open(false);
        prompt.type('a');
        prompt.type('b');
        prompt.type('c');
        prompt.moveLeft();

        prompt.backspace();

        assertEquals("ac", prompt.text(), "backspace takes the left character");

        prompt.delete();

        assertEquals("a", prompt.text(), "delete takes the right character");
    }

    @Test
    void theCursorStaysInsideTheLine() {
        prompt.open(false);
        prompt.type('a');

        prompt.moveLeft();
        prompt.moveLeft();
        assertEquals(0, prompt.cursor(), "the cursor stops in front of the line");

        prompt.moveToEnd();
        prompt.moveRight();
        assertEquals(1, prompt.cursor(), "and behind it");

        prompt.moveToStart();
        assertEquals(0, prompt.cursor());
    }

    @Test
    void controlCharactersAreNotText() {
        prompt.open(false);

        prompt.type('\n');
        prompt.type((char) 8);

        assertEquals("", prompt.text());
    }

    @Test
    void aLineIsCutAtItsMaximumLength() {
        prompt.open(false);

        for (int index = 0; index < ChatPrompt.MAX_LENGTH + 20; index++) {
            prompt.type('x');
        }

        assertEquals(ChatPrompt.MAX_LENGTH, prompt.text().length());
    }

    @Test
    void submitClosesTheLineAndKeepsItAsHistory() {
        prompt.open(false);
        prompt.type('/');
        prompt.type('h');

        String submitted = prompt.submit();

        assertEquals("/h", submitted);
        assertFalse(prompt.isOpen(), "the line is closed");
        assertEquals("", prompt.text(), "and empty again");
        assertEquals(1, prompt.historySize());
    }

    @Test
    void aBlankLineIsNotRemembered() {
        prompt.open(false);
        prompt.type(' ');

        prompt.submit();

        assertEquals(0, prompt.historySize());
    }

    @Test
    void cancelForgetsWhatWasTyped() {
        prompt.open(false);
        prompt.type('h');
        prompt.type('i');

        prompt.cancel();

        assertFalse(prompt.isOpen());
        assertEquals("", prompt.text());
        assertEquals(0, prompt.historySize(), "a cancelled line is no history");
    }

    @Test
    void theArrowsWalkThroughTheHistory() {
        submit("/first");
        submit("/second");
        prompt.open(false);
        prompt.type('n');
        prompt.type('o');
        prompt.type('w');

        prompt.showPrevious();
        assertEquals("/second", prompt.text(), "the most recent line comes first");
        prompt.showPrevious();
        assertEquals("/first", prompt.text());
        prompt.showPrevious();
        assertEquals("/first", prompt.text(), "the oldest line is the end of the walk");

        prompt.showNext();
        assertEquals("/second", prompt.text());
        prompt.showNext();
        assertEquals("now", prompt.text(), "walking past the end returns to what was typed");

        prompt.showNext();
        assertEquals("now", prompt.text(), "and stays there");
    }

    @Test
    void theHistoryIsCutAtItsLimit() {
        for (int index = 0; index < ChatPrompt.HISTORY_LIMIT + 3; index++) {
            submit("/line " + index);
        }
        prompt.open(false);

        prompt.showPrevious();
        assertEquals("/line " + (ChatPrompt.HISTORY_LIMIT + 2), prompt.text());
        for (int index = 0; index < ChatPrompt.HISTORY_LIMIT; index++) {
            prompt.showPrevious();
        }
        assertEquals("/line 3", prompt.text(), "the three oldest lines are forgotten");
        assertTrue(prompt.isOpen());
    }

    /** Types a line and submits it, which is what a player does with the chat. */
    private void submit(String line) {
        prompt.open(false);
        for (int index = 0; index < line.length(); index++) {
            prompt.type(line.charAt(index));
        }
        prompt.submit();
    }
}
