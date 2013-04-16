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
package com.google.cloud.backend.android.sample.socialtalk;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.cloud.backend.android.CloudBackendActivity;
import com.google.cloud.backend.android.CloudCallbackHandler;
import com.google.cloud.backend.android.CloudEntity;
import com.google.cloud.backend.android.R;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Sample app for CloudMessage API of Mobile Backend Starter. It subscribes to
 * any posts that includes specified keywords.
 * 
 */
public class SocialTalkActivity extends CloudBackendActivity {

  // data formatter for formatting createdAt property
  private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss ", Locale.US);

  // UI components
  private TextView tvPosts;
  private EditText etMessage;

  // a list of posts on the UI
  List<CloudEntity> posts = new LinkedList<CloudEntity>();

  // initialize UI
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    tvPosts = (TextView) findViewById(R.id.tvPosts);
    etMessage = (EditText) findViewById(R.id.etMessage);
  }

  @Override
  protected void onPostCreate() {
    super.onPostCreate();

    // create handler for Cloud Messages
    CloudCallbackHandler<List<CloudEntity>> handler = new CloudCallbackHandler<List<CloudEntity>>() {
      @Override
      public void onComplete(List<CloudEntity> messages) {
        for (CloudEntity ce : messages) {
          posts.add(0, ce);
        }
        updateTimelineUI();
      }
    };

    // receive all posts that includes "#dog" or "#cat" hashtags
    getCloudBackend().subscribeToCloudMessage("#dog", handler, 50);
    getCloudBackend().subscribeToCloudMessage("#cat", handler, 50);
  }

  // convert posts into string and update UI
  private void updateTimelineUI() {
    final StringBuilder sb = new StringBuilder();
    for (CloudEntity post : posts) {
      sb.append(sdf.format(post.getCreatedAt()) + getCreatorName(post) + ": " + post.get("message")
          + "\n");
    }
    tvPosts.setText(sb.toString());
  }

  // removing the domain name part from email address
  private String getCreatorName(CloudEntity b) {
    if (b.getCreatedBy() != null) {
      return " " + b.getCreatedBy().replaceFirst("@.*", "");
    } else {
      return "<anonymous>";
    }
  }

  // post a new message to all subscribers of each hashtags
  public void onSendButtonPressed(View view) {
    String msg = etMessage.getText().toString();
    for (String token : msg.split(" ")) {
      if (token.startsWith("#")) {
        CloudEntity cm = getCloudBackend().createCloudMessage(token);
        cm.put("message", msg);
        getCloudBackend().sendCloudMessage(cm);
      }
    }
    etMessage.setText("");
    updateTimelineUI();
  }
}