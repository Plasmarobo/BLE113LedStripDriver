package ledcmd.plasmarobo.com.ledcommand.Patterns;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ledcmd.plasmarobo.com.ledcommand.BluetoothLink;
import ledcmd.plasmarobo.com.ledcommand.R;

/**
 * Created by root on 2/29/16.
 */
public class PatternManager {
    private BluetoothLink link;
    private ColorPattern currentPattern;
    private HashMap<String, ColorPattern> patterns;
    private int updateInterval; //Miliseconds
    private ScheduledThreadPoolExecutor updateTimer;

    public PatternManager(BluetoothLink l)
    {
        link = l;
        updateTimer = null;
        updateInterval = 1000;
        patterns = new HashMap<String, ColorPattern>();
        patterns.put("RainbowWave", new RainbowWavePattern());
        patterns.put("Cylon", new CylonPattern());
        patterns.put("Chase", new ChasePattern());
        patterns.put("MultiSpark", new MultiSparkPattern());
    }

    public void SetPattern(ColorPattern pattern)
    {
        currentPattern = pattern;
        pattern.setLength(link.getLedCount());
    }

    public void setUpdateInterval(int interval)
    {
        updateInterval = interval;
    }

    private void writePattern()
    {
        link.writeClear();
        link.writeColor(currentPattern.getPattern());
        link.writeUpdate();
    }

    public void start()
    {
        stop();
        updateTimer = new ScheduledThreadPoolExecutor(2);
        updateTimer.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                currentPattern.tick();
                writePattern();
            }
        }, 0, updateInterval, TimeUnit.MILLISECONDS);
    }

    public void  stop()
    {
        if(updateTimer != null) updateTimer.shutdown();
        updateTimer = null;
    }

    public void populateLayout(Context context, LinearLayout list)
    {
        for(String name : this.patterns.keySet())
        {
            Button b = new Button(context);
            b.setText(name);
            b.setOnClickListener(new Button.OnClickListener() {

                @Override
                public void onClick(View v) {
                    stop();
                    SetPattern(patterns.get(((Button) v).getText()));
                    start();
                }
            });
            list.addView(b);
        }


    }




}
