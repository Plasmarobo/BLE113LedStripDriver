package ledcmd.plasmarobo.com.ledcommand.Patterns;

/**
 * Created by root on 2/29/16.
 */
public class BasicPattern implements ColorPattern {
    protected byte[] colors;

    @Override
    public void tick()
    {

    }

    @Override
    public void setLength(int leds) {
        colors = new byte[leds*3];
    }

    @Override
    public byte[] getPattern() {
        return colors;
    }
}
