package net.media.injector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import net.media.lib.Injector;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Injector.load(this);
  }
}
