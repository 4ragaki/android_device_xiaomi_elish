/**
 * Copyright (C) 2020 The LineageOS Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lineageos.settings.thermal;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settingslib.applications.ApplicationsState;

import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForceStopSettingsFragment extends PreferenceFragment
        implements ApplicationsState.Callbacks {

    private UserPackagesAdapter mAllPackagesAdapter;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private ActivityFilter mActivityFilter;
    private Map<String, ApplicationsState.AppEntry> mEntryMap =
            new HashMap<String, ApplicationsState.AppEntry>();

    private RecyclerView mAppsRecyclerView;

    private ThermalUtils mThermalUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.onResume();
        mActivityFilter = new ActivityFilter(getActivity().getPackageManager());

        mAllPackagesAdapter = new UserPackagesAdapter(getActivity());

        mThermalUtils = new ThermalUtils(getActivity());
        mThermalUtils.initialStopSet(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.forcestop_layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAppsRecyclerView = view.findViewById(R.id.thermal_rv_view);
        mAppsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAppsRecyclerView.setAdapter(mAllPackagesAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getResources().getString(R.string.forcestop_title));
        rebuild();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSession.onPause();
        mSession.onDestroy();
    }

    @Override
    public void onPackageListChanged() {
        mActivityFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null) {
            handleAppEntries(entries);
            mAllPackagesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        final ArrayList<String> sections = new ArrayList<String>();
        final ArrayList<Integer> positions = new ArrayList<Integer>();
        final PackageManager pm = getActivity().getPackageManager();
        String lastSectionIndex = null;
        int offset = 0;

        for (int i = 0; i < entries.size(); i++) {
            final ApplicationInfo info = entries.get(i).info;
            final String label = (String) info.loadLabel(pm);
            final String sectionIndex;

            if (!info.enabled) {
                sectionIndex = "--"; // XXX
            } else if (TextUtils.isEmpty(label)) {
                sectionIndex = "";
            } else {
                sectionIndex = label.substring(0, 1).toUpperCase();
            }

            if (lastSectionIndex == null ||
                    !TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }

            offset++;
        }

        mAllPackagesAdapter.setEntries(entries, sections, positions);
        mEntryMap.clear();
        for (ApplicationsState.AppEntry e : entries) {
            mEntryMap.put(e.info.packageName, e);
        }
    }

    private void rebuild() {
        mSession.rebuild(mActivityFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView status;
        private ImageView icon;
        private View rootView;
        private Switch preference;

        private ViewHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.app_name);
            this.status = view.findViewById(R.id.app_status);
            this.icon = view.findViewById(R.id.app_icon);
            this.preference = view.findViewById(R.id.preference);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private class UserPackagesAdapter extends RecyclerView.Adapter<ViewHolder>
            implements View.OnClickListener {

        private List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();
        private String[] mSections;
        private int[] mPositions;

        public UserPackagesAdapter(Context context) {
            mActivityFilter = new ActivityFilter(context.getPackageManager());
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }

        @Override
        public long getItemId(int position) {
            return mEntries.get(position).id;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.forcestop_list_item, parent, false));
            holder.preference.setOnClickListener(this);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ApplicationsState.AppEntry entry = mEntries.get(position);

            if (entry == null) {
                return;
            }

            holder.title.setText(entry.label);
            holder.rootView.setOnClickListener(v -> holder.preference.performClick());
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);

            boolean packageState = mThermalUtils.getForceStopStateForPackage(entry.info.packageName);
            holder.status.setText(packageState ? R.string.forcestop_enabled : R.string.forcestop_disabled);
            holder.preference.setTag(entry);
            holder.preference.setChecked(packageState);
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries,
                                List<String> sections, List<Integer> positions) {
            mEntries = entries;
            mSections = sections.toArray(new String[sections.size()]);
            mPositions = new int[positions.size()];
            for (int i = 0; i < positions.size(); i++) {
                mPositions[i] = positions.get(i);
            }
            notifyDataSetChanged();
        }

        @Override
        public void onClick(View pref) {
            final ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) pref.getTag();
            mThermalUtils.writeForceStopPackage(entry.info.packageName, ((Switch) pref).isChecked());
            notifyDataSetChanged();
        }
    }

    private class ActivityFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> mLauncherResolveInfoList = new ArrayList<String>();

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(i, 0);

            synchronized (mLauncherResolveInfoList) {
                mLauncherResolveInfoList.clear();
                for (ResolveInfo ri : resolveInfoList) {
                    try {
                        PackageInfo info = mPackageManager.getPackageInfo(ri.activityInfo.packageName, PackageManager.PackageInfoFlags.of(0));

                        if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
                            mLauncherResolveInfoList.add(ri.activityInfo.packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry entry) {
            boolean show = !mAllPackagesAdapter.mEntries.contains(entry.info.packageName);
            if (show) {
                synchronized (mLauncherResolveInfoList) {
                    show = mLauncherResolveInfoList.contains(entry.info.packageName);
                }
            }
            return show;
        }
    }
}
