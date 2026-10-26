import json
import sys
import time

import paho.mqtt.client as mqtt

BROKER = "localhost"
PORTA = 1883
TOPICO_TEMPERATURA = "caldeira/temperatura"
TOPICO_ALARME = "caldeira/alarme"

# janela em segundos; 120 e o valor do enunciado, mas pode ser reduzido
# na gravacao do video, junto com o intervalo do sensor
JANELA = float(sys.argv[1]) if len(sys.argv) > 1 else 120
LIMITE_ALTA = 200   # media acima disso dispara "temperatura alta"
LIMITE_SALTO = 5    # diferenca entre duas medias que dispara "aumento repentino"

leituras = []       # lista de (instante, valor)
media_anterior = None


# Le o nome do sensor e o valor. Aceita JSON ou um numero puro, para funcionar
# tambem com sensores reais que publiquem apenas o valor.
def extrai(payload):
    texto = payload.decode()
    try:
        dados = json.loads(texto)
        return dados.get("sensor", "?"), float(dados["temperatura"])
    except Exception:
        return "?", float(texto)


# Publica um alarme no topico de alarmes
def publica_alarme(cliente, tipo, media):
    cliente.publish(TOPICO_ALARME, json.dumps({"tipo": tipo, "media": round(media, 2)}))
    print("[CAT] ALARME publicado: " + tipo)


# Chamada a cada mensagem de temperatura recebida
def ao_receber(cliente, userdata, msg):
    global media_anterior

    try:
        origem, valor = extrai(msg.payload)
    except Exception:
        print("[CAT] payload ignorado: " + str(msg.payload))
        return

    agora = time.time()
    leituras.append((agora, valor))

    # descarta as leituras que sairam da janela
    while leituras and agora - leituras[0][0] > JANELA:
        leituras.pop(0)

    media = sum(v for _, v in leituras) / len(leituras)
    print("[CAT] " + origem + ": leitura=" + str(valor) + "  |  media de "
          + str(len(leituras)) + " valores = " + str(round(media, 2)))

    if media_anterior is not None and abs(media - media_anterior) >= LIMITE_SALTO:
        publica_alarme(cliente, "aumento de temperatura repentina", media)

    if media > LIMITE_ALTA:
        publica_alarme(cliente, "temperatura alta", media)

    media_anterior = media


cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="cat")
cliente.on_message = ao_receber
cliente.connect(BROKER, PORTA)
cliente.subscribe(TOPICO_TEMPERATURA)

print("[CAT] escutando " + TOPICO_TEMPERATURA)
cliente.loop_forever()