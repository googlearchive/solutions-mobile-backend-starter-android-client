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
package com.google.cloud.backend.android;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import java.util.concurrent.CountDownLatch;

/**
 * This class manages Google Cloud Messaging push notifications and CloudQuery
 * subscriptions.
 *
 */
public class GCMIntentService extends GCMBaseIntentService {

  private final static String GCM_KEY_SUBID = "subId";

  private final static String GCM_TYPEID_QUERY = "query";

  private final static CountDownLatch latch = new CountDownLatch(1);

  protected static String regId;

  /**
   * Register the device for GCM.
   *
   * @param app
   *          {@link Application} that will be used for registering on GCM.
   */
  public static void registerIfNeeded(Application app) {

    // check if already registered
    if (GCMRegistrar.isRegistered(app)) {
      Log.i(Consts.TAG, "register: already registered: " + app);
      return;
    }

    // validate project number
    boolean hasValidProjectNumber = Consts.PROJECT_NUMBER.matches("\\d+");
    if (!hasValidProjectNumber) {
      Log.i(Consts.TAG, "register: invalid project number: " + Consts.PROJECT_NUMBER);
      return;
    }

    // register the app
    GCMRegistrar.checkDevice(app);
    GCMRegistrar.checkManifest(app);
    GCMRegistrar.register(app, Consts.PROJECT_NUMBER);
    Log.i(Consts.TAG, "register: registering GCM for " + app + "...");
  }

  /**
   * Returns registration id associated with the specified {@link Application}.
   * This method will block the thread until regId will be available.
   *
   * @param app
   *          {@link Application}
   * @return registration id
   */
  public static String getRegistrationId(Application app) {

    // try to get regId
    String regId = GCMRegistrar.getRegistrationId(app);
    if (regId != null && regId.trim().length() > 0) {
      return regId;
    }

    // wait for regId from onRegistered
    try {
      latch.await();
    } catch (InterruptedException e) {
      Log.i(Consts.TAG, "getRegistrationId: ", e);
    }
    regId = GCMRegistrar.getRegistrationId(app);
    if (regId != null && regId.trim().length() > 0) {
      return regId;
    } else {
      throw new IllegalStateException("getRegistrationId: Can't find regId for: " + app);
    }
  }

  public GCMIntentService() {
    super(Consts.PROJECT_NUMBER);
  }

  /**
   * Called on registration error. This is called in the application of a
   * Service - no dialog or UI.
   *
   * @param application
   *          the Context
   * @param errorId
   *          an error message
   */
  @Override
  public void onError(Context context, String errorId) {
  }

  /**
   * Handles GCM and dispatch the message based on subscription ID. SubId
   * consists of 1) RegisterID, 2) typeId and 3) payload with delimiter ":".
   */
  @Override
  public void onMessage(Context context, Intent intent) {

    // decode subId in the message
    String subId = intent.getStringExtra(GCM_KEY_SUBID);
    Log.i(Consts.TAG, "onMessage: subId: " + subId);
    String[] tokens = subId.split(":");
    String typeId = tokens[1];

    // dispatch message
    if (GCM_TYPEID_QUERY.equals(typeId)) {
      CloudBackendAsync.handleQueryMessage(tokens[2]);
    }
  }

  /**
   * Called when a registration token has been received.
   *
   * @param application
   *          the Context
   */
  @Override
  public void onRegistered(Context context, String regId) {
    // notify waiters
    if (latch != null) {
      latch.countDown();
      Log.i(Consts.TAG, "onRegistered: for application: " + context + ", regId: " + regId);
    }
  }

  /**
   * Called when the device has been unregistered.
   *
   * @param application
   *          the Context
   */
  @Override
  protected void onUnregistered(Context context, String registrationId) {
  }
}
