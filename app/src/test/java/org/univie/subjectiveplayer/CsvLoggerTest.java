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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CsvLoggerTest {

    private File tempLogsDir;

    @Before
    public void setUp() throws IOException {
        tempLogsDir = File.createTempFile("test_logs", "");
        tempLogsDir.delete();
        tempLogsDir.mkdirs();
        Configuration.sFolderLogs = tempLogsDir;

        Session.reset();
        Session.sParticipantId = 1;
        Session.sCurrentMethod = Methods.TYPE_ACR_CATEGORICAL;
    }

    @After
    public void tearDown() {
        CsvLogger.closeSessionLog();
        cleanup(tempLogsDir);
    }

    // ========== idExists ==========
    // Used to warn users if they're reusing a participant ID

    @Test
    public void idExists_findsMatchingId() throws IOException {
        // ID is extracted from filename prefix (before first underscore)
        createLogFile("1_20231215-143052_ACR_-_Categorical.csv");
        createLogFile("2_20231215-143052_ACR_-_Categorical.csv");

        assertTrue(CsvLogger.idExists(1));
        assertTrue(CsvLogger.idExists(2));
        assertFalse(CsvLogger.idExists(3));
    }

    // ========== Session logging ==========
    // Main output: CSV with video_position, video_name, rating, rated_at

    @Test
    public void sessionLog_writesHeaderAndRatings() throws IOException {
        // Simulates a full test session with ratings and a break
        CsvLogger.startSessionLog();
        CsvLogger.logRating(0, "video1.mp4", 5, 1702650000000L);
        CsvLogger.logRating(1, "video2.mp4", 4, 1702650010000L);
        CsvLogger.logBreak();
        CsvLogger.logRating(2, "video3.mp4", 3, 1702650020000L);
        CsvLogger.closeSessionLog();

        File[] files = tempLogsDir.listFiles();
        assertEquals(files.length, 1);
        // Filename format: <participantId>_<timestamp>_<method>.csv
        assertTrue(files[0].getName().startsWith("1_"));
        assertTrue(files[0].getName().contains("ACR"));

        List<String> lines = readFileLines(files[0]);
        assertEquals(lines.size(), 5);
        assertEquals(lines.get(0), "video_position,video_name,rating,rated_at");
        assertTrue(lines.get(1).startsWith("0,video1.mp4,5,"));
        assertTrue(lines.get(2).startsWith("1,video2.mp4,4,"));
        // BREAK entries have position -1 and empty rating/timestamp
        assertEquals(lines.get(3), "-1,BREAK,,");
        assertTrue(lines.get(4).startsWith("2,video3.mp4,3,"));
    }

    @Test
    public void logRating_autoStartsSession() throws IOException {
        // Logging without explicit startSessionLog() should work
        CsvLogger.logRating(0, "video.mp4", 5, System.currentTimeMillis());
        CsvLogger.closeSessionLog();

        assertEquals(tempLogsDir.listFiles().length, 1);
    }

    // ========== Time-continuous logging ==========
    // For TYPE_TIME_CONTINUOUS: multiple ratings per video go to same session log

    @Test
    public void continuousRatings_useSessionLog() throws IOException {
        // Continuous ratings now use the same format as regular ratings
        CsvLogger.startSessionLog();
        long baseTime = 1702650000000L;
        // Multiple ratings for same video during playback
        CsvLogger.logRating(0, "video.mp4", 50, baseTime);
        CsvLogger.logRating(0, "video.mp4", 55, baseTime + 1000);
        CsvLogger.logRating(0, "video.mp4", 60, baseTime + 2000);
        CsvLogger.closeSessionLog();

        File[] files = tempLogsDir.listFiles();
        assertEquals(files.length, 1);

        List<String> lines = readFileLines(files[0]);
        assertEquals(lines.size(), 4);
        assertEquals(lines.get(0), "video_position,video_name,rating,rated_at");
        assertTrue(lines.get(1).startsWith("0,video.mp4,50,"));
        assertTrue(lines.get(2).startsWith("0,video.mp4,55,"));
        assertTrue(lines.get(3).startsWith("0,video.mp4,60,"));
    }

    // ========== Helpers ==========

    private void createLogFile(String filename) throws IOException {
        new File(tempLogsDir, filename).createNewFile();
    }

    private List<String> readFileLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void cleanup(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            dir.delete();
        }
    }
}
