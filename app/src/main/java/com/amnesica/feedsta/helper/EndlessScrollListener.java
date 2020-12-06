package com.amnesica.feedsta.helper;

import android.widget.AbsListView;
import android.widget.GridView;

/**
 * Helper for scrolling through gridViews
 */
@SuppressWarnings("CanBeFinal")
public class EndlessScrollListener implements AbsListView.OnScrollListener {
    private final GridView gridView;
    private final RefreshList refreshList;
    public boolean hasMorePages;
    private boolean isLoading;
    private int pageNumber = 0;
    private boolean isRefreshing;

    public EndlessScrollListener(GridView gridView, RefreshList refreshList) {
        this.gridView = gridView;
        this.isLoading = false;
        this.hasMorePages = true;
        this.refreshList = refreshList;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (gridView.getLastVisiblePosition() + 1 == totalItemCount && !isLoading) {
            isLoading = true;
            if (hasMorePages && !isRefreshing) {
                isRefreshing = true;
                refreshList.onRefresh(pageNumber);
            }
        } else {
            isLoading = false;
        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void noMorePages() {
        this.hasMorePages = false;
    }

    public void notifyMorePages() {
        isRefreshing = false;
        pageNumber = pageNumber + 1;
    }

    public interface RefreshList {
        void onRefresh(int pageNumber);
    }
}
