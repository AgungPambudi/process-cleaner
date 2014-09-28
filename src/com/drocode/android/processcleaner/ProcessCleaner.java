package com.drocode.android.processcleaner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.drocode.android.processcleaner.list.Detail;
import com.drocode.android.processcleaner.view.ViewBinder;

public class ProcessCleaner extends ListActivity implements
		View.OnClickListener {
	private static final String ICON = "icon";
	private static final String TITLE = "title";
	private static final String DESCRIPTION = "desc";

	private static final int KILL_ID = Menu.FIRST + 0;
	private static final int HIDE_ID = Menu.FIRST + 1;
	private static final int DETAILS_ID = Menu.FIRST + 2;

	private static final String HIDDENPACKAGES_FILENAME = "hiddenpackages";

	private List<ActivityManager.RunningTaskInfo> mRunningTasks;
	private List<String> mHiddenPackages;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		((Button) findViewById(R.id.refresh)).setOnClickListener(this);
		((Button) findViewById(R.id.showall)).setOnClickListener(this);

		loadHiddenPackages();
		registerForContextMenu(getListView());
	}

	@Override
	protected void onResume() {
		super.onResume();
		listTasks();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		int index = (int) id;
		killTask(index);
		listTasks();
	}

	private void listTasks() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> runningTasks = activityManager
				.getRunningTasks(128);

		PackageManager packageManager = getPackageManager();

		mRunningTasks = new ArrayList<RunningTaskInfo>();
		ArrayList<Map<String, Object>> listEntries = new ArrayList<Map<String, Object>>();

		for (RunningTaskInfo taskInfo : runningTasks) {
			if (mHiddenPackages
					.contains(taskInfo.baseActivity.getPackageName()))
				continue;

			mRunningTasks.add(taskInfo);

			Map<String, Object> entry = new HashMap<String, Object>();

			try {
				ApplicationInfo applicationInfo = packageManager
						.getApplicationInfo(
								taskInfo.baseActivity.getPackageName(), 0);

				entry.put(ICON,
						packageManager.getApplicationIcon(applicationInfo));
				entry.put(TITLE,
						packageManager.getApplicationLabel(applicationInfo)
								.toString());
			} catch (NameNotFoundException e) {
				entry.put(TITLE, taskInfo.baseActivity.getClassName());
			}

			entry.put(
					DESCRIPTION,
					getString((taskInfo.numRunning > 0) ? R.string.state_running
							: R.string.state_stopped));

			listEntries.add(entry);
		}
		SimpleAdapter adapter = new SimpleAdapter(this, listEntries,
				R.layout.row, new String[] { ICON, TITLE, DESCRIPTION },
				new int[] { R.id.icon, R.id.title, R.id.description });
		adapter.setViewBinder(new ViewBinder());
		setListAdapter(adapter);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		PackageManager packageManager = getPackageManager();
		ActivityManager.RunningTaskInfo taskInfo = mRunningTasks
				.get(info.position);

		try {
			ApplicationInfo applicationInfo = packageManager
					.getApplicationInfo(taskInfo.baseActivity.getPackageName(),
							0);
			menu.setHeaderTitle(packageManager.getApplicationLabel(
					applicationInfo).toString());
		} catch (NameNotFoundException e) {
			// Blank
		}

		menu.add(0, KILL_ID, 0, R.string.menu_kill);
		menu.add(0, HIDE_ID, 0, R.string.menu_hide);
		menu.add(0, DETAILS_ID, 0, R.string.menu_details);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		String packageName = mRunningTasks.get(info.position).baseActivity
				.getPackageName();

		switch (item.getItemId()) {
		case KILL_ID:
			killTask(info.position);
			return true;

		case HIDE_ID:
			mHiddenPackages.add(packageName);
			saveHiddenPackages();
			listTasks();
			return true;

		case DETAILS_ID:
			Intent intent = new Intent(this, Detail.class);
			intent.putExtra(Detail.PACKAGE_NAME, packageName);
			startActivity(intent);
			return true;
		}

		return super.onContextItemSelected(item);
	}

	@SuppressWarnings("unchecked")
	private void loadHiddenPackages() {
		try {
			FileInputStream fis = openFileInput(HIDDENPACKAGES_FILENAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mHiddenPackages = (List<String>) ois.readObject();
			ois.close();
			fis.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}

		if (mHiddenPackages == null)
			mHiddenPackages = new ArrayList<String>();
	}

	private void saveHiddenPackages() {
		try {
			FileOutputStream fos = openFileOutput(HIDDENPACKAGES_FILENAME,
					Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mHiddenPackages);
			oos.close();
			fos.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private void killTask(int index) {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		activityManager.restartPackage(mRunningTasks.get(index).baseActivity
				.getPackageName());
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.refresh:
			listTasks();
			break;

		case R.id.showall:
			mHiddenPackages.clear();
			saveHiddenPackages();
			listTasks();
			break;
		}
	}

}