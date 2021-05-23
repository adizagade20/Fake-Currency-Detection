#include <SoftwareSerial.h>
#include <FastLED.h>


#define NUM_LEDS 60
#define DATA_PIN 10
#define COLOR_ORDER GRB

int ledNumbers[29] = {2,3,4,5,6,7,8,9,18,19,20,21,22,23,24,34,35,36,37,38,39,40,50,51,52,53,54,55,56};

CRGB leds[NUM_LEDS];

void setup() {
  FastLED.addLeds<WS2812B, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  Serial.begin(9600);
}

void loop() {
  int i;

  for(i=0; i<60; i++) {
    leds[i] = CRGB(0,0,0);
    FastLED.show();
  }

  if(Serial.available()) {
    String data = Serial.readString();

    if(data == "A") {
      for(i=0; i<60; i++) {
        leds[i] = CRGB(0,0,0);
        FastLED.show();
      }
      delay(200);
      
      for(i=0; i<29; i++) {
        leds[ledNumbers[i]] = CRGB::White;
        FastLED.show();
        delay(20);
      }

      Serial.println("A");

      delay(7000);

      for(i=0; i<60; i++) {
        leds[i] = CRGB(0,0,0);
        FastLED.show();
        delay(20);
      }
    }

    else if(data == "B") {
      for(i=0; i<60; i++) {
        leds[i] = CRGB(0,0,0);
        FastLED.show();
      }
      delay(200);
      
      for(i=0; i<29; i++) {
        leds[ledNumbers[i]] = CRGB::Violet;
        FastLED.show();
        delay(20);
      }

      Serial.println("B");

      delay(7000);

      for(i=0; i<60; i++) {
        leds[i] = CRGB(0,0,0);
        FastLED.show();
        delay(20);
      }
    }
 
    else {
      for(i=0; i<60; i++) {
        leds[i] = CRGB(0,0,0);
        printf("%d", ledNumbers[i]);
        FastLED.show();
      }
    }
  }
//  else {
//    for(i=0; i<60; i++) {
//      leds[i] = CRGB(0,0,0);
//      FastLED.show();
//    }
//  }
}
