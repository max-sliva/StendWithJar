#include <OLED_I2C.h>
OLED display(SDA, SCL, 8);
extern uint8_t SmallFont[];
#define n 3           //кол-во кнопок
char recv = '0';      //переменная для приема и отправки сообщений
char lastRecv = '0';  //дополнительная переменная для хранения предыдущего значения recv
String recvStr = "";
byte buttonPress = HIGH;                       // переменная для определения нажата кнопка или нет, HIGH – не нажата
byte buttArray[n] = { 9, 10, 11 };             //массив с пинами кнопок
byte ledArray[n] = { 29, 30, 31 };                //массив с пинами светодиодов
String numbersArray[n] = { "9", "10", "11" };  //строки с номерами кнопок (может, не нужны)
bool butState[n] = { 0, 0, 0 };                   //массив состояний кнопок

long time;  // переменная для таймера
void setup() {
  pinMode(13, OUTPUT);  //настраиваем пин для встроенного светодиода
  for (byte i = 0; i < n; i++) {
    pinMode(buttArray[i], INPUT_PULLUP);  //настраиваем пин для кнопки
    pinMode(ledArray[i], OUTPUT);
  }
  digitalWrite(13, LOW);  //гасим светодиод на всякий случай
  Serial.begin(9600);     //задаем скорость порта
  //time = millis(); //стартуем таймер
  display.begin();
  // display.drawCircle(CENTER, CENTER, 30);
  display.setFont(SmallFont);
  display.print("from comp = ", 10, 10);
  display.update();
}
void loop() {
  if (Serial.available() > 0) {  //если есть данные для приема из ком-порта
    recv = Serial.read();        //считываем 1 символ в переменную recv
    recvStr += recv;
    display.print(recvStr, 10, 30);
    display.update();
    if (recv == '+') {
      // send(numbersArray); //todo отправлять кол-во кнопок, а не их порты
      display.print("-----", 10, 30);
      display.update();
      //recv
      Serial.print("n=");
      Serial.print(n);
      Serial.println(';');
    } else if (recv == ';') {
      fireLed(recvStr);
      display.print(recvStr, 10, 30);
      display.update();
      recvStr = "";
    } else {
      // recvStr += recv;
      // display.print(recvStr, 10, 30);
      // display.update();
    }
  }  //можно использовать Serial.readString() для чтения строк
  for (byte i = 0; i < n; i++)
    buttonsListener(i);

  delay(15);  //небольшая задержка для правильной работы всей схемы
}

void send(String *numbersArray) {
  for (int i = 0; i < n; i++) {
    Serial.print(numbersArray[i]);
    if (i != n - 1) Serial.print(',');
  }
  Serial.println(';');
}

void fireLed(String recvStr) {
  // Serial.println(recvStr.toInt());
  byte buttI = recvStr.toInt();
  for (byte i = 0; i < n; i++) {
    if (i != buttI) digitalWrite(ledArray[i], LOW);
    else digitalWrite(ledArray[i], HIGH);
  }
  // digitalWrite(ledArray[recvStr.toInt()], HIGH);
}

void buttonsListener(byte buttI) {
  bool but = digitalRead(buttArray[buttI]);   //считываем состояние кнопки
  if (but == LOW) butState[buttI] = 1;        //если нажали, задаем переменной state 1 (true)
  if (but == HIGH && butState[buttI] == 1) {  //если кнопку отпустили
    // Serial.print(numbersArray[buttI]);
    // Serial.print(buttArray[buttI]);
    Serial.print(buttI);
    Serial.println(";");
    for (byte i = 0; i < n; i++) {
      if (i != buttI) digitalWrite(ledArray[i], LOW);
      else digitalWrite(ledArray[i], HIGH);
    }
    butState[buttI] = 0;  //сбрасываем состояние кнопки
  }
}
