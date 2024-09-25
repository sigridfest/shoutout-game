package ntnu.tdt4240.shoutout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TutorialPagerAdapter extends RecyclerView.Adapter<TutorialPagerAdapter.TutorialViewHolder> {

    private final LayoutInflater mLayoutInflater;
    private final int[] mLayouts = new int[]{
            R.layout.tutorial_page_1,
            R.layout.tutorial_page_2,
            R.layout.tutorial_page_3,
            R.layout.tutorial_page_4,
            R.layout.tutorial_page_5,
            R.layout.tutorial_page_6,
            R.layout.tutorial_page_7

    };

    public TutorialPagerAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public TutorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(viewType, parent, false);
        return new TutorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorialViewHolder holder, int position) {
        // No-op
    }

    @Override
    public int getItemCount() {
        return mLayouts.length;
    }

    @Override
    public int getItemViewType(int position) {
        return mLayouts[position];
    }

    static class TutorialViewHolder extends RecyclerView.ViewHolder {

        public TutorialViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}

