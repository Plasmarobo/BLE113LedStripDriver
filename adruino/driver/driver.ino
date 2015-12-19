#include <SPI.h>

const unsigned short MAX_LEDS = 480;


void setup() {
  SPI.begin();
  SPI.transfer(
}

void loop() {
  SPI.beginTransaction(SPISettings(2000000, MSBFIRST, SPI_MODE0));
  writeRGB(20,0,0);
  writeRGB(0,20,0);
  writeRGB(0,0,20);
  
  SPI.endTransaction();
  delay(1000);
  writeZero();
  writeZero();
  writeZero();
  writeZero();
  writeZero();
}

void writeRGB(byte r, byte g, byte b)
{
  SPI.transfer(g|0x80);
  SPI.transfer(r|0x80);
  SPI.transfer(b|0x80);
}

void writeZero()
{
  SPI.transfer(0);
}



