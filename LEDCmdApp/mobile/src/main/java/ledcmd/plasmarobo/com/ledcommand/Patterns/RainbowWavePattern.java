package ledcmd.plasmarobo.com.ledcommand.Patterns;

/**
 * Created by root on 2/29/16.
 */
public class RainbowWavePattern extends BasicPattern {

    private final byte AMPLITUDE = 32;
    private final byte CENTER = 64;
    private final double FREQUENCY = 2.0;

    private final double redPhase = 0;
    private final double greenPhase = 2*Math.PI/3;
    private final double bluePhase = 2*Math.PI/4;

    private int pixelPosition;
    private double waveOffset;

    public RainbowWavePattern()
    {
        pixelPosition = 0;
        waveOffset = 0;
    }

    private byte colorSine(double angle, double phase)
    {
        return (byte)(Math.sin(FREQUENCY*angle + phase) * AMPLITUDE + CENTER);
    }

    private void setPixel(double angle)
    {
        int subpixel = pixelPosition * 3;
        colors[subpixel] = colorSine(angle, redPhase);
        colors[subpixel+1] = colorSine(angle, greenPhase);
        colors[subpixel+2] = colorSine(angle, bluePhase);
    }

    private void generateRainbow()
    {
        double waveStep = (2*Math.PI)/(colors.length);
        for(pixelPosition = 0; pixelPosition < colors.length; ++pixelPosition)
        {
            setPixel(waveStep * (pixelPosition+waveOffset));
        }
    }

    @Override
    public void tick()
    {
        generateRainbow();
        ++waveOffset;
    }



}
