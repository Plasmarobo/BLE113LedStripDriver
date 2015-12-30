#include <SPI.h>

const unsigned short MAX_LEDS = 480;
const byte LEDS_PER_METER = 32;

unsigned short num_leds;
byte meters;

byte strip_data[1440]; //(480*3)
byte zero[] = {0,0,0,0,0}; //at most 5m
unsigned short strip_pos;

void setLedCount(unsigned short led_count)
{
  num_leds = (led_count < MAX_LEDS) ? led_count : MAX_LEDS;
  meters = num_leds / LEDS_PER_METER;
}

void writeStripData()
{
  SPI.beginTransaction(SPISettings(300, MSBFIRST, SPI_MODE0));
  SPI.transfer((void*)&(strip_data[0]), (num_leds*3));
  writeZero();
  SPI.endTransaction();
  strip_pos = 0;
}

byte sinePattern(byte center, float period_ms, byte amplitude = 0)
{
  if(amplitude = 0)
  {
    amplitude = center;
  }
  return center + (amplitude * sin(2*PI*(((float)millis())/period_ms))); 
}

void setup() {
  SPI.begin();
  setLedCount(160); //Single strip for now
  for(int i = 0; i < num_leds; ++i)
  {
    writeRGB(0,0,0);
  }
  writeStripData();
}

void loop() {
  //Implement some sort of pattern
  for(int i = 0; i < num_leds; ++i)
  {
    //Generate three sines between 0 and 128
    writeRGB(12, 0,0);
  }
  writeStripData();
  delay(1000);
}

void writeRGB(byte r, byte g, byte b)
{
  strip_data[strip_pos++] = g | 0x80;
  strip_data[strip_pos++] = r | 0x80;
  strip_data[strip_pos++] = b | 0x80;
}

void writeZero()
{
  SPI.transfer( (void*)&(zero[0]), meters);
}




