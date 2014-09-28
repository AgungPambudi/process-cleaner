package com.drocode.android.processcleaner.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.drocode.android.processcleaner.R;

public class Detail extends Activity implements View.OnClickListener {

	public static final String PACKAGE_NAME = "PACKAGE_NAME";

	private ImageView mIconImage, mShowMoreIcon;
	private TextView mTitleText, mDetailText, mDescriptionText;
	private Button mUninstallButton, mKillButton;

	private LinearLayout mDangerousList, mShowMore, mNormalList;
	private TextView mPermissionHeader, mShowMoreText;

	private String mPackageName;
	public boolean mExpanded;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.detail);

		mIconImage = (ImageView) findViewById(R.id.icon);
		mTitleText = (TextView) findViewById(R.id.title);
		mDetailText = (TextView) findViewById(R.id.version);
		mDescriptionText = (TextView) findViewById(R.id.description);

		mUninstallButton = (Button) findViewById(R.id.uninstall);
		mUninstallButton.setOnClickListener(this);

		mKillButton = (Button) findViewById(R.id.kill);
		mKillButton.setOnClickListener(this);

		mPermissionHeader = (TextView) findViewById(R.id.permission_header);
		mDangerousList = (LinearLayout) findViewById(R.id.dangerous_list);

		mShowMore = (LinearLayout) findViewById(R.id.show_more);
		mShowMoreIcon = (ImageView) findViewById(R.id.show_more_icon);
		mShowMoreText = (TextView) findViewById(R.id.show_more_text);

		mNormalList = (LinearLayout) findViewById(R.id.normal_list);

		mShowMore.setClickable(true);
		mShowMore.setOnClickListener(this);
		mShowMore.setFocusable(true);
		mShowMore
				.setBackgroundResource(android.R.drawable.list_selector_background);
		mExpanded = false;

		updateExpandable();

		if (savedInstanceState != null) {
			mPackageName = savedInstanceState.getString(PACKAGE_NAME);
		}
		if (mPackageName == null) {
			Bundle extras = getIntent().getExtras();
			mPackageName = (extras != null) ? extras.getString(PACKAGE_NAME)
					: null;
		}

		fillViews();
	}

	private void fillViews() {
		PackageManager packageManager = getPackageManager();
		try {
			ApplicationInfo applicationInfo = packageManager
					.getApplicationInfo(mPackageName, 0);

			if (applicationInfo != null) {
				mIconImage.setImageDrawable(packageManager
						.getApplicationIcon(applicationInfo));
				mTitleText.setText(packageManager.getApplicationLabel(
						applicationInfo).toString());

				CharSequence description = applicationInfo
						.loadDescription(packageManager);

				if (description != null) {
					mDescriptionText.setVisibility(View.VISIBLE);
					mDescriptionText.setText(description);
				} else {
					mDescriptionText.setVisibility(View.GONE);
				}

				mUninstallButton.setEnabled(!isSysPackage(applicationInfo));
			}
		} catch (NameNotFoundException e) {
			mDescriptionText.setVisibility(View.GONE);
			mUninstallButton.setVisibility(View.GONE);
		}

		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(
					mPackageName, PackageManager.GET_PERMISSIONS);
			if (packageInfo != null) {
				if (packageInfo.versionName != null) {
					mDetailText.setText("version " + packageInfo.versionName);
				} else if (packageInfo.versionCode > 0) {
					mDetailText.setText("version " + packageInfo.versionCode);
				} else {
					mDetailText.setText("");
				}

				if (packageInfo.requestedPermissions != null) {
					Map<String, ArrayList<String>> dangerousPermissions = new TreeMap<String, ArrayList<String>>();
					Map<String, ArrayList<String>> normalPermissions = new TreeMap<String, ArrayList<String>>();

					for (String permission : packageInfo.requestedPermissions) {
						try {
							PermissionInfo permissionInfo = packageManager
									.getPermissionInfo(permission, 0);
							PermissionGroupInfo permissionGroupInfo = packageManager
									.getPermissionGroupInfo(
											permissionInfo.group, 0);

							String groupName = permissionGroupInfo.loadLabel(
									packageManager).toString();
							String permName = permissionInfo.loadLabel(
									packageManager).toString();

							if (permissionInfo.protectionLevel == PermissionInfo.PROTECTION_NORMAL) {
								addPermission(normalPermissions, groupName,
										permName);
							} else {
								addPermission(dangerousPermissions, groupName,
										permName);
							}
						} catch (NameNotFoundException e) {
							// Blank
						}
					}

					viewPermissions(mDangerousList,
							R.drawable.bullet_key_permission,
							dangerousPermissions);
					viewPermissions(mNormalList, R.drawable.text_dot,
							normalPermissions);

					mPermissionHeader.setVisibility(View.VISIBLE);
					mDangerousList.setVisibility(View.VISIBLE);
					mShowMore
							.setVisibility((normalPermissions.isEmpty()) ? View.GONE
									: View.VISIBLE);
				} else {
					mPermissionHeader.setVisibility(View.GONE);
					mDangerousList.setVisibility(View.GONE);
					mShowMore.setVisibility(View.GONE);
				}
			}
		} catch (NameNotFoundException e) {
			// Blank
		}

	}

	private void viewPermissions(ViewGroup permissionView, int icon,
			Map<String, ArrayList<String>> permissions) {
		for (String key : permissions.keySet()) {
			View view = getLayoutInflater().inflate(R.layout.permission_item,
					null);
			((ImageView) view.findViewById(R.id.permission_icon))
					.setImageDrawable(getResources().getDrawable(icon));

			((TextView) view.findViewById(R.id.permission_group)).setText(key);
			((TextView) view.findViewById(R.id.permission_list))
					.setText(addCommas(permissions.get(key)));

			permissionView.addView(view);
		}
	}

	private String addCommas(List<String> items) {
		String result = "";
		Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
		if (items.size() > 0) {
			result += items.get(0);
		}

		for (int i = 1; i < items.size(); i++) {
			result += (", " + items.get(i));
		}

		return result;
	}

	private void updateExpandable() {
		if (mExpanded) {
			mShowMoreIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.expander_maximized));
			mShowMoreText.setText(R.string.detail_perms_hide);
			mNormalList.setVisibility(View.VISIBLE);
		} else {
			mShowMoreIcon.setImageDrawable(getResources().getDrawable(
					R.drawable.expander_minimized));
			mShowMoreText.setText(R.string.detail_perms_show);
			mNormalList.setVisibility(View.GONE);
		}
	}

	private void addPermission(Map<String, ArrayList<String>> permissions,
			String groupName, String permName) {
		if (!permissions.containsKey(groupName)) {
			permissions.put(groupName, new ArrayList<String>());
		}
		permissions.get(groupName).add(permName);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PACKAGE_NAME, mPackageName);
	}

	private boolean isSysPackage(ApplicationInfo applicationInfo) {
		return ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
	}

	@Override
	public void onClick(View v) {
		if (v == mUninstallButton) {
			Uri packageURI = Uri.parse("package:" + mPackageName);
			Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
			startActivity(intent);
			finish();
		}
		if (v == mKillButton) {
			ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			activityManager.restartPackage(mPackageName);
			finish();
		}
		if (v == mShowMore) {
			mExpanded = !mExpanded;
			updateExpandable();
		}
	}

}
