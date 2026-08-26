package org.dpdns.nlt.app.xweb;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class BrowserPagerAdapter extends FragmentStateAdapter {

    private final List<WebFragment> fragmentList = new ArrayList<>();
    private final List<Long> itemIdList = new ArrayList<>();

    public BrowserPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addFragment(WebFragment fragment) {
        fragmentList.add(fragment);
        itemIdList.add((long) fragment.hashCode());
        notifyItemInserted(fragmentList.size() - 1);
    }

    public void removeFragment(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            fragmentList.remove(position);
            itemIdList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, fragmentList.size() - position);
        }
    }

    public WebFragment getFragment(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            return fragmentList.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    @Override
    public long getItemId(int position) {
        return itemIdList.get(position);
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemIdList.contains(itemId);
    }
}
