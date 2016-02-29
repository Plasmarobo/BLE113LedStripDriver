package ledcmd.plasmarobo.com.ledcommand.Patterns;

/**
 * Created by root on 2/29/16.
 */
public class CylonPattern extends BasicPattern {

    private int direction;
    private int position;
    private final int FORWARD = 1;
    private final int BACKWARDS = -1;

    public CylonPattern()
    {
        colors = null;
        direction = FORWARD;
        position = 0;
    }
    
    @Override
    public void tick() {
        colors[position] = (byte)0;
        if (direction == FORWARD)
        {
            position += 3;
        }
        else if (direction == BACKWARDS)
        {
            position -= 3;
        }
        if(position > colors.length)
        {
            position = colors.length - 2;
            direction = BACKWARDS;
        }else if (position < 0)
        {
            position = 0;
            direction = FORWARD;
        }
    }


}
