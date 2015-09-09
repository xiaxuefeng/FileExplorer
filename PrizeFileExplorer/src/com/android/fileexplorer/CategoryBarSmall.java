/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.android.fileexplorer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CategoryBarSmall extends View {
	private static final String LOG_TAG = "CategoryBar";

	private static final int MARGIN = 0;

	private static final int ANI_TOTAL_FRAMES = 10;
	private static final int ANI_PERIOD = 100;
	private Timer timer;

	private class Category {
		public long value;

		public long tmpValue; // used for animation

		public long aniStep; // animation step

		public int resImg;
	}

	private ArrayList<Category> categories = new ArrayList<Category>();

	private long mFullValue;

	public void setFullValue(long value) {
		mFullValue = value;
	}

	public CategoryBarSmall(Context context) {
		this(context, null);
	}

	public CategoryBarSmall(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CategoryBarSmall(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void addCategory(int categoryImg) {
		Category ca = new Category();
		ca.resImg = categoryImg;
		categories.add(ca);
	}

	public boolean setCategoryValue(int index, long value) {
		if (index < 0 || index >= categories.size())
			return false;
		categories.get(index).value = value;
		invalidate();
		return true;
	}

	
	
	 /**
	 * 方法描述：set the occupied storage value
	 * @param value
	 * @return true or false
	 * @author hekeyi
	 */
	public boolean setCategoryValue(long value) {

		categories.get(0).value = value;
		invalidate();
		return true;
	}

	
	
	 /**
	 * 方法描述：draw the CategoryBar
	 * @param 参数名 说明
	 * @return 返回类型 说明
	 * @see 类名/完整类名/完整类名#方法名
	 * @author hekeyi
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		int width = getWidth() - MARGIN * 2;
		int height = getHeight();
		boolean isHorizontal = (width > height);
		Drawable d = getDrawable(R.drawable.category_bar_capacity);
		Rect bounds = null;
		if (isHorizontal) {
			bounds = new Rect(0, 0, d.getIntrinsicWidth(), height);
		} else {
			bounds = new Rect(MARGIN, 0, MARGIN + width, d.getIntrinsicHeight());
		}

		if (isHorizontal) {
			bounds.left = 0;
			bounds.right = getWidth();
			bounds.bottom = bounds.top + d.getIntrinsicHeight();
		} else {
			bounds.top = 0;
			bounds.bottom = getHeight() - MARGIN;
			bounds.right = bounds.left + d.getIntrinsicWidth();
		}

		d.setBounds(bounds);
		d.draw(canvas);

		int beginning = MARGIN;
		if (!isHorizontal)
			beginning += height;
		d.setBounds(bounds);
		d.draw(canvas);

		if (mFullValue != 0) {
			for (Category c : categories) {
				long value = (timer == null ? c.value : c.tmpValue);
				if (isHorizontal) {
					int w = (int) (value * width / mFullValue);
					if (w == 0)
						continue;
					bounds.left = beginning;
					bounds.right = beginning + w;
					int oldHeight = bounds.bottom - bounds.top;
					d = getDrawable(c.resImg);
					bounds.bottom = bounds.top + oldHeight;
					d.setBounds(bounds);
					d.draw(canvas);
					beginning += w;
				} else {
					int h = (int) (value * height / mFullValue);
					if (h == 0)
						continue;
					bounds.bottom = beginning;
					bounds.top = beginning - h;
					d = getDrawable(c.resImg);
					bounds.right = bounds.left + d.getIntrinsicWidth();
					d.setBounds(bounds);
					d.draw(canvas);
					beginning -= h;
				}
			}

		}

	}

	private Drawable getDrawable(int id) {
		return getContext().getResources().getDrawable(id);
	}

	private void stepAnimation() {
		if (timer == null)
			return;

		int finished = 0;
		for (Category c : categories) {
			c.tmpValue += c.aniStep;
			if (c.tmpValue >= c.value) {
				c.tmpValue = c.value;
				finished++;
				if (finished >= categories.size()) {
					// stop animation
					timer.cancel();
					timer = null;
					Log.v(LOG_TAG, "Animation stopped");
					break;
				}
			}
		}

		postInvalidate();
	}

	synchronized public void startAnimation() {
		if (timer != null) {
			return;
		}

		Log.v(LOG_TAG, "startAnimation");

		for (Category c : categories) {
			c.tmpValue = 0;
			c.aniStep = c.value / ANI_TOTAL_FRAMES;
		}

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			public void run() {
				stepAnimation();
			}

		}, 0, ANI_PERIOD);
	}

}
