package com.philia093.neofactory.chat;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.chat.command.CommandRegistry;
import com.philia093.neofactory.support.TestCommandContext;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the one input line that serves both the chat and the commands. */
class ChatControllerTest {

    private final TestCommandContext context = new TestCommandContext();
    private final ChatController chat = new ChatController(CommandRegistry.withDefaults(), context);

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theSlashKeyOpensTheCommandLine() {
        assertFalse(chat.isOpen());
        assertTrue(ChatController.isOpenKey(ChatController.KEY_COMMAND));
        assertTrue(ChatController.isOpenKey(ChatController.KEY_CHAT));
        assertFalse(ChatController.isOpenKey(Input.Keys.E));

        chat.open(ChatController.KEY_COMMAND);
        chat.keyTyped('/');

        assertTrue(chat.isOpen());
        assertEquals("/", chat.text(), "the slash starts the command");
        assertTrue(ChatController.discardsOpeningCharacter(ChatController.KEY_CHAT));
        assertFalse(ChatController.discardsOpeningCharacter(ChatController.KEY_COMMAND));
    }

    @Test
    void theChatKeyOpensAnEmptyLine() {
        chat.open(ChatController.KEY_CHAT);

        chat.keyTyped('t');
        chat.keyTyped('o');

        assertEquals("o", chat.text(), "the letter of the opening key was dropped");
    }

    @Test
    void everyKeyBelongsToTheOpenLine() {
        chat.open(ChatController.KEY_CHAT);

        assertTrue(chat.keyDown(Input.Keys.W), "walking must not happen behind the line");
        assertTrue(chat.keyDown(Input.Keys.E), "and the inventory must not open");
        assertTrue(chat.keyDown(Input.Keys.ESCAPE));

        assertFalse(chat.isOpen(), "escape closes the line");
        assertFalse(chat.keyDown(Input.Keys.ESCAPE), "the next escape belongs to the game again");
    }

    @Test
    void aKeyIsNotTakenWhileTheLineIsClosed() {
        assertFalse(chat.keyDown(Input.Keys.W));
        assertFalse(chat.keyTyped('a'));
        assertTrue(chat.visibleLines().isEmpty());
    }

    @Test
    void enterSendsAMessage() {
        say("hello");

        assertEquals(1, chat.visibleLines().size());
        assertEquals("<Player> hello", chat.visibleLines().get(0).text());
        assertEquals(ChatMessage.Kind.PLAYER, chat.visibleLines().get(0).kind());
        assertFalse(chat.isOpen(), "sending closes the line");
    }

    @Test
    void enterRunsACommand() {
        chat.open(ChatController.KEY_COMMAND);
        type("/seed");

        assertTrue(chat.keyDown(Input.Keys.ENTER));

        assertEquals("Seed: 1234", chat.visibleLines().get(0).text());
        assertEquals(ChatMessage.Kind.SYSTEM, chat.visibleLines().get(0).kind());
        assertFalse(chat.isOpen());
    }

    @Test
    void aLineBehindASlashIsNeverAMessage() {
        chat.open(ChatController.KEY_COMMAND);
        type("/nope");

        chat.submit();

        ChatMessage line = chat.visibleLines().get(0);
        assertEquals(ChatMessage.Kind.ERROR, line.kind());
        assertTrue(line.text().startsWith("Unknown command"), line.text());
    }

    @Test
    void anEmptyLineOnlyClosesTheChat() {
        chat.open(ChatController.KEY_CHAT);

        chat.keyDown(Input.Keys.ENTER);

        assertFalse(chat.isOpen());
        assertTrue(chat.visibleLines().isEmpty(), "nothing was added to the chat");
    }

    @Test
    void escapeForgetsTheLine() {
        chat.open(ChatController.KEY_CHAT);
        type("hello");

        chat.keyDown(Input.Keys.ESCAPE);

        assertFalse(chat.isOpen());
        assertTrue(chat.visibleLines().isEmpty(), "the line was never sent");
    }

    @Test
    void theArrowsWalkThroughTheLinesThatWereSentBefore() {
        say("first");
        say("second");
        chat.open(ChatController.KEY_CHAT);

        chat.keyDown(Input.Keys.UP);
        assertEquals("second", chat.text(), "the line before comes first");
        chat.keyDown(Input.Keys.UP);
        assertEquals("first", chat.text());
        chat.keyDown(Input.Keys.DOWN);
        assertEquals("second", chat.text());
        chat.keyDown(Input.Keys.DOWN);
        assertEquals("", chat.text(), "and walking on returns to the empty line");
    }

    @Test
    void theMessagesLeaveTheScreenButComeBackWhileTyping() {
        say("hello");
        assertEquals(1, chat.visibleLines().size());

        chat.update(ChatLog.MESSAGE_SECONDS + 1.0f);
        assertTrue(chat.visibleLines().isEmpty(), "the message left the screen");

        chat.open(ChatController.KEY_CHAT);
        assertEquals(1, chat.visibleLines().size(), "the recent conversation is shown again");
        chat.close();
        assertTrue(chat.visibleLines().isEmpty());
    }

    /** Sends a message through the chat, the way the keyboard does. */
    private void say(String line) {
        chat.open(ChatController.KEY_CHAT);
        chat.keyTyped('t');
        type(line);
        chat.submit();
    }

    /** Types every character of a line into the open prompt. */
    private void type(String line) {
        for (int index = 0; index < line.length(); index++) {
            chat.keyTyped(line.charAt(index));
        }
    }
}
