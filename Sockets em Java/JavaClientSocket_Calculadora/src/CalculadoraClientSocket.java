import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

/**
 * Cliente da calculadora via socket TCP.
 * Interface textual: operacoes simples (1 a 4) ou expressao com parenteses (5).
 * Para expressoes existem as duas abordagens pedidas na atividade:
 * decompor no cliente (varias chamadas) ou enviar a expressao pro servidor (uma chamada).
 */
public class CalculadoraClientSocket {

    //Menu de interacao com o usuario
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n1-somar 2-subtrair 3-dividir 4-multiplicar 5-expressao 0-sair");
            System.out.print("Operacao: ");
            int operacao = Integer.parseInt(sc.nextLine().trim());
            if (operacao == 0) break;

            try {
                if (operacao == 5) {
                    System.out.print("Expressao (ex: (10+15)*4 ): ");
                    String expr = sc.nextLine().trim();
                    System.out.print("1-decompor no cliente  2-calcular no servidor: ");
                    String modo = sc.nextLine().trim();

                    if (modo.equals("1")) {
                        System.out.println("resultado=" + calcularDecompondo(expr));
                    } else {
                        String r = calcular(5, expr, "0");
                        System.out.println(r.startsWith("ERRO") ? r : "resultado=" + r);
                    }
                } else {
                    System.out.print("Operando 1: ");
                    String oper1 = sc.nextLine().trim();
                    System.out.print("Operando 2: ");
                    String oper2 = sc.nextLine().trim();

                    String r = calcular(operacao, oper1, oper2);
                    System.out.println(r.startsWith("ERRO") ? r : "resultado=" + r);
                }
            } catch (RuntimeException e) {
                System.out.println("ERRO: " + e.getMessage());
            }
        }
        sc.close();
    }

    //Abordagem 1: o cliente interpreta a expressao e faz uma chamada remota por operacao
    public static double calcularDecompondo(String expr) {
        return Expressao.avaliar(expr, (operador, a, b) -> {
            int cod;
            switch (operador) {
                case '-':
                    cod = 2;
                    break;
                case '/':
                    cod = 3;
                    break;
                case '*':
                    cod = 4;
                    break;
                default:
                    cod = 1;
            }
            String r = calcular(cod, "" + a, "" + b);
            System.out.println("   chamada remota: " + a + " " + operador + " " + b + " -> " + r);
            if (r.startsWith("ERRO")) throw new ArithmeticException(r.replace("ERRO: ", ""));
            return Double.parseDouble(r);
        });
    }

    //Abre a conexao, envia as 3 linhas do protocolo e devolve a resposta do servidor
    public static String calcular(int operacao, String oper1, String oper2) {
        String result = "";
        try {

            //Conexao com o Servidor
            Socket clientSocket = new Socket("localhost", 9090);
            DataOutputStream socketSaidaServer = new DataOutputStream(clientSocket.getOutputStream());

            //Enviando os dados
            socketSaidaServer.writeBytes(operacao + "\n");
            socketSaidaServer.writeBytes(oper1 + "\n");
            socketSaidaServer.writeBytes(oper2 + "\n");
            socketSaidaServer.flush();

            //Recebendo a resposta
            BufferedReader messageFromServer = new BufferedReader
                    (new InputStreamReader(clientSocket.getInputStream()));
            result = messageFromServer.readLine();

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}