package com.philia093.neofactory.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the list of chat lines and the two limits that shape what is on screen. */
class ChatLogTest {

    private final ChatLog log = new ChatLog();

    @Test
    void showsTheLinesOldestFirst() {
        log.addPlayer("<Player> first");
        log.addSystem("second");
        log.addError("third");

        List<ChatMessage> lines = log.visibleLines(false);

        assertEquals(3, lines.size());
        assertEquals("<Player> first", lines.get(0).text());
        assertEquals(ChatMessage.Kind.PLAYER, lines.get(0).kind());
        assertEquals(ChatMessage.Kind.SYSTEM, lines.get(1).kind());
        assertEquals(ChatMessage.Kind.ERROR, lines.get(2).kind());
    }

    @Test
    void drawsAtMostTheVisibleAmountOfLines() {
        for (int index = 0; index < ChatLog.VISIBLE_LINES + 4; index++) {
            log.addSystem("line " + index);
        }

        List<ChatMessage> lines = log.visibleLines(false);

        assertEquals(ChatLog.VISIBLE_LINES, lines.size(), "only the last lines are drawn");
        assertEquals("line " + (ChatLog.VISIBLE_LINES + 3), lines.get(lines.size() - 1).text());
    }

    @Test
    void forgetsTheOldestLinesWhenItIsFull() {
        for (int index = 0; index < ChatLog.MAX_LINES + 5; index++) {
            log.addSystem("line " + index);
        }

        assertEquals(ChatLog.MAX_LINES, log.size(), "only the limit is kept");

        List<ChatMessage> lines = log.visibleLines(true);
        assertEquals(ChatLog.VISIBLE_LINES, lines.size());
        assertEquals("line " + (ChatLog.MAX_LINES + 4), lines.get(lines.size() - 1).text(),
                "the newest line is the last one");
        assertEquals("line " + (ChatLog.MAX_LINES + 4 - ChatLog.VISIBLE_LINES + 1),
                lines.get(0).text(), "and the oldest lines are the ones that were dropped");
    }

    @Test
    void aLineLeavesTheScreenAfterItsTime() {
        log.addSystem("hello");

        log.update(ChatLog.MESSAGE_SECONDS - 0.5f);
        assertEquals(1, log.visibleLines(false).size(), "the line is still fresh");

        log.update(1.0f);
        assertTrue(log.visibleLines(false).isEmpty(), "the line left the screen");
        assertEquals(1, log.size(), "but it is still part of the history");
        assertEquals(1, log.visibleLines(true).size(), "which an open input line shows again");
    }

    @Test
    void ignoresTimeThatDoesNotPass() {
        log.addSystem("hello");

        log.update(-5.0f);
        log.update(0.0f);

        assertEquals(1, log.visibleLines(false).size());
    }

    @Test
    void clearDropsEveryLine() {
        log.addSystem("hello");
        log.addError("oops");

        log.clear();

        assertTrue(log.isEmpty());
        assertTrue(log.visibleLines(true).isEmpty());
    }

    @Test
    void aMessageWithoutTextStaysReadable() {
        log.add(new ChatMessage(null, null));

        ChatMessage line = log.visibleLines(false).get(0);

        assertEquals("", line.text());
        assertEquals(ChatMessage.Kind.SYSTEM, line.kind());
    }

    @Test
    void anEmptyLogShowsNothing() {
        assertFalse(log.visibleLines(true).iterator().hasNext());
    }
}
