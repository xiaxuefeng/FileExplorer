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

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class FileExplorerTabActivity extends Activity implements
		OnClickListener {
	private static final String INSTANCESTATE_TAB = "tab";
	private static final int DEFAULT_OFFSCREEN_PAGES = 2;
	LinearLayout mActionBarView;
	TextView category, list, control;
	ViewPager mViewPager;
	// TabsAdapter mTabsAdapter;
	FragAdapter mFragAdapter;
	ActionMode mActionMode;
	List<Fragment> fragments = new ArrayList<Fragment>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(R.style.AppBaseTheme);
		setContentView(R.layout.fragment_pager);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(DEFAULT_OFFSCREEN_PAGES);

		/* PRIZE-Support ImmersiveMode-liguizeng-2015-4-9-start */
		getWindow().setStatusBarColor(Color.parseColor("#394652"));
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

		// int visibility = getWindow().getDecorView().getVisibility();
		// getWindow().getDecorView().setSystemUiVisibility(visibility^View.SYSTEM_UI_FLAG_LAYOUT_STABLE
		// | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		// | View.SYSTEM_UI_FLAG_FULLSCREEN
		// | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		// getActionBar().show();
		/* PRIZE-Support ImmersiveMode-liguizeng-2015-4-9-end */

		final ActionBar bar = getActionBar();
		// bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE
				| ActionBar.DISPLAY_SHOW_HOME);
		bar.setCustomView(R.layout.fileexplorer_actionbar);
		bar.setDisplayShowCustomEnabled(true);
		mActionBarView = (LinearLayout) bar.getCustomView();
		category = (TextView) mActionBarView.findViewById(R.id.title_category);
		category.setOnClickListener(this);
		list = (TextView) mActionBarView.findViewById(R.id.title_listview);
		list.setOnClickListener(this);
		control = (TextView) mActionBarView.findViewById(R.id.title_control);
		control.setOnClickListener(this);
		Intent service = new Intent(this, FileExplorerService.class);
		getApplicationContext().bindService(service, sc,
				Service.BIND_AUTO_CREATE);
		// mTabsAdapter = new TabsAdapter(this, mViewPager);
		// Tab category, sd, remote;
		// category = bar.newTab().setText(R.string.tab_category);
		// sd = bar.newTab().setText(R.string.tab_sd);
		// remote = bar.newTab().setText(R.string.tab_remote);
		mFragAdapter = new FragAdapter(getFragmentManager(), fragments);
		fragments.add(new FileCategoryActivity());
		fragments.add(new FileViewActivity());
		fragments.add(new ServerControlActivity());
		mViewPager.setAdapter(mFragAdapter);
		mViewPager.setOnPageChangeListener(mFragAdapter);
		mViewPager.setCurrentItem(0);
		mFragAdapter.onPageSelected(0);
		// mTabsAdapter.addTab(category, FileCategoryActivity.class, null);
		// mTabsAdapter.addTab(sd, FileViewActivity.class, null);
		// mTabsAdapter.addTab(remote, ServerControlActivity.class, null);
		// bar.setSelectedNavigationItem(PreferenceManager.getDefaultSharedPreferences(this)
		// .getInt(INSTANCESTATE_TAB, Util.CATEGORY_TAB_INDEX));
		// bar.setSelectedNavigationItem(0);
		// mViewPager.setAdapter(new TabsAdapter(getFragmentManager()));
		// tabs.setViewPager(mViewPager);
		// setTabsValue();
	}

	private FileExplorerService serviceBinder;

	private ServiceConnection sc = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceBinder = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceBinder = ((FileExplorerService.MyBinder) service)
					.getService();
			FileOperationHelper.setService(serviceBinder);
			setService(serviceBinder);
		}
	};

	public FileExplorerService getService() {
		return serviceBinder;
	}

	protected void setService(FileExplorerService service) {

	}

	/*
	 * private void setTabsValue() {
	 * 
	 * // TODO Auto-generated method stub
	 * mTabsAdapter.setSelectedTextColor(Color.parseColor("#45c01a")); }
	 */
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		/*
		 * SharedPreferences.Editor editor = PreferenceManager
		 * .getDefaultSharedPreferences(this).edit();
		 * editor.putInt(INSTANCESTATE_TAB, getActionBar()
		 * .getSelectedNavigationIndex()); editor.commit();
		 */
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mViewPager.getCurrentItem() == Util.CATEGORY_TAB_INDEX) {
			FileCategoryActivity categoryFragement = (FileCategoryActivity) mFragAdapter
					.getItem(Util.CATEGORY_TAB_INDEX);
			if (categoryFragement.isHomePage()) {
				reInstantiateCategoryTab();
			} else {
				categoryFragement.setConfigurationChanged(true);
			}
		}
		super.onConfigurationChanged(newConfig);
	}

	public void reInstantiateCategoryTab() {
		mFragAdapter.destroyItem(mViewPager, Util.CATEGORY_TAB_INDEX,
				mFragAdapter.getItem(Util.CATEGORY_TAB_INDEX));
		mFragAdapter.instantiateItem(mViewPager, Util.CATEGORY_TAB_INDEX);
	}

	@Override
	public void onBackPressed() {
		IBackPressedListener backPressedListener = (IBackPressedListener) mFragAdapter
				.getItem(mViewPager.getCurrentItem());
		if (!backPressedListener.onBack()) {
			super.onBackPressed();
		}
	}

	public interface IBackPressedListener {
		/**
		 * 处理back事件。
		 * 
		 * @return True: 表示已经处理; False: 没有处理，让基类处理。
		 */
		boolean onBack();
	}

	public void setActionMode(ActionMode actionMode) {
		mActionMode = actionMode;
	}

	public ActionMode getActionMode() {
		return mActionMode;
	}

	public Fragment getFragment(int tabIndex) {
		return mFragAdapter.getItem(tabIndex);
	}

	/**
	 * This is a helper class that implements the management of tabs and all
	 * details of connecting a ViewPager with associated TabHost. It relies on a
	 * trick. Normally a tab host has a simple API for supplying a View or
	 * Intent that each tab will show. This is not sufficient for switching
	 * between pages. So instead we make the content part of the tab host 0dp
	 * high (it is not shown) and the TabsAdapter supplies its own dummy view to
	 * show as the tab content. It listens to changes in tabs, and takes care of
	 * switch to the correct paged in the ViewPager whenever the selected tab
	 * changes.
	 */
