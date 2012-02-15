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

/**
 * Class container for all implemented methods
 */
public abstract class Methods {

	// method names
	public static final String[] METHOD_NAMES = { "ACR - Categorical",
			"Continuous scale", "DSIS", "Continuous rating" };

	// method IDs, these should be in the same order
	// as the names above!
	public static final int UNDEFINED = -1;
	public static final int TYPE_ACR_CATEGORIGAL = 0;
	public static final int TYPE_CONTINUOUS = 1;
	public static final int TYPE_DSIS_CATEGORICAL = 2;
	public static final int TYPE_CONTINUOUS_RATING = 3;

	// labels for the ACR category
	public static final String[] LABELS_ACR = { "Bad", "Poor", "Fair", "Good",
			"Excellent" };

	// labels for the impairment scale
	public static final String[] LABELS_DSIS = { "Very Annoying", "Annoying",
			"Slightly Annoying", "Perceptible, but not annoying",
			"Imperceptible" };


}
