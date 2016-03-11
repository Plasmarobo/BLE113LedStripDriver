#include <FastLED.h>
#define NUM_LEDS 160
CRGB leds[NUM_LEDS];

void setup()
{
  FastLED.addLeds<LPD8806, RGB>(leds, NUM_LEDS);
}

void loop()
{
  leds[0] = CRGB::Red;
  FastLED.show();
  delay(500);
  leds[0] = CRGB::Black;
  FastLED.show();
  delay(500);
}
