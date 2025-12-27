/*	This file is part of SubjectivePlayer for Android.
 *
 *	SubjectivePlayer for Android is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	SubjectivePlayer for Android is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with SubjectivePlayer for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.univie.subjectiveplayer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests for Session class.
 * Focuses on Session-specific behavior: utility methods, training logic, and
 * runtime behavior like missing file filtering.
 *
 * Config file parsing is tested in ConfigFileTest.java.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SessionTest {

    private File tempDir;

    @Before
    public void setUp() throws IOException {
        Session.reset();
        tempDir = File.createTempFile("test_videos", "");
        tempDir.delete();
        tempDir.mkdirs();
        Configuration.sFolderVideos = tempDir;
    }

    @After
    public void tearDown() {
        if (tempDir != null) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempDir.delete();
        }
    }

    // ========== Utility Methods ==========
    // These are used by config parsers and during session playback

    @Test
    public void parseBreakDuration_withValidDuration() {
        assertEquals(60, Session.parseBreakDuration("BREAK 60"));
        assertEquals(0, Session.parseBreakDuration("BREAK 0"));
    }

    @Test
    public void parseBreakDuration_withoutDuration() {
        assertEquals(-1, Session.parseBreakDuration("BREAK"));
        assertEquals(-1, Session.parseBreakDuration("BREAK abc"));
    }

    @Test
    public void parseMethodType_allValidMethods() {
        assertEquals(Methods.TYPE_ACR_CATEGORICAL, Session.parseMethodType("METHOD ACR"));
        assertEquals(Methods.TYPE_CONTINUOUS, Session.parseMethodType("METHOD CONTINUOUS"));
        assertEquals(Methods.TYPE_DSIS_CATEGORICAL, Session.parseMethodType("METHOD DSIS"));
        assertEquals(Methods.TYPE_TIME_CONTINUOUS, Session.parseMethodType("METHOD TIME_CONTINUOUS"));
    }

    @Test
    public void parseMethodType_caseInsensitive() {
        assertEquals(Methods.TYPE_ACR_CATEGORICAL, Session.parseMethodType("method acr"));
    }

    @Test
    public void parseMethodType_invalidReturnsUndefined() {
        assertEquals(Methods.UNDEFINED, Session.parseMethodType("METHOD UNKNOWN"));
        assertEquals(Methods.UNDEFINED, Session.parseMethodType("METHOD"));
    }

    @Test
    public void parseMessageDirective_extractsMessage() {
        assertEquals("Welcome!", Session.parseMessageDirective("START_MESSAGE Welcome!", "START_MESSAGE"));
    }

    @Test
    public void parseMessageDirective_convertsEscapedNewlines() {
        assertEquals("Line1\nLine2\nLine3",
                Session.parseMessageDirective("START_MESSAGE Line1\\nLine2\\nLine3", "START_MESSAGE"));
    }

    @Test
    public void parseMessageDirective_emptyMessageReturnsNull() {
        assertNull(Session.parseMessageDirective("START_MESSAGE ", "START_MESSAGE"));
    }

    // ========== Training Section Logic ==========
    // Tests Session's training state management

    @Test
    public void trainingSection_requiresBothMarkers() {
        Session.sTrainingStartIndex = 0;
        Session.sTrainingEndIndex = -1;
        assertFalse(Session.hasTrainingSection());

        Session.sTrainingEndIndex = 2;
        assertTrue(Session.hasTrainingSection());
    }

    @Test
    public void isTrainingTrack_checksRange() {
        Session.sTrainingStartIndex = 2;
        Session.sTrainingEndIndex = 4;

        assertFalse(Session.isTrainingTrack(1));
        assertTrue(Session.isTrainingTrack(2));
        assertTrue(Session.isTrainingTrack(3));
        assertTrue(Session.isTrainingTrack(4));
        assertFalse(Session.isTrainingTrack(5));
    }

    @Test
    public void isFirstTrainingTrack_checksExactIndex() {
        Session.sTrainingStartIndex = 2;
        Session.sTrainingEndIndex = 4;

        assertFalse(Session.isFirstTrainingTrack(1));
        assertTrue(Session.isFirstTrainingTrack(2));
        assertFalse(Session.isFirstTrainingTrack(3));
    }

    @Test
    public void isLastTrainingTrack_checksExactIndex() {
        Session.sTrainingStartIndex = 2;
        Session.sTrainingEndIndex = 4;

        assertFalse(Session.isLastTrainingTrack(3));
        assertTrue(Session.isLastTrainingTrack(4));
        assertFalse(Session.isLastTrainingTrack(5));
    }

    // ========== readVideosFromFile - Session-specific behavior ==========
    // Tests behavior unique to Session: missing file removal, state population

    @Test
    public void readVideosFromFile_removesMissingVideos() throws IOException {
        createTempFile(tempDir, "exists.mp4");

        File config = createTempConfigFile("exists.mp4\nmissing.mp4\n");
        Session.readVideosFromFile(config);

        assertEquals(1, Session.sTracks.size());
        assertEquals("exists.mp4", Session.sTracks.get(0));

        config.delete();
    }

    @Test
    public void readVideosFromFile_keepsBreakCommands() throws IOException {
        File config = createTempConfigFile("BREAK 60\nBREAK\n");
        Session.readVideosFromFile(config);

        assertEquals(2, Session.sTracks.size());
        assertTrue(Session.isBreakCommand(Session.sTracks.get(0)));
        assertTrue(Session.isBreakCommand(Session.sTracks.get(1)));

        config.delete();
    }

    @Test
    public void readVideosFromFile_populatesSessionState() throws IOException {
        createTempFile(tempDir, "video.mp4");

        File config = createTempConfigFile(
            "METHOD CONTINUOUS\n" +
            "START_MESSAGE Hello\n" +
            "FINISH_MESSAGE Bye\n" +
            "video.mp4\n"
        );
        Session.readVideosFromFile(config);

        assertEquals(Methods.TYPE_CONTINUOUS, Session.sCurrentMethod);
        assertEquals("Hello", Session.sStartMessage);
        assertEquals("Bye", Session.sFinishMessage);
        assertEquals(1, Session.sTracks.size());

        config.delete();
    }

    @Test
    public void readVideosFromFile_supportsJsonFormat() throws IOException {
        createTempFile(tempDir, "video.mp4");

        File config = new File(tempDir, "test.json");
        writeFile(config, "{\"method\": \"ACR\", \"playlist\": [\"video.mp4\"]}");

        Session.readVideosFromFile(config);

        assertEquals(Methods.TYPE_ACR_CATEGORICAL, Session.sCurrentMethod);
        assertEquals(1, Session.sTracks.size());
        assertEquals("video.mp4", Session.sTracks.get(0));

        config.delete();
    }

    @Test
    public void readVideosFromFile_adjustsTrainingIndicesOnMissingFiles() throws IOException {
        createTempFile(tempDir, "training.mp4");
        createTempFile(tempDir, "test.mp4");
        // missing.mp4 intentionally not created

        File config = createTempConfigFile(
            "missing.mp4\n" +
            "TRAINING_START\n" +
            "training.mp4\n" +
            "TRAINING_END\n" +
            "test.mp4\n"
        );
        Session.readVideosFromFile(config);

        // missing.mp4 removed, so training.mp4 is now at index 0
        assertEquals(2, Session.sTracks.size());
        assertEquals(0, Session.sTrainingStartIndex);
        assertEquals(0, Session.sTrainingEndIndex);

        config.delete();
    }

    // ========== Helpers ==========

    private File createTempConfigFile(String content) throws IOException {
        File tempFile = File.createTempFile("test_config", ".cfg");
        writeFile(tempFile, content);
        return tempFile;
    }

    private void createTempFile(File dir, String filename) throws IOException {
        new File(dir, filename).createNewFile();
    }

    private void writeFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
