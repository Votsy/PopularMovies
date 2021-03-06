package com.jacquessmuts.popularmovies.Activities;

import android.content.res.Configuration;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jacquessmuts.popularmovies.Adapters.MovieListAdapter;
import com.jacquessmuts.popularmovies.Movie;
import com.jacquessmuts.popularmovies.R;
import com.jacquessmuts.popularmovies.Utils.Server;
import com.jacquessmuts.popularmovies.Utils.Util;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeActivity extends AppCompatActivity implements MovieListAdapter.MovieListOnClickHandler, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.swiperefresh_home) SwipeRefreshLayout swiperefresh_home;
    @BindView(R.id.recyclerview_home) RecyclerView recyclerview_home;
    private GridLayoutManager mLayoutManager;
    private MovieListAdapter mMovieListAdapter;
    private ScrollPagingListener mScrollListener;

    @BindView(R.id.tv_error_message_display) TextView tv_error_message_display;
    @BindView(R.id.pb_loading_indicator) ProgressBar pb_loading_indicator;

    private Server.SortingOption mSortingOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        setupRecyclerView();
        mSortingOption = Server.SortingOption.POPULAR;
        onRefresh();
    }

    @Override
    public void onRefresh() {
        setLoading(true);
        getData(1);
    }

    @Override
    public void onClick(Movie movieObject) {
        startActivity(MovieDetailActivity.getIntent(this, movieObject));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_sort_popular:
                mSortingOption = Server.SortingOption.POPULAR;
                onRefresh();
                return true;
            case R.id.menu_sort_rating:
                mSortingOption = Server.SortingOption.RATING;
                onRefresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupRecyclerView(){
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            mLayoutManager = new GridLayoutManager(this, 3);
        } else{
            mLayoutManager = new GridLayoutManager(this, 5);
        }
        swiperefresh_home.setOnRefreshListener(this);
        recyclerview_home.setLayoutManager(mLayoutManager);
        recyclerview_home.setHasFixedSize(true);

        mMovieListAdapter = new MovieListAdapter(this);
        recyclerview_home.setAdapter(mMovieListAdapter);

        mScrollListener = new ScrollPagingListener(mLayoutManager);
        recyclerview_home.addOnScrollListener(mScrollListener);
    }

    private void getData(int pageNumber){
        if (pageNumber > 1) {
            swiperefresh_home.setRefreshing(true);
        } else {
            recyclerview_home.removeOnScrollListener(mScrollListener);
            mScrollListener = new ScrollPagingListener(mLayoutManager);
            recyclerview_home.addOnScrollListener(mScrollListener);
        }
        if (Util.getConnected(this)) {
            Server.getMovies(mSortingOption, pageNumber, new GetMoviesListener());
        } else {
            handleServerSuccess(false);
        }
    }

    private void setLoading(boolean isLoading){
        if (isLoading){
            pb_loading_indicator.setVisibility(View.VISIBLE);
            recyclerview_home.setVisibility(View.GONE);
        } else {
            pb_loading_indicator.setVisibility(View.GONE);
            recyclerview_home.setVisibility(View.VISIBLE);
        }
    }

    public void handleServerResponse(final String response){

        //runOnUiThread needs to be done because the adapter's notifydatasetchanged only works on UI thread
        HomeActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final ArrayList<Movie> movies = Movie.listFromJson(response);
                setLoading(false);
                handleServerSuccess(movies != null && movies.size() > 0);
                if (swiperefresh_home.isRefreshing()){
                    mMovieListAdapter.addData(movies);
                    swiperefresh_home.setRefreshing(false);
                } else {
                    mMovieListAdapter.setData(movies);
                }
            }
        });

    }

    private void handleServerSuccess(boolean success){
        if (success){
            tv_error_message_display.setVisibility(View.GONE);
        } else {
            tv_error_message_display.setVisibility(View.VISIBLE);
        }
    }

    private class GetMoviesListener implements Server.ServerListener{

        @Override
        public void serverResponse(String response) {
            handleServerResponse(response);
        }
    }

    private class ScrollPagingListener extends RecyclerView.OnScrollListener {

        private int previousTotal = 0; // The total number of items in the dataset after the last load
        private boolean loading = true; // True if we are still waiting for the last set of data to load.
        private int visibleThreshold = 5; // The minimum amount of items to have below your current scroll position before loading more.
        int firstVisibleItem, visibleItemCount, totalItemCount;

        private int current_page = 1;

        private GridLayoutManager mLayoutManager;

        public ScrollPagingListener(GridLayoutManager linearLayoutManager) {
            this.mLayoutManager = linearLayoutManager;
        }

        public void reset(){
            previousTotal = 0;
            loading = false;
            current_page = 1;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = mLayoutManager.getItemCount();
            firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount)
                    <= (firstVisibleItem + visibleThreshold)) {
                // End has been reached
                current_page++;

                getData(current_page);

                loading = true;
            }
        }
    }
}
