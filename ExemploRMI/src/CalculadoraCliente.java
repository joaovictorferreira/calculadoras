import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;


public class CalculadoraCliente {

    //Menu de interacao com o usuario
    public static void main(String[] args) {
        Registry reg = null;
        ICalculadora calc = null;
        try {
            reg = LocateRegistry.getRegistry(1099);
            calc = (ICalculadora) reg.lookup("calculadora");
        } catch (RemoteException | NotBoundException e) {
            System.out.println(e);
            System.exit(0);
        }

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

                    if (modo.equals("1")) System.out.println("resultado=" + calcularDecompondo(calc, expr));
                    else System.out.println("resultado=" + calc.avaliarExpressao(expr));
                } else {
                    System.out.print("Operando 1: ");
                    double a = Double.parseDouble(sc.nextLine().trim());
                    System.out.print("Operando 2: ");
                    double b = Double.parseDouble(sc.nextLine().trim());

                    System.out.println("resultado=" + chamar(calc, "+-/*".charAt(operacao - 1), a, b));
                }
            } catch (RemoteException | RuntimeException e) {
                System.out.println("ERRO: " + e.getMessage());
            }
        }
        sc.close();
    }

    //Chama no servidor o metodo remoto correspondente ao operador
    public static double chamar(ICalculadora calc, char op, double a, double b) throws RemoteException {
        switch (op) {
            case '-':
                return calc.subtrai(a, b);
            case '*':
                return calc.multiplica(a, b);
            case '/':
                return calc.divide(a, b);
            default:
                return calc.soma(a, b);
        }
    }

    //Abordagem 1: o cliente interpreta a expressao e faz uma chamada remota por operacao
    public static double calcularDecompondo(ICalculadora calc, String expr) {
        return Expressao.avaliar(expr, (op, a, b) -> {
            try {
                double r = chamar(calc, op, a, b);
                System.out.println("   chamada remota: " + a + " " + op + " " + b + " -> " + r);
                return r;
            } catch (RemoteException e) {
                throw new RuntimeException(e.getMessage());
            }
        });
    }

}