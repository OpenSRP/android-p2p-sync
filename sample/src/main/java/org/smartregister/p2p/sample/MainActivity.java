package org.smartregister.p2p.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.smartregister.p2p.activity.P2pModeSelectActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startActivity(new Intent(this, P2pModeSelectActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
