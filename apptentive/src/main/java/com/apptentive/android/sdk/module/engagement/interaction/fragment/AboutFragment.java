/*
 * Copyright (c) 2016, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */
package com.apptentive.android.sdk.module.engagement.interaction.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.apptentive.android.sdk.R;
import com.apptentive.android.sdk.model.ExtendedData;
import com.apptentive.android.sdk.module.engagement.EngagementModule;
import com.apptentive.android.sdk.module.engagement.interaction.model.Interaction;
import com.apptentive.android.sdk.util.Constants;
import com.apptentive.android.sdk.util.Util;


public class AboutFragment extends ApptentiveBaseFragment<Interaction> {

	private static final String INTERACTION_NAME = "About";

	private static final String EVENT_NAME_CLOSE = "close";
	private static final String EVENT_NAME_CANCEL = "cancel";

	private View root;
	private boolean showBrandingBand;

	public static AboutFragment newInstance(Bundle bundle) {
		AboutFragment fragment = new AboutFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getArguments();

		if (bundle != null) {
			showBrandingBand = bundle.getBoolean(Constants.FragmentConfigKeys.EXTRA);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getActivity().getTheme().applyStyle(R.style.ApptentiveThemeAbout, true);
		root = inflater.inflate(R.layout.apptentive_about, container, false);

		final String packageName = getActivity().getPackageName();

		if (!showBrandingBand) {
			root.findViewById(R.id.apptentive_branding_view).setVisibility(View.GONE);
		}

		View close = root.findViewById(R.id.close_about);
		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				EngagementModule.engage(getActivity(), "com.apptentive", INTERACTION_NAME, null, EVENT_NAME_CLOSE, null, null, (ExtendedData[]) null);
				transit();
			}
		});

		Button information = (Button) root.findViewById(R.id.about_description_link);
		information.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("http://www.apptentive.com/?source=%s", packageName)));
				if (Util.canLaunchIntent(getActivity(), browserIntent)) {
					getActivity().startActivity(browserIntent);
				}
			}
		});

		Button privacy = (Button) root.findViewById(R.id.privacy_link);
		privacy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("http://www.apptentive.com/privacy/?source=%s", packageName)));
				if (Util.canLaunchIntent(getActivity(), browserIntent)) {
					getActivity().startActivity(browserIntent);
				}
			}
		});

		return root;
	}

	public boolean onBackPressed(boolean hardwareButton) {
		if (hardwareButton) {
			EngagementModule.engage(getActivity(), "com.apptentive", INTERACTION_NAME, null, EVENT_NAME_CANCEL, null, null, (ExtendedData[]) null);
		} else {
			EngagementModule.engage(getActivity(), "com.apptentive", INTERACTION_NAME, null, EVENT_NAME_CLOSE, null, null, (ExtendedData[]) null);
		}
		return false;
	}


	@Override
	protected void sendLaunchEvent(Activity activity) {
		EngagementModule.engage(getActivity(), "com.apptentive", INTERACTION_NAME, null, EVENT_NAME_LAUNCH, null, null, (ExtendedData[]) null);
	}
}