package com.example.demo.apt;

import android.app.Activity;
import android.widget.Button;
import android.widget.ListView;

import com.example.apt.annotation.BindView;
import com.example.demo.R;

public class AptDemo extends Activity {
    @BindView(R.id.button1)
    Button button1;
    @BindView(R.id.list)
    ListView listView;
}
