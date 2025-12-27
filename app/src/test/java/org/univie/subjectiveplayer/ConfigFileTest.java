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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ConfigFileTest {

    private File tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = File.createTempFile("config_test", "");
        tempDir.delete();
        tempDir.mkdirs();
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

    // ========== ConfigFileFactory ==========

    @Test
    public void factory_createsTextConfigForCfgExtension() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile, "video1.mp4\n");

        BaseConfigFile config = ConfigFileFactory.create(cfgFile);
        assertTrue(config instanceof TextConfigFile);
    }

    @Test
    public void factory_createsJsonConfigForJsonExtension() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"playlist\": [\"video1.mp4\"]}");

        BaseConfigFile config = ConfigFileFactory.create(jsonFile);
        assertTrue(config instanceof JsonConfigFile);
    }

    @Test
    public void factory_isConfigFile_detectsSupportedFormats() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        File jsonFile = new File(tempDir, "subject_1.json");
        File txtFile = new File(tempDir, "subject_1.txt");
        cfgFile.createNewFile();
        jsonFile.createNewFile();
        txtFile.createNewFile();

        assertTrue(ConfigFileFactory.isConfigFile(cfgFile));
        assertTrue(ConfigFileFactory.isConfigFile(jsonFile));
        assertFalse(ConfigFileFactory.isConfigFile(txtFile));
    }

    // ========== TextConfigFile ==========

    @Test
    public void textConfig_parsesBasicPlaylist() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile, "video1.mp4\nvideo2.mp4\nvideo3.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertEquals(3, config.getTotalVideoCount());
        assertEquals(3, config.getEntries().size());
        assertFalse(config.hasErrors());
    }

    @Test
    public void textConfig_parsesMethod() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile, "METHOD ACR\nvideo1.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertEquals(Methods.TYPE_ACR_CATEGORICAL, config.getMethod());
    }

    @Test
    public void textConfig_parsesMessages() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile,
            "START_MESSAGE Welcome\\nTo the test\n" +
            "FINISH_MESSAGE Goodbye!\n" +
            "video1.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertEquals("Welcome\nTo the test", config.getStartMessage());
        assertEquals("Goodbye!", config.getFinishMessage());
    }

    @Test
    public void textConfig_parsesTrainingSection() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile,
            "TRAINING_START\n" +
            "training.mp4\n" +
            "TRAINING_END\n" +
            "video1.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertTrue(config.hasTrainingSection());
        assertEquals(0, config.getTrainingStartIndex());
        assertEquals(0, config.getTrainingEndIndex());
        assertEquals(1, config.getTrainingVideoCount());
        assertEquals(1, config.getVideoCount());
    }

    @Test
    public void textConfig_parsesBreaks() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile, "video1.mp4\nBREAK 30\nvideo2.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertEquals(1, config.getBreakCount());
        assertEquals(3, config.getEntries().size());
    }

    @Test
    public void textConfig_reportsInvalidMethod() throws IOException {
        File cfgFile = new File(tempDir, "subject_1.cfg");
        writeFile(cfgFile, "METHOD INVALID\nvideo1.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertTrue(config.hasErrors());
        assertTrue(config.getParseErrors().get(0).message.contains("Unknown METHOD"));
    }

    @Test
    public void textConfig_extractsIdFromFilename() throws IOException {
        File cfgFile = new File(tempDir, "subject_42.cfg");
        writeFile(cfgFile, "video1.mp4\n");

        BaseConfigFile config = new TextConfigFile(cfgFile);
        assertEquals("42", config.getId());
    }

    // ========== JsonConfigFile ==========

    @Test
    public void jsonConfig_parsesBasicPlaylist() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"playlist\": [\"video1.mp4\", \"video2.mp4\"]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertEquals(2, config.getTotalVideoCount());
        assertFalse(config.hasErrors());
    }

    @Test
    public void jsonConfig_parsesMethod() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"method\": \"CONTINUOUS\", \"playlist\": [\"video1.mp4\"]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertEquals(Methods.TYPE_CONTINUOUS, config.getMethod());
    }

    @Test
    public void jsonConfig_parsesCustomMessages() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{" +
            "\"custom_messages\": {" +
            "  \"start_message\": \"Welcome!\"," +
            "  \"finish_message\": \"Goodbye!\"," +
            "  \"training_message\": \"Training time\"" +
            "}," +
            "\"playlist\": [\"video1.mp4\"]" +
            "}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertEquals("Welcome!", config.getStartMessage());
        assertEquals("Goodbye!", config.getFinishMessage());
        assertEquals("Training time", config.getTrainingMessage());
    }

    @Test
    public void jsonConfig_parsesQuestionnaireMessages() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{" +
            "\"custom_messages\": {" +
            "  \"pre_questionnaire_message\": \"Please answer a few questions before we begin.\"," +
            "  \"post_questionnaire_message\": \"Please provide feedback on your experience.\"" +
            "}," +
            "\"playlist\": [\"video1.mp4\"]" +
            "}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertEquals("Please answer a few questions before we begin.", config.getPreQuestionnaireMessage());
        assertEquals("Please provide feedback on your experience.", config.getPostQuestionnaireMessage());
    }

    @Test
    public void jsonConfig_parsesTrainingSection() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{\"playlist\": [\"TRAINING_START\", \"training.mp4\", \"TRAINING_END\", \"video1.mp4\"]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasTrainingSection());
        assertEquals(0, config.getTrainingStartIndex());
        assertEquals(0, config.getTrainingEndIndex());
        assertEquals(1, config.getTrainingVideoCount());
        assertEquals(1, config.getVideoCount());
    }

    @Test
    public void jsonConfig_parsesBreaks() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"playlist\": [\"video1.mp4\", \"BREAK 60\", \"video2.mp4\"]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertEquals(1, config.getBreakCount());
        assertEquals(3, config.getEntries().size());
    }

    @Test
    public void jsonConfig_parsesPreQuestionnaire() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{" +
            "\"playlist\": [\"video1.mp4\"]," +
            "\"pre_questionnaire\": [" +
            "  {\"question\": \"How old are you?\", \"type\": \"number\"}," +
            "  {\"question\": \"Your gender?\", \"type\": \"radio\", \"options\": [\"Male\", \"Female\"]}" +
            "]" +
            "}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasPreQuestionnaire());
        assertEquals(2, config.getPreQuestionnaire().size());
        assertEquals("How old are you?", config.getPreQuestionnaire().getQuestions().get(0).getQuestion());
        assertEquals("number", config.getPreQuestionnaire().getQuestions().get(0).getType());
    }

    @Test
    public void jsonConfig_parsesPostQuestionnaire() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{" +
            "\"playlist\": [\"video1.mp4\"]," +
            "\"post_questionnaire\": [" +
            "  {\"question\": \"Any comments?\", \"type\": \"text\", \"required\": false}" +
            "]" +
            "}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasPostQuestionnaire());
        assertEquals(1, config.getPostQuestionnaire().size());
        assertFalse(config.getPostQuestionnaire().getQuestions().get(0).isRequired());
    }

    @Test
    public void jsonConfig_reportsInvalidMethod() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"method\": \"INVALID\", \"playlist\": [\"video1.mp4\"]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasErrors());
        assertTrue(config.getParseErrors().get(0).message.contains("Unknown method"));
    }

    @Test
    public void jsonConfig_reportsEmptyPlaylist() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"playlist\": []}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasErrors());
        assertTrue(config.getParseErrors().get(0).message.contains("Playlist is required"));
    }

    @Test
    public void jsonConfig_reportsMissingPlaylist() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{\"method\": \"ACR\"}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasErrors());
    }

    @Test
    public void jsonConfig_reportsInvalidJson() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile, "{ invalid json }");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasErrors());
        assertTrue(config.getParseErrors().get(0).message.contains("Invalid JSON"));
    }

    @Test
    public void jsonConfig_reportsInvalidQuestionType() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{\"playlist\": [\"video1.mp4\"], " +
            "\"pre_questionnaire\": [{\"question\": \"Test?\", \"type\": \"invalid\"}]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasErrors());
        assertTrue(config.getParseErrors().stream()
            .anyMatch(e -> e.message.contains("Invalid question type")));
    }

    @Test
    public void jsonConfig_reportsRadioWithoutOptions() throws IOException {
        File jsonFile = new File(tempDir, "subject_1.json");
        writeFile(jsonFile,
            "{\"playlist\": [\"video1.mp4\"], " +
            "\"pre_questionnaire\": [{\"question\": \"Choice?\", \"type\": \"radio\"}]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertTrue(config.hasErrors());
        assertTrue(config.getParseErrors().stream()
            .anyMatch(e -> e.message.contains("Options are required")));
    }

    @Test
    public void jsonConfig_extractsIdFromFilename() throws IOException {
        File jsonFile = new File(tempDir, "subject_123.json");
        writeFile(jsonFile, "{\"playlist\": [\"video1.mp4\"]}");

        BaseConfigFile config = new JsonConfigFile(jsonFile);
        assertEquals("123", config.getId());
    }

    // ========== Question validation ==========

    @Test
    public void question_validatesCorrectly() {
        Question q = new Question("Test?", "text", null, true);
        assertNull(q.validate());

        Question radioWithOptions = new Question("Choice?", "radio",
            java.util.Arrays.asList("A", "B"), null);
        assertNull(radioWithOptions.validate());
    }

    @Test
    public void question_requiresQuestionText() {
        Question q = new Question("", "text", null, null);
        assertNotNull(q.validate());
        assertTrue(q.validate().contains("Question text is required"));
    }

    @Test
    public void question_requiresType() {
        Question q = new Question("Test?", null, null, null);
        assertNotNull(q.validate());
        assertTrue(q.validate().contains("Question type is required"));
    }

    // ========== Helpers ==========

    private void writeFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
