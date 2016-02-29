package ledcmd.plasmarobo.com.ledcommand.Patterns;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ledcmd.plasmarobo.com.ledcommand.BluetoothLink;
import ledcmd.plasmarobo.com.ledcommand.R;

/**
 * Created by root on 2/29/16.
 */
public class PatternManager {
    private BluetoothLink link;
    private ColorPattern currentPattern;
    private HashMap<String, ColorPattern> patterns;
    private int updateInterval;
    private Timer updateTimer;
    private SimpleAdapter listadapter;

    public PatternManager(BluetoothLink l)
    {
        link = l;
        updateTimer = new Timer();
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
        link.writeColor(currentPattern.getPattern());
        link.writeUpdate();
    }

    public void start()
    {
        updateTimer.scheduleAtFixedRate(new TimerTask() {
                                            @Override
                                            public void run() {
                                                currentPattern.tick();
                                                writePattern();
                                            }
                                        },
                0,
                updateInterval);
    }

    public void  stop()
    {
        updateTimer.cancel();
    }

    public void populateListView(Context context, ListView list)
    {
        String[] from = new String[] {"pattern_name"};
        int[] to = new int[] {R.id.patternname};
        List<HashMap<String, ColorPattern>> fillMap = new ArrayList<>();
        fillMap.add(patterns);
        listadapter = new SimpleAdapter(context, fillMap, R.layout.pattern_row, from, to);
        list.setAdapter(listadapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                stop();
                // selected item
                String selected = ((TextView) view.findViewById(R.id.patternname)).getText().toString();
                SetPattern(patterns.get(selected));
                start();
            }
        });

    }




}