/*	public static class TabsAdapter extends FragmentPagerAdapter implements
			ActionBar.TabListener, ViewPager.OnPageChangeListener {
		private final Context mContext;
		private final ActionBar mActionBar;
		private final ViewPager mViewPager;
		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

		static final class TabInfo {
			private final Class<?> clss;
			private final Bundle args;
			private Fragment fragment;

			TabInfo(Class<?> _class, Bundle _args) {
				clss = _class;
				args = _args;
			}
		}

		public TabsAdapter(Activity activity, ViewPager pager) {
			super(activity.getFragmentManager());
			mContext = activity;
			mActionBar = activity.getActionBar();
			mViewPager = pager;
			mViewPager.setAdapter(this);
			mViewPager.setOnPageChangeListener(this);
		}

		public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
			TabInfo info = new TabInfo(clss, args);
			tab.setTag(info);
			tab.setTabListener(this);
			mTabs.add(info);
			mActionBar.addTab(tab);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mTabs.size();
		}

		@Override
		public Fragment getItem(int position) {
			TabInfo info = mTabs.get(position);
			if (info.fragment == null) {
				info.fragment = Fragment.instantiate(mContext,
						info.clss.getName(), info.args);
			}
			return info.fragment;
		}

		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			mActionBar.setSelectedNavigationItem(position);
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {

			Object tag = tab.getTag();
			for (int i = 0; i < mTabs.size(); i++) {
				if (mTabs.get(i) == tag) {
					mViewPager.setCurrentItem(i);
				}
			}

			if (!tab.getText().equals(mContext.getString(R.string.tab_sd))) {
				ActionMode actionMode = ((FileExplorerTabActivity) mContext)
						.getActionMode();
				if (actionMode != null) {
					actionMode.finish();
				}
			}

		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

	}*/

	public class FragAdapter extends FragmentPagerAdapter implements 
						ViewPager.OnPageChangeListener {

		private List<Fragment> fragments;

		public FragAdapter(FragmentManager fm) {
			super(fm);
		}

		public FragAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}
		
		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
		}

		@Override
		public void onPageSelected(int position) {
			
			switch (position) {
			case 0:
				category.setTextColor(Color.WHITE);
				list.setTextColor(Color.GRAY);
				control.setTextColor(Color.GRAY);
				break;
			case 1:
				category.setTextColor(Color.GRAY);
				list.setTextColor(Color.WHITE);
				control.setTextColor(Color.GRAY);
				break;
			case 2:
				category.setTextColor(Color.GRAY);
				list.setTextColor(Color.GRAY);
				control.setTextColor(Color.WHITE);
				break;
			default:
				break;
			}
		}

		@Override
		public void onPageScrollStateChanged(int state) {

		}
	}

	@Override
	protected void onDestroy() {
		getApplicationContext().unbindService(sc);
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_category:
			mViewPager.setCurrentItem(0);
			break;
		case R.id.title_listview:
			mViewPager.setCurrentItem(1);
			ActionMode actionMode = getActionMode();
			if (actionMode != null) {
				actionMode.finish();
			}
			break;
		case R.id.title_control:
			mViewPager.setCurrentItem(2);
			break;
		default:
			break;
		}
	}
	public void setCurrentPage(int index) {
		if (mViewPager != null) {
			mViewPager.setCurrentItem(index);
		}
	}

	public int getCurrentPage() {
		if (mViewPager != null) {
			mViewPager.getCurrentItem();
		}
		return 0;
	}
}
