package ntnu.tdt4240.shoutout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class HowToPlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TutorialPagerAdapter(this));

        findViewById(R.id.btn_back_to_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HowToPlayActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}