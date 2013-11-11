/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.backend.core;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.cloud.backend.GCMIntentService;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;

/**
 * An {@link Fragment} class that allows hosting Activities to access the
 * features and functionalities of the CloudBackend classes, including CRUD of
 * {@link CloudEntity}, Google account authentication and Google Cloud
 * Messaging.
 */
public class CloudBackendFragment extends Fragment {

    /**
     * onActivityResult code
     */
    private static final int REQUEST_ACCOUNT_PICKER = 2;

    private GoogleAccountCredential mCredential;

    private CloudBackendMessaging mCloudBackend;

    /**
     * The listener to use upon completion of certain functions.
     */
    private OnListener callback;

    /**
     * BroadcastReceiver for incoming messages.
     */
    private BroadcastReceiver mMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String token = intent.getStringExtra("token");
            Log.i(Consts.TAG, "A message has been recieved of token: " + token);
            mCloudBackend.handleQueryMessage(token);
        }
    };

    /**
     * Returns {@link CloudBackendMessaging} instance for this activity.
     *
     * @return {@link CloudBackendMessaging}
     */
    public CloudBackendMessaging getCloudBackend() {
        return mCloudBackend;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callback = (OnListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnListener");
        }
    }

    /**
     * Subclasses may override this to execute initialization of the activity.
     * If it uses any CloudBackend features, it should be executed inside
     * {@link #onCreateFinished()} that will be called after CloudBackend
     * initializations such as user authentication.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init backend
        mCloudBackend = new CloudBackendMessaging(getActivity());

        // create credential
        mCredential = GoogleAccountCredential.usingAudience(getActivity(), Consts.AUTH_AUDIENCE);
        mCloudBackend.setCredential(mCredential);

        signInAndSubscribe(false);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMsgReceiver,
                new IntentFilter(GCMIntentService.BROADCAST_ON_MESSAGE));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMsgReceiver);
        super.onDestroy();
    }

    /**
     * A listener interface for GameActivity to use to process the actions this
     * player makes.
     *
     */
    public interface OnListener {
        public void onCreateFinished();

        public void onBroadcastMessageReceived(List<CloudEntity> message);
    }

    /**
     * Signs in to the application with an account. Subscribes to broadcast messages
     * once the account picking flow has completed, or immediately if there is no
     * authentication required.
     *
     * @param overrideCurrent {@code true} if user can choose an account even if
     *            already signed in, {@code false} if the user can choose an
     *            account only if there is no currently signed in user
     */
    public void signInAndSubscribe(boolean overrideCurrent) {
        if (Consts.IS_AUTH_ENABLED) {
            String accountName =
                    mCloudBackend.getSharedPreferences().getString(
                            Consts.PREF_KEY_ACCOUNT_NAME, null);
            if (accountName == null || overrideCurrent) {
                super.startActivityForResult(mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
                return;
            } else {
                mCredential.setSelectedAccountName(accountName);
                subscribeToBroadcast();
            }
        } else {
            subscribeToBroadcast();
        }
    }

    private void subscribeToBroadcast() {
        // init subscription to broadcast message
        CloudCallbackHandler<List<CloudEntity>> handler =
                new CloudCallbackHandler<List<CloudEntity>>() {
                    @Override
                    public void onComplete(List<CloudEntity> results) {
                        callback.onBroadcastMessageReceived(results);
                    }
                };
        mCloudBackend.subscribeToCloudMessage(
                CloudBackendMessaging.TOPIC_ID_BROADCAST, handler);

        callback.onCreateFinished();
    }

    /**
     * Handles callback from Intents like authorization request or account
     * picking. If the hosting Activity also makes use of onActivityResult, make
     * sure the Activity calls super.onActivityResult to handle all
     * requestCodes, including the code here. Forgetting to call the super
     * method is a common mistake and is often cause for confusion.
     */
    @Override
    public final void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("result from activity", "resultcode: " + resultCode);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {

                    // set the picked account name to the credential
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    mCredential.setSelectedAccountName(accountName);

                    // save account name to shared pref
                    SharedPreferences.Editor e = mCloudBackend.getSharedPreferences().edit();
                    e.putString(Consts.PREF_KEY_ACCOUNT_NAME, accountName);
                    e.commit();
                }

                // post create initialization
                subscribeToBroadcast();
                break;
        }
    }

    /**
     * Returns account name selected by user, or null if any account is
     * selected.
     */
    protected String getAccountName() {
        return mCredential == null ? null : mCredential.getSelectedAccountName();
    }
}
