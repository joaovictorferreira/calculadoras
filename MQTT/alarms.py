import json
import time

import paho.mqtt.client as mqtt

BROKER = "localhost"
PORTA = 1883
TOPICO_ALARME = "caldeira/alarme"


def ao_receber(cliente, userdata, msg):
    alarme = json.loads(msg.payload.decode())
    hora = time.strftime("%H:%M:%S")
    print("[" + hora + "]  *** ALARME: " + alarme["tipo"].upper()
          + " - media " + str(alarme["media"]) + " ***")


cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="alarms")
cliente.on_message = ao_receber
cliente.connect(BROKER, PORTA)
cliente.subscribe(TOPICO_ALARME)

print("[ALARMS] escutando " + TOPICO_ALARME)
cliente.loop_forever()