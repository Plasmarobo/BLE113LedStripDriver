#include <FastLED.h>
#define NUM_LEDS 160
#define SPI_DATA_PIN 13
#define SPI_CLK_PIN 14
CRGB leds[NUM_LEDS];

void setup()
{
  FastLED.addLeds<LPD8806, SPI_DATA_PIN, SPI_CLK_PIN>(leds, NUM_LEDS);
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
