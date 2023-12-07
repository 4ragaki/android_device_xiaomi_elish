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
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ConcatAdapter;

import com.android.settingslib.applications.ApplicationsState;

import org.lineageos.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForceStopSettingsFragment extends PreferenceFragment
        implements ApplicationsState.Callbacks {

    private ConcatAdapter mRVAdapter;
    private UserPackagesAdapter mUserPackagesAdapter;
    private ExtraComponentsAdapter mExtraAdapter;
    private PackageManager mPm;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppFilter mAppFilter;
    private List<String> mUserApps = new ArrayList<>();

    private ComponentName mExtraComponents[] = new ComponentName[] {
        new ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.chimera.GmsIntentOperationService"
        )
    };

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

        mPm = getActivity().getPackageManager();
        mAppFilter = new AppFilter(mPm);

        mUserPackagesAdapter = new UserPackagesAdapter();
        mExtraAdapter = new ExtraComponentsAdapter();
        mRVAdapter = new ConcatAdapter(mUserPackagesAdapter, mExtraAdapter);

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
        mAppsRecyclerView.setAdapter(mRVAdapter);
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
        mAppFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        if (entries != null) {
            handleAppEntries(entries);
            mUserPackagesAdapter.notifyDataSetChanged();
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
        String lastSectionIndex = null;
        int offset = 0;

        for (int i = 0; i < entries.size(); i++) {
            final ApplicationInfo info = entries.get(i).info;
            final String label = (String) info.loadLabel(mPm);
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

        List<ApplicationsState.AppEntry> userEntries =
                entries.stream().filter(appEntry -> mUserApps.stream()
                        .anyMatch(p -> p.equals(appEntry.info.packageName)))
                        .collect(Collectors.toList());
        mUserPackagesAdapter.setEntries(userEntries);

        List<ApplicationsState.AppEntry> extraEntries =
                entries.stream().filter(appEntry -> Arrays.stream(mExtraComponents)
                        .anyMatch(p -> p.getPackageName().equals(appEntry.info.packageName)))
                        .collect(Collectors.toList());
        mExtraAdapter.setEntries(extraEntries);
    }

    private void rebuild() {
        mSession.rebuild(mAppFilter, ApplicationsState.ALPHA_COMPARATOR);
    }

    private class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView title;

        private CategoryViewHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.category_name);

            view.setTag(this);
        }
    }

    private class AppViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView status;
        private ImageView icon;
        private View rootView;
        private Switch preference;

        private AppViewHolder(View view) {
            super(view);
            this.title = view.findViewById(R.id.app_name);
            this.status = view.findViewById(R.id.app_status);
            this.icon = view.findViewById(R.id.app_icon);
            this.preference = view.findViewById(R.id.preference);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private class UserPackagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements View.OnClickListener {

        private List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();

        @Override
        public int getItemCount() {
            return mEntries.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                CategoryViewHolder holder = new CategoryViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.forcestop_category_item, parent, false));
                return holder;
            }
            AppViewHolder holder = new AppViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.forcestop_list_item, parent, false));
            holder.preference.setOnClickListener(this);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            if (holder instanceof CategoryViewHolder) {
                ((CategoryViewHolder) holder).title.setText(R.string.forcestop_category_stop);
                return;
            }

            ApplicationsState.AppEntry entry = mEntries.get(position - 1);

            if (entry == null) {
                return;
            }

            AppViewHolder appViewHolder = (AppViewHolder) holder;
            appViewHolder.title.setText(entry.label);
            appViewHolder.rootView.setOnClickListener(v -> appViewHolder.preference.performClick());
            mApplicationsState.ensureIcon(entry);
            appViewHolder.icon.setImageDrawable(entry.icon);

            boolean packageState = mThermalUtils.getForceStopStateForPackage(entry.info.packageName);
            appViewHolder.status.setText(packageState ? R.string.forcestop_enabled : R.string.forcestop_disabled);
            appViewHolder.preference.setTag(entry);
            appViewHolder.preference.setChecked(packageState);
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            mEntries = entries;
            notifyDataSetChanged();
        }

        @Override
        public void onClick(View pref) {
            final ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) pref.getTag();
            mThermalUtils.writeForceStopPackage(entry.info.packageName, ((Switch) pref).isChecked());
            notifyDataSetChanged();
        }
    }

    private class ExtraComponentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements View.OnClickListener {

        private List<ApplicationsState.AppEntry> mEntries = new ArrayList<>();

        @Override
        public int getItemCount() {
            return mExtraComponents.length + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                CategoryViewHolder holder = new CategoryViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.forcestop_category_item, parent, false));
                return holder;
            }
            AppViewHolder holder = new AppViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.forcestop_list_item, parent, false));
            holder.preference.setOnClickListener(this);
            return holder;
        }



        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof  CategoryViewHolder) {
                ((CategoryViewHolder) holder).title.setText(R.string.forcestop_category_freeze);
                return;
            }

            ComponentName comp = mExtraComponents[position - 1];

            if (comp == null) {
                return;
            }

            AppViewHolder appViewHolder = (AppViewHolder) holder;
            appViewHolder.title.setText(comp.flattenToShortString());
            appViewHolder.rootView.setOnClickListener(v -> appViewHolder.preference.performClick());

            Optional<ApplicationsState.AppEntry> entry = mEntries.stream()
                    .filter(p -> p.info.packageName.equals(comp.getPackageName()))
                    .findAny();
            if (entry.isPresent()) {
                ApplicationsState.AppEntry appEntry = entry.get();
                mApplicationsState.ensureIcon(appEntry);
                appViewHolder.icon.setImageDrawable(appEntry.icon);
            }

            boolean disabled = mPm.getComponentEnabledSetting(comp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            appViewHolder.status.setText(disabled ? R.string.forcestop_frozen : R.string.forcestop_freeze_disabled);
            appViewHolder.preference.setTag(comp);
            appViewHolder.preference.setChecked(disabled);
        }

        @Override
        public void onClick(View pref) {
            final ComponentName component = (ComponentName) pref.getTag();
            mPm.setComponentEnabledSetting(
                component, ((Switch)pref).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                : PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.SYNCHRONOUS
            );
            notifyDataSetChanged();
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            mEntries = entries;
            notifyDataSetChanged();
        }
    }


    private class AppFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> mPackageRequired = new ArrayList<String>();

        private AppFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            List<PackageInfo> packages = mPackageManager.getInstalledPackages(0);

            synchronized (mPackageRequired) {
                mPackageRequired.clear();
                mUserApps.clear();

                packages.forEach( p -> {
                    boolean c = Arrays.stream(mExtraComponents).anyMatch(cn ->
                            cn.getPackageName().equals(p.packageName)
                    );

                    if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0){
                        mPackageRequired.add(p.packageName);
                        mUserApps.add(p.packageName);
                    } else if (c) mPackageRequired.add(p.packageName);
                });
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry entry) {
            boolean show = !mUserPackagesAdapter.mEntries.contains(entry.info.packageName);
            if (show) {
                synchronized (mPackageRequired) {
                    show = mPackageRequired.contains(entry.info.packageName);
                }
            }
            return show;
        }
    }
}
