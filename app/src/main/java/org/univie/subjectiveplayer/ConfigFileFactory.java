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

import java.io.File;

/**
 * Factory for creating config file parsers based on file extension.
 */
public class ConfigFileFactory {

    /** File extension for JSON config files */
    public static final String JSON_EXTENSION = ".json";

    /** File extension for legacy text config files */
    public static final String CFG_EXTENSION = ".cfg";

    /**
     * Creates the appropriate config file parser based on file extension.
     * @param file The config file to parse
     * @return A parsed config file (JsonConfigFile for .json, TextConfigFile for .cfg)
     */
    public static BaseConfigFile create(File file) {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(JSON_EXTENSION)) {
            return new JsonConfigFile(file);
        }
        return new TextConfigFile(file);
    }

    /**
     * Checks if a file is a supported config file format.
     * @param file The file to check
     * @return true if the file has a .json or .cfg extension
     */
    public static boolean isConfigFile(File file) {
        String filename = file.getName().toLowerCase();
        return filename.endsWith(JSON_EXTENSION) || filename.endsWith(CFG_EXTENSION);
    }

    /**
     * Checks if a filename is a JSON config file.
     * @param filename The filename to check
     * @return true if the file has a .json extension
     */
    public static boolean isJsonConfig(String filename) {
        return filename != null && filename.toLowerCase().endsWith(JSON_EXTENSION);
    }

    /**
     * Checks if a filename is a text config file.
     * @param filename The filename to check
     * @return true if the file has a .cfg extension
     */
    public static boolean isTextConfig(String filename) {
        return filename != null && filename.toLowerCase().endsWith(CFG_EXTENSION);
    }
}
