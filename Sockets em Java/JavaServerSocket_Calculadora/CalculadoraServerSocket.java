import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor da calculadora via socket TCP.
 * <p>
 * Protocolo (3 linhas de texto, como no projeto original):
 * linha 1: operacao -> 1=soma 2=subtracao 3=divisao 4=multiplicacao 5=expressao
 * linha 2: operando 1 (ou a expressao inteira, quando a operacao e 5)
 * linha 3: operando 2 (ignorado quando a operacao e 5)
 * Resposta: o resultado, ou "ERRO: <motivo>".
 */
public class CalculadoraServerSocket {

    public static void main(String[] args) {
        ServerSocket welcomeSocket;
        DataOutputStream socketOutput;
        DataInputStream socketInput;
        BufferedReader socketEntrada;
        Calculadora calc = new Calculadora();
        try {
            welcomeSocket = new ServerSocket(9090);
            int i = 0; //numero de clientes

            System.out.println("Servidor no ar");
            while (true) {

                Socket connectionSocket = welcomeSocket.accept();
                i++;
                System.out.println("Nova conexao");

                //Interpretando dados do servidor
                socketEntrada = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                String operacao = socketEntrada.readLine();
                String oper1 = socketEntrada.readLine();
                String oper2 = socketEntrada.readLine();

                //Chamando a calculadora (antes era sempre soma; agora usa a operacao recebida)
                String result;
                try {
                    int cod = Integer.parseInt(operacao);
                    double valor;
                    if (cod == 5) {
                        //operacao 5 = expressao inteira, calculada aqui no servidor
                        valor = Expressao.avaliar(oper1, (operador, x, y) -> calc.executa(operador, x, y));
                    } else {
                        double a = Double.parseDouble(oper1);
                        double b = Double.parseDouble(oper2);
                        switch (cod) {
                            case 2:
                                valor = calc.subtrai(a, b);
                                break;
                            case 3:
                                valor = calc.divide(a, b);
                                break;
                            case 4:
                                valor = calc.multiplica(a, b);
                                break;
                            default:
                                valor = calc.soma(a, b);
                        }
                    }
                    result = "" + valor;
                } catch (RuntimeException e) {
                    //erro de calculo ou expressao invalida: responde o erro e continua no ar
                    result = "ERRO: " + e.getMessage();
                }

                //Enviando dados para o servidor
                socketOutput = new DataOutputStream(connectionSocket.getOutputStream());
                socketOutput.writeBytes(result + '\n');
                System.out.println(result);
                socketOutput.flush();
                socketOutput.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}