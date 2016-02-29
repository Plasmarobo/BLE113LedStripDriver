package ledcmd.plasmarobo.com.ledcommand.Patterns;

/**
 * Created by root on 2/29/16.
 */
public interface ColorPattern {
    void tick();
    void setLength(int leds);
    byte[] getPattern();
}
