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
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class SubjectivePlayer extends Activity {

	private static final String TAG = SubjectivePlayer.class.getSimpleName();

	static final int DIALOG_ABOUT_ID = 0;
	static final int DIALOG_FILEBROWSER = 1;
	static final int DIALOG_METHODBROWSER = 2;
	static final int DIALOG_EMPTY = 3;
    static final int DIALOG_ID_ALREADY_USED = 4;

	private static EditText mEditId = null;
	
	/*
	 * Main code
	 */
	/**
	 * Called when the activity is being started or restarted
	 */
	@Override
	public void onResume() {
		super.onResume();

		// reload any updated preferences
		Configuration.setPreferences(PreferenceManager
				.getDefaultSharedPreferences(getBaseContext()));

		
		// reset the session
		Session.reset();

        mEditId.setText("");
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// initialize the SD card for retrieving all config and media files
		try {
			Configuration.initialize(getApplicationContext());
		} catch (Exception e) {
			Log.e(TAG, "Error while initializing: " + e.getMessage());
			e.printStackTrace();
			// TODO do something useful, not just quit
			finish();
		}
		
		mEditId = (EditText) findViewById(R.id.edit_id);
		mEditId.setInputType(InputType.TYPE_NULL);
		mEditId.setOnTouchListener(new View.OnTouchListener() {
	        public boolean onTouch(View v, MotionEvent event) {
	        mEditId.setInputType(InputType.TYPE_CLASS_NUMBER);
	        mEditId.onTouchEvent(event); // call native handler
	        return true; // consume touch even
	        } 
	    });

		Button buttonStart = (Button) findViewById(R.id.button_start);
		buttonStart.setOnClickListener(mButtonStartListener);
	}		


	/**
	 * Populates the options menu
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.subjective_menu, menu);
		return true;
	}

	/**
	 * Handles the option menu selections
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		// change to the preferences activity
		case R.id.menu_preferences:
			Intent prefIntent = new Intent();
			prefIntent.setClass(getApplicationContext(),
					SubjectivePlayerPreferences.class);
			startActivity(prefIntent);
			return true;

			// Display the about dialog
		case R.id.menu_about:
			showDialog(DIALOG_ABOUT_ID);
			return true;

			// Exit the application
		case R.id.menu_exit:
			SubjectivePlayer.this.finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Handles the dialogs shown in the application
	 */
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {

		// display the about dialog
		case DIALOG_ABOUT_ID:
			AlertDialog.Builder builderAbout = new AlertDialog.Builder(this);
			builderAbout.setTitle(R.string.about_caption).setMessage(
					R.string.about_body).setCancelable(false)
					.setPositiveButton("Close",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							});
			dialog = (AlertDialog) builderAbout.create();
			break;

		// display an error dialog if the fields for participant ID, method or
		// config file are empty
		case DIALOG_EMPTY:
			AlertDialog.Builder builderEmpty = new AlertDialog.Builder(this);
			builderEmpty.setTitle(R.string.error_nodata_caption).setMessage(
					R.string.error_nodata_body).setCancelable(false)
					.setPositiveButton("Close",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.dismiss();
								}
							});
			dialog = (AlertDialog) builderEmpty.create();
			break;

        case DIALOG_ID_ALREADY_USED:
            AlertDialog.Builder builderUsed = new AlertDialog.Builder(this);
            builderUsed.setTitle(R.string.error_id_used_caption).setMessage(
                    R.string.error_id_used_body).setCancelable(false)
                    .setPositiveButton("Close",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.dismiss();
                                }
                            });
            dialog = (AlertDialog) builderUsed.create();
            break;

		default:
			dialog = null;
		}
		return dialog;
	}

	/*
	 * Button Listeners
	 */

	/**
	 * Click listener for the file browser button
	 */
	private OnClickListener mButtonFileBrowserListener = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_FILEBROWSER);
		}
	};

	/**
	 * Click listener for the method selection button
	 */
	private OnClickListener mButtonMethodBrowserListener = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_METHODBROWSER);
		}
	};

	/**
	 * Click listener for the start button
	 */
	private OnClickListener mButtonStartListener = new OnClickListener() {
		public void onClick(View v) {
			// get the contents of the ID
			EditText editId = (EditText) findViewById(R.id.edit_id);
			Editable editIdString = (Editable) editId.getText();

            // P.NATS: Only allow ACR
            Session.sCurrentMethod = Methods.TYPE_ACR_CATEGORICAL;

			if (editIdString.toString().equalsIgnoreCase("")) {
				showDialog(DIALOG_EMPTY);
			} else {
				Session.sParticipantId = Integer.parseInt(editIdString.toString());

                // set config file from ID
                Configuration.sFileConfig = new File(
                        Configuration.sFolderApproot, "playlist" + Session.sParticipantId + "." + Configuration.sConfigSuffix);

                if (!(Configuration.sFileConfig.exists() && Configuration.sFileConfig.canRead())) {
                    showDialog(DIALOG_EMPTY);
                    return;
                }

                // check if ID hasn't been used already
                if (Configuration.sAllowDuplicateIds == false) {
                    if (Logger.idExists(Session.sParticipantId)) {
                        showDialog(DIALOG_ID_ALREADY_USED);
                        return;
                    }
                }

				Session.readVideosFromFile(Configuration.sFileConfig);
				
				// if we use continuous rating
				// TODO: refactor, this is unnecessarily duplicated
				if (Session.sCurrentMethod == Methods.TYPE_CONTINUOUS_RATING) {
					Intent sessionIntent = new Intent();
					sessionIntent.setClass(getApplicationContext(), SubjectivePlayerSession.class);
					startActivity(sessionIntent);
				} 
				// else, use the classical method of rating (one video after another)
				else {
					Intent sessionIntent = new Intent();
					sessionIntent.setClass(getApplicationContext(),
							SubjectivePlayerSession.class);
					startActivity(sessionIntent);					
				}
			}
		}
	};

}