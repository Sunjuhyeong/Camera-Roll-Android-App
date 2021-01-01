package us.koller.cameraroll.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import us.koller.cameraroll.R;
import us.koller.cameraroll.room.ImageDB;

public class ShowDBActivity extends AppCompatActivity {
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showdb_activity);

        textView = (TextView) findViewById(R.id.textView);
        //DB 생성
        ImageDB db = ImageDB.getDatabase(this);

        String s = db.imageDataDao().getAll().toString();

        //디비에 저장된 DescribeData들을 가지고옴
        textView.setText(s);
    }
}
