/*
 * Copyright (c) 2015, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk.module.messagecenter.model;

import android.text.TextUtils;

import com.apptentive.android.sdk.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Sky Kelsey
 */
public class MessageFactory {
	public static ApptentiveMessage fromJson(String json) {
		try {
			ApptentiveMessage.Type type = ApptentiveMessage.Type.CompoundMessage;
			JSONObject root = new JSONObject(json);
			if (!root.isNull(ApptentiveMessage.KEY_TYPE)) {
				String typeStr = root.getString(ApptentiveMessage.KEY_TYPE);
				if (!TextUtils.isEmpty(typeStr)) {
					type = ApptentiveMessage.Type.valueOf(typeStr);
				}
			}
			switch (type) {
				case CompoundMessage:
					return new CompoundMessage(json);
				case unknown:
					break;
				default:
					break;
			}
		} catch (JSONException e) {
			Log.v("Error parsing json as Message: %s", e, json);
		} catch (IllegalArgumentException e) {
			// Unknown unknown #rumsfeld
		}
		return null;
	}
}