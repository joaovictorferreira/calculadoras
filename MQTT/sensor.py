import json
import random
import sys
import time

import paho.mqtt.client as mqtt

BROKER = "localhost"
PORTA = 1883
TOPICO = "caldeira/temperatura"

# argumentos: nome do sensor, intervalo em segundos, perfil (demo ou estavel)
nome = sys.argv[1] if len(sys.argv) > 1 else "sensor1"
intervalo = float(sys.argv[2]) if len(sys.argv) > 2 else 5.0
perfil = sys.argv[3] if len(sys.argv) > 3 else "demo"


# Gera a temperatura de cada leitura. O perfil "demo" sobe de proposito,
# para que os dois alarmes acontecam durante a gravacao do video.
def temperatura(leitura):
    if perfil == "estavel":
        return 25 + random.uniform(-1, 1)
    if leitura < 5:
        return 25 + random.uniform(-1, 1)                      # operacao normal
    if leitura < 10:
        return 60 + random.uniform(-1, 1)                      # salto repentino
    return min(400, 150 + (leitura - 10) * 15) + random.uniform(-1, 1)   # sobe ate passar de 200


cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=nome)
cliente.connect(BROKER, PORTA)
cliente.loop_start()

print("[" + nome + "] publicando em " + TOPICO + " a cada " + str(intervalo) + "s")

leitura = 0
while True:
    valor = round(temperatura(leitura), 1)
    cliente.publish(TOPICO, json.dumps({"sensor": nome, "temperatura": valor}))
    print("[" + nome + "] temperatura = " + str(valor))
    leitura = leitura + 1
    time.sleep(intervalo)