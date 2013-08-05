/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.incallui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity {
    private CallButtonFragment mCallButtonFragment;
    private CallCardFragment mCallCardFragment;
    private AnswerFragment mAnswerFragment;

    @Override
    protected void onCreate(Bundle icicle) {
        Logger.d(this, "onCreate()...  this = " + this);

        super.onCreate(icicle);

        // set this flag so this activity will stay in front of the keyguard
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate everything in incall_screen.xml and add it to the screen.
        setContentView(R.layout.incall_screen);

        initializeInCall();

        Logger.d(this, "onCreate(): exit");
    }

    @Override
    protected void onResume() {
        Logger.d(this, "onResume()...");
        super.onResume();
    }

    // onPause is guaranteed to be called when the InCallActivity goes
    // in the background.
    @Override
    protected void onPause() {
        Logger.d(this, "onPause()...");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Logger.d(this, "onStop()...");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Logger.d(this, "onDestroy()...  this = " + this);
        super.onDestroy();
    }

    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallActivity, since we don't want to get destroyed and then
     * have to be re-created from scratch for the next call.  Instead, we just move ourselves to the
     * back of the activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK button (since moveTaskToBack()
     * puts us behind the Home app, but the home app doesn't allow the BACK key to move you any
     * farther down in the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means that we'll keep a single
     * InCallActivity instance around for the entire uptime of the device.  This noticeably improves
     * the UI responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        Logger.d(this, "finish()...");
        tearDownPresenters();

        super.finish();

        // TODO(klp): Actually finish the activity for now.  Revisit performance implications of
        // this before launch.
        // moveTaskToBack(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Logger.d(this, "onNewIntent: intent = " + intent);

        // We're being re-launched with a new Intent.  Since it's possible for a
        // single InCallActivity instance to persist indefinitely (even if we
        // finish() ourselves), this sequence can potentially happen any time
        // the InCallActivity needs to be displayed.

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle the intent.
        //TODO(klp): handle intent
    }

    @Override
    public void onBackPressed() {
        // TODO(klp): implement

        // Nothing special to do.  Fall back to the default behavior.
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                // TODO(klp): handle call key
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                // Not sure if needed. If so, silence ringer.
                break;

            case KeyEvent.KEYCODE_MUTE:
                toast("mute");
                return true;

            // Various testing/debugging features, enabled ONLY when VERBOSE == true.
            case KeyEvent.KEYCODE_SLASH:
                if (Logger.VERBOSE) {
                    Logger.v(this, "----------- InCallActivity View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    decorView.debug();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                // TODO(klp): Dump phone state?
                break;
        }

        // TODO(klp): handle dialer key down

        return super.onKeyDown(keyCode, event);
    }

    private void initializeInCall() {
        // TODO(klp): Make sure that this doesn't need to move back to onResume() since they are
        // statically added fragments.
        if (mCallButtonFragment == null) {
            mCallButtonFragment = (CallButtonFragment) getFragmentManager()
                    .findFragmentById(R.id.callButtonFragment);
        }

        if (mCallCardFragment == null) {
            mCallCardFragment = (CallCardFragment) getFragmentManager()
                    .findFragmentById(R.id.callCardFragment);
        }

        if (mAnswerFragment == null) {
            mAnswerFragment = (AnswerFragment) getFragmentManager()
                    .findFragmentById(R.id.answerFragment);
        }

        setUpPresenters();
    }

    private void setUpPresenters() {
        InCallPresenter mainPresenter = InCallPresenter.getInstance();

        mainPresenter.addListener(mCallButtonFragment.getPresenter());
        mainPresenter.addListener(mCallCardFragment.getPresenter());
        mainPresenter.addListener(mAnswerFragment.getPresenter());

        // setting activity should be last thing in setup process
        mainPresenter.setActivity(this);
    }

    private void tearDownPresenters() {
        InCallPresenter mainPresenter = InCallPresenter.getInstance();

        mainPresenter.removeListener(mCallButtonFragment.getPresenter());
        mainPresenter.removeListener(mCallCardFragment.getPresenter());
        mainPresenter.removeListener(mAnswerFragment.getPresenter());
    }

    private void toast(String text) {
        final Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);

        toast.show();
    }
}