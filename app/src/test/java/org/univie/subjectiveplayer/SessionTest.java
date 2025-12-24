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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SessionTest {

    @Before
    public void setUp() {
        Session.reset();
    }

    // ========== parseBreakDuration ==========
    // Config files can have "BREAK" or "BREAK <seconds>" to insert pauses

    @Test
    public void parseBreakDuration_withValidDuration() {
        assertEquals(Session.parseBreakDuration("BREAK 60"), 60);
        assertEquals(Session.parseBreakDuration("BREAK 0"), 0);
    }

    @Test
    public void parseBreakDuration_withoutDuration() {
        // Missing or invalid duration returns -1 (indefinite break)
        assertEquals(Session.parseBreakDuration("BREAK"), -1);
        assertEquals(Session.parseBreakDuration("BREAK abc"), -1);
    }

    // ========== parseMethodType ==========
    // Config files specify rating method via "METHOD <type>" directive

    @Test
    public void parseMethodType_allValidMethods() {
        assertEquals(Session.parseMethodType("METHOD ACR"), Methods.TYPE_ACR_CATEGORICAL);
        assertEquals(Session.parseMethodType("METHOD CONTINUOUS"), Methods.TYPE_CONTINUOUS);
        assertEquals(Session.parseMethodType("METHOD DSIS"), Methods.TYPE_DSIS_CATEGORICAL);
        assertEquals(Session.parseMethodType("METHOD TIME_CONTINUOUS"), Methods.TYPE_TIME_CONTINUOUS);
    }

    @Test
    public void parseMethodType_caseInsensitive() {
        assertEquals(Session.parseMethodType("method acr"), Methods.TYPE_ACR_CATEGORICAL);
    }

    @Test
    public void parseMethodType_invalidReturnsUndefined() {
        assertEquals(Session.parseMethodType("METHOD UNKNOWN"), Methods.UNDEFINED);
        assertEquals(Session.parseMethodType("METHOD"), Methods.UNDEFINED);
    }

    // ========== parseMessageDirective ==========
    // Custom messages can be set via START_MESSAGE, FINISH_MESSAGE, TRAINING_MESSAGE

    @Test
    public void parseMessageDirective_extractsMessage() {
        assertEquals(Session.parseMessageDirective("START_MESSAGE Welcome!", "START_MESSAGE"), "Welcome!");
    }

    @Test
    public void parseMessageDirective_convertsEscapedNewlines() {
        // Config files use \n for line breaks since they're single-line
        assertEquals(Session.parseMessageDirective("START_MESSAGE Line1\\nLine2\\nLine3", "START_MESSAGE"), "Line1\nLine2\nLine3");
    }

    @Test
    public void parseMessageDirective_emptyMessageReturnsNull() {
        assertNull(Session.parseMessageDirective("START_MESSAGE ", "START_MESSAGE"));
    }

    // ========== Training section logic ==========
    // Training videos are marked with TRAINING_START/TRAINING_END in config

    @Test
    public void trainingSection_requiresBothMarkers() {
        // Incomplete training section (only start) should not be recognized
        Session.sTrainingStartIndex = 0;
        Session.sTrainingEndIndex = -1;
        assertFalse(Session.hasTrainingSection());

        Session.sTrainingEndIndex = 2;
        assertTrue(Session.hasTrainingSection());
    }

    @Test
    public void isTrainingTrack_checksRange() {
        // Training section spans indices 2-4 inclusive
        Session.sTrainingStartIndex = 2;
        Session.sTrainingEndIndex = 4;

        assertFalse(Session.isTrainingTrack(1));
        assertTrue(Session.isTrainingTrack(2));
        assertTrue(Session.isTrainingTrack(3));
        assertTrue(Session.isTrainingTrack(4));
        assertFalse(Session.isTrainingTrack(5));
    }

    // ========== readVideosFromFile ==========
    // Full config file parsing - the main entry point for loading a test session

    @Test
    public void readVideosFromFile_parsesAllDirectives() throws IOException {
        // Comprehensive test: METHOD, messages, videos, and BREAK commands
        File tempDir = createTempDir();
        Configuration.sFolderVideos = tempDir;
        createTempFile(tempDir, "video.mp4");

        File config = createTempConfigFile(
            "METHOD ACR\n" +
            "START_MESSAGE Welcome\\nTo the test\n" +
            "FINISH_MESSAGE Goodbye!\n" +
            "video.mp4\n" +
            "BREAK 30\n"
        );

        Session.readVideosFromFile(config);

        assertEquals(Session.sCurrentMethod, Methods.TYPE_ACR_CATEGORICAL);
        assertEquals(Session.sStartMessage, "Welcome\nTo the test");
        assertEquals(Session.sFinishMessage, "Goodbye!");
        assertEquals(Session.sTracks.size(), 2);
        assertEquals(Session.sTracks.get(0), "video.mp4");
        assertEquals(Session.sTracks.get(1), "BREAK 30");

        cleanup(config, tempDir);
    }

    @Test
    public void readVideosFromFile_parsesTrainingSection() throws IOException {
        // Training section: videos between TRAINING_START and TRAINING_END
        File tempDir = createTempDir();
        Configuration.sFolderVideos = tempDir;
        createTempFile(tempDir, "training.mp4");
        createTempFile(tempDir, "test.mp4");

        File config = createTempConfigFile(
            "TRAINING_START\n" +
            "training.mp4\n" +
            "TRAINING_END\n" +
            "test.mp4\n"
        );

        Session.readVideosFromFile(config);

        // training.mp4 is at index 0, test.mp4 at index 1
        assertEquals(Session.sTrainingStartIndex, 0);
        assertEquals(Session.sTrainingEndIndex, 0);
        assertTrue(Session.hasTrainingSection());
        assertEquals(Session.sTracks.size(), 2);

        cleanup(config, tempDir);
    }

    @Test
    public void readVideosFromFile_removesMissingVideos() throws IOException {
        // Videos that don't exist on disk are silently removed from playlist
        File tempDir = createTempDir();
        Configuration.sFolderVideos = tempDir;
        createTempFile(tempDir, "exists.mp4");

        File config = createTempConfigFile("exists.mp4\nmissing.mp4\n");
        Session.readVideosFromFile(config);

        assertEquals(Session.sTracks.size(), 1);
        assertEquals(Session.sTracks.get(0), "exists.mp4");

        cleanup(config, tempDir);
    }

    @Test
    public void readVideosFromFile_keepsBreakCommands() throws IOException {
        // BREAK commands are kept even though they're not actual files
        File tempDir = createTempDir();
        Configuration.sFolderVideos = tempDir;

        File config = createTempConfigFile("BREAK 60\nBREAK\n");
        Session.readVideosFromFile(config);

        assertEquals(Session.sTracks.size(), 2);
        assertTrue(Session.isBreakCommand(Session.sTracks.get(0)));

        cleanup(config, tempDir);
    }

    // ========== Helpers ==========

    private File createTempConfigFile(String content) throws IOException {
        File tempFile = File.createTempFile("test_config", ".cfg");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }

    private File createTempDir() throws IOException {
        File tempDir = File.createTempFile("test_videos", "");
        tempDir.delete();
        tempDir.mkdirs();
        return tempDir;
    }

    private void createTempFile(File dir, String filename) throws IOException {
        new File(dir, filename).createNewFile();
    }

    private void cleanup(File config, File tempDir) {
        config.delete();
        File[] files = tempDir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        tempDir.delete();
    }
}
