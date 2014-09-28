package com.drocode.android.processcleaner.view;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

public class ViewBinder implements SimpleAdapter.ViewBinder {

	@Override
	public boolean setViewValue(View view, Object data,
			String textRepresentation) {

		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;
			imageView.setImageDrawable((Drawable) data);
			return true;
		}

		return false;
	}

}
