package ledcmd.plasmarobo.com.ledcommand.Patterns;

import java.util.Random;

/**
 * Created by root on 2/29/16.
 */
public class ChasePattern extends BasicPattern {
    private int position;
    private Random rand;

    public ChasePattern()
    {
        rand = new Random();
        position = getNewPosition();
    }

    private int getNewPosition()
    {
        return rand.nextInt(3);
    }

    @Override
    public void tick()
    {
        colors[position] = (byte)0;
        position += 3;
        if(position > colors.length)
        {
            position = getNewPosition();
        }
        colors[position] = (byte)0xA0;

    }
}
